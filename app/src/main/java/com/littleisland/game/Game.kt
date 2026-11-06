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

    var wood = 12; private set
    var stone = 4; private set
    var houses = 0; private set
    var storage = 0; private set
    var dock = false; private set
    var shipT = 0f
    private var last = 0L

    /** 비운 동안 모인 결과 (배경화면 카드/앱 안내에 한 번 보여주고 비움) */
    class Report(val minutes: Int, val wood: Int, val stone: Int, val full: Boolean)
    var report: Report? = null

    private var prefs: SharedPreferences? = null
    private var dirty = 0f

    fun cap() = CAPS[storage]
    fun pop() = 3 + houses

    fun init(c: Context) {
        if (prefs != null) return
        val sp = c.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = sp
        wood = sp.getInt("wood", 12); stone = sp.getInt("stone", 4)
        houses = sp.getInt("houses", 0); storage = sp.getInt("storage", 0)
        dock = sp.getBoolean("dock", false); shipT = sp.getFloat("shipT", 0f)
        last = sp.getLong("last", System.currentTimeMillis())
    }

    fun save() {
        dirty = 0f
        last = System.currentTimeMillis()
        prefs?.edit()?.putInt("wood", wood)?.putInt("stone", stone)?.putInt("houses", houses)
            ?.putInt("storage", storage)?.putBoolean("dock", dock)?.putFloat("shipT", shipT)
            ?.putLong("last", last)?.apply()
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
        dirty += dt
        if (dirty > 10f) save()
    }

    /** 다시 보이기 시작할 때 부른다. 안 보던 시간만큼 자원을 쌓는다. */
    fun catchUp() {
        val now = System.currentTimeMillis()
        val secs = min((now - last) / 1000f, OFFLINE_MAX)
        last = now
        if (secs < 20f) return
        val per = pop() / 11f * 0.6f * secs
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
        save()
    }
}
