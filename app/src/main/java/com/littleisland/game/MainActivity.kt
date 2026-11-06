package com.littleisland.game

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min

/** 앱 화면: 섬 미리보기 + 배경화면 설정 버튼 + 시간대 미리보기 */
class MainActivity : Activity() {

    private lateinit var preview: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0d1726"))
            setPadding((16 * dp).toInt(), (28 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }
        val title = TextView(this).apply {
            text = "작은 섬"
            setTextColor(Color.parseColor("#f2b544"))
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sub = TextView(this).apply {
            text = "테스트 버전 " + versionName() + " · 홈 화면을 옆으로 넘기면 바다와 항구가 나와요"
            setTextColor(Color.parseColor("#93a7bf"))
            textSize = 13f
            setPadding(0, (4 * dp).toInt(), 0, (12 * dp).toInt())
        }
        preview = PreviewView(this)
        val set = Button(this).apply {
            text = "배경화면으로 설정"
            setOnClickListener { openWallpaperPicker() }
        }
        val times = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setGravity(Gravity.CENTER) }
        val opts = listOf("지금" to null, "낮" to 12f, "노을" to 18.6f, "밤" to 23f)
        for ((label, h) in opts) {
            times.addView(Button(this).apply {
                text = label
                setOnClickListener { preview.scene.hourOverride = h }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(title)
        root.addView(sub)
        root.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(times)
        root.addView(set)
        setContentView(root)
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (e: Exception) { "" }

    private fun openWallpaperPicker() {
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, IslandWallpaperService::class.java))
        }
        try {
            startActivity(direct)
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e: Exception) {
                Toast.makeText(this, "설정 > 배경화면에서 '작은 섬'을 골라 주세요", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() { super.onResume(); preview.running = true; preview.invalidate() }
    override fun onPause() { super.onPause(); preview.running = false }

    /** 앱 안에서 보는 섬 미리보기. 세 페이지를 천천히 번갈아 보여준다. */
    class PreviewView(ctx: Context) : View(ctx) {
        val scene = Scene()
        var running = false
        private val paint = Paint().apply { isFilterBitmap = false }
        private val src = Rect()
        private val dst = Rect()
        private var last = SystemClock.uptimeMillis()
        private var cam = 0f

        override fun onDraw(canvas: Canvas) {
            val now = SystemClock.uptimeMillis()
            val dt = min(0.1f, (now - last) / 1000f)
            last = now
            scene.update(dt)
            scene.draw(now / 1000f)
            val scale = height.toFloat() / Scene.H
            val viewW = (width / scale).coerceAtMost(Scene.W.toFloat())
            val page = ((now / 4000L) % 3).toInt()
            val target = (page * Scene.PAGE + (Scene.PAGE - viewW) / 2f).coerceIn(0f, Scene.W - viewW)
            cam += (target - cam) * 0.08f
            src.set(cam.toInt(), 0, (cam + viewW).toInt(), Scene.H)
            dst.set(0, 0, width, height)
            canvas.drawBitmap(scene.bitmap, src, dst, paint)
            if (running) postInvalidateDelayed(66L)
        }
    }
}
