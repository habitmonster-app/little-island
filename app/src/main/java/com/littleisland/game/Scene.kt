package com.littleisland.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 옆에서 본 바다 띠 (200 x 467 도트).
 * 화면 가운데 가로 띠에서 늘 무언가 움직인다:
 *  - 배가 섬에 있으면: 섬 장면 (주민이 나무를 베고 돌을 날라 창고에 쌓는다)
 *  - 항해 중이면: 배가 오른쪽으로 나아가고 바다·먼 섬·구름이 흘러간다.
 *    출항 → 해적과 포격전 → 항구에 닿아 짐 내리기 → 다시 섬으로.
 */
class Scene {
    companion object {
        const val W = 200
        const val H = 467
        const val HOR = 226f      // 수평선
        const val GROUND = 244f   // 섬 땅 높이 (발 딛는 곳)
        const val WL = 252f       // 물 높이 (배가 뜨는 곳)
        const val SAIL_X = 74f    // 항해 중 배가 머무는 화면 위치
        const val SHIFT = 260f    // 장면이 들어오고 나가는 거리
        /** 좌표는 200도트 그대로, 실제 그림은 SS배 크기로 그려서 또렷하게 */
        const val SS = 3
    }

    val bitmap: Bitmap = Bitmap.createBitmap(W * SS, H * SS, Bitmap.Config.ARGB_8888)
    private val mainC = Canvas(bitmap).apply { scale(SS.toFloat(), SS.toFloat()) }
    private var c = mainC
    /** 앱의 양옆 띠용: 배·섬·항구 없이 하늘과 바다만 그린 그림 (wantBg일 때만) */
    var wantBg = false
    val bgBitmap: Bitmap by lazy { Bitmap.createBitmap(W * SS, H * SS, Bitmap.Config.ARGB_8888) }
    private val bgC by lazy { Canvas(bgBitmap).apply { scale(SS.toFloat(), SS.toFloat()) } }
    private fun aget(n: String, w: Int) = Art.get(n, w * SS)
    private fun acell(n: String, cols: Int, rows: Int, i: Int, w: Int) = Art.cell(n, cols, rows, i, w * SS)
    private val p = Paint().apply { isAntiAlias = false }
    private val gp = Paint().apply { isAntiAlias = false }
    private var ga = 1f

    var hourOverride: Float? = null

