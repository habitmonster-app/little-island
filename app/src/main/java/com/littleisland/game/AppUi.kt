package com.littleisland.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/**
 * 앱 화면 전체를 도트로 그린다 (가로 200도트).
 * 위: 섬·바다 장면, 가운데: 아이콘 탭, 아래: 나무 판넬 속 칸들, 맨 아래: 자원 막대.
 * 칸을 누르면 판넬 위에 설명 창이 뜨고, 거기서 짓기·열기·고르기를 한다.
 */
class AppUi(val scene: Scene, val host: Host) {
    interface Host {
        fun toast(s: String)
        fun openJournal(); fun openMap(); fun openCargo(); fun openTutorial(); fun pickTime(); fun setWallpaper()
        fun openTraining() {}
    }

    /** 좌표는 가로 200도트 그대로 쓰고, 실제 그림은 SC배 크기로 그려서 글자·아이콘이 또렷하게 */
    companion object { const val W = 232; const val SC = 5 }
    var H = 440; private set
    var bitmap: Bitmap = Bitmap.createBitmap(W * SC, H * SC, Bitmap.Config.ARGB_8888); private set
    private var c = Canvas(bitmap).apply { scale(SC.toFloat(), SC.toFloat()) }

    fun resize(h: Int) {
        if (h == H) return
        H = h.coerceAtLeast(360); bitmap = Bitmap.createBitmap(W * SC, H * SC, Bitmap.Config.ARGB_8888); c = Canvas(bitmap).apply { scale(SC.toFloat(), SC.toFloat()) }
    }

    enum class St { READY, NORMAL, DONE, LOCKED, ON, OFF }
    class Tile(val icon: String, val name: String, val sub: String, val st: St, val detail: String,
               val cost: Data.Cost? = null, val btn: String? = null, val act: (() -> Boolean)? = null, val done: String? = null,
               val keep: Boolean = false,
               val btn2: String? = null, val act2: (() -> Boolean)? = null, val done2: String? = null, val go: Int = -1)   // 두 번째 버튼 (선원: 태우기/내리기)   // keep: 버튼을 눌러도 창을 닫지 않고 새 내용으로 (부품 연속 올리기)

    private val tabs = listOf("store" to "짓기", "ship" to "배", "anchor" to "항해", "person" to "알바", "map" to "할 일", "book" to "기록", "gear" to "설정", "skill:9" to "항해술", "part:5" to "강화", "map" to "더보기", "person" to "선원")
    /** 화면에 놓는 탭 순서: 배와 선원이 맨 앞 */
    private val order = intArrayOf(8, 10, 2, 9)
    /** 더보기 안에 들어 있는 탭들 */
    private val SUB = intArrayOf(0, 1, 3, 4, 5, 6, 7)
    var tab = if (Game.dock) 8 else 0
    private var backRect: Rect? = null
    private var popup: Tile? = null
    var fxAt = 0L                        // 올리기 성공 반짝임 시작 시각
    private var after: Tile? = null      // 버튼을 누른 뒤 이어서 띄울 창 (뽑기 결과)
    private var page = 0
    private val tileRects = ArrayList<Pair<Rect, Tile>>()
    private val tabRects = ArrayList<Rect>()
    private var btnRect: Rect? = null
    private var btn2Rect: Rect? = null
    private var goalRect: Rect? = null
    private var pageRects = ArrayList<Pair<Rect, Int>>()
    private val src = Rect(); private val dst = Rect()
    private val bp = Paint().apply { isFilterBitmap = false }
    private val top = 16f
    private val sceneH = 184f
    private val Z = .8f
    private val dstF = android.graphics.RectF()

    // ---------- 칸 목록 ----------
    private fun need(keys: List<String>): String = keys.filter { !Game.has(it) }.joinToString(", ") { k ->
        when {
            k.startsWith("main_") -> "이야기 " + k.removePrefix("main_").toIntOrNull() + "화"
            k.startsWith("port_") -> Data.ports.firstOrNull { it.id == k }?.name + " 개방"
            k.startsWith("ship_") -> "배 " + k.removePrefix("ship_") + "단계"
            k == "store_1" -> "창고 1단계"; k == "house_1" -> "첫 숙소"; k == "first_gather" -> "첫 채집"
            k == "lighthouse" -> "등대"; k == "market" -> "장터"; k == "dock" -> "부두"
            k.startsWith("voyages_") -> "항해 " + k.removePrefix("voyages_") + "번"
            k.startsWith("parts_") -> "부품 4개 모두 Lv" + k.removePrefix("parts_")
            else -> k
        }
    }

    private fun prog(cost: Data.Cost?): Float {
        val cc = cost ?: return 1f
        val l = listOfNotNull(if (cc.wood > 0) Game.wood / cc.wood.toFloat() else null, if (cc.stone > 0) Game.stone / cc.stone.toFloat() else null,
            if (cc.gold > 0) Game.gold / cc.gold.toFloat() else null).map { it.coerceIn(0f, 1f) }
        return if (l.isEmpty()) 1f else l.minOrNull()!!
    }

    private fun buyTile(icon: String, name: String, ready: Boolean, cost: Data.Cost?, detail: String, req: List<String>, btn: String, act: () -> Boolean, done: String) =
        if (!ready) Tile("lock", name, "잠김", St.LOCKED, detail + "\n\n열리려면: " + need(req))
        else Tile(icon, name, if (Game.canPay(cost)) "지금 가능!" else "모으는 중", if (Game.canPay(cost)) St.READY else St.NORMAL, detail, cost, btn, act, done)

    /** 해적 대응 칸: 무엇이 얼마나 도와주는지와 이길 확률 */
    private fun fightTile(): Tile {
        val n = Game.normalOdds(); val b = Game.bossOdds()
        val parts = Game.fightParts().filter { it.second > 0f }.joinToString("\n") { (nm, v) -> nm + "  " + (if (nm.startsWith("배 ")) Fleet.pct(v) else "+" + Fleet.pts(v)) }
        val my = Game.myPower()
        val ep = Game.routePirates().maxByOrNull { it.power }?.let { Game.enemyPower(it) }
        val bp = Game.routeBosses().maxByOrNull { it.power }?.let { Game.enemyPower(it) }
        return Tile("boss:0", "함대 힘", "%,d".format(my), if (n >= .8f) St.ON else St.NORMAL,
            "내 함대 힘  " + "%,d".format(my) + (ep?.let { "\n센 해적 힘  " + "%,d".format(it) } ?: "") + (bp?.let { "\n우두머리 힘  " + "%,d".format(it) } ?: "") +
                "\n힘이 1,000 더 크면 10%p 더 잘 이겨요.\n\n이길 확률\n보통 해적  " + Fleet.pct(n) + (b?.let { "\n우두머리  " + Fleet.pct(it) } ?: "") +
                "\n\n" + parts + "\n\n대포를 올리고, 선원 레벨을 올리면 더 잘 싸워요.")
    }

    private fun renownTile(): Tile? {
        if (Game.shipLevel < 4 && Game.renown == 0) return null
        val ok = Game.renownReady()
        val cap = Fleet.maxLevel(Game.shipLevel)
        return Tile("title:0", "명성 " + Game.renown, if (ok) "새 항해!" else "부품 Lv" + cap, if (ok) St.READY else if (Game.renown > 0) St.ON else St.NORMAL,
            "부품 넷을 다 최고로 올리면 새 항해를 시작할 수 있어요.\n\n내려놓는 것  부품 레벨, 금화\n남는 것  배, 선원, 이야기, 장식\n\n명성 1마다 영원히\n해적 이길 확률 +2%p (함대 힘 +200)\n판매 금화 +10%\n\n지금 명성 " + Game.renown +
                (if (ok) "" else "\n필요: 배 5단계, 부품 모두 Lv" + cap),
            null, if (ok) "새 항해 시작" else null, { Game.doRenown() }, "명성 " + (Game.renown + 1) + "!")
    }

    private fun dailyTiles(l: ArrayList<Tile>) {
        val ar = Game.attReward()
        l.add(Tile("chest:1", "출석 상자", if (Game.attReady()) (Game.attIdx + 1).toString() + "일째 받기!" else "내일 또", if (Game.attReady()) St.READY else St.DONE,
            "하루 한 번 받아요. 7일째가 가장 커요.\n\n" + (0 until 7).joinToString("\n") { i -> (if (i < Game.attIdx) "● " else if (i == Game.attIdx && Game.attReady()) "▶ " else "○ ") + (i + 1) + "일  " + Game.resText(Game.attReward(i)) },
            null, if (Game.attReady()) "받기" else null, { Game.claimAtt() }, "출석 " + Game.resText(ar)))
        Game.dailyBoss()?.let { p ->
            val nm = Fleet.boss(p.id)?.name ?: p.name
            val o = Game.winChance(p, Game.aboard.filter { it in Game.promoted }.take(Game.slots()))
            l.add(Tile("boss:" + (Fleet.boss(p.id)?.face ?: 0), "오늘 우두머리", if (Game.dbReady()) "도전!" else "내일 또", if (Game.dbReady()) St.READY else St.DONE,
                nm + "\n함대 힘 " + "%,d".format(Game.myPower()) + " vs " + "%,d".format(Game.enemyPower(p)) + "\n이길 확률 " + Fleet.pct(o) +
                    "\n\n하루 한 번 도전해요.\n이기면 금화와 쪽지 2장, 져도 쪽지 1장.",
                null, if (Game.dbReady()) "도전" else null, { Game.challengeBoss()?.let { host.toast(it) }; true }))
        }
        val (pts, got) = Game.seasonInfo()
        val se = Game.season()
        val next = Game.SEASON_AT.getOrNull(got)
        l.add(Tile("season:" + (se?.deco?.firstOrNull() ?: 0), (se?.name ?: "") + " 시즌", if (Game.seasonReady()) "받기!" else if (next != null) "$pts/$next" else "다 받음",
            if (Game.seasonReady()) St.READY else if (next == null) St.DONE else St.ON,
            "항해, 해적, 주문, 도감, 출석으로 점수를 모아요.\n계절이 바뀌면 처음부터.\n\n점수 " + pts + "\n" + Game.SEASON_AT.withIndex().joinToString("\n") { (i, at) -> (if (i < got) "● " else "○ ") + at + "점  " + Game.resText(Game.seasonReward(i)) },
            null, if (Game.seasonReady()) "받기" else null, { Game.claimSeason() }, "시즌 보상을 받았어요", keep = true))
    }

