package com.littleisland.game

import android.app.Activity
import android.app.AlertDialog
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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min
import android.view.MotionEvent

/**
 * 앱 화면: 전부 도트로 그린다 (AppUi). 위에는 섬·바다, 가운데 아이콘 탭, 아래 나무 판넬 속 칸.
 * 홈 화면(배경화면)은 보기만 하고, 고르고 짓는 건 여기서 한다.
 */
class MainActivity : Activity(), AppUi.Host {

    private lateinit var view: GameView
    private var dp = 1f
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Game.init(this); Pix.load(this)
        dp = resources.displayMetrics.density
        view = GameView(this, this)
        setContentView(view)
    }

    override fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    override fun openJournal() = openJournalList()
    override fun openMap() = openMapPuzzle()
    override fun openCargo() = openCargoPuzzle()
    override fun openTutorial() = showTutorial(0)
    override fun pickTime() {
        val opts = arrayOf("지금", "낮", "노을", "밤"); val hs = arrayOf<Float?>(null, 12f, 18.6f, 23f)
        AlertDialog.Builder(this).setItems(opts) { _, i -> view.scene.hourOverride = hs[i] }.show()
    }
    override fun setWallpaper() = openWallpaperPicker()

    private fun showTutorial(i: Int) {
        val sc = listOf(
            "주머니 속 작은 섬" to "단·보리·해온이 나무와 돌을 모아요. 홈 화면에서는 섬이 움직이는 모습과 소식만 편하게 봐요.",
            "섬이 알아서 자라요" to "재료가 모이면 숙소·창고·부두를 자동으로 지어요. 직접 고르고 싶으면 앱에서 짓기를 눌러요.",
            "배가 알아서 다녀와요" to "부두가 생기면 배가 짐을 싣고 항구로 떠나 금화를 벌어 와요. 해적은 자동으로 싸워요.",
            "금화로 더 멀리" to "모은 금화로 배를 고치고 새 항구를 열어요. 바다에 반짝이는 유리병이나 보물상자가 떠오면 홈 화면에서 톡 쳐서 주워요.",
            "소식은 기다려 줘요" to "비운 동안의 보상은 최대 8시간까지 모여요. 이야기는 항해일지에 쌓이고, 읽지 않아도 섬은 계속 움직여요.")
        if (i >= sc.size) return
        AlertDialog.Builder(this).setTitle((i + 1).toString() + "/" + sc.size + "  " + sc[i].first).setMessage(sc[i].second)
            .setPositiveButton(if (i == sc.size - 1) "섬으로 가기" else "다음") { _, _ -> showTutorial(i + 1) }
            .setNegativeButton("건너뛰기", null).show()
    }

    private fun openCargoPuzzle() {
        var dlg: AlertDialog? = null
        val v = CargoPuzzleView(this) {
            Game.pack()
            ui.postDelayed({ dlg?.dismiss(); toast("짐을 다 실었어요! 다음 판매금 +" + (Game.PACK_BONUS * 100).toInt() + "%") }, 700L)
        }
        val pad = (16 * dp).toInt()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, 0) }
        box.addView(TextView(this).apply { text = "아래 상자를 고르고 짐칸을 누르면 놓여요. 놓인 상자를 누르면 다시 빠져요."; textSize = 13f })
        box.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = pad / 2 })
        box.addView(Button(this).apply { text = "고른 상자 돌리기"; setOnClickListener { v.rotateSelected() } })
        dlg = AlertDialog.Builder(this).setTitle("짐 정리").setView(box).setNegativeButton("나중에", null).show()
    }

    private fun openMapPuzzle() {
        var dlg: AlertDialog? = null
        val v = MapPuzzleView(this) {
            if (Game.solveMap()) {
                ui.postDelayed({
                    dlg?.dismiss()
                    AlertDialog.Builder(this).setTitle("지도가 완성됐어요!")
                        .setMessage("X 표시가 보물섬이에요. 다음 항해 때 배가 들렀다가 보물을 싣고 돌아와요.")
                        .setPositiveButton("좋아요", null).show()
                }, 700L)
            }
        }
        val pad = (16 * dp).toInt()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, 0) }
        box.addView(TextView(this).apply { text = "빈칸 옆 조각을 누르면 미끄러져요. 원래 지도로 맞춰 보세요."; textSize = 13f })
        box.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = pad / 2 })
        dlg = AlertDialog.Builder(this).setTitle("보물 지도 맞추기").setView(box).setNegativeButton("나중에", null).show()
    }

    private fun openJournalList() {
        val list = Story.entries.filter { Game.isOpen(it.id) }
            .sortedWith(compareBy({ it.kind }, { Story.entries.indexOf(it) }))
        if (list.isEmpty()) { toast("아직 일지가 없어요"); return }
        val items = list.map { (if (Game.isRead(it.id)) "" else "● ") + it.label + " · " + it.title }.toTypedArray()
        AlertDialog.Builder(this).setTitle("항해일지")
            .setItems(items) { _, i -> openEntry(list[i]) }
            .setNegativeButton("닫기", null).show()
    }

    private fun openEntry(e: Story.Entry) {
        Game.markRead(e.id)
        val tv = TextView(this).apply {
            text = (if (e.summary.isNotEmpty()) Story.fill(e.summary) + "\n\n" else "") + Story.fill(e.body)
            textSize = 16f
            setLineSpacing(0f, 1.35f)
            setPadding((22 * dp).toInt(), (12 * dp).toInt(), (22 * dp).toInt(), (12 * dp).toInt())
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (e.kind == Story.MAIN) {
            val part = e.label.substringBefore("부").trim().toIntOrNull() ?: 0
            val pic = if (e.label.contains("60화")) "ending" else "part_$part"
            Art.get(pic, 480)?.let { bm ->
                box.addView(ImageView(this).apply {
                    setImageBitmap(bm); adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), 0)
                })
            }
        }
        box.addView(tv)
        AlertDialog.Builder(this).setTitle(e.label + " · " + e.title).setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("목록") { _, _ -> openJournal() }
            .setNegativeButton("닫기", null).show()
    }

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
                Toast.makeText(this, "설정 > 배경화면에서 '리틀 아일랜드'를 골라 주세요", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sp = getSharedPreferences("island", Context.MODE_PRIVATE)
        if (!sp.getBoolean("tutorialSeen", false)) { sp.edit().putBoolean("tutorialSeen", true).apply(); showTutorial(0) }
        Game.catchUp()
        Game.report?.let { r ->
            Game.report = null
            val t = if (r.minutes >= 60) (r.minutes / 60).toString() + "시간 " + (r.minutes % 60) + "분" else r.minutes.coerceAtLeast(1).toString() + "분"
            AlertDialog.Builder(this).setTitle(t + " 동안 섬에서")
                .setMessage("나무 +" + r.wood + ", 돌 +" + r.stone + ", 금화 +" + r.gold +
                    (if (r.voyages > 0) "\n항해 " + r.voyages + "번 다녀왔어요." else "") + (if (r.full) "\n창고가 가득 찼어요. 창고를 넓혀 보세요." else ""))
                .setPositiveButton("좋아요", null).show()
        }
        view.running = true; view.invalidate()
    }

    override fun onPause() { super.onPause(); view.running = false; Game.save() }

    /** 도트 화면을 그리고 키워서 보여준다 */
    class GameView(ctx: Context, host: AppUi.Host) : View(ctx) {
        val scene = Scene()
        private val app = AppUi(scene, host)
        var running = false
        private val paint = Paint().apply { isFilterBitmap = false }
        private val src = Rect(); private val dst = Rect()
        private var last = SystemClock.uptimeMillis()
        private var scale = 1f

        override fun onDraw(canvas: Canvas) {
            val now = SystemClock.uptimeMillis()
            val dt = min(0.1f, (now - last) / 1000f); last = now
            scale = width.toFloat() / AppUi.W
            app.resize((height / scale).toInt())
            scene.update(dt)
            scene.draw(now / 1000f)
            app.refreshPopup()
            app.draw()
            src.set(0, 0, AppUi.W, app.H)
            dst.set(0, 0, width, (app.H * scale).toInt())
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(app.bitmap, src, dst, paint)
            if (running) postInvalidateDelayed(66L)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_UP) { app.tap(e.x / scale, e.y / scale); invalidate() }
            return true
        }
    }
}
