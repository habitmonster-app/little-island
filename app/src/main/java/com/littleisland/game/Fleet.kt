package com.littleisland.game

import android.content.Context
import org.json.JSONObject

/**
 * 배 부품(19_ship_parts.json)과 선원(20_crew.json) 기획 데이터.
 * 부품은 몇 분마다 올리는 짧은 성장, 선원은 항해하며 자라는 긴 성장을 맡는다.
 */
object Fleet {
    class Level(val value: Float, val cost: Data.Cost, val reqShip: Int, val lossCut: Float)
    class Part(val id: String, val name: String, val desc: String, val icon: Int, val levels: List<Level>)
    class Skill(val level: Int, val id: String, val name: String, val effect: String, val value: Float, val icon: Int, val desc: String)
    class Crew(val id: String, val name: String, val role: String, val roleDesc: String, val unlock: String,
               var skills: List<Skill>, val line: String, val sheet: String, val row: Int, val portrait: Int)
    class Synergy(val a: String, val b: String, val name: String, val effect: String, val value: Float, val desc: String)

    val parts = ArrayList<Part>()
    private val maxPer = HashMap<Int, Int>()
    private val needFor = HashMap<Int, Int>()
    private val lookThreshold = HashMap<Int, Int>()
    val crew = ArrayList<Crew>()
    val synergies = ArrayList<Synergy>()
    private val slots = HashMap<Int, Int>()
    val caps = HashMap<String, Float>()
    var promoteContracts = 2; private set
    var promoteGold = 200; private set
    var xpPerVoyage = 10; private set
    var xpTo = listOf(30, 60, 100, 150, 220, 300, 400, 520, 660); private set
    var maxCrewLevel = 10; private set
    /** 21_crew_stories.json: 갑판 대사, 싸움 대사, 레벨 업 한마디, 짝 대사 */
    class Voice(val deck: List<String>, val fight: List<String>, val levelUp: String)
    class Pair(val ids: List<String>, val speakers: List<String>, val lines: List<String>, val minLv: Int = 1)
    val voices = HashMap<String, Voice>()
    /** 22_ship_cosmetics.json: 배 꾸미기 (깃발·뱃머리·돛·갑판). 능력치 없이 모양만 */
    class Cond(val type: String, val key: String, val value: Int)
    class Deco(val id: String, val slot: String, val name: String, val icon: Int, val desc: String, val howText: String,
               val auto: Boolean, val gold: Int, val conds: List<Cond>, val sheet: Int = 1) {
        /** 고르는 화면 아이콘 이름 ("deco:3" 또는 2탄 "deco2:3") */
        val iconKey get() = (when (sheet) { 2 -> "deco2:"; 3 -> "deco3:"; else -> "deco:" }) + icon
        val onSheet get() = when (sheet) { 2 -> "ship_deco_on_2"; 3 -> "ship_deco_on_3"; else -> "ship_deco_on" }
        val iconSheet get() = when (sheet) { 2 -> "ship_deco_2"; 3 -> "ship_deco_3"; else -> "ship_deco" }
    }
    val decos = ArrayList<Deco>()
    /** 23_crew_missions.json: 선원 특별 항해 (선원마다 2개) */
    class Mission(val id: String, val crew: String, val title: String, val summary: String,
                  val crewLv: Int, val aboard: List<String>, val parts: Map<String, Int>, val shipLv: Int, val port: String,
                  val flag: String?, val prev: String?, val durMult: Float, val pirateAdd: Float, val mustWin: Boolean,
                  val gold: Int, val wood: Int, val stone: Int, val maps: Int, val xp: Int, val cosmetic: String?,
                  val homeLine: String, val resultCard: String)
    val missions = ArrayList<Mission>()
    fun mission(id: String?) = missions.firstOrNull { it.id == id }

    /** 25_pirate_bosses.json: 해적 우두머리 (보통 해적 대신 가끔 나옴) */
    class Boss(val id: String, val name: String, val captain: String, val minShip: Int, val unlock: String, val ports: List<String>,
               val chance: Float, val sprite: Int, val face: Int, val rewardGold: Int, val rewardMaps: Int, val rewardDeco: String?,
               val appear: String, val loseLine: String, val winLine: String, val crewLines: Map<String, String>,
               val storyId: String, val storyTitle: String, val storyBody: String)
    val bosses = ArrayList<Boss>()
    fun boss(id: String?) = bosses.firstOrNull { it.id == id }

    /** 26_souvenirs.json: 항해 도감 */
    class Souvenir(val id: String, val port: String, val name: String, val desc: String, val icon: Int, val chance: Float, val rare: Boolean)
    class Milestone(val id: String, val count: Int, val allPorts: Boolean, val gold: Int, val deco: String?, val card: String)
    val souvenirs = ArrayList<Souvenir>()
    val milestones = ArrayList<Milestone>()

