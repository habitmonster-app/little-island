package com.littleisland.game

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 섬의 진행 상황과 시뮬레이션. 배경화면·앱·부재 정산이 모두 같은 advance()를 쓴다.
 * 수치는 Data(아스트라 기획 데이터)에서 가져온다.
 */
object Game {
    const val RESIDENTS = 3
    private const val OFFLINE_MAX_MS = 8 * 3600 * 1000L
    private const val STEP_MS = 10_000L
    private const val HOUR = 3600_000L

    // ---------- 자원·건물 ----------
    var wood = 12; private set
    var stone = 4; private set
    private var goldC = 0L                     // 금화 1/100 단위
    val gold get() = (goldC / 100).toInt()
    private var fracW = 0.0; private var fracS = 0.0
    var houses = 0; private set
    var warehouse = 0; private set
    var dock = false; private set
    var field = false; private set
    var lighthouse = false; private set
    var market = false; private set
    var shipLevel = 1; private set
    var route = "port_haesol"; private set
    var autoSail = true
    var autoHire = true
    var autoBuild = true                       // 금화가 안 드는 기본 시설은 알아서 짓기
    var hireAny = false                        // 비싼 알바도 자동 고용
    var supplyT = 0f; private set              // 부두 짐배 주기
    var supplies = 0                           // 짐배 도착 수 (화면 연출용, Scene이 0으로 돌림)
    private var hireSpent = 0; private var hireDay = -1L

    fun cap() = Data.capacity(warehouse)
    fun ship() = Data.ship(shipLevel)
    fun port() = Data.port(route)

    // ---------- 알바 ----------
    class Contract(val id: String, val expire: Long)
    val workers = ArrayList<Contract>()
    private val completed = HashMap<String, Int>()
    var arrived = 0
    fun pop() = RESIDENTS + workers.size
    fun beds() = houses
    fun completedOf(id: String) = completed[id] ?: 0

    // ---------- 항해 ----------
    class Voyage(var start: Long, val dur: Float, val port: String, var cw: Int, var cs: Int, val encounter: Boolean,
                 val pirate: String?, val win: Boolean, var step: Int, var rewardC: Long, var hire: String?, var saleC: Long)
    var voy: Voyage? = null; private set
    var voyages = 0; private set
    var mapPieces = 0; private set            // 홈 화면 톡 기믹으로 모으는 지도 조각
    var finds = 0; private set                // 톡으로 주운 횟수
    var treasurePending = false; private set  // 지도를 맞추면 다음 항해에 보물섬에 들른다
    var treasures = 0; private set
    const val MAP_NEED = 8
    var packed = false; private set          // 짐 싣기 퍼즐을 풀면 다음 판매 +20%
    const val PACK_BONUS = .2f
    var lastBattle = ""; private set          // "pirate_win" / "pirate_lose" (말풍선용)
    var lastBattleAt = 0L; private set
    var lastReturnAt = 0L; private set

    /** 출발 후 지난 초. 섬에 있으면 -1 */
    fun voyT(now: Long = last): Float = voy?.let { (now - it.start) / 1000f } ?: -1f
    fun voyFrac(now: Long = last): Float = voy?.let { ((now - it.start) / 1000f / it.dur).coerceIn(0f, 1f) } ?: -1f

    // ---------- 이야기 ----------
    private val flags = HashSet<String>()
    private val opened = HashSet<String>()
    private val read = HashSet<String>()
    private val eventLast = HashMap<String, Int>()
    private var lastEvent = ""
    fun has(f: String): Boolean = when {
        f.isEmpty() || f == "start" -> true
        f.startsWith("voyages_") -> voyages >= (f.removePrefix("voyages_").toIntOrNull() ?: Int.MAX_VALUE)
        else -> f in flags
    }
    fun hasAll(l: List<String>) = l.all { has(it) }
    fun flag(f: String) { if (flags.add(f)) storyDirty = true }
    fun isOpen(id: String) = id in opened
    fun open(id: String) { opened.add(id) }
    fun isRead(id: String) = id in read
    fun markRead(id: String) { if (read.add(id)) save() }
    fun unread() = opened.count { it !in read }
    private var storyDirty = true

