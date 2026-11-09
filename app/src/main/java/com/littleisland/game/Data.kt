package com.littleisland.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 아스트라 기획 데이터(assets 폴더의 json 파일들)를 읽어 둔다. 숫자는 여기서만 가져온다. */
object Data {
    class Cost(val gold: Int, val wood: Int, val stone: Int) {
        fun text(): String = listOfNotNull(
            if (gold > 0) "금화 " + big(gold) else null,
            if (wood > 0) "나무 $wood" else null,
            if (stone > 0) "돌 $stone" else null).joinToString(" · ").ifEmpty { "무료" }
    }
    class Ship(val level: Int, val name: String, val cargoW: Int, val cargoS: Int, val speed: Float,
               val pirateWin: Float, val cost: Cost?, val requires: List<String>, val news: String, val masts: Int, val cannons: Int)
    class Port(val id: String, val name: String, val minShip: Int, val distance: Float, val buyW: Float, val buyS: Float,
               val risk: Float, val workers: List<String>, val colors: List<Int>, val look: String)
    class Unlock(val key: String, val requires: List<String>, val cost: Cost?, val needWarehouse: Int)
    class Worker(val id: String, val name: String, val port: String, val unlock: String, val role: String, val trait: String,
                 val wage: Int, val hours: Int, val woodBonus: Float, val stoneBonus: Float, val saleBonus: Float, val pirateBonus: Float,
                 val look: String)
    class Pirate(val id: String, val name: String, val unlock: String, val requires: List<String>, val disabledBy: List<String>,
                 val normal: Boolean, val ports: List<String>?, val weight: Int, val power: Float, val winGold: IntArray,
                 val lose: Float, val colors: List<Int>)
    class Event(val id: String, val unlock: String, val requires: List<String>, val ports: List<String>?, val needField: Boolean,
                val weight: Int, val gold: Int, val wood: Int, val stone: Int, val repeat: Boolean, val cooldown: Int)
    class HomeLine(val id: String, val who: String, val time: String, val unlock: String, val text: String, val workerId: String?)
    class Building(val id: String, val name: String, val requires: List<String>, val cost: Cost?, val effect: String)

    val ships = ArrayList<Ship>()
    val ports = ArrayList<Port>()
    val unlocks = ArrayList<Unlock>()
    val workers = ArrayList<Worker>()
    val pirates = ArrayList<Pirate>()
    val events = ArrayList<Event>()
    val lines = ArrayList<HomeLine>()
    val houses = ArrayList<Building>()
    val warehouse = ArrayList<Pair<Int, Unlock>>()   // 레벨별 (용량, 조건/비용)
    var dock: Building? = null
    var field: Building? = null
    val extraBuildings = HashMap<String, Building>()  // lighthouse, market

    // 규칙 값 (기본값은 파일이 없을 때 대비)
    var baseSeconds = 720f; var minSeconds = 480f; var maxSeconds = 1800f
    var fPirate = 0.2361111f; var fArrive = 0.4166667f; var fLeave = 0.5833333f
    var gatherRate = 0.055f; var woodFrac = 0.68f; var stoneFrac = 0.32f
    var supplyEvery = 75f; var supplyW = intArrayOf(10, 20); var supplyS = intArrayOf(5, 10)
    var powerCoef = 0.1f; var lighthouseBonus = 0.03f; var winMin = 0.35f; var winMax = 0.97f
    var saleCap = 0.06f; var pirateCap = 0.02f; var marketMul = 1.04f; var fieldBonus = 0.05f
    var eventChance = 0.35f
    var reserveGold = 20; var dailyLimit = 120; var singleLimit = 10
    var talkProb = 0.3f; var talkInterval = 120f

    private var loaded = false