    // ---------- 그리기 도구 ----------
    private fun col(hex: String) = Color.parseColor(hex)
    private fun r(x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.color = color
        p.alpha = (Color.alpha(color) * ga).roundToInt().coerceIn(0, 255)
        val xx = x.roundToInt().toFloat(); val yy = y.roundToInt().toFloat()
        c.drawRect(xx, yy, xx + w.roundToInt(), yy + h.roundToInt(), p)
    }
    private fun ell(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        val iry = ry.roundToInt()
        if (iry <= 0) { r(cx - rx, cy, rx * 2, 1f, color); return }
        for (y in -iry..iry) {
            val t = 1f - (y * y).toFloat() / (ry * ry)
            if (t < 0f) continue
            val w = (rx * sqrt(t)).roundToInt().toFloat()
            r(cx - w, cy + y, w * 2, 1f, color)
        }
    }
    private fun mix(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        fun ch(x: Int, y: Int) = (x + (y - x) * tt).roundToInt()
        return Color.rgb(ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
    private fun ease(t: Float): Float { val x = t.coerceIn(0f, 1f); return x * x * (3 - 2 * x) }

    // ---------- 시간대 색 ----------
    private class Key(val h: Float, top: String, bot: String, sea: String, val dark: Float) {
        val top = Color.parseColor(top); val bot = Color.parseColor(bot); val sea = Color.parseColor(sea)
    }
    private val keys = listOf(
        Key(0f, "#0b1030", "#1c2350", "#0f2a4a", .7f),
        Key(5f, "#0b1030", "#1c2350", "#0f2a4a", .7f),
        Key(6.2f, "#3a3f7a", "#f0a27a", "#2a5a7a", .3f),
        Key(7.6f, "#4aa8e8", "#bfe6f5", "#2f8fb8", 0f),
        Key(17f, "#4aa8e8", "#bfe6f5", "#2f8fb8", 0f),
        Key(18.5f, "#5a4f9a", "#ff9a5a", "#3a6a8a", .2f),
        Key(19.6f, "#2a2a60", "#a05a70", "#1f3f60", .5f),
        Key(21f, "#0b1030", "#1c2350", "#0f2a4a", .7f),
        Key(24f, "#0b1030", "#1c2350", "#0f2a4a", .7f),
    )
    private var skyTop = 0; private var skyBot = 0; private var sea = 0; private var dark = 0f
    private fun palette(h: Float) {
        for (i in 0 until keys.size - 1) {
            val a = keys[i]; val b = keys[i + 1]
            if (h >= a.h && h <= b.h) {
                val t = if (b.h - a.h == 0f) 0f else (h - a.h) / (b.h - a.h)
                skyTop = mix(a.top, b.top, t); skyBot = mix(a.bot, b.bot, t); sea = mix(a.sea, b.sea, t)
                dark = a.dark + (b.dark - a.dark) * t
                return
            }
        }
    }
    fun hourNow(): Float {
        hourOverride?.let { return it }
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
    }

    // =====================================================================
    // 섬 사람들

    private class Tree(val x: Float) { var hp = 3; var regrow = 0f; var shake = 0f }
    private class Rock(val x: Float)
    private class Person(var x: Float, val look: Int) {
        var st = 0          // 0 쉼, 1 일터로, 2 일하는 중, 3 창고로
        var t = Random.nextFloat() * 2f
        var tx = 0f
        var tree: Tree? = null
        var carry = 0       // 1 나무, 2 돌
        var dir = 1f
        var step = 0f
        var wid = ""        // 견습선원이면 견습선원 id
    }

    private val trees = listOf(12f, 28f, 160f).map { Tree(it) }
    private val rocks = listOf(44f, 146f).map { Rock(it) }
    private val storeX = 62f          // 창고 가운데
    private val fireX = 86f
    private val houseX = listOf(104f, 124f, 144f)
    private val dockX = 170f
    private val people = ArrayList<Person>()

    /** 주민 3명 + 지금 일하는 견습선원 */
    private fun syncPeople() {
        val want = Game.RESIDENTS + Game.workers.size
        while (people.size < want) {
            val i = people.size
            people.add(Person(if (i < 3) fireX - 6 + i * 6 else dockX, if (i < 3) i else 3 + (i % 3)))
        }
        while (people.size > want) people.removeAt(people.size - 1)
        for (i in 3 until people.size) people[i].wid = Game.workers.getOrNull(i - 3)?.id ?: ""
    }

    /** 떠다니는 물건: 0 유리병, 1 보물상자, 2 갈매기(나무 물고 감) */
    class Floater(var x: Float, var y: Float, val kind: Int, var t: Float)
    val floaters = ArrayList<Floater>()
    private var floatWait = 20f

    /** 톡 친 곳 근처의 물건을 줍는다 (그림 좌표). 주우면 보상 글 */
    fun tap(x: Float, y: Float): String? {
        visX?.let { vx -> if (abs(vx - x) < 26 && y > visY - 34 && y < visY + 6) {
            val ok = Game.canTrade(); val t = Game.trade()
            pops.add(Pop(vx, visY - 36, t, 3.5f, ok)); return t } }
        // 바닷속: 진주 조개, 지나가는 친구
        clamX?.let { cx -> if (abs(cx - x) < 14 && abs(y - (SAND - 6)) < 14) { val t = Game.takePearl(); if (t != null) { pops.add(Pop(cx, SAND - 24, t, 3.5f, true)); return t } } }
        swimmer?.let { sw -> val sy = swimY(sw); if (abs(sw.x - x) < 18 && abs(y - sy) < 18) { val t = Game.seeCritter(sw.kind); if (t.isNotEmpty()) { pops.add(Pop(sw.x, sy - 18, t, 3.5f, true)); return t } } }
        // 바다 동물을 톡 치면 도감에 적는다
        critter?.let { cr -> if (abs(cr.x - x) < 22 && abs(y - WL) < 26) { val t = Game.seeCritter(cr.kind); if (t.isNotEmpty()) { pops.add(Pop(cr.x, WL - 30, t, 3.5f, true)); return t } } }
        val f = floaters.minByOrNull { abs(it.x - x) + abs(it.y - y) } ?: return null
        if (abs(f.x - x) > 22 || abs(f.y - y) > 26) return null
        floaters.remove(f)
        val t = Game.pickUp(f.kind)
        pops.add(Pop(f.x, f.y - 10, t, 3.5f, true))
        return t
    }

    // ---------- 지나가는 바다 동물 (장식) ----------
    private class Critter(val kind: Int, var x: Float, var t: Float)
    private var critter: Critter? = null
    private var critterWait = 20f + Random.nextFloat() * 40f
    private fun updateCritter(dt: Float) {
        critter?.let { cr -> cr.t += dt; cr.x -= dt * (when (cr.kind) { 2 -> 7f; 1 -> 9f; 4 -> 12f; 5 -> 4f; 6 -> 10f; 7 -> 5f; else -> 16f }); if (cr.x < -30f) critter = null }
        if (critter == null) {
            critterWait -= dt
            if (critterWait <= 0f) { critterWait = (60f + Random.nextFloat() * 90f) / Game.tideV("critter_more", 1f); critter = Critter(Random.nextInt(if (Art.has("sea_creatures_2")) 8 else 4), W + 24f, 0f) }
        }
    }
    /** Lv11 이상이면 다 자란 선원 대사(40번)도 섞는다 */
    private fun voiceOf(id: String, fight: Boolean): String? {
        val base = Fleet.voices[id]?.let { if (fight) it.fight else it.deck } ?: emptyList()
        val more = if (Game.crewLevel(id) >= 11) Fleet.voices2[id]?.let { if (fight) it.fight else it.deck } ?: emptyList() else emptyList()
        return (base + more).randomOrNull()
    }
    fun spawnCritter(kind: Int, x: Float, t: Float) { critter = Critter(kind, x, t) }
    private fun drawCritter() {
        val cr = critter ?: return
        val fr = (floor(cr.t * (if (cr.kind == 1) 1.6f else 3f)) % 4).toInt()
        // 칸 가운데가 수면. 칸 크기 36도트 → 아래 끝을 수면 + 18에 맞춘다 (먼 바다라 약간 위)
        val sz = if (cr.kind == 1) 54 else 36          // 고래는 크게
        val bm = (if (cr.kind >= 4) acell("sea_creatures_2", 4, 4, (cr.kind - 4) * 4 + fr, sz) else acell("sea_creatures", 4, 4, cr.kind * 4 + fr, sz)) ?: return
        Art.draw(c, bm, cr.x, WL + 2 + sz / 2f)
    }

    private fun updateFloaters(dt: Float) {
        floatWait -= dt
        if (floatWait <= 0f && floaters.size < 2) {
            floatWait = (35f + Random.nextFloat() * 50f) / Game.tideV("find_more", 1f)
            val k = Random.nextFloat()
            floaters.add(when {
                k < .12f -> Floater(W + 10f, WL + 1, 1, 40f)
                k < .35f -> Floater(W + 10f, HOR - 40 - Random.nextFloat() * 30, 2, 30f)
                else -> Floater(W + 10f, WL + 1, 0, 40f)
            })
        }
        for (f in floaters) {
            f.t -= dt
            f.x -= dt * (if (f.kind == 2) 9f else if (sailing()) 7f else 4f)
        }
        floaters.removeAll { it.t <= 0f || it.x < -15f }
    }

    private fun drawFloaters(time: Float) {
        for (f in floaters) {
            val bob = sin(time * 2.5f + f.x) * 1.2f
            val glow = (floor(time * 3) % 2).toInt() == 0
            val fxi = when (f.kind) { 0 -> 7; 1 -> 8; else -> if ((floor(time * 5) % 2).toInt() == 0) 9 else 10 }
            val fb = acell("fx", 6, 2, fxi, if (f.kind == 1) 16 else 14)
            if (fb != null) {
                Art.draw(c, fb, f.x, f.y + 3 + (if (f.kind == 2) 0f else bob), f.kind == 2)
                if (glow && f.kind != 2) acell("fx", 6, 2, 6, 7)?.let { Art.draw(c, it, f.x + 5, f.y - 8 + bob) }
                continue
            }
            when (f.kind) {
                0 -> {   // 유리병 + 반짝임
                    r(f.x - 5, f.y - 5 + bob, 9f, 5f, col("#7cc8b4")); r(f.x + 4, f.y - 4 + bob, 3f, 2f, col("#c8a050"))
                    r(f.x - 4, f.y - 5 + bob, 6f, 1f, col("#e8fff6")); r(f.x - 3, f.y - 3 + bob, 4f, 2f, col("#f4ead2"))
                    if (glow) r(f.x, f.y - 8 + bob, 1f, 3f, col("#fff6c0")).also { r(f.x - 1, f.y - 7 + bob, 3f, 1f, col("#fff6c0")) }
                }
                1 -> {   // 보물상자
                    r(f.x - 5, f.y - 6 + bob, 10f, 6f, col("#9a6236")); r(f.x - 5, f.y - 7 + bob, 10f, 2f, col("#6b4022"))
                    r(f.x - 1, f.y - 5 + bob, 2f, 2f, col("#ffd36a")); r(f.x - 5, f.y - 4 + bob, 10f, 1f, col("#c8a050"))
                    if (glow) { r(f.x - 7, f.y - 11 + bob, 1f, 1f, col("#fff6c0")); r(f.x + 6, f.y - 12 + bob, 1f, 1f, col("#fff6c0")); r(f.x, f.y - 14 + bob, 1f, 1f, col("#fff6c0")) }
                }
                else -> {  // 갈매기가 나뭇가지를 물고 날아간다
                    val up = (floor(time * 5) % 2).toInt() == 0
                    r(f.x - 2, f.y, 5f, 2f, Color.WHITE); r(f.x - 3, f.y + 1, 1f, 1f, col("#ffb347"))
                    if (up) { r(f.x - 1, f.y - 3, 1f, 3f, Color.WHITE); r(f.x + 2, f.y - 3, 1f, 3f, Color.WHITE) } else { r(f.x - 4, f.y + 2, 3f, 1f, Color.WHITE); r(f.x + 3, f.y + 2, 3f, 1f, Color.WHITE) }
                    r(f.x - 5, f.y + 2, 4f, 1f, col("#a8703f"))
                }
            }
        }
    }

    class Pop(var x: Float, var y: Float, val text: String, var t: Float, val gold: Boolean = false)
    val pops = ArrayList<Pop>()

    private fun walk(v: Person, x: Float, dt: Float): Boolean {
        val dx = x - v.x; val sp = 13f * dt
        if (abs(dx) <= sp) { v.x = x; return true }
        v.x += if (dx > 0) sp else -sp
        v.dir = if (dx < 0) -1f else 1f
        v.step += dt * 9
        return false
    }

    private fun updatePeople(dt: Float) {
        for (t in trees) { if (t.hp <= 0) { t.regrow -= dt; if (t.regrow <= 0) t.hp = 3 }; if (t.shake > 0) t.shake -= dt }
        for (v in people) {
            when (v.st) {
                0 -> { v.t -= dt; if (v.t <= 0) {
                    val live = trees.filter { it.hp > 0 }
                    if (Random.nextFloat() < .68f && live.isNotEmpty()) {
                        val t = live[Random.nextInt(live.size)]; v.tree = t; v.tx = t.x + (if (t.x < 100) 7f else -7f)
                    } else { v.tree = null; val rk = rocks[Random.nextInt(rocks.size)]; v.tx = rk.x + (if (rk.x < 100) 6f else -6f) }
                    v.st = 1
                } }
                1 -> if (walk(v, v.tx, dt)) {
                    val t = v.tree
                    if (t != null && t.hp <= 0) { v.st = 0; v.t = .3f }
                    else { v.st = 2; v.t = 2.4f; v.dir = if ((t?.x ?: v.tx) > v.x) 1f else -1f
                        v.dir = if (t != null) (if (t.x > v.x) 1f else -1f) else (if (v.tx < 100) -1f else 1f) }
                }
                2 -> {
                    v.t -= dt
                    v.tree?.let { if ((v.t * 2.5f) % 1f < .1f) it.shake = .15f }
                    if (v.t <= 0) {
                        v.tree?.let { if (it.hp > 0) { it.hp--; if (it.hp <= 0) it.regrow = 40f } }
                        v.carry = if (v.tree != null) 1 else 2
                        v.st = 3
                    }
                }
                3 -> if (walk(v, storeX + (if (v.x < storeX) -8f else 8f), dt)) {
                    if (pops.size < 8) pops.add(Pop(storeX, GROUND - 26, if (v.carry == 1) "+나무" else "+돌", 1.6f))
                    v.carry = 0; v.st = 0; v.t = .3f + Random.nextFloat() * 1.2f
                }
            }
        }
    }

    // ---------- 말풍선 ----------
    val NAMES = listOf("단", "보리", "해온")
    private var talkWait = 6f
    var talkText: String? = null; private set
    var talkWho = 0; private set
    var talkT = 0f; private set

    /** 지금 말하는 사람 이름 (주민 또는 선원) */
    var talkName = ""; private set
    private var talkCrew: String? = null                  // 선원이 말하면 그 id
    private val crewHead = HashMap<String, Pair<Float, Float>>()   // 이번 장면에서 그린 선원 머리 위치
    private var crewSeen = 0f                             // 선원이 화면에 보인 지 몇 초
    private val pairQueue = ArrayDeque<Pair<String, String>>()     // 짝 대사 남은 줄 (선원 id, 대사)
    private var pairIds: List<String> = emptyList()

    private fun say(id: String, line: String) {
        talkCrew = id; talkName = Fleet.crewOf(id)?.name ?: ""; talkText = line; talkT = 4f
    }

    /** 항해 중 갑판 위 선원이 한마디. 싸울 땐 싸움 대사, 짝이 같이 타면 가끔 셋이 주고받는 대사 */
    private fun crewTalk(): Boolean {
        val v = Game.voy ?: return false
        if (v.crew.isEmpty() || crewSeen > .3f) return false
        val u = pirateU()
        val fight = u >= 0f && u < pA / (pA + .035f)
        if (fight) {
            Fleet.boss(v.pirate)?.let { b -> v.crew.firstOrNull { it in b.crewLines }?.let { id -> if (Random.nextBoolean()) { say(id, b.crewLines[id]!!); return true } } }
            val id = v.crew.random(); val l = voiceOf(id, true) ?: return false
            say(id, l); return true
        }
        val pr = Fleet.pairs.filter { p -> p.ids.all { it in v.crew && Game.crewLevel(it) >= p.minLv } }
        if (pr.isNotEmpty() && Random.nextInt(3) == 0) {
            val p = pr.random()
            pairIds = p.ids; pairQueue.clear()
            for (k in p.lines.indices) pairQueue.addLast((p.speakers.getOrNull(k) ?: p.ids[0]) to p.lines[k])
            val (id, l) = pairQueue.removeFirst(); say(id, l); return true
        }
        // 특별 항해 중이면 주인공 선원이 임무 대사를 자주 한다
        // 날씨와 계절 한마디
        if (Random.nextInt(4) == 0) Game.weather()?.let { w -> val id = v.crew.random(); (Fleet.weatherLines[id]?.get(w.id) ?: w.crewLine).takeIf { it.isNotEmpty() }?.let { say(id, it); return true } }
        if (Random.nextInt(6) == 0) { val id = v.crew.random(); Game.season()?.crewLines?.get(id)?.let { say(id, it); return true } }
        if (Random.nextInt(7) == 0) { val id = v.crew.random(); Fleet.seabedLines[id]?.randomOrNull()?.let { say(id, it); return true } }
        Fleet.mission(v.mission)?.let { m -> if (m.crew in v.crew && m.homeLine.isNotEmpty() && Random.nextBoolean()) { say(m.crew, m.homeLine); return true } }
        val id = v.crew.random(); val l = voiceOf(id, false) ?: return false
        say(id, l); return true
    }

    private fun updateTalk(dt: Float) {
        crewSeen += dt
        if (talkT > 0f) { talkT -= dt; if (talkT <= 0f) { talkText = null; talkCrew = null } }
        // 짝 대사는 세 줄을 이어서. 도중에 배치가 바뀌면 그만
        if (talkT <= 0f && pairQueue.isNotEmpty()) {
            val crew = Game.voy?.crew ?: emptyList()
            if (crewSeen > .3f || !pairIds.all { it in crew }) pairQueue.clear()
            else { val (id, l) = pairQueue.removeFirst(); say(id, l); talkT = 3.5f; return }
        }
        talkWait -= dt
        if (talkWait > 0f) return
        val island = people.size >= 3 && islandOffset() != null
        if (crewSeen <= .3f && (!island || Random.nextBoolean())) {
            if (crewTalk()) { talkWait = 18f + Random.nextFloat() * 20f; return }
        }
        if (!island) return
        talkWait = 25f + Random.nextFloat() * 30f
        val h = hourNow()
        val now = System.currentTimeMillis()
        val time = when {
            Game.lastBattleAt > now - 60_000L -> Game.lastBattle
            Game.lastReturnAt > now - 60_000L -> "ship_return"
            Game.wood >= Game.cap() && Random.nextInt(3) == 0 -> "storage_full"
            Game.voyT() >= 0f && Random.nextBoolean() -> "ship_wait"
            h >= 6f && h < 17f -> "day"
            h >= 17f && h < 19f -> "dusk"
            else -> "night"
        }
        val who = Random.nextInt(3)
        val sl = Game.season()?.islandLine?.takeIf { it.isNotEmpty() && Random.nextInt(5) == 0 }
        val line = sl ?: Story.pickLine(NAMES[who], time) ?: Story.pickLine(NAMES[who], "day") ?: return
        talkWho = who; talkCrew = null; talkName = NAMES[who]; talkText = line; talkT = 5f
    }

    /** 말하는 주민의 머리 위치 (그림 좌표). 섬이 화면에 없으면 null */
    fun talkPos(): Pair<Float, Float>? {
        talkCrew?.let { id -> return if (crewSeen > .3f) null else crewHead[id] }
        val o = islandOffset() ?: return null
        val v = people.getOrNull(talkWho) ?: return null
        val x = v.x + o
        if (x < 0 || x > W) return null
        return x to (GROUND - 17)
    }

    // =====================================================================
    // 항해 장면: 지금 화면에 무엇이 어디 있는지

    private var scroll = 0f         // 바다가 흘러간 거리
    private var lastF = -1f

    /** 섬이 화면에서 얼마나 밀려 있는지. 안 보이면 null */
    private fun islandOffset(): Float? {
        val f = Game.voyFrac()
        if (f < 0f) return 0f
        val dep = .05f; val ret = .95f
        return when {
            f < dep -> -SHIFT * ease(f / dep)
            f > ret -> SHIFT * (1 - ease((f - ret) / (1 - ret)))
            else -> null
        }
    }

    private fun portOffset(): Float? {
        val f = Game.voyFrac()
        if (f < 0f) return null
        val a0 = Data.fArrive - .05f
        return when {
            f in a0..Data.fArrive -> SHIFT * (1 - ease((f - a0) / .05f))
            f in Data.fArrive..Data.fLeave -> 0f
            f in Data.fLeave..(Data.fLeave + .05f) -> -SHIFT * ease((f - Data.fLeave) / .05f)
            else -> null
        }
    }

    /** 배 그림 폭. 섬 부두에 묶여 있을 때는 마을을 가리지 않게 작게 (멀리 정박한 느낌), 떠나면서 원래 크기로 */
    private fun shipW(ff: Float = Game.voyFrac()): Int {
        val lv = Game.shipLevel; val full = if (lv <= 6) 54 + (lv - 1) * 8 else 94 + (lv - 6) * 4
        val dw = min(full, DOCK_W)
        return when { ff < 0f -> dw; ff < .05f -> lerp(dw.toFloat(), full.toFloat(), ease(ff / .05f)).roundToInt()
            ff > .95f -> lerp(full.toFloat(), dw.toFloat(), ease((ff - .95f) / .05f)).roundToInt(); else -> full }
    }
    private val DOCK_W = 66
    private data class Quad(val a: Int, val b: Int, val c: Float, val d: Int)
    private fun dockedBehind() = Game.dock && Game.voyFrac() < 0f && islandOffset() == 0f

    /** 우리 배의 화면 x. 부두가 없으면 null */
    private fun shipX(): Float? {
        if (!Game.dock) return null
        val f = Game.voyFrac()
        // 큰 배도 화면 밖으로 잘리지 않게 부두 자리를 왼쪽으로 당긴다
        val sw = shipW(-1f)
        val hx = min(dockX + 16, W - sw / 2f + 2)
        if (f < 0f) return hx
        return when {
            f < .05f -> lerp(hx, SAIL_X, ease(f / .05f))
            f > .95f -> lerp(SAIL_X, hx, ease((f - .95f) / .05f))
            else -> SAIL_X
        }
    }

    /** 바다가 흘러가는 중인가 (항구에 묶여 있거나 섬에 있으면 멈춘다) */
    private fun sailing(): Boolean {
        val f = Game.voyFrac()
        if (f < 0f) return false
        return !(f in Data.fArrive..Data.fLeave)
    }

    // 해적: 다가옴 → 포격 → (이기면) 가라앉음 / (지면) 짐을 들고 달아남
    private val pA = .055f; private val pB = .02f
    private fun pirateU(): Float {
        val v = Game.voy ?: return -1f
        if (!v.encounter) return -1f
        val f = Game.voyFrac()
        val s = Data.fPirate - pA
        return if (f in s..(Data.fPirate + .035f)) (f - s) / (pA + .035f) else -1f
    }

    fun update(dt: Float) {
        Game.tick(dt)
        syncPeople()
        updatePeople(dt)
        updateTalk(dt)
        updateFloaters(dt)
        updateCritter(dt)
        updateSeabed(dt)
        if (sailing()) scroll += dt * 16f
        if (Game.levelUps > 0) { pops.add(Pop(dockX - 10, GROUND - 30, "선원 레벨 업!", 3.5f, true)); Game.levelUps = 0 }
        if (Game.festLeft() > 0 && islandOffset() == 0f && pops.none { it.text == "귀항 잔치!" }) pops.add(Pop(dockX - 40, GROUND - 34, "귀항 잔치!", 3f, true))
        if (Game.arrived > 0) { pops.add(Pop(dockX, GROUND - 24, "견습선원 도착", 3f)); Game.arrived = 0 }
        if (Game.supplies > 0) { if (islandOffset() == 0f) pops.add(Pop(dockX + 10, WL - 22, "짐배 +짐", 2.5f)); Game.supplies = 0 }
        // 해적 결과, 항구 판매 순간에 숫자를 띄운다
        val f = Game.voyFrac()
        val v = Game.voy
        if (v != null && lastF >= 0f) {
            if (v.encounter && lastF < Data.fPirate - .05f && f >= Data.fPirate - .05f) Data.pirate(v.pirate ?: "")?.let { pr ->
                pops.add(Pop(100f, WL - 50, "힘 " + "%,d".format(Game.myPower(v.crew)) + " vs " + "%,d".format(Game.enemyPower(pr)), 3.5f, true)) }
            if (v.encounter && lastF < Data.fPirate && f >= Data.fPirate)
                pops.add(if (v.win) Pop(150f, WL - 34, "승리! 보물", 3f, true) else Pop(SAIL_X, WL - 34, "짐을 빼앗겼어요", 3f))
            if (lastF < Data.fArrive && f >= Data.fArrive) pops.add(Pop(SAIL_X + 30, WL - 40, "판매 +" + gold(v.saleC / 100) + "금화", 3.5f, true))
        }
        lastF = f
        for (pp in pops) { pp.t -= dt; pp.y -= dt * 5 }
        pops.removeAll { it.t <= 0f }
    }

    // =====================================================================
    // 그리기

    fun draw(time: Float) {
        val h = hourNow()
        palette(h)
        ga = 1f
        if (!artBackground()) {
            sky(time, h)
            farIslands()
            seaBody(time)
        }
        skyWeather(time)
        drawSeabed(time)
        birds(time)
        val io = islandOffset()
        val po = portOffset()
        // 섬·항구가 보이면 물결은 그 뒤에 (섬 풀밭을 물이 덮지 않게), 넓은 바다에서는 배 앞에
        landWater = (io != null || po != null) && surfInit && artSurface(time)
        if (io != null) island(time, io)
        if (po != null) port(time, po)
        drawCritter()
        pirates(time)
        drawVisitor(time, io, po)
        if (!dockedBehind()) shipX()?.let { ourShip(it, time) }
        drawFloaters(time)
        foreWater(time)
        weatherFx(time)
        if (dark > 0f) {
            p.color = Color.argb((dark * (if (Art.has("bg_sea_night")) .3f else .6f) * 255).roundToInt(), 8, 12, 40)
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
            ga = min(1f, dark * 1.6f); nightLights(time, io, po); ga = 1f
        }
        if (wantBg) {
            c = bgC
            if (!artBackground()) { sky(time, h); farIslands(); seaBody(time) }
            skyWeather(time); if (landWater) artSurface(time) else foreWater(time)
            if (dark > 0f) { p.color = Color.argb((dark * (if (Art.has("bg_sea_night")) .3f else .6f) * 255).roundToInt(), 8, 12, 40); c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p) }
            c = mainC
        }
    }

    // ---------- 14차: 바닷속 (모래 바닥, 정원, 거품, 물고기, 진주 조개) ----------
    private val FLOOR = 386f          // 바닥 띠 아래 끝 (배경 그림의 바닥 위를 덮는다)
    private val SAND = 362f           // 해초·산호 밑동 높이
    private class Swimmer(val kind: Int, var x: Float, var t: Float)
    private var swimmer: Swimmer? = null
    private var swimWait = 12f + Random.nextFloat() * 20f
    private var schoolX = -99f; private var schoolWait = 6f
    private val bubbles = ArrayList<FloatArray>()          // x, y, t
    private var bubbleWait = 1f
    private var clamX: Float? = null
    private val WEED_X = floatArrayOf(18f, 92f, 168f, 246f, 318f, 372f)
    private val CORAL_X = floatArrayOf(52f, 132f, 206f, 282f, 344f, 392f)
    private fun seaX(base: Float): Float { val v = ((base - scroll) % 400f + 400f) % 400f; return v - 100f }
    private fun swimY(sw: Swimmer): Float = when (sw.kind) { 8, 10 -> SAND - 7; 9 -> 300f; 11 -> 318f + sin(sw.t * 1.5f) * 4f; 12 -> 292f; else -> 328f }
    fun spawnSwimmer(kind: Int, x: Float) { swimmer = Swimmer(kind, x, 0f); schoolX = 150f }
    private fun updateSeabed(dt: Float) {
        val drift = if (sailing()) dt * 16f else 0f
        swimmer?.let { sw -> sw.t += dt
            sw.x -= when (sw.kind) { 8 -> 3f * dt + drift; 10 -> drift; 9 -> 10f * dt; 11 -> 4f * dt; 12 -> 6f * dt; else -> 9f * dt }
            if (sw.x < -30f) swimmer = null }
        if (swimmer == null) { swimWait -= dt; if (swimWait <= 0f && Fleet.creatures.any { it.kind >= 8 }) {
            swimWait = (35f + Random.nextFloat() * 60f) / Game.tideV("critter_more", 1f); swimmer = Swimmer(8 + Random.nextInt(6), W + 20f, 0f) } }
        if (schoolX > -40f) schoolX -= dt * 14f else { schoolWait -= dt; if (schoolWait <= 0f) { schoolWait = 25f + Random.nextFloat() * 40f; schoolX = W + 30f } }
        bubbleWait -= dt
        if (bubbleWait <= 0f) { bubbleWait = 1.2f + Random.nextFloat() * 2f
            val n = Fleet.gardenAt(Game.voyages)?.seaweed ?: 2
            val bx = seaX(WEED_X[Random.nextInt(n.coerceIn(1, WEED_X.size))])
            if (bx in 0f..W.toFloat()) bubbles.add(floatArrayOf(bx, SAND - 24, 0f)) }
        for (b in bubbles) { b[2] += dt; b[1] -= dt * 11f; b[0] += sin(b[2] * 3f) * dt * 3f - drift }
        bubbles.removeAll { it[1] < WL + 8 || it[0] < -10 }
    }
    private fun drawSeabed(time: Float) {
        if (!Art.has("seabed")) return
        // 낮에는 빛줄기
        if (dark < .5f) acell("light_rays", 4, 1, (floor(time * 1.2f) % 4).toInt(), 70)?.let { bm ->
            val a = ((1 - dark * 2) * 150).roundToInt()
            for (k in 0..1) Art.draw(c, bm, ((60f + k * 110f - scroll * .3f) % 260f + 260f) % 260f - 30f, WL + 140, alpha = a) }
        // 모래 바닥 (배가 가면 흘러간다) + 그 아래는 모래색
        gp.shader = LinearGradient(0f, FLOOR - 1, 0f, H.toFloat(), col("#e9cf8c"), col("#a8925c"), Shader.TileMode.CLAMP)
        c.drawRect(0f, FLOOR - 1, W.toFloat(), H.toFloat(), gp); gp.shader = null
        aget("seabed", 400)?.let { bm -> val x0 = -(((scroll % 400f) + 400f) % 400f); Art.draw(c, bm, x0 + 200f, FLOOR); Art.draw(c, bm, x0 + 600f, FLOOR) }
        p.color = Color.argb(70, 10, 70, 130); c.drawRect(0f, SAND - 14, W.toFloat(), H.toFloat(), p)   // 물속이라 살짝 푸르게
        val g = Fleet.gardenAt(Game.voyages)
        for (i in 0 until (g?.seaweed ?: 2).coerceAtMost(WEED_X.size)) { val x = seaX(WEED_X[i]); if (x < -20 || x > W + 20) continue
            acell("coral_sway", 4, 4, (i % 2) * 4 + (floor(time * 2f + i * .7f) % 4).toInt(), 40)?.let { Art.draw(c, it, x, SAND + 1.4f) } }
        for (i in 0 until (g?.coral ?: 2).coerceAtMost(CORAL_X.size)) { val x = seaX(CORAL_X[i]); if (x < -20 || x > W + 20) continue
            acell("coral_sway", 4, 4, (2 + i % 2) * 4 + (floor(time * 1.5f + i * .9f) % 4).toInt(), 34)?.let { Art.draw(c, it, x, SAND + 1.2f) } }
        // 진주 조개
        val cx = seaX(228f); clamX = if (cx in 0f..W.toFloat()) cx else null
        if (clamX != null) acell("seabed_life", 4, 6, 20 + if (Game.pearlOpen()) 2 else if ((floor(time * .5f) % 6).toInt() == 0) 1 else 0, 22)?.let { Art.draw(c, it, cx, SAND + 1f) }
        // 노란 물고기 떼, 바닷속 친구, 거품
        if (schoolX > -40f) acell("seabed_life", 4, 6, 4 + (floor(time * 4f) % 4).toInt(), 36)?.let { Art.draw(c, it, schoolX, 300f) }
        swimmer?.let { sw ->
            val (sheet, rows, row) = when (sw.kind) { 8 -> Triple("seabed_life", 6, 2); 9 -> Triple("seabed_life", 6, 3); 10 -> Triple("seabed_life_2", 3, 0); 11 -> Triple("seabed_life", 6, 4); 12 -> Triple("seabed_life_2", 3, 1); else -> Triple("seabed_life_2", 3, 2) }
            val sz = if (sw.kind == 13) 34 else 26
            val fr = if (sw.kind == 10) (floor(sw.t * 1.5f) % 4).toInt() else (floor(sw.t * 4f) % 4).toInt()
            acell(sheet, 4, rows, row * 4 + fr, sz)?.let { val y = swimY(sw); Art.draw(c, it, sw.x, if (sw.kind == 8 || sw.kind == 10) SAND + 1f else y + sz / 2f) } }
        for (b in bubbles) acell("seabed_life", 4, 6, (floor(b[2] * 3f) % 4).toInt(), 12)?.let { Art.draw(c, it, b[0], b[1] + 6f) }
    }

    // ---------- 12차: 날씨와 손님 배 ----------
    private fun skyWeather(time: Float) {
        val w = Game.weather() ?: return
        val i = when (w.layer) { "stars" -> 0; "sunrays" -> 1; "aurora" -> 2; "rainbow" -> 3; else -> return }
        when (i) {
            0 -> { acell("weather_sky", 4, 4, 0, 40)?.let { Art.draw(c, it, 34f, HOR - 26, alpha = if ((floor(time * 2) % 2).toInt() == 0) 255 else 200) }
                   acell("weather_sky", 4, 4, 0, 34)?.let { Art.draw(c, it, 172f, HOR - 34, true) } }
            1 -> acell("weather_sky", 4, 4, 1, 64)?.let { Art.draw(c, it, 156f, HOR - 6) }
            2 -> acell("weather_sky", 4, 4, 2, 84)?.let { Art.draw(c, it, 126f, HOR + 2, alpha = 230) }
            else -> acell("weather_sky", 4, 4, 3, 84)?.let { Art.draw(c, it, 150f, HOR + 10, alpha = 220) }
        }
    }
    private fun weatherFx(time: Float) {
        val w = Game.weather() ?: return
        val row = when (w.layer) { "rain" -> 0; "snow" -> 1; "petals" -> 2; "leaves" -> 3; "fog" -> 4; else -> return }
        val fr = (floor(time * (if (row == 0) 8f else 3f)) % 4).toInt()
        if (row == 4) {   // 안개: 큰 덩어리가 천천히 흘러간다
            val bm = acell("weather_fx", 4, 5, 16 + fr % 2, 110) ?: return
            for (k in 0 until 4) { val x = ((k * 70f - time * 3f) % 280f + 280f) % 280f - 40f; Art.draw(c, bm, x, WL - 6 + (k % 2) * 14f, alpha = 200) }
            return
        }
        val ts = 64f
        val bm = acell("weather_fx", 4, 5, row * 4 + fr, ts.toInt()) ?: return
        val vy = when (row) { 0 -> 90f; 1 -> 14f; 2 -> 12f; else -> 16f }; val vx = when (row) { 0 -> 30f; 1 -> 4f; else -> 10f }
        val ox = ((time * vx) % ts); val oy = ((time * vy) % ts)
        var y = HOR - 120 + oy - ts
        c.save(); c.clipRect(0f, 0f, W.toFloat(), WL + 3)     // 물속에는 내리지 않게
        while (y < WL) { var x = -ox; while (x < W + ts) { Art.draw(c, bm, x + ts / 2, y + ts); x += ts }; y += ts }
        c.restore()
    }
    private var visX: Float? = null
    private var visY = WL
    private fun drawVisitor(time: Float, io: Float?, po: Float?) {
        visX = null
        val v = Game.visitor() ?: return
        if (po != null || pirateU() >= 0f) return
        val home = io != null
        val x = if (home) 30f + io!! else 150f
        if (x < -20 || x > W + 20) return
        val bm = acell("visitor_boats", 4, 2, v.sprite, 46) ?: return
        val by = if (home) WL + 7 else WL + 3          // 섬에 있을 땐 섬 앞 물에 떠 있다
        Art.draw(c, bm, x, by + sin(time * 2f) * 1f)
        visX = x; visY = by
        if (!Game.visDone && (floor(time * 2) % 2).toInt() == 0) acell("fx", 6, 2, 6, 8)?.let { Art.draw(c, it, x + 14, by - 25) }
    }

    /** 그림 파일 bg_sea가 있으면 하늘·바다를 그 그림으로 (가로로 이어 붙여 흘러가게) */
    private fun artBackground(): Boolean {
        if (!Art.has("bg_sea")) return false
        surfInit = false
        val h = hourNow()
        bgLayer("bg_sea", 255)
        // 저녁놀(17:30~19:00, 새벽 5:00~6:30)과 밤
        val dusk = when {
            h in 17.5f..19f -> (h - 17.5f) / 1.5f
            h in 19f..20f -> 1f
            h in 4.5f..5.5f -> 1f
            h in 5.5f..6.5f -> 1 - (h - 5.5f)
            else -> 0f
        }
        val night = when { h >= 20f || h < 4f -> 1f; h in 19f..20f -> h - 19f; h in 4f..4.5f -> 1 - (h - 4f) * 2; else -> 0f }
        bgDusk = dusk; bgNight = night
        if (dusk > 0f) bgLayer("bg_sea_dusk", (dusk * 255).toInt())
        if (night > 0f) bgLayer("bg_sea_night", (night * 255).toInt())
        if (stormy()) bgLayer("bg_storm", 230)
        return true
    }

    /** 가끔 낮에 비바람이 분다 (3시간마다 열 번에 한 번쯤) */
    fun stormy(): Boolean {
        val block = System.currentTimeMillis() / (3 * 3600_000L)
        val h = hourNow()
        return h in 7f..18f && ((block * 2654435761L) ushr 7) % 10 == 3L && Art.has("bg_storm")
    }

    private fun bgLayer(name: String, alpha: Int) {
        val bg = aget(name, 360) ?: return
        val a = alpha.coerceIn(0, 255)
        // 그림의 수평선(높이 48%)을 화면 수평선에 맞추고, 위아래 남는 곳은 그림 맨 위·맨 아래 색으로 채운다
        // 그림의 수면(높이 정확히 50%)을 배·항구가 떠 있는 물선(WL)에 맞춘다
        val bw = bg.width / SS.toFloat(); val bh = bg.height / SS.toFloat()
        val top = WL - bh * .5f
        val bottom = top + bh
        // 채우는 칸과 그림이 겹치면 반투명(저녁·비) 층에서 겹친 곳만 두 번 칠해져 하늘에 가로줄이 생긴다.
        // 그래서 겹치지 않게 그림의 위·아래 끝에서 정확히 멈춘다.
        p.color = bg.getPixel(bg.width / 2, 1) or (0xff shl 24); p.alpha = a; c.drawRect(0f, 0f, W.toFloat(), top, p)
        p.color = bg.getPixel(bg.width / 2, bg.height - 2) or (0xff shl 24); p.alpha = a; c.drawRect(0f, bottom, W.toFloat(), H.toFloat(), p)
        p.alpha = 255
        var x = -((scroll * .5f) % bw)
        while (x < W) { Art.draw(c, bg, x + bw / 2f, bottom, false, a); x += bw }
        // 물결용: 그림의 수면 띠(청록) 색을 층마다 섞어 둔다
        // 하얀 거품을 피해서, 여러 곳 중 가장 덜 하얀 수면 색
        val sy = (bg.height / 2 + SS * 1.2f).toInt().coerceAtMost(bg.height - 1)
        val sc = (1..9).map { bg.getPixel(bg.width * it / 10, sy) }.minByOrNull { Color.red(it) + Color.green(it) + Color.blue(it) }!! or (0xff shl 24)
        val dc = bg.getPixel(bg.width / 3, (bg.height / 2 + SS * 6).coerceAtMost(bg.height - 1)) or (0xff shl 24)
        deepC = if (!surfInit) dc else mix(deepC, dc, a / 255f)
        surfC = if (!surfInit) sc else mix(surfC, sc, a / 255f); surfInit = true
    }
    private var surfC = 0
    private var bgDusk = 0f; private var bgNight = 0f
    private var landWater = false
    /** 아스트라 수면 띠 (19차): 물결 4장면을 옆으로 이어 붙여 흘려 보낸다. 가장 낮은 골(띠 y76/128)이 수면 WL */
    private fun artSurface(time: Float): Boolean {
        if (!Art.has("sea_surface")) return false
        val wU = 144                                   // 띠 한 장(1024px)의 화면 폭
        val u = wU / 1024f
        val fr = ((time * 5).toInt() % 4 + 4) % 4
        val bottom = WL + (128 - 76) * u
        fun layer(name: String, a: Int) {
            if (a <= 0) return
            val bm = acell(name, 1, 4, fr, wU) ?: acell("sea_surface", 1, 4, fr, wU) ?: return
            var x = -((scroll * .5f) % wU)
            while (x < W) { Art.draw(c, bm, x + wU / 2f, bottom, false, a); x += wU }
        }
        layer("sea_surface", 255)
        layer("sea_surface_dusk", (bgDusk * 255).toInt())
        layer("sea_surface_night", (bgNight * 255).toInt())
        if (stormy()) layer("sea_surface_night", 140)
        return true
    }
    private var deepC = 0
    private var surfInit = false

    /** 수면을 일자 대신 출렁이는 물결로: 그림의 곧은 수면 띠를 물결 모양으로 덮고, 물마루에 하얀 거품 */
    private fun waveSurface(time: Float) {
        if (!surfInit) return
        val sc = scroll; val band = 2.6f
        var x = 0f
        while (x < W) {
            val w = sin(x * .09f + time * 1.8f + sc * .05f) * 1.3f + sin(x * .21f - time * 2.6f + sc * .08f) * .7f
            val crest = ((WL - 2f + w) * SS).roundToInt() / SS.toFloat()
            r(x, crest, 1f, WL + band - crest, surfC)
            if (w < -1.2f) { ga = .9f; r(x, crest, 1f, 1f / SS * 2, Color.WHITE); ga = 1f }
            x += 1f
        }
        // 배 밑바닥이 물에 잠겨 보이게 수면 아래를 살짝 덮는다
        ga = .5f; r(0f, WL + band, W.toFloat(), 3f, deepC); ga = 1f
    }

    private fun sky(time: Float, h: Float) {
        gp.shader = LinearGradient(0f, 60f, 0f, HOR, skyTop, skyBot, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), HOR, gp); gp.shader = null
        if (dark > .3f) {
            ga = (dark - .3f) * 2
            for (i in 0 until 90) {
                val x = (i * 73) % W; val y = (i * 37) % HOR.toInt()
                if ((i + floor(time * 1.5f).toInt()) % 11 > 1) r(x.toFloat(), y.toFloat(), 1f, 1f, Color.WHITE)
            }
            ga = 1f
        }
        if (h in 5.6f..19.4f) {
            val t = (h - 5.6f) / 13.8f
            ell(14 + t * 172, HOR - 20 - sin(t * PI.toFloat()) * 70, 7f, 7f, if (h < 7 || h > 17.8f) col("#ffb070") else col("#fff2b0"))
        } else {
            val hh = if (h < 12) h + 24 else h
            val t = (hh - 19.4f) / 10.2f
            val x = 14 + t * 172; val y = HOR - 20 - sin(t * PI.toFloat()) * 70
            ell(x, y, 6f, 6f, col("#e8ecff")); ell(x + 2, y - 1, 5f, 5f, skyTop)
        }
        // 구름: 항해 중엔 조금 빨리 흘러간다
        for (i in 0 until 7) {
            val sp = .5f + (i % 3) * .3f
            val x = ((i * 53 + time * 1.5f * sp + scroll * .15f * sp) % (W + 60)).let { W + 30 - it }
            val y = 70f + (i * 23) % 120
            ga = .85f - dark * .5f
            ell(x, y, 13f, 3f, Color.WHITE); ell(x + 8, y - 2, 8f, 3f, Color.WHITE); ell(x - 6, y + 1, 6f, 2f, Color.WHITE)
            ga = 1f
        }
    }

