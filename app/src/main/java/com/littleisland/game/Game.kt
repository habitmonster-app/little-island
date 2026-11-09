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
    /** 초당 금화 (최근 항해 판매로 어림) */
    var gps = 0f
    /** 바다를 톡 치면 받는 금화: 배가 클수록, 짐칸이 넓을수록 많다 */
    fun tapGold(): Int = max(1, Math.round(gps * .4f) + shipLevel * shipLevel + (loadW() + loadS() * 2) / 20)
    @Synchronized fun addTapGold(n: Int) { goldC += n * 100L; tapsDone++ }
    var tapsDone = 0
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

    // ---------- 배 부품·선원 ----------
    val parts = HashMap<String, Int>()                 // 부품 id → 레벨 (0~20)
    val promoted = LinkedHashSet<String>()             // 선원이 된 알바
    val aboard = ArrayList<String>()                   // 다음 출항에 탈 선원
    private val crewLv = HashMap<String, Int>()
    private val crewXp = HashMap<String, Int>()
    // 7차: 우두머리, 도감, 부적, 훈련
    val bossWins = LinkedHashSet<String>()               // 처음 이긴 우두머리
    val souvOwned = LinkedHashSet<String>()              // 모은 기념품
    val milesDone = LinkedHashSet<String>()              // 받은 도감 이정표
    var tickets = 0; private set                         // 항해 쪽지
    val charmLv = HashMap<String, Int>()                 // 가진 부적 → 단계(1~5)
    val charmOn = HashMap<String, String>()              // 선원 → 단 부적
    var pity = 0; private set                            // 영웅 안 나온 연속 횟수
    var trainDay = -1L; private set
    var trainLeft = 0; private set
    var trainSet: List<String> = emptyList(); private set   // 오늘의 문제 3개
    var missionQ: String? = null; private set          // 다음 출항에 할 특별 항해
    val missionsDone = LinkedHashSet<String>()           // 성공한 특별 항해
    val decoOwned = LinkedHashSet<String>()             // 가진 배 장식
    val decoOn = HashMap<String, String>()               // 자리(flag/figurehead/sail/deck) → 단 장식
    var levelUps = 0                                   // 화면 연출용 (Scene이 0으로 돌림)

    fun partLevel(id: String) = parts[id] ?: 0
    fun partValue(id: String): Float { val lv = partLevel(id); return if (lv <= 0) 0f else (Fleet.part(id)?.levels?.getOrNull(lv - 1)?.value ?: 0f) * (1 + .1f * stars(id)) }
    // ---------- 부품 별 강화: 지금 배에서 최고 레벨이면 금화로 별 1~5 (별 하나마다 효과 +10%) ----------
    val partStars = HashMap<String, Int>()
    const val STAR_MAX = 5
    fun stars(id: String) = partStars[id] ?: 0
    fun starCost(id: String) = Data.Cost((400 * (1 shl stars(id)) * shipLevel), 0, 0)
    fun starReady(id: String) = partLevel(id) > 0 && partNext(id) == null && stars(id) < STAR_MAX
    @Synchronized fun buyStar(id: String): Boolean {
        if (!starReady(id) || !pay(starCost(id))) return false
        partStars[id] = stars(id) + 1; bump("part_upgrade"); save(); return true
    }
    /** 해적에게 지면 금고 금화도 조금 빼앗긴다 (키·선원 수리로 줄어듦). 빼앗긴 금화 수를 돌려준다 */
    private fun plunder(winGold: Int, frac: Float, crew: List<String>): Int {
        val cut = 1 - min(.5f, partLossCut() + cb(crew, "repair_after_loss"))
        val g = min(goldC / 100 * frac * cut, max(20, winGold * 3).toFloat()).toLong().coerceAtLeast(0)
        goldC -= g * 100; return g.toInt()
    }
    private fun partLossCut(): Float { val lv = partLevel("helm"); return if (lv <= 0) 0f else Fleet.part("helm")?.levels?.getOrNull(lv - 1)?.lossCut ?: 0f }
    fun minPart() = Fleet.core.minOfOrNull { partLevel(it.id) } ?: 0
    fun crewLevel(id: String) = crewLv[id] ?: 1
    fun crewXp(id: String) = crewXp[id] ?: 0
    fun slots() = Fleet.slots(shipLevel)
    fun crewBonus(ids: List<String> = aboard): Map<String, Float> = Fleet.bonus(ids, { crewLevel(it) }, charmBonus(ids))
    /** 탄 선원이 단 부적 효과 (역할이 맞으면 1.5배). 상한은 선원 스킬과 함께 한 번에 씌운다 */
    fun charmBonus(ids: List<String>): Map<String, Float> {
        val m = HashMap<String, Float>()
        for (id in ids) { val ch = Fleet.charm(charmOn[id]) ?: continue; val v = charmValue(ch, id); m[ch.effect] = (m[ch.effect] ?: 0f) + v }
        return m
    }
    fun charmValue(ch: Fleet.Charm, crewId: String?): Float {
        val lv = (charmLv[ch.id] ?: 1).coerceIn(1, ch.values.size)
        val role = crewId?.let { Fleet.crewOf(it)?.role }
        return ch.values[lv - 1] * (if (role != null && role == ch.role) 1.5f else 1f)
    }
    private fun cb(ids: List<String>, k: String) = crewBonus(ids)[k] ?: 0f

    /** 배 그림 단계(0~3): 이번 배 단계 기준 레벨을 넘은 부품 수 */
    fun lookStage(): Int { val t = Fleet.lookThreshold(shipLevel); return minOf(3, Fleet.core.count { partLevel(it.id) >= t }) }

    fun loadW(ids: List<String> = aboard) = floor(ship().cargoW * (1 + min(1.35f, partValue("hull") + cb(ids, "cargo_bonus") + tree("cargo") + cs(ids, 2)))).toInt()
    fun loadS(ids: List<String> = aboard) = floor(ship().cargoS * (1 + min(1.35f, partValue("hull") + cb(ids, "cargo_bonus") + tree("cargo") + cs(ids, 2)))).toInt()

    // ---------- 항해 ----------
    class Voyage(var start: Long, val dur: Float, val port: String, var cw: Int, var cs: Int, val encounter: Boolean,
                 val pirate: String?, val win: Boolean, var step: Int, var rewardC: Long, var hire: String?, var saleC: Long,
                 val crew: List<String> = emptyList(), val mission: String? = null)
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
        f.startsWith("parts_") -> minPart() >= (f.removePrefix("parts_").toIntOrNull() ?: Int.MAX_VALUE)
        // 선원 이야기: 승진했으면 crew_<id>_promote, 레벨이 닿으면 crew_<id>_lv5 / _lv10 (지난 것도 소급해서 열림)
        f.startsWith("crew_") && f.endsWith("_promote") -> f.removePrefix("crew_").removeSuffix("_promote") in promoted
        f.startsWith("crew_") && "_lv" in f -> f.removePrefix("crew_").substringBeforeLast("_lv").let { id ->
            id in promoted && crewLevel(id) >= (f.substringAfterLast("_lv").toIntOrNull() ?: Int.MAX_VALUE) }
        else -> f in flags
    }
    fun hasAll(l: List<String>) = l.all { has(it) }
    fun flag(f: String) { if (flags.add(f)) storyDirty = true }
    fun isOpen(id: String) = id in opened
    fun open(id: String) { opened.add(id) }
    fun isRead(id: String) = id in read
    fun markRead(id: String) { if (read.add(id)) save() }
    fun unread() = opened.count { it !in read }
    fun markAllRead() { if (read.addAll(opened)) save() }
    private var storyDirty = true

    // ---------- 카드 ----------
    class Report(val minutes: Int, val wood: Int, val stone: Int, val gold: Int, val voyages: Int, val full: Boolean)
    var report: Report? = null
    val cards = ArrayDeque<Pair<String, String>>()
    val log = ArrayList<String>()
    private fun card(t: String, m: String) {
        cards.addLast(t to m); while (cards.size > 8) cards.removeFirst()
        log.add(0, t.replace(Regex("\u0001\\d*\u0001"), "") + " " + m.replace("\n", " ")); while (log.size > 6) log.removeAt(log.size - 1)
    }

    // 이야기 자리표시자용
    val LOAD_W get() = loadW()
    val LOAD_S get() = loadS()
    val WAGE get() = Data.worker("yeoul")?.wage ?: 10
    const val WORK_MS = 6 * HOUR

    private var prefs: SharedPreferences? = null
    private var last = 0L
    private var saveT = 0f

    // =====================================================================
    fun init(c: Context) {
        if (prefs != null) return
        Data.load(c); Story.load(c); Art.init(c); Fleet.load(c)
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
        // 배 부품: 예전 저장이면 이미 산 배 단계만큼 무료로 채운다
        parts.clear(); partStars.clear(); gps = sp.getFloat("gps", 0f)
        sp.getString("stars", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); partStars[k] = v.toIntOrNull() ?: 0 }
        if (sp.contains("parts")) sp.getString("parts", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); parts[k] = v.toIntOrNull() ?: 0 }
        else if (sp.contains("last")) Fleet.core.forEach { parts[it.id] = 5 * (shipLevel - 1) }
        promoted.clear(); sp.getString("promoted", "")!!.split(",").filter { it.isNotEmpty() }.forEach { promoted.add(it) }
        aboard.clear(); sp.getString("aboard", "")!!.split(",").filter { it.isNotEmpty() && it in promoted }.forEach { aboard.add(it) }
        sp.getString("crewLv", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); crewLv[k] = v.toIntOrNull() ?: 1 }
        fun set(k: String, to: MutableSet<String>) { to.clear(); sp.getString(k, "")!!.split(",").filter { it.isNotEmpty() }.forEach { to.add(it) } }
        set("bossWins", bossWins); set("souvOwned", souvOwned); set("milesDone", milesDone)
        tickets = sp.getInt("tickets", 0); pity = sp.getInt("pity", 0)
        charmLv.clear(); sp.getString("charmLv", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); charmLv[k] = v.toIntOrNull() ?: 1 }
        charmOn.clear(); sp.getString("charmOn", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); if (v in charmLv) charmOn[k] = v }
        cevId = sp.getString("cevId", null); ordDay = sp.getLong("ordDay", -1L); ordIds = sp.getString("ordIds", "")!!.split(",").filter { it.isNotEmpty() }
        ordProg.clear(); sp.getString("ordProg", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); ordProg[k] = v.toIntOrNull() ?: 0 }
        ordDone.clear(); sp.getString("ordDone", "")!!.split(",").filter { it.isNotEmpty() }.forEach { ordDone.add(it) }
        weekStart = sp.getLong("weekStart", -1L); weekCount = sp.getInt("weekCount", 0); weekGot = sp.getBoolean("weekGot", false)
        trainDay = sp.getLong("trainDay", -1L); trainLeft = sp.getInt("trainLeft", 0); trainSet = sp.getString("trainSet", "")!!.split(",").filter { it.isNotEmpty() }
        missionQ = sp.getString("missionQ", null)
        missionsDone.clear(); sp.getString("missionsDone", "")!!.split(",").filter { it.isNotEmpty() }.forEach { missionsDone.add(it) }
        decoOwned.clear(); sp.getString("decoOwned", "")!!.split(",").filter { it.isNotEmpty() }.forEach { decoOwned.add(it) }
        decoOn.clear(); sp.getString("decoOn", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); if (v in decoOwned) decoOn[k] = v }
        sp.getString("crewXp", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); crewXp[k] = v.toIntOrNull() ?: 0 }
        critSeen.clear(); sp.getString("critSeen", "")!!.split(",").mapNotNull { it.toIntOrNull() }.forEach { critSeen.add(it) }
        bookGot.clear(); sp.getString("bookGot", "")!!.split(",").mapNotNull { it.toIntOrNull() }.forEach { bookGot.add(it) }
        pearlUntil = sp.getLong("pearlUntil", 0L); pearlNext = sp.getLong("pearlNext", 0L)
        crewPts.clear(); sp.getString("crewPts", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); crewPts[k] = v.split(".").map { x -> x.toIntOrNull() ?: 0 }.plus(listOf(0, 0, 0, 0)).take(4).toIntArray() }
        treeOwned.clear(); sp.getString("tree", "")!!.split(",").mapNotNull { it.toIntOrNull() }.forEach { treeOwned.add(it) }
        renown = sp.getInt("renown", 0); attDay = sp.getLong("attDay", -1L); attIdx = sp.getInt("attIdx", 0)
        dbDay = sp.getLong("dbDay", -1L); seasonKey = sp.getString("seasonKey", "") ?: ""; seasonPts = sp.getInt("seasonPts", 0); seasonGot = sp.getInt("seasonGot", 0)
        visTrades.clear(); sp.getString("visTrades", "")!!.split(",").filter { ":" in it }.forEach { val (k, v) = it.split(":"); visTrades[k] = v.toIntOrNull() ?: 0 }
        set("titlesGot", titlesGot); titleOn = sp.getString("titleOn", null)?.takeIf { it in titlesGot }
        set("festGot", festGot); weatherId = sp.getString("v_weather", null)
        reqOffer.clear(); sp.getString("reqOffer", "")!!.split(",").filter { it.isNotEmpty() }.forEach { reqOffer.add(it) }
        reqAcc.clear(); sp.getString("reqAcc", "")!!.split(",").filter { ":" in it }.forEach { val q = it.split(":"); reqAcc[q[0]] = intArrayOf(q.getOrNull(1)?.toIntOrNull() ?: 0, q.getOrNull(2)?.toIntOrNull() ?: 0) }
        reqNext = sp.getLong("reqNext", 0L)
        visId = sp.getString("visId", null); visOffer = sp.getInt("visOffer", 0); visUntil = sp.getLong("visUntil", 0L); visDone = sp.getBoolean("visDone", false); visNext = sp.getLong("visNext", 0L)
        // 항해
        voy = null
        if (sp.contains("v_start")) {
            val st = sp.getLong("v_start", 0L)
            if (st != 0L) voy = Voyage(st, sp.getFloat("v_dur", 720f), sp.getString("v_port", route)!!, sp.getInt("v_cw", 0), sp.getInt("v_cs", 0),
                sp.getBoolean("v_enc", false), sp.getString("v_pirate", null), sp.getBoolean("v_win", true), sp.getInt("v_step", 0),
                sp.getLong("v_reward", 0L), sp.getString("v_hire", null), sp.getLong("v_sale", 0L),
                sp.getString("v_crew", "")!!.split(",").filter { it.isNotEmpty() }, sp.getString("v_mission", null))
        } else if (sp.getLong("voyStart", 0L) != 0L) {
            // 빌드 5~6의 진행 중 항해는 정산만 하고 마친다
            goldC += sp.getInt("voyGold", 0) * 100L
        }
        // 이미 지은 것은 일어난 일로 인정
        if (houses >= 1) flags.add("house_1")
        if (warehouse >= 1) flags.add("store_1")
        if (dock) flags.add("dock")
        for (n in 2..shipLevel) flags.add("ship_$n")
        if (lighthouse) flags.add("lighthouse")
        if (market) flags.add("market")
        checkTitles(false)
        storyDirty = true
    }

    @Synchronized fun save() {
        saveT = 0f
        val v = voy
        prefs?.edit()?.apply {
            putInt("wood", wood); putInt("stone", stone); putLong("goldC", goldC); putFloat("gps", gps)
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
            putString("parts", parts.entries.joinToString(",") { it.key + ":" + it.value })
            putString("stars", partStars.entries.joinToString(",") { it.key + ":" + it.value })
            putString("promoted", promoted.joinToString(",")); putString("aboard", aboard.joinToString(","))
            putString("crewLv", crewLv.entries.joinToString(",") { it.key + ":" + it.value })
            putString("bossWins", bossWins.joinToString(",")); putString("souvOwned", souvOwned.joinToString(",")); putString("milesDone", milesDone.joinToString(","))
            putInt("tickets", tickets); putInt("pity", pity)
            putString("charmLv", charmLv.entries.joinToString(",") { it.key + ":" + it.value }); putString("charmOn", charmOn.entries.joinToString(",") { it.key + ":" + it.value })
            putString("cevId", cevId); putLong("ordDay", ordDay); putString("ordIds", ordIds.joinToString(",")); putString("ordProg", ordProg.entries.joinToString(",") { it.key + ":" + it.value })
            putString("ordDone", ordDone.joinToString(",")); putLong("weekStart", weekStart); putInt("weekCount", weekCount); putBoolean("weekGot", weekGot)
            putLong("trainDay", trainDay); putInt("trainLeft", trainLeft); putString("trainSet", trainSet.joinToString(","))
            putString("missionQ", missionQ); putString("missionsDone", missionsDone.joinToString(","))
            putString("decoOwned", decoOwned.joinToString(",")); putString("decoOn", decoOn.entries.joinToString(",") { it.key + ":" + it.value })
            putString("crewXp", crewXp.entries.joinToString(",") { it.key + ":" + it.value })
            putString("titlesGot", titlesGot.joinToString(",")); putString("titleOn", titleOn); putString("festGot", festGot.joinToString(","))
            putString("reqOffer", reqOffer.joinToString(",")); putString("reqAcc", reqAcc.entries.joinToString(",") { it.key + ":" + it.value[0] + ":" + it.value[1] }); putLong("reqNext", reqNext)
            putString("visId", visId); putInt("visOffer", visOffer); putLong("visUntil", visUntil); putBoolean("visDone", visDone); putLong("visNext", visNext)
            putString("v_weather", weatherId)
            putLong("pearlUntil", pearlUntil); putLong("pearlNext", pearlNext)
            putString("tree", treeOwned.joinToString(",")); putString("crewPts", crewPts.entries.joinToString(",") { it.key + ":" + it.value.joinToString(".") }); putInt("renown", renown); putLong("attDay", attDay); putInt("attIdx", attIdx); putLong("dbDay", dbDay)
            putString("seasonKey", seasonKey); putInt("seasonPts", seasonPts); putInt("seasonGot", seasonGot)
            putString("critSeen", critSeen.joinToString(",")); putString("bookGot", bookGot.joinToString(",")); putString("visTrades", visTrades.entries.joinToString(",") { it.key + ":" + it.value })
            putLong("voyStart", 0L)
            if (v == null) putLong("v_start", 0L) else {
                putLong("v_start", v.start); putFloat("v_dur", v.dur); putString("v_port", v.port); putInt("v_cw", v.cw); putInt("v_cs", v.cs)
                putBoolean("v_enc", v.encounter); putString("v_pirate", v.pirate); putBoolean("v_win", v.win); putInt("v_step", v.step)
                putLong("v_reward", v.rewardC); putString("v_hire", v.hire); putLong("v_sale", v.saleC); putString("v_crew", v.crew.joinToString(",")); putString("v_mission", v.mission)
            }
            apply()
        }
    }

    // =====================================================================
    // 시뮬레이션

    /** 화면이 그려지는 동안 매 프레임 부른다. */
    // ---------- 12차: 날씨, 칭호, 계절 축제, 항구 의뢰, 손님 배 ----------
    var weatherId: String? = null; private set            // 이번 항해 날씨 (출항 때 정하고 귀항 때 지움)
    fun weather(): Fleet.Weather? = if (voy == null) null else Fleet.weather(weatherId)
    private fun cal(t: Long = System.currentTimeMillis()) = java.util.Calendar.getInstance().apply { timeInMillis = t }
    var monthOverride: Int? = null                         // 미리보기용
    fun month(): Int = monthOverride ?: (cal().get(java.util.Calendar.MONTH) + 1)
    fun season(): Fleet.Season? = Fleet.season(month())
    private fun rollWeather(t: Long): Fleet.Weather? {
        val k = cal(t); val h = k.get(java.util.Calendar.HOUR_OF_DAY); val night = h < 6 || h >= 19
        val sid = Fleet.season(k.get(java.util.Calendar.MONTH) + 1)?.id
        val ok = Fleet.weathers.filter { w -> shipLevel >= w.minShip && (sid == null || sid in w.seasons) &&
            (w.time == "any" || (w.time == "night") == night) }
        val tb = tide()?.takeIf { it.kind == "weather_boost" }
        return pickWeighted(ok) { if (tb != null && it.id == tb.target) (it.weight * tb.value).roundToIntSafe() else it.weight }
    }
    private fun Float.roundToIntSafe() = Math.round(this)
    private fun wv(kind: String): Float = Fleet.weather(weatherId)?.takeIf { it.kind == kind }?.value ?: 0f

    val titlesGot = LinkedHashSet<String>()
    var titleOn: String? = null; private set
    fun counterOf(k: String): Int = when (k) {
        "voyages" -> voyages; "treasures" -> treasures; "finds" -> finds; "boss_wins" -> bossWins.size; "souvenirs" -> souvOwned.size
        "charms" -> charmLv.size; "missions" -> missionsDone.size; "decos" -> decoOwned.size; else -> 0 }
    fun titleOk(t: Fleet.Title): Boolean = when (t.type) {
        "counter_at_least" -> counterOf(t.key) >= t.value
        "ship_level_at_least" -> shipLevel >= t.value
        "any_promoted_crew_level_at_least" -> promoted.any { crewLevel(it) >= t.value }
        "promoted_count_at_least" -> promoted.size >= t.value
        "boss_first_win" -> t.key in bossWins
        "creature_book" -> critSeen.size >= t.value
        else -> false
    }
    /** 새로 채운 칭호를 연다. 한 번 연 칭호는 다시 잠그지 않는다 */
    fun checkTitles(announce: Boolean = true) {
        for (t in Fleet.titles) if (t.id !in titlesGot && titleOk(t)) {
            titlesGot.add(t.id)
            if (announce) card("새 칭호 · " + t.name, t.short + "\n앱 할 일 탭에서 달 수 있어요")
        }
    }
    @Synchronized fun setTitle(id: String?): Boolean { if (id != null && id !in titlesGot) return false; titleOn = id; save(); return true }
    fun titleName(): String? = Fleet.title(titleOn)?.name

    private val festGot = HashSet<String>()
    /** 오늘 계절 축제 기간이면 (축제, 이미 받았는지) */
    fun festNow(): Pair<Fleet.Season, Boolean>? {
        val k = cal(); val m = k.get(java.util.Calendar.MONTH) + 1; val d = k.get(java.util.Calendar.DAY_OF_MONTH)
        val s = Fleet.seasons.firstOrNull { s -> s.festDays.size == 4 && s.festId.isNotEmpty() && run {
            val a = s.festDays[0] * 100 + s.festDays[1]; val b = s.festDays[2] * 100 + s.festDays[3]; val x = m * 100 + d
            if (a <= b) x in a..b else x >= a || x <= b } } ?: return null
        return s to ((s.festId + ":" + k.get(java.util.Calendar.YEAR)) in festGot)
    }
    private fun festCheck() {
        val (s, got) = festNow() ?: return
        if (got) return
        festGot.add(s.festId + ":" + cal().get(java.util.Calendar.YEAR))
        tickets += s.festTickets; goldC += s.festGold * 100L
        card("계절 축제 · " + s.festName, s.festShort + "\n쪽지 +" + s.festTickets + ", 금화 +" + s.festGold)
        save()
    }

    // 항구 의뢰: 제시 중(reqOffer) + 받은 것(reqAcc: id → [나무, 돌] 넘긴 양)이 합쳐서 최대 reqMax
    val reqOffer = ArrayList<String>()
    val reqAcc = LinkedHashMap<String, IntArray>()
    var reqNext = 0L; private set
    fun reqOpen(r: Fleet.PortReq) = shipLevel >= r.minShip && Data.ports.firstOrNull { it.id == r.port }?.let { routeOk(it) } == true
    @Synchronized fun reqRefresh(now: Long = System.currentTimeMillis()) {
        if (Fleet.reqs.isEmpty()) return
        if (now >= reqNext) { reqOffer.clear(); reqNext = now + Fleet.reqRefreshH * HOUR }
        reqOffer.removeAll { id -> Fleet.req(id)?.let { !reqOpen(it) } ?: true }
        val free = Fleet.reqMax - reqAcc.size - reqOffer.size
        if (free > 0) Fleet.reqs.filter { it.id !in reqAcc && it.id !in reqOffer && reqOpen(it) }.shuffled().take(free).forEach { reqOffer.add(it.id) }
    }
    @Synchronized fun acceptReq(id: String): Boolean {
        if (id !in reqOffer || reqAcc.size >= Fleet.reqMax) return false
        reqOffer.remove(id); reqAcc[id] = intArrayOf(0, 0); save(); return true
    }
    @Synchronized fun cancelReq(id: String): Boolean {
        val r = Fleet.req(id) ?: return false
        if (voy?.port == r.port) return false          // 그 항구로 가는 중이면 귀항 뒤에
        reqAcc.remove(id); reqRefresh(); save(); return true
    }
    /** 항구에 닿으면 받은 의뢰부터 짐을 넘긴다. 넘긴 짐은 팔지 않는다 */
    private fun deliverReqs(v: Voyage): String {
        var dw = 0; var ds = 0
        for ((id, got) in reqAcc.entries.toList()) {
            val r = Fleet.req(id) ?: continue
            if (r.port != v.port) continue
            val w = min(v.cw, r.needW - got[0]); val s = min(v.cs, r.needS - got[1])
            if (w > 0) { got[0] += w; v.cw -= w; dw += w }
            if (s > 0) { got[1] += s; v.cs -= s; ds += s }
            if (got[0] >= r.needW && got[1] >= r.needS) {
                reqAcc.remove(id)
                val rg = Math.round(r.gold * (1 + tideV("request_bonus", 0f)))
                goldC += rg * 100L; tickets += r.tickets
                card("의뢰 완료 · " + r.title, r.client + ": “" + r.thanks + "”\n금화 +" + rg + (if (r.tickets > 0) ", 쪽지 +" + r.tickets else ""))
            }
        }
        return if (dw + ds == 0) "" else "\n의뢰에 넘김" + (if (dw > 0) " 나무 $dw" else "") + (if (ds > 0) " 돌 $ds" else "")
    }

    // 손님 배: 한 번에 한 척, 머무는 동안 거래 한 번
    var visId: String? = null; private set
    var visOffer = 0; private set
    var visUntil = 0L; private set
    var visDone = false; private set
    private var visNext = 0L
    fun visitor(): Fleet.Visitor? = Fleet.visitor(visId)
    fun visOfferNow(): Fleet.Offer? = visitor()?.offers?.getOrNull(visOffer)
    private fun visCheck(now: Long) {
        if (visId != null && now >= visUntil) visId = null
        if (Fleet.visitors.isEmpty() || !dock) return
        if (visNext == 0L) visNext = now + 10 * 60_000L
        if (visId == null && now >= visNext) {
            val v = Fleet.visitors.filter { shipLevel >= it.minShip }.randomOrNull() ?: return
            visId = v.id; visOffer = Random.nextInt(v.offers.size); visDone = false
            visUntil = now + Fleet.visStay * 60_000L
            visNext = visUntil + ((Fleet.visEvery[0] + Random.nextInt(max(1, Fleet.visEvery[1] - Fleet.visEvery[0] + 1))) * 60_000L / tideV("visitor_more", 1f)).toLong()
            card(v.name + "이(가) 들렀어요", v.hello + "\n" + (v.offers[visOffer].short) + " · 배를 톡 하면 거래")
            save()
        }
    }
    fun resText(r: Fleet.Res) = listOfNotNull(r.wood.takeIf { it > 0 }?.let { "나무 $it" }, r.stone.takeIf { it > 0 }?.let { "돌 $it" },
        r.gold.takeIf { it > 0 }?.let { "금화 $it" }, r.tickets.takeIf { it > 0 }?.let { "쪽지 $it" }).joinToString(", ")
    fun canTrade(): Boolean { val o = visOfferNow() ?: return false
        return !visDone && wood >= o.give.wood && stone >= o.give.stone && goldC >= o.give.gold * 100L && tickets >= o.give.tickets }
    /** 손님과 거래. 결과 글 (못 하면 이유) */
    @Synchronized fun trade(): String {
        val v = visitor() ?: return "손님이 떠났어요"
        val o = visOfferNow() ?: return ""
        if (visDone) return v.bye
        if (!canTrade()) return resText(o.give) + "이(가) 필요해요"
        wood -= o.give.wood; stone -= o.give.stone; goldC -= o.give.gold * 100L; tickets -= o.give.tickets
        add(o.get.wood, o.get.stone); goldC += o.get.gold * 100L; tickets += o.get.tickets
        visDone = true
        val n = (visTrades[v.id] ?: 0) + 1; visTrades[v.id] = n
        for (st in Fleet.vstories) if (st.visitor == v.id && st.trades <= n && !has(st.id)) {
            flag(st.id); tickets += st.reward.tickets; goldC += st.reward.gold * 100L
            card("손님 이야기 · " + st.title, st.cardLine + "\n일지에 적었어요" + (if (st.reward.tickets + st.reward.gold > 0) " · " + resText(st.reward) else ""))
        }
        Story.refresh(false)
        card(v.name + "과(와) 거래했어요", "“" + v.bye + "”\n받음 " + resText(o.get))
        save()
        return "거래 완료 " + resText(o.get)
    }
    private var t12 = 0f

    // ---------- 13차: 도감, 주간 바다 소식, 손님 이야기 ----------
    val critSeen = LinkedHashSet<Int>()               // 본 바다 동물 kind
    private val bookGot = HashSet<Int>()              // 받은 도감 문턱 count
    /** 바다 동물을 톡: 처음이면 도감에 적고 보상. 화면에 띄울 글 */
    @Synchronized fun seeCritter(kind: Int): String {
        val cr = Fleet.creature(kind) ?: return ""
        if (!critSeen.add(kind)) return cr.name
        seasonAdd(1)
        tickets += cr.reward.tickets; goldC += cr.reward.gold * 100L
        card("도감 · " + cr.name, (Fleet.crewOf(cr.seenBy)?.name?.let { "$it: “" + cr.firstLine + "”\n" } ?: "") + "쪽지 +" + cr.reward.tickets + ", 금화 +" + cr.reward.gold)
        for (m in Fleet.bookMiles) if (critSeen.size >= m.count && bookGot.add(m.count)) {
            tickets += m.reward.tickets; goldC += m.reward.gold * 100L
            card("도감 " + m.count + "마리!", "쪽지 +" + m.reward.tickets + ", 금화 +" + m.reward.gold)
        }
        checkTitles(); save()
        return "도감 " + critSeen.size + "/" + Fleet.creatures.size + " " + cr.name + "!"
    }
    /** 이번 주 바다 소식: 2026-01-05(월)부터 7일마다 다음 소식 */
    fun tide(): Fleet.Tide? {
        if (Fleet.tides.isEmpty()) return null
        val base = java.util.Calendar.getInstance().apply { clear(); set(2026, 0, 5) }
        val k = cal(); val now = java.util.Calendar.getInstance().apply { clear(); set(k.get(java.util.Calendar.YEAR), k.get(java.util.Calendar.MONTH), k.get(java.util.Calendar.DAY_OF_MONTH)) }
        val days = Math.round((now.timeInMillis - base.timeInMillis) / 86_400_000.0).toInt()
        return Fleet.tides[Math.floorMod(Math.floorDiv(days, 7), Fleet.tides.size)]
    }
    fun tideV(kind: String, def: Float): Float = tide()?.takeIf { it.kind == kind }?.value ?: def
    val visTrades = HashMap<String, Int>()

    // ---------- 14차: 진주 조개 ----------
    var pearlUntil = 0L; private set                // 이 시각까지 조개가 열려 있다
    private var pearlNext = 0L
    fun pearlOpen(now: Long = System.currentTimeMillis()) = now < pearlUntil
    private fun pearlCheck(now: Long) {
        if (pearlNext == 0L) pearlNext = now + 20 * 60_000L
        if (now >= pearlNext && !pearlOpen(now)) {
            pearlUntil = now + 10 * 60_000L
            pearlNext = now + ((60f / Fleet.pearlChance.coerceAtLeast(.05f)) * (.5f + Random.nextFloat()) * 60_000L).toLong()
        }
    }
    @Synchronized fun takePearl(): String? {
        if (!pearlOpen()) return null
        pearlUntil = 0L
        val r = Fleet.pearlReward
        tickets += r.tickets; goldC += r.gold * 100L
        card("진주를 주웠어요", Fleet.pearlLine + "\n" + resText(r))
        save()
        return "진주! " + resText(r)
    }

    // ---------- 선원 능력치 찍기: 레벨마다 2점, 네 가지에 나눠 찍는다 ----------
    val STAT_NAME = listOf("대포술", "돛 손질", "짐 꾸리기", "흥정")
    val STAT_PER = floatArrayOf(.005f, .01f, .02f, .02f)
    const val STAT_MAX = 10
    val crewPts = HashMap<String, IntArray>()
    fun pts(id: String): IntArray = crewPts.getOrPut(id) { IntArray(4) }
    fun ptsTotal(id: String) = (crewLevel(id) - 1) * 2
    fun ptsFree(id: String) = ptsTotal(id) - pts(id).sum()
    /** 배에 탄 선원들이 찍은 능력치 합 */
    fun cs(ids: List<String>, k: Int): Float = ids.filter { it in promoted }.sumOf { (crewPts[it]?.get(k) ?: 0).toDouble() }.toFloat() * STAT_PER[k] * (1 + partValue("cabin"))
    fun statText(k: Int, n: Int): String = when (k) { 0 -> "이길 확률 +" + "%.1f".format(n * STAT_PER[0] * 100).removeSuffix(".0") + "%p"
        1 -> "항해 시간 -" + (n * 1) + "%"; 2 -> "짐칸 +" + (n * 2) + "%"; else -> "판매 +" + (n * 2) + "%" }
    @Synchronized fun statAdd(id: String, k: Int, d: Int): Boolean {
        if (id !in promoted) return false
        val a = pts(id)
        if (d > 0 && (ptsFree(id) <= 0 || a[k] >= STAT_MAX)) return false
        if (d < 0 && a[k] <= 0) return false
        a[k] += d; save(); return true
    }

    // ---------- 항해술 나무: 5x5 칸, 이웃한 칸을 배우면 다음 칸이 열린다 ----------
    class Node(val i: Int, val kind: String, val big: Boolean, val icon: Int, val name: String) {
        val value get() = when (kind) { "win" -> if (big) .03f else .01f; "speed" -> if (big) .05f else .02f; "cargo" -> if (big) .12f else .05f
            "sale" -> if (big) .10f else .04f; "xp" -> if (big) .30f else .10f; else -> if (big) .25f else .10f }
    }
    val TREE: List<Node> = listOf(
        Node(0, "find", false, 14, "지도 읽기"), Node(1, "find", false, 4, "행운"), Node(2, "win", true, 10, "매의 눈"), Node(3, "win", false, 2, "포탄 쌓기"), Node(4, "xp", true, 13, "밤샘 공부"),
        Node(5, "speed", false, 1, "바람 타기"), Node(6, "win", false, 2, "조준 연습"), Node(7, "win", false, 2, "포수 훈련"), Node(8, "xp", false, 11, "든든한 밥"), Node(9, "xp", false, 13, "등불 아래"),
        Node(10, "speed", true, 9, "별 보고 가기"), Node(11, "speed", false, 1, "돛 다루기"), Node(12, "cargo", false, 0, "첫 항해술"), Node(13, "sale", false, 3, "값 매기기"), Node(14, "sale", true, 15, "단골 상인"),
        Node(15, "speed", false, 1, "물길 읽기"), Node(16, "cargo", false, 0, "밧줄 묶기"), Node(17, "cargo", false, 0, "짐 쌓기"), Node(18, "sale", false, 3, "흥정"), Node(19, "sale", false, 3, "장부 쓰기"),
        Node(20, "find", false, 4, "네잎 클로버"), Node(21, "cargo", true, 6, "큰 짐칸"), Node(22, "cargo", false, 7, "통나무 싣기"), Node(23, "cargo", false, 8, "돌 싣기"), Node(24, "find", true, 14, "보물 지도"))
    val treeOwned = LinkedHashSet<Int>()
    fun tree(kind: String): Float = treeOwned.sumOf { i -> TREE[i].takeIf { it.kind == kind }?.value?.toDouble() ?: 0.0 }.toFloat()
    fun treeOpen(i: Int): Boolean {
        if (i in treeOwned) return false
        if (i == 12) return true
        val c = i % 5; val r = i / 5
        return listOf(if (c > 0) i - 1 else -1, if (c < 4) i + 1 else -1, if (r > 0) i - 5 else -1, if (r < 4) i + 5 else -1).any { it in treeOwned }
    }
    fun treeCost(i: Int): Int = Math.round(150.0 * Math.pow(1.4, treeOwned.size.toDouble()) * (if (TREE[i].big) 2 else 1) / 10).toInt() * 10
    fun treeKindText(n: Node): String = when (n.kind) {
        "win" -> "해적 이길 확률 +" + Math.round(n.value * 100) + "%p"
        "speed" -> "항해 시간 -" + Math.round(n.value * 100) + "%"
        "cargo" -> "짐칸 +" + Math.round(n.value * 100) + "%"
        "sale" -> "판매 금화 +" + Math.round(n.value * 100) + "%"
        "xp" -> "선원 경험치 +" + Math.round(n.value * 100) + "%"
        else -> "떠다니는 보물 금화 +" + Math.round(n.value * 100) + "%"
    }
    @Synchronized fun learn(i: Int): Boolean {
        if (!treeOpen(i)) return false
        val g = treeCost(i); if (gold < g) return false
        goldC -= g * 100L; treeOwned.add(i); bump("skill_learn")
        save(); return true
    }

    // ---------- 15차: 함대 힘, 출석, 오늘의 우두머리, 명성, 시즌 ----------
    /** 함대 힘: 이길 확률 식에서 해적 쪽을 뺀 내 쪽 합 × 10000. 해적 힘과 같으면 반반 */
    fun myPower(ids: List<String> = aboard.filter { it in promoted }.take(slots())): Int =
        Math.round((ship().pirateWin + (if (lighthouse) Data.lighthouseBonus else 0f) + workerPirateBonus() + partValue("cannon") + cb(ids, "pirate_win_bonus") + renownWin() + tree("win") + cs(ids, 0)) * 10000)
    fun enemyPower(p: Data.Pirate): Int = Math.round((.5f + Data.powerCoef * (p.power - 1f)) * 10000)
    fun powerLine(p: Data.Pirate, ids: List<String>) = "함대 힘 " + "%,d".format(myPower(ids)) + " vs " + "%,d".format(enemyPower(p))

    var renown = 0; private set
    fun renownWin() = renown * .02f
    fun renownSale() = renown * .1f
    /** 명성: 배 5단계 이상, 부품 넷 다 지금 최고 레벨이면 다시 시작할 수 있다 */
    fun renownReady() = shipLevel >= 5 && Fleet.core.all { partNext(it.id) == null && partLevel(it.id) > 1 }
    @Synchronized fun doRenown(): Boolean {
        if (!renownReady()) return false
        renown++
        for (p in Fleet.parts) parts[p.id] = 1
        goldC = 0L
        card("명성 " + renown + "!", "부품과 금화를 내려놓고 새 항해를 시작해요.\n해적 이길 확률 +" + (renown * 2) + "%p, 판매 +" + (renown * 10) + "%")
        save(); return true
    }

    // 출석 상자: 하루 한 번, 7칸을 돌아가며
    var attDay = -1L; private set
    var attIdx = 0; private set
    fun attReady() = today() != attDay
    fun attReward(i: Int = attIdx): Fleet.Res = Fleet.Res(0, 0, 100 * shipLevel * intArrayOf(1, 1, 2, 2, 3, 3, 6)[i % 7], intArrayOf(1, 1, 1, 2, 2, 2, 5)[i % 7])
    @Synchronized fun claimAtt(): Boolean {
        if (!attReady()) return false
        val r = attReward(); tickets += r.tickets; goldC += r.gold * 100L
        card("출석 " + (attIdx % 7 + 1) + "일째", resText(r))
        attDay = today(); attIdx = (attIdx + 1) % 7; seasonAdd(1); save(); return true
    }

    // 오늘의 우두머리: 하루 한 번 도전
    var dbDay = -1L; private set
    fun dailyBoss(): Data.Pirate? {
        val ok = Fleet.bosses.filter { it.minShip <= max(2, shipLevel) }.mapNotNull { Data.pirate(it.id) }
        if (ok.isEmpty()) return null
        return ok[Math.floorMod(today().toInt() * 7 + 3, ok.size)]
    }
    fun dbReady() = dock && today() != dbDay && dailyBoss() != null
    @Synchronized fun challengeBoss(): String? {
        if (!dbReady()) return null
        val p = dailyBoss() ?: return null
        val ids = aboard.filter { it in promoted }.take(slots())
        val win = Random.nextFloat() < winChance(p, ids)
        dbDay = today()
        val txt = if (win) {
            val g = (p.winGold[0] + p.winGold[1]) / 2
            tickets += 2; goldC += g * 100L; bump("boss_win")
            "이겼어요! 금화 +" + g + ", 쪽지 +2"
        } else { tickets += 1; "졌어요. 쪽지 +1 · 내일 다시!" }
        val nm = Fleet.boss(p.id)?.name ?: p.name
        card("오늘의 우두머리 " + nm, powerLine(p, ids) + "\n" + txt)
        save(); return txt
    }

    // 시즌 보상: 계절마다 점수를 모아 칸을 연다
    private var seasonKey = ""
    var seasonPts = 0; private set
    var seasonGot = 0; private set
    val SEASON_AT = intArrayOf(10, 25, 45, 70, 100, 140, 190, 250, 320, 400)
    private fun seasonNow(): String {
        val se = season() ?: return ""
        val k = cal(); val y = k.get(java.util.Calendar.YEAR) + (if (k.get(java.util.Calendar.MONTH) == 11) 1 else 0)
        return se.id + y
    }
    private fun seasonCheck() { val k = seasonNow(); if (k != seasonKey) { seasonKey = k; seasonPts = 0; seasonGot = 0 } }
    fun seasonInfo(): Pair<Int, Int> { seasonCheck(); return seasonPts to seasonGot }
    private fun seasonAdd(n: Int) { seasonCheck(); seasonPts += n }
    fun seasonReward(i: Int): Fleet.Res = Fleet.Res(0, 0, 200 * shipLevel * (1 + i / 3), if (i % 3 == 2) 3 else 1)
    fun seasonReady(): Boolean { seasonCheck(); return seasonGot < SEASON_AT.size && seasonPts >= SEASON_AT[seasonGot] }
    @Synchronized fun claimSeason(): Boolean {
        if (!seasonReady()) return false
        val r = seasonReward(seasonGot); tickets += r.tickets; goldC += r.gold * 100L
        seasonGot++
        card("시즌 보상 " + seasonGot + "칸", resText(r))
        save(); return true
    }

    // ---------- 선원 항해 사건 ----------
    var cevId: String? = null; private set          // 이번 항해에 생길 선원 사건 (귀항 때 정산)
    private fun rollCrewEvent(crew: List<String>) {
        cevId = null
        if (Fleet.crewEvents.isEmpty() || Random.nextFloat() >= Fleet.crewEvChance) return
        // 짝 사건(with)은 두 선원이 같이 탔을 때만
        fun fits(e: Fleet.CrewEvent) = e.crew in crew && crewLevel(e.crew) >= e.minLv && (e.with == null || e.with in crew)
        val ok = crew.filter { id -> Fleet.crewEvents.any { it.crew == id && fits(it) } }
        if (ok.isEmpty()) return
        val who = ok.random()
        cevId = Fleet.crewEvents.filter { it.crew == who && fits(it) }.random().id
    }
    private fun settleCrewEvent() {
        val e = Fleet.crewEvent(cevId) ?: return
        cevId = null
        val got = when (e.kind) {
            "gold" -> { goldC += e.value * 100L; "금화 +" + e.value }
            "wood" -> { add(e.value, 0); "나무 +" + e.value }
            "stone" -> { add(0, e.value); "돌 +" + e.value }
            "crew_xp" -> { giveXp(e.crew, e.value); (Fleet.crewOf(e.crew)?.name ?: "") + " 경험치 +" + e.value }
            "tickets" -> { tickets += e.value; "쪽지 +" + e.value }
            else -> ""
        }
        val pi = Data.workers.indexOfFirst { it.id == e.crew }.let { if (it >= 0 && crewLevel(e.crew) >= 11) it + 100 else it }   // 100+: 코트 배지
        // 제목 앞의 \u0001번호\u0001은 카드에 선원 배지를 붙이라는 표시
        card((if (pi >= 0) "\u0001" + pi + "\u0001" else "") + e.title, e.line + (if (got.isNotEmpty()) "\n" + got else ""))
    }

    // ---------- 오늘의 주문 ----------
    var ordDay = -1L; private set
    var ordIds: List<String> = emptyList(); private set
    val ordProg = HashMap<String, Int>()
    val ordDone = LinkedHashSet<String>()
    var weekStart = -1L; private set
    var weekCount = 0; private set
    var weekGot = false; private set
    /** 지금 할 수 없는 주문은 고르지 않는다 (부품이 다 최고, 기념품 다 모음 등) */
    private fun orderOk(o: Fleet.Order): Boolean = shipLevel >= o.minShip && when (o.counter) {
        "part_upgrade" -> Fleet.parts.any { partNext(it.id) != null }
        "souvenir_get" -> souvOwned.size < Fleet.souvenirs.size
        "mission_clear" -> Fleet.missions.any { it.id !in missionsDone && it.crew in promoted }
        "boss_win" -> shipLevel >= 2 && bossWins.size < Fleet.bosses.size
        "gacha_draw" -> Fleet.charms.isNotEmpty()
        "training_clear" -> Fleet.trainings.isNotEmpty()
        else -> true
    }
    fun ordCheckDay() {
        if (Fleet.orders.isEmpty()) return
        val d = today()
        if (weekStart < 0 || d - weekStart >= 7 || d < weekStart) { weekStart = d; weekCount = 0; weekGot = false }
        if (d == ordDay) return
        ordDay = d; ordProg.clear(); ordDone.clear()
        val rnd = java.util.Random(d * 31 + 7)
        val pick = ArrayList<String>()
        val diffs = listOf("easy", "normal", "hard")
        for (k in 0 until Fleet.dailyCount) {
            val want = diffs.getOrElse(k) { "easy" }
            // 원하는 난도에 후보가 없으면 더 쉬운 난도로
            val o = diffs.subList(0, diffs.indexOf(want) + 1).reversed().asSequence()
                .map { df -> Fleet.orders.filter { it.diff == df && it.id !in pick && orderOk(it) } }
                .firstOrNull { it.isNotEmpty() }?.let { it[rnd.nextInt(it.size)] } ?: continue
            pick.add(o.id)
        }
        ordIds = pick
        save()
    }
    /** 일이 하나 끝날 때마다: 오늘 주문 중 맞는 것의 진행을 올리고, 다 되면 바로 보상 */
    @Synchronized fun bump(counter: String, n: Int = 1) {
        seasonAdd(when (counter) { "voyage_return", "pirate_win", "training_clear" -> 2; "boss_win" -> 5; "mission_clear" -> 3; else -> 1 } * n)
        ordCheckDay()
        var changed = false
        for (id in ordIds) {
            val o = Fleet.order(id) ?: continue
            if (o.counter != counter || id in ordDone) continue
            ordProg[id] = min(o.target, (ordProg[id] ?: 0) + n); changed = true
            if (ordProg[id]!! >= o.target) {
                ordDone.add(id); tickets += o.tickets; goldC += o.gold * 100L; weekCount++; seasonAdd(3)
                card("주문 완료! " + o.title, listOfNotNull(o.tickets.takeIf { it > 0 }?.let { "쪽지 +$it" }, o.gold.takeIf { it > 0 }?.let { "금화 +$it" }).joinToString(", "))
                if (weekCount >= Fleet.weeklyGoal && !weekGot) {
                    weekGot = true; tickets += Fleet.weeklyTickets; goldC += Fleet.weeklyGold * 100L
                    card("주간 상자를 열었어요!", "쪽지 +" + Fleet.weeklyTickets + ", 금화 +" + Fleet.weeklyGold)
                }
            }
        }
        if (changed) save()
    }

    @Synchronized fun tick(dt: Float) {
        advance(System.currentTimeMillis())
        t12 += dt
        if (t12 > 5f) { t12 = 0f; val now = System.currentTimeMillis(); festCheck(); visCheck(now); reqRefresh(now); pearlCheck(now) }
        saveT += dt
        if (saveT > 10f) save()
    }

    /** 다시 보이기 시작할 때. 안 보던 시간(최대 8시간)을 시간 순서대로 계산하고 결과 카드를 만든다. */
    @Synchronized fun catchUp() {
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

    @Synchronized fun advance(now: Long) {
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

    fun winChance(p: Data.Pirate, ids: List<String> = aboard): Float = (ship().pirateWin - Data.powerCoef * (p.power - 1f) +
        (if (lighthouse) Data.lighthouseBonus else 0f) + workerPirateBonus() + partValue("cannon") + cb(ids, "pirate_win_bonus") + renownWin() + tree("win") + cs(ids, 0)).coerceIn(Data.winMin, Data.winMax)

    // ---------- 해적 대응: 배·대포·선원이 얼마나 도와주는지 ----------
    /** 지금 항로에서 만날 수 있는 보통 해적 (출항 때와 같은 조건) */
    fun routePirates(p: Data.Port = port()): List<Data.Pirate> = Data.pirates.filter { it.normal && it.weight > 0 && has(it.unlock) && hasAll(it.requires) &&
        it.disabledBy.none { d -> has(d) } && (it.ports == null || p.id in it.ports) }
    /** 지금 항로의 우두머리 */
    fun routeBosses(p: Data.Port = port()): List<Data.Pirate> = Fleet.bosses.filter { shipLevel >= it.minShip && p.id in it.ports }.mapNotNull { Data.pirate(it.id) }
    /** 보통 해적을 이길 확률 (가중 평균) */
    fun normalOdds(ids: List<String> = aboard.filter { it in promoted }.take(slots())): Float {
        val ps = routePirates(); if (ps.isEmpty()) return winChance(Data.pirate("hook_minor") ?: return 1f, ids)
        val tw = ps.sumOf { it.weight }.toFloat()
        return ps.sumOf { (winChance(it, ids) * it.weight).toDouble() }.toFloat() / tw
    }
    /** 가장 센 우두머리를 이길 확률 (없으면 null) */
    fun bossOdds(ids: List<String> = aboard.filter { it in promoted }.take(slots())): Float? = routeBosses().maxByOrNull { it.power }?.let { winChance(it, ids) }
    /** 해적 대응을 이루는 것들: 이름과 더해지는 값 */
    fun fightParts(ids: List<String> = aboard.filter { it in promoted }.take(slots())): List<Pair<String, Float>> = listOf(
        "배 " + shipLevel + "단계" to ship().pirateWin,
        "대포 Lv" + partLevel("cannon") to partValue("cannon"),
        "선원 스킬·부적" to cb(ids, "pirate_win_bonus"),
        "등대" to (if (lighthouse) Data.lighthouseBonus else 0f),
        "알바" to workerPirateBonus(),
        "명성 " + renown to renownWin(),
        "항해술" to tree("win"),
        "선원 대포술" to cs(ids, 0))

    // ---------- 선원 가르치기: 금화로 바로 다음 레벨 ----------
    fun teachCost(id: String): Int? { val lv = crewLevel(id); return if (id !in promoted || lv >= Fleet.maxCrewLevel) null else 250 * lv * lv }
    @Synchronized fun teach(id: String): Boolean {
        val g = teachCost(id) ?: return false
        if (gold < g) return false
        goldC -= g * 100L
        val need = (Fleet.xpTo.getOrNull(crewLevel(id) - 1) ?: return false) - crewXp(id)
        giveXp(id, need.coerceAtLeast(1)); bump("crew_teach"); save(); return true
    }

    fun speedCut(ids: List<String> = aboard) = min(.35f, partValue("sail") + cb(ids, "speed_bonus") + tree("speed") + cs(ids, 1))
    fun voyageSeconds(p: Data.Port = port(), ids: List<String> = aboard): Float =
        ceil(Data.baseSeconds * p.distance / ship().speed * (1 - speedCut(ids))).coerceIn(Data.minSeconds, Data.maxSeconds)

    private fun depart(t: Long) {
        var p = port()
        if (!routeOk(p)) { route = "port_haesol"; p = port() }
        val crew = aboard.filter { it in promoted }.take(slots())
        // 특별 항해: 예약한 임무가 아직 조건에 맞으면 이번 한 번만 그 항구로 (평소 항로 설정은 그대로)
        val m = Fleet.mission(missionQ)?.takeIf { missionMissing(it, crew).isEmpty() }
        missionQ = null
        rollCrewEvent(crew)
        if (m != null) Data.ports.firstOrNull { it.id == m.port }?.let { p = it }
        weatherId = rollWeather(t)?.id
        val cm = 1f + wv("cargo")
        val cw = min(wood, floor(loadW(crew) * cm).toInt()); val cs = min(stone, floor(loadS(crew) * cm).toInt())
        wood -= cw; stone -= cs
        flag("sail_1")
        // 해적: 첫 조우는 반드시, 그 뒤는 항구 위험도만큼
        val forced = !has("pirate_1") && !has("main_53")
        val enc = forced || Random.nextFloat() < (p.risk + (m?.pirateAdd ?: 0f) + wv("pirate_chance")) * (1 - min(.4f, partValue("lookout")))
        var pirate: Data.Pirate? = null
        if (enc) {
            pirate = if (forced) Data.pirate("hook_minor") else {
                val ok = Data.pirates.filter { it.normal && it.weight > 0 && has(it.unlock) && hasAll(it.requires) &&
                    it.disabledBy.none { d -> has(d) } && (it.ports == null || p.id in it.ports) }
                pickWeighted(ok) { it.weight }
            }
        }
        // 해적을 만났으면 가끔 우두머리가 대신 나온다 (우두머리마다 4% 구간, 한 번의 추첨)
        if (pirate != null && !forced) {
            val ok = Fleet.bosses.filter { shipLevel >= it.minShip && has(it.unlock) && p.id in it.ports }
            var r = Random.nextFloat()
            for (b in ok) { if (r < b.chance) { pirate = Data.pirate(b.id) ?: pirate; break }; r -= b.chance }
        }
        val win = pirate?.let { Random.nextFloat() < winChance(it, crew) } ?: true
        val sec = voyageSeconds(p, crew).let { if (m == null) it else ceil(it * m.durMult).coerceIn(Data.minSeconds, Data.maxSeconds) }
            .let { if (wv("speed") == 0f) it else ceil(it * (1 - wv("speed"))).coerceIn(Data.minSeconds, Data.maxSeconds) }
        voy = Voyage(t, sec, p.id, cw, cs, pirate != null, pirate?.id, win, 0, 0L, null, 0L, crew, m?.id)
        if (m != null) card("특별 항해 출발", (Fleet.crewOf(m.crew)?.name ?: "") + " · " + m.title)
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
                if (v.win) { bump("pirate_win"); if (Fleet.boss(pr?.id) != null) bump("boss_win") }
                val boss = Fleet.boss(pr?.id)
                if (pr != null && boss != null) {
                    if (v.win) {
                        val g = Random.nextInt(pr.winGold[0], max(pr.winGold[0], pr.winGold[1]) + 1)
                        v.rewardC = g * 100L
                        card("우두머리 " + boss.name + "을 물리쳤어요!", powerLine(pr, v.crew) + "\n귀항하면 금화 +$g\n" + boss.captain + ": “" + boss.loseLine + "”")
                    } else {
                        val lose = pr.lose * (1 - min(.5f, partLossCut() + cb(v.crew, "repair_after_loss")))
                        val lw = v.cw - floor(v.cw * (1 - lose)).toInt(); val ls = v.cs - floor(v.cs * (1 - lose)).toInt()
                        v.cw -= lw; v.cs -= ls
                        val pg = plunder(pr.winGold[1], .1f, v.crew)
                        card("우두머리 " + boss.name + "에게 약탈당했어요", powerLine(pr, v.crew) + "\n빼앗긴 것  나무 -$lw 돌 -$ls 금화 -$pg\n" + boss.captain + ": “" + boss.winLine + "”")
                    }
                } else if (pr != null) {
                    if (v.win) {
                        val g = Random.nextInt(pr.winGold[0], max(pr.winGold[0], pr.winGold[1]) + 1)
                        v.rewardC = g * 100L
                        card(pr.name + "을 물리쳤어요!", powerLine(pr, v.crew) + "\n귀항하면 금화 +$g")
                    } else {
                        val lose = pr.lose * (1 - min(.5f, partLossCut() + cb(v.crew, "repair_after_loss")))
                        val lw = v.cw - floor(v.cw * (1 - lose)).toInt(); val ls = v.cs - floor(v.cs * (1 - lose)).toInt()
                        v.cw -= lw; v.cs -= ls
                        val pg = plunder(pr.winGold[1], .05f, v.crew)
                        card(pr.name + "에게 약탈당했어요", powerLine(pr, v.crew) + "\n빼앗긴 것  나무 -$lw 돌 -$ls 금화 -$pg")
                    }
                }
            }
        }
        if (v.step == 1 && f >= Data.fArrive) {
            v.step = 2
            val delivered = deliverReqs(v)
            val mul = 1f + (if (market) Data.marketMul - 1f else 0f) + workerSaleBonus() + (if (packed) PACK_BONUS else 0f) + cb(v.crew, "sale_bonus") + renownSale() + tree("sale") + cs(v.crew, 3)
            packed = false
            val sale = floor((v.cw * p.buyW * (1 + cb(v.crew, "wood_price_bonus")) + v.cs * p.buyS * (1 + cb(v.crew, "stone_price_bonus"))) * mul * 100).toLong()
            goldC += sale; v.saleC = sale
            gps = if (gps <= 0f) sale / 100f / v.dur else gps * .7f + sale / 100f / v.dur * .3f
            flag("sale_1")
            v.hire = tryHire(p, t)
            v.hire?.let { goldC -= (Data.worker(it)?.wage ?: 0) * 100L }
            card(p.name + "에 도착했어요", "짐을 팔아 금화 +" + sale / 100 + (v.hire?.let { ", " + (Data.worker(it)?.name ?: "") + " 고용" } ?: "") + delivered)
            rollSouvenir(p.id)
        }
        if (v.step == 2 && f >= Data.fLeave) v.step = 3
        if (v.step >= 2 && f >= 1f) returnHome(v, v.start + (v.dur * 1000).toLong())
    }

    private fun tryHire(p: Data.Port, t: Long): String? {
        if (workers.size >= houses) return null
        val gold = goldC / 100
        val busy = workers.map { it.id }.toSet()
        val list = Data.workers.filter { it.port == p.id && has(it.unlock) && it.id !in busy && it.id !in promoted }
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
        val g0 = Fleet.gardenAt(voyages)
        voyages++
        Fleet.gardenAt(voyages)?.takeIf { it !== g0 && it.news.isNotEmpty() }?.let { line += "\n바닷속 정원 · " + it.news }
        voy = null
        lastReturnAt = t
        Fleet.weather(weatherId)?.let { w -> line += "\n" + w.name + " · " + w.cardLine }
        weatherId = null
        card("배가 돌아왔어요", line)
        for (id in v.crew) giveXp(id, Math.round(Fleet.xpPerVoyage * (1 + tree("xp"))))
        tickets++
        bump("voyage_return")
        settleCrewEvent()
        Fleet.boss(v.pirate)?.let { b -> if (v.encounter && v.win && bossWins.add(b.id)) {
            goldC += b.rewardGold * 100L; if (b.rewardMaps > 0) mapPieces += b.rewardMaps
            flag(b.storyId)
            card("우두머리 첫 승리!", b.name + " · 금화 +" + b.rewardGold + (if (b.rewardMaps > 0) ", 지도 조각 +" + b.rewardMaps else ""))
        } }
        Fleet.mission(v.mission)?.let { settleMission(it, v) }
        rollEvent(v)
        // 11화부터는 귀항할 때마다 최대 한 화씩
        Story.refresh(true).forEach { announce(it) }
        checkDeco()
        checkTitles()
        if (isOpen("main_60")) flag("ending")
        save()
    }

    /** 선원 경험치: 단계별로 깎으며 레벨을 올리고, Lv10이면 남는 건 버린다 */
    private fun giveXp(id: String, amount: Int) {
        run {
            var lv = crewLevel(id); if (lv >= Fleet.maxCrewLevel) return
            var xp = crewXp(id) + amount
            val before = lv
            while (lv < Fleet.maxCrewLevel && xp >= (Fleet.xpTo.getOrNull(lv - 1) ?: Int.MAX_VALUE)) { xp -= Fleet.xpTo[lv - 1]; lv++ }
            if (lv >= Fleet.maxCrewLevel) xp = 0
            crewLv[id] = lv; crewXp[id] = xp
            if (lv > before) {
                levelUps++
                val c = Fleet.crewOf(id)
                val sk = c?.skills?.firstOrNull { it.level in (before + 1)..lv }
                val say = Fleet.voices[id]?.levelUp?.takeIf { it.isNotEmpty() }
                card("선원 레벨 업!", (c?.name ?: "선원") + " Lv" + lv + (sk?.let { " · 새 스킬 " + it.name } ?: "") + (say?.let { "\n“$it”" } ?: ""))
            }
        }
    }

    // ---------- 선원 특별 항해 ----------
    /** 아직 못 채운 조건들 (비어 있으면 출발 가능). ids는 태울 선원 */
    fun missionMissing(m: Fleet.Mission, ids: List<String> = aboard.filter { it in promoted }.take(slots())): List<String> {
        val out = ArrayList<String>()
        val name = Fleet.crewOf(m.crew)?.name ?: m.crew
        if (m.crew !in promoted) out.add(name + " 승진")
        if (crewLevel(m.crew) < m.crewLv) out.add(name + " Lv" + m.crewLv)
        for (a in m.aboard) if (a !in ids) out.add((Fleet.crewOf(a)?.name ?: a) + " 탑승")
        for ((k, v) in m.parts) if (partLevel(k) < v) out.add((Fleet.part(k)?.name ?: k) + " Lv" + v)
        if (shipLevel < m.shipLv) out.add("배 " + m.shipLv + "단계")
        val p = Data.ports.firstOrNull { it.id == m.port }
        if (p == null || !routeOk(p)) out.add((p?.name ?: m.port) + " 항로")
        if (m.flag != null && !has(m.flag)) out.add("이야기 진행")
        if (m.prev != null && m.prev !in missionsDone) out.add("앞 임무 성공")
        return out
    }
    @Synchronized fun queueMission(id: String?): Boolean {
        if (id != null) { val m = Fleet.mission(id) ?: return false; if (id in missionsDone || missionMissing(m).isNotEmpty()) return false }
        missionQ = id; save(); return true
    }
    private fun settleMission(m: Fleet.Mission, v: Voyage) {
        val ok = !m.mustWin || !v.encounter || v.win
        if (!ok) { card("특별 항해 · " + m.title, "다시 준비해 출항할 수 있어요."); return }
        if (m.id in missionsDone) { card("특별 항해 · " + m.title, "다시 다녀왔어요."); return }
        missionsDone.add(m.id); bump("mission_clear")
        goldC += m.gold * 100L
        if (m.wood > 0 || m.stone > 0) add(m.wood, m.stone)
        if (m.maps > 0) mapPieces += m.maps
        if (m.xp > 0) giveXp(m.crew, m.xp)
        m.cosmetic?.let { if (Fleet.deco(it) != null && decoOwned.add(it) && decoOn[Fleet.deco(it)!!.slot] == null) decoOn[Fleet.deco(it)!!.slot] = it }
        val got = listOfNotNull(m.gold.takeIf { it > 0 }?.let { "금화 +$it" }, m.wood.takeIf { it > 0 }?.let { "나무 +$it" },
            m.stone.takeIf { it > 0 }?.let { "돌 +$it" }, m.maps.takeIf { it > 0 }?.let { "지도 조각 +$it" },
            m.xp.takeIf { it > 0 }?.let { (Fleet.crewOf(m.crew)?.name ?: "") + " 경험치 +$it" })
        card("특별 항해 성공!", m.resultCard + "\n" + got.joinToString(", "))
        flag(m.id)
    }

    private fun rollEvent(v: Voyage) {
        val good = min(.15f, partValue("helm") + cb(v.crew, "event_good_bonus"))
        val treasure = cb(v.crew, "treasure_find_bonus")
        val r = Random.nextFloat()
        val bonusOnly = r >= Data.eventChance
        if (bonusOnly && r >= Data.eventChance + good + treasure) return
        val ok0 = Data.events.filter { e ->
            val story = Story.byId(e.id)
            story != null && has(story.unlock) && has(e.unlock) && hasAll(e.requires) &&
                (e.ports == null || v.port in e.ports) && (!e.needField || field) &&
                (e.repeat || e.id !in eventLast) && voyages - (eventLast[e.id] ?: -999) >= e.cooldown && e.id != lastEvent
        }
        // 기본 확률을 넘긴 몫은 키·선원 덕분: 좋은 사건(보상 있는 것)이나 보물 사건만 고른다
        val ok = if (!bonusOnly) ok0 else if (r < Data.eventChance + treasure && ok0.any { it.id == "voy_21" }) ok0.filter { it.id == "voy_21" }
            else ok0.filter { it.gold > 0 || it.wood > 0 || it.stone > 0 }
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
        checkDeco()
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
    @Synchronized fun buildHouse(): Boolean {
        val b = nextHouse() ?: return false
        if (!houseReady() || !pay(b.cost)) return false
        houses++; done(if (houses == 1) "house_1" else null); return true
    }

    fun nextWarehouse() = Data.warehouse.getOrNull(warehouse + 1)
    fun warehouseReady() = nextWarehouse()?.let { hasAll(it.second.requires) } ?: false
    @Synchronized fun upgradeWarehouse(): Boolean {
        val l = nextWarehouse() ?: return false
        if (!warehouseReady() || !pay(l.second.cost)) return false
        warehouse++; done(if (warehouse == 1) "store_1" else null); return true
    }

    fun dockReady() = !dock && (Data.dock?.let { hasAll(it.requires) } ?: true)
    @Synchronized fun buildDock(): Boolean {
        if (!dockReady() || !pay(Data.dock?.cost ?: Data.Cost(0, 60, 20))) return false
        dock = true; supplyT = Data.supplyEvery - 8; done("dock"); return true
    }

    fun fieldReady() = !field && (Data.field?.let { hasAll(it.requires) } ?: false)
    @Synchronized fun buildField(): Boolean {
        if (!fieldReady() || !pay(Data.field?.cost)) return false
        field = true; done(null); return true
    }

    /** 배 강화·항구 개방·등대·장터: 01 progression 항목 */
    fun unlockReady(u: Data.Unlock) = !has(u.key) && hasAll(u.requires) && warehouse >= u.needWarehouse && u.cost != null &&
        (!u.key.startsWith("ship_") || minPart() >= Fleet.needForShip(u.key.removePrefix("ship_").toIntOrNull() ?: 0))

    // ---------- 배 부품 ----------
    fun partNext(id: String): Fleet.Level? {
        val p = Fleet.part(id) ?: return null
        val lv = partLevel(id)
        if (lv >= Fleet.maxLevel(shipLevel)) return null
        return p.levels.getOrNull(lv)?.takeIf { it.reqShip <= shipLevel }
    }
    @Synchronized fun buyPart(id: String): Boolean {
        val n = partNext(id) ?: return false
        val before = lookStage()
        if (!pay(n.cost)) return false
        parts[id] = partLevel(id) + 1
        if (lookStage() > before) card("배가 달라졌어요", "부품을 손봐서 배 모습이 바뀌었어요")
        bump("part_upgrade")
        save(); return true
    }

    // ---------- 배 꾸미기 ----------
    fun decoReady(d: Fleet.Deco) = d.conds.all { c ->
        when (c.type) {
            "game_flag" -> has(c.key)
            "counter_at_least" -> (if (c.key == "treasures") treasures else voyages) >= c.value
            "any_promoted_crew_level_at_least" -> promoted.any { crewLevel(it) >= c.value }
            "boss_first_win" -> c.key in bossWins
            "souvenir_milestone_complete" -> c.key in milesDone
            else -> false
        }
    }
    /** 조건이 된 무료 장식을 준다 (지난 기록도 소급). 그 자리가 비어 있으면 바로 단다 */
    fun checkDeco() {
        for (d in Fleet.decos) {
            if (!d.auto || d.id in decoOwned || !decoReady(d)) continue
            decoOwned.add(d.id)
            if (decoOn[d.slot] == null && d.sheet == 1) decoOn[d.slot] = d.id    // 2탄은 자동으로 달지 않음
            card("배 장식을 얻었어요", Fleet.slotName(d.slot) + " · " + d.name)
        }
    }
    @Synchronized fun buyDeco(id: String): Boolean {
        val d = Fleet.deco(id) ?: return false
        if (d.auto || id in decoOwned || !decoReady(d) || goldC < d.gold * 100L) return false
        goldC -= d.gold * 100L; decoOwned.add(id); decoOn[d.slot] = id
        save(); return true
    }
    @Synchronized fun setDeco(slot: String, id: String?): Boolean {
        if (id == null) decoOn.remove(slot) else { if (id !in decoOwned) return false; decoOn[slot] = id }
        save(); return true
    }
    /** 달려 있으면 떼고, 아니면 그 자리에 단다 */
    @Synchronized fun toggleDeco(id: String): Boolean {
        val d = Fleet.deco(id) ?: return false
        if (id !in decoOwned) return false
        if (decoOn[d.slot] == id) decoOn.remove(d.slot) else decoOn[d.slot] = id
        save(); return true
    }

    // ---------- 항해 도감 ----------
    private fun rollSouvenir(port: String) {
        val all = Fleet.souvenirs.filter { it.port == port }
        val left = all.filter { it.id !in souvOwned }
        if (left.isEmpty() || Random.nextFloat() >= (all.firstOrNull()?.chance ?: .2f) + tideV("souvenir_more", 0f)) return
        val s = left.random()
        souvOwned.add(s.id); bump("souvenir_get")
        card("기념품을 얻었어요", s.name + " · 도감 " + souvOwned.size + "/" + Fleet.souvenirs.size)
        checkMilestones()
    }
    fun portsWithSouvenir() = Fleet.souvenirs.filter { it.id in souvOwned }.map { it.port }.toSet().size
    private fun checkMilestones() {
        val ports = Fleet.souvenirs.map { it.port }.toSet().size
        for (m in Fleet.milestones) {
            if (m.id in milesDone) continue
            val ok = if (m.allPorts) portsWithSouvenir() >= ports else souvOwned.size >= m.count
            if (!ok) continue
            milesDone.add(m.id); goldC += m.gold * 100L
            card("도감 보상", m.card + (if (m.gold > 0) "\n금화 +" + m.gold else ""))
        }
        checkDeco()
    }

    // ---------- 부적 뽑기 ----------
    class Draw(val charm: Fleet.Charm, val level: Int, val isNew: Boolean, val refund: Int, val forced: Boolean)
    var lastDraw: Draw? = null; private set
    fun canDraw(gold: Boolean) = if (gold) goldC >= Fleet.gachaGold * 100L else tickets >= Fleet.gachaTickets
    /** 한 번 뽑기. 등급을 먼저 고르고 그 등급 안에서 똑같은 확률로 하나 */
    @Synchronized fun draw(gold: Boolean): Draw? {
        if (Fleet.charms.isEmpty() || !canDraw(gold)) return null
        if (gold) goldC -= Fleet.gachaGold * 100L else tickets -= Fleet.gachaTickets
        bump("gacha_draw")
        val forced = pity >= Fleet.pityAt
        val rar = if (forced) "epic" else {
            val total = Fleet.rarityW.values.sum(); var r = Random.nextInt(total); var pick = "common"
            for ((k, w) in Fleet.rarityW) { if (r < w) { pick = k; break }; r -= w }
            pick
        }
        val pool = Fleet.charms.filter { it.rarity == rar }.ifEmpty { Fleet.charms }
        val ch = pool.random()
        pity = if (ch.rarity == "epic") 0 else pity + 1
        val had = charmLv[ch.id]
        var refund = 0
        val lv = when {
            had == null -> 1
            had < ch.values.size -> had + 1
            else -> { refund = Fleet.dupRefund; tickets += refund; had }
        }
        charmLv[ch.id] = lv
        val d = Draw(ch, lv, had == null, refund, forced)
        lastDraw = d
        save(); return d
    }
    /** 부적을 이 선원에게 단다 (null이면 뗀다). 다른 선원이 달고 있었으면 옮긴다 */
    @Synchronized fun equipCharm(charmId: String, crewId: String?): Boolean {
        if (charmId !in charmLv) return false
        charmOn.entries.removeAll { it.value == charmId }
        if (crewId != null) { if (crewId !in promoted) return false; charmOn[crewId] = charmId }
        save(); return true
    }

    // ---------- 훈련 퍼즐 ----------
    fun today() = (System.currentTimeMillis() + java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())) / (24 * HOUR)
    fun trainCheckDay() {
        val d = today()
        if (d != trainDay) {
            trainDay = d; trainLeft = Fleet.trainPerDay
            trainSet = Fleet.trainings.shuffled(java.util.Random(d)).take(Fleet.trainPerDay).map { it.id }
        }
    }
    /** 도전 시작: 횟수를 하나 쓰고 오늘의 문제를 준다 */
    @Synchronized fun trainStart(): Fleet.Training? {
        trainCheckDay()
        if (trainLeft <= 0) return null
        val id = trainSet.getOrNull(Fleet.trainPerDay - trainLeft)
        trainLeft--
        save()
        return Fleet.trainings.firstOrNull { it.id == id } ?: Fleet.trainings.randomOrNull()
    }
    /** 성공하면 교환 수에 따라 쪽지 3/2/1장 */
    @Synchronized fun trainDone(t: Fleet.Training, swaps: Int): Int {
        val n = when { swaps <= t.optimal -> 3; swaps == t.optimal + 1 -> 2; else -> 1 }
        tickets += n; bump("training_clear"); save(); return n
    }

    // ---------- 선원 ----------
    // 일하는 중이어도 승진할 수 있다 (자동 고용이 같은 알바를 계속 데려오면 영영 못 올리던 문제)
    fun canPromote(id: String) = id !in promoted && completedOf(id) >= Fleet.promoteContracts &&
        (Fleet.crewOf(id)?.let { has(it.unlock) } ?: false)
    @Synchronized fun promote(id: String): Boolean {
        if (!canPromote(id) || goldC < Fleet.promoteGold * 100L) return false
        goldC -= Fleet.promoteGold * 100L
        promoted.add(id); crewLv[id] = 1; crewXp[id] = 0
        workers.removeAll { it.id == id }
        if (aboard.size < slots()) aboard.add(id)
        card("새 선원!", (Fleet.crewOf(id)?.name ?: "") + "이(가) 선원이 됐어요")
        storyDirty = true
        save(); return true
    }
    @Synchronized fun toggleAboard(id: String): Boolean {
        if (id !in promoted) return false
        if (id in aboard) aboard.remove(id) else { if (aboard.size >= slots()) return false; aboard.add(id) }
        save(); return true
    }
    @Synchronized fun buyUnlock(key: String): Boolean {
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
        // 그다음은 해적과 싸울 힘: 배 단계 → 부품 → 선원 레벨. 지금 살 수 있는 게 있으면 그것부터.
        val su = Data.unlock("ship_" + (shipLevel + 1))?.takeIf { !has(it.key) && unlockReady(it) }
        if (su != null && canPay(su.cost)) return Goal(su.key, "배 " + (shipLevel + 1) + "단계", su.cost, true, false)
        val ups = ArrayList<Goal>()
        for (p in Fleet.parts) partNext(p.id)?.let { ups.add(Goal("part:" + p.id, p.name + " Lv" + (partLevel(p.id) + 1), it.cost, true, false)) }
        for (id in aboard.filter { it in promoted }) teachCost(id)?.let { ups.add(Goal("crew:" + id, (Fleet.crewOf(id)?.name ?: "선원") + " Lv" + (crewLevel(id) + 1), Data.Cost(it, 0, 0), true, false)) }
        fun w(g: Goal) = (g.cost?.gold ?: 0) * 10 + (g.cost?.wood ?: 0) + (g.cost?.stone ?: 0) + (if (g.key == "part:cannon") -1 else 0)
        ups.filter { canPay(it.cost) }.minByOrNull { w(it) }?.let { return it }
        if (su != null) return Goal(su.key, "배 " + (shipLevel + 1) + "단계", su.cost, true, false)
        val cands = Data.unlocks.filter { !has(it.key) && it.cost != null && (it.key.startsWith("port_") || it.key == "lighthouse" || it.key == "market") }
        cands.firstOrNull { unlockReady(it) && canPay(it.cost) }?.let { u -> return Goal(u.key, unlockName(u.key), u.cost, true, false) }
        ups.minByOrNull { w(it) }?.let { return it }
        val u = cands.firstOrNull { unlockReady(it) } ?: Data.unlock("ship_" + (shipLevel + 1))?.takeIf { !has(it.key) } ?: return null
        return Goal(u.key, unlockName(u.key), u.cost, unlockReady(u), false)
    }
    private fun unlockName(key: String) = when {
        key.startsWith("ship_") -> "배 " + key.removePrefix("ship_") + "단계"
        key.startsWith("port_") -> Data.port(key).name + " 항로"
        key == "lighthouse" -> "등대"
        else -> "장터"
    }

    fun goalProgress(g: Goal): String {
        val c = g.cost ?: return ""
        return listOfNotNull(
            if (c.wood > 0) "나무 " + min(wood, c.wood) + "/" + c.wood else null,
            if (c.stone > 0) "돌 " + min(stone, c.stone) + "/" + c.stone else null,
            if (c.gold > 0) "금화 " + big(min(gold, c.gold)) + "/" + big(c.gold) else null).joinToString(" · ")
    }

    private fun autoBuildStep() {
        val g = goal() ?: return
        if (!g.basic || !g.ready || (g.cost?.gold ?: 0) > 0 || !canPay(g.cost)) return
        val ok = when (g.key) { "house" -> buildHouse(); "ware" -> upgradeWarehouse(); "dock" -> buildDock(); "field" -> buildField(); else -> false }
        if (ok) card("섬이 자랐어요", g.name + " 완성!")
    }

    /** 홈 화면에서 떠다니는 물건을 톡 쳤을 때. 보상 글을 돌려준다. */
    @Synchronized fun pickUp(kind: Int): String {
        finds++; bump("floater_pick")
        val lv = shipLevel
        val text = when (kind) {
            1 -> { val g = Math.round((20 + kotlin.random.Random.nextInt(21)) * lv * (1 + tree("find"))); goldC += g * 100L; mapPieces++; "보물상자! 금화 +$g, 지도 조각 +1" }
            2 -> { val w = 4 + kotlin.random.Random.nextInt(5); add(w, 0); "갈매기가 떨어뜨린 나무 +$w" }
            else -> if (kotlin.random.Random.nextFloat() < .3f) { mapPieces++; "유리병 속 지도 조각 +1" }
                    else { val g = Math.round((3 + kotlin.random.Random.nextInt(6)) * lv * (1 + tree("find"))); goldC += g * 100L; "유리병 속 금화 +$g" }
        }
        save(); return text
    }

    /** 지도 퍼즐을 다 맞췄을 때: 조각을 쓰고 다음 항해에 보물섬에 들른다 */
    fun pack() { packed = true; bump("cargo_pack"); save() }

    fun solveMap(): Boolean {
        if (mapPieces < MAP_NEED || treasurePending) return false
        mapPieces -= MAP_NEED; treasurePending = true; save(); return true
    }

    fun reset() {
        wood = 12; stone = 4; goldC = 0; houses = 0; warehouse = 0; dock = false; field = false; lighthouse = false; market = false
        shipLevel = 1; route = "port_haesol"; supplyT = 0f; voy = null; voyages = 0; workers.clear(); completed.clear()
        flags.clear(); opened.clear(); read.clear(); eventLast.clear(); lastEvent = ""; cards.clear(); log.clear(); report = null
        parts.clear(); partStars.clear(); promoted.clear(); aboard.clear(); crewLv.clear(); crewXp.clear(); decoOwned.clear(); decoOn.clear(); missionQ = null; missionsDone.clear()
        bossWins.clear(); souvOwned.clear(); milesDone.clear(); tickets = 0; charmLv.clear(); charmOn.clear(); pity = 0; trainDay = -1L; trainLeft = 0
        ordDay = -1L; ordIds = emptyList(); ordProg.clear(); ordDone.clear(); weekStart = -1L; weekCount = 0; weekGot = false
        storyDirty = true
        save()
    }
}