    private fun tiles(): List<Tile> {
        val l = ArrayList<Tile>()
        when (tab) {
            9 -> {
                fun go(t: Int, sub: String, ready: Boolean) = Tile(tabs[t].first, tabs[t].second, sub, if (ready) St.READY else St.NORMAL, "", go = t)
                val daily = Game.attReady() || Game.dbReady() || Game.seasonReady()
                l.add(go(4, if (daily) "받을 것 있음" else "하루 보상", daily))
                val pf = Game.promoted.any { Game.ptsFree(it) > 0 }; val tr = (0 until 25).any { Game.treeOpen(it) && Game.gold >= Game.treeCost(it) }
                l.add(go(7, if (tr) "배울 수 있음" else "항해술 나무", tr))
                l.add(go(3, "알바·부적", false))
                l.add(go(1, "단계·꾸미기", false))
                l.add(go(0, "섬 짓기", false))
                val n = Game.unread()
                l.add(go(5, if (n > 0) "새 글 $n" else "도감·일지", n > 0))
                l.add(go(6, "배경화면", false))
            }
            0 -> {
                val h = Game.nextHouse()
                l.add(if (h == null) Tile("house", "숙소 3/3", "다 지음", St.DONE, Fleet.short("buildings", "house", "알바가 머무는 숙소예요."))
                    else buyTile("house", "숙소 " + Game.houses + "/3", Game.houseReady(), h.cost, Fleet.short("buildings", "house", "알바가 머무는 숙소예요."), h.requires, "짓기", { Game.buildHouse() }, "숙소를 지었어요"))
                val w = Game.nextWarehouse()
                l.add(if (w == null) Tile("store", "창고 " + Game.warehouse + "단계", "가장 큼", St.DONE, "나무·돌을 각각 " + Game.cap() + "개까지 담아요.")
                    else buyTile("store", "창고 " + Game.warehouse + "단계", Game.warehouseReady(), w.second.cost,
                        "지금 각 " + Game.cap() + "개 → 넓히면 각 " + w.first + "개까지 담아요.", w.second.requires, "넓히기", { Game.upgradeWarehouse() }, "창고가 넓어졌어요"))
                l.add(if (Game.dock) Tile("dock", "부두", "다 지음", St.DONE, Fleet.short("buildings", "dock", "짐배가 들르고 큰 배가 항구로 오가요."))
                    else buyTile("dock", "부두", Game.dockReady(), Data.dock?.cost, Fleet.short("buildings", "dock", "부두가 생기면 배가 항구로 떠나요."), Data.dock?.requires ?: emptyList(), "짓기", { Game.buildDock() }, "부두가 열렸어요"))
                val fd = Data.field
                l.add(if (Game.field) Tile("field", "작은 밭", "다 지음", St.DONE, Fleet.short("buildings", "field", fd?.effect ?: ""))
                    else buyTile("field", "작은 밭", Game.fieldReady(), fd?.cost, fd?.effect ?: "", fd?.requires ?: emptyList(), "짓기", { Game.buildField() }, "밭을 일궜어요"))
                for (key in listOf("lighthouse", "market")) {
                    val u = Data.unlock(key) ?: continue
                    val b = Data.extraBuildings[key]
                    val nm = b?.name ?: if (key == "lighthouse") "등대" else "장터"
                    l.add(if (Game.has(key)) Tile(key, nm, "다 지음", St.DONE, b?.effect ?: "")
                        else buyTile(key, nm, Game.unlockReady(u), u.cost, b?.effect ?: "", u.requires, "짓기", { Game.buyUnlock(key) }, nm + " 완성!"))
                }
            }
            1 -> {
                l.add(fightTile())
                renownTile()?.let { l.add(it) }
                for (p in Fleet.parts) {
                    val lv = Game.partLevel(p.id); val n = Game.partNext(p.id)
                    val now = if (lv > 0) Fleet.partText(p, Game.partValue(p.id)) else "아직 없음"
                    val cap = Fleet.maxLevel(Game.shipLevel)
                    val head = Fleet.short("parts", p.id, p.desc) + "\n\n지금  " + now
                    val top = p.levels.size
                    // 키는 Lv20에서 효과가 이미 최대. 그 뒤는 레벨과 배 모습만 오른다
                    val maxed = lv > 0 && p.levels.getOrNull(lv)?.let { it.value <= p.levels[lv - 1].value } == true
                    val note = if (maxed) "\n(효과는 최대. 모습만 바뀌어요)" else ""
                    val odds = if (p.id == "cannon" && n != null) { val d = n.value - Game.partValue(p.id); val o = Game.normalOdds(); val bo = Game.bossOdds()
                        "\n\n이길 확률 (올리면)\n보통 해적  " + pct1(o) + " → " + pct1((o + d).coerceAtMost(Data.winMax)) +
                            (bo?.let { "\n우두머리  " + pct1(it) + " → " + pct1((it + d).coerceAtMost(Data.winMax)) } ?: "") } else ""
                    val stN = Game.stars(p.id); val stS = if (stN > 0) "★$stN" else ""
                    l.add(if (n == null && Game.starReady(p.id)) { val sc = Game.starCost(p.id)
                        Tile("part:" + p.icon, p.name + " Lv" + lv + stS, if (Game.canPay(sc)) "별 강화!" else "★ " + stN + "/" + Game.STAR_MAX,
                            if (Game.canPay(sc)) St.READY else St.NORMAL,
                            head + "\n\n별 강화 " + stN + "/" + Game.STAR_MAX + " · 별 하나마다 효과 +10%\n다음  " + Fleet.partText(p, Game.partValue(p.id) / (1 + .1f * stN) * (1 + .1f * (stN + 1))) +
                                (if (lv < top) "\n\n배를 다음 단계로 고치면 레벨도 더 올려요." else ""),
                            sc, "★" + (stN + 1) + " 강화", { Game.buyStar(p.id) }, p.name + " ★" + (stN + 1) + "!", keep = true) }
                        else if (n == null) Tile("part:" + p.icon, p.name + " Lv" + lv + stS, if (lv >= top) "최고" else "배 키우면 더", St.DONE,
                            head + "\n\n" + (if (lv < top) "배를 다음 단계로 고치면 더 올려요." else "가장 높은 레벨이에요."))
                        else Tile("part:" + p.icon, p.name + " Lv" + lv + stS, if (Game.canPay(n.cost)) "올리기!" else Fleet.partText(p, Game.partValue(p.id)),
                            if (Game.canPay(n.cost)) St.READY else St.NORMAL, head + "\n다음  " + Fleet.partText(p, n.value) + note + "\n(최대 Lv" + cap + ")" + odds,
                            n.cost, "Lv" + (lv + 1) + "로 올리기", { Game.buyPart(p.id) }, p.name + " Lv" + (lv + 1) + "!", keep = true))
                }
                for (lv in 1..Data.ships.size) {
                    val s = Data.ship(lv)
                    val info = "짐 나무 " + s.cargoW + " · 돌 " + s.cargoS + "\n속도 " + fmt(s.speed) + " · 해적 대응 " + (s.pirateWin * 100).toInt() + "%"
                    when {
                        lv <= Game.shipLevel -> l.add(Tile("ship", lv.toString() + "단계", if (lv == Game.shipLevel) "지금 배" else "지나옴", St.DONE, s.name + "\n" + info))
                        else -> {
                            val u = Data.unlock("ship_$lv") ?: continue
                            val pn = Fleet.needForShip(lv)
                            l.add(if (lv == Game.shipLevel + 1) buyTile("ship", lv.toString() + "단계", Game.unlockReady(u), u.cost, s.name + "\n" + info + "\n선원 자리 " + Fleet.slots(lv) + "칸", u.requires +
                                (if (Game.warehouse < u.needWarehouse) listOf("store_" + u.needWarehouse) else emptyList()) + (if (pn > 0) listOf("parts_$pn") else emptyList()),
                                "고치기", { Game.buyUnlock(u.key) }, "배가 " + s.name + "이(가) 됐어요")
                                else Tile("lock", lv.toString() + "단계", "잠김", St.LOCKED, s.name + "\n" + info + "\n\n앞 단계를 먼저 고쳐요"))
                        }
                    }
                }
                l.add(Tile("auto", "자동 출항", if (Game.autoSail) "켜짐" else "꺼짐", if (Game.autoSail) St.ON else St.OFF,
                    "나무가 모이면 배가 바로 떠나요.", null, if (Game.autoSail) "끄기" else "켜기", { Game.autoSail = !Game.autoSail; Game.save(); true }))
                if (!Game.autoSail) l.add(Tile("ship", "지금 출항", if (Game.canSail()) "가능" else "대기", if (Game.canSail()) St.READY else St.NORMAL,
                    "나무 " + Game.LOAD_W + " · 돌 " + Game.LOAD_S + "까지 싣고 떠나요.", null, "출항", { Game.sail() }, "배가 떠났어요"))
                l.add(Tile("ship", "배 상태", "짐 " + Game.LOAD_W + "/" + Game.LOAD_S, St.NORMAL,
                    Game.ship().name + "\n짐: 나무 " + Game.LOAD_W + " · 돌 " + Game.LOAD_S + "\n항해 시간 -" + Fleet.pct(Game.speedCut()) +
                    " (" + (Game.voyageSeconds() / 60).toInt() + "분)\n선원 " + Game.aboard.size + "/" + Game.slots() + "명 탑승"))
                // 선원 특별 항해: 승진한 선원마다 아직 못 한 다음 임무 하나
                for (cr in Fleet.crew) {
                    if (cr.id !in Game.promoted) continue
                    val m = Fleet.missions.firstOrNull { it.crew == cr.id && it.id !in Game.missionsDone } ?: continue
                    val face = (if (Game.crewLevel(cr.id) >= 11) "face15:" else "face:") + cr.portrait
                    val port = Data.ports.firstOrNull { it.id == m.port }?.name ?: m.port
                    val rw = listOfNotNull(m.gold.takeIf { it > 0 }?.let { "금화 $it" }, m.wood.takeIf { it > 0 }?.let { "나무 $it" },
                        m.stone.takeIf { it > 0 }?.let { "돌 $it" }, m.maps.takeIf { it > 0 }?.let { "지도 조각 $it" }, m.xp.takeIf { it > 0 }?.let { cr.name + " 경험치 $it" })
                    val info = Fleet.short("missions", m.id, m.summary) + "\n\n가는 곳  " + port + (if (m.mustWin) "\n해적을 만나면 이겨야 성공" else "") + "\n보상  " + rw.joinToString(", ") + " + 이야기"
                    val miss = Game.missionMissing(m)
                    l.add(when {
                        Game.voy?.mission == m.id -> Tile(face, m.title, "항해 중", St.ON, info)
                        Game.missionQ == m.id -> Tile(face, m.title, "다음 출항", St.ON, info + "\n\n다음 출항 한 번만 다녀와요.",
                            null, "취소", { Game.queueMission(null) }, "특별 항해를 취소했어요")
                        miss.isEmpty() -> Tile(face, m.title, "특별 항해!", St.READY, info, null, "예약하기", { Game.queueMission(m.id) }, m.title + " · 다음 출항에 떠나요")
                        else -> Tile(face, m.title, "준비 중", St.LOCKED, info + "\n\n필요: " + miss.joinToString(", "))
                    })
                }
                // 배 꾸미기: 자리마다 한 칸. 누르면 가진 장식을 차례로 바꿔 달기 (모양만 바뀌고 능력치는 그대로)
                for (slot in Fleet.SLOTS) {
                    val all = Fleet.decos.filter { it.slot == slot }
                    val own = all.filter { it.id in Game.decoOwned }
                    val cur = Game.decoOn[slot]?.let { Fleet.deco(it) }
                    val list = all.joinToString("\n") { dc -> (if (dc.id in Game.decoOwned) "● " else "○ ") + dc.name + " · " + dc.howText +
                        (if (!dc.auto) " (금화 " + dc.gold + ")" else "") }
                    val next = if (own.isEmpty()) null else { val k = own.indexOfFirst { it.id == cur?.id }; if (k + 1 < own.size) own[k + 1] else null }
                    val info = "지금  " + (cur?.name ?: "비어 있음") + (next?.let { "\n다음  " + it.name } ?: "") + "\n\n모은 장식 " + own.size + "/" + all.size + " (모양만 바뀌어요)" +
                        (all.firstOrNull { it.id !in Game.decoOwned }?.let { "\n얻는 법 예: " + it.howText } ?: "")
                    l.add(if (own.isEmpty()) Tile("lock", "꾸미기 " + Fleet.slotName(slot), "아직 없음", St.LOCKED, info)
                        else Tile(cur?.iconKey ?: own[0].iconKey, Fleet.slotName(slot) + " " + own.size + "/" + all.size,
                            cur?.name ?: "비어 있음", if (cur != null) St.ON else St.NORMAL, info, null,
                            if (next != null) "바꿔 달기" else "떼기",
                            { Game.setDeco(slot, next?.id) }, next?.let { it.name + "을(를) 달았어요" } ?: Fleet.slotName(slot) + " 장식을 뗐어요"))
                    for (dc in all) if (!dc.auto && dc.id !in Game.decoOwned && Game.decoReady(dc))
                        l.add(Tile(dc.iconKey, dc.name, "금화 " + dc.gold, if (Game.gold >= dc.gold) St.READY else St.NORMAL,
                            Fleet.short("decos", dc.id, dc.desc) + "\n(모양만 바뀌어요)", Data.Cost(dc.gold, 0, 0), "사기", { Game.buyDeco(dc.id) }, dc.name + "을(를) 달았어요"))
                }
            }
            2 -> {
                for (p in Data.ports) {
                    val u = Data.unlock(p.id)
                    val open = p.id == "port_haesol" || Game.has(p.id)
                    val sv = Fleet.souvenirs.filter { it.port == p.id }
                    val info = p.name + "\n나무 " + fmt(p.buyW) + " · 돌 " + fmt(p.buyS) + "금화에 사요\n해적 " + (p.risk * 100).toInt() + "% · 거리 " + fmt(p.distance) + "\n" + p.look +
                        (if (sv.isNotEmpty()) "\n\n기념품 " + sv.count { it.id in Game.souvOwned } + "/" + sv.size else "")
                    l.add(when {
                        open && Game.route == p.id -> Tile("port:" + Data.ports.indexOf(p), p.name, "지금 항로", St.ON, info)
                        open && Game.routeOk(p) -> Tile("port:" + Data.ports.indexOf(p), p.name, "고르기", St.NORMAL, info, null, "이 항로로", { Game.setRoute(p.id) }, p.name + "로 다음 출항부터 가요")
                        open -> Tile("lock", p.name, "배 " + p.minShip + "단계", St.LOCKED, info + "\n\n배 " + p.minShip + "단계부터 갈 수 있어요")
                        u != null -> buyTile("anchor", p.name, Game.unlockReady(u), u.cost, info, u.requires, "항로 열기", { Game.buyUnlock(p.id) }, p.name + " 항로가 열렸어요")
                        else -> Tile("lock", p.name, "잠김", St.LOCKED, info)
                    })
                }
            }
            3 -> {
                val now = System.currentTimeMillis()
                l.add(fightTile())
                // 배에 탄 선원 → 대기 선원 → 승진할 수 있는 알바
                val order = Game.aboard + Game.promoted.filter { it !in Game.aboard }
                for (id in order) {
                    val c = Fleet.crewOf(id) ?: continue
                    val on = id in Game.aboard
                    val lv = Game.crewLevel(id)
                    val next = Fleet.xpTo.getOrNull(lv - 1)
                    val learned = c.skills.filter { it.level <= lv }; val nx = c.skills.firstOrNull { it.level > lv }
                    // 배운 스킬은 효과별로 합쳐서 짧게, 다음 스킬은 한 줄만
                    val skills = "스킬 " + learned.size + "/" + c.skills.size + "\n" + learned.groupBy { it.effect }.entries.joinToString("\n") { (e, l) -> "● " + eff(e, l.sumOf { it.value.toDouble() }.toFloat()) } +
                        (nx?.let { "\n○ Lv" + it.level + " " + it.name } ?: "")
                    val syn = Fleet.synergies.filter { it.a == id || it.b == id }.joinToString("\n") { s ->
                        val other = Fleet.crewOf(if (s.a == id) s.b else s.a)?.name ?: ""
                        "짝 " + other + ": " + eff(s.effect, s.value) }
                    val pi = Data.workers.indexOfFirst { it.id == id }
                    val full = !on && Game.aboard.size >= Game.slots()
                    val tc = Game.teachCost(id)
                    val canT = tc != null && Game.gold >= tc
                    l.add(Tile((if (lv >= 11) "face15:" else "face:") + pi, c.name + " Lv" + lv, if (canT) "가르치기!" else if (on) "탑승" else "대기", if (canT) St.READY else if (on) St.ON else St.NORMAL,
                        c.role + " · " + Fleet.short("crew_roles", id, c.roleDesc) + "\n경험치 " + (if (next != null) Game.crewXp(id).toString() + "/" + next else "최고") + "\n\n" + skills +
                            (if (syn.isNotEmpty()) "\n\n$syn" else "") + (Game.charmOn[id]?.let { Fleet.charm(it) }?.let { "\n\n부적: " + it.name + " (" + eff(it.effect, Game.charmValue(it, id)) + ")" } ?: "") + (if (full) "\n\n자리가 꽉 찼어요. 다른 선원을 내리면 태울 수 있어요." else ""),
                        tc?.let { Data.Cost(it, 0, 0) }, if (tc != null) "Lv" + (lv + 1) + "로 가르치기" else null, { Game.teach(id) }, c.name + " Lv" + (lv + 1) + "!", keep = true,
                        btn2 = if (on) "내리기" else if (full) null else "태우기", act2 = { Game.toggleAboard(id) }, done2 = if (on) c.name + " 다음 항해는 쉬어요" else c.name + " 다음 항해에 타요"))
                }
                for (w in Data.workers) {
                    if (w.id in Game.promoted || !Game.canPromote(w.id)) continue
                    val c = Fleet.crewOf(w.id) ?: continue
                    val pi = Data.workers.indexOf(w)
                    l.add(Tile("face:$pi", w.name, "선원 승진!", St.READY,
                        c.role + " · " + Fleet.short("crew_roles", w.id, c.roleDesc) + "\n선원이 되면 항해할수록 자라요.\n\n" +
                            c.skills.joinToString("\n") { "Lv" + it.level + " " + it.name + " (" + eff(it.effect, it.value) + ")" },
                        Data.Cost(Fleet.promoteGold, 0, 0), "선원으로", { Game.promote(w.id) }, w.name + "이(가) 선원이 됐어요"))
                }
                if (Game.promoted.isEmpty()) l.add(Tile("part:4", "선원 되는 법", "자리 " + Game.slots() + "칸", St.NORMAL,
                    "알바가 섬에서 일을 " + Fleet.promoteContracts + "번 마치면 금화 " + Fleet.promoteGold + "으로 선원이 될 수 있어요. 선원은 배에 타고 항해할 때마다 경험치를 얻어 Lv" + Fleet.maxCrewLevel + "까지 자라고, 레벨마다 새 스킬이 열려요. 짝이 맞는 둘을 같이 태우면 보너스가 더 붙어요."))
                for (i in 0 until 3) {
                    val wk = Game.workers.getOrNull(i)
                    l.add(when {
                        wk != null -> { val d = Data.worker(wk.id); val left = Fleet.promoteContracts - Game.completedOf(wk.id)
                            val cr = Fleet.crewOf(wk.id) != null && wk.id !in Game.promoted
                            Tile("person", d?.name ?: "알바", if (cr && left > 0) "선원까지 " + left + "번" else mmss(wk.expire - now), St.DONE,
                            (d?.name ?: "") + " · " + (d?.role ?: "") + "\n" + (d?.trait ?: "") + "\n" + (d?.look ?: "") + "\n\n남은 시간 " + mmss(wk.expire - now) +
                                (if (cr) "\n일을 " + Fleet.promoteContracts + "번 마치면 선원이 될 수 있어요. 지금 " + Game.completedOf(wk.id) + "번" else "")) }
                        i < Game.houses -> Tile("house", "빈 숙소", "구하는 중", St.NORMAL, "배가 항구에 가면 빈 숙소만큼 알바를 데려와요.")
                        else -> Tile("lock", "숙소 없음", "짓기 탭", St.LOCKED, "숙소를 지으면 알바가 묵을 수 있어요.")
                    })
                }
                // 부적 뽑기 (게임 안 쪽지·금화로만), 훈련 퍼즐, 가진 부적
                if (Fleet.charms.isNotEmpty()) {
                    val rates = "일반 70 · 희귀 25 · 영웅 5%\n영웅 확정까지 " + (Fleet.pityAt - Game.pity) + "번\n같은 부적이 나오면 레벨 업"
                    fun result(gold: Boolean): Tile? {
                        val r = Game.lastDraw ?: return null
                        val ch = r.charm
                        return Tile("charm:" + ch.icon, ch.name + " Lv" + r.level, when { r.refund > 0 -> "쪽지 +" + r.refund; r.isNew -> "새 부적!"; else -> "한 단계 업!" },
                            St.READY, Fleet.rarityName(ch.rarity) + " 부적" + (if (r.forced) " (확정)" else "") + "\n" + Fleet.short("charms", ch.id, ch.desc) + "\n\n효과: " + eff(ch.effect, Game.charmValue(ch, null)) +
                                "\n" + ch.role + "이(가) 달면 1.5배",
                            null, if (Game.canDraw(gold)) "한 번 더" else null, { (Game.draw(gold) != null).also { if (it) after = result(gold) } })
                    }
                    l.add(Tile("chest:0", "부적 뽑기", "쪽지 " + Game.tickets + "장", if (Game.canDraw(false)) St.READY else St.NORMAL,
                        "쪽지 " + Fleet.gachaTickets + "장 = 부적 1개\n쪽지는 항해와 훈련으로 받아요.\n\n" + rates,
                        null, if (Game.canDraw(false)) "쪽지 " + Fleet.gachaTickets + "장으로 뽑기" else null, { (Game.draw(false) != null).also { if (it) after = result(false) } }))
                    l.add(Tile("chest:2", "금화로 뽑기", "금화 " + Fleet.gachaGold, if (Game.canDraw(true)) St.NORMAL else St.LOCKED,
                        "금화 " + Fleet.gachaGold + " = 부적 1개\n\n" + rates,
                        null, if (Game.canDraw(true)) "금화로 뽑기" else null, { (Game.draw(true) != null).also { if (it) after = result(true) } }))
                    Game.trainCheckDay()
                    l.add(Tile("map", "훈련 퍼즐", "오늘 " + Game.trainLeft + "번", if (Game.trainLeft > 0) St.READY else St.DONE,
                        "상자를 바꿔서 같은 짐끼리 세로로 모아요.\n" + Fleet.trainSeconds + "초 · " + Fleet.trainMaxSwaps + "번까지 · 하루 " + Fleet.trainPerDay + "번\n빨리 풀면 쪽지 최대 3장",
                        null, if (Game.trainLeft > 0) "도전" else null, { host.openTraining(); true }))
                    for (ch in Fleet.charms) {
                        val lv = Game.charmLv[ch.id] ?: continue
                        val who = Game.charmOn.entries.firstOrNull { it.value == ch.id }?.key
                        val crewList = Game.promoted.toList()
                        val nextWho = if (crewList.isEmpty()) null else { val k = crewList.indexOf(who); if (k + 1 < crewList.size) crewList[k + 1] else null }
                        l.add(Tile("charm:" + ch.icon, ch.name + " Lv" + lv, who?.let { Fleet.crewOf(it)?.name } ?: "보관 중", if (who != null) St.ON else St.NORMAL,
                            Fleet.rarityName(ch.rarity) + " · Lv" + lv + "/" + ch.values.size + "\n" + Fleet.short("charms", ch.id, "") + "\n효과  " + eff(ch.effect, Game.charmValue(ch, null)) +
                                "\n" + ch.role + "이(가) 달면 1.5배\n\n지금  " + (who?.let { Fleet.crewOf(it)?.name ?: "" } ?: "없음") +
                                (nextWho?.let { "\n다음  " + (Fleet.crewOf(it)?.name ?: "") } ?: ""),
                            null, if (crewList.isEmpty()) null else if (nextWho != null) "바꿔 달기" else "떼기", { Game.equipCharm(ch.id, nextWho) },
                            nextWho?.let { ch.name + " → " + (Fleet.crewOf(it)?.name ?: "") } ?: ch.name + "을(를) 뗐어요"))
                    }
                }
                l.add(Tile("auto", "자동 고용", if (Game.autoHire) "켜짐" else "꺼짐", if (Game.autoHire) St.ON else St.OFF,
                    "고용비 " + Data.singleLimit + "금화까지, 하루 " + Data.dailyLimit + "금화, 금화 " + Data.reserveGold + "은 남겨 둬요.", null,
                    if (Game.autoHire) "끄기" else "켜기", { Game.autoHire = !Game.autoHire; Game.save(); true }))
                l.add(Tile("gold", "비싼 알바", if (Game.hireAny) "켜짐" else "꺼짐", if (Game.hireAny) St.ON else St.OFF,
                    "먼 항구의 알바는 고용비가 더 들지만 솜씨가 좋아요.", null, if (Game.hireAny) "끄기" else "켜기", { Game.hireAny = !Game.hireAny; Game.save(); true }))
            }
            4 -> {
                dailyTiles(l)
                // 손님 배 (있을 때만)
                Game.visitor()?.let { v ->
                    val o = Game.visOfferNow()
                    val left = ((Game.visUntil - System.currentTimeMillis()) / 60000L + 1).coerceAtLeast(1)
                    l.add(Tile("visitor:" + v.face, v.name, if (Game.visDone) "거래함" else left.toString() + "분 머묾", if (Game.visDone) St.DONE else if (Game.canTrade()) St.READY else St.NORMAL,
                        "“" + v.hello + "”\n\n" + (o?.short ?: "") + "\n줄 것  " + (o?.let { Game.resText(it.give) } ?: "") + "\n받을 것  " + (o?.let { Game.resText(it.get) } ?: "") +
                            (if (Game.visDone) "\n\n“" + v.bye + "”" else ""), null, if (Game.canTrade()) "거래" else null, { Game.trade(); true }, "거래했어요"))
                }
                // 항구 의뢰: 받은 것 → 제시 중
                Game.reqRefresh()
                for ((id, got) in Game.reqAcc) {
                    val r = Fleet.req(id) ?: continue
                    val pn = Data.port(r.port).name
                    val need = listOfNotNull(if (r.needW > 0) "나무 " + got[0] + "/" + r.needW else null, if (r.needS > 0) "돌 " + got[1] + "/" + r.needS else null).joinToString(" · ")
                    l.add(Tile("port:" + Data.ports.indexOfFirst { it.id == r.port }.coerceAtLeast(0), r.title, need, St.ON, r.client + " · " + pn + "\n" + r.short + "\n\n" + need + "\n보상  금화 " + r.gold + (if (r.tickets > 0) " · 쪽지 " + r.tickets else "") +
                        "\n\n" + pn + "로 항해하면 실은 짐을 넘겨요.", null, "포기", { Game.cancelReq(id) }, "의뢰를 접었어요"))
                }
                for (id in Game.reqOffer.toList()) {
                    val r = Fleet.req(id) ?: continue
                    val pn = Data.port(r.port).name
                    val need = listOfNotNull(if (r.needW > 0) "나무 " + r.needW else null, if (r.needS > 0) "돌 " + r.needS else null).joinToString(" · ")
                    l.add(Tile("port:" + Data.ports.indexOfFirst { it.id == r.port }.coerceAtLeast(0), r.title, pn, St.NORMAL, r.client + " · " + pn + "\n" + r.short + "\n\n필요  " + need + "\n보상  금화 " + r.gold + (if (r.tickets > 0) " · 쪽지 " + r.tickets else "") +
                        "\n\n받으면 " + pn + " 항해 때 짐을 넘겨요.", null, "받기", { Game.acceptReq(id) }, "의뢰를 받았어요"))
                }
                // 오늘의 항구 주문 3개 + 주간 상자
                Game.ordCheckDay()
                for (id in Game.ordIds) {
                    val o = Fleet.order(id) ?: continue
                    val done = id in Game.ordDone; val pr = Game.ordProg[id] ?: 0
                    val rw = listOfNotNull(o.tickets.takeIf { it > 0 }?.let { "쪽지 $it" }, o.gold.takeIf { it > 0 }?.let { "금화 $it" }).joinToString(" · ")
                    l.add(Tile("daily:" + o.icon, o.title, if (done) "완료" else "$pr/" + o.target, if (done) St.DONE else if (pr > 0) St.ON else St.NORMAL,
                        o.short + "\n\n진행  $pr/" + o.target + "\n보상  " + rw + (if (done) "\n\n받았어요!" else "\n\n다 하면 바로 받아요. 내일 새 주문이 와요.")))
                }
                if (Game.ordIds.isNotEmpty()) l.add(Tile("daily:1", "주간 상자", if (Game.weekGot) "받음" else Game.weekCount.toString() + "/" + Fleet.weeklyGoal,
                    if (Game.weekGot) St.DONE else St.NORMAL, "7일 동안 주문을 " + Fleet.weeklyGoal + "개 끝내면 열려요.\n\n보상  쪽지 " + Fleet.weeklyTickets + " · 금화 " + Fleet.weeklyGold +
                        "\n남은 날  " + (7 - (Game.today() - Game.weekStart)).coerceIn(1, 7) + "일"))
                val mp = Game.mapPieces
                l.add(Tile("map", "보물 지도", "조각 $mp/" + Game.MAP_NEED, when { Game.treasurePending -> St.DONE; mp >= Game.MAP_NEED -> St.READY; else -> St.NORMAL },
                    when { Game.treasurePending -> "지도를 맞췄어요. 다음 항해에 보물섬에 들러요."
                           else -> "홈 화면에 떠다니는 유리병·보물상자를 톡 쳐서 조각을 모아요. " + Game.MAP_NEED + "개면 밀기 퍼즐로 지도를 맞춰요." },
                    null, if (mp >= Game.MAP_NEED && !Game.treasurePending) "맞추기" else null, { host.openMap(); false }))
                l.add(Tile("chest", "짐 정리", if (Game.packed) "정리 끝" else "+" + (Game.PACK_BONUS * 100).toInt() + "%", if (Game.packed) St.DONE else St.READY,
                    "짐칸을 상자로 빈틈없이 채우면 다음 항구 판매금이 " + (Game.PACK_BONUS * 100).toInt() + "% 늘어요.", null,
                    if (Game.packed) null else "정리하기", { host.openCargo(); false }))
            }
            5 -> {
                val n = Game.unread()
                l.add(Tile("book", "항해일지", if (n > 0) "새 글 $n" else "읽기", if (n > 0) St.READY else St.NORMAL, "섬과 바다의 이야기가 쌓여요.", null, "열기", { host.openJournal(); false }))
                // 모으는 것들: 바다 동물 도감, 칭호, 계절, 바다 소식, 날씨, 항해 도감, 우두머리
                // 바다 동물 도감
                if (Fleet.creatures.isNotEmpty()) {
                    val seen = Fleet.creatures.filter { it.kind in Game.critSeen }
                    val list = Fleet.creatures.chunked(2).joinToString("\n") { r -> r.joinToString("   ") { c -> (if (c.kind in Game.critSeen) "●" else "○") + c.name } }
                    val nm = Fleet.bookMiles.firstOrNull { it.count > seen.size }
                    l.add(Tile("crit:" + (seen.lastOrNull()?.icon ?: 0), "바다 동물 도감", seen.size.toString() + "/" + Fleet.creatures.size, if (seen.isNotEmpty()) St.ON else St.NORMAL,
                        "홈 화면 바다에 지나가는 동물을 톡 쳐요.\n처음 보면 보상을 받아요.\n\n" + list + (nm?.let { "\n\n" + it.count + "마리  " + Game.resText(it.reward) + (if (it.titleName.isNotEmpty()) " · 칭호" else "") } ?: "")))
                }
                // 계절과 축제, 선장 칭호
                Game.season()?.let { se ->
                    val fe = Game.festNow()
                    l.add(Tile("season:" + (se.deco.firstOrNull() ?: 0), se.name + " 섬", fe?.let { if (it.second) "축제 받음" else "축제 중" } ?: "계절", if (fe != null) St.ON else St.NORMAL,
                        "“" + se.islandLine + "”\n\n" + se.festName + "  " + se.festDays.let { d -> if (d.size == 4) d[0].toString() + "/" + d[1] + "~" + d[2] + "/" + d[3] else "" } +
                            "\n" + se.festShort + "\n보상  쪽지 " + se.festTickets + " · 금화 " + se.festGold))
                }
                if (Fleet.titles.isNotEmpty()) {
                    Game.checkTitles()
                    val got = Fleet.titles.filter { it.id in Game.titlesGot }
                    val cur = Fleet.title(Game.titleOn)
                    val nextT = if (got.isEmpty()) null else got.getOrNull(got.indexOf(cur) + 1)
                    val locked = Fleet.titles.firstOrNull { it.id !in Game.titlesGot }
                    l.add(Tile("title:" + (cur ?: got.lastOrNull())?.icon.let { it ?: 0 }, "선장 칭호", got.size.toString() + "/" + Fleet.titles.size, if (cur != null) St.ON else St.NORMAL,
                        "지금  " + (cur?.name ?: "없음") + "\n홈 화면 글줄 앞에 붙어요.\n\n" + (locked?.let { "다음  " + it.name + "\n" + it.short } ?: "모든 칭호를 열었어요!"),
                        null, if (got.isEmpty()) null else if (nextT != null) "바꾸기" else "떼기", { Game.setTitle(nextT?.id) }, nextT?.let { "칭호 " + it.name } ?: "칭호를 뗐어요"))
                }
                // 이번 주 바다 소식
                Game.tide()?.let { t ->
                    l.add(Tile("tide:" + t.icon, t.name, "이번 주", St.ON, t.short + "\n\n" + t.news + "\n\n월요일마다 새 소식이 와요."))
                }
                // 지금 항해 날씨
                Game.weather()?.let { w ->
                    val e = when (w.kind) { "speed" -> if (w.value > 0) "빨리 가요" else "조금 느려요"; "cargo" -> "짐을 더 실어요"; "pirate_chance" -> "해적을 덜 만나요"; else -> "평온해요" }
                    l.add(Tile("weather:" + w.icon, "날씨 " + w.name, e, St.ON, "“" + w.crewLine + "”\n\n이번 항해  " + e + "\n날씨는 떠날 때 정해져요."))
                }
                // 항해 도감과 해적 우두머리
                if (Fleet.souvenirs.isNotEmpty()) {
                    val n = Game.souvOwned.size; val tot = Fleet.souvenirs.size
                    val first = Fleet.souvenirs.lastOrNull { it.id in Game.souvOwned }
                    val nm = Fleet.milestones.firstOrNull { it.id !in Game.milesDone && !it.allPorts }
                    val ms = nm?.let { m -> "다음 보상  " + m.count + "개 → 금화 " + m.gold + (m.deco?.let { id -> " + " + (Fleet.deco(id)?.name ?: "장식") } ?: "") } ?: "개수 보상을 다 받았어요"
                    val ports = "항구마다 몇 개 모았는지는 항구 칸에서 봐요."
                    l.add(Tile(first?.let { "souv:" + it.icon } ?: "book", "항해 도감", "$n/$tot", if (n > 0) St.ON else St.NORMAL,
                        "항구에 닿을 때 가끔 기념품을 얻어요.\n\n" + ms + "\n\n" + ports))
                }
                if (Fleet.bosses.isNotEmpty()) {
                    val won = Game.bossWins.size
                    val list = Fleet.bosses.groupBy { it.minShip }.toSortedMap().entries.joinToString("\n") { (lv, bs) -> "배 " + lv + "단계  " + bs.joinToString(" ") { b -> (if (b.id in Game.bossWins) "●" else "○") + b.name } }
                    val last = Fleet.bosses.lastOrNull { it.id in Game.bossWins }
                    l.add(Tile(last?.let { "boss:" + it.face } ?: "boss:0", "해적 우두머리", "$won/" + Fleet.bosses.size, if (won > 0) St.ON else St.NORMAL,
                        "가끔 해적 대신 센 우두머리가 나와요.\n처음 이기면 장식과 이야기를 받아요.\n\n" + list))
                }
            }
            6 -> {
                l.add(Tile("auto", "자동 짓기", if (Game.autoBuild) "켜짐" else "꺼짐", if (Game.autoBuild) St.ON else St.OFF,
                    "숙소·창고·부두·밭은 재료가 모이면 알아서 지어요. 금화는 안 써요.", null, if (Game.autoBuild) "끄기" else "켜기", { Game.autoBuild = !Game.autoBuild; Game.save(); true }))
                l.add(Tile("book", "처음 안내", "다시 보기", St.NORMAL, "이 게임이 어떻게 돌아가는지 다시 봐요.", null, "보기", { host.openTutorial(); false }))
                l.add(Tile("lighthouse", "시간대", "미리보기", St.NORMAL, "위 장면의 낮·노을·밤을 바꿔 봐요.", null, "바꾸기", { host.pickTime(); false }))
                l.add(Tile("house", "배경화면", "설정", St.NORMAL, "홈 화면에 작은 섬을 띄워요.", null, "설정", { host.setWallpaper(); false }))
            }
        }
        return l
    }