    /** 수평선 위 먼 섬들 (천천히 흘러간다) */
    private fun farIslands() {
        val cl = mix(sea, skyBot, .55f)
        for (i in 0 until 4) {
            val span = 300f
            val x = ((i * 83f - scroll * .25f) % span + span) % span - 50f
            val w = 18f + (i * 11) % 22; val hh = 4f + (i * 5) % 7
            ell(x, HOR, w, hh, cl)
        }
    }

    private fun seaBody(time: Float) {
        val deep = mix(sea, col("#06101e"), .85f)
        gp.shader = LinearGradient(0f, HOR, 0f, H.toFloat(), sea, deep, Shader.TileMode.CLAMP)
        c.drawRect(0f, HOR, W.toFloat(), H.toFloat(), gp); gp.shader = null
        // 물결 줄: 가까울수록 빨리 흘러간다
        var y = HOR + 2; var k = 0
        while (y < WL + 40) {
            val sp = .2f + (y - HOR) / 40f
            val off = (scroll * sp + time * 3) % 26
            ga = .25f
            var x = -off + (k * 9) % 26 - 26
            while (x < W) { r(x, y, 6f, 1f, Color.WHITE); x += 26 }
            ga = 1f
            y += 5; k++
        }
        // 깊은 바다: 빛줄기와 거품
        ga = .08f
        for (i in 0 until 5) {
            val x = ((i * 47 - scroll * .4f) % 230 + 230) % 230 - 15
            r(x, WL + 20, 6f, 150f, Color.WHITE)
        }
        ga = .35f
        for (i in 0 until 14) {
            val x = ((i * 31 - scroll * .6f) % 210 + 210) % 210 - 5
            val yy = H - ((time * (6 + i % 4) + i * 37) % (H - WL - 30))
            r(x + sin(time + i) * 2, yy, 1f, 1f, Color.WHITE)
        }
        ga = 1f
    }