    // ---------- 카드 ----------
    class Report(val minutes: Int, val wood: Int, val stone: Int, val gold: Int, val voyages: Int, val full: Boolean)
    var report: Report? = null
    val cards = ArrayDeque<Pair<String, String>>()
    val log = ArrayList<String>()
    private fun card(t: String, m: String) {
        cards.addLast(t to m); while (cards.size > 8) cards.removeFirst()
        log.add(0, t + " " + m); while (log.size > 6) log.removeAt(log.size - 1)
    }

    // 이야기 자리표시자용
    val LOAD_W get() = ship().cargoW
    val LOAD_S get() = ship().cargoS
    val WAGE get() = Data.worker("yeoul")?.wage ?: 10
    const val WORK_MS = 6 * HOUR

    private var prefs: SharedPreferences? = null
    private var last = 0L
    private var saveT = 0f

    // =====================================================================
    fun init(c: Context) {
        if (prefs != null) return
        Data.load(c); Story.load(c); Art.init(c)
        val sp = c.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = sp
        wood = sp.getInt("wood", 12); stone = sp.getInt("stone", 4)
        houses = sp.getInt("houses", 0)
        warehouse = if (sp.contains("warehouse")) sp.getInt("warehouse", 0) else sp.getInt("storage", 0)
        dock = sp.getBoolean("dock", false); field = sp.getBoolean("field", false)
        lighthouse = sp.getBoolean("lighthouse", false); market = sp.getBoolean("market", false)
        shipLevel = sp.getInt("shipLevel", 1); route = sp.getString("route", "port_haesol")!!
        autoSail = sp.getBoolean("autoSail", true); autoHire = sp.getBoolean("autoHire", true); autoBuild = sp.getBoolean("autoBuild", true); hireAny = sp.getBoolean("hireAny", false)
        supplyT = sp.getFloat("supplyT", sp.getFloat("shipT", 0f))
        hireSpent = sp.getInt("hireSpent", 0); hireDay = sp.getLong("hireDay", -1L)
        last = sp.getLong("last", System.currentTimeMillis())
        voyages = sp.getInt("voyages", 0); mapPieces = sp.getInt("mapPieces", 0); treasurePending = sp.getBoolean("treasurePending", false); treasures = sp.getInt("treasures", 0); packed = sp.getBoolean("packed", false); finds = sp.getInt("finds", 0)
        fun set(k: String, h: HashSet<String>) { h.clear(); sp.getString(k, "")!!.split(",").filter { it.isNotEmpty() }.forEach { h.add(it) } }
        set("flags", flags); set("opened", opened); set("read", read)
        sp.getString("completed", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); completed[k] = v.toIntOrNull() ?: 0 }
        sp.getString("eventLast", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); eventLast[k] = v.toIntOrNull() ?: 0 }
        lastEvent = sp.getString("lastEvent", "")!!

        // 금화: 예전엔 정수, 이제 1/100 단위
        goldC = if (sp.contains("goldC")) sp.getLong("goldC", 0L) else sp.getInt("gold", 0) * 100L
        // 알바: 예전엔 만료 시각만, 이제 "id:만료"
        workers.clear()
        val raw = sp.getString("workers", "")!!.split(",").filter { it.isNotEmpty() }
        val haesol = listOf("yeoul", "mungi")
        for ((i, w) in raw.withIndex()) {
            if (":" in w) { val (id, e) = w.split(":"); e.toLongOrNull()?.let { workers.add(Contract(id, it)) } }
            else w.toLongOrNull()?.let { e -> haesol.getOrNull(i)?.let { workers.add(Contract(it, e)) } }
        }
        // 항해
        voy = null
        if (sp.contains("v_start")) {
            val st = sp.getLong("v_start", 0L)
            if (st != 0L) voy = Voyage(st, sp.getFloat("v_dur", 720f), sp.getString("v_port", route)!!, sp.getInt("v_cw", 0), sp.getInt("v_cs", 0),
                sp.getBoolean("v_enc", false), sp.getString("v_pirate", null), sp.getBoolean("v_win", true), sp.getInt("v_step", 0),
                sp.getLong("v_reward", 0L), sp.getString("v_hire", null), sp.getLong("v_sale", 0L))
        } else if (sp.getLong("voyStart", 0L) != 0L) {
            // 빌드 5~6의 진행 중 항해는 정산만 하고 마친다
            goldC += sp.getInt("voyGold", 0) * 100L
        }
        // 이미 지은 것은 일어난 일로 인정
        if (houses >= 1) flags.add("house_1")
        if (warehouse >= 1) flags.add("store_1")
        if (dock) flags.add("dock")
        if (shipLevel >= 2) flags.add("ship_2")
        if (shipLevel >= 3) flags.add("ship_3")
        if (shipLevel >= 4) flags.add("ship_4")
        storyDirty = true
    }

    fun save() {
        saveT = 0f
        val v = voy
        prefs?.edit()?.apply {
            putInt("wood", wood); putInt("stone", stone); putLong("goldC", goldC)
            putInt("houses", houses); putInt("warehouse", warehouse); putInt("storage", min(warehouse, 2))
            putBoolean("dock", dock); putBoolean("field", field); putBoolean("lighthouse", lighthouse); putBoolean("market", market)
            putInt("shipLevel", shipLevel); putString("route", route)
            putBoolean("autoSail", autoSail); putBoolean("autoHire", autoHire); putBoolean("autoBuild", autoBuild); putBoolean("hireAny", hireAny)
            putFloat("supplyT", supplyT); putInt("hireSpent", hireSpent); putLong("hireDay", hireDay)
            putLong("last", last); putInt("voyages", voyages); putInt("mapPieces", mapPieces); putBoolean("treasurePending", treasurePending); putInt("treasures", treasures); putBoolean("packed", packed); putInt("finds", finds)
            putString("flags", flags.joinToString(",")); putString("opened", opened.joinToString(",")); putString("read", read.joinToString(","))
            putString("completed", completed.entries.joinToString(",") { it.key + ":" + it.value })
            putString("eventLast", eventLast.entries.joinToString(",") { it.key + ":" + it.value })
            putString("lastEvent", lastEvent)
            putString("workers", workers.joinToString(",") { it.id + ":" + it.expire })
            putLong("voyStart", 0L)
            if (v == null) putLong("v_start", 0L) else {
                putLong("v_start", v.start); putFloat("v_dur", v.dur); putString("v_port", v.port); putInt("v_cw", v.cw); putInt("v_cs", v.cs)
                putBoolean("v_enc", v.encounter); putString("v_pirate", v.pirate); putBoolean("v_win", v.win); putInt("v_step", v.step)
                putLong("v_reward", v.rewardC); putString("v_hire", v.hire); putLong("v_sale", v.saleC)
            }
            apply()
        }
    }

    // =====================================================================
    // 시뮬레이션

    /** 화면이 그려지는 동안 매 프레임 부른다. */
    fun tick(dt: Float) {
        advance(System.currentTimeMillis())
        saveT += dt
        if (saveT > 10f) save()
    }

    /** 다시 보이기 시작할 때. 안 보던 시간(최대 8시간)을 시간 순서대로 계산하고 결과 카드를 만든다. */
    fun catchUp() {
        val now = System.currentTimeMillis()
        val gap = now - last
        if (gap < 20_000L) { advance(now); return }
        val w0 = wood; val s0 = stone; val g0 = gold; val v0 = voyages
        val credited = min(gap, OFFLINE_MAX_MS)
        advance(now)
        val full = wood >= cap() || stone >= cap()
        report = Report((credited / 60000L).toInt(), max(0, wood - w0), max(0, stone - s0), max(0, gold - g0), voyages - v0, full)
        save()
    }

    fun advance(now: Long) {
        if (now <= last) return
        val gap = now - last
        if (gap > OFFLINE_MAX_MS) {
            // 8시간을 넘는 부분은 계산하지 않고 건너뛴다. 항해는 그동안 멈춰 있던 것으로 본다.
            val skip = gap - OFFLINE_MAX_MS
            voy?.let { it.start += skip }
            last += skip
        }
        while (last < now) {
            val step = min(STEP_MS, now - last)
            step(last + step, step / 1000f)
            last += step
        }
        checkStory()
    }

    private fun step(t: Long, dt: Float) {
        // 알바 계약 종료
        val it = workers.iterator()
        while (it.hasNext()) {
            val w = it.next()
            if (w.expire <= t) {
                it.remove()
                completed[w.id] = completedOf(w.id) + 1
                flag("worker_leave_1")
                Data.worker(w.id)?.let { d -> card("알바가 떠났어요", d.name + "의 계약이 끝났어요") }
            }
        }
        // 채집: 주민은 기본 속도, 알바는 각자 보너스 (밭이 있으면 알바만 조금 더)
        var rw = RESIDENTS * Data.gatherRate * Data.woodFrac
        var rs = RESIDENTS * Data.gatherRate * Data.stoneFrac
        val fm = if (field) 1f + Data.fieldBonus else 1f
        for (w in workers) {
            val d = Data.worker(w.id) ?: continue
            rw += Data.gatherRate * Data.woodFrac * (1 + d.woodBonus) * fm
            rs += Data.gatherRate * Data.stoneFrac * (1 + d.stoneBonus) * fm
        }
        fracW += rw * dt; fracS += rs * dt
        val gw = floor(fracW).toInt(); val gs = floor(fracS).toInt()
        fracW -= gw; fracS -= gs
        if (gw + gs > 0) { add(gw, gs); flag("first_gather") }

        // 부두 짐배
        if (dock) {
            supplyT += dt
            if (supplyT >= Data.supplyEvery) {
                supplyT -= Data.supplyEvery
                add(Random.nextInt(Data.supplyW[0], Data.supplyW[1] + 1), Random.nextInt(Data.supplyS[0], Data.supplyS[1] + 1))
                supplies++
            }
        }

        if (autoBuild) autoBuildStep()

        // 항해
        val v = voy
        if (v == null) { if (dock && autoSail && wood >= 1) depart(t) }
        else voyageStep(v, t)
    }

    /** 창고 칸까지만 넣는다. 이미 넘게 가진 건 버리지 않는다. */
    fun add(w: Int, s: Int): Pair<Int, Int> {
        val bw = wood; val bs = stone
        if (wood < cap()) wood = min(cap(), wood + w)
        if (stone < cap()) stone = min(cap(), stone + s)
        return (wood - bw) to (stone - bs)
    }

    // ---------- 항해 ----------
    fun canSail() = dock && voy == null && wood >= 1

    /** 앱의 출항 버튼 (자동 출항을 꺼 두었을 때) */
    fun sail(): Boolean { if (!canSail()) return false; depart(max(last, System.currentTimeMillis())); save(); return true }

    fun routeOk(p: Data.Port) = (p.id == "port_haesol" || has(p.id)) && shipLevel >= p.minShip
    fun setRoute(id: String): Boolean { val p = Data.ports.firstOrNull { it.id == id } ?: return false; if (!routeOk(p)) return false; route = id; save(); return true }

    private fun workerPirateBonus() = min(Data.pirateCap, workers.sumOf { (Data.worker(it.id)?.pirateBonus ?: 0f).toDouble() }.toFloat())
    fun workerSaleBonus() = min(Data.saleCap, workers.sumOf { (Data.worker(it.id)?.saleBonus ?: 0f).toDouble() }.toFloat())

    fun winChance(p: Data.Pirate): Float = (ship().pirateWin - Data.powerCoef * (p.power - 1f) +
        (if (lighthouse) Data.lighthouseBonus else 0f) + workerPirateBonus()).coerceIn(Data.winMin, Data.winMax)

    fun voyageSeconds(p: Data.Port = port()): Float =
        ceil(Data.baseSeconds * p.distance / ship().speed).coerceIn(Data.minSeconds, Data.maxSeconds)

    private fun depart(t: Long) {
        var p = port()
        if (!routeOk(p)) { route = "port_haesol"; p = port() }
        val s = ship()
        val cw = min(wood, s.cargoW); val cs = min(stone, s.cargoS)
        wood -= cw; stone -= cs
        flag("sail_1")
        // 해적: 첫 조우는 반드시, 그 뒤는 항구 위험도만큼
        val forced = !has("pirate_1") && !has("main_53")
        val enc = forced || Random.nextFloat() < p.risk
        var pirate: Data.Pirate? = null
        if (enc) {
            pirate = if (forced) Data.pirate("hook_minor") else {
                val ok = Data.pirates.filter { it.normal && it.weight > 0 && has(it.unlock) && hasAll(it.requires) &&
                    it.disabledBy.none { d -> has(d) } && (it.ports == null || p.id in it.ports) }
                pickWeighted(ok) { it.weight }
            }
        }
        val win = pirate?.let { Random.nextFloat() < winChance(it) } ?: true
        voy = Voyage(t, voyageSeconds(p), p.id, cw, cs, pirate != null, pirate?.id, win, 0, 0L, null, 0L)
        storyDirty = true
        save()
    }

    private fun voyageStep(v: Voyage, t: Long) {
        val f = (t - v.start) / 1000f / v.dur
        val p = Data.port(v.port)
        if (v.step == 0 && f >= Data.fPirate) {
            v.step = 1
            if (v.encounter) {
                val pr = Data.pirate(v.pirate ?: "") ?: Data.pirates.firstOrNull()
                flag("pirate_1")
                lastBattle = if (v.win) "pirate_win" else "pirate_lose"; lastBattleAt = t
                if (pr != null) {
                    if (v.win) {
                        val g = Random.nextInt(pr.winGold[0], max(pr.winGold[0], pr.winGold[1]) + 1)
                        v.rewardC = g * 100L
                        card(pr.name + "을 물리쳤어요!", "귀항하면 금화 +$g")
                    } else {
                        val lw = v.cw - floor(v.cw * (1 - pr.lose)).toInt(); val ls = v.cs - floor(v.cs * (1 - pr.lose)).toInt()
                        v.cw -= lw; v.cs -= ls
                        card(pr.name + "에게 당했어요", "짐을 빼앗겼어요 나무 -$lw 돌 -$ls")
                    }
                }
            }
        }
        if (v.step == 1 && f >= Data.fArrive) {
            v.step = 2
            val mul = 1f + (if (market) Data.marketMul - 1f else 0f) + workerSaleBonus() + (if (packed) PACK_BONUS else 0f)
            packed = false
            val sale = floor((v.cw * p.buyW + v.cs * p.buyS) * mul * 100).toLong()
            goldC += sale; v.saleC = sale
            flag("sale_1")
            v.hire = tryHire(p, t)
            v.hire?.let { goldC -= (Data.worker(it)?.wage ?: 0) * 100L }
            card(p.name + "에 도착했어요", "짐을 팔아 금화 +" + sale / 100 + (v.hire?.let { ", " + (Data.worker(it)?.name ?: "") + " 고용" } ?: ""))
        }
        if (v.step == 2 && f >= Data.fLeave) v.step = 3
        if (v.step >= 2 && f >= 1f) returnHome(v, v.start + (v.dur * 1000).toLong())
    }

    private fun tryHire(p: Data.Port, t: Long): String? {
        if (workers.size >= houses) return null
        val gold = goldC / 100
        val busy = workers.map { it.id }.toSet()
        val list = Data.workers.filter { it.port == p.id && has(it.unlock) && it.id !in busy }
        // 첫 알바는 해솔항의 여울로 정해져 있다 (예비금 규칙 예외)
        if (!has("worker_arrive_1")) {
            val y = list.firstOrNull { it.id == "yeoul" } ?: return null
            return if (gold >= y.wage) y.id else null
        }
        if (!autoHire) return null
        val day = t / (24 * HOUR)
        if (day != hireDay) { hireDay = day; hireSpent = 0 }
        val limit = if (hireAny) Int.MAX_VALUE else Data.singleLimit
        val daily = if (hireAny) Int.MAX_VALUE else Data.dailyLimit
        val w = list.firstOrNull { it.wage <= limit && gold - it.wage >= Data.reserveGold && hireSpent + it.wage <= daily } ?: return null
        hireSpent += w.wage
        return w.id
    }

    private fun returnHome(v: Voyage, t: Long) {
        goldC += v.rewardC
        var line = "판매 금화 +" + v.saleC / 100 + (if (v.rewardC > 0) ", 보물 +" + v.rewardC / 100 else "")
        v.hire?.let { id ->
            val d = Data.worker(id)
            workers.add(Contract(id, t + (d?.hours ?: 6) * HOUR))
            arrived++
            flag("worker_arrive_1")
            line += ", " + (d?.name ?: "알바") + " 도착"
        }
        if (treasurePending) {
            treasurePending = false; treasures++
            val g = 150 * shipLevel + 50 * treasures
            goldC += g * 100L; add(30, 15)
            card("보물섬에 다녀왔어요!", "금화 +$g, 나무 +30, 돌 +15")
        }
        voyages++
        voy = null
        lastReturnAt = t
        card("배가 돌아왔어요", line)
        rollEvent(v)
        // 11화부터는 귀항할 때마다 최대 한 화씩
        Story.refresh(true).forEach { announce(it) }
        if (isOpen("main_60")) flag("ending")
        save()
    }

    private fun rollEvent(v: Voyage) {
        if (Random.nextFloat() >= Data.eventChance) return
        val ok = Data.events.filter { e ->
            val story = Story.byId(e.id)
            story != null && has(story.unlock) && has(e.unlock) && hasAll(e.requires) &&
                (e.ports == null || v.port in e.ports) && (!e.needField || field) &&
                (e.repeat || e.id !in eventLast) && voyages - (eventLast[e.id] ?: -999) >= e.cooldown && e.id != lastEvent
        }
        val e = pickWeighted(ok) { it.weight } ?: return
        eventLast[e.id] = voyages; lastEvent = e.id
        if (e.gold > 0) goldC += e.gold * 100L
        if (e.wood > 0 || e.stone > 0) add(e.wood, e.stone)
        val st = Story.byId(e.id) ?: return
        open(st.id)
        card("항해 기록 · " + st.title, Story.fill(st.news))
    }

    private fun <T> pickWeighted(l: List<T>, w: (T) -> Int): T? {
        val sum = l.sumOf { w(it) }
        if (sum <= 0) return null
        var r = Random.nextInt(sum)
        for (x in l) { r -= w(x); if (r < 0) return x }
        return l.last()
    }

    // ---------- 이야기 ----------
    private fun announce(e: Story.Entry) = card("새 일지 · " + e.label, Story.fill(e.news))

    fun checkStory() {
        if (!storyDirty) return
        storyDirty = false
        val got = Story.refresh(false).filter { it.kind != Story.VOYAGE }
        if (got.isEmpty()) return
        if (got.size == 1) announce(got[0])
        else card("새 일지 " + got.size + "편", Story.fill(got.last().news))
        save()
    }

    // =====================================================================
    // 짓기·사기 (앱)

    fun canPay(c: Data.Cost?) = c != null && goldC >= c.gold * 100L && wood >= c.wood && stone >= c.stone
    private fun pay(c: Data.Cost?): Boolean {
        if (!canPay(c)) return false
        goldC -= c!!.gold * 100L; wood -= c.wood; stone -= c.stone
        return true
    }
    private fun done(f: String?) { if (f != null) flag(f); checkStory(); save() }

    fun nextHouse() = Data.houses.getOrNull(houses)
    fun houseReady() = nextHouse()?.let { hasAll(it.requires) } ?: false
    fun buildHouse(): Boolean {
        val b = nextHouse() ?: return false
        if (!houseReady() || !pay(b.cost)) return false
        houses++; done(if (houses == 1) "house_1" else null); return true
    }

    fun nextWarehouse() = Data.warehouse.getOrNull(warehouse + 1)
    fun warehouseReady() = nextWarehouse()?.let { hasAll(it.second.requires) } ?: false
    fun upgradeWarehouse(): Boolean {
        val l = nextWarehouse() ?: return false
        if (!warehouseReady() || !pay(l.second.cost)) return false
        warehouse++; done(if (warehouse == 1) "store_1" else null); return true
    }

    fun dockReady() = !dock && (Data.dock?.let { hasAll(it.requires) } ?: true)
    fun buildDock(): Boolean {
        if (!dockReady() || !pay(Data.dock?.cost ?: Data.Cost(0, 60, 20))) return false
        dock = true; supplyT = Data.supplyEvery - 8; done("dock"); return true
    }

    fun fieldReady() = !field && (Data.field?.let { hasAll(it.requires) } ?: false)
    fun buildField(): Boolean {
        if (!fieldReady() || !pay(Data.field?.cost)) return false
        field = true; done(null); return true
    }

    /** 배 강화·항구 개방·등대·장터: 01 progression 항목 */
    fun unlockReady(u: Data.Unlock) = !has(u.key) && hasAll(u.requires) && warehouse >= u.needWarehouse && u.cost != null
    fun buyUnlock(key: String): Boolean {
        val u = Data.unlock(key) ?: return false
        if (!unlockReady(u) || !pay(u.cost)) return false
        when {
            key.startsWith("ship_") -> shipLevel = key.removePrefix("ship_").toIntOrNull() ?: shipLevel
            key == "lighthouse" -> lighthouse = true
            key == "market" -> market = true
        }
        if (key.startsWith("ship_")) card("배를 고쳤어요", ship().news)
        done(key); return true
    }

    // ---------- 다음 목표 (홈 화면 소식판과 앱 맨 위) ----------

    class Goal(val key: String, val name: String, val cost: Data.Cost?, val ready: Boolean, val basic: Boolean)

    /** 섬이 다음에 할 일. 기본 시설(숙소·창고·부두·밭)을 먼저, 그다음 배와 항구. */
    fun goal(): Goal? {
        val h = nextHouse(); val w = nextWarehouse()
        if (houses == 0 && h != null) return Goal("house", "첫 알바 숙소", h.cost, houseReady(), true)
        if (warehouse == 0 && w != null) return Goal("ware", "창고 1단계", w.second.cost, warehouseReady(), true)
        if (!dock) return Goal("dock", "부두", Data.dock?.cost ?: Data.Cost(0, 60, 20), dockReady(), true)
        if (h != null && houseReady()) return Goal("house", "알바 숙소 " + (houses + 1) + "채", h.cost, true, true)
        if (warehouse < 2 && w != null && warehouseReady()) return Goal("ware", "창고 " + (warehouse + 1) + "단계", w.second.cost, true, true)
        if (fieldReady()) return Goal("field", "작은 밭", Data.field?.cost, true, true)
        val ships = Data.unlock("ship_" + (shipLevel + 1))
        val cands = Data.unlocks.filter { !has(it.key) && it.cost != null && (it.key.startsWith("ship_") || it.key.startsWith("port_") || it.key == "lighthouse" || it.key == "market") }
        val u = cands.firstOrNull { unlockReady(it) } ?: ships?.takeIf { !has(it.key) } ?: return null
        val name = when {
            u.key.startsWith("ship_") -> "배 " + u.key.removePrefix("ship_") + "단계"
            u.key.startsWith("port_") -> Data.port(u.key).name + " 항로"
            u.key == "lighthouse" -> "등대"
            else -> "장터"
        }
        return Goal(u.key, name, u.cost, unlockReady(u), false)
    }

    fun goalProgress(g: Goal): String {
        val c = g.cost ?: return ""
        return listOfNotNull(
            if (c.wood > 0) "나무 " + min(wood, c.wood) + "/" + c.wood else null,
            if (c.stone > 0) "돌 " + min(stone, c.stone) + "/" + c.stone else null,
            if (c.gold > 0) "금화 " + "%,d".format(min(gold, c.gold)) + "/" + "%,d".format(c.gold) else null).joinToString(" · ")
    }

    private fun autoBuildStep() {
        val g = goal() ?: return
        if (!g.basic || !g.ready || (g.cost?.gold ?: 0) > 0 || !canPay(g.cost)) return
        val ok = when (g.key) { "house" -> buildHouse(); "ware" -> upgradeWarehouse(); "dock" -> buildDock(); "field" -> buildField(); else -> false }
        if (ok) card("섬이 자랐어요", g.name + " 완성!")
    }

    /** 홈 화면에서 떠다니는 물건을 톡 쳤을 때. 보상 글을 돌려준다. */
    fun pickUp(kind: Int): String {
        finds++
        val lv = shipLevel
        val text = when (kind) {
            1 -> { val g = (20 + kotlin.random.Random.nextInt(21)) * lv; goldC += g * 100L; mapPieces++; "보물상자! 금화 +$g, 지도 조각 +1" }
            2 -> { val w = 4 + kotlin.random.Random.nextInt(5); add(w, 0); "갈매기가 떨어뜨린 나무 +$w" }
            else -> if (kotlin.random.Random.nextFloat() < .3f) { mapPieces++; "유리병 속 지도 조각 +1" }
                    else { val g = (3 + kotlin.random.Random.nextInt(6)) * lv; goldC += g * 100L; "유리병 속 금화 +$g" }
        }
        save(); return text
    }

    /** 지도 퍼즐을 다 맞췄을 때: 조각을 쓰고 다음 항해에 보물섬에 들른다 */
    fun pack() { packed = true; save() }

    fun solveMap(): Boolean {
        if (mapPieces < MAP_NEED || treasurePending) return false
        mapPieces -= MAP_NEED; treasurePending = true; save(); return true
    }

    fun reset() {
        wood = 12; stone = 4; goldC = 0; houses = 0; warehouse = 0; dock = false; field = false; lighthouse = false; market = false
        shipLevel = 1; route = "port_haesol"; supplyT = 0f; voy = null; voyages = 0; workers.clear(); completed.clear()
        flags.clear(); opened.clear(); read.clear(); eventLast.clear(); lastEvent = ""; cards.clear(); log.clear(); report = null
        storyDirty = true
        save()
    }
}
