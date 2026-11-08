package com.littleisland.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * 그림 파일 (assets 안 art 폴더의 png). 파일이 있으면 그 그림을 쓰고, 없으면 코드로 그린 그림을 쓴다.
 * 파일 이름과 크기 규칙은 저장소의 ART_GUIDE.md 참고.
 * 진한 분홍(#FF00FF 근처) 배경은 투명으로 지운다.
 */
object Art {
    private var ctx: Context? = null
    private val raw = HashMap<String, Bitmap?>()
    private val cache = HashMap<String, Bitmap?>()
    // 배경화면과 앱이 서로 다른 스레드에서 동시에 그리므로, 그리기 도구는 스레드마다 따로 쓴다
    private val tl = ThreadLocal.withInitial { Triple(Paint().apply { isFilterBitmap = false }, Rect(), android.graphics.RectF()) }

    fun init(c: Context) { if (ctx == null) ctx = c.applicationContext }

    @Synchronized private fun load(name: String): Bitmap? = raw.getOrPut(name) {
        val c = ctx ?: return@getOrPut null
        try {
            val b = c.assets.open("art/$name.png").use { BitmapFactory.decodeStream(it) } ?: return@getOrPut null
            val m = b.copy(Bitmap.Config.ARGB_8888, true)
            m.setHasAlpha(true)   // 색 수가 적은 png는 '불투명'으로 읽혀서, 지운 곳이 까맣게 나올 수 있다
            val px = IntArray(m.width * m.height)
            m.getPixels(px, 0, m.width, 0, 0, m.width, m.height)
            for (i in px.indices) {
                val v = px[i]; val r = Color.red(v); val g = Color.green(v); val bl = Color.blue(v)
                if (r > 180 && bl > 180 && g < 90) px[i] = 0          // 분홍 배경 지우기
            }
            m.setPixels(px, 0, m.width, 0, 0, m.width, m.height)
            m
        } catch (e: Exception) { null }
    }

    @Synchronized fun has(name: String) = load(name) != null

    /** 그림 전체를 가로 w도트로 줄여서 (도트 느낌을 살리려고 한 번 줄인 뒤 그대로 쓴다) */
    @Synchronized fun get(name: String, w: Int): Bitmap? = cache.getOrPut("$name@$w") {
        val b = load(name) ?: return@getOrPut null
        val h = (b.height * w / b.width.toFloat()).toInt().coerceAtLeast(1)
        mutable(Bitmap.createScaledBitmap(b, w, h, true)).also { clean(it) }
    }

    /** 격자 그림(cols x rows)에서 한 칸을 가로 w도트로 */
    @Synchronized fun cell(name: String, cols: Int, rows: Int, idx: Int, w: Int): Bitmap? = cache.getOrPut("$name#$cols.$rows.$idx@$w") {
        val b = load(name) ?: return@getOrPut null
        val cw = b.width / cols; val ch = b.height / rows
        val cx = (idx % cols) * cw; val cy = (idx / cols) * ch
        val part = Bitmap.createBitmap(b, cx, cy, cw, ch)
        val h = (ch * w / cw.toFloat()).toInt().coerceAtLeast(1)
        mutable(Bitmap.createScaledBitmap(part, w, h, true)).also { clean(it) }
    }

    private fun mutable(b: Bitmap): Bitmap = (if (b.isMutable) b else b.copy(Bitmap.Config.ARGB_8888, true)).also { it.setHasAlpha(true) }

    /** 줄이면서 생긴 반투명 가장자리를 또렷하게 */
    private fun clean(b: Bitmap) {
        if (!b.isMutable) return
        val px = IntArray(b.width * b.height)
        b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        for (i in px.indices) { val a = Color.alpha(px[i]); px[i] = if (a < 110) 0 else (px[i] or (0xff shl 24)) }
        b.setPixels(px, 0, b.width, 0, 0, b.width, b.height)
    }

    /** 가운데-아래를 (x, bottom)에 맞춰 그린다. flip이면 좌우를 뒤집는다.
     *  장면 캔버스가 200도트의 k배 크기면, 그림도 k배로 받아 둔 것이라 1/k 크기 자리에 그린다 */
    fun draw(c: Canvas, b: Bitmap, x: Float, bottom: Float, flip: Boolean = false, alpha: Int = 255) {
        val (p, src, dst) = tl.get()!!
        p.alpha = alpha
        val k = maxOf(1, c.width / 200).toFloat()
        val w = b.width / k; val h = b.height / k
        val l = x - w / 2f; val t = bottom - h
        if (flip) { c.save(); c.scale(-1f, 1f, x, 0f) }
        src.set(0, 0, b.width, b.height); dst.set(l, t, l + w, t + h)
        c.drawBitmap(b, src, dst, p)
        if (flip) c.restore()
        p.alpha = 255
    }
}