    /** 갈매기와 뛰어오르는 물고기: 바다가 비어 보이지 않게 */
    private fun birds(time: Float) {
        val cl = if (dark > .4f) col("#c8cce0") else col("#ffffff")
        for (i in 0 until 3) {
            val x = W + 20 - ((time * (7f + i * 2) + i * 90 + scroll * .3f) % (W + 40))
            val y = HOR - 60 - i * 22 + sin(time * .7f + i) * 4
            val up = (floor(time * 4 + i) % 2).toInt() == 0
            r(x, y, 1f, 1f, cl)
            if (up) { r(x - 2, y - 1, 2f, 1f, cl); r(x + 1, y - 1, 2f, 1f, cl); r(x - 3, y - 2, 1f, 1f, cl); r(x + 3, y - 2, 1f, 1f, cl) }
            else { r(x - 3, y, 3f, 1f, cl); r(x + 1, y, 3f, 1f, cl) }
        }
        if (sailing()) {
            val t = (time * .35f) % 1f
            if (t < .25f) {
                val u = t / .25f
                val fx = 150f - u * 20; val fy = WL + 2 - sin(u * PI.toFloat()) * 10
                r(fx, fy, 3f, 2f, col("#9fc4d8")); r(fx + 3, fy - 1, 1f, 1f, col("#9fc4d8"))
                if (u < .15f || u > .85f) { ga = .7f; ell(fx + 1, WL + 2, 3f, 1f, Color.WHITE); ga = 1f }
            }
        }
    }

