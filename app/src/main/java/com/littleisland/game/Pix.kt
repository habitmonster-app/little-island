package com.littleisland.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * 도트 화면 도구: 도트 한글 글꼴(갈무리, OFL), 10 x 10 아이콘, 나무 판넬, 칸, 막대.
 * 모든 좌표는 "도트" 단위. 작은 그림(가로 200)에 그린 뒤 화면에 그대로 키운다.
 */
object Pix {
    var big: Typeface = Typeface.DEFAULT_BOLD     // 갈무리11 굵게 (12도트)
    var small: Typeface = Typeface.DEFAULT        // 갈무리9 (10도트)
    private var loaded = false

    fun load(c: Context) {
        if (loaded) return
        try { big = Typeface.createFromAsset(c.assets, "galmuri11b.ttf"); small = Typeface.createFromAsset(c.assets, "galmuri9.ttf") } catch (e: Exception) { }
        loaded = true
    }

    private val tp = Paint().apply { isAntiAlias = false }
    /** 앱 화면은 읽기 쉬운 보통 글꼴(휴대폰 기본 한글 글꼴)로 쓴다. 홈 화면 장면은 도트 글꼴 그대로 */
    private val cleanTL = ThreadLocal.withInitial { false }
    fun clean(on: Boolean) = cleanTL.set(on)
    private val sans: Typeface by lazy { Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    private fun font(size: Int) {
        if (cleanTL.get()) { tp.typeface = sans; tp.isAntiAlias = true; tp.textSize = size * 1.05f }
        else { tp.typeface = if (size >= 12) big else small; tp.isAntiAlias = false; tp.textSize = size.toFloat() }
    }
    private val p = Paint().apply { isAntiAlias = false }

    fun col(h: String) = Color.parseColor(h)
    val INK = col("#2a1a10"); val CREAM = col("#f4ead2"); val GOLD = col("#ffd36a"); val DIM = col("#8a7a66"); val GREEN = col("#8fd06a"); val RED = col("#e0705a")

    @Synchronized fun rect(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) { p.color = color; c.drawRect(x, y, x + w, y + h, p) }

    /** 글자. size 12 = 큰 글꼴, 10 = 작은 글꼴. outline이면 어두운 테두리 */
    @Synchronized fun text(c: Canvas, s: String, x: Float, y: Float, size: Int, color: Int, align: Paint.Align = Paint.Align.LEFT, outline: Boolean = false) {
        font(size); tp.textAlign = align
        if (outline) {
            tp.color = INK
            val o = if (cleanTL.get()) .6f else 1f
            for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, 1 to 1)) c.drawText(s, x + dx * o, y + dy * o, tp)
        }
        tp.color = color; c.drawText(s, x, y, tp)
    }
    @Synchronized fun width(s: String, size: Int): Float { font(size); return tp.measureText(s) }

    /** 폭에 맞게 줄을 나눈다 (한글은 글자 단위로도 끊는다) */
    @Synchronized fun wrap(s: String, size: Int, maxW: Float): List<String> {
        val out = ArrayList<String>()
        for (para in s.split("\n")) {
            var line = ""
            for (word in para.split(" ")) {
                val t = if (line.isEmpty()) word else "$line $word"
                if (width(t, size) <= maxW) { line = t; continue }
                if (line.isNotEmpty()) out.add(line)
                line = ""
                var w = word
                while (width(w, size) > maxW) {
                    var k = w.length
                    while (k > 1 && width(w.substring(0, k), size) > maxW) k--
                    out.add(w.substring(0, k)); w = w.substring(k)
                }
                line = w
            }
            out.add(line)
        }
        return out
    }

    /** 글자가 폭을 넘으면 끝을 줄인다 */
    @Synchronized fun fit(s: String, size: Int, maxW: Float): String {
        if (width(s, size) <= maxW) return s
        var t = s
        while (t.length > 1 && width("$t…", size) > maxW) t = t.dropLast(1)
        return "$t…"
    }

    // ---------- 아이콘 (10 x 10) ----------
    private val pal = mapOf(
        'k' to "#2a1a10", 'w' to "#a8703f", 'W' to "#d9a86a", 'b' to "#6b4022", 's' to "#8b96a3", 'S' to "#c8d0d8", 'x' to "#5a6068",
        'g' to "#e0b030", 'G' to "#ffe58a", 'y' to "#a07010", 'r' to "#c0463a", 'R' to "#e87a5a", 'p' to "#e8d6a8", 'P' to "#c8b080",
        'e' to "#5fae4e", 'E' to "#3f8a3a", 'u' to "#4a8fd0", 'U' to "#9fd8f0", 'c' to "#f4ead2", 'n' to "#263a50", 'm' to "#e0b895",
        'o' to "#ff9a3a", 'l' to "#f0f0f0", 'v' to "#9a6ab0", 't' to "#3B8F91").mapValues { col(it.value) }

    val icons: Map<String, Array<String>> = mapOf(
        "wood" to arrayOf(
            "..........",
            ".kkkkkkkk.",
            "kwwwwwwkWk",
            "kbbbbbbkbk",
            ".kkkkkkkk.",
            ".kkkkkkkk.",
            "kwwwwwwkWk",
            "kbbbbbbkbk",
            ".kkkkkkkk.",
            ".........."),
        "stone" to arrayOf(
            "..........",
            "...kkkk...",
            "..kSSSsk..",
            ".kSSsssskk",
            "kSssssssxk",
            "kssssssxxk",
            "ksssssxxxk",
            ".kxxxxxxk.",
            "..kkkkkk..",
            ".........."),
        "gold" to arrayOf(
            "...kkkk...",
            ".kkGGGgkk.",
            ".kGGgggyk.",
            "kGGgyyggyk",
            "kGggyggyyk",
            "kGgggyggyk",
            "kGggyyggyk",
            ".kgggggyk.",
            ".kkyyyykk.",
            "...kkkk..."),
        "map" to arrayOf(
            "..........",
            "kkkkkkkkk.",
            "kppPpppPpk",
            "kpePpppPpk",
            "kpppPprPpk",
            "kppPprpPpk",
            "kpPpppPprk",
            "kppPpppPpk",
            "kkkkkkkkkk",
            ".........."),
        "house" to arrayOf(
            "....kk....",
            "...krrk...",
            "..krrRrk..",
            ".krrrrRrk.",
            "kkkkkkkkkk",
            ".kccccccck",
            ".kcUkkcUck",
            ".kccwkccck",
            ".kccwkccck",
            ".kkkkkkkkk"),
        "store" to arrayOf(
            "..........",
            ".kkkkkkkk.",
            "kbbbbbbbbk",
            "kwWwWwWwWk",
            "kwWkkkkwWk",
            "kwWkWWkwWk",
            "kwWkkkkwWk",
            "kwWkWWkwWk",
            "kkkkkkkkkk",
            ".........."),
        "dock" to arrayOf(
            "..........",
            "........k.",
            "........o.",
            "........k.",
            "kkkkkkkkkk",
            "WWWWWWWWWk",
            "kbkkkbkkbk",
            ".b...b..b.",
            "UbUUUbUUbU",
            "uuuuuuuuuu"),
        "field" to arrayOf(
            "..........",
            "...e..e...",
            "..eEeeEe..",
            "...E..E...",
            "kkkkkkkkkk",
            "kbwbwbwbwk",
            "kwbwbwbwbk",
            "kbwbwbwbwk",
            "kkkkkkkkkk",
            ".........."),
        "lighthouse" to arrayOf(
            "...kGGk...",
            "...kooк...".replace('к', 'k'),
            "..kkkkkk..",
            "...klrk...",
            "...kllk...",
            "...krrk...",
            "..kllllk..",
            "..krrrrk..",
            ".kllllllk.",
            "kkkkkkkkkk"),
        "market" to arrayOf(
            "..........",
            "kkkkkkkkkk",
            "krcrcrcrck",
            "kkkkkkkkkk",
            ".kw....wk.",
            ".kw.gG.wk.",
            "kkkkkkkkkk",
            "kWWWWWWWWk",
            "kbbbbbbbbk",
            "kkkkkkkkkk"),
        "ship" to arrayOf(
            "....kr....",
            "....kcc...",
            "...kccck..",
            "...kcccck.",
            "....k.....",
            "..kcccck..",
            "....k.....",
            "kkkkkkkkkk",
            ".kwwwwwwkt",
            "..kkkkkk.."),
        "anchor" to arrayOf(
            "....kk....",
            "...kUUk...",
            "....kk....",
            "..kkUUkk..",
            "....UU....",
            "....UU....",
            "k...UU...k",
            "kU..UU..Uk",
            ".kUUUUUUk.",
            "..kkkkkk.."),
        "person" to arrayOf(
            "...kkkk...",
            "..kGGGGk..",
            ".kGGGGGGk.",
            "..kmmmmk..",
            "..kmkmmk..",
            "...kmmk...",
            "..keeeek..",
            ".kmeeeemk.",
            "..keeeek..",
            "..kb..bk.."),
        "puzzle" to arrayOf(
            "...kk.....",
            "..kGGk....",
            "kkkGGkkkk.",
            "kgggggggk.",
            "kgggggggkk",
            "kgggggggGG",
            "kgggggggkk",
            "kgggggggk.",
            "kkkkkkkkk.",
            ".........."),
        "book" to arrayOf(
            "..........",
            ".kkkkkkkk.",
            "kcccckccck",
            "kcxxckcxxk",
            "kcccckccck",
            "kcxxckcxxk",
            "kcccckccck",
            "kkkkkkkkkk",
            ".rk.......",
            ".........."),
        "gear" to arrayOf(
            "....kk....",
            ".kk.ss.kk.",
            ".kssssssk.",
            "..sskkss..",
            "kssk..kssk",
            "kssk..kssk",
            "..sskkss..",
            ".kssssssk.",
            ".kk.ss.kk.",
            "....kk...."),
        "lock" to arrayOf(
            "..........",
            "...kkkk...",
            "..kx..xk..",
            "..kx..xk..",
            ".kkkkkkkk.",
            ".kggggggk.",
            ".kgggkggk.",
            ".kgggkggk.",
            ".kggggggk.",
            ".kkkkkkkk."),
        "check" to arrayOf(
            "..........",
            "........kk",
            ".......keк".replace('к', 'k'),
            "......kek.",
            "kk...kek..",
            "kek.kek...",
            ".kekek....",
            "..kek.....",
            "...k......",
            ".........."),
        "chest" to arrayOf(
            "..........",
            ".kkkkkkkk.",
            "kwwwwwwwwk",
            "kwWWWWWWwk",
            "kkkkggkkkk",
            "kwwwgGwwwk",
            "kwwwkkwwwk",
            "kwwwwwwwwk",
            "kkkkkkkkkk",
            ".........."),
        "pirate" to arrayOf(
            "....k.....",
            "...kkkk...",
            "..knSnnk..",
            "..knnSnk..",
            "...kSSk...",
            "..knnnnk..",
            "....k.....",
            "kkkkkkkkkk",
            ".kbbbbbbk.",
            "..kkkkkk.."),
        "auto" to arrayOf(
            "...kkkk...",
            ".kkUUUUkk.",
            ".kU....Uk.",
            "kU..kk..Uk",
            "kU.kUUk.Uk",
            "kU..kk..Uk",
            ".kU....kUk",
            ".kkUUUUkUk",
            "...kkkk.kk",
            ".........."),
    )

    /** 아이콘을 s배로 그린다 */
    private val iconOrder = listOf("wood", "stone", "gold", "map", "house", "store", "dock", "field", "lighthouse", "market", "ship", "anchor", "person", "puzzle", "book", "gear")
    private val ip = Paint().apply { isFilterBitmap = false }
    private val iSrc = android.graphics.Rect(); private val iDst = android.graphics.RectF()
    /** 앱 화면(가로 400 이상)은 두 배 그림을 쓴다 */
    private fun hi(c: Canvas) = maxOf(1, c.width / 200)
    private fun blit(c: Canvas, b: android.graphics.Bitmap, x: Float, y: Float, w: Float, alpha: Int) {
        ip.alpha = alpha
        iSrc.set(0, 0, b.width, b.height); iDst.set(x, y, x + w, y + w * b.height / b.width)
        c.drawBitmap(b, iSrc, iDst, ip)
    }

    @Synchronized fun icon(c: Canvas, name: String, x: Float, y: Float, s: Float, alpha: Int = 255) {
        // 그림 시트의 한 칸: "part:0" 부품, "skill:3" 스킬, "face:2" 알바 얼굴
        val ci = name.indexOf(':')
        if (ci > 0) {
            val idx = name.substring(ci + 1).toIntOrNull() ?: 0
            val px = (10 * s * hi(c)).toInt()
            val bm = when (name.substring(0, ci)) {
                "part" -> Art.cell("part_icons", 4, 2, idx, px)
                "skill" -> Art.cell("skill_icons", 4, 4, idx, px)
                "face" -> Art.cell("worker_portraits", 4, 3, idx, px)
                "face15" -> Art.cell("crew_portraits_15", 4, 3, idx, px)
                "fx" -> Art.cell("upgrade_fx", 4, 1, idx, px)
                "daily" -> Art.cell("daily_icons", 4, 3, idx, px)
                "deco" -> Art.cell("ship_deco", 4, 4, idx, px)
                "deco2" -> Art.cell("ship_deco_2", 4, 4, idx, px)
                "souv" -> Art.cell("souvenir_icons", 6, 6, idx, px)
                "charm" -> if (idx >= 100) Art.cell("charm_icons_2", 4, 3, idx - 100, px) else Art.cell("charm_icons", 6, 4, idx, px)
                "deco3" -> Art.cell("ship_deco_3", 4, 4, idx, px)
                "title" -> Art.cell("title_badges", 4, 6, idx, px)
                "weather" -> Art.cell("weather_sky", 4, 4, 4 + idx, px)
                "visitor" -> Art.cell("visitor_faces", 4, 2, idx, px)
                "season" -> Art.cell("season_deco", 4, 4, idx, px)
                "crit" -> if (idx >= 100) Art.cell("seabed_book", 4, 2, idx - 100, px) else Art.cell("creature_book", 4, 2, idx, px)
                "tide" -> Art.cell("tide_icons", 4, 3, idx, px)
                "port" -> Art.cell("port_icons", 4, 3, idx, px)
                "chest" -> Art.cell("gacha_chest", 4, 1, idx, px)
                "boss" -> if (idx >= 100) Art.cell("boss_captains_2", 4, 1, idx - 100, px) else Art.cell("boss_captains", 4, 2, idx, px)
                "bossship" -> if (idx >= 100) Art.cell("pirate_bosses_2", 4, 1, idx - 100, px) else Art.cell("pirate_bosses", 4, 2, idx, px)
                else -> null
            }
            if (bm != null) { blit(c, bm, x, y, 10 * s, alpha); return }
            return icon(c, when (name.substring(0, ci)) { "face", "boss" -> "person"; "part", "deco", "deco2", "deco3", "bossship" -> "ship"; else -> "book" }, x, y, s, alpha)
        }
        val k = iconOrder.indexOf(name)
        if (k >= 0) Art.cell("icons", 4, 4, k, (10 * s * hi(c)).toInt())?.let { blit(c, it, x, y, 10 * s, alpha); return }
        val rows = icons[name] ?: return
        for ((r, line) in rows.withIndex()) for ((k, ch) in line.withIndex()) {
            if (k >= 10) break
            val cl = pal[ch] ?: continue
            p.color = cl; p.alpha = alpha
            c.drawRect(x + k * s, y + r * s, x + (k + 1) * s, y + (r + 1) * s, p)
        }
        p.alpha = 255
    }

    // ---------- 판넬과 칸 ----------
    /** 돌 테두리 + 나무판 판넬 */
    @Synchronized fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        rect(c, x, y, w, h, col("#5a5a60"))
        for (i in 0 until (w / 6).toInt()) { rect(c, x + i * 6 + 1, y, 4f, 1f, col("#8a8a92")); rect(c, x + i * 6 + 3, y + h - 1, 3f, 1f, col("#3a3a40")) }
        rect(c, x + 3, y + 3, w - 6, h - 6, col("#3a2414"))
        rect(c, x + 4, y + 4, w - 8, h - 8, col("#5d3a1f"))
        var yy = y + 4f
        while (yy < y + h - 6) { rect(c, x + 4, yy, w - 8, 1f, col("#4a2c16")); yy += 9 }
        rect(c, x + 4, y + 4, w - 8, 5f, col("#7a5030")); rect(c, x + 4, y + 4, w - 8, 1f, col("#a87444"))
    }

    /** 물건 칸 (어두운 홈). glow면 금색 테두리 */
    @Synchronized fun slot(c: Canvas, x: Float, y: Float, w: Float, h: Float, glow: Boolean = false, sel: Boolean = false) {
        rect(c, x, y, w, h, if (glow) GOLD else if (sel) col("#c8a050") else col("#1a1008"))
        rect(c, x + 1, y + 1, w - 2, h - 2, col("#2e2014"))
        rect(c, x + 1, y + 1, w - 2, 1f, col("#1a1008")); rect(c, x + 1, y + h - 2, w - 2, 1f, col("#4a3420"))
        if (glow) { rect(c, x - 1, y + 2, 1f, h - 4, GOLD); rect(c, x + w, y + 2, 1f, h - 4, GOLD) }
    }

    /** 나무 단추 */
    @Synchronized fun button(c: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, on: Boolean) {
        rect(c, x, y, w, h, INK)
        rect(c, x + 1, y + 1, w - 2, h - 2, if (on) col("#c8903a") else col("#5a4a3a"))
        rect(c, x + 1, y + 1, w - 2, 1f, if (on) col("#f0c060") else col("#6a5a4a"))
        rect(c, x + 1, y + h - 2, w - 2, 1f, if (on) col("#8a5a20") else col("#3a2a1a"))
        text(c, label, x + w / 2, y + h / 2 + 4, 10, if (on) INK else DIM, Paint.Align.CENTER)
    }

    @Synchronized fun bar(c: Canvas, x: Float, y: Float, w: Float, h: Float, f: Float, fill: Int = col("#e0b45a")) {
        rect(c, x, y, w, h, col("#c8a050")); rect(c, x + 1, y + 1, w - 2, h - 2, col("#1a1410"))
        rect(c, x + 1, y + 1, (w - 2) * f.coerceIn(0f, 1f), h - 2, fill)
    }
}
