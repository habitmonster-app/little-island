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
    private lateinit var diag: TextView
    private lateinit var res: TextView
    private lateinit var rowHouse: Row
    private lateinit var rowStore: Row
    private lateinit var rowDock: Row
    private val ui = android.os.Handler(android.os.Looper.getMainLooper())
    private val refresher = object : Runnable { override fun run() { refresh(); ui.postDelayed(this, 500L) } }

    /** 건물 한 줄: 이름 · 설명/비용 · 짓기 버튼 */
    class Row(val text: TextView, val btn: Button, val layout: LinearLayout)

    private fun buildRow(name: String, desc: String, act: () -> Boolean): LinearLayout {
        val label = TextView(this).apply { setTextColor(Color.parseColor("#c9d6e4")); textSize = 13f }
        val btn = Button(this).apply { text = "짓기" }
        val l = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setGravity(Gravity.CENTER_VERTICAL) }
        l.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        l.addView(btn)
        btn.setOnClickListener {
            if (act()) Toast.makeText(this, name + "을(를) 지었어요!", Toast.LENGTH_SHORT).show()
            refresh()
        }
        l.tag = Row(label, btn, l)
        label.tag = name + " · " + desc
        return l
    }
    private fun LinearLayout.withRow(f: (Row) -> Unit): LinearLayout { f(tag as Row); return this }

    private fun costText(c: Pair<Int, Int>?): String =
        if (c == null) "완료" else "나무 " + c.first + (if (c.second > 0) " · 돌 " + c.second else "")

    private fun setRow(r: Row, level: String, c: Pair<Int, Int>?) {
        r.text.text = (r.text.tag as String) + "  " + level + "\n" + costText(c)
        r.btn.isEnabled = c != null && Game.wood >= c.first && Game.stone >= c.second
        r.btn.text = if (c == null) "완료" else "짓기"
    }

    private fun refresh() {
        res.text = "나무 " + Game.wood + "/" + Game.cap() + "    돌 " + Game.stone + "/" + Game.cap() + "    주민 " + Game.pop() + "명"
        setRow(rowHouse, Game.houses.toString() + "/3", Game.houseCost())
        setRow(rowStore, Game.storage.toString() + "/2", Game.storeCost())
        setRow(rowDock, if (Game.dock) "1/1" else "0/1", Game.dockCost())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Game.init(this)
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
            text = "테스트 버전 " + versionName() + "\n홈 화면 빈 곳을 톡 치면 섬 → 바다 → 항구로 이동해요"
            setTextColor(Color.parseColor("#93a7bf"))
            textSize = 13f
            setPadding(0, (4 * dp).toInt(), 0, (12 * dp).toInt())
        }
        diag = TextView(this).apply {
            setTextColor(Color.parseColor("#5f7590"))
            textSize = 11f
            setPadding(0, 0, 0, (8 * dp).toInt())
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
        root.addView(diag)
        res = TextView(this).apply {
            setTextColor(Color.parseColor("#eef3f8"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, (10 * dp).toInt(), 0, (6 * dp).toInt())
        }
        root.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(times)
        root.addView(res)
        root.addView(buildRow("집", "주민 +1") { Game.buildHouse() }.withRow { rowHouse = it })
        root.addView(buildRow("창고", "더 많이 담기") { Game.buildStore() }.withRow { rowStore = it })
        root.addView(buildRow("부두", "짐배가 들러요") { Game.buildDock() }.withRow { rowDock = it })
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

    override fun onResume() {
        super.onResume(); diag.text = Diag.text(this)
        Game.catchUp()
        Game.report?.let { r ->
            Game.report = null
            val t = if (r.minutes >= 60) (r.minutes / 60).toString() + "시간 " + (r.minutes % 60) + "분" else r.minutes.coerceAtLeast(1).toString() + "분"
            android.app.AlertDialog.Builder(this).setTitle(t + " 동안 모인 자원")
                .setMessage("주민 " + Game.pop() + "명이 나무 " + r.wood + "개, 돌 " + r.stone + "개를 모아 뒀어요." + (if (r.full) "\n창고가 가득 찼어요. 창고를 넓혀 보세요." else ""))
                .setPositiveButton("좋아요", null).show()
        }
        ui.post(refresher); preview.running = true; preview.invalidate() }
    override fun onPause() { super.onPause(); preview.running = false; ui.removeCallbacks(refresher); Game.save() }

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
