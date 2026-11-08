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
               val skills: List<Skill>, val line: String, val sheet: String, val row: Int, val portrait: Int)
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
    private var loaded = false

    fun load(c: Context) {
        if (loaded) return
        loaded = true
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
    }

    fun part(id: String) = parts.firstOrNull { it.id == id }
    fun crewOf(id: String) = crew.firstOrNull { it.id == id }
    fun maxLevel(shipLevel: Int) = maxPer[shipLevel] ?: (shipLevel * 5)
    /** 큰 배 단계를 올리려면 부품 4개가 모두 이 레벨 이상 */
    fun needForShip(target: Int) = needFor[target] ?: 0
    fun slots(shipLevel: Int) = slots[shipLevel] ?: (shipLevel + 1)
    fun lookThreshold(shipLevel: Int) = lookThreshold[shipLevel] ?: (shipLevel * 5 - 2)

    /** 같이 탄 선원들의 스킬·짝 보너스를 효과별로 더하고 상한을 씌운다 */
    fun bonus(ids: List<String>, level: (String) -> Int): Map<String, Float> {
        val m = HashMap<String, Float>()
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