    /** 배 앞쪽 물: 배가 물에 잠겨 보이게 띠를 덮는다 */
    private fun foreWater(time: Float) {
        if (surfInit) { if (landWater) return; if (!artSurface(time)) waveSurface(time); return }   // 그림 배경이면 물결이 수면을 맡는다
        ga = .55f
        r(0f, WL + 3, W.toFloat(), 3f, sea)
        ga = .3f
        val off = (scroll * 1.2f + time * 4) % 18
        var x = -off
        while (x < W) { r(x, WL + 3, 8f, 1f, Color.WHITE); x += 18 }
        ga = 1f
    }

    // ---------- 섬 ----------
    private fun island(time: Float, o: Float) {
        c.save(); c.translate(o, 0f)
        // 섬 모습: 이야기 진도와 배 단계 중 앞선 쪽 (배 3·6·9단계마다 섬이 한 번씩 커진다)
        val sl = Game.shipLevel
        val stage = when { Game.has("main_60") || sl >= 9 -> "island_4"; Game.has("main_30") || sl >= 6 -> "island_3"; Game.has("main_10") || sl >= 3 -> "island_2"; else -> "island" }
        val isl = aget(stage, 196) ?: aget("island", 196)
        if (isl != null) {
            // 그림마다 풀밭 높이(위에서부터 비율)가 조금씩 달라서, 풀밭이 GROUND에 오도록 맞춘다
            val g = when (stage) { "island_2" -> .432f; "island_3" -> .438f; "island_4" -> .445f; else -> .431f }
            Art.draw(c, isl, 98f, GROUND + isl.height / SS.toFloat() * (1 - g))
            // 계절: 4단계 섬은 나무 덮개, 모든 섬은 계절 소품 두 개
            val sea = Game.season()
            val si = Fleet.seasons.indexOf(sea)
            val sheet = when (stage) { "island_4" -> "island_seasons"; "island_3" -> "island_seasons_3"; "island_2" -> "island_seasons_2"; else -> null }
            if (sheet != null && si >= 0) acell(sheet, 1, 4, si, 196)?.let { Art.draw(c, it, 98f, GROUND + it.height / SS.toFloat() * (1 - g)) }
            sea?.deco?.let { d -> d.getOrNull(0)?.let { acell("season_deco", 4, 4, it, 16)?.let { b -> Art.draw(c, b, 75f, GROUND + 1) } }
                d.getOrNull(2)?.let { acell("season_deco", 4, 4, it, 15)?.let { b -> Art.draw(c, b, 96f, GROUND + 1) } } }
            artIsland(time); c.restore(); return
        }
        // 물 밑 바위와 모래 (옆 단면)
        for (i in 0..40) {
            val y = WL + i
            val inset = i * 1.3f
            r(4 + inset, y, 186 - inset * 2, 1f, mix(col("#8a6a44"), col("#1d2a33"), i / 40f))
        }
        // 모래와 풀
        r(2f, GROUND, 192f, WL - GROUND + 1, col("#e6cf8f"))
        r(6f, GROUND - 3, 168f, 4f, col("#5fae4e"))
        r(6f, GROUND - 4, 168f, 1f, col("#7cc65e"))
        for (i in 0 until 30) r(8f + (i * 37) % 164, GROUND - 1 + (i % 2), 1f, 1f, col("#4f9a42"))
        // 부두 (없으면 부서진 말뚝)
        if (Game.dock) {
            r(dockX - 4, GROUND - 1, 34f, 3f, col("#8a5a32")); r(dockX - 4, GROUND - 1, 34f, 1f, col("#a87444"))
            var x = dockX; while (x < dockX + 30) { r(x, GROUND + 2, 2f, 10f, col("#5a3a1e")); x += 8 }
            r(dockX + 26, GROUND - 10, 1f, 9f, col("#5a3a1e")); r(dockX + 25, GROUND - 12, 3f, 3f, col("#ffd36a"))
        } else {
            r(dockX + 2, GROUND, 2f, 9f, col("#5a3a1e")); r(dockX + 12, GROUND + 3, 2f, 7f, col("#5a3a1e"))
            r(dockX, GROUND - 1, 8f, 2f, col("#7a5030"))
        }
        // 창고
        storehouse(storeX, GROUND - 3)
        // 모닥불
        r(fireX - 4, GROUND - 4, 8f, 2f, col("#7d7d7d")); flame(fireX, GROUND - 5, time)
        // 숙소
        for (i in 0 until Game.houses) house(houseX[i], GROUND - 3)
        // 나무와 돌
        for (t in trees) tree(t, GROUND - 3)
        for (rk in rocks) { ell(rk.x, GROUND - 6, 6f, 4f, col("#8b96a3")); ell(rk.x - 1, GROUND - 7, 4f, 2f, col("#b3bcc6")) }
        // 사람
        for (v in people) person(v, time)
        c.restore()
    }

    /** 그림 파일이 있는 섬: 건물과 사람만 그 위에 */
    private fun artIsland(time: Float) {
        fun b(name: String, x: Float, w: Int, fallback: () -> Unit) { val bm = acell("buildings", 7, 1, name.toInt(), w); if (bm != null) Art.draw(c, bm, x, GROUND + 1) else fallback() }
        landmarks(time)
        // 섬에 정박한 배는 마을 뒤에: 집과 장터가 배에 가려지지 않는다
        if (dockedBehind()) shipX()?.let { ourShip(it, time) }
        if (Game.lighthouse) b("4", 8f, 16) {}
        if (Game.field) b("3", 40f, 24) {}
        b(if (Game.dock) "2" else "6", dockX + 13, 34) {}
        b("1", storeX, 24) { storehouse(storeX, GROUND - 3) }
        r(fireX - 4, GROUND - 4, 8f, 2f, col("#7d7d7d")); flame(fireX, GROUND - 5, time)
        for (i in 0 until Game.houses) b("0", houseX[i], 18) { house(houseX[i], GROUND - 3) }
        if (Game.market) b("5", 160f, 28) {}
        for (v in people) person(v, time)
    }

