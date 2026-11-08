package com.littleisland.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 이야기 「돌아오는 바다」. assets/story.json을 읽고, 게임 사건(깃발)에 맞춰 화를 연다.
 * 이야기를 안 읽어도 게임은 그대로 돌아간다. 읽음 여부는 진행 조건이 아니다.
 */
object Story {
    class Entry(val id: String, val kind: Int, val label: String, val title: String, val unlock: String,
                val after: String?, val summary: String, val body: String, val news: String, val art: Int = -1)
    class Line(val who: String, val time: String, val text: String, val unlock: String)

    const val MAIN = 0; const val SIDE = 1; const val VOYAGE = 2; const val CREW = 3

    val entries = ArrayList<Entry>()
    val lines = ArrayList<Line>()
    private var loaded = false

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        try {
            val txt = c.assets.open("story.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val d = JSONObject(txt)
            fun arr(k: String): JSONArray = d.optJSONArray(k) ?: JSONArray()
            val eps = arr("episodes")
            for (i in 0 until eps.length()) {
                val e = eps.getJSONObject(i)
                entries.add(Entry(e.getString("id"), MAIN, e.optInt("part").toString() + "부 " + "%02d".format(e.optInt("no")) + "화",
                    e.optString("title"), e.optString("unlock"), if (e.isNull("after")) null else e.optString("after"),
                    e.optString("summary"), e.optString("body"), e.optString("home_news")))
            }
            val side = arr("side_stories")
            for (i in 0 until side.length()) {
                val e = side.getJSONObject(i)
                entries.add(Entry(e.getString("id"), SIDE, "외전", e.optString("title"), e.optString("unlock"), null,
                    e.optString("summary"), e.optString("body"), e.optString("home_news")))
            }
            val voy = arr("voyage_events")
            for (i in 0 until voy.length()) {
                val e = voy.getJSONObject(i)
                entries.add(Entry(e.getString("id"), VOYAGE, "항해 기록", e.optString("title"), e.optString("unlock"), null,
                    "", e.optString("body"), e.optString("home_news")))
            }
            val hl = arr("home_lines")
            for (i in 0 until hl.length()) {
                val e = hl.getJSONObject(i)
                lines.add(Line(e.optString("who"), e.optString("time"), e.optString("text"), e.optString("unlock", "start")))
            }
        } catch (e: Exception) { /* 이야기 파일이 없어도 게임은 돈다 */ }
        // 선원 이야기 (21_crew_stories.json): 승진·Lv5·Lv10. 조건은 "crew_<id>_promote" 같은 이름 그대로
        try {
            val d = JSONObject(c.assets.open("21_crew_stories.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            val cs = d.getJSONArray("crew")
            val names = try {
                val w = JSONObject(c.assets.open("20_crew.json").bufferedReader(Charsets.UTF_8).use { it.readText() }).getJSONArray("crew")
                (0 until w.length()).associate { w.getJSONObject(it).getString("worker_id") to w.getJSONObject(it).optString("name") }
            } catch (e: Exception) { emptyMap() }
            for (i in 0 until cs.length()) {
                val x = cs.getJSONObject(i); val id = x.getString("worker_id")
                for ((k, tag) in listOf("promote_story" to "승진", "lv5_story" to "Lv5", "lv10_story" to "Lv10")) {
                    val s = x.optJSONObject(k) ?: continue
                    val sid = s.getString("id")
                    val sum = s.optString("summary")
                    entries.add(Entry(sid, CREW, "선원 " + (names[id] ?: id) + " · " + tag, s.optString("title"), sid, null,
                        sum, s.optString("body"), sum.substringBefore("\n"), names.keys.indexOf(id)))
                }
            }
            // 해적 우두머리 첫 승리 이야기 (25_pirate_bosses.json)
            try {
                val bs = JSONObject(c.assets.open("25_pirate_bosses.json").bufferedReader(Charsets.UTF_8).use { it.readText() }).getJSONArray("bosses")
                for (i in 0 until bs.length()) {
                    val b = bs.getJSONObject(i); val st = b.optJSONObject("story") ?: continue
                    val id = st.optString("id", "story_" + b.getString("id"))
                    entries.add(Entry(id, SIDE, "우두머리 · " + b.optString("name"), st.optString("title"), id, null,
                        b.optString("captain") + " 선장의 배를 처음 이긴 날", st.optString("body"), "우두머리 이야기가 열렸어요"))
                }
            } catch (e: Exception) { }
            // 특별 항해 이야기 (23_crew_missions.json): 첫 성공 때 열림
            for ((file, artBase) in listOf("23_crew_missions.json" to 0, "32_crew_missions_3.json" to 100)) try {
                val ms = JSONObject(c.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }).getJSONArray("missions")
                for (i in 0 until ms.length()) {
                    val m = ms.getJSONObject(i); val st = m.optJSONObject("story") ?: continue
                    val id = m.getString("id"); val who = m.optString("crew")
                    entries.add(Entry(id, CREW, "특별 항해 · " + (names[who] ?: who), st.optString("title"), id, null,
                        m.optString("summary"), st.optString("body"), m.optString("result_card"), if (artBase > 0) artBase + names.keys.indexOf(who) else st.optJSONObject("art")?.optInt("index", -1) ?: names.keys.indexOf(who)))
                }
            } catch (e: Exception) { }
        } catch (e: Exception) { }
    }

    /** 숫자 자리표시자를 실제 게임 값으로 */
    fun fill(s: String): String = s
        .replace("{load_wood}", Game.LOAD_W.toString()).replace("{load_stone}", Game.LOAD_S.toString())
        .replace("{wood_price}", "1").replace("{stone_price}", "2")
        .replace("{work_hours}", (Game.WORK_MS / 3600000L).toString()).replace("{wage}", Game.WAGE.toString())

    fun byId(id: String) = entries.firstOrNull { it.id == id }

    /**
     * 깃발을 보고 열 수 있는 본편·외전을 연다. 새로 열린 것들을 돌려준다.
     * 본편이 열리면 "main_NN" 깃발이 생겨 다음 화나 외전 조건이 된다.
     */
    fun refresh(later: Boolean): List<Entry> {
        val opened = ArrayList<Entry>()
        var laterUsed = false
        var changed = true
        while (changed) {
            changed = false
            for (e in entries) {
                if (e.kind == VOYAGE || Game.isOpen(e.id)) continue
                if (!Game.has(e.unlock)) continue
                if (e.after != null && !Game.isOpen(e.after)) continue
                // 11화부터는 귀항 때만, 한 번에 한 화씩
                val no = if (e.kind == MAIN) e.id.removePrefix("main_").toIntOrNull() ?: 0 else 0
                if (no > 10) { if (!later || laterUsed) continue; laterUsed = true }
                Game.open(e.id)
                if (e.kind == MAIN) Game.flag(e.id)
                opened.add(e); changed = true
            }
        }
        return opened
    }

    /** 귀항 때 열린 항해 사건 중 하나를 고른다 (아직 안 본 것 우선) */
    fun pickVoyageEvent(): Entry? {
        val ok = entries.filter { it.kind == VOYAGE && Game.has(it.unlock) }
        if (ok.isEmpty()) return null
        val fresh = ok.filter { !Game.isOpen(it.id) }
        return (if (fresh.isNotEmpty()) fresh else ok).random()
    }

    private val recent = ArrayDeque<String>()

    /** 홈 화면 말풍선 한 줄 (08_home_lines 192줄, 없으면 이야기 파일의 18줄) */
    fun pickLine(who: String, time: String): String? {
        val src = if (Data.lines.isNotEmpty()) Data.lines.map { Line(it.who, it.time, it.text, it.unlock) to it.id } else lines.map { it to it.text }
        val ok = src.filter { it.first.who == who && it.first.time == time && Game.has(it.first.unlock) }
        if (ok.isEmpty()) return null
        val fresh = ok.filter { it.second !in recent }
        val pick = (if (fresh.isNotEmpty()) fresh else ok).random()
        recent.addLast(pick.second); while (recent.size > 8) recent.removeFirst()
        return fill(pick.first.text)
    }
}