    private fun JSONArray?.strings(): List<String> { val o = ArrayList<String>(); if (this != null) for (i in 0 until length()) o.add(getString(i)); return o }
    private fun JSONObject.cost(k: String): Cost? { val c = optJSONObject(k) ?: return null; return Cost(c.optInt("gold"), c.optInt("wood"), c.optInt("stone")) }
    private fun colors(a: JSONArray?): List<Int> = a.strings().mapNotNull { try { android.graphics.Color.parseColor(it) } catch (e: Exception) { null } }

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        fun read(name: String): JSONObject? = try {
            JSONObject(c.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() })
        } catch (e: Exception) { null }

        read("01_economy.json")?.let { e ->
            val p = e.optJSONArray("progression") ?: JSONArray()
            for (i in 0 until p.length()) {
                val u = p.getJSONObject(i)
                unlocks.add(Unlock(u.getString("unlock"), u.optJSONArray("requires").strings(), u.cost("cost"),
                    u.optJSONObject("requires_building_levels")?.optInt("warehouse") ?: 0))
            }
            e.optJSONObject("voyage")?.let { v ->
                baseSeconds = v.optDouble("base_seconds", 720.0).toFloat()
                minSeconds = v.optDouble("minimum_duration_seconds", 480.0).toFloat()
                maxSeconds = v.optDouble("maximum_duration_seconds", 1800.0).toFloat()
                v.optJSONObject("event_fractions")?.let { f ->
                    fPirate = f.optDouble("pirate_without_specialty", fPirate.toDouble()).toFloat()
                    fArrive = f.optDouble("port_arrive", fArrive.toDouble()).toFloat()
                    fLeave = f.optDouble("port_leave", fLeave.toDouble()).toFloat()
                }
            }
            e.optJSONObject("gathering")?.let { g ->
                gatherRate = g.optDouble("resident_rate_per_second", .055).toFloat()
                woodFrac = g.optDouble("wood_fraction", .68).toFloat(); stoneFrac = g.optDouble("stone_fraction", .32).toFloat()
            }
            e.optJSONObject("money")?.let { m -> saleCap = m.optDouble("worker_sale_bonus_cap", .06).toFloat(); pirateCap = m.optDouble("worker_combat_bonus_cap", .02).toFloat() }
            e.optJSONObject("auto_hire_budget")?.let { a ->
                reserveGold = a.optInt("reserve_gold_default", 20); dailyLimit = a.optInt("daily_limit_gold_default", 120)
                singleLimit = a.optInt("single_hire_limit_gold_default", 10)
            }
        }
        read("02_ships.json")?.optJSONArray("ships")?.let { a ->
            for (i in 0 until a.length()) {
                val s = a.getJSONObject(i)
                ships.add(Ship(s.getInt("level"), s.optString("name"), s.optInt("cargo_wood"), s.optInt("cargo_stone"),
                    s.optDouble("speed", 1.0).toFloat(), s.optDouble("pirate_win", .7).toFloat(), s.cost("upgrade_cost"),
                    s.optJSONArray("upgrade_requires").strings(), s.optString("home_news"), s.optInt("mast_count", 1), s.optInt("cannon_count")))
            }
        }
        read("03_ports.json")?.optJSONArray("ports")?.let { a ->
            for (i in 0 until a.length()) {
                val p = a.getJSONObject(i)
                val buys = p.optJSONObject("buys") ?: JSONObject()
                ports.add(Port(p.getString("id"), p.optString("name"), p.optInt("min_ship_level", 1), p.optDouble("distance", 1.0).toFloat(),
                    buys.optDouble("wood", 1.0).toFloat(), buys.optDouble("stone", 2.0).toFloat(), p.optDouble("pirate_risk", .3).toFloat(),
                    p.optJSONArray("workers").strings(), colors(p.optJSONObject("scene")?.optJSONArray("colors")), p.optString("look")))
            }
        }
        read("04_buildings.json")?.optJSONArray("buildings")?.let { a ->
            for (i in 0 until a.length()) {
                val b = a.getJSONObject(i)
                val id = b.getString("id")
                val bd = Building(id, b.optString("name"), b.optJSONArray("requires").strings(), b.cost("cost"), b.optString("effect"))
                when {
                    id.startsWith("house_") -> houses.add(bd)
                    id == "dock" -> {
                        dock = bd
                        b.optJSONObject("effects")?.let { e ->
                            supplyEvery = e.optDouble("supply_interval_seconds", 75.0).toFloat()
                            e.optJSONArray("supply_wood_range")?.let { r -> supplyW = intArrayOf(r.getInt(0), r.getInt(1)) }
                            e.optJSONArray("supply_stone_range")?.let { r -> supplyS = intArrayOf(r.getInt(0), r.getInt(1)) }
                        }
                    }
                    id == "field" -> { field = bd; fieldBonus = b.optJSONObject("effects")?.optDouble("worker_own_gather_rate_bonus", .05)?.toFloat() ?: .05f }
                    id == "lighthouse" -> { extraBuildings[id] = bd; lighthouseBonus = b.optJSONObject("effects")?.optDouble("pirate_win_bonus", .03)?.toFloat() ?: .03f }
                    id == "market" -> { extraBuildings[id] = bd; marketMul = b.optJSONObject("effects")?.optDouble("sale_gold_multiplier", 1.04)?.toFloat() ?: 1.04f }
                    id == "warehouse" -> {
                        val lv = b.optJSONArray("levels") ?: JSONArray()
                        for (j in 0 until lv.length()) {
                            val l = lv.getJSONObject(j)
                            warehouse.add(l.optInt("capacity") to Unlock("warehouse_" + l.optInt("level"), l.optJSONArray("requires").strings(), l.cost("cost"), 0))
                        }
                    }
                }
            }
        }
        read("05_workers.json")?.let { w ->
            val a = w.optJSONArray("workers") ?: JSONArray()
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i)
                val ef = x.optJSONObject("effects") ?: JSONObject()
                var trait = x.optString("trait")
                x.optJSONObject("trait_values")?.let { tv -> for (k in tv.keys()) trait = trait.replace("{$k}", tv.optDouble(k).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }) }
                workers.add(Worker(x.getString("id"), x.optString("name"), x.optString("port"), x.optString("unlock", "start"), x.optString("role"), trait,
                    x.optInt("wage", 10), x.optInt("work_hours", 6), ef.optDouble("gather_wood_bonus").toFloat(), ef.optDouble("gather_stone_bonus").toFloat(),
                    ef.optDouble("sale_bonus").toFloat(), ef.optDouble("pirate_win_bonus").toFloat(), x.optString("look")))
            }
        }
        read("06_pirates.json")?.let { p ->
            p.optJSONObject("combat_rules")?.optJSONObject("conditional_win_probability")?.let { r ->
                powerCoef = r.optDouble("power_penalty_coefficient", .1).toFloat(); winMin = r.optDouble("minimum", .35).toFloat(); winMax = r.optDouble("maximum", .97).toFloat()
            }
            val a = p.optJSONArray("pirates") ?: JSONArray()
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i)
                val wg = x.optJSONArray("win_gold")
                pirates.add(Pirate(x.getString("id"), x.optString("name"), x.optString("unlock", "start"), x.optJSONArray("requires").strings(),
                    x.optJSONArray("disabled_by").strings(), x.optBoolean("normal_pool", true), if (x.isNull("ports")) null else x.optJSONArray("ports").strings(),
                    x.optInt("selection_weight"), x.optDouble("power", 1.0).toFloat(), intArrayOf(wg?.optInt(0) ?: 10, wg?.optInt(1) ?: 20),
                    x.optDouble("lose_cargo", .4).toFloat(), colors(x.optJSONArray("colors"))))
            }
        }
        read("07_voyage_event_effects.json")?.let { v ->
            eventChance = v.optJSONObject("selection")?.optDouble("event_chance", .35)?.toFloat() ?: .35f
            val a = v.optJSONArray("voyage_event_effects") ?: JSONArray()
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i)
                val ef = x.optJSONObject("effect")
                val sr = x.optJSONArray("state_requirements")
                events.add(Event(x.getString("id"), x.optString("unlock", "start"), x.optJSONArray("requires").strings(),
                    if (x.isNull("ports")) null else x.optJSONArray("ports").strings(), sr != null && sr.length() > 0,
                    x.optInt("weight", 1), ef?.optInt("gold") ?: 0, ef?.optInt("wood") ?: 0, ef?.optInt("stone") ?: 0,
                    x.optBoolean("repeat", true), x.optInt("cooldown_completed_voyages", 2)))
            }
        }
        read("08_home_lines.json")?.let { h ->
            h.optJSONObject("selection")?.let { s -> talkProb = s.optDouble("show_probability_per_wallpaper_open", .3).toFloat(); talkInterval = s.optDouble("minimum_interval_seconds", 120.0).toFloat() }
            val a = h.optJSONArray("home_lines") ?: JSONArray()
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i)
                lines.add(HomeLine(x.optString("id"), x.optString("who"), x.optString("time"), x.optString("unlock", "start"), x.optString("text"),
                    if (x.isNull("worker_id")) null else x.optString("worker_id")))
            }
        }
    }

    fun ship(level: Int) = ships.firstOrNull { it.level == level } ?: Ship(1, "귀항호", 20, 10, 1f, .7f, null, emptyList(), "", 1, 0)
    fun port(id: String) = ports.firstOrNull { it.id == id } ?: ports.firstOrNull() ?: Port("port_haesol", "해솔항", 1, 1f, 1f, 2f, .3f, listOf("yeoul"), emptyList(), "")
    fun worker(id: String) = workers.firstOrNull { it.id == id }
    fun pirate(id: String) = pirates.firstOrNull { it.id == id }
    fun unlock(key: String) = unlocks.firstOrNull { it.key == key }
    fun capacity(level: Int) = warehouse.getOrNull(level)?.first ?: listOf(50, 150, 400, 900, 1800).getOrElse(level) { 50 }
}
