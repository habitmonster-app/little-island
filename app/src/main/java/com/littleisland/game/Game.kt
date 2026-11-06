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
    const val TRIP_MS = 3 * 60 * 1000L         // 알바 구하러 다녀오는 시간
    const val WORK_MS = 6 * 3600 * 1000L       // 알바가 일하는 시간
    val HIRE_COST = 15 to 5

    var wood = 12; private set
    var stone = 4; private set
    var houses = 0; private set
    var storage = 0; private set
    var dock = false; private set
    var shipT = 0f
    private var last = 0L
    /** 일하고 있는 알바들이 집에 돌아가는 시각 */
    val workers = ArrayList<Long>()
    /** 알바 구하러 간 배가 돌아오는 시각 (0이면 안 감) */
    var tripEnd = 0L; private set
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
    fun canHire() = dock && tripEnd == 0L && workers.size < houses &&
        wood >= HIRE_COST.first && stone >= HIRE_COST.second

    /** 부두에서 배를 보내 다른 마을 알바를 구해 온다 */
    fun hire(): Boolean {
        if (!canHire()) return false
        wood -= HIRE_COST.first; stone -= HIRE_COST.second
        tripEnd = System.currentTimeMillis() + TRIP_MS
        save(); return true
    }

    /** 배 도착·알바 퇴근 처리. 알바 수가 바뀌면 true */
    private fun updateWorkers(now: Long): Boolean {
        var changed = false
        if (tripEnd != 0L && now >= tripEnd) {
            if (workers.size < houses) { workers.add(tripEnd + WORK_MS); arrived++ }
            tripEnd = 0L; changed = true
        }
        if (workers.removeAll { it <= now }) changed = true
        if (changed) save()
        return changed
    }

    fun init(c: Context) {
        if (prefs != null) return
        val sp = c.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = sp
        wood = sp.getInt("wood", 12); stone = sp.getInt("stone", 4)
        houses = sp.getInt("houses", 0); storage = sp.getInt("storage", 0)
        dock = sp.getBoolean("dock", false); shipT = sp.getFloat("shipT", 0f)
        last = sp.getLong("last", System.currentTimeMillis())
        tripEnd = sp.getLong("tripEnd", 0L)
        workers.clear()
        sp.getString("workers", "")!!.split(",").mapNotNull { it.toLongOrNull() }.forEach { workers.add(it) }
    }

    fun save() {
        dirty = 0f
        last = System.currentTimeMillis()
        prefs?.edit()?.putInt("wood", wood)?.putInt("stone", stone)?.putInt("houses", houses)
            ?.putInt("storage", storage)?.putBoolean("dock", dock)?.putFloat("shipT", shipT)
            ?.putLong("last", last)?.putLong("tripEnd", tripEnd)
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
        if (tripEnd != 0L && tripEnd < now && workers.size < houses) {
            workSecs += ((now - tripEnd) / 1000f).coerceIn(0f, secs)
        }
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

    fun buildHouse(): Boolean { if (!pay(houseCost())) return false; houses++; save(); return true }
    fun buildStore(): Boolean { if (!pay(storeCost())) return false; storage++; save(); return true }
    fun buildDock(): Boolean { if (!pay(dockCost())) return false; dock = true; shipT = SHIP_EVERY - 8; save(); return true }

    fun reset() {
        wood = 12; stone = 4; houses = 0; storage = 0; dock = false; shipT = 0f; report = null
        workers.clear(); tripEnd = 0L
        save()
    }
}