    /** 28_crew_charms.json: 선원 부적과 뽑기, 훈련 퍼즐 */
    class Charm(val id: String, val name: String, val rarity: String, val effect: String, val values: List<Float>, val role: String, val icon: Int, val desc: String)
    class Training(val id: String, val initial: List<String>, val target: List<String>, val optimal: Int)
    val charms = ArrayList<Charm>()
    val trainings = ArrayList<Training>()
    var gachaTickets = 10; private set
    var gachaGold = 500; private set
    var rarityW = mapOf("common" to 7000, "rare" to 2500, "epic" to 500); private set
    var pityAt = 30; private set
    var dupRefund = 3; private set
    var trainPerDay = 3; private set
    var trainSeconds = 60; private set
    var trainMaxSwaps = 4; private set
    fun charm(id: String?) = charms.firstOrNull { it.id == id }
    fun rarityName(r: String) = when (r) { "epic" -> "영웅"; "rare" -> "희귀"; else -> "일반" }
    /** 선원 이야기 그림 칸 (20_crew 순서와 같다) */
    fun artIndex(crewId: String) = crew.indexOfFirst { it.id == crewId }
    val SLOTS = listOf("flag", "figurehead", "sail", "deck")
    fun slotName(s: String) = when (s) { "flag" -> "깃발"; "figurehead" -> "뱃머리"; "sail" -> "돛 무늬"; else -> "갑판" }
    fun deco(id: String) = decos.firstOrNull { it.id == id }
    val pairs = ArrayList<Pair>()
    private var loaded = false

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        loadMissions(c)
        loadOrder7(c)
        loadOrder9(c)
        loadOrder10(c)
        try {
            val d = JSONObject(c.assets.open("19_ship_parts.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            val ps = d.getJSONArray("parts")
            for (i in 0 until ps.length()) {
                val p = ps.getJSONObject(i)
                val lv = p.getJSONArray("levels")
                val levels = (0 until lv.length()).map { k ->
                    val l = lv.getJSONObject(k); val co = l.getJSONObject("cost")
                    Level(l.getDouble("value").toFloat(), Data.Cost(co.optInt("gold"), co.optInt("wood"), co.optInt("stone")),
                        l.optInt("requires_ship_level", 1), l.optJSONObject("secondary_effects")?.optDouble("bad_event_loss_reduction", 0.0)?.toFloat() ?: 0f)
                }
                parts.add(Part(p.getString("id"), p.getString("name"), p.optString("desc"), p.optInt("icon_index", i), levels))
            }
            d.optJSONObject("max_level_per_ship_level")?.let { o -> o.keys().forEach { k -> maxPer[k.toInt()] = o.getInt(k) } }
            d.optJSONObject("ship_upgrade_needs_parts_level")?.let { o -> o.keys().forEach { k -> needFor[k.toInt()] = o.getInt(k) } }
            d.optJSONObject("look_stages")?.optJSONObject("threshold_by_ship_level")?.let { o -> o.keys().forEach { k -> lookThreshold[k.toInt()] = o.getInt(k) } }
        } catch (e: Exception) { }
        try {
            val d = JSONObject(c.assets.open("20_crew.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            d.optJSONObject("slots_by_ship_level")?.let { o -> o.keys().forEach { k -> slots[k.toInt()] = o.getInt(k) } }
            d.optJSONObject("promote_rule")?.let { promoteContracts = it.optInt("completed_contracts", 2); promoteGold = it.optInt("cost_gold", 200) }
            xpPerVoyage = d.optInt("xp_per_voyage", 10)
            d.optJSONArray("xp_to_next")?.let { a -> xpTo = (0 until a.length()).map { a.getInt(it) } }
            maxCrewLevel = d.optInt("max_level", 10)
            val cs = d.getJSONArray("crew")
            for (i in 0 until cs.length()) {
                val x = cs.getJSONObject(i)
                val sk = x.getJSONArray("skills")
                crew.add(Crew(x.getString("worker_id"), x.optString("name"), x.optString("crew_role"), x.optString("role_desc"), x.optString("unlock", "start"),
                    (0 until sk.length()).map { k -> val s = sk.getJSONObject(k)
                        Skill(s.getInt("level"), s.optString("id"), s.optString("name"), s.optString("effect"), s.getDouble("value").toFloat(), s.optInt("icon_index"), s.optString("desc")) },
                    x.optString("deck_line"), if (x.optString("deck_sprite").contains("_b")) "crew_deck_b" else "crew_deck_a", x.optInt("deck_row"), x.optInt("portrait_index", i)))
            }
            d.optJSONObject("effect_caps")?.let { o -> o.keys().forEach { k -> caps[k] = o.getDouble(k).toFloat() } }
            val sy = d.optJSONArray("synergy")
            if (sy != null) for (i in 0 until sy.length()) {
                val s = sy.getJSONObject(i); val ids = s.getJSONArray("ids")
                synergies.add(Synergy(ids.getString(0), ids.getString(1), s.optString("name"), s.optString("effect"), s.getDouble("value").toFloat(), s.optString("desc")))
            }
        } catch (e: Exception) { }
        loadOrder8(c)
        try {
            val d = JSONObject(c.assets.open("21_crew_stories.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            fun strs(a: org.json.JSONArray?) = if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }
            val cs = d.getJSONArray("crew")
            for (i in 0 until cs.length()) {
                val x = cs.getJSONObject(i)
                voices[x.getString("worker_id")] = Voice(strs(x.optJSONArray("deck_lines")), strs(x.optJSONArray("fight_lines")), x.optString("levelup_line"))
            }
            val pl = d.optJSONArray("pair_lines")
            if (pl != null) for (i in 0 until pl.length()) {
                val x = pl.getJSONObject(i)
                pairs.add(Pair(strs(x.optJSONArray("ids")), strs(x.optJSONArray("speakers")), strs(x.optJSONArray("lines"))))
            }
        } catch (e: Exception) { }
        try {
            val d = JSONObject(c.assets.open("22_ship_cosmetics.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            val it0 = d.getJSONArray("items")
            for (i in 0 until it0.length()) {
                val x = it0.getJSONObject(i); val a = x.optJSONObject("acquisition") ?: JSONObject()
                val all = a.optJSONArray("all_of")
                val conds = if (all == null) emptyList() else (0 until all.length()).map { k -> val o = all.getJSONObject(k)
                    Cond(o.optString("type"), o.optString("key", o.optString("counter")), o.optInt("value", o.optInt("level"))) }
                decos.add(Deco(x.getString("id"), x.optString("slot"), x.optString("name"), x.optInt("icon_index", i), x.optString("desc"),
                    x.optString("how_to_get_text"), a.optString("mode") != "manual_purchase", a.optJSONObject("cost")?.optInt("gold") ?: 0, conds))
            }
        } catch (e: Exception) { }
        loadOrder12(c)
        loadOrder13(c)
        loadOrder14(c)
    }

    // ---------- 12차: 칭호, 날씨, 항구 의뢰, 계절, 손님 배 ----------
    class Title(val id: String, val name: String, val short: String, val tier: String, val icon: Int, val type: String, val key: String, val value: Int)
    class Weather(val id: String, val name: String, val weight: Int, val seasons: List<String>, val time: String, val minShip: Int,
                  val cardLine: String, val crewLine: String, val kind: String, val value: Float, val layer: String, val icon: Int)
    class PortReq(val id: String, val port: String, val client: String, val title: String, val short: String, val needW: Int, val needS: Int,
                  val gold: Int, val tickets: Int, val minShip: Int, val thanks: String)
    class Season(val id: String, val name: String, val months: List<Int>, val deco: List<Int>, val islandLine: String, val crewLines: Map<String, String>,
                 val festId: String, val festName: String, val festDays: List<Int>, val festShort: String, val festTickets: Int, val festGold: Int)
    class Res(val wood: Int, val stone: Int, val gold: Int, val tickets: Int)
    class Offer(val give: Res, val get: Res, val short: String)
    class Visitor(val id: String, val name: String, val sprite: Int, val face: Int, val hello: String, val offers: List<Offer>, val bye: String, val minShip: Int)
    val titles = ArrayList<Title>()
    val weathers = ArrayList<Weather>()
    val reqs = ArrayList<PortReq>()
    var reqMax = 3; private set
    var reqRefreshH = 6; private set
    val seasons = ArrayList<Season>()
    val visitors = ArrayList<Visitor>()
    var visEvery = listOf(90, 240); private set
    var visStay = 30; private set
    fun title(id: String?) = titles.firstOrNull { it.id == id }
    fun weather(id: String?) = weathers.firstOrNull { it.id == id }
    fun req(id: String?) = reqs.firstOrNull { it.id == id }
    fun visitor(id: String?) = visitors.firstOrNull { it.id == id }
    fun season(month: Int) = seasons.firstOrNull { month in it.months }
    private fun res(o: JSONObject?) = Res(o?.optInt("wood") ?: 0, o?.optInt("stone") ?: 0, o?.optInt("gold") ?: 0, o?.optInt("tickets") ?: 0)
    // ---------- 13차: 바다 동물 도감, 주간 바다 소식, 손님 이야기, 선원 날씨 말 ----------
    class Creature(val id: String, val kind: Int, val name: String, val short: String, val firstLine: String, val seenBy: String, val reward: Res, val icon: Int)
    class BookMile(val count: Int, val reward: Res, val titleName: String)
    class Tide(val id: String, val name: String, val short: String, val news: String, val kind: String, val value: Float, val target: String, val icon: Int)
    class VStory(val id: String, val visitor: String, val trades: Int, val title: String, val cardLine: String, val reward: Res, val art: Int)
    val creatures = ArrayList<Creature>()
    val bookMiles = ArrayList<BookMile>()
    val tides = ArrayList<Tide>()
    val vstories = ArrayList<VStory>()
    val weatherLines = HashMap<String, Map<String, String>>()
    fun creature(kind: Int) = creatures.firstOrNull { it.kind == kind }
    // ---------- 14차: 바닷속 ----------
    class Garden(val voyages: Int, val coral: Int, val seaweed: Int, val news: String)
    val garden = ArrayList<Garden>()
    var pearlChance = .5f; private set
    var pearlReward = Res(0, 0, 80, 1); private set
    var pearlLine = ""; private set
    val seabedLines = HashMap<String, List<String>>()
    fun gardenAt(voyages: Int) = garden.lastOrNull { voyages >= it.voyages }
    private fun loadOrder14(c: Context) {
        try { val d = json(c, "52_seabed.json")
            d.optJSONArray("creatures")?.let { a -> for (i in 0 until a.length()) { val x = a.getJSONObject(i)
                creatures.add(Creature(x.getString("id"), x.optInt("kind"), x.optString("name"), x.optString("short"), x.optString("first_line"), x.optString("seen_by"), res(x.optJSONObject("reward")), 100 + x.optInt("icon_index", i))) } }
            d.optJSONArray("garden")?.let { a -> for (i in 0 until a.length()) { val x = a.getJSONObject(i); garden.add(Garden(x.optInt("voyages"), x.optInt("coral"), x.optInt("seaweed"), x.optString("news"))) } }
            d.optJSONObject("pearl")?.let { p -> pearlChance = p.optDouble("chance_per_hour", .5).toFloat(); pearlReward = res(p.optJSONObject("reward")); pearlLine = p.optString("line") }
        } catch (e: Exception) { }
        try { val o = json(c, "53_seabed_lines.json").getJSONObject("lines"); o.keys().forEach { k -> seabedLines[k] = strs(o.getJSONArray(k)) } } catch (e: Exception) { }
        // 도감 8마리 칭호 → 전체 수로 (바닷속 친구까지)
    }
    private fun loadOrder13(c: Context) {
        try { val d = json(c, "48_sea_creature_book.json"); val a = d.getJSONArray("creatures"); for (i in 0 until a.length()) { val x = a.getJSONObject(i)
            creatures.add(Creature(x.getString("id"), x.optInt("kind", i), x.optString("name"), x.optString("short"), x.optString("first_line"), x.optString("seen_by"), res(x.optJSONObject("reward")), x.optInt("icon_index", i))) }
            d.optJSONArray("milestones")?.let { m -> for (i in 0 until m.length()) { val x = m.getJSONObject(i); val r = x.optJSONObject("reward")
                bookMiles.add(BookMile(x.optInt("count"), res(r), r?.optString("title_name") ?: "")) } }
            bookMiles.firstOrNull { it.titleName.isNotEmpty() }?.let { titles.add(Title("title_creature_book", it.titleName, "바다 동물을 모두 보기", "gold", 2, "creature_book", "", it.count)) }
        } catch (e: Exception) { }
        try { val a = json(c, "49_weekly_tides.json").getJSONArray("tides"); for (i in 0 until a.length()) { val x = a.getJSONObject(i); val ef = x.optJSONObject("effect")
            tides.add(Tide(x.getString("id"), x.optString("name"), x.optString("short"), x.optString("news"), ef?.optString("kind") ?: "none", (ef?.optDouble("value") ?: 0.0).toFloat(), ef?.optString("target") ?: "", x.optInt("icon_index", i))) } } catch (e: Exception) { }
        try { val a = json(c, "50_visitor_stories.json").getJSONArray("stories"); for (i in 0 until a.length()) { val x = a.getJSONObject(i)
            vstories.add(VStory(x.getString("id"), x.optString("visitor"), x.optInt("trades", 1), x.optString("title"), x.optString("card_line"), res(x.optJSONObject("reward")), x.optInt("art_index"))) } } catch (e: Exception) { }
        try { val o = json(c, "51_crew_weather_lines.json").getJSONObject("lines"); o.keys().forEach { k -> val m = o.getJSONObject(k); val h = HashMap<String, String>(); m.keys().forEach { w -> h[w] = m.getString(w) }; weatherLines[k] = h } } catch (e: Exception) { }
    }
    private fun loadOrder12(c: Context) {
        try { val a = json(c, "41_captain_titles.json").getJSONArray("titles"); for (i in 0 until a.length()) { val x = a.getJSONObject(i); val k = x.getJSONObject("condition")
            titles.add(Title(x.getString("id"), x.optString("name"), x.optString("short"), x.optString("tier"), x.optInt("icon_index", i), k.optString("type"),
                k.optString("counter", k.optString("boss_id")), k.optInt("value"))) } } catch (e: Exception) { }
        try { val a = json(c, "42_sea_weather.json").getJSONArray("weather"); for (i in 0 until a.length()) { val x = a.getJSONObject(i)
            val ef = x.optJSONObject("effect"); val fx = x.optJSONObject("fx")
            weathers.add(Weather(x.getString("id"), x.optString("name"), x.optInt("weight", 1), strs(x.optJSONArray("seasons")), x.optString("time", "any"), x.optInt("min_ship_level", 1),
                x.optString("card_line"), x.optString("crew_line"), ef?.optString("kind") ?: "none", (ef?.optDouble("value") ?: 0.0).toFloat(), fx?.optString("layer") ?: "none", fx?.optInt("icon_index") ?: i)) } } catch (e: Exception) { }
        try { val d = json(c, "43_port_requests.json"); d.optJSONObject("rules")?.let { reqMax = it.optInt("active_max", 3); reqRefreshH = it.optInt("refresh_hours", 6) }
            val a = d.getJSONArray("requests"); for (i in 0 until a.length()) { val x = a.getJSONObject(i); val n = x.optJSONObject("need"); val r = x.optJSONObject("reward")
            reqs.add(PortReq(x.getString("id"), x.optString("port"), x.optString("client"), x.optString("title"), x.optString("short"), n?.optInt("wood") ?: 0, n?.optInt("stone") ?: 0,
                r?.optInt("gold") ?: 0, r?.optInt("tickets") ?: 0, x.optInt("min_ship_level", 1), x.optString("thanks"))) } } catch (e: Exception) { }
        try { val a = json(c, "44_crew_pair_talks.json").getJSONArray("talks"); for (i in 0 until a.length()) { val x = a.getJSONObject(i); val ls = x.getJSONArray("lines")
            val who = (0 until ls.length()).map { ls.getJSONObject(it).optString("who") }; val tx = (0 until ls.length()).map { ls.getJSONObject(it).optString("text") }
            pairs.add(Pair(strs(x.optJSONArray("pair")), who, tx, x.optInt("min_crew_level", 1))) } } catch (e: Exception) { }
        try { val a = json(c, "45_seasons.json").getJSONArray("seasons"); for (i in 0 until a.length()) { val x = a.getJSONObject(i)
            val cl = HashMap<String, String>(); x.optJSONObject("crew_lines")?.let { o -> o.keys().forEach { k -> cl[k] = o.getString(k) } }
            val f = x.optJSONObject("festival"); val fr = f?.optJSONObject("reward")
            seasons.add(Season(x.getString("id"), x.optString("name"), (x.optJSONArray("months")?.let { m -> (0 until m.length()).map { m.getInt(it) } }) ?: emptyList(),
                (x.optJSONArray("deco_cells")?.let { m -> (0 until m.length()).map { m.getInt(it) } }) ?: emptyList(), x.optString("island_line"), cl,
                f?.optString("id") ?: "", f?.optString("name") ?: "", (f?.optJSONArray("days")?.let { m -> (0 until m.length()).map { m.getInt(it) } }) ?: emptyList(),
                f?.optString("short") ?: "", fr?.optInt("tickets") ?: 0, fr?.optInt("gold") ?: 0)) } } catch (e: Exception) { }
        try { val d = json(c, "46_island_visitors.json"); d.optJSONObject("rules")?.let { r -> r.optJSONArray("every_minutes")?.let { e -> visEvery = listOf(e.getInt(0), e.getInt(1)) }; visStay = r.optInt("stay_minutes", 30) }
            val a = d.getJSONArray("visitors"); for (i in 0 until a.length()) { val x = a.getJSONObject(i); val of = x.getJSONArray("offers")
            visitors.add(Visitor(x.getString("id"), x.optString("name"), x.optInt("sprite_index", i), x.optInt("face_index", i), x.optString("hello"),
                (0 until of.length()).map { val o = of.getJSONObject(it); Offer(res(o.optJSONObject("give")), res(o.optJSONObject("get")), o.optString("short")) },
                x.optString("bye"), x.optInt("min_ship_level", 1))) } } catch (e: Exception) { }
    }

    private fun json(c: Context, f: String) = JSONObject(c.assets.open(f).bufferedReader(Charsets.UTF_8).use { it.readText() })
    private fun strs(a: org.json.JSONArray?) = if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

    /** 31_daily_orders.json: 오늘의 항구 주문 (하루 3개, 7일에 15개면 주간 상자) */
    class Order(val id: String, val counter: String, val target: Int, val diff: String, val title: String, val short: String,
                val tickets: Int, val gold: Int, val minShip: Int, val icon: Int)
    val orders = ArrayList<Order>()
    var dailyCount = 3; private set
    var weeklyGoal = 15; private set
    var weeklyTickets = 10; private set
    var weeklyGold = 500; private set
    fun order(id: String) = orders.firstOrNull { it.id == id }
    /** 33_crew_sea_events.json: 그 선원이 탔을 때 가끔 생기는 작은 일 (귀항 카드 한 줄 + 머리 위 감정) */
    class CrewEvent(val id: String, val crew: String, val minLv: Int, val title: String, val line: String, val emote: String, val kind: String, val value: Int, val with: String? = null)
    /** 40_crew_voices_2.json: Lv11 이상 선원이 섞어 하는 말 */
    val voices2 = HashMap<String, Voice>()
    private val charms2 = ArrayList<Charm>()
    val crewEvents = ArrayList<CrewEvent>()
    var crewEvChance = .25f; private set
    val EMOTES = listOf("heart", "note", "sweat", "star", "zzz", "surprise", "question", "angry")
    fun crewEvent(id: String?) = crewEvents.firstOrNull { it.id == id }
    private fun loadOrder10(c: Context) {
        for (file in listOf("33_crew_sea_events.json", "35_crew_sea_events_2.json")) try {
            val d = json(c, file)
            if (file.startsWith("33")) crewEvChance = d.optDouble("chance_per_voyage", .25).toFloat()
            val a = d.getJSONArray("events")
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i); val e = x.optJSONObject("effect") ?: JSONObject()
                if (crewEvents.any { it.id == x.getString("id") }) continue
                crewEvents.add(CrewEvent(x.getString("id"), x.optString("crew"), x.optInt("min_crew_level", 1), x.optString("card_title"), x.optString("card_line"),
                    x.optString("emote", "heart"), e.optString("kind", "none"), e.optInt("value"), x.optString("with").takeIf { it.isNotEmpty() }))
            }
        } catch (e: Exception) { }
        // 36: 주문 2탄 / 37: 부적 2탄 (28번 뒤에 붙임) / 40: Lv11 이상 대사
        try {
            val a = json(c, "36_daily_orders_2.json").getJSONArray("orders")
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i); val r = x.optJSONObject("reward") ?: JSONObject()
                if (orders.any { it.id == x.getString("id") }) continue
                orders.add(Order(x.getString("id"), x.optString("counter"), x.optInt("target", 1), x.optString("difficulty", "easy"), x.optString("title"),
                    x.optString("short"), r.optInt("tickets"), r.optInt("gold"), x.optInt("min_ship_level", 1), x.optInt("icon_index", 2)))
            }
        } catch (e: Exception) { }
        for (ch in charms2) if (charms.none { it.id == ch.id }) charms.add(ch)
        try {
            val a = json(c, "40_crew_voices_2.json").getJSONArray("crew")
            fun strs(a: org.json.JSONArray?) = if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }
            for (i in 0 until a.length()) { val x = a.getJSONObject(i)
                voices2[x.getString("worker_id")] = Voice(strs(x.optJSONArray("deck_lines")), strs(x.optJSONArray("fight_lines")), "") }
        } catch (e: Exception) { }
        // 34_training_puzzles_2.json: 훈련 퍼즐 30개 더 (3×3 판 포함)
        try {
            val pz = json(c, "34_training_puzzles_2.json").optJSONObject("training_puzzle")?.optJSONArray("puzzles")
            fun strs(a: org.json.JSONArray) = (0 until a.length()).map { a.getString(it) }
            if (pz != null) for (i in 0 until pz.length()) { val x = pz.getJSONObject(i)
                if (trainings.any { it.id == x.getString("id") }) continue
                trainings.add(Training(x.getString("id"), strs(x.getJSONArray("initial")), strs(x.getJSONArray("target")), x.optInt("optimal_swaps", 1))) }
        } catch (e: Exception) { }
    }

    private fun loadOrder9(c: Context) {
        try {
            val d = json(c, "31_daily_orders.json")
            dailyCount = d.optInt("daily_count", 3); weeklyGoal = d.optInt("weekly_goal", 15)
            d.optJSONObject("weekly_reward")?.let { weeklyTickets = it.optInt("tickets", 10); weeklyGold = it.optInt("gold", 500) }
            val a = d.getJSONArray("orders")
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i); val r = x.optJSONObject("reward") ?: JSONObject()
                orders.add(Order(x.getString("id"), x.optString("counter"), x.optInt("target", 1), x.optString("difficulty", "easy"), x.optString("title"),
                    x.optString("short"), r.optInt("tickets"), r.optInt("gold"), x.optInt("min_ship_level", 1), x.optInt("icon_index", 2)))
            }
        } catch (e: Exception) { }
    }

