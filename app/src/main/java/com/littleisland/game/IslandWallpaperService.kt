package com.littleisland.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Paint
import android.graphics.Rect
import android.app.WallpaperManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.min

/** 홈 화면 배경화면으로 섬을 그린다. 화면이 꺼져 있거나 앱에 가려지면 멈춰서 배터리를 아낀다. */
class IslandWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = IslandEngine()

    inner class IslandEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val scene = Scene()
        private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
        private val src = Rect()
        private val dst = Rect()
        private var visible = false
        private var lastMs = 0L
        private var xOffset = 0f
        private var paging = false   // 홈 화면이 페이지 넘김을 실제로 알려주는지
        private var firstOff = -1f
        private var tapPage = 0       // 넘김을 안 알려주는 홈 화면(삼성 등): 빈 곳을 톡 치면 페이지 이동
        private var cam = -1f
        private var lastPhase = -1
        private val frame = Runnable { drawFrame() }
        private val labelCv by lazy { Canvas(scene.bitmap).apply { scale(Scene.SS.toFloat(), Scene.SS.toFloat()) } }
        private var card: Triple<String, String, String>? = null   // 결과 카드 (제목, 내용, 작은 글씨)
        private var cardT = 0f
        private val cardBg = Paint().apply { isAntiAlias = true; color = Color.parseColor("#f3e6c4") }
        private val cardEdge = Paint().apply { isAntiAlias = true; color = Color.parseColor("#8a5a32"); style = Paint.Style.STROKE }
        private val cardTitle = Paint().apply { isAntiAlias = true; color = Color.parseColor("#4a2c16"); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        private val cardText = Paint().apply { isAntiAlias = true; color = Color.parseColor("#6b4a2a"); textAlign = Paint.Align.CENTER }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(true)
            Game.init(this@IslandWallpaperService); Pix.load(this@IslandWallpaperService)
            for (pt in listOf(cardTitle, cardText, talkTx, talkName, popTx)) pt.typeface = Pix.big
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) {
                Game.catchUp()
                Game.report?.let { r ->
                    val t = if (r.minutes >= 60) (r.minutes / 60).toString() + "시간" + (if (r.minutes % 60 > 0) " " + (r.minutes % 60) + "분" else "") else (r.minutes.coerceAtLeast(1)).toString() + "분"
                    card = Triple(t + " 동안 섬에서", "나무 +" + r.wood + "  돌 +" + r.stone + "  금화 +" + r.gold,
                        (if (r.voyages > 0) "항해 " + r.voyages + "번 다녀왔어요" else "") + (if (r.full) (if (r.voyages > 0) " · " else "") + "창고가 가득 찼어요" else ""))
                    cardT = 7f; Game.report = null
                }
                lastMs = SystemClock.uptimeMillis(); drawFrame()
            } else { handler.removeCallbacks(frame); Game.save() }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(frame)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(frame)
        }

        override fun onOffsetsChanged(xOff: Float, yOff: Float, xStep: Float, yStep: Float, xPx: Int, yPx: Int) {
            // 값이 실제로 바뀌는 걸 봐야 페이지 넘김 지원으로 판단 (삼성 One UI는 고정값만 줌)
            if (firstOff < 0f) firstOff = xOff
            if (xStep > 0f && xStep < 1f && kotlin.math.abs(xOff - firstOff) > 0.01f) paging = true
            xOffset = xOff
            Diag.save(this@IslandWallpaperService, xOff, xStep, paging)
            if (visible) drawFrame()
        }

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) {
                // 떠다니는 유리병·보물상자·갈매기를 톡 치면 줍는다
                if (scene.tap(x / scale + ox, y / scale + oy) == null) cardT = 0f
                if (visible) drawFrame()
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        private fun drawFrame() {
            handler.removeCallbacks(frame)
            val now = SystemClock.uptimeMillis()
            val dt = min(0.1f, (now - lastMs) / 1000f).coerceAtLeast(0f)
            lastMs = now
            scene.update(dt)
            if (cardT > 0f) cardT -= dt
            if (cardT <= 0f && Game.cards.isNotEmpty()) {   // 항해 결과 카드를 하나씩
                val (t, m) = Game.cards.removeFirst()
                val sp = m.split("\n", limit = 2); card = Triple(t, sp[0], sp.getOrElse(1) { "" }); cardT = 6f
            }
            scene.draw(now / 1000f)
            // 진행 한 줄: 도트 글꼴로 장면 그림 위에 바로 쓴다 (화면에서 함께 커진다)
            Pix.text(labelCv, Hud.title(), 6f, Scene.HOR - 58f, 10, Pix.col("#ffe7a8"), android.graphics.Paint.Align.LEFT, true)

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (e: IllegalStateException) { }
                }
            }
            if (visible) handler.postDelayed(frame, 66L) // 초당 15장 정도
        }

        // 그림 좌표 → 화면 좌표
        private var scale = 1f; private var ox = 0f; private var oy = 0f

        private fun render(cv: Canvas) {
            val sw = cv.width; val sh = cv.height
            scale = maxOf(sw.toFloat() / Scene.W, sh.toFloat() / Scene.H)
            val vw = sw / scale; val vh = sh / scale
            val left = (Scene.W - vw) / 2f
            val top = ((Scene.H - vh) / 2f).coerceIn(0f, (Scene.H - vh).coerceAtLeast(0f))
            ox = left; oy = top
            val k = Scene.SS
            src.set((left * k).toInt(), (top * k).toInt(), ((left + vw) * k).toInt(), ((top + vh) * k).toInt())
            dst.set(0, 0, sw, sh)
            cv.drawBitmap(scene.bitmap, src, dst, paint)
            drawPops(cv)
            drawTalk(cv)
            drawCard(cv)
        }
        private fun sxOf(x: Float) = (x - ox) * scale
        private fun syOf(y: Float) = (y - oy) * scale

        private val popTx = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        private fun drawPops(cv: Canvas) {
            val d = cv.width / 400f
            for (pp in scene.pops) {
                val a = (min(1f, pp.t) * 255).toInt()
                popTx.textSize = (if (pp.gold) 17 else 13) * d
                val x = sxOf(pp.x); val y = syOf(pp.y)
                popTx.color = Color.argb(a, 30, 20, 10); cv.drawText(pp.text, x + 2 * d, y + 2 * d, popTx)
                popTx.color = if (pp.gold) Color.argb(a, 255, 211, 106) else Color.argb(a, 255, 244, 214); cv.drawText(pp.text, x, y, popTx)
            }
        }

        private val boardHi = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        private val barBg = Paint().apply { color = Color.parseColor("#1a1410") }
        private val barEdge = Paint().apply { color = Color.parseColor("#c8a050"); style = Paint.Style.STROKE }
        private val barFill = Paint().apply { color = Color.parseColor("#e0b45a") }
        private val shadow = Paint().apply { isAntiAlias = true; color = Color.parseColor("#1a1410"); typeface = Typeface.DEFAULT_BOLD }

        /** 띠 위의 진행 표시: "항해 14 · 소금여울 가는 중" + 진행 막대 + 자원 한 줄. 늘 보인다. */
        private fun drawBoard(cv: Canvas) {
            val d = cv.width / 400f
            val now = System.currentTimeMillis()
            val v = Game.voy
            val g = Game.goal()
            val title: String; val sub: String; val frac: Float
            if (v != null) {
                val f = Game.voyFrac(now)
                val pn = Data.port(v.port).name
                val left = ((1f - f) * v.dur / 60f).toInt() + 1
                title = "항해 " + (Game.voyages + 1) + " · " + when {
                    v.encounter && f > Data.fPirate - .055f && f < Data.fPirate + .035f -> "해적과 싸우는 중!"
                    f < Data.fArrive -> pn + " 가는 중"
                    f < Data.fLeave -> pn + "에서 짐 파는 중"
                    else -> "섬으로 돌아오는 중"
                }
                sub = "귀항까지 " + left + "분" + (if (Game.treasurePending) " · 보물섬 경유" else "") + (if (Game.packed) " · 짐 정리 +20%" else "")
                frac = f
            } else if (g != null) {
                title = "섬 · 다음 목표 " + g.name
                sub = Game.goalProgress(g) + if (!g.basic && g.ready && Game.canPay(g.cost)) " · 앱에서 열기" else ""
                frac = g.cost?.let { c ->
                    val parts = listOfNotNull(if (c.wood > 0) min(1f, Game.wood / c.wood.toFloat()) else null,
                        if (c.stone > 0) min(1f, Game.stone / c.stone.toFloat()) else null,
                        if (c.gold > 0) min(1f, Game.gold / c.gold.toFloat()) else null)
                    if (parts.isEmpty()) 1f else parts.average().toFloat()
                } ?: 0f
            } else { title = "섬 · 배가 곧 떠나요"; sub = ""; frac = 0f }
            val res = "나무 " + Game.wood + " · 돌 " + Game.stone + " · 금화 " + "%,d".format(Game.gold)
            val y0 = syOf(Scene.HOR - 74f)
            val x0 = 22 * d; val w = cv.width - 44 * d
            fun outlined(t: String, x: Float, y: Float, size: Float, cl: Int, align: Paint.Align, maxW: Float) {
                shadow.textSize = size; shadow.textAlign = align
                while (shadow.measureText(t) > maxW && shadow.textSize > 9 * d) shadow.textSize -= 1
                boardHi.textSize = shadow.textSize; boardHi.textAlign = align
                for ((dx, dy) in listOf(-2 to 0, 2 to 0, 0 to -2, 0 to 2, 2 to 2)) cv.drawText(t, x + dx * d, y + dy * d, shadow)
                boardHi.color = cl; cv.drawText(t, x, y, boardHi)
            }
            outlined(title, x0, y0, 17 * d, Color.parseColor("#ffe7a8"), Paint.Align.LEFT, w)
            val by = y0 + 9 * d; val bh = 9 * d
            cv.drawRect(x0, by, x0 + w, by + bh, barBg)
            cv.drawRect(x0 + 2 * d, by + 2 * d, x0 + 2 * d + (w - 4 * d) * frac.coerceIn(0f, 1f), by + bh - 2 * d, barFill)
            barEdge.strokeWidth = 2 * d; cv.drawRect(x0, by, x0 + w, by + bh, barEdge)
            if (sub.isNotEmpty()) outlined(sub, x0, by + bh + 17 * d, 13 * d, Color.parseColor("#f4ead2"), Paint.Align.LEFT, w * .52f)
            outlined(res, x0 + w, by + bh + 17 * d, 13 * d, Color.parseColor("#ffd36a"), Paint.Align.RIGHT, if (sub.isEmpty()) w else w * .46f)
        }

        private val talkBg = Paint().apply { isAntiAlias = true; color = Color.parseColor("#fbf6e8") }
        private val talkTx = Paint().apply { isAntiAlias = true; color = Color.parseColor("#3a2a1a"); textAlign = Paint.Align.CENTER }
        private val talkName = Paint().apply { isAntiAlias = true; color = Color.parseColor("#a0683a"); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }

        /** 주민 머리 위 말풍선. 결과 카드가 떠 있으면 비켜 준다. */
        private fun drawTalk(cv: Canvas) {
            if (cardT > 0f) return
            val text = scene.talkText ?: return
            val (wx, wy) = scene.talkPos() ?: return
            val d = cv.width / 400f
            val px = sxOf(wx); val py = syOf(wy)
            if (px < 0 || px > cv.width) return
            talkTx.textSize = 15 * d; talkName.textSize = 12 * d
            val name = scene.talkName
            while (talkTx.measureText(text) > cv.width - 60 * d && talkTx.textSize > 9 * d) talkTx.textSize -= 1
            val w = talkTx.measureText(text) + 24 * d; val h = 46 * d
            val left = (px - w / 2).coerceIn(8 * d, cv.width - w - 8 * d)
            val top = py - h - 10 * d
            val a = (min(1f, scene.talkT) * 255).toInt()
            talkBg.alpha = (a * .94f).toInt(); talkTx.alpha = a; talkName.alpha = a
            cv.drawRoundRect(android.graphics.RectF(left, top, left + w, top + h), 12 * d, 12 * d, talkBg)
            val tail = android.graphics.Path().apply { moveTo(px - 6 * d, top + h); lineTo(px + 6 * d, top + h); lineTo(px, top + h + 9 * d); close() }
            cv.drawPath(tail, talkBg)
            cv.drawText(name, left + w / 2, top + 17 * d, talkName)
            cv.drawText(text, left + w / 2, top + 37 * d, talkTx)
        }

        /** 다시 화면을 켰을 때, 비운 동안 모인 자원을 양피지 카드로 몇 초 보여준다. */
        private val badgeSrc = android.graphics.Rect(); private val badgeDst = android.graphics.RectF()
        private val badgePaint = Paint().apply { isFilterBitmap = false }
        private fun drawCard(cv: Canvas) {
            val r = card ?: return
            if (cardT <= 0f) { card = null; return }
            val a = min(1f, cardT / 1.2f).coerceIn(0f, 1f)
            val w = cv.width * .78f; val h = w * .36f
            val x = (cv.width - w) / 2f; val y = cv.height * .64f
            val d = cv.width / 400f
            for (p in listOf(cardBg, cardEdge, cardTitle, cardText)) p.alpha = (255 * a).toInt()
            cardEdge.strokeWidth = 3 * d
            val box = android.graphics.RectF(x, y, x + w, y + h)
            cv.drawRoundRect(box, 14 * d, 14 * d, cardBg)
            cv.drawRoundRect(box, 14 * d, 14 * d, cardEdge)
            cardTitle.textSize = 20 * d; cardText.textSize = 16 * d
            // 긴 문장은 카드 안에 들어가게 글씨를 줄인다
            fun fit(p: Paint, t: String, size: Float) { p.textSize = size; while (p.measureText(t) > w - 24 * d && p.textSize > 9 * d) p.textSize -= 1 }
            // 선원 사건 카드: 제목 앞 \u0001번호\u0001 → 왼쪽에 선원 얼굴 배지
            var title = r.first; var tx = x + w / 2; var tw = w
            if (title.startsWith("\u0001")) {
                val k = title.indexOf('\u0001', 1)
                val idx = title.substring(1, maxOf(1, k)).toIntOrNull() ?: -1
                title = title.substring(k + 1)
                val bs = h * .78f
                Art.cell("crew_event_badge", 4, 3, idx, bs.toInt())?.let { bm ->
                    badgeSrc.set(0, 0, bm.width, bm.height); badgeDst.set(x + 10 * d, y + (h - bs) / 2, x + 10 * d + bs, y + (h + bs) / 2)
                    badgePaint.alpha = (255 * a).toInt(); cv.drawBitmap(bm, badgeSrc, badgeDst, badgePaint)
                    tx = x + 10 * d + bs + (w - 10 * d - bs) / 2; tw = w - 10 * d - bs
                }
            }
            fun fit2(p: Paint, t: String, size: Float) { p.textSize = size; while (p.measureText(t) > tw - 24 * d && p.textSize > 9 * d) p.textSize -= 1 }
            fit2(cardTitle, title, 20 * d)
            cv.drawText(title, tx, y + h * .36f, cardTitle)
            fit2(cardText, r.second, 16 * d)
            cv.drawText(r.second, tx, y + h * .62f, cardText)
            if (r.third.isNotEmpty()) { fit2(cardText, r.third, 16 * d); cv.drawText(r.third, tx, y + h * .85f, cardText) }
        }
    }
}