    /** 배 단계마다 섬에 하나씩 늘어나는 큰 건물 (건물 뒤에 그려서 지붕 위로 솟아 보인다) */
    private fun landmarks(time: Float) {
        val sl = Game.shipLevel; val g = GROUND + 1
        // 아스트라 그림이 있으면 그것으로 (번호, 배 단계, x, 폭). 높은 것부터 그려서 앞뒤가 자연스럽게
        if (acell("landmarks", 6, 1, 0, 20) != null) {
            for ((i, need, x, w) in listOf(Quad(3, 7, 110f, 26), Quad(1, 4, 28f, 28), Quad(0, 2, 92f, 20), Quad(4, 8, 52f, 24), Quad(5, 10, 74f, 24), Quad(2, 5, 128f, 28)))
                if (sl >= need) acell("landmarks", 6, 1, i, w)?.let { Art.draw(c, it, x, g) }
            return
        }
        val wood = col("#7a4a24"); val woodD = col("#5a3418"); val stone = col("#b8b2a6"); val stoneD = col("#8a8478"); val gold = col("#ffd36a"); val goldD = col("#c8962a")
        // 4단계: 풍차 (날개가 돈다)
        if (sl >= 4) { val x = 30f
            for (i in 0 until 26) { val w = 12f - i * .2f; r(x - w / 2, g - i - 1, w, 1f, if (i % 6 == 0) stoneD else stone) }
            r(x - 2, g - 8, 4f, 7f, woodD); r(x - 4, g - 30, 8f, 4f, col("#a8442e")); r(x - 3, g - 31, 6f, 1f, col("#c85a3e"))
            val hx = x; val hy = g - 24f; val a0 = time * 1.6f
            for (k in 0 until 4) { val a = a0 + k * Math.PI.toFloat() / 2
                for (j in 2 until 14) { val px = hx + Math.cos(a.toDouble()).toFloat() * j; val py = hy + Math.sin(a.toDouble()).toFloat() * j
                    r(px.roundToInt().toFloat(), py.roundToInt().toFloat(), 1f, 1f, woodD)
                    if (j > 5) { val qx = px + Math.cos(a + 1.57).toFloat() * 2; val qy = py + Math.sin(a + 1.57).toFloat() * 2
                        r(qx.roundToInt().toFloat(), qy.roundToInt().toFloat(), 2f, 2f, col("#efe6cc")) } } }
            r(hx - 1, hy - 1, 2f, 2f, goldD) }
        // 7단계: 종탑
        if (sl >= 7) { val x = 124f
            r(x - 6, g - 40, 12f, 40f, stone); for (i in 0 until 8) r(x - 6, g - 40 + i * 5, 12f, 1f, stoneD)
            r(x - 4, g - 34, 8f, 9f, col("#2a2018")); val sw = Math.round(Math.sin(time * 3.0) * 1).toFloat()
            r(x - 2 + sw, g - 32, 4f, 5f, gold); r(x - 3 + sw, g - 28, 6f, 1f, goldD)
            for (i in 0 until 7) r(x - 8 + i, g - 41 - i, 16f - i * 2, 1f, if (i < 2) col("#3a6ea8") else col("#4a86c8"))
            r(x, g - 50, 1f, 3f, gold) }
        // 10단계: 황금 동상
        if (sl >= 10) { val x = 86f
            r(x - 6, g - 8, 12f, 6f, stone); r(x - 7, g - 9, 14f, 1f, stoneD)
            r(x - 2, g - 22, 4f, 14f, gold); r(x - 2, g - 26, 4f, 4f, gold); r(x - 1, g - 25, 1f, 1f, goldD)
            r(x + 2, g - 30, 1f, 12f, goldD); r(x + 3, g - 30, 5f, 3f, col("#e2483c"))
            r(x - 5, g - 19, 3f, 1f, gold); r(x + 2, g - 19, 1f, 1f, gold)
            if (((time * 2).toInt() % 3) == 0) { r(x - 4, g - 27, 1f, 1f, col("#ffffff")); r(x + 1, g - 16, 1f, 1f, col("#ffffff")) } }
        // 2단계: 섬 깃발
        if (sl >= 2) { val x = 96f
            r(x, g - 34, 1f, 34f, woodD); r(x - 1, g - 35, 3f, 1f, gold)
            val wv = (time * 3).toInt() % 2
            for (i in 0 until 6) r(x + 1, g - 33 + i, 12f - i * .5f + (if ((i + wv) % 2 == 0) 1f else 0f), 1f, if (i < 2) col("#3a9a9a") else if (i < 4) col("#efe6cc") else col("#3a9a9a")) }
        // 8단계: 이긴 해적 깃발들
        if (sl >= 8) for ((i, x) in listOf(54f, 60f, 66f).withIndex()) {
            r(x, g - 28 + i * 2, 1f, 28f - i * 2, woodD)
            r(x + 1, g - 27 + i * 2, 5f, 4f, col("#1e1e24")); r(x + 3, g - 26 + i * 2, 1f, 1f, col("#efe6cc")) }
        // 5단계: 등불 줄 (깜빡)
        if (sl >= 5) for (i in 0 until 7) { val x = 100f + i * 9; val sag = (3 - Math.abs(i - 3)) * 1.2f
            r(x, g - 24 + sag, 9f, 1f, col("#4a2c16"))
            val on = ((time * 2).toInt() + i) % 4 != 0
            r(x + 3, g - 23 + sag, 3f, 3f, if (on) (if (i % 2 == 0) col("#ff8a5a") else gold) else goldD) }
    }

    private fun tree(t: Tree, base: Float) {
        val sx = if (t.shake > 0) (if ((t.shake * 40).toInt() % 2 == 0) 1f else -1f) else 0f
        if (t.hp <= 0) { r(t.x - 2, base - 3, 4f, 3f, col("#7a4a24")); r(t.x - 2, base - 3, 4f, 1f, col("#c49a6a")); return }
        val sc = .7f + t.hp * .1f
        r(t.x - 1, base - 12 * sc, 3f, 12 * sc, col("#6b4022"))
        ell(t.x + sx, base - 18 * sc, 9 * sc, 7 * sc, col("#3f8a3a"))
        ell(t.x - 3 + sx, base - 21 * sc, 6 * sc, 5 * sc, col("#4f9a42"))
        ell(t.x + 3 + sx, base - 23 * sc, 5 * sc, 4 * sc, col("#5fae4e"))
    }

    private fun house(x: Float, base: Float) {
        r(x - 8, base - 11, 16f, 11f, col("#e8d6b0"))
        for (i in 0 until 6) r(x - 10 + i, base - 12 - i, 20f - i * 2, 1f, col("#c0463a"))
        r(x - 2, base - 7, 4f, 7f, col("#7a4a24"))
        r(x - 7, base - 8, 3f, 3f, col("#7fb3d5")); r(x + 4, base - 8, 3f, 3f, col("#7fb3d5"))
    }

    private fun storehouse(x: Float, base: Float) {
        val lv = Game.warehouse
        val w = 20f + lv * 2
        r(x - w / 2, base - 13, w, 13f, col("#8a5a32"))
        var i = 0f; while (i < w) { r(x - w / 2 + i, base - 13, 1f, 13f, col("#6b4022")); i += 4 }
        r(x - w / 2 - 1, base - 15, w + 2, 2f, col("#5d3a1f"))
        r(x - 3, base - 8, 6f, 8f, col("#3a2412"))
        // 쌓인 나무·돌 더미가 창고 옆에 보인다
        val logs = min(6, Game.wood / 12)
        for (k in 0 until logs) r(x + w / 2 + 1 + (k % 3) * 3, base - 2 - (k / 3) * 2, 3f, 2f, col("#a8703f"))
        val st = min(4, Game.stone / 10)
        for (k in 0 until st) ell(x - w / 2 - 4 - (k % 2) * 3, base - 1 - (k / 2) * 2, 2f, 1f, col("#9aa4ae"))
    }

    private fun flame(x: Float, y: Float, time: Float) {
        val k = (floor(time * 8) % 3).toInt()
        r(x - 2, y - 2, 4f, 3f, col("#ff8a2a")); r(x - 1, y - 4 - k, 2f, 3f, col("#ffd36a"))
    }

    /** 9 x 14 주민 (옆모습). 단: 밤색 머리·벽돌색 옷 / 보리: 둥근 단발·겨자색 앞치마 / 해온: 남색 머리·물빛 셔츠 */
    private val looks = listOf(
        intArrayOf(col("#714A36"), col("#D9B58A"), col("#B85B42"), col("#6D6250")),
        intArrayOf(col("#573B34"), col("#F0C49A"), col("#D5A751"), col("#3f8f8a")),
        intArrayOf(col("#334F5C"), col("#E0B895"), col("#668F9A"), col("#6D6250")),
        intArrayOf(col("#d9b86a"), col("#E8C49A"), col("#5a9a4a"), col("#5a4a3a")),   // 견습선원 (밀짚모자)
        intArrayOf(col("#3a2a20"), col("#D9B58A"), col("#7a6ab0"), col("#4a4a5a")),
        intArrayOf(col("#8a4a2a"), col("#F0C49A"), col("#c07a3a"), col("#4a4a5a")),
    )
    private val outline = Color.parseColor("#263A40")

    private fun person(v: Person, time: Float) {
        val moving0 = v.st == 1 || v.st == 3
        val frame = if (v.st == 2) 4 + (floor(time * 5) % 2).toInt() else if (moving0) (floor(v.step) % 4).toInt() else 0
        val wi = if (v.look < 3) -1 else Data.workers.indexOfFirst { it.id == v.wid }
        val sheet = when {
            v.look < 3 -> acell("residents", 6, 3, v.look * 6 + frame, 22)
            wi in 0..5 -> acell("workers_all_a", 6, 6, wi * 6 + frame, 22)
            wi in 6..11 -> acell("workers_all_b", 6, 6, (wi - 6) * 6 + frame, 22)
            else -> null
        } ?: acell("worker", 6, 1, frame, 22)
        if (sheet != null) { Art.draw(c, sheet, v.x, GROUND + 1, v.dir < 0); return }
        val lk = looks[v.look % looks.size]
        val d = v.dir
        val x0 = v.x - 4; val top = GROUND - 17
        fun px(i: Float, y: Float, w: Float, h: Float, cl: Int) { val xx = if (d > 0) x0 + i else x0 + 9 - i - w; r(xx, top + y, w, h, cl) }
        val moving = v.st == 1 || v.st == 3
        val f = if (moving) (floor(v.step) % 4).toInt() else 0
        val bob = if (moving && f % 2 == 1) -1f else if (!moving && (floor(time * 1.6f + v.look) % 2).toInt() == 1) -0f else 0f
        c.save(); c.translate(0f, bob)
        // 다리
        val l1 = if (moving) intArrayOf(0, 1, 0, -1)[f].toFloat() else 0f
        px(2f + l1, 11f, 2f, 3f, lk[3]); px(5f - l1, 11f, 2f, 3f, lk[3])
        // 몸
        px(2f, 6f, 5f, 5f, lk[2]); px(1f, 6f, 1f, 4f, outline)
        if (v.look == 2) px(3f, 7f, 1f, 3f, col("#6b4a2a"))      // 해온의 조끼
        if (v.look == 1) px(3f, 8f, 4f, 3f, col("#e8c870"))      // 보리의 앞치마
        // 머리
        px(2f, 1f, 5f, 5f, lk[1])
        px(1f, 0f, 7f, 2f, lk[0]); px(1f, 2f, 2f, 2f, lk[0])
        if (v.look == 0) px(5f, -1f, 2f, 1f, lk[0])              // 단의 뻗친 앞머리
        if (v.look == 1) px(1f, 2f, 1f, 4f, lk[0])               // 보리의 단발
        if (v.look == 3) { px(-1f, 0f, 11f, 1f, col("#e8c870")); px(1f, -2f, 7f, 2f, col("#e8c870")) }  // 밀짚모자
        px(5f, 3f, 1f, 1f, outline)                              // 눈
        // 팔과 도구
        if (v.st == 2) {
            val sw = (floor(time * 5) % 2).toInt()
            if (v.tree != null) {   // 도끼질
                if (sw == 0) { px(7f, 5f, 1f, 3f, lk[1]); px(7f, 2f, 1f, 4f, col("#6b4022")); px(7f, 1f, 3f, 2f, col("#b3bcc6")) }
                else { px(7f, 8f, 2f, 1f, lk[1]); px(8f, 8f, 4f, 1f, col("#6b4022")); px(11f, 7f, 2f, 3f, col("#b3bcc6")) }
            } else {                // 곡괭이
                if (sw == 0) { px(7f, 4f, 1f, 4f, col("#6b4022")); px(6f, 3f, 4f, 1f, col("#8b96a3")) }
                else { px(7f, 8f, 3f, 1f, col("#6b4022")); px(9f, 7f, 1f, 4f, col("#8b96a3")) }
            }
        } else if (v.carry == 1) { px(0f, -3f, 9f, 2f, col("#a8703f")); px(0f, -3f, 1f, 2f, col("#c49a6a")); px(7f, 6f, 1f, 2f, lk[1]) }
        else if (v.carry == 2) { px(7f, 7f, 3f, 3f, col("#9aa4ae")) }
        else { val a = if (moving) intArrayOf(0, 1, 0, -1)[f].toFloat() else 0f; px(4f + a, 7f, 1f, 3f, lk[1]) }
        c.restore()
    }