    /** 29_short_texts.json: 설명 창에 쓰는 짧은 한 줄 (분류 → id → 글) */
    val short = HashMap<String, Map<String, String>>()
    fun short(kind: String, id: String?, fallback: String = ""): String = short[kind]?.get(id ?: "")?.takeIf { it.isNotBlank() } ?: fallback

    /** 30_crew_levels_2.json: 선원 Lv11~15와 Lv12·Lv15 새 스킬. 20번 자료를 읽은 뒤에 덧붙인다 */
    private fun loadOrder8(c: Context) {
        try {
            val d = json(c, "29_short_texts.json")
            d.keys().forEach { k -> val o = d.optJSONObject(k) ?: return@forEach
                val m = HashMap<String, String>(); o.keys().forEach { id -> m[id] = o.optString(id) }; short[k] = m }
        } catch (e: Exception) { }
        try {
            val d = json(c, "30_crew_levels_2.json")
            val add = d.optJSONArray("xp_to_next_11_15")
            if (add != null && xpTo.size == maxCrewLevel - 1) {
                xpTo = xpTo + (0 until add.length()).map { add.getInt(it) }
                maxCrewLevel = d.optInt("max_level", maxCrewLevel)
            }
            val sk = d.optJSONArray("skills")
            if (sk != null) for (i in 0 until sk.length()) {
                val x = sk.getJSONObject(i)
                val cr = crewOf(x.optString("crew")) ?: continue
                if (cr.skills.any { it.id == x.optString("id") }) continue
                cr.skills = (cr.skills + Skill(x.getInt("level"), x.optString("id"), x.optString("name"), x.optString("effect"),
                    x.getDouble("value").toFloat(), x.optInt("icon_index"), x.optString("short"))).sortedBy { it.level }
            }
        } catch (e: Exception) { }
    }

