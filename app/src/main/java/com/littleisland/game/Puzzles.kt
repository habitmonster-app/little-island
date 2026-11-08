package com.littleisland.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

/**
 * 지도 조각 밀어 맞추기 (3 x 3, 빈칸 하나).
 * 맞춘 상태에서 정해진 수만큼 섞어서 늘 풀 수 있다.
 */
class MapPuzzleView(ctx: Context, val onSolved: () -> Unit) : View(ctx) {
    private val n = 3
    private val tiles = IntArray(n * n) { it }        // 칸 → 조각 번호, 마지막 번호가 빈칸
    private val blank = n * n - 1
    private val art: Bitmap = Art.get("puzzle_map", 240) ?: drawMap()
    private val paint = Paint().apply { isFilterBitmap = false }
    private val line = Paint().apply { color = Color.parseColor("#4a2c16"); style = Paint.Style.STROKE }
    private val src = Rect(); private val dst = Rect()
    var moves = 0; private set
    private var solved = false

    init {
        var b = tiles.indexOf(blank)
        var last = -1
        repeat(60) {
            val nb = neighbors(b).filter { it != last }
            val k = nb[Random.nextInt(nb.size)]
            tiles[b] = tiles[k]; tiles[k] = blank; last = b; b = k
        }
        if (isSolved()) { tiles[b] = tiles[neighbors(b)[0]]; tiles[neighbors(b)[0]] = blank }
    }

    private fun neighbors(i: Int): List<Int> {
        val r = i / n; val c = i % n
        return listOfNotNull(if (r > 0) i - n else null, if (r < n - 1) i + n else null, if (c > 0) i - 1 else null, if (c < n - 1) i + 1 else null)
    }
    private fun isSolved() = tiles.withIndex().all { it.index == it.value }

    override fun onMeasure(w: Int, h: Int) { val s = MeasureSpec.getSize(w); setMeasuredDimension(s, s) }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN || solved) return true
        val cs = width / n
        val i = (e.y.toInt() / cs).coerceIn(0, n - 1) * n + (e.x.toInt() / cs).coerceIn(0, n - 1)
        val b = tiles.indexOf(blank)
        if (i in neighbors(b)) {
            tiles[b] = tiles[i]; tiles[i] = blank; moves++
            if (isSolved()) { solved = true; invalidate(); onSolved(); return true }
            invalidate()
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        val cs = width / n; val ts = art.width / n
        cv.drawColor(Color.parseColor("#2a1a10"))
        for (i in tiles.indices) {
            val t = tiles[i]
            if (t == blank && !solved) continue
            src.set((t % n) * ts, (t / n) * ts, (t % n + 1) * ts, (t / n + 1) * ts)
            dst.set((i % n) * cs, (i / n) * cs, (i % n + 1) * cs, (i / n + 1) * cs)
            cv.drawBitmap(art, src, dst, paint)
            if (!solved) { line.strokeWidth = width / 150f; cv.drawRect(dst, line) }
        }
    }

    /** 보물 지도 (도트 60 x 60): 양피지, 섬 세 개, 점선 항로, X 표시 */
    private fun drawMap(): Bitmap {
        val b = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); val p = Paint()
        fun r(x: Int, y: Int, w: Int, h: Int, col: String) { p.color = Color.parseColor(col); c.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), p) }
        fun blob(cx: Int, cy: Int, rx: Int, ry: Int, col: String) {
            for (y in -ry..ry) { val w = (rx * Math.sqrt(1.0 - (y * y).toDouble() / (ry * ry))).toInt(); r(cx - w, cy + y, w * 2 + 1, 1, col) }
        }
        r(0, 0, 60, 60, "#e8d6a8")
        for (i in 0 until 40) r((i * 37) % 60, (i * 23) % 60, 1, 1, "#d4bd86")
        r(0, 0, 60, 2, "#b08a50"); r(0, 58, 60, 2, "#b08a50"); r(0, 0, 2, 60, "#b08a50"); r(58, 0, 2, 60, "#b08a50")
        // 바다 물결
        for (y in 8 until 56 step 7) for (x in (y % 14) until 56 step 14) r(x + 3, y, 4, 1, "#9ab8b8")
        blob(14, 14, 8, 5, "#9ab06a"); blob(14, 14, 5, 3, "#7a9a4a"); r(12, 12, 2, 2, "#c0463a")     // 작은 섬 (출발)
        blob(44, 18, 6, 4, "#9ab06a"); r(43, 16, 3, 2, "#7a6a5a")                                    // 항구
        blob(42, 44, 10, 7, "#b0a060"); blob(42, 44, 7, 4, "#8a9a4a"); r(38, 40, 2, 4, "#5a3a1e"); r(36, 39, 6, 2, "#4f8a3a")   // 보물섬
        // 점선 항로
        val path = listOf(14 to 20, 16 to 24, 19 to 27, 22 to 30, 25 to 32, 28 to 35, 31 to 38, 34 to 41, 37 to 43)
        for ((x, y) in path) r(x, y, 2, 1, "#8a4a2a")
        // X
        for (k in 0 until 5) { r(44 + k, 42 + k, 1, 1, "#c0302a"); r(48 - k, 42 + k, 1, 1, "#c0302a") }
        // 나침반
        r(50, 50, 1, 5, "#4a2c16"); r(48, 52, 5, 1, "#4a2c16"); r(50, 49, 1, 1, "#c0302a")
        return b
    }
}