    // ---------- 우리 배 ----------
    /** 배 위 선원: 항해 중엔 키·밧줄, 해적과 싸울 땐 대포, 항구와 승리 뒤엔 환호 */
    private val DECK = 9f
    private fun deckH() = if (Game.shipLevel >= 5) 15f else DECK   // 5단계 배는 갑판이 높다
    private val deckX = floatArrayOf(-.20f, .16f, -.02f, .30f, -.34f, .07f)
    // 배 그림(128칸) 안의 장식 자리: 돛대 꼭대기, 뱃머리 끝, 큰 돛 가운데, 갑판 장식 자리 (배 단계별)
    // 1~4단계는 ship_stages.png, 5단계는 ship_stage5.png (같은 128칸, 같은 물선)
    private val MAST = arrayOf(intArrayOf(60, 40), intArrayOf(63, 22), intArrayOf(55, 25), intArrayOf(66, 23), intArrayOf(64, 13))
    private val MAST3 = arrayOf(intArrayOf(58, 30), intArrayOf(60, 16), intArrayOf(56, 20), intArrayOf(64, 15), intArrayOf(62, 6))   // 깃대가 있는 모양의 깃대 꼭대기
    private val BOW = arrayOf(intArrayOf(98, 97), intArrayOf(106, 92), intArrayOf(112, 94), intArrayOf(116, 95), intArrayOf(121, 83))
    private val SAIL = arrayOf(intArrayOf(70, 70), intArrayOf(76, 56), intArrayOf(66, 60), intArrayOf(74, 60), intArrayOf(73, 44))
    private val DECKP = arrayOf(intArrayOf(40, 96), intArrayOf(40, 76), intArrayOf(36, 73), intArrayOf(31, 78), intArrayOf(22, 65))

    /** 이 단계 배 그림 한 칸 (5단계는 따로 된 시트) */
    /** 앱의 배 화면에 그릴 지금 배 그림 */
    fun shipArt(w: Int): android.graphics.Bitmap? = shipCell(Game.shipLevel, Game.lookStage(), w, false)
    private fun shipCell(lv: Int, look: Int, sw: Int, cutFlag: Boolean): android.graphics.Bitmap? {
        val five = lv >= 5
        // 7~10단계는 새 그림(ship_stage7..10)이 오기 전까지 6단계 그림을 쓴다
        val sheet = if (lv >= 7 && Art.has("ship_stage$lv")) "ship_stage$lv" else if (lv >= 6 && Art.has("ship_stage6")) "ship_stage6" else if (five) "ship_stage5" else "ship_stages"
        val rows = if (five) 1 else 4
        val idx = if (five) look else (lv - 1) * 4 + look
        if (cutFlag && look == 3) {
            val t = MAST3[(lv - 1).coerceIn(0, 4)]
            Art.cellCut(sheet, 4, rows, idx, sw * SS, t[0] + 6, t[1] + 1, t[0] + 34, t[1] + 18)?.let { return it }
        }
        return acell(sheet, 4, rows, idx, sw)
    }

    /** 배 꾸미기: 단 장식을 배 그림 위에 겹쳐 그린다 (ship_deco_on: 배에 붙이는 모양. 없으면 아이콘 시트에서 잘라 씀) */
    private fun shipDeco(x: Float, bottom: Float, sw: Float, lv: Int, time: Float) {
        if (Game.decoOn.isEmpty()) return
        val i = (lv - 1).coerceIn(0, 4)
        fun px(cx: Int) = x + (cx / 128f - .5f) * sw
        fun py(cy: Int) = bottom - (1 - cy / 128f) * sw
        for ((slot, id) in Game.decoOn) {
            val d = Fleet.deco(id) ?: continue
            // 20차 큰 장식 그림: 칸(256)의 붙는 점을 배의 자리에 맞춘다
            if (Art.has(d.bigSheet)) {
                val k = sw / 94f
                fun cs(f: Float) = (f * k).roundToInt().coerceAtLeast(8)
                when (slot) {
                    // 깃발: 천 왼쪽 가운데 = 깃대 꼭대기 옆
                    "flag" -> { val n = cs(20f); val wave = if ((floor(time * 3) % 2).toInt() == 0) 0f else 1f
                        val m = if (Game.lookStage() == 3) intArrayOf(MAST3[i][0] + 3, MAST3[i][1] + 1) else MAST[i]
                        acell(d.bigSheet, 4, 3, d.bigIdx, n)?.let { Art.draw(c, it, px(m[0]) + 1f + n / 2f, py(m[1]) + 3f + n / 2f + wave) } }
                    // 뱃머리: 왼쪽 아래 = 뱃머리 끝
                    "figurehead" -> { val n = cs(19f); acell(d.bigSheet, 4, 3, d.bigIdx, n)?.let { Art.draw(c, it, px(BOW[i][0]) - n * .2f + n / 2f, py(BOW[i][1]) + 3f) } }
                    // 돛 무늬: 가운데 = 큰 돛 가운데
                    "sail" -> { val n = cs(22f); acell(d.bigSheet, 4, 3, d.bigIdx, n)?.let { Art.draw(c, it, px(SAIL[i][0]), py(SAIL[i][1]) + 3f + n / 2f) } }
                    "deck" -> { val n = cs(17f); acell(d.bigSheet, 4, 3, d.bigIdx, n)?.let { Art.draw(c, it, px(DECKP[i][0]), py(DECKP[i][1]) + .5f) } }
                }
                continue
            }
            val on = Art.has(d.onSheet); val ons = d.onSheet; val ics = d.iconSheet
            when (slot) {
                // 무늬 가운데(칸 가운데)를 큰 돛 가운데에
                "sail" -> (if (on) acell(ons, 4, 4, d.icon, 12) else Art.sub(ics, 4, 4, d.icon, .24f, .34f, .80f, .74f, 9 * SS))?.let {
                    Art.draw(c, it, px(SAIL[i][0]), py(SAIL[i][1]) + (if (on) 6f else 3f)) }
                // 깃발 천의 왼쪽 가운데(칸 왼쪽 가운데)를 깃대 꼭대기 옆에
                "flag" -> {
                    val wave = if ((floor(time * 3) % 2).toInt() == 0) 0f else 1f
                    val m = if (Game.lookStage() == 3) intArrayOf(MAST3[i][0] + 3, MAST3[i][1] + 1) else MAST[i]
                    if (on) acell(ons, 4, 4, d.icon, 14)?.let { Art.draw(c, it, px(m[0]) + 1f + 7f, py(m[1]) + 3f + 7f + wave) }
                    else Art.sub(ics, 4, 4, d.icon, .32f, .14f, .94f, .68f, 10 * SS)?.let { Art.draw(c, it, px(m[0]) + 5.5f, py(m[1]) + 6f + wave) }
                }
                // 뱃머리·갑판 물건은 칸 아래 가운데가 붙는 곳
                "figurehead" -> (if (on) acell(ons, 4, 4, d.icon, 13) else acell(ics, 4, 4, d.icon, 10))?.let {
                    Art.draw(c, it, px(BOW[i][0]), py(BOW[i][1]) + 4f) }
                "deck" -> (if (on) acell(ons, 4, 4, d.icon, 16) else acell(ics, 4, 4, d.icon, 9))?.let {
                    Art.draw(c, it, px(DECKP[i][0]), py(DECKP[i][1]) + (if (on) .5f else 0f)) }
            }
        }
    }

    private fun deckCrew(x: Float, y: Float, w: Float, time: Float) {
        val v = Game.voy ?: return
        if (v.crew.isEmpty()) return
        val f = Game.voyFrac()
        val u = pirateU()
        val fight = u >= 0f && u < pA / (pA + .035f)
        val cheer = (f in Data.fArrive..Data.fLeave) || (u >= 0f && !fight && v.win)
        for ((i, id) in v.crew.withIndex()) {
            val cd = Fleet.crewOf(id) ?: continue
            val pose = when { cheer -> 3; fight -> if (i % 2 == 0) 2 else 0; i == 0 -> 1; else -> 0 }
            // Lv11부터는 코트 입은 갑판 그림
            val sheet = if (Game.crewLevel(id) >= 11) cd.sheet.replace("crew_deck_", "crew_deck_15_") else cd.sheet
            val bm = acell(sheet, 4, 6, cd.row * 4 + pose, 18) ?: acell(cd.sheet, 4, 6, cd.row * 4 + pose, 18) ?: continue
            val bob = if (pose == 0 && (floor(time * 2 + i) % 2).toInt() == 1) 1f else 0f
            Art.draw(c, bm, x + deckX[i % deckX.size] * w, y - deckH() + bob)
            crewHead[id] = (x + deckX[i % deckX.size] * w) to (y - deckH() - 15)
            // 선원 사건: 항해 62~74% 사이에 그 선원 머리 위에 감정 말풍선
            Fleet.crewEvent(Game.cevId)?.takeIf { it.crew == id && f in .62f..(.74f) }?.let { e ->
                acell("crew_emotes", 4, 2, Fleet.EMOTES.indexOf(e.emote).coerceAtLeast(0), 14)?.let { em ->
                    Art.draw(c, em, x + deckX[i % deckX.size] * w + 4, y - deckH() - 14 + sin(time * 4f) * .8f) }
            }
        }
        crewSeen = 0f
    }