    private fun loadOrder7(c: Context) {
        for ((file, off) in listOf("25_pirate_bosses.json" to 0, "39_pirate_bosses_2.json" to 100)) try {
            val a = json(c, file).getJSONArray("bosses")
            for (i in 0 until a.length()) {
                val b = a.getJSONObject(i); val r = b.optJSONObject("first_win_reward") ?: JSONObject()
                val l = b.optJSONObject("lines") ?: JSONObject(); val st = b.optJSONObject("story") ?: JSONObject()
                val cl = HashMap<String, String>(); b.optJSONObject("crew_lines")?.let { o -> o.keys().forEach { k -> cl[k] = o.getString(k) } }
                if (bosses.any { it.id == b.getString("id") }) continue
                bosses.add(Boss(b.getString("id"), b.optString("name"), b.optString("captain"), b.optInt("min_ship_level", 2), b.optString("unlock", "start"),
                    strs(b.optJSONArray("ports")), b.optDouble("chance", .04).toFloat(), off + b.optInt("sprite_index", i), off + b.optInt("captain_icon_index", i),
                    r.optInt("gold"), r.optInt("map_pieces"), if (r.isNull("cosmetic_id")) null else r.optString("cosmetic_id"),
                    l.optString("appear"), l.optString("lose"), l.optString("win"), cl,
                    st.optString("id", "story_" + b.getString("id")), st.optString("title"), st.optString("body")))
            }
            // 우두머리도 해적 목록에 넣어 둔다 (보통 해적 뽑기에는 안 들어감: normal=false, weight 0)
            for (i in 0 until a.length()) {
                val b = a.getJSONObject(i); val wg = b.optJSONArray("win_gold")
                if (Data.pirates.none { it.id == b.getString("id") })
                    Data.pirates.add(Data.Pirate(b.getString("id"), b.optString("name"), b.optString("unlock", "start"), emptyList(), emptyList(), false,
                        strs(b.optJSONArray("ports")), 0, b.optDouble("power", 2.0).toFloat(), intArrayOf(wg?.optInt(0) ?: 50, wg?.optInt(1) ?: 80),
                        b.optDouble("lose", .3).toFloat(), emptyList()))
            }
        } catch (e: Exception) { }
        try {
            val d = json(c, "26_souvenirs.json")
            val a = d.getJSONArray("souvenirs")
            for (i in 0 until a.length()) { val x = a.getJSONObject(i)
                souvenirs.add(Souvenir(x.getString("id"), x.optString("port"), x.optString("name"), x.optString("desc"), x.optInt("icon_index", i),
                    x.optDouble("chance", .2).toFloat(), x.optString("rarity") == "rare")) }
            val m = d.optJSONArray("milestones")
            if (m != null) for (i in 0 until m.length()) { val x = m.getJSONObject(i); val r = x.optJSONObject("reward") ?: JSONObject()
                val cond = x.optJSONObject("condition") ?: JSONObject()
                milestones.add(Milestone(x.getString("id"), x.optInt("count", cond.optInt("value")), cond.optString("kind").contains("port") || x.getString("id").contains("all_ports"),
                    r.optInt("gold"), if (r.isNull("cosmetic_id")) null else r.optString("cosmetic_id"), x.optString("card"))) }
        } catch (e: Exception) { }
        for ((file, sheet) in listOf("27_ship_cosmetics_2.json" to 2, "38_ship_cosmetics_3.json" to 3)) try {
            val d = json(c, file)
            val it0 = d.getJSONArray("items")
            for (i in 0 until it0.length()) {
                val x = it0.getJSONObject(i); val a = x.optJSONObject("acquisition") ?: JSONObject()
                val all = a.optJSONArray("all_of")
                val conds = if (all == null) emptyList() else (0 until all.length()).map { k -> val o = all.getJSONObject(k)
                    Cond(o.optString("type"), o.optString("key", o.optString("boss_id", o.optString("milestone_id", o.optString("counter")))), o.optInt("value", o.optInt("level"))) }
                decos.add(Deco(x.getString("id"), x.optString("slot"), x.optString("name"), x.optInt("icon_index", i), x.optString("desc"),
                    x.optString("how_to_get_text"), a.optString("mode") != "manual_purchase", a.optJSONObject("cost")?.optInt("gold") ?: 0, conds, sheet))
            }
        } catch (e: Exception) { }
        try {
            val a2 = json(c, "37_crew_charms_2.json").getJSONArray("charms")
            for (i in 0 until a2.length()) { val x = a2.getJSONObject(i); val v = x.getJSONArray("value_by_level")
                charms2.add(Charm(x.getString("id"), x.optString("name"), x.optString("rarity"), x.optString("effect"),
                    (0 until v.length()).map { v.getDouble(it).toFloat() }, x.optString("best_for_role"), 100 + x.optInt("icon_index", i), x.optString("desc"))) }
        } catch (e: Exception) { }
        try {
            val d = json(c, "28_crew_charms.json")
            val a = d.getJSONArray("charms")
            for (i in 0 until a.length()) { val x = a.getJSONObject(i); val v = x.getJSONArray("value_by_level")
                charms.add(Charm(x.getString("id"), x.optString("name"), x.optString("rarity"), x.optString("effect"),
                    (0 until v.length()).map { v.getDouble(it).toFloat() }, x.optString("best_for_role"), x.optInt("icon_index", i), x.optString("desc"))) }
            d.optJSONObject("gacha")?.let { g ->
                gachaTickets = g.optInt("cost_tickets", 10); gachaGold = g.optInt("cost_gold", 500)
                g.optJSONObject("rarity_weights")?.let { o -> rarityW = o.keys().asSequence().associateWith { o.getInt(it) } }
                g.optJSONObject("pity_rule")?.let { pityAt = it.optInt("force_epic_if_counter_before_draw_at_least", 30) }
            }
            val tp = d.optJSONObject("training_puzzle")
            if (tp != null) {
                trainPerDay = tp.optInt("plays_per_day", 3); trainSeconds = tp.optInt("timer_seconds", 60); trainMaxSwaps = tp.optInt("max_swaps", 4)
                val pz = tp.optJSONArray("puzzles")
                if (pz != null) for (i in 0 until pz.length()) { val x = pz.getJSONObject(i)
                    trainings.add(Training(x.getString("id"), strs(x.getJSONArray("initial")), strs(x.getJSONArray("target")), x.optInt("optimal_swaps", 1))) }
            }
        } catch (e: Exception) { }
    }

