package com.littleisland.game

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.floor
import kotlin.math.min

/**
 * 섬의 진행 상황. 배경화면과 앱이 같은 값을 함께 쓴다.
 * 휴대폰에 저장되어 껐다 켜도 남고, 안 보는 동안 모인 자원도 계산한다.
 */
object Game {
    val HOUSE_COST = listOf(20 to 0, 40 to 10, 80 to 30)
    val STORE_COST = listOf(30 to 10, 80 to 40)
    val DOCK_COST = 60 to 20
    val CAPS = listOf(50, 150, 400)
    const val SHIP_EVERY = 75f          // 부두가 있으면 이 초마다 배가 짐을 놓고 감
    private const val OFFLINE_MAX = 8 * 3600f  // 최대 8시간까지 쌓임
    const val RESIDENTS = 3                    // 섬 주민은 늘 3명
    const val WORK_MS = 6 * 3600 * 1000L       // 알바가 일하는 시간
    // 항해 일정 (출발 후 초)
    const val BATTLE_AT = 85f                   // 해적 전투 결과
    const val PORT_AT = 150f                    // 항구 도착, 짐 팔기, 알바 고용
    const val LEAVE_AT = 210f                   // 항구 출발
    const val HOME_AT = 360f                    // 섬 도착
    const val WAGE = 10                         // 알바 한 명 고용비 (금화)
    const val LOAD_W = 20                       // 한 번에 싣는 나무
    const val LOAD_S = 10                       // 한 번에 싣는 돌

    var wood = 12; private set
    var stone = 4; private set
    var houses = 0; private set
    var storage = 0; private set
    var dock = false; private set
    var shipT = 0f
    private var last = 0L
    /** 일하고 있는 알바들이 집에 돌아가는 시각 */
    val workers = ArrayList<Long>()
    var gold = 0; private set
    // 항해 중인 배
    var voyStart = 0L; private set        // 0이면 섬에 있음
    var voyWin = true; private set        // 출발할 때 미리 정해 둔 해적 전투 결과
    private var voyStep = 0               // 0 항해, 1 전투 끝, 2 항구 끝
    private var cargoW = 0; private var cargoS = 0
    private var voyGold = 0; private var voyHire = 0

    // ---------- 이야기 진행 ----------
    private val flags = HashSet<String>()     // 일어난 사건 (house_1, sail_1, main_05 …)
    private val opened = HashSet<String>()    // 열린 화
    private val read = HashSet<String>()      // 읽은 화
    var voyages = 0; private set

    fun has(f: String): Boolean = when {
        f.isEmpty() || f == "start" -> true
        f.startsWith("voyages_") -> voyages >= (f.removePrefix("voyages_").toIntOrNull() ?: Int.MAX_VALUE)
        else -> f in flags
    }
    fun flag(f: String) { if (flags.add(f)) storyDirty = true }
    fun isOpen(id: String) = id in opened
    fun open(id: String) { opened.add(id) }
    fun isRead(id: String) = id in read
    fun markRead(id: String) { if (read.add(id)) save() }
    fun unread() = opened.count { it !in read }
    private var storyDirty = true

    /** 새 사건이 생겼으면 이야기를 열고 카드로 알린다 */
    fun checkStory() {
        if (!storyDirty) return
        storyDirty = false
        val got = Story.refresh().filter { it.kind != Story.VOYAGE }
        if (got.isEmpty()) return
        val first = got.first()
        if (got.size == 1) card("새 일지 · " + first.label, Story.fill(first.news))
        else card("새 일지 " + got.size + "편", Story.fill(got.last().news))
        save()
    }

    /** 결과 카드 (제목, 내용). 배경화면이 하나씩 보여준다 */
    val cards = ArrayDeque<Pair<String, String>>()
    /** 앱에 보여줄 최근 항해 일지 */
    val log = ArrayList<String>()
    private fun card(t: String, m: String) {
        cards.addLast(t to m); while (cards.size > 6) cards.removeFirst()
        log.add(0, t + " " + m); while (log.size > 4) log.removeAt(log.size - 1)
    }
    /** 새로 도착한 알바 수 (배경화면이 "+알바"를 띄우고 0으로 돌림) */
    var arrived = 0

    /** 비운 동안 모인 결과 (배경화면 카드/앱 안내에 한 번 보여주고 비움) */
    class Report(val minutes: Int, val wood: Int, val stone: Int, val full: Boolean)
    var report: Report? = null