    private fun eff(e: String, v: Float) = Fleet.effectName(e) + " " + (if (e == "speed_bonus") "-" else "+") +
        (if (e == "pirate_win_bonus" || e == "event_good_bonus" || e == "treasure_find_bonus") Fleet.pts(v) else Fleet.pct(v))
    private fun pct1(v: Float) = "%.1f".format(v * 100).removeSuffix(".0") + "%"
    private fun fmt(f: Float) = if (f % 1f == 0f) f.toInt().toString() else "%.2f".format(f).trimEnd('0').trimEnd('.')
    private fun mmss(ms: Long): String { val t = (ms / 1000).coerceAtLeast(0); return if (t >= 3600) (t / 3600).toString() + "시간 " + (t % 3600 / 60) + "분" else (t / 60).toString() + "분" }

    // ---------- 그리기 ----------
    fun draw() {
        Pix.clean(true)
        try { drawAll() } finally { Pix.clean(false) }
    }

    private fun drawAll() {
        val k = Pix
        k.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), k.col("#0d0a08"))
        // 위: 장면 (바다 띠 부분을 잘라서)
        // 멀리서 보듯: 장면을 작게(Z배) 그리고, 양옆은 거울처럼 이어 붙인다
        val vh = sceneH / Z; val sy = (Scene.WL - vh * .55f).toInt()
        val sw = Scene.W * Z; val ox = (W - sw) / 2f
        c.save(); c.clipRect(0f, top, W.toFloat(), top + sceneH)
        fun band(x0: Float, flip: Boolean) {
            src.set(0, sy * Scene.SS, Scene.W * Scene.SS, (sy + vh.toInt()) * Scene.SS)
            if (flip) { c.save(); c.scale(-1f, 1f, x0 + sw / 2, 0f) }
            dstF.set(x0, top, x0 + sw, top + sceneH); c.drawBitmap(scene.bitmap, src, dstF, bp)
            if (flip) c.restore()
        }
        band(ox, false)
        // 양옆: 배가 없는 오른쪽 끝을 이어 붙인다 (왼쪽은 그대로 이어서, 오른쪽은 거울로)
        band(ox - sw, false); band(ox + sw, true)
        c.restore()
        label(c, 6f, top + 14)
        drawPower(top + 28)
        // 큰 금화 숫자와 초당 금화
        k.icon(c, "gold", 6f, top + 22, 1.6f)
        k.text(c, big(Game.gold), 25f, top + 36, 16, k.GOLD, Paint.Align.LEFT, true)
        k.text(c, (if (Game.gps > 0.05f) "초당 +" + (if (Game.gps < 10) "%.1f".format(Game.gps) else big(Game.gps.toLong())) + " · " else "") + "바다를 톡톡 눌러요!", 7f, top + 49, 8, k.CREAM, Paint.Align.LEFT, true)
        drawGoal(top + sceneH - 21)
        // 탭
        val ty = top + sceneH + 4
        tabRects.clear()
        for ((i, ti) in order.withIndex()) {
            val t = tabs[ti]
            val st = (W - 6f) / order.size
            val x = 3f + i * st; val bw = st - 2f
            val on = ti == tab || (ti == 9 && tab in SUB)
            k.rect(c, x, ty, bw, 25f, k.INK)
            k.rect(c, x + 1, ty + 1, bw - 2, 23f, if (on) k.col("#c8903a") else k.col("#8a6030"))
            k.rect(c, x + 1, ty + 1, bw - 2, 2f, if (on) k.col("#f0c060") else k.col("#a87444"))
            val tw = 16 + 2 + k.width(t.second, 10) * 1.1f; val ix = (x + (bw - tw) / 2).coerceAtLeast(x + 2)
            k.icon(c, t.first, ix, ty + 5f, 1.6f)
            k.text(c, t.second, ix + 18, ty + 16.5f, 10, if (on) k.CREAM else k.col("#f0dcb0"), Paint.Align.LEFT, true)
            val dot = when (ti) { 8 -> upRows().any { it.can }; 10 -> crewRows().any { it.can }; 9 -> moreDot(); else -> false }
            if (dot) k.rect(c, x + bw - 6, ty - 1, 6f, 6f, if (ti == 9) k.RED else k.GOLD)
            tabRects.add(Rect(x.toInt(), ty.toInt(), (x + bw).toInt(), (ty + 25).toInt()))
        }
        // 판넬
        val py = ty + 30; val ph = H - py - 26
        k.panel(c, 2f, py, W - 4f, ph)
        if (tab != 7 && tab != 8 && tab != 10) k.text(c, tabs[tab].second, W / 2f, py + 17, 12, k.CREAM, Paint.Align.CENTER, true)
        tileRects.clear(); treeRects.clear(); treeBtn = null; slotRects.clear(); modeRects.clear(); statRects.clear(); faceRects.clear(); upRects.clear(); multRects.clear()
        backRect = null
        if (tab in SUB && tab != 7) { k.text(c, "◀ 더보기", 10f, py + 16, 10, k.GOLD, Paint.Align.LEFT, true); backRect = Rect(2, py.toInt(), 70, (py + 22).toInt()) }
        if (tab == 8 || tab == 10) { drawUpgrade(py, ph); drawBar(k); drawFloats(); popup?.let { drawPopup(it) }; return }
        if (tab == 7) {
            // 위의 두 버튼: 선원 능력치 / 항해술 나무
            k.rect(c, 6f, py + 3, W - 12f, 19f, k.col("#3a2614"))
            k.button(c, 8f, py + 4, 22f, 16f, "◀", true); backRect = Rect(2, py.toInt(), 32, (py + 24).toInt())
            for ((m, nm) in listOf("선원 능력치", "항해술 나무").withIndex()) {
                val bx = 34f + m * (W - 42f) / 2; val bw = (W - 42f) / 2 - 2
                k.button(c, bx, py + 4, bw, 16f, nm, skillMode == m)
                modeRects.add(Rect(bx.toInt(), (py + 1).toInt(), (bx + bw).toInt(), (py + 23).toInt()) to m)
            }
            if (skillMode == 0) drawStats(py, ph) else drawTree(py, ph)
            drawBar(k); drawFloats(); popup?.let { drawPopup(it) }; return
        }
        val all = tiles()
        val list = if (tab == 1) all.filter { !it.icon.startsWith("part:") && it.sub != "지나옴" } else all
        // 위아래로 미는 목록: 배 화면 머리도 같이 밀린다
        val areaTop = py + 22; val areaBot = py + ph - 4
        val head = if (tab == 1) 108f else 0f
        val tw = 58f; val th = 60f; val gx = 6f
        val x0 = (W - (tw * 3 + gx * 2)) / 2f
        val rows = (list.size + 2) / 3
        val contentH = head + rows * (th + gx)
        maxScroll = (contentH - (areaBot - areaTop) + 4).coerceAtLeast(0f)
        scrollTo?.let { t -> val i = list.indexOf(t); if (i >= 0) scrollY = head + (i / 3) * (th + gx) - 4 }; scrollTo = null
        scrollY = scrollY.coerceIn(0f, maxScroll)
        c.save(); c.clipRect(2f, areaTop, W - 2f, areaBot)
        val y0 = areaTop + 2 - scrollY
        if (tab == 1) drawShipHead(y0, all, areaTop, areaBot)
        for ((n, t) in list.withIndex()) {
            val x = x0 + (n % 3) * (tw + gx); val y = y0 + head + (n / 3) * (th + gx)
            if (y + th < areaTop || y > areaBot) continue
            drawTile(t, x, y, tw, th)
            tileRects.add(Rect(x.toInt(), maxOf(y, areaTop).toInt(), (x + tw).toInt(), minOf(y + th, areaBot).toInt()) to t)
        }
        c.restore()
        pageRects.clear()
        // 더 있으면 오른쪽에 막대와 아래 화살표
        if (maxScroll > 0) {
            val vh = areaBot - areaTop; val bh = (vh * vh / (contentH + 4)).coerceAtLeast(14f)
            val by = areaTop + (vh - bh) * (scrollY / maxScroll)
            k.rect(c, W - 7f, areaTop, 3f, vh, k.col("#2a1a10")); k.rect(c, W - 7f, by, 3f, bh, k.GOLD)
            if (scrollY < maxScroll - 2) k.text(c, "▼", W - 5.5f, areaBot + 3, 7, k.GOLD, Paint.Align.CENTER, true)
        }
        drawBar(k)
        drawFloats()
        popup?.let { drawPopup(it) }
    }

    private fun drawBar(k: Pix) {
        // 자원 막대
        val by = H - 22f
        k.rect(c, 20f, by, W - 40f, 18f, k.INK); k.rect(c, 21f, by + 1, W - 42f, 16f, k.col("#5d3a1f")); k.rect(c, 21f, by + 1, W - 42f, 1f, k.col("#a87444"))
        val items = listOf("wood" to Game.wood.toString(), "stone" to Game.stone.toString(), "gold" to big(Game.gold), "map" to Game.mapPieces.toString())
        // 금화는 자리를 넓게 (5단계 배는 18만 금화라 숫자가 길다)
        val gap = floatArrayOf(34f, 34f, 60f, 0f)
        var x = 26f
        for ((i, iv) in items.withIndex()) { val (ic, v) = iv; k.icon(c, ic, x, by + 4, 1f); k.text(c, v, x + 12, by + 13, 10, k.GOLD); x += gap[i] }
    }

    // ---------- 강화: 거지키우기처럼 한 줄 목록 하나 ----------
    class URow(val icon: String, val title: String, val sub: String, val cost: Data.Cost?, val can: Boolean, val label: String,
               val act: () -> Boolean, val big: Boolean = false, val btnText: String? = null, val small: Boolean = false, val head: Boolean = false)
    private val upRects = ArrayList<Pair<Rect, URow>>()
    private val multRects = ArrayList<Pair<Rect, Int>>()
    private var mult = 1                       // 1, 10, 0(최대)
    fun upRows(): List<URow> {
        val l = ArrayList<URow>()
        // 다음 배 단계
        val nx = Game.shipLevel + 1
        Data.unlock("ship_$nx")?.takeIf { !Game.has(it.key) && Data.ships.any { s -> s.level == nx } }?.let { u ->
            val need = Fleet.needForShip(nx); val minP = Game.minPart()
            val ready = Game.unlockReady(u)
            l.add(URow("ship", nx.toString() + "단계 배로 키우기", if (minP < need) "부품 모두 Lv$need (지금 $minP)" else Data.ship(nx).name.substringAfter("· "),
                u.cost, ready && Game.canPay(u.cost), "키우기", { Game.buyUnlock(u.key) }, big = true))
        }
        for (p in Fleet.parts) {
            val lv = Game.partLevel(p.id); val n = Game.partNext(p.id)
            val st = Game.stars(p.id); val stS = if (st > 0) " ★$st" else ""
            if (n != null) l.add(URow("part:" + p.icon, p.name + " Lv" + lv + stS, Fleet.partText(p, Game.partValue(p.id)) + " → " + Fleet.partText(p, n.value * (1 + .1f * st)).substringAfterLast(" "),
                n.cost, Game.canPay(n.cost), "+1", { Game.buyPart(p.id) }))
            else if (Game.starReady(p.id)) { val sc = Game.starCost(p.id)
                l.add(URow("part:" + p.icon, p.name + " Lv" + lv + stS, "별 " + st + "/" + Game.STAR_MAX + " · 별마다 효과 +10%", sc, Game.canPay(sc), "★+1", { Game.buyStar(p.id) })) }
            else l.add(URow("part:" + p.icon, p.name + " Lv" + lv + stS, if (lv >= p.levels.size) "가장 높은 레벨" else "배를 키우면 더 올려요", null, false, "최고", { false }))
        }
        return l
    }
    private val STAT_ICON = listOf("skill:6", "skill:1", "skill:0", "skill:3")
    fun crewRows(): List<URow> {
        val l = ArrayList<URow>()
        l.add(URow("", "선원 자리 " + Game.aboard.size + "/" + Game.slots() + " · 배에 탄 선원만 힘을 보태요", "", null, false, "", { false }, head = true))
        for (w in Data.workers) if (Game.canPromote(w.id)) {
            val pi = Data.workers.indexOf(w); val cst = Data.Cost(Fleet.promoteGold, 0, 0)
            l.add(URow("face:$pi", w.name + " 선원으로", "알바 일을 마쳤어요. 선원이 될 수 있어요", cst, Game.canPay(cst), "승진", { Game.promote(w.id) }))
        }
        if (Game.promoted.isEmpty()) l.add(URow("", "알바가 계약을 2번 마치면 선원이 될 수 있어요", "", null, false, "", { false }, head = true))
        for (id in Game.promoted) {
            val pi = Data.workers.indexOfFirst { it.id == id }; val lv = Game.crewLevel(id)
            val tc = Game.teachCost(id); val free = Game.ptsFree(id); val on = id in Game.aboard
            val nm = Fleet.crewOf(id)?.name ?: id
            l.add(URow((if (lv >= 11) "face15:" else "face:") + pi, nm + " Lv" + lv + (if (on) " · 승선" else " · 쉬는 중"),
                if (free > 0) "남은 점수 $free" else "레벨이 오르면 점수 +2",
                tc?.let { Data.Cost(it, 0, 0) }, tc != null && Game.gold >= tc, "+1", { Game.teach(id) }))
            val a = Game.pts(id)
            for (k in 0 until 4) {
                val can = free > 0 && a[k] < Game.STAT_MAX
                l.add(URow(STAT_ICON[k], Game.STAT_NAME[k] + " " + a[k] + "/" + Game.STAT_MAX, listOf("승률", "항해 시간", "짐칸", "판매")[k] + " " + Game.statText(k, a[k]).substringAfterLast(" ") +
                    (if (a[k] < Game.STAT_MAX) " → " + Game.statText(k, a[k] + 1).substringAfterLast(" ") else ""),
                    null, can, "", { Game.statAdd(id, k, 1) }, btnText = if (a[k] >= Game.STAT_MAX) "최고" else "점수 +1", small = true))
            }
            if (!on && Game.aboard.size < Game.slots()) l.add(URow("ship", nm + " 배에 태우기", "자리가 비었어요", null, true, "", { Game.toggleAboard(id) }, btnText = "태우기", small = true))
        }
        return l
    }
    private fun moreDot() = Game.attReady() || Game.dbReady() || Game.seasonReady() || Game.unread() > 0 ||
        false

    private fun buyRow(u: URow) {
        if (!u.can) { host.toast("재료가 모자라요"); return }
        val p0 = Game.myPower()
        val times = if (u.big) 1 else if (mult == 0) 999 else mult
        var n = 0
        while (n < times && u.act()) n++
        if (n == 0) return
        fxAt = System.currentTimeMillis()
        val dp = Game.myPower() - p0
        if (dp > 0) { powFx = dp; powFxAt = fxAt }
        val nm = u.title.substringBefore(" Lv")
        floats.add(Float3(W - 30f, upY[u.title] ?: 300f, if (u.big) "배가 커졌어요!" else nm + " +" + n, fxAt))
    }
    private val upY = HashMap<String, Float>()

    private fun drawUpgrade(py: Float, ph: Float) {
        val k = Pix
        k.text(c, tabs[tab].second, 12f, py + 17, 12, k.CREAM, Paint.Align.LEFT, true)
        // x1 / x10 / 최대
        val ms = listOf(1 to "x1", 10 to "x10", 0 to "최대")
        var bx = W - 10f - 3 * 34f
        for ((m, lb) in ms) { k.button(c, bx, py + 4, 32f, 16f, lb, mult == m); multRects.add(Rect(bx.toInt(), (py + 1).toInt(), (bx + 32).toInt(), (py + 23).toInt()) to m); bx += 34f }
        val rows = if (tab == 10) crewRows() else upRows()
        val areaTop = py + 24; val areaBot = py + ph - 4
        val rh = 34f
        fun hOf(u: URow) = if (u.head) 14f else if (u.big) rh + 8 else if (u.small) rh - 8 else rh - 2
        val contentH = rows.sumOf { (hOf(it) + 4).toDouble() }.toFloat()
        maxScroll = (contentH - (areaBot - areaTop) + 4).coerceAtLeast(0f)
        scrollY = scrollY.coerceIn(0f, maxScroll)
        c.save(); c.clipRect(2f, areaTop, W - 2f, areaBot)
        var y = areaTop + 2 - scrollY
        upY.clear()
        for (u in rows) {
            val h = hOf(u)
            if (u.head) { if (y + h > areaTop && y < areaBot) k.text(c, k.fit(u.title, 8, W - 24f), 10f, y + 10, 8, k.col("#c8b896")); y += h + 4; continue }
            val ix = if (u.small) 18f else 6f
            if (y + h > areaTop && y < areaBot) {
                k.rect(c, ix, y, W - 8f - ix, h, k.INK)
                k.rect(c, ix + 1, y + 1, W - 10f - ix, h - 2, if (u.big) k.col("#4a3018") else if (u.small) k.col("#2e1e10") else k.col("#3a2614"))
                if (u.can) k.rect(c, ix + 1, y + 1, 2f, h - 2, k.GOLD)
                k.slot(c, ix + 4, y + 3, h - 6, h - 6, false, false)
                k.icon(c, u.icon, ix + 4 + (h - 6) / 2 - 10 * (h - 6) / 32f, y + 3 + (h - 6) / 2 - 10 * (h - 6) / 32f, 2f * (h - 6) / 32f)
                val tx = ix + 8 + h - 6
                val bw = 60f; val bxx = W - 12f - bw
                val ts = if (u.small) 9 else 10
                k.text(c, k.fit(u.title, ts, bxx - tx - 4), tx, y + h / 2 - (if (u.small) 0 else 1), ts, k.CREAM, Paint.Align.LEFT, true)
                k.text(c, k.fit(u.sub, 8, bxx - tx - 4), tx, y + h / 2 + (if (u.small) 9 else 10), 8, k.col("#c8b896"))
                // 오른쪽 버튼: 위에 값, 아래에 행동
                if (u.cost != null) {
                    k.button(c, bxx, y + 3, bw, h - 6, "", u.can)
                    val cg = u.cost
                    val main = if (cg.gold > 0) big(cg.gold) else if (cg.wood > 0) big(cg.wood) else big(cg.stone)
                    val ic = if (cg.gold > 0) "gold" else if (cg.wood > 0) "wood" else "stone"
                    k.icon(c, ic, bxx + 5, y + h / 2 - 9, .9f)
                    k.text(c, k.fit(main, 9, bw - 20), bxx + 16, y + h / 2 - 1, 9, if (u.can) k.INK else k.col("#c8b896"), Paint.Align.LEFT, true)
                    val extra = listOfNotNull(if (cg.gold > 0 && cg.wood > 0) "나무" + big(cg.wood) else null, if ((cg.gold > 0 || cg.wood > 0) && cg.stone > 0) "돌" + big(cg.stone) else null).joinToString(" ")
                    k.text(c, k.fit(if (extra.isNotEmpty()) extra else u.label + (if (!u.big && mult != 1) (if (mult == 0) " 최대" else " x$mult") else ""), 7, bw - 4), bxx + bw / 2, y + h / 2 + 10, 7, if (u.can) k.INK else k.col("#a89878"), Paint.Align.CENTER)
                    upRects.add(Rect(bxx.toInt() - 2, maxOf(y, areaTop).toInt(), (bxx + bw).toInt() + 4, minOf(y + h, areaBot).toInt()) to u)
                } else if (u.btnText != null) {
                    k.button(c, bxx, y + 3, bw, h - 6, k.fit(u.btnText, 9, bw - 4), u.can)
                    if (u.can) upRects.add(Rect(bxx.toInt() - 2, maxOf(y, areaTop).toInt(), (bxx + bw).toInt() + 4, minOf(y + h, areaBot).toInt()) to u)
                } else k.text(c, u.label, bxx + bw / 2, y + h / 2 + 4, 10, k.DIM, Paint.Align.CENTER, true)
            }
            upY[u.title] = y + h / 2
            y += h + 4
        }
        c.restore()
        if (maxScroll > 0) {
            val vh = areaBot - areaTop; val bh = (vh * vh / (contentH + 4)).coerceAtLeast(14f)
            val by = areaTop + (vh - bh) * (scrollY / maxScroll)
            k.rect(c, W - 5f, areaTop, 2f, vh, k.col("#2a1a10")); k.rect(c, W - 5f, by, 2f, bh, k.GOLD)
        }
    }

    // ---------- 떠오르는 +숫자 ----------
    class Float3(val x: Float, val y: Float, val s: String, val t0: Long)
    private val floats = ArrayList<Float3>()
    private var combo = 0; private var lastTap = 0L
    private fun drawFloats() {
        val now = System.currentTimeMillis()
        floats.removeAll { now - it.t0 > 900 }
        for (f in floats.toList()) {
            val e = (now - f.t0) / 900f
            val a = (255 * (1 - e)).toInt().coerceIn(0, 255)
            Pix.text(c, f.s, f.x, f.y - 6 - e * 22, 11, android.graphics.Color.argb(a, 255, 211, 106), Paint.Align.CENTER, true)
        }
    }

    /** 돌아왔을 때 보상 창 */
    fun showReport(r: Game.Report) {
        val t = if (r.minutes >= 60) (r.minutes / 60).toString() + "시간 " + (r.minutes % 60) + "분" else r.minutes.coerceAtLeast(1).toString() + "분"
        popup = Tile("gold", "돌아왔어요!", "금화 +" + big(r.gold), St.READY,
            t + " 동안 섬과 배가 일했어요.\n\n금화 +" + big(r.gold) + "\n나무 +" + big(r.wood) + " · 돌 +" + big(r.stone) +
                (if (r.voyages > 0) "\n항해 " + r.voyages + "번 다녀왔어요" else "") + (if (r.full) "\n창고가 가득 찼어요" else ""),
            null, "받기", { true }, null)
    }

    // ---------- 배 화면: 가운데 배, 양옆에 부품 넷 ----------
    private val slotRects = ArrayList<Pair<Rect, Tile>>()
    private fun drawShipHead(y: Float, all: List<Tile>, aTop: Float, aBot: Float): Float {
        val k = Pix
        val h = 106f
        k.rect(c, 8f, y, W - 16f, h - 4, k.col("#2a1a10"))
        k.rect(c, 9f, y + 1, W - 18f, h - 6, k.col("#1d3550"))
        k.rect(c, 9f, y + h * .62f, W - 18f, h * .38f - 5, k.col("#17496e"))
        scene.shipArt(64)?.let { bm -> src.set(0, 0, bm.width, bm.height); dst.set((W / 2 - 40), (y + 6).toInt(), (W / 2 + 40), (y + 86).toInt()); c.drawBitmap(bm, src, dst, bp) }
        k.rect(c, W / 2 - 52f, y + h - 16, 104f, 11f, k.col("#cc10243a")); k.text(c, Game.ship().name, W / 2f, y + h - 8, 8, k.CREAM, Paint.Align.CENTER, true)
        val parts = all.filter { it.icon.startsWith("part:") }
        for ((i, t) in parts.withIndex()) {
            val left = i < 3
            val sx = if (left) 12f else W - 12f - 48f; val sy = y + 4 + (i % 3) * 33f
            k.slot(c, sx, sy, 48f, 31f, t.st == St.READY, false)
            k.icon(c, t.icon, sx + 3, sy + 6, 1f)
            val nm = t.name.substringBefore(" Lv"); val lv = t.name.substringAfter(" Lv", "")
            k.text(c, nm, sx + 29, sy + 13, 8, k.CREAM, Paint.Align.CENTER)
            k.text(c, "Lv$lv", sx + 29, sy + 25, 9, if (t.st == St.READY) k.GOLD else k.col("#c8b896"), Paint.Align.CENTER, true)
            if (t.st == St.READY) k.text(c, "!", sx + 44, sy + 9, 10, k.GOLD, Paint.Align.CENTER, true)
            if (sy + 31 > aTop && sy < aBot) slotRects.add(Rect(sx.toInt(), maxOf(sy, aTop).toInt(), (sx + 48).toInt(), minOf(sy + 31, aBot).toInt()) to t)
        }
        return h
    }

    // ---------- 선원 능력치 ----------
    var skillMode = 1
    private val modeRects = ArrayList<Pair<Rect, Int>>()
    private val statRects = ArrayList<Pair<Rect, Pair<Int, Int>>>()   // (칸, (능력치, +1/-1))
    private val faceRects = ArrayList<Pair<Rect, String>>()
    private var statSel: String? = null
    private fun drawStats(py: Float, ph: Float) {
        val k = Pix
        val crew = Game.aboard.filter { it in Game.promoted } + Game.promoted.filter { it !in Game.aboard }
        if (crew.isEmpty()) {
            k.text(c, "아직 선원이 없어요.", W / 2f, py + 60, 11, k.CREAM, Paint.Align.CENTER, true)
            k.text(c, "선원 탭에서 알바를 선원으로 올려요.", W / 2f, py + 78, 9, k.col("#c8b896"), Paint.Align.CENTER)
            return
        }
        if (statSel !in crew) statSel = crew.first()
        val id = statSel!!
        // 선원 얼굴 줄 (작게)
        var fx = 10f
        for (cid in crew) {
            val pi = Data.workers.indexOfFirst { it.id == cid }
            k.slot(c, fx, py + 25, 22f, 22f, Game.ptsFree(cid) > 0, cid == id)
            k.icon(c, (if (Game.crewLevel(cid) >= 11) "face15:" else "face:") + pi, fx + 2, py + 27, 1.8f)
            if (Game.ptsFree(cid) > 0) k.rect(c, fx + 17, py + 24, 5f, 5f, k.GOLD)
            faceRects.add(Rect(fx.toInt(), (py + 23).toInt(), (fx + 24).toInt(), (py + 49).toInt()) to cid)
            fx += 25
        }
        val cn = Fleet.crewOf(id)?.name ?: ""
        val free = Game.ptsFree(id)
        val tx = maxOf(fx + 4, 10f)
        k.text(c, cn + " Lv" + Game.crewLevel(id) + (if (id in Game.aboard) "" else " (대기)"), tx, py + 34, 10, k.CREAM, Paint.Align.LEFT, true)
        k.text(c, "남은 점수 " + free, tx, py + 46, 9, if (free > 0) k.GOLD else k.col("#c8b896"), Paint.Align.LEFT, true)
        val a = Game.pts(id)
        val top = py + 52
        val rowH = ((ph - 52 - 14) / 4f).coerceIn(22f, 30f)
        for (st in 0 until 4) {
            val y = top + st * rowH
            k.rect(c, 8f, y, W - 16f, rowH - 3, k.INK); k.rect(c, 9f, y + 1, W - 18f, rowH - 5, k.col("#3a2614"))
            val canM = a[st] > 0; val canP = free > 0 && a[st] < Game.STAT_MAX
            val bh = rowH - 9
            k.button(c, 11f, y + 3, 18f, bh, "-", canM)
            k.button(c, W - 29f, y + 3, 18f, bh, "+", canP)
            val l1 = y + (rowH - 3) / 2 - 1; val l2 = l1 + 10
            val kc = kindColor(listOf("win", "speed", "cargo", "sale")[st])
            k.text(c, Game.STAT_NAME[st], 34f, l1, 9, k.CREAM, Paint.Align.LEFT, true)
            k.text(c, a[st].toString() + "/" + Game.STAT_MAX, W - 33f, l1, 8, k.col("#c8b896"), Paint.Align.RIGHT)
            k.text(c, Game.statText(st, a[st]), 34f, l2, 8, kc, Paint.Align.LEFT, true)
            if (a[st] < Game.STAT_MAX) k.text(c, "→ " + Game.statText(st, a[st] + 1).substringAfterLast(" "), W - 33f, l2, 8, k.col("#c8b896"), Paint.Align.RIGHT)
            statRects.add(Rect(6, y.toInt(), 32, (y + rowH).toInt()) to (st to -1))
            statRects.add(Rect(W - 32, y.toInt(), W - 6, (y + rowH).toInt()) to (st to 1))
        }
        k.text(c, "레벨이 오를 때마다 2점 · 눌러서 찍어요", W / 2f, py + ph - 5, 8, k.col("#c8b896"), Paint.Align.CENTER)
    }

    // ---------- 항해술 나무 ----------
    private val treeRects = ArrayList<Pair<Rect, Int>>()
    private var treeBtn: Rect? = null
    private var treeSel = -1
    private fun kindColor(kind: String) = Pix.col(when (kind) { "win" -> "#c8503a"; "speed" -> "#3aa0c8"; "cargo" -> "#a8743a"; "sale" -> "#e0b030"; "xp" -> "#70b84a"; else -> "#9a6ad0" })
    private fun drawTree(py: Float, ph: Float) {
        val k = Pix
        if (treeSel < 0 || treeSel >= 25) treeSel = (0 until 25).firstOrNull { Game.treeOpen(it) } ?: 12
        val avail = ph - 28 - 12
        val step = minOf(36f, avail / 5f, (W - 24f) / 5f)
        val ns = step * .72f
        val gx = (W - step * 5) / 2f + (step - ns) / 2; val gy = py + 24 + (avail - step * 5) / 2 + (step - ns) / 2
        fun cx(i: Int) = gx + (i % 5) * step + ns / 2
        fun cy(i: Int) = gy + (i / 5) * step + ns / 2
        // 줄: 이웃한 칸끼리
        for (i in 0 until 25) for (j in listOf(i + 1, i + 5)) {
            if (j >= 25 || (j == i + 1 && i % 5 == 4)) continue
            val on = i in Game.treeOwned && j in Game.treeOwned
            val half = (i in Game.treeOwned) != (j in Game.treeOwned)
            val col = if (on) k.col("#f0c060") else if (half) k.col("#8a6a3a") else k.col("#3a2a18")
            if (j == i + 1) k.rect(c, cx(i), cy(i) - 1.5f, step, 3f, col) else k.rect(c, cx(i) - 1.5f, cy(i), 3f, step, col)
        }
        for (n in Game.TREE) {
            val i = n.i; val x = gx + (i % 5) * step; val y = gy + (i / 5) * step
            val own = i in Game.treeOwned; val open = Game.treeOpen(i)
            val kc = kindColor(n.kind)
            k.rect(c, x - 1, y - 1, ns + 2, ns + 2, if (i == treeSel) k.col("#fff2b0") else k.INK)
            k.rect(c, x, y, ns, ns, if (own) kc else if (open) k.col("#4a3420") else k.col("#24180e"))
            k.rect(c, x + 1, y + 1, ns - 2, 1.5f, if (own) k.col("#ffffff") else kc)
            if (n.big) { k.rect(c, x, y, 3f, 3f, k.GOLD); k.rect(c, x + ns - 3, y, 3f, 3f, k.GOLD); k.rect(c, x, y + ns - 3, 3f, 3f, k.GOLD); k.rect(c, x + ns - 3, y + ns - 3, 3f, 3f, k.GOLD) }
            k.icon(c, "skill:" + n.icon, x + (ns - ns * .8f) / 2, y + (ns - ns * .8f) / 2, ns * .8f / 10f, if (own || open) 255 else 90)
            treeRects.add(Rect(x.toInt() - 3, y.toInt() - 3, (x + ns).toInt() + 3, (y + ns).toInt() + 3) to i)
        }
        k.text(c, "배운 칸 " + Game.treeOwned.size + "/25 · 칸을 눌러 배워요", W / 2f, py + ph - 6, 8, k.GOLD, Paint.Align.CENTER, true)
    }
    /** 항해술 칸을 눌렀을 때 뜨는 창 */
    private fun treeTile(i: Int): Tile {
        val n = Game.TREE[i]; val own = i in Game.treeOwned; val open = Game.treeOpen(i)
        val g = Game.treeCost(i)
        return Tile("skill:" + n.icon, n.name + (if (n.big) " ★" else ""), if (own) "배움" else if (open) "금화 " + big(g) else "잠김",
            if (own) St.DONE else if (open) St.READY else St.LOCKED,
            Game.treeKindText(n) + (if (n.big) "\n★ 큰 칸이라 효과가 커요." else "") + "\n\n" + when { own -> "이미 배웠어요."; open -> "배우면 이웃 칸이 열려요."; else -> "이어진 옆 칸을 먼저 배워요." },
            if (open) Data.Cost(g, 0, 0) else null, if (open) "배우기" else null, { Game.learn(i) }, n.name + "! " + Game.treeKindText(n))
    }


    /** 함대 힘 표시 (오른쪽 위). 올리면 +숫자가 잠깐 떠오른다 */
    private var powFxAt = 0L; private var powFx = 0
    private fun drawPower(y: Float) {
        val k = Pix
        val s = "함대 힘 " + "%,d".format(Game.myPower())
        val w = k.width(s, 10) + 10
        k.rect(c, W - 6 - w, y - 10, w, 14f, android.graphics.Color.argb(160, 20, 12, 6))
        k.text(c, s, W - 11f, y + 1, 10, k.GOLD, Paint.Align.RIGHT, true)
        val e = System.currentTimeMillis() - powFxAt
        if (powFx > 0 && e in 0 until 1800) {
            val a = (255 * (1 - e / 1800f)).toInt().coerceIn(0, 255)
            k.text(c, "+" + "%,d".format(powFx), W - 11f, y + 14 - e / 1800f * 10, 12, android.graphics.Color.argb(a, 143, 208, 106), Paint.Align.RIGHT, true)
        }
    }

    /** 장면 아래 '다음 목표' 띠: 누르면 그 칸을 바로 연다 */
    private fun drawGoal(y: Float) {
        val k = Pix
        goalRect = null
        if (popup != null) return
        val g = appGoal() ?: return
        val ok = g.ready && Game.canPay(g.cost)
        k.rect(c, 4f, y, W - 8f, 18f, k.INK)
        k.rect(c, 5f, y + 1, W - 10f, 16f, if (ok) k.col("#7a4f1c") else k.col("#4a3420"))
        k.rect(c, 5f, y + 1, W - 10f, 1f, if (ok) k.col("#f0c060") else k.col("#8a6030"))
        // 모으는 중이면 진행 막대
        if (!ok) { val f = prog(g.cost); k.rect(c, 5f, y + 15, (W - 10f) * f, 2f, k.GOLD) }
        k.text(c, "다음 목표", 10f, y + 12, 8, k.col("#c8b896"))
        k.text(c, k.fit(g.name, 10, W - 104f), 52f, y + 13, 10, k.CREAM, Paint.Align.LEFT, true)
        k.text(c, if (ok) "지금 하기 ▶" else (prog(g.cost) * 100).toInt().toString() + "%", W - 10f, y + 13, 10, if (ok) k.GOLD else k.col("#c8b896"), Paint.Align.RIGHT, true)
        goalRect = Rect(4, y.toInt(), W - 4, (y + 18).toInt())
    }

    /** 목표 띠를 눌렀을 때: 그 탭으로 가서 그 칸의 창을 연다 */
    /** 앱 목표: 하루 보상이 있으면 그것부터, 아니면 섬의 다음 목표 */
    private fun appGoal(): Game.Goal? = when {
        Game.attReady() -> Game.Goal("d:chest:1", "출석 상자 받기", null, true, false)
        Game.dbReady() -> Game.Goal("d:boss", "오늘의 우두머리", null, true, false)
        Game.seasonReady() -> Game.Goal("d:season", "시즌 보상 받기", null, true, false)
        Game.renownReady() -> Game.Goal("d:renown", "명성 새 항해", null, true, false)
        Game.promoted.any { Game.ptsFree(it) > 0 } -> Game.Goal("d:stats", "선원 능력치 찍기", null, true, false)
        Data.workers.any { Game.canPromote(it.id) } -> Data.workers.first { Game.canPromote(it.id) }.let { w -> Game.Goal("d:promote", w.name + " 선원 승진", Data.Cost(Fleet.promoteGold, 0, 0), true, false) }
        else -> Game.goal()
    }

    private fun openGoal() {
        val g = appGoal() ?: return
        val key = g.key
        if (key == "d:stats" || key == "d:promote") { tab = 10; scrollY = 0f; return }
        if (key.startsWith("d:")) {
            tab = when (key) { "d:renown" -> 1; "d:promote" -> 3; else -> 4 }; page = 0; scrollY = 0f
            val list = tiles()
            popup = when (key) {
                "d:chest:1" -> list.firstOrNull { it.name == "출석 상자" }
                "d:boss" -> list.firstOrNull { it.name == "오늘 우두머리" }
                "d:season" -> list.firstOrNull { it.name.endsWith(" 시즌") }
                "d:promote" -> list.firstOrNull { it.sub == "선원 승진!" }
                else -> list.firstOrNull { it.name.startsWith("명성") }
            }
            return
        }
        tab = when {
            key.startsWith("part:") || key.startsWith("ship_") || key.startsWith("crew:") -> 8
            key.startsWith("crew:") -> 3
            key.startsWith("port_") -> 2
            else -> 0
        }
        page = 0
        val list = tiles()
        val t = when {
            key.startsWith("part:") -> Fleet.part(key.removePrefix("part:"))?.let { p -> list.firstOrNull { it.icon == "part:" + p.icon } }
            key.startsWith("crew:") -> Data.workers.indexOfFirst { it.id == key.removePrefix("crew:") }.let { pi -> list.firstOrNull { it.icon == "face:$pi" || it.icon == "face15:$pi" } }
            key.startsWith("ship_") -> list.firstOrNull { it.name == key.removePrefix("ship_") + "단계" }
            key.startsWith("port_") -> list.firstOrNull { it.name == Data.port(key).name }
            else -> { val ic = when (key) { "ware" -> "store"; else -> key }; list.firstOrNull { it.icon == ic } }
        }
        scrollY = 0f
        t?.let { tl -> if (!tl.icon.startsWith("part:")) scrollTo = tl; popup = tl }
    }
    private var perPageNow = 6
    var scrollY = 0f; private var maxScroll = 0f
    private var scrollTo: Tile? = null
    /** 손가락으로 끌기 (dy: 화면 좌표, 아래로 끌면 +) */
    fun scrollBy(dy: Float) { if (popup != null) return; scrollY = (scrollY - dy).coerceIn(0f, maxScroll) }

    /** 장면 위 진행 표시 (홈 화면과 같은 말) */
    fun label(cv: Canvas, x: Float, y: Float) {
        Pix.text(cv, Hud.title(), x, y, 10, Pix.col("#ffe7a8"), Paint.Align.LEFT, true)
    }

    private fun drawTile(t: Tile, x: Float, y: Float, w: Float, h: Float) {
        val k = Pix
        k.slot(c, x, y, w, h, t.st == St.READY, t.st == St.ON)
        val a = if (t.st == St.LOCKED) 120 else 255
        k.icon(c, t.icon, x + w / 2 - 10, y + 5, 2f, a)
        when (t.st) {
            St.DONE -> k.icon(c, "check", x + w - 12, y + 2, 1f)
            St.READY -> k.text(c, "!", x + w - 6, y + 11, 12, k.GOLD, Paint.Align.CENTER, true)
            St.ON -> k.text(c, "ON", x + w - 3, y + 10, 10, k.GREEN, Paint.Align.RIGHT, true)
            else -> {}
        }
        val ns = listOf(10, 9, 8).firstOrNull { k.fits(t.name, it, w - 4) } ?: 7          // 긴 이름은 글자를 조금 줄여서 다 보이게
        k.text(c, k.fit(t.name, ns, w - 4), x + w / 2, y + 37, ns, if (t.st == St.LOCKED) k.DIM else k.CREAM, Paint.Align.CENTER)
        val subColor = when (t.st) { St.READY -> k.GOLD; St.LOCKED, St.OFF -> k.DIM; St.ON -> k.GREEN; else -> k.col("#c8b896") }
        val ss = listOf(10, 9, 8).firstOrNull { k.fits(t.sub, it, w - 4) } ?: 7
        k.text(c, k.fit(t.sub, ss, w - 4), x + w / 2, y + 49, ss, subColor, Paint.Align.CENTER)
        if (t.cost != null && t.st != St.DONE) k.bar(c, x + 5, y + h - 7, w - 10, 4f, prog(t.cost), if (t.st == St.READY) k.GOLD else k.col("#c8903a"))
    }

    private fun drawPopup(t: Tile) {
        val k = Pix
        k.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), android.graphics.Color.argb(150, 0, 0, 0))
        val lines = k.wrap(t.detail, 10, W - 52f)
        val costH = if (t.cost != null) 16f else 0f
        val ph = 54f + lines.size * 13f + costH + (if (t.btn != null || t.btn2 != null) 26f else 6f)
        val px = 14f; val pw = W - 28f; val py = ((H - ph) / 2f).coerceAtLeast(20f)
        k.panel(c, px, py, pw, ph)
        k.slot(c, px + 10, py + 10, 28f, 28f)
        k.icon(c, t.icon, px + 14, py + 14, 2f)
        val fe = System.currentTimeMillis() - fxAt
        if (fe in 0 until 400) k.icon(c, "fx:" + (fe / 100), px + 2, py + 2, 4.4f)
        k.text(c, t.name, px + 46, py + 24, 12, k.CREAM, Paint.Align.LEFT, true)
        k.text(c, t.sub, px + 46, py + 36, 10, k.GOLD)
        var y = py + 54
        for (ln in lines) { k.text(c, ln, px + 12, y, 10, k.CREAM); y += 13 }
        t.cost?.let { cc ->
            var x = px + 12
            for ((ic, need, have) in listOf(Triple("wood", cc.wood, Game.wood), Triple("stone", cc.stone, Game.stone), Triple("gold", cc.gold, Game.gold))) {
                if (need <= 0) continue
                k.icon(c, ic, x, y - 4, 1f)
                val s = big(need)
                k.text(c, s, x + 12, y + 4, 10, if (have >= need) k.GREEN else k.RED)
                x += 18 + k.width(s, 10)
            }
            y += costH
        }
        btnRect = null; btn2Rect = null
        val two = t.btn2 != null && t.act2 != null
        if (t.btn != null && t.act != null) {
            val ok = t.cost == null || Game.canPay(t.cost)
            val bw = maxOf(if (two) 60f else 70f, k.width(t.btn, 10) + 16f); val bx = if (two) px + 10 else px + (pw - bw) / 2
            k.button(c, bx, y - 4, bw, 18f, t.btn, ok)
            if (ok) btnRect = Rect(bx.toInt() - 4, (y - 8).toInt(), (bx + bw).toInt() + 4, (y + 18).toInt())
        }
        if (two) {
            val b2 = t.btn2!!
            val bw = maxOf(50f, k.width(b2, 10) + 16f); val bx = if (t.btn != null) px + pw - 10 - bw else px + (pw - bw) / 2
            k.button(c, bx, y - 4, bw, 18f, b2, true)
            btn2Rect = Rect(bx.toInt() - 4, (y - 8).toInt(), (bx + bw).toInt() + 4, (y + 18).toInt())
        }
    }

    // ---------- 누르기 ----------
    fun tap(x: Float, y: Float) {
        val ix = x.toInt(); val iy = y.toInt()
        val pop = popup
        if (pop != null) {
            val b2 = btn2Rect
            if (b2 != null && b2.contains(ix, iy)) {
                val ok = pop.act2?.invoke() ?: false
                if (ok && pop.done2 != null) host.toast(pop.done2)
                popup = if (ok) tiles().firstOrNull { it.icon == pop.icon } else null; after = null
                return
            }
            val b = btnRect
            if (b != null && b.contains(ix, iy)) {
                val p0 = Game.myPower()
                val ok = pop.act?.invoke() ?: false
                val dp = Game.myPower() - p0
                if (ok && dp > 0) { powFx = dp; powFxAt = System.currentTimeMillis(); host.toast((pop.done ?: "") + "  함대 힘 +" + "%,d".format(dp)) }
                if (ok && pop.keep) { fxAt = System.currentTimeMillis(); popup = tiles().firstOrNull { it.icon == pop.icon && it.keep } ?: tiles().firstOrNull { it.icon == pop.icon }; after = null; return }
                if (ok && pop.done != null && dp <= 0) host.toast(pop.done)
                popup = if (ok) after else null; after = null
            } else popup = null
            return
        }
        // 위 장면: 병, 상자, 바다 동물, 조개를 앱에서도 톡 쳐서 줍는다
        goalRect?.let { r -> if (r.contains(ix, iy)) { openGoal(); return } }
        if (y >= top && y < top + sceneH) {
            val vh = sceneH / Z
            val got = scene.tap((x - (W - Scene.W * Z) / 2f) / Z, (Scene.WL - vh * .55f).toInt() + (y - top) / Z)
            if (got != null) { host.toast(got); return }
            // 빈 바다를 톡: 금화가 튄다. 빨리 연달아 치면 콤보(최대 2배)
            val now = System.currentTimeMillis()
            combo = if (now - lastTap < 600) (combo + 1).coerceAtMost(20) else 0; lastTap = now
            val n = Math.round(Game.tapGold() * (1 + combo * .05f))
            Game.addTapGold(n)
            floats.add(Float3(x + (Math.random() * 16 - 8).toFloat(), y, "+" + big(n) + (if (combo >= 5) " x" + "%.1f".format(1 + combo * .05f) else ""), now))
            return
        }
        backRect?.let { r -> if (r.contains(ix, iy)) { tab = 9; scrollY = 0f; return } }
        for ((i, r) in tabRects.withIndex()) if (r.contains(ix, iy)) { tab = order[i]; page = 0; scrollY = 0f; return }
        for ((r, m) in multRects) if (r.contains(ix, iy)) { mult = m; return }
        for ((r, u) in upRects) if (r.contains(ix, iy)) { buyRow(u); return }
        treeBtn?.let { r -> if (r.contains(ix, iy)) {
            val p0 = Game.myPower(); val n = Game.TREE[treeSel]
            if (Game.learn(treeSel)) { fxAt = System.currentTimeMillis(); val dp = Game.myPower() - p0
                if (dp > 0) { powFx = dp; powFxAt = fxAt }
                host.toast(n.name + "! " + Game.treeKindText(n) + (if (dp > 0) "  함대 힘 +" + "%,d".format(dp) else "")) }
            return } }
        for ((r, i) in treeRects) if (r.contains(ix, iy)) { treeSel = i; popup = treeTile(i); return }
        for ((r, m) in modeRects) if (r.contains(ix, iy)) { skillMode = m; return }
        for ((r, cid) in faceRects) if (r.contains(ix, iy)) { statSel = cid; return }
        for ((r, sd) in statRects) if (r.contains(ix, iy)) { val id = statSel ?: return
            val p0 = Game.myPower()
            if (Game.statAdd(id, sd.first, sd.second)) { val dp = Game.myPower() - p0; if (dp > 0) { powFx = dp; powFxAt = System.currentTimeMillis() } }
            return }
        for ((r, t) in slotRects) if (r.contains(ix, iy)) { popup = t; return }
        for ((r, pg) in pageRects) if (r.contains(ix, iy)) { page = pg; return }
        for ((r, t) in tileRects) if (r.contains(ix, iy)) { if (t.go >= 0) { tab = t.go; scrollY = 0f } else popup = t; return }
    }

    /** 길게 누르면 계속 올리기: 강화 줄의 버튼 위인지 */
    fun holdable(x: Float, y: Float): Boolean = popup == null && (tab == 8 || tab == 10) && upRects.any { it.first.contains(x.toInt(), y.toInt()) }

    /** 옆으로 밀어서 쪽 넘기기 (dir: +1 다음, -1 앞) */
    fun swipe(dir: Int) {}

    /** 칸 내용(남은 시간 등)은 매번 다시 만들지만, 떠 있는 설명 창은 같은 칸의 새 내용으로 바꾼다 */
    fun refreshPopup() {
        val p = popup ?: return
        popup = tiles().firstOrNull { it.name == p.name && it.icon == p.icon } ?: tiles().firstOrNull { it.name == p.name } ?: p
    }
}

/** 홈 화면과 앱이 함께 쓰는 진행 한 줄 */
object Hud {
    fun title(): String {
        val v = Game.voy
        if (v != null) {
            val f = Game.voyFrac()
            val pn = Data.port(v.port).name
            val left = ((1f - f) * v.dur / 60f).toInt() + 1
            return "항해 " + (Game.voyages + 1) + " · " + when {
                v.encounter && f > Data.fPirate - .055f && f < Data.fPirate + .035f -> "해적과 싸우는 중!"
                f < Data.fArrive -> pn + " 가는 중"
                f < Data.fLeave -> pn + "에서 짐 파는 중"
                else -> "돌아오는 중"
            } + " · " + left + "분"
        }
        val g = Game.goal()
        return if (g != null && (!Game.dock || g.basic)) "섬 · 다음 목표: " + g.name else "섬 · 배가 곧 떠나요"
    }
}