    private fun loadMissions(c: Context) {
        for (file in listOf("23_crew_missions.json", "32_crew_missions_3.json")) try {
            val d = JSONObject(c.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() })
            val a = d.getJSONArray("missions")
            fun nul(o: JSONObject, k: String) = if (o.isNull(k)) null else o.optString(k).takeIf { it.isNotEmpty() }
            for (i in 0 until a.length()) {
                val m = a.getJSONObject(i); val r = m.optJSONObject("requires") ?: JSONObject()
                val v = m.optJSONObject("voyage") ?: JSONObject(); val w = m.optJSONObject("reward") ?: JSONObject()
                val ab = r.optJSONArray("crew_aboard")
                val pa = HashMap<String, Int>(); r.optJSONObject("parts_at_least")?.let { o -> o.keys().forEach { k -> pa[k] = o.getInt(k) } }
                if (missions.any { it.id == m.getString("id") }) continue
                missions.add(Mission(m.getString("id"), m.getString("crew"), m.optString("title"), m.optString("summary"),
                    r.optInt("crew_level_at_least", 1), if (ab == null) emptyList() else (0 until ab.length()).map { ab.getString(it) }, pa,
                    r.optInt("ship_level_at_least", 1), r.optString("port", "port_haesol"), nul(r, "game_flag"), nul(r, "previous_mission_id"),
                    v.optDouble("duration_mult", 1.0).toFloat(), v.optDouble("pirate_chance_add", 0.0).toFloat(),
                    m.optString("success_rule_id") == "win_if_encountered",
                    w.optInt("gold"), w.optInt("wood"), w.optInt("stone"), w.optInt("map_pieces"), w.optInt("crew_xp"), nul(w, "cosmetic_id"),
                    m.optString("home_line"), m.optString("result_card")))
            }
        } catch (e: Exception) { }
    }

    fun part(id: String) = parts.firstOrNull { it.id == id }
    fun crewOf(id: String) = crew.firstOrNull { it.id == id }
    fun maxLevel(shipLevel: Int) = maxPer[shipLevel] ?: (shipLevel * 5)
    /** 큰 배 단계를 올리려면 부품 4개가 모두 이 레벨 이상 */
    fun needForShip(target: Int) = needFor[target] ?: 0
    fun slots(shipLevel: Int) = slots[shipLevel] ?: (shipLevel + 1)
    fun lookThreshold(shipLevel: Int) = lookThreshold[shipLevel] ?: (shipLevel * 5 - 2)

    /** 같이 탄 선원들의 스킬·짝 보너스를 효과별로 더하고 상한을 씌운다 */
    fun bonus(ids: List<String>, level: (String) -> Int, extra: Map<String, Float> = emptyMap()): Map<String, Float> {
        val m = HashMap<String, Float>(extra)
        for (id in ids) {
            val c = crewOf(id) ?: continue
            val lv = level(id)
            for (s in c.skills) if (s.level <= lv) m[s.effect] = (m[s.effect] ?: 0f) + s.value
        }
        for (s in synergies) if (s.a in ids && s.b in ids) m[s.effect] = (m[s.effect] ?: 0f) + s.value
        for ((k, v) in m) caps[k]?.let { m[k] = minOf(v, it) }
        return m
    }

    fun effectName(e: String) = when (e) {
        "cargo_bonus" -> "짐칸"; "speed_bonus" -> "항해 시간"; "pirate_win_bonus" -> "해적 승률"; "sale_bonus" -> "판매 수입"
        "event_good_bonus" -> "좋은 사건"; "repair_after_loss" -> "짐 지키기"; "treasure_find_bonus" -> "보물 찾기"
        "wood_price_bonus" -> "나무값"; "stone_price_bonus" -> "돌값"; else -> e
    }

    /** 부품 효과를 사람이 읽는 말로 */
    fun partText(p: Part, value: Float): String = when (p.id) {
        "hull" -> "짐 +" + pct(value)
        "sail" -> "항해 시간 -" + pct(value)
        "cannon" -> "해적 승률 +" + pts(value)
        else -> "좋은 사건 +" + pts(value)
    }
    fun pct(v: Float) = (v * 100 + .5f).toInt().toString() + "%"
    fun pts(v: Float): String { val x = v * 100; return (if (x % 1f == 0f) x.toInt().toString() else "%.1f".format(x)) + "%p" }
}