    private var prefs: SharedPreferences? = null
    private var dirty = 0f

    fun cap() = CAPS[storage]
    fun pop() = RESIDENTS + workers.size
    fun beds() = houses
    /** 출발한 지 몇 초 지났는지. 섬에 있으면 -1 */
    fun voyT(now: Long = System.currentTimeMillis()): Float = if (voyStart == 0L) -1f else (now - voyStart) / 1000f
    fun canSail() = dock && voyStart == 0L && (wood > 0 || stone > 0)

    /** 짐을 싣고 항구로 떠난다. 해적 전투는 자동이고 결과만 카드로 나온다 */
    fun sail(): Boolean {
        if (!canSail()) return false
        cargoW = min(wood, LOAD_W); cargoS = min(stone, LOAD_S)
        wood -= cargoW; stone -= cargoS
        voyWin = Math.random() < 0.7
        voyStep = 0; voyGold = 0; voyHire = 0
        voyStart = System.currentTimeMillis()
        flag("sail_1"); checkStory()
        save(); return true
    }

    private fun updateVoyage(now: Long) {
        if (voyStart == 0L) return
        val t = voyT(now)
        if (voyStep == 0 && t >= BATTLE_AT) {
            voyStep = 1
            flag("pirate_1")
            if (voyWin) {
                val g = 10 + (Math.random() * 11).toInt()
                voyGold += g
                card("해적을 물리쳤어요!", "해적선의 보물 금화 +" + g)
            } else {
                val lw = (cargoW * .4f).toInt(); val ls = (cargoS * .4f).toInt()
                cargoW -= lw; cargoS -= ls
                card("해적에게 당했어요", "짐을 빼앗겼어요 나무 -" + lw + " 돌 -" + ls)
            }
        }
        if (voyStep == 1 && t >= PORT_AT) {
            voyStep = 2
            flag("sale_1")
            val sale = cargoW + cargoS * 2
            voyGold += sale
            val free = houses - workers.size
            if (free > 0 && gold + voyGold >= WAGE) { voyHire = 1; voyGold -= WAGE }
            card("항구에 도착했어요", "짐을 팔아 금화 +" + sale + if (voyHire > 0) ", 알바 1명 고용" else if (free <= 0) " (알바 숙소 없음)" else "")
        }
        if (t >= HOME_AT) {
            val homeAt = voyStart + (HOME_AT * 1000).toLong()
            gold += voyGold
            repeat(voyHire) { workers.add(homeAt + WORK_MS) }
            arrived += voyHire
            card("배가 돌아왔어요", "금화 +" + voyGold + if (voyHire > 0) ", 알바 +" + voyHire else "")
            voyages++
            if (voyHire > 0) flag("worker_arrive_1")
            Story.pickVoyageEvent()?.let { ev -> open(ev.id); card("항해 기록 · " + ev.title, Story.fill(ev.news)) }
            voyStart = 0L; voyStep = 0; cargoW = 0; cargoS = 0; voyGold = 0; voyHire = 0
        }
    }

    private fun updateWorkers(now: Long) {
        val before = voyStep to voyStart
        updateVoyage(now)
        val changed = workers.removeAll { it <= now }
        if (changed) flag("worker_leave_1")
        checkStory()
        if (changed || before != (voyStep to voyStart)) save()
    }

    fun init(c: Context) {
        if (prefs != null) return
        val sp = c.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = sp
        wood = sp.getInt("wood", 12); stone = sp.getInt("stone", 4)
        houses = sp.getInt("houses", 0); storage = sp.getInt("storage", 0)
        dock = sp.getBoolean("dock", false); shipT = sp.getFloat("shipT", 0f)
        last = sp.getLong("last", System.currentTimeMillis())
        gold = sp.getInt("gold", 0)
        voyStart = sp.getLong("voyStart", 0L); voyWin = sp.getBoolean("voyWin", true)
        voyStep = sp.getInt("voyStep", 0); cargoW = sp.getInt("cargoW", 0); cargoS = sp.getInt("cargoS", 0)
        voyGold = sp.getInt("voyGold", 0); voyHire = sp.getInt("voyHire", 0)
        voyages = sp.getInt("voyages", 0)
        fun set(k: String, h: HashSet<String>) { h.clear(); sp.getString(k, "")!!.split(",").filter { it.isNotEmpty() }.forEach { h.add(it) } }
        set("flags", flags); set("opened", opened); set("read", read)
        // 이야기 이전부터 하던 사람: 이미 지은 것은 일어난 일로 인정
        if (houses >= 1) flags.add("house_1")
        if (storage >= 1) flags.add("store_1")
        if (dock) flags.add("dock")
        Story.load(c)
        storyDirty = true
        workers.clear()
        sp.getString("workers", "")!!.split(",").mapNotNull { it.toLongOrNull() }.forEach { workers.add(it) }
    }

