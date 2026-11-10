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
        fun sound(name: String) {}
        /** 보상 광고: 끝까지 보면 onReward (앱이 아닌 곳에서는 바로 준다) */
        fun showAd(onReward: () -> Unit) { onReward() }
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

    private val tabs = listOf("store" to "짓기", "ship" to "배", "anchor" to "항해", "person" to "견습선원", "map" to "할 일", "book" to "기록", "gear" to "설정", "skill:9" to "항해술", "part:5" to "강화", "map" to "더보기", "person" to "선원")
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
                l.add(go(3, "견습·부적", false))
                l.add(go(1, "단계·꾸미기", false))
                l.add(go(0, "섬 짓기", false))
                val n = Game.unread()
                l.add(go(5, if (n > 0) "새 글 $n" else "도감·일지", n > 0))
                l.add(go(6, "배경화면", false))
            }
            0 -> {
                val h = Game.nextHouse()
                l.add(if (h == null) Tile("house", "숙소 3/3", "다 지음", St.DONE, Fleet.short("buildings", "house", "견습선원이 머무는 숙소예요."))
                    else buyTile("house", "숙소 " + Game.houses + "/3", Game.houseReady(), h.cost, Fleet.short("buildings", "house", "견습선원이 머무는 숙소예요."), h.requires, "짓기", { Game.buildHouse() }, "숙소를 지었어요"))
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
                    val face = faceOf(Game.crewLevel(cr.id)) + cr.portrait
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
                // 배에 탄 선원 → 대기 선원 → 승진할 수 있는 견습선원
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
                    l.add(Tile(faceOf(lv) + pi, c.name + " Lv" + lv, if (canT) "가르치기!" else if (on) "탑승" else "대기", if (canT) St.READY else if (on) St.ON else St.NORMAL,
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
                    "견습선원이 섬에서 일을 " + Fleet.promoteContracts + "번 마치면 금화 " + Fleet.promoteGold + "으로 선원이 될 수 있어요. 선원은 배에 타고 항해할 때마다 경험치를 얻어 Lv" + Fleet.maxCrewLevel + "까지 자라고, 레벨마다 새 스킬이 열려요. 짝이 맞는 둘을 같이 태우면 보너스가 더 붙어요."))
                for (i in 0 until 3) {
                    val wk = Game.workers.getOrNull(i)
                    l.add(when {
                        wk != null -> { val d = Data.worker(wk.id); val left = Fleet.promoteContracts - Game.completedOf(wk.id)
                            val cr = Fleet.crewOf(wk.id) != null && wk.id !in Game.promoted
                            Tile("person", d?.name ?: "견습선원", if (cr && left > 0) "선원까지 " + left + "번" else mmss(wk.expire - now), St.DONE,
                            (d?.name ?: "") + " · " + (d?.role ?: "") + "\n" + (d?.trait ?: "") + "\n" + (d?.look ?: "") + "\n\n남은 시간 " + mmss(wk.expire - now) +
                                (if (cr) "\n일을 " + Fleet.promoteContracts + "번 마치면 선원이 될 수 있어요. 지금 " + Game.completedOf(wk.id) + "번" else "")) }
                        i < Game.houses -> Tile("house", "빈 숙소", "구하는 중", St.NORMAL, "배가 항구에 가면 빈 숙소만큼 견습선원을 데려와요.")
                        else -> Tile("lock", "숙소 없음", "짓기 탭", St.LOCKED, "숙소를 지으면 견습선원이 묵을 수 있어요.")
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
                        l.add(Tile("charm:" + ch.icon, ch.name, "Lv" + lv + " " + (who?.let { Fleet.crewOf(it)?.name } ?: "보관"), if (who != null) St.ON else St.NORMAL,
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
                l.add(Tile("gold", "비싼 견습", if (Game.hireAny) "켜짐" else "꺼짐", if (Game.hireAny) St.ON else St.OFF,
                    "먼 항구의 견습선원은 고용비가 더 들지만 솜씨가 좋아요.", null, if (Game.hireAny) "끄기" else "켜기", { Game.hireAny = !Game.hireAny; Game.save(); true }))
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
                    l.add(Tile("crit:" + (seen.lastOrNull()?.icon ?: 0), "동물 도감", seen.size.toString() + "/" + Fleet.creatures.size, if (seen.isNotEmpty()) St.ON else St.NORMAL,
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
                l.add(Tile("chest:1", "효과음", if (Game.soundOn) "켜짐" else "꺼짐", if (Game.soundOn) St.ON else St.OFF,
                    "톡톡, 강화, 해적 싸움 소리예요.", null, if (Game.soundOn) "끄기" else "켜기", { Game.soundOn = !Game.soundOn; Game.save(); true }))
                l.add(Tile("auto", "자동 짓기", if (Game.autoBuild) "켜짐" else "꺼짐", if (Game.autoBuild) St.ON else St.OFF,
                    "숙소·창고·부두·밭은 재료가 모이면 알아서 지어요. 금화는 안 써요.", null, if (Game.autoBuild) "끄기" else "켜기", { Game.autoBuild = !Game.autoBuild; Game.save(); true }))
                l.add(Tile("book", "처음 안내", "다시 보기", St.NORMAL, "손가락으로 짚어 주는 처음 안내를 다시 봐요.", null, "보기", { Game.tut = 0; Game.tips.clear(); Game.save(); false }))
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
        snapTo?.let { t -> scrollY += (t - scrollY) * .35f; if (Math.abs(t - scrollY) < .5f) { scrollY = t; snapTo = null } }
        Pix.clean(true)
        try { drawAll(); drawTut() } finally { Pix.clean(false) }
    }

    private fun drawAll() {
        val k = Pix
        k.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), k.col("#0d0a08"))
        // 위: 장면 (바다 띠 부분을 잘라서)
        // 멀리서 보듯: 장면을 작게(Z배) 그리고, 양옆은 거울처럼 이어 붙인다
        val vh = sceneH / Z; val sy = (Scene.WL - vh * .55f).toInt()
        val sw = Scene.W * Z; val ox = (W - sw) / 2f
        c.save(); c.clipRect(0f, top, W.toFloat(), top + sceneH)
        scene.wantBg = true
        fun band(x0: Float, bm: android.graphics.Bitmap) {
            src.set(0, sy * Scene.SS, Scene.W * Scene.SS, (sy + vh.toInt()) * Scene.SS)
            dstF.set(x0, top, x0 + sw, top + sceneH); c.drawBitmap(bm, src, dstF, bp)
        }
        // 양옆: 배·섬·항구가 두 번 보이지 않게 하늘과 바다만 이어 붙인다
        band(ox - sw, scene.bgBitmap); band(ox + sw, scene.bgBitmap)
        band(ox, scene.bitmap)
        c.restore()
        label(c, 6f, top + 14)
        drawPower(top + 28)
        // 큰 금화 숫자와 초당 금화
        k.icon(c, "gold", 6f, top + 22, 1.6f)
        k.text(c, big(Game.gold), 25f, top + 36, 16, k.GOLD, Paint.Align.LEFT, true)
        k.text(c, (if (Game.gpsAll() > 0.05f) "초당 +" + (if (Game.gpsAll() < 10) "%.1f".format(Game.gpsAll()) else big(Game.gpsAll().toLong())) + " · " else "") + (if (Game.festLeft() > 0) "잔치! 톡톡 2배" else "바다를 톡톡 눌러요!"), 7f, top + 49, 8, k.CREAM, Paint.Align.LEFT, true)
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
        snaps.clear(); snaps.add(0f); for (r in 0 until rows) snaps.add(head + r * (th + gx))
        if (tab == 1) drawShipHead(y0, all, areaTop, areaBot)
        for ((n, t) in list.withIndex()) {
            val x = x0 + (n % 3) * (tw + gx); val y = y0 + head + (n / 3) * (th + gx)
            if (y + th < areaTop || y > areaBot) continue
            drawTile(t, x, y, tw, th)
            tileRects.add(Rect(x.toInt(), maxOf(y, areaTop).toInt(), (x + tw).toInt(), minOf(y + th, areaBot).toInt()) to t)
        }
        // 밀린 칸이 위아래 끝에서 뚝 잘려 보이지 않게 그늘
        if (scrollY > 0.5f) for (i in 0 until 10) k.rect(c, 2f, areaTop + i, W - 4f, 1f, android.graphics.Color.argb(240 - i * 24, 42, 26, 16))
        if (scrollY < maxScroll - 0.5f) for (i in 0 until 10) k.rect(c, 2f, areaBot - 1 - i, W - 4f, 1f, android.graphics.Color.argb(240 - i * 24, 42, 26, 16))
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
        val items = listOf("wood" to big(Game.wood), "stone" to big(Game.stone), "gold" to big(Game.gold), "map" to Game.mapPieces.toString())
        // 금화는 자리를 넓게 (5단계 배는 18만 금화라 숫자가 길다)
        val gap = floatArrayOf(44f, 44f, 62f, 0f)
        var x = 26f
        for ((i, iv) in items.withIndex()) { val (ic, v) = iv; k.icon(c, ic, x, by + 4, 1f); k.text(c, v, x + 12, by + 13, 10, k.GOLD); x += gap[i] }
    }

    // ---------- 강화: 거지키우기처럼 한 줄 목록 하나 ----------
    class URow(val icon: String, val title: String, val sub: String, val cost: Data.Cost?, val can: Boolean, val label: String,
               val act: () -> Boolean, val big: Boolean = false, val btnText: String? = null, val small: Boolean = false, val head: Boolean = false,
               val costAt: ((Int) -> Data.Cost?)? = null)
    /** x10·최대일 때 실제로 몇 단계를 사는지와 그 합계 */
    private fun multiCost(u: URow): Pair<Int, Data.Cost>? {
        val f = u.costAt ?: return null
        if (mult == 1) return null
        var g = 0L; var w = 0L; var st = 0L; var n = 0
        val lim = if (mult == 0) 999 else mult
        while (n < lim) { val c = f(n) ?: break
            if (mult == 0 && (Game.gold < g + c.gold || Game.wood < w + c.wood || Game.stone < st + c.stone)) break
            g += c.gold; w += c.wood; st += c.stone; n++ }
        if (n == 0) { val c = f(0) ?: return null; return 1 to c }
        return n to Data.Cost(g.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), w.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), st.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }
    private val upRects = ArrayList<Pair<Rect, URow>>()
    private val multRects = ArrayList<Pair<Rect, Int>>()
    private var mult = 1                       // 1, 10, 0(최대)
    fun upRows(): List<URow> {
        val l = ArrayList<URow>()
        // 4시간마다 보물 상자
        if (Game.chestReady()) l.add(URow("chest:1", "보물 상자 도착!", "금화 " + big(Game.chestGold()) + " + 쪽지 1장", null, true, "", {
            val g = Game.openChest(); if (g > 0) { host.sound("chest"); coins(W / 2f, 300f, 24); floats.add(Float3(W / 2f, 300f, "+" + big(g), System.currentTimeMillis())) }; g > 0 }, btnText = "열기"))
        else l.add(URow("chest:1", "다음 보물 상자", mmss(Game.chestLeft()) + " 뒤에 도착해요", null, false, "", { false }, btnText = "대기"))
        // 섬에 머물기 / 출항
        if (Game.dock) {
            val fl = Game.festLeft()
            l.add(URow("anchor", if (fl > 0) "귀항 잔치 중!" else if (Game.atHome()) "섬에 들러 있어요" else "섬으로 돌아가기",
                if (fl > 0) "톡톡 2배 · " + mmss(fl) + " 남음" else if (Game.stayHome) (if (Game.atHome()) "출항을 누르면 떠나요" else "돌아오면 섬에 머물러요")
                else "귀항 잔치까지 항해 " + (Game.FEST_EVERY - Game.voyages % Game.FEST_EVERY) + "번",
                null, true, "", { if (Game.festLeft() > 0 && !Game.stayHome) { Game.festUntil = 0L; Game.save(); true } else Game.toggleStay() },
                btnText = if (Game.stayHome || fl > 0) "출항" else "섬으로"))
        }
        // 해적 토벌 단계
        if (Game.dock) {
            val w = Game.stageWait()
            l.add(URow("boss:0", Game.stageTitle(), if (w > 0) "쉬는 중 " + (w / 60000 + 1) + "분 남았어요" else "승률 " + pct1(Game.stageOdds()) + (if (Game.stageBoss()) " · 우두머리! 쪽지 3" else " · 금화 " + big(Game.stageGold())),
                null, true, "", { if (w > 0) host.showAd { Game.skipStageCool(); fight() } else fight(); true }, btnText = if (w > 0) "광고 도전" else "도전!"))
        }
        // 명성: 다 키우면 새 항해 (영구 보너스)
        if (Game.renownReady()) l.add(URow("title:0", "다시 출항! 명성 " + (Game.renown + 1), "판매 +10% · 승률 +2%p", null, true, "", { popup = renownTile(); true }, btnText = "출항", big = false))
        else if (Game.shipLevel >= 5 || Game.renown > 0) {
            val cap = Fleet.maxLevel(Game.shipLevel); val low = Fleet.core.minOf { Game.partLevel(it.id) }
            l.add(URow("title:0", "다시 출항 · 명성 " + Game.renown, if (Game.shipLevel < 5) "배 5단계부터 열려요" else "판매 +10% · 승률 +2%p", null, false, "", { popup = renownTile(); true },
                btnText = if (Game.shipLevel < 5) "배 5단계" else "부품 $low/$cap"))
        }
        // 다음 배 단계
        val nx = Game.shipLevel + 1
        Data.unlock("ship_$nx")?.takeIf { !Game.has(it.key) && Data.ships.any { s -> s.level == nx } }?.let { u ->
            val need = Fleet.needForShip(nx); val minP = Game.minPart()
            val ready = Game.unlockReady(u)
            val capLow = u.cost != null && (u.cost.wood > Game.cap() || u.cost.stone > Game.cap())
            l.add(URow("ship", "배 " + nx + "단계로 키우기", if (minP < need) "부품 모두 Lv$need (지금 $minP)" else if (capLow) "창고를 넓혀야 재료를 담아요" else Data.ship(nx).name.substringAfter("· "),
                u.cost, ready && Game.canPay(u.cost), "키우기", { Game.buyUnlock(u.key) }, big = true))
        }
        // 섬 생산: 배가 커지는 만큼 섬도 같이
        fun rate(v: Float) = if (v < 10) "%.2f".format(v) else big(v.toLong())
        val wc = Game.prodCost(Game.woodLv); val sc = Game.prodCost(Game.stoneLv); val stc = Game.storeCost(Game.storeLv)
        fun nx(lv: Int) = (Game.prodMul(lv + 1) / Game.prodMul(lv))
        l.add(URow("wood", "나무 캐기 Lv" + Game.woodLv, "초당 " + rate(Game.woodRate()) + " → " + rate(Game.woodRate() * nx(Game.woodLv)) + (if ((Game.woodLv + 1) % 10 == 0) " 2배!" else ""),
            wc, Game.canPay(wc), "+1", { Game.buyProd(0) }, costAt = { i -> Game.prodCost(Game.woodLv + i) }))
        l.add(URow("stone", "돌 캐기 Lv" + Game.stoneLv, "초당 " + rate(Game.stoneRate()) + " → " + rate(Game.stoneRate() * nx(Game.stoneLv)) + (if ((Game.stoneLv + 1) % 10 == 0) " 2배!" else ""),
            sc, Game.canPay(sc), "+1", { Game.buyProd(1) }, costAt = { i -> Game.prodCost(Game.stoneLv + i) }))
        val capNow = Game.cap(); Game.storeLv++; val capNext = Game.cap(); Game.storeLv--
        l.add(URow("store", "창고 Lv" + Game.storeLv, "보관 " + big(capNow) + " → " + big(capNext), stc, Game.canPay(stc), "+1", { Game.buyProd(2) }, costAt = { i -> Game.storeCost(Game.storeLv + i) }))
        for (p in Fleet.parts) {
            val lv = Game.partLevel(p.id); val n = Game.partNext(p.id)
            val st = Game.stars(p.id); val stS = if (st > 0) " ★$st" else ""
            if (n != null) l.add(URow("part:" + p.icon, p.name + " Lv" + lv + stS, if ((lv + 1) % 10 == 0) "Lv" + (lv + 1) + " 달성하면 판매 +10%" else Fleet.partText(p, Game.partValue(p.id)) + " → " + Fleet.partText(p, n.value * (1 + .1f * st)).substringAfterLast(" "),
                n.cost, Game.canPay(n.cost), "+1", { Game.buyPart(p.id) }, costAt = { i -> Game.partCostAt(p.id, i) }))
            else if (Game.starReady(p.id)) { val sc = Game.starCost(p.id)
                l.add(URow("part:" + p.icon, p.name + " Lv" + lv + stS, "별 " + st + "/" + Game.STAR_MAX + " · 별마다 효과 +10%", sc, Game.canPay(sc), "★+1", { Game.buyStar(p.id) })) }
            else l.add(URow("part:" + p.icon, p.name + " Lv" + lv + stS, if (lv >= p.levels.size) "가장 높은 레벨" else "배를 키우면 더 올려요", null, false, "최고", { false }))
        }
        return l
    }
    /** 선원 얼굴: Lv11·30·50에서 그림이 바뀐다 (30·50 그림이 없으면 15 그림) */
    private fun faceOf(lv: Int) = if (lv >= 50) "face50:" else if (lv >= 30) "face30:" else if (lv >= 11) "face15:" else "face:"
    private val STAT_ICON = listOf("skill:6", "skill:1", "skill:0", "skill:3")
    private fun pct(f: Float) = Math.round(f * 100).toString() + "%"
    fun crewRows(): List<URow> {
        val l = ArrayList<URow>()
        l.add(URow("", "선원 자리 " + Game.aboard.size + "/" + Game.slots() + " · 배에 탄 선원만 힘을 보태요", "", null, false, "", { false }, head = true))
        if (Game.promoted.isNotEmpty()) l.add(URow("", "선원 덕분에 판매 +" + pct(Game.crewSaleAll()) + " · 승률 +" + "%.1f".format(Game.cs(Game.aboard.take(Game.slots()), 0) * 100).removeSuffix(".0") + "%p", "", null, false, "", { false }, head = true))
        for (w in Data.workers) if (Game.canPromote(w.id)) {
            val pi = Data.workers.indexOf(w); val cst = Data.Cost(Fleet.promoteGold, 0, 0)
            l.add(URow("face:$pi", w.name + " 선원으로", "견습 기간을 마쳤어요. 선원이 될 수 있어요", cst, Game.canPay(cst), "승진", { Game.promote(w.id) }))
        }
        if (Game.promoted.isEmpty()) l.add(URow("", "견습선원이 계약을 2번 마치면 선원이 될 수 있어요", "", null, false, "", { false }, head = true))
        for (id in Game.promoted) {
            val pi = Data.workers.indexOfFirst { it.id == id }; val lv = Game.crewLevel(id)
            val tc = Game.teachCost(id); val free = Game.ptsFree(id); val on = id in Game.aboard
            val nm = Fleet.crewOf(id)?.name ?: id
            l.add(URow(faceOf(lv) + pi, nm + " Lv" + lv + (if (on) " · 승선" else " · 쉬는 중"),
                if (lv >= Fleet.maxCrewLevel) "최고 레벨 · 판매 +" + pct(Game.crewStat(id, 3)) else if (lv >= Game.crewCap()) "배 " + (lv / 5 + 1) + "단계부터 더 올라요"
                else Fleet.crewOf(id)?.skills?.firstOrNull { it.level == lv + 1 }?.let { _ -> "판매 +" + pct(Game.crewStat(id, 3)) + " · 다음에 새 스킬!" }
                    ?: ("판매 +" + pct(Game.crewStat(id, 3)) + " → +" + pct(Game.crewStat(id, 3, lv + 1)) + (if ((lv + 1) % 10 == 0) " ×1.35!" else "")),
                tc?.let { Data.Cost(it, 0, 0) }, tc != null && Game.gold >= tc, "+1", { Game.teach(id) }, costAt = { i -> Game.teachCostAt(id, i) }))
            if (!on && Game.aboard.size < Game.slots()) l.add(URow("ship", nm + " 배에 태우기", "자리가 비었어요", null, true, "", { Game.toggleAboard(id) }, btnText = "태우기", small = true))
        }
        return l
    }
    private fun moreDot() = Game.attReady() || Game.dbReady() || Game.seasonReady() || Game.unread() > 0 ||
        false

    private fun buyRow(u: URow) {
        if (!u.can) { host.toast("재료가 모자라요"); return }
        multiCost(u)?.let { (_, tc) -> if (mult == 10 && !Game.canPay(tc)) { host.toast("10번 올리기엔 모자라요"); return } }
        val p0 = Game.myPower()
        val times = if (u.big || (u.btnText != null && u.cost == null && !u.small)) 1 else if (mult == 0) 999 else mult
        var n = 0
        while (n < times && u.act()) n++
        if (n == 0) return
        fxAt = System.currentTimeMillis(); sparkIcon = u.icon
        if (u.cost != null || u.small) host.sound("up")
        val dp = Game.myPower() - p0
        if (dp > 0) { powFx = dp; powFxAt = fxAt }
        val nm = u.title.substringBefore(" Lv")
        // 10레벨을 넘기면 크게 축하
        val lvBefore = u.title.substringAfter(" Lv", "").takeWhile { it.isDigit() }.toIntOrNull()
        if (lvBefore != null && (lvBefore + n) / 10 > lvBefore / 10) { host.sound("win"); coins(W / 2f, 200f, 30)
            floats.add(Float3(W / 2f, 230f, "Lv" + ((lvBefore + n) / 10 * 10) + " 달성! " + (if (u.icon == "wood" || u.icon == "stone" || u.icon == "store") "2배!" else if (u.icon.startsWith("face")) "판매 ×1.35" else "판매 +10%"), fxAt)) }
        // 선원 새 스킬: 뽑기처럼 크게
        if (lvBefore != null && u.icon.startsWith("face")) Fleet.crew.firstOrNull { it.name == nm }?.skills?.lastOrNull { it.level in (lvBefore + 1)..(lvBefore + n) }?.let { sk ->
            host.sound("chest"); coins(W / 2f, 180f, 20); floats.add(Float3(W / 2f, 205f, "새 스킬! 「" + sk.name + "」", fxAt)) }
        if (u.cost != null || u.small) floats.add(Float3(W - 30f, upY[u.title] ?: 300f, if (u.big) "배가 커졌어요!" else nm + " +" + n, fxAt))
        if (u.big) coins(W / 2f, 120f, 30)
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
        upY.clear(); snaps.clear()
        run { var cy = 0f; for (u in rows) { snaps.add(cy); cy += hOf(u) + 4 } }
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
                if (u.icon == sparkIcon) { val fe = System.currentTimeMillis() - fxAt; if (fe in 0 until 400) k.icon(c, "fx:" + (fe / 100), ix - 4, y - 8, 2.6f) }
                val tx = ix + 8 + h - 6
                val bw = 60f; val bxx = W - 12f - bw
                val ts0 = if (u.small) 9 else 10
                val ts = listOf(ts0, ts0 - 1, ts0 - 2).firstOrNull { k.fits(u.title, it, bxx - tx - 4) } ?: (ts0 - 2)
                k.text(c, k.fit(u.title, ts, bxx - tx - 4), tx, y + h / 2 - (if (u.small) 0 else 1), ts, k.CREAM, Paint.Align.LEFT, true)
                k.text(c, k.fit(u.sub, 8, bxx - tx - 4), tx, y + h / 2 + (if (u.small) 9 else 10), 8, k.col("#c8b896"))
                // 오른쪽 버튼: 위에 값, 아래에 행동
                if (u.cost != null) {
                    val mc = multiCost(u)
                    val cg = mc?.second ?: u.cost
                    val ok = u.can && (mc == null || Game.canPay(cg))
                    k.button(c, bxx, y + 3, bw, h - 6, "", ok)
                    val main = if (cg.gold > 0) big(cg.gold) else if (cg.wood > 0) big(cg.wood) else big(cg.stone)
                    val ic = if (cg.gold > 0) "gold" else if (cg.wood > 0) "wood" else "stone"
                    k.icon(c, ic, bxx + 5, y + h / 2 - 9, .9f)
                    k.text(c, k.fit(main, 9, bw - 20), bxx + 16, y + h / 2 - 1, 9, if (ok) k.INK else k.col("#c8b896"), Paint.Align.LEFT)
                    fun mats(f: (Long) -> String) = listOfNotNull(if (cg.gold > 0 && cg.wood > 0) "나무" + f(cg.wood.toLong()) else null, if ((cg.gold > 0 || cg.wood > 0) && cg.stone > 0) "돌" + f(cg.stone.toLong()) else null).joinToString(" ")
                    fun kk(n: Long) = if (n < 1000) n.toString() else if (n < 1_000_000) ("%.1f".format(n / 1000f)).removeSuffix(".0") + "K" else ("%.1f".format(n / 1e6f)).removeSuffix(".0") + "M"
                    val plus = if (mc != null) "+" + mc.first + " " else ""
                    val m1 = mats { big(it) }; val m2 = mats { kk(it) }
                    val extra = if (m1.isBlank()) "" else listOf(plus + m1, m1, plus + m2, m2).firstOrNull { k.fits(it, 7, bw - 4) } ?: m2
                    k.text(c, k.fit(if (extra.isNotBlank()) extra else (if (mc != null) "+" + mc.first else u.label), 7, bw - 4), bxx + bw / 2, y + h / 2 + 10, 7, if (ok) k.INK else k.col("#a89878"), Paint.Align.CENTER)
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
        // 목록 위·아래 끝: 잘린 줄이 머리줄과 겹쳐 보이지 않게 그림자로 덮는다
        if (scrollY > 0.5f) for (i in 0 until 10) k.rect(c, 2f, areaTop + i, W - 4f, 1f, android.graphics.Color.argb(240 - i * 24, 42, 26, 16))
        if (scrollY < maxScroll - .5f) for (i in 0 until 10) k.rect(c, 2f, areaBot - 1 - i, W - 4f, 1f, android.graphics.Color.argb(240 - i * 24, 42, 26, 16))
        k.rect(c, 4f, areaTop - 1, W - 8f, 1f, k.col("#6b4a2a"))
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
    class Coin(var x: Float, var y: Float, var vx: Float, var vy: Float, val t0: Long)
    private val coinList = ArrayList<Coin>()
    private var sparkIcon = ""
    private var lastCoinT = 0L
    fun coins(x: Float, y: Float, n: Int) {
        val now = System.currentTimeMillis()
        repeat(n) { coinList.add(Coin(x, y, (Math.random() * 120 - 60).toFloat(), (-Math.random() * 120 - 60).toFloat(), now + (Math.random() * 200).toLong())) }
    }
    private var seenBattle = Game.lastBattleAt
    // ---------- 해적 토벌: 보스 도전처럼 대포와 피해 숫자가 터지는 싸움 장면 ----------
    private class Battle(val t0: Long, val win: Boolean, val my: Int, val en: Int, val title: String, val boss: Boolean, val gold: Long, val sprite: Int,
                         val hits: List<Triple<Boolean, Int, Boolean>>)   // (내가 쐈나, 피해, 치명타)
    private var battle: Battle? = null
    private var battleShot = -1
    private val SHOT_MS = 520L
    private val INTRO_MS = 800L
    private val FLY_MS = 300L
    private fun battleEnd(b: Battle) = INTRO_MS + (b.hits.size - 1) * SHOT_MS + FLY_MS + 250L
    private fun fight() {
        if (battle != null) return
        val n = Game.pStage; val title = Game.stageTitle(n); val boss = Game.stageBoss(n)
        val my = Game.myPower(); val en = Game.enemyPower(Game.stagePirate(n)); val gold = Game.stageGold(n).toLong()
        val res = Game.challengeStage()
        if (res == 0) return
        val win = res == 1
        fun split(total: Int): List<Int> { val w = List(4) { .6 + Math.random() }; val s = w.sum(); val l = w.map { (total * it / s).toInt().coerceAtLeast(1) }.toMutableList(); l[3] += total - l.sum(); return l }
        val mine = split(if (win) en else (en * (.55 + Math.random() * .3)).toInt())
        val theirs = split(if (win) (my * (.25 + Math.random() * .45)).toInt() else my)
        val hits = ArrayList<Triple<Boolean, Int, Boolean>>()
        for (i in 0 until 4) { hits.add(Triple(true, mine[i], mine[i] > en * .3f)); if (win && i == 3) break; hits.add(Triple(false, theirs[i], theirs[i] > my * .3f)) }
        battle = Battle(System.currentTimeMillis(), win, my, en, title, boss, gold, if (boss) Game.gang(n).sprite else n % 8, hits); battleShot = -1
    }
    /** 굵은 테두리 글자 (피해 숫자가 배경에 묻히지 않게) */
    private fun boldText(s: String, x: Float, y: Float, size: Int, col: Int, align: Paint.Align = Paint.Align.CENTER) = Pix.bigText(c, s, x, y, size, col, align)
    private fun drawBattle() {
        val b = battle ?: return
        val k = Pix; val now = System.currentTimeMillis(); val t = now - b.t0
        val end = battleEnd(b)
        if (t > end + 3400) { battle = null; return }
        val hr = maxOf(1, c.width / 200)
        val wl = top + 120f
        fun shotT(i: Int) = INTRO_MS + i * SHOT_MS
        fun hitT(i: Int) = shotT(i) + FLY_MS
        // 화면 흔들림: 맞을 때마다, 치명타는 더 세게
        var sx = 0f; var sy = 0f
        for ((i, h) in b.hits.withIndex()) { val d = t - hitT(i); if (d in 0..220) { val p = (if (h.third) 3.5f else 1.8f) * (1 - d / 220f)
            sx = (if ((d / 35) % 2 == 0L) 1 else -1) * p; sy = (if ((d / 50) % 2 == 0L) -1 else 1) * p * .6f } }
        c.save(); c.clipRect(0f, top, W.toFloat(), top + sceneH); c.translate(sx, sy)
        // 바탕: 진짜 바다 그림 (우두머리는 노을 바다)
        val bgName = if (b.boss) "bg_sea_dusk" else "bg_sea"
        val bg = Art.get(bgName, 384)
        if (bg != null) { val drift = ((t / 90) % 60).toInt()
            src.set(40 + drift, 128 - 118, 40 + drift + 250, 128 - 118 + 198); dst.set(-4, top.toInt() - 4, W + 4, (top + sceneH + 4).toInt()); c.drawBitmap(bg, src, dst, bp)
        } else { k.rect(c, -4f, top - 4, W + 8f, wl - top + 4, k.col("#5d9fc4")); k.rect(c, -4f, wl, W + 8f, top + sceneH - wl + 4, k.col("#2d5f94")) }
        // 물결 줄
        for (i in 0 until 14) { val yy = wl + 10 + (i % 4) * 13 + (i / 4) * 3; val xx = ((i * 41 + t / (28 + i % 3 * 9)) % (W + 30)).toFloat() - 15
            k.rect(c, xx, yy, 12f - (i % 3) * 2, 1f, android.graphics.Color.argb(150, 210, 240, 255)) }
        // 지금 HP
        fun hpAt(tt: Long): Pair<Float, Float> { var m = b.my.toFloat(); var e = b.en.toFloat()
            for ((i, h) in b.hits.withIndex()) if (tt >= hitT(i)) { if (h.first) e -= h.second else m -= h.second }
            return m.coerceAtLeast(0f) to e.coerceAtLeast(0f) }
        val (myHp, enHp) = hpAt(t); val (myLag, enLag) = hpAt(t - 380)
        // 소리
        val cur = b.hits.indices.lastOrNull { t >= shotT(it) } ?: -1
        if (cur != battleShot && cur >= 0 && battleShot != 99) { battleShot = cur; host.sound("boom") }
        // 배 자리: 처음엔 양쪽에서 들어온다
        val ie = (t / INTRO_MS.toFloat()).coerceIn(0f, 1f); val ease = 1 - (1 - ie) * (1 - ie) * (1 - ie)
        fun recoil(mine: Boolean): Float { for ((i, h) in b.hits.withIndex()) { val d = t - shotT(i); if (h.first == mine && d in 0..140) return (if (mine) -3f else 3f) * (1 - d / 140f) }; return 0f }
        fun jolt(mine: Boolean): Float { for ((i, h) in b.hits.withIndex()) { val d = t - hitT(i); if (h.first != mine && d in 0..200) return ((d / 40) % 2 * 2 - 1).toFloat() * (if (h.third) 3f else 2f) }; return 0f }
        val myX = -60 + 112 * ease + recoil(true) + jolt(true); val enX = 292 - 112 * ease + recoil(false) + jolt(false)
        val sinkE = ((t - end) / 1300f).coerceIn(0f, 1f)
        fun drawShip(bm: android.graphics.Bitmap, cx: Float, w: Float, mine: Boolean) {
            val h = w * bm.height / bm.width
            val sinking = t > end && (mine != b.win)
            val bob = (Math.sin(now / 380.0 + if (mine) 0.0 else 2.0) * 1.3).toFloat()
            val bottom = wl + 9 + bob + (if (sinking) sinkE * 70 else 0f)
            c.save()
            if (sinking) c.rotate((if (mine) 1 else -1) * sinkE * 22f, cx, bottom)
            else c.rotate((Math.sin(now / 520.0 + if (mine) 1.0 else 0.0) * 1.6).toFloat(), cx, bottom)
            src.set(0, 0, bm.width, bm.height); dst.set((cx - w / 2).toInt(), (bottom - h).toInt(), (cx + w / 2).toInt(), bottom.toInt()); c.drawBitmap(bm, src, dst, bp)
            c.restore()
        }
        scene.shipArt(86)?.let { drawShip(it, myX, 86f, true) }
        (if (b.boss) (if (b.sprite >= 100) Art.cell("pirate_bosses_2", 4, 1, b.sprite - 100, 84 * hr) else Art.cell("pirate_bosses", 4, 2, b.sprite, 84 * hr)) else Art.cell("pirates_all", 4, 2, b.sprite, 80 * hr))?.let { drawShip(it, enX, if (b.boss) 84f else 80f, false) }
        // 배 아랫부분을 물이 덮는다 + 뱃머리 물거품
        k.rect(c, -4f, wl + 4, W + 8f, top + sceneH - wl, if (b.boss) android.graphics.Color.argb(140, 60, 110, 140) else android.graphics.Color.argb(140, 20, 100, 180))
        for (x0 in listOf(myX, enX)) for (j in 0 until 5) { val fx = x0 - 34 + j * 17 + ((now / 160 + j) % 3).toFloat(); k.rect(c, fx, wl + 4, 7f, 1.5f, android.graphics.Color.argb(200, 235, 250, 255)) }
        // 포격: 불꽃, 연기, 포탄, 터짐, 파편, 물보라
        for ((i, h) in b.hits.withIndex()) {
            val d = t - shotT(i); if (d < 0) continue
            val mx = if (h.first) myX + 38 else enX - 36; val my = wl - 14
            val tx = if (h.first) enX else myX; val ty = wl - 22
            if (d < 110) Art.cell("fx", 6, 2, 0, (if (h.first) 18 else 16) * hr)?.let { Art.draw(c, it, mx, my + 9, !h.first) }
            if (d < 700) { val sm = when { d < 200 -> 1; d < 420 -> 2; else -> 3 }
                Art.cell("fx", 6, 2, sm, (10 + sm * 3) * hr)?.let { Art.draw(c, it, mx + (if (h.first) -1 else 1) * d / 60f, my + 4 - d / 45f, false, (210 * (1 - d / 700f)).toInt()) } }
            if (d in 0 until FLY_MS) { val e = d / FLY_MS.toFloat()
                for (tr in 3 downTo 0) { val ee = (e - tr * .06f).coerceAtLeast(0f); val bx = mx + (tx - mx) * ee; val by = my + (ty - my) * ee - 34 * 4 * ee * (1 - ee)
                    if (tr == 0) { k.rect(c, bx - 2.5f, by - 3.5f, 5f, 7f, k.INK); k.rect(c, bx - 3.5f, by - 2.5f, 7f, 5f, k.INK); k.rect(c, bx - 2.5f, by - 2.5f, 5f, 5f, k.col("#3a3a44")); k.rect(c, bx - 1.5f, by - 1.5f, 2f, 2f, k.col("#b8b8c8")) }
                    else k.rect(c, bx - 1, by - 1, 2f, 2f, android.graphics.Color.argb(160 - tr * 40, 230, 230, 230)) } }
            val e2 = d - FLY_MS
            if (e2 in 0..160) Art.cell("fx", 6, 2, 0, (if (h.third) 40 else 28) * hr)?.let { Art.draw(c, it, tx, ty + 14 + e2 / 30f, false) }
            if (e2 in 120..900) Art.cell("fx", 6, 2, 3, 22 * hr)?.let { Art.draw(c, it, tx, ty + 10 - e2 / 40f, false, (170 * (1 - (e2 - 120) / 780f)).toInt()) }
            if (e2 in 0..600) { val r = java.util.Random(i * 97L + 5)
                for (p in 0 until (if (h.third) 10 else 6)) { val vx = (r.nextFloat() - .5f) * 90; val vy = -40 - r.nextFloat() * 60; val ss = e2 / 1000f
                    k.rect(c, tx + vx * ss, ty + vy * ss + 160 * ss * ss, 2f, 2f, if (p % 2 == 0) k.col("#8a5a2b") else k.col("#d9a35c")) } }
            if (e2 in 60..460) Art.cell("fx", 6, 2, 5, 26 * hr)?.let { Art.draw(c, it, tx + (if (h.first) 14 else -14), wl + 8, false, (255 * (1 - (e2 - 60) / 400f)).toInt()) }
        }
        // 가라앉는 배 물보라와 거품
        if (t > end) { val e = t - end
            if (e < 700) Art.cell("fx", 6, 2, 5, 44 * hr)?.let { Art.draw(c, it, if (b.win) enX else myX, wl + 10, false, (255 * (1 - e / 700f)).toInt()) }
            val bx = if (b.win) enX else myX
            for (j in 0 until 6) { val by = wl + 30 - ((e / 12 + j * 23) % 60); k.rect(c, bx - 18 + j * 7f, by.toFloat(), 2f, 2f, android.graphics.Color.argb(170, 220, 245, 255)) } }
        // 피해 숫자: 튀어나왔다가 위로 뜬다
        for ((i, h) in b.hits.withIndex()) {
            val e2 = t - hitT(i); if (e2 !in 0..760) continue
            val tx = if (h.first) enX else myX
            val base = if (h.third) 19 else 15
            val sz = if (e2 < 110) base + 7 - (e2 / 110f * 7).toInt() else base
            val yy = wl - 44 - (e2 / 60f).coerceAtMost(10f)
            if (h.third) boldText("치명타!", tx, yy - sz - 2, 10, k.col("#ffd23f"))
            boldText("-" + big(h.second), tx, yy, sz, if (!h.first) k.col("#ff7a6a") else if (h.third) k.col("#ffe14d") else k.col("#ffffff"))
        }
        // 이번에 쏜 선원 얼굴
        val crewIds = Game.aboard.filter { it in Game.promoted }
        if (crewIds.isNotEmpty() && cur >= 0 && t < end) { val h = b.hits[cur]; val d = t - shotT(cur)
            if (h.first && d < SHOT_MS) { val n = b.hits.take(cur).count { it.first }; val id = crewIds[n % crewIds.size]
                val pi = Data.workers.indexOfFirst { it.id == id }; val lv = Game.crewLevel(id)
                val pop = if (d < 90) (90 - d) / 18f else 0f
                k.rect(c, 5f, wl + 22 - pop, 26f, 26f, k.INK); k.rect(c, 6f, wl + 23 - pop, 24f, 24f, k.col("#f3e2b8"))
                k.icon(c, faceOf(lv) + pi, 7f, wl + 24 - pop, 2.2f)
                boldText((Fleet.crewOf(id)?.name ?: "") + " 발사!", 34f, wl + 39 - pop, 10, k.col("#fff2c0"), Paint.Align.LEFT) } }
        c.restore()
        // 위: 이름표와 HP 막대
        c.save(); c.clipRect(0f, top, W.toFloat(), top + sceneH)
        val tsz = listOf(10, 9, 8).firstOrNull { k.fits(b.title, it, 140f) } ?: 8
        val tw = (k.width(b.title, tsz) * 1.1f + 14).coerceIn(104f, 150f)
        k.rect(c, W / 2f - tw / 2 - 1, top + 2, tw + 2, 15f, k.INK); k.rect(c, W / 2f - tw / 2, top + 3, tw, 13f, if (b.boss) k.col("#7a2a3a") else k.col("#3a2a1e"))
        k.text(c, k.fit(b.title, tsz, tw - 4), W / 2f, top + 13.5f, tsz, if (b.boss) k.col("#ffb0a0") else k.GOLD, Paint.Align.CENTER, true)
        fun bar(x: Float, f: Float, lag: Float, col: Int, label: String, right: Boolean) {
            val bw = 98f; val y = top + 20
            k.rect(c, x, y, bw, 10f, k.INK); k.rect(c, x + 1, y + 1, bw - 2, 8f, k.col("#3b2a22"))
            fun fill(ff: Float, cc: Int) { val w = (bw - 2) * ff.coerceIn(0f, 1f); k.rect(c, if (right) x + bw - 1 - w else x + 1, y + 1, w, 8f, cc) }
            fill(lag, k.col("#fff4d0")); fill(f, col)
            fill(f, col); k.rect(c, x + 1, y + 1, bw - 2, 2f, android.graphics.Color.argb(70, 255, 255, 255))
            boldText(label, if (right) x + bw else x, y + 19, 8, k.CREAM, if (right) Paint.Align.RIGHT else Paint.Align.LEFT) }
        bar(6f, myHp / b.my, myLag / b.my, k.col("#58c35a"), "내 함대 " + big(myHp.toLong()), false)
        bar(W - 104f, enHp / b.en, enLag / b.en, k.col("#e2483c"), (if (b.boss) "선장 " else "해적 ") + big(enHp.toLong()), true)
        // 시작: 전투 시작!
        if (t < INTRO_MS - 60) { val e = t / INTRO_MS.toFloat(); val sz = (30 - 8 * e).toInt()
            val itx = if (b.boss) b.title.substringBeforeLast(" ") + " 등장!" else "전투 시작!"
            val isz = generateSequence(sz) { it - 1 }.take(16).firstOrNull { k.fits(itx, it, W - 16f) } ?: 14
            boldText(itx, W / 2f, top + 84, isz, if (b.boss) k.col("#ffd23f") else k.col("#fff2c0")) }
        // 끝: 승리 / 패배
        if (t > end + 500) {
            val e = t - end - 500
            if (battleShot != 99) { battleShot = 99; if (b.win) { host.sound("win"); coins(enX, wl, 40) } else host.sound("lose") }
            if (!b.win) k.rect(c, 0f, top, W.toFloat(), sceneH, android.graphics.Color.argb((e / 4).coerceAtMost(90L).toInt(), 20, 10, 30))
            val sl = ((e / 220f).coerceAtMost(1f)); val by = wl - 66
            k.rect(c, 0f, by, W * sl, 40f, android.graphics.Color.argb(215, 25, 16, 10))
            k.rect(c, 0f, by, W * sl, 2f, if (b.win) k.GOLD else k.RED); k.rect(c, 0f, by + 38, W * sl, 2f, if (b.win) k.GOLD else k.RED)
            if (sl >= 1f) {
                val pop = if (e < 330) ((330 - e) / 330f * 8).toInt() else 0
                boldText(if (b.win) "승리!" else "패배…", W / 2f, by + 22, 22 + pop, if (b.win) k.col("#ffd23f") else k.col("#ff6b5b"))
                k.text(c, if (b.win) "금화 +" + big(b.gold) + (if (b.boss) " · 쪽지 +3" else "") else "30분 쉬거나 광고 보고 다시", W / 2f, by + 34, 9, k.CREAM, Paint.Align.CENTER, true)
            }
        }
        c.restore()
    }
    // ---------- 처음 안내: 화면의 진짜 버튼을 손가락으로 짚어 준다 ----------
    private class TStep(val text: String, val target: () -> Rect?, val done: () -> Boolean, val btn: String? = null, val btn2: String? = null, val act: (() -> Unit)? = null, val extra: (() -> String)? = null)
    private var tutTaps = 0
    private var tutBtn: Rect? = null; private var tutBtn2: Rect? = null; private var tutSkip: Rect? = null; private var tutHole: Rect? = null; private var tutOn = false
    private var tutStep: TStep? = null; private var tutKey: String? = null; private var tutShownAt = 0L
    private fun rowRect(f: (URow) -> Boolean): Rect? = upRects.firstOrNull { f(it.second) }?.first
    private fun tabRect(t: Int): Rect? = tabRects.getOrNull(order.indexOf(t))
    private fun seaRect() = Rect(0, (top + 54).toInt(), W, (top + sceneH - 24).toInt())
    private fun powerRect(): Rect { val w = Pix.width("함대 힘 " + "%,d".format(Game.myPower()), 10) + 10; return Rect((W - 8 - w).toInt(), (top + 16).toInt(), W - 4, (top + 33).toInt()) }
    private fun woodCost() = Game.prodCost(Game.woodLv).gold.toLong()
    private val mainSteps: List<TStep> by lazy { listOf(
        TStep("어서 와요, 선장님!\n배를 키우고 해적을 이겨서\n더 먼 바다로 가는 게임이에요.", { null }, { false }, "시작!"),
        TStep("바다를 톡톡 누르면 금화가 나와요.\n빨리 누를수록 더 많이 나와요!", { seaRect() }, { tutTaps >= 8 }, extra = { "톡톡 " + tutTaps + " / 8" }),
        TStep("먼저 [강화] 탭을 눌러요.\n여기서 거의 모든 걸 키워요.", { tabRect(8) }, { tab == 8 }),
        TStep("보물 상자가 와 있어요!\n[열기]를 눌러 금화를 받아요.", { rowRect { it.icon.startsWith("chest") && it.can } }, { !Game.chestReady() }),
        TStep("[나무 캐기]를 올려요.\n섬이 저절로 버는 돈이 늘어요.", { rowRect { it.icon == "wood" } }, { Game.woodLv > 0 }),
        TStep("앱을 꺼도 섬과 배는 일해요.\n다시 오면 모인 금화를 받아요.", { null }, { false }, "알겠어요"),
        TStep("이제 해적 토벌!\n[도전!]을 눌러 싸워 봐요.", { rowRect { it.icon.startsWith("boss") && it.can } }, { battle != null || Game.pStage > 0 || Game.stageWait() > 0 }),
        TStep("[함대 힘]이 해적보다 세면\n잘 이겨요! 배·부품·선원을\n키워서 힘을 올려요.", { powerRect() }, { false }, "알겠어요"),
        TStep("홈 화면 배경으로 두면\n바탕화면에서 섬이 움직여요.", { null }, { false }, "배경화면 설정", "나중에", { host.setWallpaper() }))
    }
    /** 처음 안내가 끝난 뒤, 처음 그 순간이 오면 한 번씩 */
    private fun tipFor(key: String): TStep? = when (key) {
        "promote" -> TStep("견습선원이 선원이 될 수 있어요!\n선원은 키울수록 돈을 더 벌어요.",
            { if (tab != 10) tabRect(10) else rowRect { it.label == "승진" } }, { Data.workers.none { Game.canPromote(it.id) } })
        "crew" -> TStep("선원 레벨을 올려 봐요.\n10레벨마다 판매가 크게 뛰어요!",
            { if (tab != 10) tabRect(10) else rowRect { it.icon.startsWith("face") && it.label == "+1" && it.can } }, { Game.promoted.any { Game.crewLevel(it) > 1 } })
        "ship" -> TStep("배를 키울 수 있어요!\n배가 커지면 힘도 판매도 크게 올라요.",
            { if (tab != 8) tabRect(8) else rowRect { it.icon == "ship" && it.can } }, { upRows().none { it.icon == "ship" && it.can } })
        else -> null
    }
    private fun nextTip(): String? = when {
        "promote" !in Game.tips && Data.workers.any { Game.canPromote(it.id) } -> "promote"
        "crew" !in Game.tips && Game.promoted.isNotEmpty() && Game.promoted.any { Game.crewLevel(it) == 1 && (Game.teachLong(it) ?: Long.MAX_VALUE) <= Game.gold } -> "crew"
        "ship" !in Game.tips && Game.shipLevel < 4 && upRows().any { it.icon == "ship" && it.can } -> "ship"
        else -> null
    }
    private fun tutAdvance() {
        val k = tutKey
        if (k == null) Game.tut++ else Game.tips.add(k)
        if (Game.tut >= mainSteps.size) Game.tut = 99
        tutStep = null; tutKey = null; Game.save()
    }
    private fun drawTut() {
        tutOn = false; tutBtn = null; tutBtn2 = null; tutSkip = null; tutHole = null
        if (battle != null || popup != null) return
        if (tutStep == null) {
            if (Game.tut < mainSteps.size) { tutStep = mainSteps[Game.tut]; tutKey = null }
            else { val t = nextTip() ?: return; tutStep = tipFor(t); tutKey = t }
            tutShownAt = System.currentTimeMillis()
        }
        val st = tutStep ?: return
        if (st.done()) { tutAdvance(); return }
        val hole = st.target()
        if (hole == null && st.btn == null) { if (scrollY > 0f) scrollY = 0f; return }
        tutOn = true; tutHole = hole
        val k = Pix; val now = System.currentTimeMillis()
        val dim = android.graphics.Color.argb(150, 0, 0, 0)
        if (hole == null) k.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), dim)
        else {
            val l = hole.left - 2f; val t = hole.top - 2f; val r = hole.right + 2f; val b = hole.bottom + 2f
            k.rect(c, 0f, 0f, W.toFloat(), t, dim); k.rect(c, 0f, b, W.toFloat(), H - b, dim)
            k.rect(c, 0f, t, l, b - t, dim); k.rect(c, r, t, W - r, b - t, dim)
            val pulse = if ((now / 300) % 2 == 0L) k.GOLD else k.col("#fff2c0")
            k.rect(c, l, t, r - l, 2f, pulse); k.rect(c, l, b - 2, r - l, 2f, pulse); k.rect(c, l, t, 2f, b - t, pulse); k.rect(c, r - 2, t, 2f, b - t, pulse)
        }
        // 말풍선: 짚는 곳이 아래면 위에, 위면 아래에
        val raw = st.text.split("\n") + listOfNotNull(st.extra?.invoke())
        val tw = W - 16f - 46
        val ts = if (raw.all { Pix.fits(it, 10, tw) }) 10 else 9
        val lines = raw.flatMap { if (Pix.fits(it, ts, tw)) listOf(it) else Pix.wrap(it, ts, tw) }
        val bh = 16f + lines.size * 13f + (if (st.btn != null) 24f else 0f)
        val below = hole != null && ((hole.top + hole.bottom) / 2) < H * .45f
        val by = when { hole == null -> (H - bh) / 2f; below -> hole.bottom + 26f; else -> hole.top - 26f - bh }.coerceIn(4f, H - bh - 4)
        val bx = 8f; val bw = W - 16f
        k.rect(c, bx - 1, by - 1, bw + 2, bh + 2, k.INK); k.rect(c, bx, by, bw, bh, k.col("#f3e2b8")); k.rect(c, bx, by, bw, 2f, k.col("#fff8e0"))
        k.rect(c, bx + 5, by + 6, 26f, 26f, k.INK); k.icon(c, "face:0", bx + 6, by + 7, 2.4f)
        for ((i, ln) in lines.withIndex()) k.text(c, k.fit(ln, ts, bw - 46), bx + 37, by + 17 + i * 13, ts, k.INK)
        if (st.btn != null) {
            val yb = by + bh - 22
            if (st.btn2 != null) {
                val w1 = (bw - 46) / 2 - 3
                k.button(c, bx + 37, yb, w1, 17f, st.btn, true); tutBtn = Rect((bx + 37).toInt(), yb.toInt() - 2, (bx + 37 + w1).toInt(), (yb + 19).toInt())
                k.button(c, bx + 43 + w1, yb, w1, 17f, st.btn2, false); tutBtn2 = Rect((bx + 43 + w1).toInt(), yb.toInt() - 2, (bx + 43 + 2 * w1).toInt(), (yb + 19).toInt())
            } else { k.button(c, bx + 37, yb, bw - 46, 17f, st.btn, true); tutBtn = Rect((bx + 37).toInt(), yb.toInt() - 2, (bx + bw - 9).toInt(), (yb + 19).toInt()) }
        }
        // 손가락 대신 통통 튀는 화살표
        if (hole != null) {
            val ax = ((hole.left + hole.right) / 2f); val bob = ((now / 120) % 6).let { if (it < 3) it else 6 - it }.toFloat() * 1.5f
            // 끝이 뾰족한 금색 화살표 (테두리 포함)
            fun arrow(ay: Float, dir: Int) {
                for (i in 0 until 8) { val hw = i.toFloat(); val yy = ay + dir * i * 1.6f
                    k.rect(c, ax - hw - 1, yy - 1, hw * 2 + 3, 2.6f, k.INK) }
                for (i in 0 until 8) { val hw = i.toFloat(); val yy = ay + dir * i * 1.6f; k.rect(c, ax - hw, yy, hw * 2 + 1, 1.6f, k.GOLD) }
                val sy0 = ay + dir * 12.8f; val sy1 = sy0 + dir * 6f
                k.rect(c, ax - 4, minOf(sy0, sy1) - 1, 9f, 8f, k.INK); k.rect(c, ax - 3, minOf(sy0, sy1), 7f, 6f, k.GOLD) }
            if (below) arrow(hole.bottom + 3f + bob, 1) else arrow(hole.top - 3f - bob, -1)
        }
        // 건너뛰기 (처음 안내만)
        if (tutKey == null && Game.tut > 0) {
            val sy = if (below || hole == null) by + bh + 4 else by - 14
            k.rect(c, W - 72f, sy - 1, 64f, 13f, android.graphics.Color.argb(200, 20, 12, 6))
            k.text(c, "안내 건너뛰기", W - 12f, sy + 9, 8, k.col("#e8d8b0"), Paint.Align.RIGHT, true)
            tutSkip = Rect(W - 80, sy.toInt() - 4, W - 4, (sy + 14).toInt())
        }
    }
    private fun tutTap(x: Float, y: Float): Boolean {
        if (!tutOn) return false
        val ix = x.toInt(); val iy = y.toInt()
        tutSkip?.let { if (it.contains(ix, iy)) { Game.tut = 99; Game.tips.addAll(listOf("promote", "crew", "ship")); tutStep = null; Game.save(); return true } }
        tutBtn?.let { if (it.contains(ix, iy)) { tutStep?.act?.invoke(); tutAdvance(); return true } }
        tutBtn2?.let { if (it.contains(ix, iy)) { tutAdvance(); return true } }
        if (System.currentTimeMillis() - tutShownAt < 250) return true
        val h = tutHole ?: return true
        return !Rect(h.left - 3, h.top - 3, h.right + 3, h.bottom + 3).contains(ix, iy)
    }

    private fun drawFloats() {
        drawBattle()
        val now = System.currentTimeMillis()
        // 항해 중 해적을 이기면 앱에서도 금화가 쏟아진다
        if (Game.lastBattleAt != seenBattle) { seenBattle = Game.lastBattleAt
            if (Game.lastBattle == "pirate_win") { host.sound("win"); coins(W / 2f, 100f, 20) } else if (Game.lastBattle == "pirate_lose") host.sound("lose") }
        val dt = ((now - lastCoinT).coerceIn(0, 100)) / 1000f; lastCoinT = now
        coinList.removeAll { now - it.t0 > 1400 }
        for (cn in coinList) { if (now < cn.t0) continue
            cn.vy += 260 * dt; cn.x += cn.vx * dt; cn.y += cn.vy * dt
            Pix.icon(c, "gold", cn.x - 5, cn.y - 5, 1f) }
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
            null, "받기", { host.sound("chest"); coins(W / 2f, 200f, 20); true }, null,
            btn2 = if (Game.tickets > 0) "2배 (쪽지 1)" else null, act2 = { val ok = Game.doubleReport(r); if (ok) { host.sound("win"); coins(W / 2f, 200f, 40) }; ok }, done2 = "금화 2배!")
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
            k.text(c, "선원 탭에서 견습선원을 선원으로 올려요.", W / 2f, py + 78, 9, k.col("#c8b896"), Paint.Align.CENTER)
            return
        }
        if (statSel !in crew) statSel = crew.first()
        val id = statSel!!
        // 선원 얼굴 줄 (작게)
        var fx = 10f
        for (cid in crew) {
            val pi = Data.workers.indexOfFirst { it.id == cid }
            k.slot(c, fx, py + 25, 22f, 22f, Game.ptsFree(cid) > 0, cid == id)
            k.icon(c, faceOf(Game.crewLevel(cid)) + pi, fx + 2, py + 27, 1.8f)
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
        Game.chestReady() -> Game.Goal("d:chest4", "보물 상자 열기", null, true, false)
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
        if (key == "d:chest4") { tab = 8; scrollY = 0f; return }
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
            key.startsWith("crew:") -> Data.workers.indexOfFirst { it.id == key.removePrefix("crew:") }.let { pi -> list.firstOrNull { it.icon.startsWith("face") && it.icon.endsWith(":$pi") } }
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
    fun scrollBy(dy: Float) { if (popup != null) return; snapTo = null; scrollY = (scrollY - dy).coerceIn(0f, maxScroll) }
    /** 손을 떼면 칸 경계에 맞춰 멈춘다 (줄이 반쯤 잘린 채 멈추지 않게) */
    private val snaps = ArrayList<Float>(); private var snapTo: Float? = null
    fun scrollEnd() {
        if (popup != null || maxScroll <= 0f) return
        if (scrollY >= maxScroll - 1f) return
        val best = (snaps.filter { it <= maxScroll } + maxScroll).minByOrNull { Math.abs(it - scrollY) } ?: return
        snapTo = best
    }

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
        val ns = listOf(10, 9, 8).firstOrNull { k.fits(t.name, it, w - 4) }          // 긴 이름은 글자를 조금 줄여서 다 보이게
        val nc = if (t.st == St.LOCKED) k.DIM else k.CREAM
        if (ns != null) k.text(c, t.name, x + w / 2, y + 37, ns, nc, Paint.Align.CENTER)
        else { // 그래도 길면 두 줄로
            val sp = t.name.lastIndexOf(' ', t.name.length / 2 + 2).takeIf { it > 0 } ?: t.name.indexOf(' ').takeIf { it > 0 } ?: (t.name.length / 2)
            val l1 = t.name.substring(0, sp).trim(); val l2 = t.name.substring(sp).trim()
            k.text(c, k.fit(l1, 8, w - 4), x + w / 2, y + 31, 8, nc, Paint.Align.CENTER); k.text(c, k.fit(l2, 8, w - 4), x + w / 2, y + 40, 8, nc, Paint.Align.CENTER) }
        val subColor = when (t.st) { St.READY -> k.GOLD; St.LOCKED, St.OFF -> k.DIM; St.ON -> k.GREEN; else -> k.col("#c8b896") }
        // 긴 이름(예: 멀리 보는 망원경)은 뒤쪽 낱말만 (망원경)
        val subs = generateSequence(t.sub) { v -> v.indexOf(' ').takeIf { it > 0 }?.let { v.substring(it + 1) } }.toList()
        val pick = subs.flatMap { v -> listOf(10, 9, 8, 7).map { v to it } }.firstOrNull { k.fits(it.first, it.second, w - 4) && (it.second >= 8 || it.first == subs.last()) } ?: (subs.last() to 7)
        val ss = pick.second
        k.text(c, k.fit(pick.first, ss, w - 4), x + w / 2, y + 49, ss, subColor, Paint.Align.CENTER)
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
            for ((ic, need, have) in listOf(Triple("wood", cc.wood.toLong(), Game.wood.toLong()), Triple("stone", cc.stone.toLong(), Game.stone.toLong()), Triple("gold", cc.gold.toLong(), Game.gold))) {
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
        if (tutTap(x, y)) return
        battle?.let { b -> if (System.currentTimeMillis() - b.t0 > battleEnd(b) + 700) battle = null; return }
        val ix = x.toInt(); val iy = y.toInt()
        val pop = popup
        if (pop != null) {
            val b2 = btn2Rect
            if (b2 != null && b2.contains(ix, iy)) {
                val ok = pop.act2?.invoke() ?: false
                if (ok && pop.done2 != null) host.toast(pop.done2)
                popup = if (ok && pop.name != "돌아왔어요!") tiles().firstOrNull { it.icon == pop.icon } else null; after = null
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
            Game.addTapGold(n); host.sound("tap"); tutTaps++
            if (combo > 0 && combo % 10 == 0) coins(x, y, 6)
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
