package com.littleisland.game

import android.graphics.Canvas
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
        private val frame = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(true)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) { lastMs = SystemClock.uptimeMillis(); drawFrame() } else handler.removeCallbacks(frame)
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
            if (action == WallpaperManager.COMMAND_TAP && !paging) {
                tapPage = (tapPage + 1) % 3
                Diag.tap(this@IslandWallpaperService)
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
            scene.draw(now / 1000f)

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

        private fun render(cv: Canvas) {
            val sw = cv.width
            val sh = cv.height
            val scale = sh.toFloat() / Scene.H
            val viewW = (sw / scale).coerceAtMost(Scene.W.toFloat())
            // 페이지 넘김을 지원하면 섬 → 바다 → 항구로 이동, 아니면 섬만 보여줌
            val maxX = Scene.W - viewW
            val target = if (paging) xOffset * maxX
                else (tapPage * Scene.PAGE + (Scene.PAGE - viewW) / 2f).coerceIn(0f, maxX.coerceAtLeast(0f))
            // 톡 쳐서 이동할 때는 카메라가 부드럽게 미끄러진다
            cam = if (cam < 0f || paging) target else cam + (target - cam) * 0.12f
            val sx = cam
            src.set(sx.toInt(), 0, (sx + viewW).toInt(), Scene.H)
            dst.set(0, 0, sw, sh)
            cv.drawBitmap(scene.bitmap, src, dst, paint)
        }
    }
}