/**
 * 짐 싣기 퍼즐: 짐칸(5 x 3)을 상자 조각으로 빈틈없이 채운다.
 * 조각은 짐칸을 무작위로 나눠 만들어서 늘 풀 수 있다.
 * 아래 조각을 눌러 고르고, "돌리기"로 돌리고, 짐칸을 누르면 그 자리에 놓인다. 놓인 조각을 누르면 빠진다.
 */
class CargoPuzzleView(ctx: Context, val onSolved: () -> Unit) : View(ctx) {
    private val rows = 3; private val cols = 5
    private var pieces = ArrayList<List<Pair<Int, Int>>>()
    private val at = HashMap<Int, Pair<Int, Int>>()          // 놓인 조각 → 기준 칸
    private val grid = IntArray(rows * cols) { -1 }
    var sel = -1; private set
    private var solved = false
    private val trayRects = HashMap<Int, android.graphics.RectF>()
    private val colors = listOf("#b07a44", "#8a9a5a", "#c0704a", "#6a8ab0", "#c8a050", "#9a6ab0", "#5a9a8a", "#b05a5a").map { Color.parseColor(it) }
    private val p = Paint().apply { isAntiAlias = true }
    private val edge = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = Color.parseColor("#3a2412") }

    init {
        val owner = IntArray(rows * cols) { -1 }
        var id = 0
        for (i in 0 until rows * cols) {
            if (owner[i] >= 0) continue
            val size = if (Random.nextFloat() < .15f) 2 else 3 + Random.nextInt(2)
            val cells = arrayListOf(i); owner[i] = id
            while (cells.size < size) {
                val opts = cells.flatMap { c -> listOf(c - cols, c + cols, if (c % cols > 0) c - 1 else -1, if (c % cols < cols - 1) c + 1 else -1) }
                    .filter { it in 0 until rows * cols && owner[it] < 0 }.distinct()
                if (opts.isEmpty()) break
                val nx = opts[Random.nextInt(opts.size)]; owner[nx] = id; cells.add(nx)
            }
            var shape = norm(cells.map { it / cols to it % cols })
            repeat(Random.nextInt(4)) { shape = rot(shape) }      // 처음엔 아무 방향으로
            pieces.add(shape); id++
        }
        pieces.shuffle()
    }

    private fun norm(l: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val mr = l.minOf { it.first }; val mc = l.minOf { it.second }
        return l.map { (it.first - mr) to (it.second - mc) }.sortedWith(compareBy({ it.first }, { it.second }))
    }
    private fun rot(l: List<Pair<Int, Int>>) = norm(l.map { it.second to -it.first })

    fun rotateSelected() { if (sel >= 0 && sel !in at) { pieces[sel] = rot(pieces[sel]); invalidate() } }

    override fun onMeasure(w: Int, h: Int) { val s = MeasureSpec.getSize(w); setMeasuredDimension(s, (s * 1.25f).toInt()) }

    private fun cell() = width / (cols + 1f)
    private fun gx() = cell() / 2f
    private fun gy() = cell() / 4f

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN || solved) return true
        val cs = cell()
        val c = ((e.x - gx()) / cs).toInt(); val r = ((e.y - gy()) / cs).toInt()
        if (e.x >= gx() && e.y >= gy() && c in 0 until cols && r in 0 until rows) {
            val o = grid[r * cols + c]
            if (o >= 0) { remove(o); sel = o }
            else if (sel >= 0 && sel !in at) {
                // 누른 칸에 조각의 첫 칸이 오게, 안 맞으면 다른 칸 기준으로도 맞춰 본다
                val sh = pieces[sel]
                for (anchor in sh) {
                    val r0 = r - anchor.first; val c0 = c - anchor.second
                    if (fits(sh, r0, c0)) { place(sel, r0, c0); sel = -1; break }
                }
                if (grid.all { it >= 0 }) { solved = true; invalidate(); onSolved(); return true }
            }
            invalidate(); return true
        }
        for ((i, rc) in trayRects) if (rc.contains(e.x, e.y)) { sel = if (sel == i) -1 else i; invalidate(); return true }
        return true
    }

    private fun fits(sh: List<Pair<Int, Int>>, r0: Int, c0: Int) = sh.all { (r, c) ->
        val rr = r0 + r; val cc = c0 + c; rr in 0 until rows && cc in 0 until cols && grid[rr * cols + cc] < 0 }
    private fun place(i: Int, r0: Int, c0: Int) { at[i] = r0 to c0; for ((r, c) in pieces[i]) grid[(r0 + r) * cols + c0 + c] = i }
    private fun remove(i: Int) { at.remove(i); for (k in grid.indices) if (grid[k] == i) grid[k] = -1 }

    private val crSrc = Rect(); private val crDst = Rect()
    private fun crate(cv: Canvas, x: Float, y: Float, s: Float, color: Int, hi: Boolean) {
        val bm = Art.cell("crates", 4, 2, colors.indexOf(color).coerceAtLeast(0), 64)
        if (bm != null) {
            crSrc.set(0, 0, bm.width, bm.height); crDst.set((x + s * .02f).toInt(), (y + s * .02f).toInt(), (x + s * .98f).toInt(), (y + s * .98f).toInt())
            cv.drawBitmap(bm, crSrc, crDst, null)
            if (hi) { edge.strokeWidth = s * .08f; edge.color = Color.parseColor("#ffd36a"); cv.drawRect(x + s * .04f, y + s * .04f, x + s * .96f, y + s * .96f, edge) }
            return
        }
        p.color = color; cv.drawRect(x + s * .04f, y + s * .04f, x + s * .96f, y + s * .96f, p)
        p.color = Color.argb(70, 255, 255, 255); cv.drawRect(x + s * .04f, y + s * .04f, x + s * .96f, y + s * .2f, p)
        edge.strokeWidth = if (hi) s * .1f else s * .05f; edge.color = if (hi) Color.parseColor("#ffd36a") else Color.parseColor("#3a2412")
        cv.drawRect(x + s * .04f, y + s * .04f, x + s * .96f, y + s * .96f, edge)
    }

    override fun onDraw(cv: Canvas) {
        val cs = cell(); val x0 = gx(); val y0 = gy()
        // 짐칸 바닥
        p.color = Color.parseColor("#5a3a1e"); cv.drawRect(x0 - cs * .1f, y0 - cs * .1f, x0 + cols * cs + cs * .1f, y0 + rows * cs + cs * .1f, p)
        p.color = Color.parseColor("#7a5030")
        for (r in 0 until rows) for (c in 0 until cols) cv.drawRect(x0 + c * cs + 2, y0 + r * cs + 2, x0 + (c + 1) * cs - 2, y0 + (r + 1) * cs - 2, p)
        for ((i, rc) in at) for ((r, c) in pieces[i]) crate(cv, x0 + (rc.second + c) * cs, y0 + (rc.first + r) * cs, cs, colors[i % colors.size], false)
        // 아래 조각 놓는 곳
        trayRects.clear()
        val ts = width / 17f
        var tx = ts * .5f; var ty = y0 + rows * cs + cs * .5f
        for (i in pieces.indices) {
            if (i in at) continue
            val sh = pieces[i]
            val w = (sh.maxOf { it.second } + 1) * ts; val h = (sh.maxOf { it.first } + 1) * ts
            if (tx + w > width) { tx = ts * .5f; ty += ts * 4.6f }
            for ((r, c) in sh) crate(cv, tx + c * ts, ty + r * ts, ts, colors[i % colors.size], i == sel)
            trayRects[i] = android.graphics.RectF(tx - ts * .3f, ty - ts * .3f, tx + w + ts * .3f, ty + h + ts * .3f)
            tx += w + ts
        }
    }
}
