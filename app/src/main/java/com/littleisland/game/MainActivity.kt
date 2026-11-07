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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min

/**
 * 앱 화면: 위에는 섬 미리보기, 아래는 고르고 짓는 곳.
 * 홈 화면(배경화면)은 보기만 하고, 선택은 전부 여기서 한다.
 */
class MainActivity : Activity() {

    private lateinit var preview: PreviewView
    private lateinit var body: LinearLayout
    private var dp = 1f
    private val ui = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable { override fun run() { refresh(); ui.postDelayed(this, 700L) } }

    /** 한 줄: 제목, 설명, 버튼(없을 수도) */
    private class Spec(val key: String, val title: String, val text: String, val btn: String?, val enabled: Boolean, val act: (() -> Unit)?, val header: Boolean = false)
    private class RowView(val title: TextView, val text: TextView, val btn: Button?)
    private var shape = ""
    private val rows = ArrayList<RowView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Game.init(this)
        dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0d1726"))
            setPadding((14 * dp).toInt(), (22 * dp).toInt(), (14 * dp).toInt(), 0)
        }
        root.addView(TextView(this).apply {
            text = "작은 섬"; setTextColor(Color.parseColor("#f2b544")); textSize = 22f; typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "홈 화면 속 섬이 알아서 나무·돌을 모으고, 배가 항구를 오가며 금화를 벌어요."
            setTextColor(Color.parseColor("#9fb3c8")); textSize = 12f
        })
        preview = PreviewView(this)
        val ph = (resources.displayMetrics.heightPixels * .30f).toInt()
        root.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ph).apply { topMargin = (6 * dp).toInt() })
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, (6 * dp).toInt(), 0, (24 * dp).toInt()) }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    // ---------- 화면에 그릴 줄 목록 ----------
    private fun mmss(ms: Long): String {
        val t = (ms / 1000).coerceAtLeast(0)
        return if (t >= 3600) (t / 3600).toString() + "시간 " + (t % 3600 / 60) + "분" else (t / 60).toString() + ":" + "%02d".format(t % 60)
    }

    private fun need(keys: List<String>): String = keys.filter { !Game.has(it) }.joinToString(", ") { k ->
        when {
            k.startsWith("main_") -> "본편 " + k.removePrefix("main_").toIntOrNull() + "화"
            k.startsWith("port_") -> Data.ports.firstOrNull { it.id == k }?.name + " 개방"
            k.startsWith("ship_") -> "배 " + k.removePrefix("ship_") + "단계"
            k == "store_1" -> "창고 1단계"
            k == "first_gather" -> "첫 채집"
            k == "house_1" -> "첫 숙소"
            k == "lighthouse" -> "등대"
            k == "market" -> "장터"
            else -> k
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun specs(): List<Spec> {
        val l = ArrayList<Spec>()
        val now = System.currentTimeMillis()
        val s = Game.ship()

        Game.goal()?.let { g ->
            val canBuy = g.ready && Game.canPay(g.cost)
            val txt = Game.goalProgress(g) + "\n" + when {
                !g.ready -> "조건이 갖춰지면 열려요"
                g.basic && Game.autoBuild && (g.cost?.gold ?: 0) == 0 -> if (canBuy) "지금 지을 수 있어요" else "모이면 자동으로 지어요"
                canBuy -> "지금 할 수 있어요!"
                else -> "모으는 중이에요"
            }
            l.add(Spec("goal", "다음 목표 · " + g.name, txt, if (g.ready) "하기" else null, canBuy, {
                val ok = when (g.key) { "house" -> Game.buildHouse(); "ware" -> Game.upgradeWarehouse(); "dock" -> Game.buildDock(); "field" -> Game.buildField(); else -> Game.buyUnlock(g.key) }
                if (ok) toast(g.name + " 완성!")
            }))
        }
        l.add(Spec("res", "나무 " + Game.wood + "/" + Game.cap() + "   돌 " + Game.stone + "/" + Game.cap() + "   금화 " + "%,d".format(Game.gold),
            "주민 단·보리·해온 + 알바 " + Game.workers.size + "/" + Game.beds(), null, false, null, true))

        // 항해
        l.add(Spec("h_voy", "항해", "", null, false, null, true))
        val v = Game.voy
        val status = if (v != null) {
            val f = Game.voyFrac(now)
            val p = Data.port(v.port)
            val phase = when {
                f < Data.fPirate -> "바다로 나가는 중"
                f < Data.fArrive -> if (v.encounter) "해적을 만났어요" else "항구로 가는 중"
                f < Data.fLeave -> p.name + "에서 짐 파는 중"
                else -> "섬으로 돌아오는 중"
            }
            phase + " · 도착까지 " + mmss(((1f - f) * v.dur * 1000).toLong()) + "\n싣고 간 짐: 나무 " + v.cw + " · 돌 " + v.cs
        } else if (!Game.dock) "부두를 지으면 배가 자동으로 오가요"
        else if (Game.autoSail) "나무가 모이면 바로 떠나요"
        else "자동 출항이 꺼져 있어요"
        l.add(Spec("voy", "지금 항로 · " + Game.port().name + " (왕복 " + mmss((Game.voyageSeconds() * 1000).toLong()) + ")", status,
            if (Game.autoSail) "자동 끄기" else "자동 켜기", Game.dock, { Game.autoSail = !Game.autoSail; if (Game.autoSail) Game.advance(System.currentTimeMillis()); Game.save() }))
        if (!Game.autoSail) l.add(Spec("sail", "지금 출항", "나무 " + s.cargoW + " · 돌 " + s.cargoS + "까지 실어요", "출항", Game.canSail(), { if (Game.sail()) toast("배가 떠났어요") }))
        if (Game.log.isNotEmpty()) l.add(Spec("log", "최근 소식", Game.log.take(4).joinToString("\n"), null, false, null))

        // 항로
        l.add(Spec("h_route", "항로", "", null, false, null, true))
        for (p in Data.ports) {
            val u = Data.unlock(p.id)
            val open = p.id == "port_haesol" || Game.has(p.id)
            val info = "나무 " + fmt(p.buyW) + " · 돌 " + fmt(p.buyS) + "금화 · 해적 " + (p.risk * 100).toInt() + "% · 거리 " + fmt(p.distance)
            when {
                open && Game.routeOk(p) -> l.add(Spec("r_" + p.id, p.name, info, if (Game.route == p.id) "지금 항로" else "고르기",
                    Game.route != p.id, { if (Game.setRoute(p.id)) toast(p.name + "로 다음 출항부터 가요") }))
                open -> l.add(Spec("r_" + p.id, p.name, info + "\n배 " + p.minShip + "단계부터 갈 수 있어요", null, false, null))
                u != null && Game.unlockReady(u) -> l.add(Spec("r_" + p.id, p.name + " 열기", u.cost!!.text() + "\n" + p.look, "열기",
                    Game.canPay(u.cost), { if (Game.buyUnlock(p.id)) toast(p.name + " 항로가 열렸어요") }))
                u != null && Game.hasAll(u.requires.filter { !it.startsWith("main_") }) -> l.add(Spec("r_" + p.id, p.name, "열리려면: " + need(u.requires) +
                    (if (Game.warehouse < u.needWarehouse) ", 창고 " + u.needWarehouse + "단계" else ""), null, false, null))
            }
        }

        // 배
        l.add(Spec("h_ship", "배", "", null, false, null, true))
        l.add(Spec("ship", s.name, "짐 나무 " + s.cargoW + " · 돌 " + s.cargoS + " · 속도 " + fmt(s.speed) + " · 해적 대응 " + (s.pirateWin * 100).toInt() + "%", null, false, null))
        Data.unlock("ship_" + (Game.shipLevel + 1))?.let { u ->
            val ns = Data.ship(Game.shipLevel + 1)
            if (Game.unlockReady(u)) l.add(Spec("ship_up", ns.name + "(으)로 고치기", u.cost!!.text() + "\n짐 나무 " + ns.cargoW + " · 돌 " + ns.cargoS,
                "고치기", Game.canPay(u.cost), { if (Game.buyUnlock(u.key)) toast("배가 " + ns.name + "이 됐어요") }))
            else l.add(Spec("ship_up", "다음 단계 · " + ns.name, "열리려면: " + need(u.requires) +
                (if (Game.warehouse < u.needWarehouse) ", 창고 " + u.needWarehouse + "단계" else ""), null, false, null))
        }

        // 섬 건물
        l.add(Spec("h_build", "섬 짓기", "", null, false, null, true))
        l.add(Spec("autobuild", "기본 시설 자동 짓기 " + (if (Game.autoBuild) "켜짐" else "꺼짐"), "숙소·창고·부두·밭은 재료가 모이면 알아서 지어요 (금화는 안 써요)",
            if (Game.autoBuild) "끄기" else "켜기", true, { Game.autoBuild = !Game.autoBuild; Game.save() }))
        val h = Game.nextHouse()
        l.add(Spec("house", "알바 숙소 " + Game.houses + "/3", if (h == null) "다 지었어요" else (h.cost?.text() ?: "") + (if (!Game.houseReady()) "\n먼저: " + need(h.requires) else ""),
            if (h == null) null else "짓기", h != null && Game.houseReady() && Game.canPay(h.cost), { if (Game.buildHouse()) toast("숙소를 지었어요") }))
        val w = Game.nextWarehouse()
        l.add(Spec("ware", "창고 " + Game.warehouse + "단계 (각 " + Game.cap() + "개)",
            if (w == null) "가장 큰 창고예요" else "다음: 각 " + w.first + "개 · " + (w.second.cost?.text() ?: "") + (if (!Game.warehouseReady()) "\n먼저: " + need(w.second.requires) else ""),
            if (w == null) null else "넓히기", w != null && Game.warehouseReady() && Game.canPay(w.second.cost), { if (Game.upgradeWarehouse()) toast("창고가 넓어졌어요") }))
        if (!Game.dock) {
            val d = Data.dock
            l.add(Spec("dock", "부두", (d?.cost?.text() ?: "") + " · 짐배가 들르고 큰 배가 오가요" + (if (!Game.dockReady()) "\n먼저: " + need(d?.requires ?: emptyList()) else ""),
                "짓기", Game.dockReady() && Game.canPay(d?.cost), { if (Game.buildDock()) toast("부두가 열렸어요") }))
        }
        if (Game.field) l.add(Spec("field", "작은 밭", "다 지었어요 · " + (Data.field?.effect ?: ""), null, false, null))
        else if (Game.fieldReady()) l.add(Spec("field", "작은 밭", (Data.field?.cost?.text() ?: "") + "\n" + (Data.field?.effect ?: ""), "짓기",
            Game.canPay(Data.field?.cost), { if (Game.buildField()) toast("밭을 일궜어요") }))
        for (key in listOf("lighthouse", "market")) {
            val u = Data.unlock(key) ?: continue
            val b = Data.extraBuildings[key]
            if (Game.has(key)) l.add(Spec(key, b?.name ?: key, "다 지었어요 · " + (b?.effect ?: ""), null, false, null))
            else if (Game.unlockReady(u)) l.add(Spec(key, b?.name ?: key, u.cost!!.text() + "\n" + (b?.effect ?: ""), "짓기", Game.canPay(u.cost),
                { if (Game.buyUnlock(key)) toast((b?.name ?: "") + "을(를) 지었어요") }))
        }

        // 알바
        l.add(Spec("h_work", "알바", "", null, false, null, true))
        if (Game.workers.isEmpty()) l.add(Spec("w_none", "일하는 알바가 없어요", if (Game.houses == 0) "숙소를 지으면 항구에서 알바를 데려와요" else "빈 숙소가 있으면 항구에서 자동으로 구해 와요", null, false, null))
        for (c in Game.workers) {
            val d = Data.worker(c.id) ?: continue
            l.add(Spec("w_" + c.id, d.name + " · " + d.role, d.trait + " · 남은 " + mmss(c.expire - now), null, false, null))
        }
        l.add(Spec("autohire", "자동 고용 " + (if (Game.autoHire) "켜짐" else "꺼짐"),
            "고용비 " + Data.singleLimit + "금화까지, 하루 " + Data.dailyLimit + "금화, 금화 " + Data.reserveGold + "은 남겨 둬요", if (Game.autoHire) "끄기" else "켜기", true,
            { Game.autoHire = !Game.autoHire; Game.save() }))
        l.add(Spec("hireany", "비싼 알바도 고용 " + (if (Game.hireAny) "켜짐" else "꺼짐"), "먼 항구의 알바는 고용비가 더 들어요", if (Game.hireAny) "끄기" else "켜기", true,
            { Game.hireAny = !Game.hireAny; Game.save() }))

        // 그 밖
        l.add(Spec("h_etc", "이야기와 설정", "", null, false, null, true))
        val n = Game.unread()
        l.add(Spec("journal", "항해일지" + if (n > 0) " · 새 글 $n" else "", "열린 이야기를 읽어요", "열기", true, { openJournal() }))
        l.add(Spec("time", "시간대 미리보기", "위 미리보기의 낮·노을·밤을 바꿔 봐요", "바꾸기", true, {
            val opts = arrayOf("지금", "낮", "노을", "밤"); val hs = arrayOf<Float?>(null, 12f, 18.6f, 23f)
            AlertDialog.Builder(this).setItems(opts) { _, i -> preview.scene.hourOverride = hs[i] }.show()
        }))
        l.add(Spec("tut", "처음 안내 다시 보기", "이 게임이 어떻게 돌아가는지", "보기", true, { showTutorial(0) }))
        l.add(Spec("wall", "배경화면으로 설정", "홈 화면 빈 곳을 톡 치면 섬 → 바다 → 항구로 넘어가요", "설정", true, { openWallpaperPicker() }))
        return l
    }

    private fun showTutorial(i: Int) {
        val sc = listOf(
            "주머니 속 작은 섬" to "단·보리·해온이 나무와 돌을 모아요. 홈 화면에서는 섬이 움직이는 모습과 소식만 편하게 봐요.",
            "섬이 알아서 자라요" to "재료가 모이면 숙소·창고·부두를 자동으로 지어요. 직접 고르고 싶으면 앱에서 짓기를 눌러요.",
            "배가 알아서 다녀와요" to "부두가 생기면 배가 짐을 싣고 항구로 떠나 금화를 벌어 와요. 해적은 자동으로 싸워요.",
            "금화로 더 멀리" to "모은 금화로 배를 고치고 새 항구를 열어요. 홈 화면 위쪽 소식판에 다음 목표가 보여요.",
            "소식은 기다려 줘요" to "비운 동안의 보상은 최대 8시간까지 모여요. 이야기는 항해일지에 쌓이고, 읽지 않아도 섬은 계속 움직여요.")
        if (i >= sc.size) return
        AlertDialog.Builder(this).setTitle((i + 1).toString() + "/" + sc.size + "  " + sc[i].first).setMessage(sc[i].second)
            .setPositiveButton(if (i == sc.size - 1) "섬으로 가기" else "다음") { _, _ -> showTutorial(i + 1) }
            .setNegativeButton("건너뛰기", null).show()
    }

    private fun fmt(f: Float) = if (f % 1f == 0f) f.toInt().toString() else "%.2f".format(f).trimEnd('0').trimEnd('.')

    // ---------- 그리기: 줄 구성이 같으면 글자만 바꾸고, 다르면 다시 만든다 ----------
    private fun refresh() {
        val sp = specs()
        val newShape = sp.joinToString("|") { it.key + ":" + (it.btn != null) }
        if (newShape != shape) { shape = newShape; rebuild(sp) }
        for ((i, s) in sp.withIndex()) {
            val r = rows.getOrNull(i) ?: continue
            r.title.text = s.title
            r.text.text = s.text; r.text.visibility = if (s.text.isEmpty()) View.GONE else View.VISIBLE
            r.btn?.let { b -> b.text = s.btn; b.isEnabled = s.enabled; b.setOnClickListener { s.act?.invoke(); refresh() } }
        }
    }

    private fun rebuild(sp: List<Spec>) {
        body.removeAllViews(); rows.clear()
        for (s in sp) {
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val title = TextView(this).apply {
                setTextColor(Color.parseColor(if (s.header) "#f2b544" else "#eef3f8"))
                textSize = if (s.header) 15f else 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            val text = TextView(this).apply { setTextColor(Color.parseColor("#9fb2c8")); textSize = 12f }
            col.addView(title); col.addView(text)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; setGravity(Gravity.CENTER_VERTICAL)
                setPadding(0, ((if (s.header) 14 else 6) * dp).toInt(), 0, (4 * dp).toInt())
            }
            row.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            var btn: Button? = null
            if (s.btn != null) { btn = Button(this); row.addView(btn) }
            body.addView(row)
            rows.add(RowView(title, text, btn))
        }
    }

    // ---------- 항해일지 ----------
    private fun openJournal() {
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
        AlertDialog.Builder(this).setTitle(e.label + " · " + e.title).setView(ScrollView(this).apply { addView(tv) })
            .setPositiveButton("목록") { _, _ -> openJournal() }
            .setNegativeButton("닫기", null).show()
        refresh()
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
                Toast.makeText(this, "설정 > 배경화면에서 '작은 섬'을 골라 주세요", Toast.LENGTH_LONG).show()
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
        preview.running = true; preview.invalidate()
        ui.post(refresher)
    }

    override fun onPause() { super.onPause(); preview.running = false; ui.removeCallbacks(refresher); Game.save() }

    /** 앱 안에서 보는 섬 미리보기. 세 장면을 천천히 번갈아 보여준다. */
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
            // 미리보기는 가로로 넓으니 섬이 화면에 다 들어오도록 아래쪽 중심으로 자른다
            val scale = width.toFloat() / Scene.PAGE
            val viewH = (height / scale).coerceAtMost(Scene.H.toFloat())
            val page = ((now / 5000L) % 3).toInt()
            val target = page * Scene.PAGE.toFloat()
            cam += (target - cam) * 0.08f
            val top = (Scene.OY + 30f - viewH / 2f + 80f).coerceIn(0f, Scene.H - viewH)
            src.set(cam.toInt(), top.toInt(), (cam + Scene.PAGE).toInt(), (top + viewH).toInt())
            dst.set(0, 0, width, height)
            canvas.drawBitmap(scene.bitmap, src, dst, paint)
            if (running) postInvalidateDelayed(66L)
        }
    }
}