    private fun ourShip(x: Float, time: Float) {
        val lv = Game.shipLevel
        val y = WL + (sin(time * 1.8f) * 1.2f).roundToInt()
        val sw = shipW()
        val look = Game.lookStage()
        // 장식 깃발을 달면 4번째 모양의 원래 빨간 깃발은 지우고 그린다 (겹치지 않게)
        val sb = shipCell(lv, look, sw, Game.decoOn["flag"] != null)
            ?: acell("ships", 4, 1, lv - 1, sw)
        if (sb != null) { Art.draw(c, sb, x, y + 5); if (sw == shipW(.5f)) { shipDeco(x, y + 5f, sw.toFloat(), lv, time); deckCrew(x, y, sw.toFloat(), time) }; return }
        val len = 30f + (lv - 1) * 7f
        val hull = col("#6D513A"); val hullL = col("#A78355")
        for (i in 0 until 7) r(x - len / 2 + i, y - 4 + i, len - i * 2 + (if (i < 3) 4f else 0f), 1f, if (i < 2) hullL else hull)
        r(x - len / 2, y - 5, len + 4, 1f, col("#c49a6a"))
        r(x + len / 2, y - 7, 4f, 2f, hull)                       // 뱃머리
        r(x + len / 2 + 2, y - 6, 2f, 2f, col("#3B8F91"))         // 파란 끈
        // 대포 (2단계부터)
        if (lv >= 2) for (k in 0 until min(3, lv - 1)) r(x - len / 2 + 6 + k * 9, y - 3, 3f, 2f, col("#2a2a2a"))
        // 돛
        val masts = when (lv) { 1, 2 -> floatArrayOf(0f); 3 -> floatArrayOf(-8f, 7f); else -> floatArrayOf(-11f, 0f, 11f) }
        val sway = if (sailing()) (sin(time * 2.4f) * 1f).roundToInt().toFloat() else 0f
        for ((k, m) in masts.withIndex()) {
            val main = masts.size == 1 || k == masts.size / 2
            val mh = if (main) 24f + lv * 3 else 22f
            r(x + m, y - 5 - mh, 1f, mh, col("#4a2c16"))
            val sw = if (main) 12f + lv else 10f; val sh = if (main) 9f + lv else 8f
            r(x + m - sw / 2 + sway, y - 5 - mh + 3, sw, sh, col("#E8DFC4"))
            r(x + m - sw / 2 + 1 + sway, y - 5 - mh + 5 + sh, sw - 2, sh - 2, col("#E8DFC4"))
            r(x + m + sw / 2 - 2 + sway, y - 5 - mh + 4, 2f, sh - 2, col("#cfc4a4"))
        }
        r(x, y - 5 - 24f - lv * 3 - 4, 6f, 3f, col("#c0463a"))
        // 갑판 위 선원 둘
        r(x - 8, y - 9, 2f, 2f, col("#e0b895")); r(x - 8, y - 7, 2f, 2f, col("#668F9A"))
        r(x + 6, y - 9, 2f, 2f, col("#e0b895")); r(x + 6, y - 7, 2f, 2f, col("#B85B42"))
        // 실은 짐 (나무 상자)
        val v = Game.voy
        if (v != null && (v.cw + v.cs) > 0 && Game.voyFrac() < Data.fArrive) { r(x - 3, y - 9, 4f, 4f, col("#b07a44")); r(x - 3, y - 9, 4f, 1f, col("#d9a86a")) }
        // 물살
        if (sailing()) {
            ga = .7f
            for (k in 1 until 6) { val xx = x - len / 2 - k * 7 - ((time * 18) % 7); r(xx, y + 2 + (k % 2), 4f, 1f, Color.WHITE) }
            r(x + len / 2 + 3, y - 1, 2f, 1f, Color.WHITE)
            ga = 1f
        }
    }

    // ---------- 해적 ----------
    private fun pirates(time: Float) {
        val u = pirateU(); if (u < 0f) return
        val v = Game.voy ?: return
        val cs = Data.pirate(v.pirate ?: "")?.colors ?: emptyList()
        val fightA = pA * 0.55f / (pA + .035f); val fightB = pA / (pA + .035f)
        var x = 150f; var sink = 0f
        when {
            u < fightA -> x = lerp(230f, 150f, ease(u / fightA))
            u < fightB -> x = 150f
            v.win -> { x = 150f - (u - fightB) * 40f; sink = (u - fightB) / (1 - fightB) * 30f }
            else -> x = lerp(150f, 240f, ease((u - fightB) / (1 - fightB)))
        }
        pirateShip(x, WL + sink, time, cs, sink > 0)
        if (sink in 1f..14f) acell("fx", 6, 2, 5, 26)?.let { Art.draw(c, it, x, WL + 4, false, (255 * (1 - sink / 14f)).toInt()) }
        // 포격: 번갈아 쏘고 포탄이 날아간다
        if (u in fightA..fightB && Art.has("fx")) {
            val k = time * 1.6f
            val turn = floor(k).toInt() % 2; val t = k % 1f
            val sx = if (turn == 0) SAIL_X + 18 else x - 16
            val ex = if (turn == 0) x - 10 else SAIL_X + 10
            if (t < .14f) acell("fx", 6, 2, 0, 12)?.let { Art.draw(c, it, sx, WL - 3, turn != 0) }
            val sm = when { t < .3f -> 1; t < .55f -> 2; else -> 3 }
            acell("fx", 6, 2, sm, 8 + sm * 3)?.let { Art.draw(c, it, sx + (if (turn == 0) -2 else 2) * t * 6, WL - 6 - t * 6, false, (200 * (1 - t)).toInt()) }
            if (t < .7f) {
                val bx = lerp(sx, ex, t / .7f); val by = WL - 8 - sin(t / .7f * PI.toFloat()) * 14
                r(bx, by, 2f, 2f, col("#1a1a1a"))
            } else acell("fx", 6, 2, 4, 14)?.let { Art.draw(c, it, ex, WL + 3) }
        } else if (u in fightA..fightB) {
            val k = time * 1.6f
            val turn = floor(k).toInt() % 2; val t = k % 1f
            val sx = if (turn == 0) SAIL_X + 18 else x - 16
            val ex = if (turn == 0) x - 10 else SAIL_X + 10
            if (t < .12f) { r(sx - 1, WL - 9, 3f, 3f, col("#ffd36a")); r(sx, WL - 8, 1f, 1f, Color.WHITE) }
            if (t < .7f) {
                val bx = lerp(sx, ex, t / .7f); val by = WL - 8 - sin(t / .7f * PI.toFloat()) * 14
                r(bx, by, 2f, 2f, col("#1a1a1a"))
            } else {
                val s = (t - .7f) / .3f
                ga = 1 - s; ell(ex, WL - 8, 3 + s * 5, 2 + s * 4, col("#e8e8e8")); ga = 1f
            }
            ga = .5f; ell(sx, WL - 10, 4f + t * 4, 3f + t * 2, col("#d8d8d8")); ga = 1f
        }
    }

    private fun pirateShip(x: Float, y0: Float, time: Float, cs: List<Int>, sinking: Boolean) {
        val sail = cs.getOrElse(0) { col("#383D40") }; val mark = cs.getOrElse(1) { col("#B4BDB9") }; val hull = cs.getOrElse(2) { col("#644B37") }
        val y = y0 + (sin(time * 2.1f) * 1.2f).roundToInt()
        val pv = Game.voy?.pirate ?: ""
        val pi = when { pv.contains("gurok") || pv.contains("blockader") || pv.contains("farwater") -> 2; pv.contains("red") || pv.contains("raider") -> 1; else -> 0 }
        Fleet.boss(pv)?.let { b -> (if (b.sprite >= 100) acell("pirate_bosses_2", 4, 1, b.sprite - 100, 72) else acell("pirate_bosses", 4, 2, b.sprite, 72))?.let { bm ->
            c.save(); if (sinking) { c.rotate(18f * min(1f, (y0 - WL) / 20f), x, y); c.clipRect(0f, 0f, W.toFloat(), WL + 4) }
            Art.draw(c, bm, x, y + 5); c.restore(); return } }
        val pa = Data.pirates.indexOfFirst { it.id == pv }
        ((if (pa in 0..7) acell("pirates_all", 4, 2, pa, 58) else null) ?: acell("pirates", 3, 1, pi, 58))?.let { bm ->
            c.save(); if (sinking) { c.rotate(18f * min(1f, (y0 - WL) / 20f), x, y); c.clipRect(0f, 0f, W.toFloat(), WL + 4) }
            Art.draw(c, bm, x, y + 5); c.restore(); return }
        c.save()
        if (sinking) { c.rotate(18f * min(1f, (y0 - WL) / 20f), x, y); c.clipRect(0f, 0f, W.toFloat(), WL + 4) }
        for (i in 0 until 6) r(x - 15 + i - 3, y - 4 + i, 30f - i * 2, 1f, hull)
        r(x - 19, y - 6, 4f, 2f, hull)
        r(x - 15, y - 5, 30f, 1f, mix(hull, Color.WHITE, .25f))
        r(x, y - 32, 1f, 28f, col("#1e1410"))
        r(x - 7, y - 29, 14f, 10f, sail); r(x - 6, y - 17, 12f, 9f, sail)
        r(x - 1, y - 27, 2f, 5f, mark); r(x - 3, y - 23, 4f, 1f, mark); r(x - 3, y - 24, 1f, 1f, mark)   // 갈고리 무늬
        r(x, y - 36, 6f, 4f, sail)
        r(x - 12, y - 3, 3f, 2f, col("#2a2a2a"))
        c.restore()
    }

    // ---------- 항구 ----------
    private fun port(time: Float, o: Float) {
        val pr = Data.port(Game.voy?.port ?: Game.route)
        val pc = pr.colors
        (aget(if (pr.id == "port_haesol") "port" else pr.id, 170) ?: aget("port", 170))?.let { Art.draw(c, it, o + 120f, WL + 6); return }
        c.save(); c.translate(o, 0f)
        // 뒤쪽 땅과 집
        r(40f, GROUND - 2, 170f, WL - GROUND + 3, col("#cdb98a"))
        r(40f, GROUND - 3, 170f, 1f, col("#e2d3a8"))
        val bc = listOf(col("#e8d6b0"), col("#d8c4a0"), col("#cfc6b4"))
        for (i in 0 until 4) {
            val bx = 110f + i * 24; val bh = 18f + (i % 2) * 7
            r(bx, GROUND - 2 - bh, 20f, bh, bc[i % 3])
            for (k in 0 until 5) r(bx - 2 + k, GROUND - 3 - bh - k, 24f - k * 2, 1f, if (pc.isNotEmpty()) pc[i % pc.size] else col("#c0463a"))
            r(bx + 8, GROUND - 9, 4f, 7f, col("#5a4a3a"))
            r(bx + 3, GROUND - bh + 2, 3f, 3f, col("#7fb3d5"))
        }
        // 부두 판자
        r(40f, GROUND + 1, 160f, 3f, col("#8a5a32")); r(40f, GROUND + 1, 160f, 1f, col("#a87444"))
        var x = 44f; while (x < 200f) { r(x, GROUND + 4, 2f, 10f, col("#5a3a1e")); x += 12 }
        // 크레인
        r(96f, GROUND - 30, 2f, 30f, col("#5a3a1e")); r(80f, GROUND - 30, 20f, 2f, col("#5a3a1e"))
        val swing = sin(time * 1.2f) * 3
        r(82f + swing, GROUND - 28, 1f, 10f, col("#3a2a1a")); r(80f + swing, GROUND - 18, 5f, 4f, col("#b07a44"))
        // 상자 나르는 항구 사람들
        for (i in 0 until 3) {
            val t = ((time * .25f + i * .33f) % 1f)
            val px = 60f + t * 40f; val carry = t < .5f
            r(px, GROUND - 7, 2f, 2f, col("#f2c79a")); r(px, GROUND - 5, 2f, 4f, if (pc.size > 1) pc[1] else col("#2f6fb8"))
            if (carry) r(px - 1, GROUND - 10, 4f, 3f, col("#b07a44"))
        }
        // 상자 더미
        for (k in 0 until 4) r(170f + (k % 2) * 5, GROUND - 4 - (k / 2) * 4, 5f, 4f, col("#9a6236"))
        c.restore()
    }

    // ---------- 밤 불빛 ----------
    private fun nightLights(time: Float, io: Float?, po: Float?) {
        if (io != null) {
            val save = ga; ga = save * .3f; ell(fireX + io, GROUND - 6, 10f, 6f, col("#ffb347")); ga = save
            flame(fireX + io, GROUND - 5, time)
            for (i in 0 until Game.houses) { r(houseX[i] + io - 7, GROUND - 11, 3f, 3f, col("#ffd36a")); r(houseX[i] + io + 4, GROUND - 11, 3f, 3f, col("#ffd36a")) }
            if (Game.dock) r(dockX + 25 + io, GROUND - 12, 3f, 3f, col("#ffd36a"))
        }
        if (po != null) for (i in 0 until 4) r(110f + i * 24 + 3 + po, GROUND - 18, 3f, 3f, col("#ffd36a"))
        shipX()?.let { x -> r(x + 16, WL - 10, 2f, 2f, col("#ffd36a")); r(x - 16, WL - 8, 2f, 2f, col("#ffd36a")) }
    }
}