    fun save() {
        dirty = 0f
        last = System.currentTimeMillis()
        prefs?.edit()?.putInt("wood", wood)?.putInt("stone", stone)?.putInt("houses", houses)
            ?.putInt("storage", storage)?.putBoolean("dock", dock)?.putFloat("shipT", shipT)
            ?.putLong("last", last)?.putInt("gold", gold)
            ?.putLong("voyStart", voyStart)?.putBoolean("voyWin", voyWin)?.putInt("voyStep", voyStep)
            ?.putInt("cargoW", cargoW)?.putInt("cargoS", cargoS)?.putInt("voyGold", voyGold)?.putInt("voyHire", voyHire)
            ?.putInt("voyages", voyages)?.putString("flags", flags.joinToString(","))
            ?.putString("opened", opened.joinToString(","))?.putString("read", read.joinToString(","))
            ?.putString("workers", workers.joinToString(","))?.apply()
    }

    /** 자원을 넣는다. 창고가 차면 넘치는 건 버려진다. 실제로 들어간 양을 돌려준다. */
    fun add(w: Int, s: Int): Pair<Int, Int> {
        val bw = wood; val bs = stone
        wood = min(cap(), wood + w); stone = min(cap(), stone + s)
        return (wood - bw) to (stone - bs)
    }

    /** 화면이 그려지는 동안 매 프레임 부른다. 몇 초마다 저장. */
    fun tick(dt: Float) {
        last = System.currentTimeMillis()
        updateWorkers(last)
        dirty += dt
        if (dirty > 10f) save()
    }

    /** 다시 보이기 시작할 때 부른다. 안 보던 시간만큼 자원을 쌓는다. */
    fun catchUp() {
        val now = System.currentTimeMillis()
        val secs = min((now - last) / 1000f, OFFLINE_MAX)
        last = now
        if (secs < 20f) return
        // 주민은 내내, 알바는 일한 시간만큼만 센다
        val from = now - (secs * 1000).toLong()
        var workSecs = RESIDENTS * secs
        for (w in workers) workSecs += ((min(w, now) - from) / 1000f).coerceIn(0f, secs)
        updateWorkers(now)
        val per = workSecs / 11f * 0.6f
        var w = floor(per * .68f).toInt(); var s = floor(per * .32f).toInt()
        if (dock) {
            val n = floor((shipT + secs) / SHIP_EVERY).toInt()
            shipT = (shipT + secs) % SHIP_EVERY
            w += n * 15; s += n * 7
        }
        val (gw, gs) = add(w, s)
        val full = wood >= cap() || stone >= cap()
        if (gw + gs > 0 || full) report = Report((secs / 60).toInt(), gw, gs, full)
        save()
    }

    // ---------- 건물 ----------
    fun houseCost() = HOUSE_COST.getOrNull(houses)
    fun storeCost() = STORE_COST.getOrNull(storage)
    fun dockCost() = if (dock) null else DOCK_COST

    private fun pay(c: Pair<Int, Int>?): Boolean {
        if (c == null || wood < c.first || stone < c.second) return false
        wood -= c.first; stone -= c.second
        return true
    }

    fun buildHouse(): Boolean { if (!pay(houseCost())) return false; houses++; flag("house_1"); checkStory(); save(); return true }
    fun buildStore(): Boolean { if (!pay(storeCost())) return false; storage++; flag("store_1"); checkStory(); save(); return true }
    fun buildDock(): Boolean { if (!pay(dockCost())) return false; dock = true; shipT = SHIP_EVERY - 8; flag("dock"); checkStory(); save(); return true }

    /** 주민이 처음으로 나무나 돌을 창고에 넣었을 때 */
    fun gathered() { if ("first_gather" !in flags) flag("first_gather") }

    fun reset() {
        wood = 12; stone = 4; houses = 0; storage = 0; dock = false; shipT = 0f; report = null
        workers.clear(); gold = 0; voyStart = 0L; voyStep = 0; cards.clear(); log.clear()
        flags.clear(); opened.clear(); read.clear(); voyages = 0; storyDirty = true
        save()
    }
}
