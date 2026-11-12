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
        // 게임 화면 + 맨 아래 배너 광고 (앱 화면에만)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        root.addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        Ads.init(this)
        Ads.banner(this)?.let { root.addView(it) }
        setContentView(root)
        // 효과음
        @Suppress("DEPRECATION")
        sp = android.media.SoundPool(6, android.media.AudioManager.STREAM_MUSIC, 0)
        for (k in listOf("tap", "up", "win", "lose", "chest", "boom")) { val id = resources.getIdentifier("sfx_$k", "raw", packageName)
            if (id != 0) sfx[k] = sp!!.load(this, id, 1) }
        // 알림 허락 (안드로이드 13부터)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 3)
    }
    private var sp: android.media.SoundPool? = null
    // 배경음악: 앱 화면에서만 (배경화면에서는 절대 안 튼다)
    private var bgm: android.media.MediaPlayer? = null
    override fun music(on: Boolean) {
        if (on && Game.musicOn) {
            if (bgm == null) bgm = try { android.media.MediaPlayer.create(this, resources.getIdentifier("bgm_island", "raw", packageName))?.apply { isLooping = true; setVolume(.35f, .35f) } } catch (e: Exception) { null }
            if (bgm?.isPlaying == false) bgm?.start()
        } else bgm?.let { if (it.isPlaying) it.pause() }
    }
    private val sfx = HashMap<String, Int>()
    override fun sound(name: String) { if (Game.soundOn) sfx[name]?.let { sp?.play(it, if (name == "tap") .5f else .8f, if (name == "tap") .5f else .8f, 1, 0, 1f) } }

    override fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    override fun copySave() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("리틀아일랜드 저장 코드", Game.exportSave()))
        toast("저장 코드를 복사했어요. 새 폰에서 [저장 넣기]를 누르세요")
    }
    override fun pasteSave() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val code = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if (!code.startsWith("LI1:")) { toast("먼저 저장 코드를 복사해 주세요"); return }
        AlertDialog.Builder(this).setTitle("저장 코드 불러오기").setMessage("지금 진행이 저장 코드의 진행으로 바뀌어요. 되돌릴 수 없어요.")
            .setPositiveButton("불러오기") { _, _ -> if (Game.importSave(code)) { toast("불러왔어요!"); view.invalidate() } else toast("저장 코드가 올바르지 않아요") }
            .setNegativeButton("취소", null).show()
    }
    override fun showAd(onReward: () -> Unit) { if (!Ads.showReward(this) { ui.post { onReward(); view.invalidate() } }) toast("광고를 불러오는 중이에요. 잠시 뒤에 다시 눌러 주세요") }
    override fun openJournal() = openJournalList()
    override fun openMap() = openMapPuzzle()
    override fun openCargo() = openCargoPuzzle()
    override fun openTraining() = openTrainingPuzzle()
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
            "소식은 기다려 줘요" to "비운 동안의 보상은 12시간까지 모이고, 광고를 보면 24시간까지 받아요. 이야기는 항해일지에 쌓이고, 읽지 않아도 섬은 계속 움직여요.")
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

    private fun openTrainingPuzzle() {
        val t = Game.trainStart() ?: return toast("오늘 훈련은 다 했어요. 내일 다시 3번!")
        var dlg: AlertDialog? = null
        val timer = TextView(this).apply { textSize = 15f }
        var left = Fleet.trainSeconds
        lateinit var v: TrainingPuzzleView
        val tick = object : Runnable {
            override fun run() {
                if (v.over) return
                left--
                timer.text = "남은 시간 " + left + "초"
                if (left <= 0) v.timeUp() else ui.postDelayed(this, 1000L)
            }
        }
        v = TrainingPuzzleView(this, t, Fleet.trainMaxSwaps) { ok, swaps ->
            ui.removeCallbacks(tick)
            ui.postDelayed({
                dlg?.dismiss()
                val msg = if (ok) "성공! 뽑기 쪽지 +" + Game.trainDone(t, swaps) + "장 (" + swaps + "번 바꿈)" else "아쉬워요. 다음엔 더 빨리 맞춰 봐요"
                AlertDialog.Builder(this).setTitle("훈련 끝").setMessage(msg + "\n오늘 남은 도전 " + Game.trainLeft + "번").setPositiveButton("좋아요", null).show()
            }, 600L)
        }
        timer.text = "남은 시간 " + left + "초"
        val pad = (16 * dp).toInt()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, 0) }
        box.addView(TextView(this).apply { text = "상자 두 개를 차례로 누르면 자리가 바뀌어요. 위 표시처럼 칸마다 같은 상자를 모아 주세요."; textSize = 13f })
        box.addView(timer)
        box.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = pad / 2 })
        dlg = AlertDialog.Builder(this).setTitle("선원 훈련").setView(box).setNegativeButton("그만하기") { _, _ -> v.timeUp() }.show()
        dlg.setOnDismissListener { ui.removeCallbacks(tick) }
        ui.postDelayed(tick, 1000L)
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
        val all = Story.entries.filter { Game.isOpen(it.id) }
        if (all.isEmpty()) { toast("아직 일지가 없어요"); return }
        // 새 글은 맨 위에, 목록을 열면 모두 읽음으로
        val fresh = all.filter { !Game.isRead(it.id) }.map { it.id }.toSet()
        val list = all.sortedWith(compareBy({ it.id !in fresh }, { it.kind }, { Story.entries.indexOf(it) }))
        Game.markAllRead()
        val items = list.map { (if (it.id in fresh) "● 새 글 · " else "") + it.label + " · " + it.title }.toTypedArray()
        AlertDialog.Builder(this).setTitle("항해일지")
            .setItems(items) { _, i -> openEntry(list, i) }
            .setNegativeButton("닫기", null).show()
    }

    private fun openEntry(list: List<Story.Entry>, i: Int) {
        val e = list[i]
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
        // 선원 이야기·특별 항해는 그 선원 그림을 위에
        if (e.art >= 0) (if (e.art >= 200) Art.cell("visitor_story_art", 4, 2, e.art - 200, 384) else Art.cell(if (e.art >= 100) "crew_story_art_3" else "crew_story_art", 4, 3, e.art % 100, 384))?.let { bm ->
            box.addView(ImageView(this).apply {
                setImageDrawable(android.graphics.drawable.BitmapDrawable(resources, bm).apply { paint.isFilterBitmap = false }); adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding((60 * dp).toInt(), (8 * dp).toInt(), (60 * dp).toInt(), 0)
            })
        }
        box.addView(tv)
        AlertDialog.Builder(this).setTitle(e.label + " · " + e.title).setView(ScrollView(this).apply { addView(box) })
            .apply { if (i + 1 < list.size) setPositiveButton("다음 글") { _, _ -> openEntry(list, i + 1) } }
            .setNeutralButton("목록") { _, _ -> openJournal() }
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
                Toast.makeText(this, "설정 > 배경화면에서 '리틀아일랜드'를 골라 주세요", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sp = getSharedPreferences("island", Context.MODE_PRIVATE)
        Game.catchUp()
        Game.report?.let { r ->
            Game.report = null
            val t = if (r.minutes >= 60) (r.minutes / 60).toString() + "시간 " + (r.minutes % 60) + "분" else r.minutes.coerceAtLeast(1).toString() + "분"
            if (t.isNotEmpty()) view.showReport(r)
        }
        view.running = true; view.invalidate(); music(true)
    }

    override fun onDestroy() { bgm?.release(); bgm = null; super.onDestroy() }
    override fun onPause() { super.onPause(); music(false); view.running = false; Game.save(); ChestReceiver.schedule(this) }

    /** 도트 화면을 그리고 키워서 보여준다 */
    class GameView(ctx: Context, host: AppUi.Host) : View(ctx) {
        val scene = Scene()
        private val app = AppUi(scene, host)
        var running = false
        private val paint = Paint().apply { isFilterBitmap = true }
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
            src.set(0, 0, app.bitmap.width, app.bitmap.height)
            dst.set(0, 0, width, (app.H * scale).toInt())
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(app.bitmap, src, dst, paint)
            if (running) postInvalidateDelayed(66L)
        }

        fun showReport(r: Game.Report) = app.showReport(r)
        private var downX = 0f; private var downY = 0f; private var lastY = 0f; private var moved = false; private var held = false
        // 강화 버튼을 길게 누르면 계속 올라간다
        private val repeat = object : Runnable { override fun run() { held = true; app.tap(downX / scale, downY / scale); invalidate(); postDelayed(this, 110L) } }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; lastY = e.y; moved = false; held = false
                    if (app.holdable(e.x / scale, e.y / scale)) postDelayed(repeat, 450L) }
                MotionEvent.ACTION_MOVE -> {
                    // 위아래로 끌면 목록이 밀린다
                    if (!moved && Math.abs(e.y - downY) > 8 * scale) { moved = true; removeCallbacks(repeat) }
                    if (moved) { app.scrollBy((e.y - lastY) / scale); lastY = e.y; invalidate() }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { removeCallbacks(repeat); if (!moved && !held && e.action == MotionEvent.ACTION_UP) app.tap(e.x / scale, e.y / scale); if (moved) app.scrollEnd(); invalidate() }
            }
            return true
        }
    }
}
