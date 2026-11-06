package com.littleisland.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 섬 세계를 작은 도트 그림(600 x 467)으로 그린다.
 * 0~200: 내 섬, 200~400: 항해 중인 바다, 400~600: 항구.
 * 화면에는 이 그림의 일부를 확대해서 보여준다.
 */
class Scene {
    companion object {
        const val W = 600
        const val H = 467
        const val PAGE = 200
        const val OY = 153f       // 섬을 아래로 내리는 양
        const val HORIZON = 178f  // 수평선 높이
        private const val CX = 100f
        private const val CY = 102f
    }

    val bitmap: Bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    private val c = Canvas(bitmap)
    private val p = Paint().apply { isAntiAlias = false }
    private val gp = Paint().apply { isAntiAlias = false }
    private var ga = 1f   // 전역 투명도
    private val tp = Paint().apply { isAntiAlias = false; textSize = 8f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }

    /** 테스트용: null이면 실제 시각을 씀 */
    var hourOverride: Float? = null

    // ---------- 그리기 도구 ----------
    private fun col(hex: String) = Color.parseColor(hex)

    private fun r(x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.color = color
        p.alpha = (Color.alpha(color) * ga).roundToInt().coerceIn(0, 255)
        val xx = x.roundToInt().toFloat()
        val yy = y.roundToInt().toFloat()
        c.drawRect(xx, yy, xx + w, yy + h, p)
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

    // ---------- 시간대 색 ----------
    private class Key(val h: Float, top: String, bot: String, sea: String, val dark: Float) {
        val top = Color.parseColor(top); val bot = Color.parseColor(bot); val sea = Color.parseColor(sea)
    }
    private val keys = listOf(
        Key(0f, "#0b1030", "#1c2350", "#0f2a4a", .75f),
        Key(5f, "#0b1030", "#1c2350", "#0f2a4a", .75f),
        Key(6.2f, "#3a3f7a", "#f0a27a", "#2a5a7a", .35f),
        Key(7.6f, "#4aa8e8", "#bfe6f5", "#2f8fb8", 0f),
        Key(17f, "#4aa8e8", "#bfe6f5", "#2f8fb8", 0f),
        Key(18.5f, "#5a4f9a", "#ff9a5a", "#3a6a8a", .25f),
        Key(19.6f, "#2a2a60", "#a05a70", "#1f3f60", .55f),
        Key(21f, "#0b1030", "#1c2350", "#0f2a4a", .75f),
        Key(24f, "#0b1030", "#1c2350", "#0f2a4a", .75f),
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

    // ---------- 섬 시뮬레이션 ----------
    private class Tree(val x: Float, val y: Float) { var hp = 3; var regrow = 0f }
    private class Rock(val x: Float, val y: Float)
    private class Target(val wood: Boolean, val tree: Tree?, val rock: Rock?, val x: Float, val y: Float)
    private class Villager(var x: Float, var y: Float, val color: Int, val helper: Boolean = false) {
        var st = 0  // 0 쉼, 1 가는 중, 2 일하는 중, 3 돌아오는 중
        var t = Random.nextFloat() * 2f
        var tgt: Target? = null
        var carry = 0 // 0 없음, 1 나무, 2 돌
        var dir = 1f
        var step = 0f
    }

    private val houseSlots = listOf(80f to 96f, 118f to 124f, 84f to 124f)
    private val trees = listOf(50 to 80, 62 to 72, 44 to 96, 56 to 112, 70 to 130, 134 to 74, 148 to 86, 128 to 132, 142 to 118, 76 to 68, 112 to 66)
        .map { Tree(it.first.toFloat(), it.second.toFloat()) }
    private val rocks = listOf(66 to 90, 136 to 102, 106 to 138, 96 to 74).map { Rock(it.first.toFloat(), it.second.toFloat()) }
    private val fireX = 100f; private val fireY = 106f
    private val storeX = 114f; private val storeY = 96f
    private val bodyColors = listOf("#e0574a", "#4a8fe0", "#e0c04a", "#9a5ae0", "#4ac08a", "#e08a4a").map { col(it) }
    private val villagers = ArrayList<Villager>()

    // 건물은 저장된 진행 상황(Game)을 따른다
    private val houses get() = Game.houses
    private val hasStore = true
    private val hasDock get() = Game.dock

    /** 주민 수를 집 수에 맞춘다. 새 주민은 부두 쪽에서 걸어 들어온다. */
    private fun syncVillagers() {
        while (villagers.size < Game.pop()) {
            val i = villagers.size
            val v = if (i < Game.RESIDENTS) Villager(fireX + i * 4 - 4, fireY + 6, bodyColors[i])
                else Villager(170f, 102f, bodyColors[i % bodyColors.size], true)  // 알바는 부두에서 내린다
            villagers.add(v)
        }
        while (villagers.size > Game.pop()) villagers.removeAt(villagers.size - 1)
    }

    // 부두에 들르는 짐배 (0 멀리, 1 들어오는 중, 2 정박, 3 나가는 중)
    private var shipSt = 0
    private var shipX = 240f
    private var shipWait = 0f

    /** 짐이 들어왔을 때 잠깐 띄우는 글자 */
    class Pop(val x: Float, var y: Float, val text: String, var t: Float)
    val pops = ArrayList<Pop>()

    private fun pickTarget(): Target {
        if (Random.nextFloat() < .68f) {
            val live = trees.filter { it.hp > 0 }
            if (live.isNotEmpty()) {
                val t = live[Random.nextInt(live.size)]
                return Target(true, t, null, t.x + if (Random.nextBoolean()) -5f else 5f, t.y + 1)
            }
        }
        val rk = rocks[Random.nextInt(rocks.size)]
        return Target(false, null, rk, rk.x - 5, rk.y + 1)
    }

    private fun moveTo(v: Villager, x: Float, y: Float, dt: Float): Boolean {
        val dx = x - v.x; val dy = y - v.y; val d = hypot(dx, dy); val sp = 11f * dt
        if (d <= sp) { v.x = x; v.y = y; return true }
        v.x += dx / d * sp; v.y += dy / d * sp
        v.dir = if (dx < 0) -1f else 1f
        v.step += dt * 8
        return false
    }

    fun update(dt: Float) {
        Game.tick(dt)
        syncVillagers()
        if (Game.arrived > 0) { pops.add(Pop(176f, 88f + OY, "알바 +" + Game.arrived, 3f)); Game.arrived = 0 }
        for (pp in pops) { pp.t -= dt; pp.y -= dt * 6 }
        pops.removeAll { it.t <= 0f }
        if (hasDock) {
            when (shipSt) {
                0 -> { Game.shipT += dt; if (Game.shipT >= Game.SHIP_EVERY) { Game.shipT = 0f; shipSt = 1; shipX = 240f } }
                1 -> { shipX -= dt * 9; if (shipX <= 188f) {
                    shipX = 188f; shipSt = 2; shipWait = 9f
                    val (gw, gs) = Game.add(10 + Random.nextInt(11), 5 + Random.nextInt(6))
                    if (gw + gs > 0) pops.add(Pop(188f, 88f + OY, "+" + gw + " +" + gs, 3f))
                } }
                2 -> { shipWait -= dt; if (shipWait <= 0f) shipSt = 3 }
                3 -> { shipX += dt * 9; if (shipX > 240f) shipSt = 0 }
            }
        }
        for (t in trees) if (t.hp <= 0) { t.regrow -= dt; if (t.regrow <= 0) t.hp = 3 }
        for (v in villagers) {
            when (v.st) {
                0 -> { v.t -= dt; if (v.t <= 0) { v.tgt = pickTarget(); v.st = 1 } }
                1 -> {
                    val g = v.tgt!!
                    if (moveTo(v, g.x, g.y, dt)) {
                        if (g.wood && (g.tree?.hp ?: 0) <= 0) { v.st = 0; v.t = .3f }
                        else { v.st = 2; v.t = 2.4f; val ox = g.tree?.x ?: g.rock!!.x; v.dir = if (ox > v.x) 1f else -1f }
                    }
                }
                2 -> {
                    v.t -= dt
                    if (v.t <= 0) {
                        val g = v.tgt!!
                        g.tree?.let { if (it.hp > 0) { it.hp--; if (it.hp <= 0) it.regrow = 40f } }
                        v.carry = if (g.wood) 1 else 2
                        v.st = 3
                    }
                }
                3 -> {
                    val tx = if (hasStore) storeX - 2 else fireX
                    val ty = if (hasStore) storeY + 4 else fireY + 5
                    if (moveTo(v, tx, ty, dt)) {
                        if (v.carry == 1) Game.add(1, 0) else if (v.carry == 2) Game.add(0, 1)
                        v.carry = 0; v.st = 0; v.t = 0.2f + Random.nextFloat() * .8f
                    }
                }
            }
        }
    }

    // ---------- 그리기 ----------
    fun draw(time: Float) {
        val h = hourNow()
        palette(h)
        ga = 1f
        sky(time, h)
        // 바다
        r(0f, HORIZON, W.toFloat(), H - HORIZON, sea)
        waves(time)
        island(time)
        if (hasDock && shipSt != 0) smallShip(shipX, OY + 112, time, col("#c0463a"))
        seaPage(time)
        port(time)
        if (dark > 0f) {
            p.color = Color.argb((dark * .68f * 255).roundToInt(), 8, 12, 40)
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        }
        if (dark > .15f) {
            ga = min(1f, dark * 1.6f)
            lights(time)
            ga = 1f
        }
        for (pp in pops) {
            tp.alpha = (min(1f, pp.t) * 255).roundToInt()
            tp.color = Color.argb(tp.alpha, 40, 24, 10); c.drawText(pp.text, pp.x + 1, pp.y + 1, tp)
            tp.color = Color.argb(tp.alpha, 255, 226, 140); c.drawText(pp.text, pp.x, pp.y, tp)
        }
    }

    private fun sky(time: Float, h: Float) {
        gp.shader = LinearGradient(0f, 0f, 0f, HORIZON, skyTop, skyBot, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), HORIZON, gp)
        gp.shader = null
        if (dark > .3f) {
            ga = (dark - .3f) * 2
            for (i in 0 until 270) {
                val x = (i * 73) % W; val y = (i * 37) % HORIZON.toInt()
                if ((i + floor(time * 1.5f).toInt()) % 11 > 1) r(x.toFloat(), y.toFloat(), 1f, 1f, Color.WHITE)
            }
            ga = 1f
        }
        // 해와 달: 각 페이지마다 같은 자리에 보이게 그린다
        for (page in 0 until 3) {
            val ox = page * PAGE.toFloat()
            if (h in 5.6f..19.4f) {
                val t = (h - 5.6f) / 13.8f
                val x = ox + 12 + t * 176; val y = HORIZON - 6 - sin(t * PI.toFloat()) * 38
                ell(x, y, 6f, 6f, if (h < 7 || h > 17.8f) col("#ffb070") else col("#fff2b0"))
            } else {
                val hh = if (h < 12) h + 24 else h
                val t = (hh - 19.4f) / 10.2f
                val x = ox + 12 + t * 176; val y = HORIZON - 6 - sin(t * PI.toFloat()) * 38
                ell(x, y, 5f, 5f, col("#e8ecff")); ell(x + 2, y - 1, 4f, 4f, skyTop)
            }
        }
        // 구름
        val n = W / 40
        for (i in 0 until n) {
            val sp = .5f + (i % 3) * .25f
            val x = ((i * 67 + time * 2 * sp) % (W + 40)) - 30
            val y = 50f + (i * 29) % 110
            ga = .85f - dark * .5f
            ell(x, y, 12f, 3f, Color.WHITE); ell(x + 8, y - 2, 7f, 3f, Color.WHITE)
            ga = 1f
        }
    }

    private fun waves(time: Float) {
        ga = .22f
        var y = HORIZON + 3
        while (y < H) {
            var x = ((y * 13 + floor(time * 6)) % 24) - 24
            while (x < W) { r(x, y, 5f, 1f, Color.WHITE); x += 24 }
            y += 6
        }
        ga = 1f
    }

    // ----- 1페이지: 내 섬 -----
    private fun island(time: Float) {
        c.save(); c.translate(0f, OY)
        val foam = if (sin(time * 2) > 0) 1f else 0f
        ell(CX, CY + 2, 72 + foam, 45 + foam, col("#e9f4f6"))
        ell(CX, CY + 2, 70f, 44f, col("#e6cf8f"))
        ell(CX, CY - 1, 62f, 38f, col("#5fae4e"))
        ell(CX - 6, CY - 6, 48f, 26f, col("#6dbd58"))
        for (i in 0 until 60) {
            val a = i * 2.39f; val rr = sqrt(i / 60f)
            r(CX + cos(a) * rr * 56, CY - 1 + sin(a) * rr * 32, 1f, 1f, col("#4f9a42"))
        }
        if (hasDock) {
            r(166f, 100f, 34f, 4f, col("#8a5a32"))
            var x = 168f; while (x < 200) { r(x, 104f, 1f, 4f, col("#5a3a1e")); x += 6 }
            r(166f, 100f, 34f, 1f, col("#a87444"))
        }
        // y 순서대로 그리기
        val items = ArrayList<Pair<Float, () -> Unit>>()
        for (t in trees) items.add(t.y to { drawTree(t) })
        for (rk in rocks) items.add(rk.y to { ell(rk.x, rk.y - 2, 5f, 3f, col("#8b96a3")); ell(rk.x - 1, rk.y - 3, 3f, 2f, col("#b3bcc6")) })
        items.add(fireY to { r(fireX - 4, fireY - 1, 8f, 2f, col("#7d7d7d")); flame(time) })
        if (hasStore) items.add(storeY + 6 to { drawStore(storeX, storeY) })
        for (i in 0 until houses) { val (x, y) = houseSlots[i]; items.add(y + 6 to { drawHouse(x, y) }) }
        for (v in villagers) items.add(v.y to { drawVillager(v, time) })
        items.sortBy { it.first }
        for (it in items) it.second()
        c.restore()
    }

    private fun drawTree(t: Tree) {
        if (t.hp <= 0) { r(t.x - 2, t.y - 2, 4f, 2f, col("#7a4a24")); return }
        r(t.x - 1, t.y - 5, 2f, 5f, col("#7a4a24"))
        val s = when (t.hp) { 3 -> 0f; 2 -> -1f; else -> -2f }
        ell(t.x, t.y - 10, 6 + s, 6 + s, col("#2f7a3a"))
        ell(t.x - 1, t.y - 11, 4 + s, 4 + s, col("#3f9a48"))
        r(t.x - 2, t.y - 13, 2f, 1f, col("#6cc06a"))
    }

    private fun flame(time: Float) {
        val f = floor(time * 8).toInt() % 3
        r(fireX - 2, fireY - 4, 4f, 2f, col("#ff7a2a"))
        r(fireX - 1, fireY - 6 - if (f == 1) 1 else 0, 2f, 2f, col("#ffc94a"))
    }

    private fun drawHouse(x: Float, y: Float) {
        r(x, y - 6, 12f, 7f, col("#e8d6b0"))
        for (i in 0 until 5) r(x - 1 + i, y - 7 - i, 14f - i * 2, 1f, col("#c0463a"))
        r(x + 5, y - 3, 2f, 4f, col("#6b4a2a"))
        r(x + 2, y - 3, 2f, 2f, col("#7fb6d9")); r(x + 8, y - 3, 2f, 2f, col("#7fb6d9"))
    }

    private fun drawStore(x: Float, y: Float) {
        if (Game.storage >= 1) { r(x + 14, y - 3, 4f, 4f, col("#b07a44")); r(x + 14, y - 3, 4f, 1f, col("#7d4c28")) }
        if (Game.storage >= 2) { r(x + 15, y - 7, 4f, 4f, col("#b07a44")); r(x - 5, y - 3, 4f, 4f, col("#b07a44")) }
        r(x, y - 7, 13f, 8f, col("#9a6236"))
        var i = 0; while (i < 13) { r(x + i, y - 7, 1f, 8f, col("#7d4c28")); i += 3 }
        r(x - 1, y - 9, 15f, 2f, col("#5d3a1f"))
        r(x + 5, y - 4, 3f, 5f, col("#4a2c16"))
    }

    private fun drawVillager(v: Villager, time: Float) {
        val bob = if (v.st == 2) floor(time * 6).toInt() % 2 else 0
        val lx = v.x - 1; val ly = v.y - 5 - bob
        r(lx, ly, 3f, 2f, col("#f2c79a")); r(lx, ly, 3f, 1f, col("#3a2a1a"))
        if (v.helper) { r(lx - 1, ly - 1, 5f, 1f, col("#e8c060")); r(lx, ly - 2, 3f, 1f, col("#e8c060")) } // 밀짚모자
        r(lx, ly + 2, 3f, 2f, v.color)
        val walking = v.st == 1 || v.st == 3
        val st = if (walking) floor(v.step).toInt() % 2 else 0
        r(lx, ly + 4, 1f, 1f, col("#3a2a1a")); r(lx + 2, ly + 4 - st, 1f, 1f, col("#3a2a1a"))
        if (v.st == 2) r(lx + if (v.dir > 0) 3 else -1, ly + 1 + floor(time * 6).toInt() % 2, 1f, 2f, col("#9aa4ae"))
        if (v.carry != 0) r(lx, ly - 2, 3f, 2f, if (v.carry == 1) col("#a0683a") else col("#aab4be"))
    }

    // ----- 2페이지: 항해 중인 바다 -----
    private fun seaPage(time: Float) {
        c.save()
        c.clipRect(200f, HORIZON, 400f, H.toFloat())
        ga = .3f
        var y = HORIZON + 4
        while (y < H) {
            var x = 200 + (((y * 7 - floor(time * 22)) % 30) + 30) % 30 - 30
            while (x < 400) { r(x, y, 7f, 1f, Color.WHITE); x += 30 }
            y += 5
        }
        ga = 1f
        // 고래 (가끔)
        val wt = time % 14
        if (wt < 5) {
            val wx = 250 + wt * 4; val wy = 322f
            ell(wx, wy, 9f, 3f, col("#2a3f5e")); r(wx - 10, wy - 2, 3f, 2f, col("#2a3f5e"))
            if (wt > 1.5f && wt < 3.5f) { ga = .8f; r(wx + 4, wy - 8, 1f, 5f, col("#e9f4f6")); r(wx + 2, wy - 10, 5f, 2f, col("#e9f4f6")); ga = 1f }
        }
        // 갈매기
        for (i in 0 until 3) {
            val gx = 300 + i * 18 + sin(time + i) * 8; val gy = 200 + i * 10 + cos(time * 1.3f + i) * 3
            val f = (floor(time * 4).toInt() + i) % 2
            r(gx - 2, gy - f, 2f, 1f, Color.WHITE); r(gx, gy, 1f, 1f, Color.WHITE); r(gx + 1, gy - f, 2f, 1f, Color.WHITE)
        }
        c.restore()
        bigShip(300f, 300f, time, 1f)
    }

    private fun bigShip(x: Float, y0: Float, time: Float, s: Float) {
        val y = y0 + (sin(time * 1.8f) * 1.2f).roundToInt()
        for (i in 0 until 7) r(x - 18 * s + i, y + i, 36 * s - i * 2, 1f, if (i < 2) col("#8a5530") else col("#6b4022"))
        r(x - 18 * s, y - 1, 36 * s, 1f, col("#a8703f")); r(x + 14 * s, y - 3, 6 * s, 2f, col("#6b4022"))
        var i = -12; while (i <= 12) { r(x + i * s, y + 2, 2f, 2f, col("#3a2412")); i += 6 }
        val masts = floatArrayOf(-10f, 0f, 10f)
        for (k in 0 until 3) {
            val m = masts[k]
            val mh = (if (k == 1) 40f else 32f) * s
            r(x + m * s, y - mh, 1f, mh, col("#4a2c16"))
            val sw = (if (k == 1) 14f else 11f) * s; val sh = (if (k == 1) 11f else 9f) * s
            for (j in 0 until 2) {
                val top = y - mh + 3 + j * (sh + 3)
                r(x + m * s - sw / 2, top, sw, sh, col("#f6f0de"))
                r(x + m * s + sw / 2 - 2, top + 1, 2f, sh - 2, col("#e2d8bc"))
            }
        }
        r(x, y - 42 * s - 5, 6f, 3f, col("#c0463a")); r(x, y - 42 * s - 5, 1f, 5f, col("#4a2c16"))
        ga = .7f
        for (k in 1 until 6) { val xx = x - 20 * s - k * 8 - ((time * 20) % 8); r(xx, y + 6 + (k % 2), 5f, 1f, Color.WHITE) }
        ga = 1f
    }

    // ----- 3페이지: 항구 -----
    private fun port(time: Float) {
        val ox = 400f
        c.save(); c.translate(ox, OY)
        ell(100f, 104f, 76f, 40f, col("#e9f4f6")); ell(100f, 104f, 74f, 39f, col("#e6cf8f"))
        ell(104f, 100f, 64f, 33f, col("#7aa85a")); ell(96f, 96f, 46f, 22f, col("#86b464"))
        r(118f, 62f, 22f, 14f, col("#cfc6b4"))
        r(118f, 58f, 4f, 4f, col("#cfc6b4")); r(127f, 58f, 4f, 4f, col("#cfc6b4")); r(136f, 58f, 4f, 4f, col("#cfc6b4"))
        r(126f, 68f, 6f, 8f, col("#5a4a3a")); r(128f, 46f, 1f, 12f, col("#4a2c16")); r(129f, 46f, 6f, 4f, col("#2f6fb8"))
        val awn = listOf("#c0463a", "#2f6fb8", "#e0a030", "#4a9a5a")
        for (i in awn.indices) {
            val x = 56f + i * 15; val y = 96f + (i % 2) * 6
            r(x, y - 6, 12f, 6f, col("#e8d6b0"))
            var k = 0; while (k < 12) { r(x + k, y - 9, 2f, 3f, if (k % 4 != 0) col("#f4f1e6") else col(awn[i])); k += 2 }
            r(x + 2, y - 3, 3f, 3f, col("#a0683a")); r(x + 7, y - 3, 3f, 3f, col("#d9b86a"))
        }
        r(40f, 84f, 14f, 9f, col("#9a6236")); r(39f, 82f, 16f, 2f, col("#5d3a1f"))
        r(146f, 92f, 14f, 9f, col("#9a6236")); r(145f, 90f, 16f, 2f, col("#5d3a1f"))
        for (i in 0 until 7) {
            val dirSign = if (i % 2 == 1) 1 else -1
            val raw = (i * 23 + floor(time * 6).toInt() * dirSign) % 60
            val x = 60f + ((raw + 60) % 60); val y = 108f + (i % 3) * 4
            r(x, y - 5, 3f, 2f, col("#f2c79a")); r(x, y - 3, 3f, 2f, bodyColors[i % 6])
        }
        r(14f, 110f, 30f, 4f, col("#8a5a32"))
        var x = 16f; while (x < 44) { r(x, 114f, 1f, 5f, col("#5a3a1e")); x += 6 }
        c.restore()
        smallShip(ox + 20, OY + 118, time, col("#2f6fb8"))
        smallShip(ox + 170, OY + 130, time, col("#e0a030"))
    }

    private fun smallShip(x: Float, y0: Float, time: Float, flag: Int) {
        val y = y0 + sin(time * 2 + x).roundToInt()
        r(x - 6, y, 13f, 2f, col("#6b4022")); r(x - 4, y + 2, 9f, 1f, col("#4a2c16"))
        r(x, y - 10, 1f, 10f, col("#4a2c16"))
        r(x - 4, y - 9, 4f, 7f, col("#f4f1e6")); r(x + 1, y - 8, 4f, 6f, col("#f4f1e6"))
        r(x, y - 12, 3f, 2f, flag)
    }

    // ----- 밤 불빛 -----
    private fun lights(time: Float) {
        c.save(); c.translate(0f, OY)
        val save = ga; ga = save * .25f; ell(fireX, fireY - 2, 10f, 6f, col("#ffb347")); ga = save
        flame(time)
        for (i in 0 until houses) { val (x, y) = houseSlots[i]; r(x + 2, y - 3, 2f, 2f, col("#ffd36a")); r(x + 8, y - 3, 2f, 2f, col("#ffd36a")) }
        if (hasStore) r(storeX + 5, storeY - 4, 2f, 2f, col("#ffd36a"))
        for (v in villagers) r(v.x + v.dir * 2, v.y - 4, 1f, 1f, col("#ffe08a"))
        if (hasDock) r(197f, 95f, 3f, 2f, col("#ffd36a"))
        // 항구
        for (i in 0 until 4) r(400f + 60 + i * 15, 93f + (i % 2) * 6, 2f, 2f, col("#ffd36a"))
        r(526f, 64f, 2f, 2f, col("#ffd36a")); r(446f, 88f, 2f, 2f, col("#ffd36a")); r(552f, 96f, 2f, 2f, col("#ffd36a"))
        c.restore()
        // 배 등불
        r(283f, 296f, 2f, 2f, col("#ffd36a")); r(316f, 295f, 2f, 2f, col("#ffd36a"))
    }
}
