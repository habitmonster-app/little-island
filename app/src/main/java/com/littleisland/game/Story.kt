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
                val after: String?, val summary: String, val body: String, val news: String)
    class Line(val who: String, val time: String, val text: String, val unlock: String)

    const val MAIN = 0; const val SIDE = 1; const val VOYAGE = 2

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
    fun refresh(): List<Entry> {
        val opened = ArrayList<Entry>()
        var changed = true
        while (changed) {
            changed = false
            for (e in entries) {
                if (e.kind == VOYAGE || Game.isOpen(e.id)) continue
                if (!Game.has(e.unlock)) continue
                if (e.after != null && !Game.isOpen(e.after)) continue
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

    /** 홈 화면 말풍선 한 줄 */
    fun pickLine(who: String, time: String): String? {
        val ok = lines.filter { it.who == who && it.time == time && Game.has(it.unlock) }
        return if (ok.isEmpty()) null else fill(ok.random().text)
    }
}
