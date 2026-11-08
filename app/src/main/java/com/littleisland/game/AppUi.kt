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
    }

    /** 좌표는 가로 200도트 그대로 쓰고, 실제 그림은 SC배 크기로 그려서 글자·아이콘이 또렷하게 */
    companion object { const val W = 200; const val SC = 3 }
    var H = 440; private set
    var bitmap: Bitmap = Bitmap.createBitmap(W * SC, H * SC, Bitmap.Config.ARGB_8888); private set
    private var c = Canvas(bitmap).apply { scale(SC.toFloat(), SC.toFloat()) }

    fun resize(h: Int) {
        if (h == H) return
        H = h.coerceAtLeast(360); bitmap = Bitmap.createBitmap(W * SC, H * SC, Bitmap.Config.ARGB_8888); c = Canvas(bitmap).apply { scale(SC.toFloat(), SC.toFloat()) }
    }

    enum class St { READY, NORMAL, DONE, LOCKED, ON, OFF }
    class Tile(val icon: String, val name: String, val sub: String, val st: St, val detail: String,
               val cost: Data.Cost? = null, val btn: String? = null, val act: (() -> Boolean)? = null, val done: String? = null)

    private val tabs = listOf("store" to "짓기", "ship" to "배", "anchor" to "항로", "person" to "선원", "map" to "퍼즐", "book" to "일지", "gear" to "설정")
    var tab = 0
    private var popup: Tile? = null
    private var page = 0
    private val tileRects = ArrayList<Pair<Rect, Tile>>()
    private val tabRects = ArrayList<Rect>()
    private var btnRect: Rect? = null
    private var pageRects = ArrayList<Pair<Rect, Int>>()
    private val src = Rect(); private val dst = Rect()
    private val bp = Paint().apply { isFilterBitmap = false }
    private val top = 16f
    private val sceneH = 184f

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

    private fun tiles(): List<Tile> {
        val l = ArrayList<Tile>()
        when (tab) {
            0 -> {
                val h = Game.nextHouse()
                l.add(if (h == null) Tile("house", "숙소 3/3", "다 지음", St.DONE, "알바가 머무는 숙소예요. 한 채에 한 명씩 묵어요.")
                    else buyTile("house", "숙소 " + Game.houses + "/3", Game.houseReady(), h.cost, "알바가 머무는 숙소예요. 한 채에 한 명씩 묵어요.", h.requires, "짓기", { Game.buildHouse() }, "숙소를 지었어요"))
                val w = Game.nextWarehouse()
                l.add(if (w == null) Tile("store", "창고 " + Game.warehouse + "단계", "가장 큼", St.DONE, "나무·돌을 각각 " + Game.cap() + "개까지 담아요.")
                    else buyTile("store", "창고 " + Game.warehouse + "단계", Game.warehouseReady(), w.second.cost,
                        "지금 각 " + Game.cap() + "개 → 넓히면 각 " + w.first + "개까지 담아요.", w.second.requires, "넓히기", { Game.upgradeWarehouse() }, "창고가 넓어졌어요"))
                l.add(if (Game.dock) Tile("dock", "부두", "다 지음", St.DONE, "짐배가 들르고 큰 배가 항구로 오가요.")
                    else buyTile("dock", "부두", Game.dockReady(), Data.dock?.cost, "부두가 생기면 배가 알아서 항구로 떠나 금화를 벌어 와요.", Data.dock?.requires ?: emptyList(), "짓기", { Game.buildDock() }, "부두가 열렸어요"))
                val fd = Data.field
                l.add(if (Game.field) Tile("field", "작은 밭", "다 지음", St.DONE, fd?.effect ?: "")
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
                for (p in Fleet.parts) {
                    val lv = Game.partLevel(p.id); val n = Game.partNext(p.id)
                    val now = if (lv > 0) Fleet.partText(p, Game.partValue(p.id)) else "아직 없음"
                    val cap = Fleet.maxLevel(Game.shipLevel)
                    val head = p.name + " Lv" + lv + " · " + p.desc + "\n지금: " + now
                    val top = p.levels.size
                    // 키는 Lv20에서 효과가 이미 최대. 그 뒤는 레벨과 배 모습만 오른다
                    val maxed = lv > 0 && p.levels.getOrNull(lv)?.let { it.value <= p.levels[lv - 1].value } == true
                    val note = if (maxed) "\n효과는 이미 최대예요. 이후 정비는 레벨과 배 모습만 올려요." else ""
                    l.add(if (n == null) Tile("part:" + p.icon, p.name + " Lv" + lv, if (lv >= top) "최고" else "배 키우면 더", St.DONE,
                            head + "\n\n배 " + Game.shipLevel + "단계에서는 Lv" + cap + "까지 올려요." + (if (lv < top) " 배를 다음 단계로 고치면 더 올릴 수 있어요." else ""))
                        else Tile("part:" + p.icon, p.name + " Lv" + lv, if (Game.canPay(n.cost)) "올리기!" else Fleet.partText(p, Game.partValue(p.id)),
                            if (Game.canPay(n.cost)) St.READY else St.NORMAL, head + "\n다음: " + Fleet.partText(p, n.value) + note + "\n\n배 " + Game.shipLevel + "단계 최대 Lv" + cap,
                            n.cost, "올리기", { Game.buyPart(p.id) }, p.name + " Lv" + (lv + 1) + "!"))
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
                    val face = "face:" + cr.portrait
                    val port = Data.ports.firstOrNull { it.id == m.port }?.name ?: m.port
                    val rw = listOfNotNull(m.gold.takeIf { it > 0 }?.let { "금화 $it" }, m.wood.takeIf { it > 0 }?.let { "나무 $it" },
                        m.stone.takeIf { it > 0 }?.let { "돌 $it" }, m.maps.takeIf { it > 0 }?.let { "지도 조각 $it" }, m.xp.takeIf { it > 0 }?.let { cr.name + " 경험치 $it" })
                    val info = "특별 항해 · " + cr.name + "\n" + m.summary + "\n\n가는 곳: " + port + "\n시간: 평소의 " + Math.round(m.durMult * 100) + "%" +
                        (if (m.pirateAdd > 0f) " · 해적 +" + Math.round(m.pirateAdd * 100) + "%p" else "") +
                        "\n성공: " + (if (m.mustWin) "해적을 만나면 이겨야 해요" else "무사히 돌아오면 성공") + "\n첫 성공 보상: " + rw.joinToString(", ") + "\n이야기 한 편이 열려요."
                    val miss = Game.missionMissing(m)
                    l.add(when {
                        Game.voy?.mission == m.id -> Tile(face, m.title, "항해 중", St.ON, info)
                        Game.missionQ == m.id -> Tile(face, m.title, "다음 출항", St.ON, info + "\n\n다음 출항 한 번만 " + port + "(으)로 가요. 평소 항로는 그대로예요.",
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
                    val info = "배 꾸미기 · " + Fleet.slotName(slot) + "\n" + (cur?.let { it.name + ": " + it.desc } ?: "지금은 비어 있어요") + (next?.let { "\n누르면 → " + it.name } ?: "") +
                        "\n\n" + list + "\n\n모양만 바뀌어요. 능력치는 그대로예요."
                    l.add(if (own.isEmpty()) Tile("lock", "꾸미기 " + Fleet.slotName(slot), "아직 없음", St.LOCKED, info)
                        else Tile(cur?.let { "deco:" + it.icon } ?: "deco:" + own[0].icon, Fleet.slotName(slot) + " " + own.size + "/" + all.size,
                            cur?.name ?: "비어 있음", if (cur != null) St.ON else St.NORMAL, info, null,
                            if (next != null) "바꿔 달기" else "떼기",
                            { Game.setDeco(slot, next?.id) }, next?.let { it.name + "을(를) 달았어요" } ?: Fleet.slotName(slot) + " 장식을 뗐어요"))
                    for (dc in all) if (!dc.auto && dc.id !in Game.decoOwned && Game.decoReady(dc))
                        l.add(Tile("deco:" + dc.icon, dc.name, "금화 " + dc.gold, if (Game.gold >= dc.gold) St.READY else St.NORMAL,
                            dc.desc + "\n자리: " + Fleet.slotName(slot) + "\n모양만 바뀌어요. 능력치는 그대로예요.", Data.Cost(dc.gold, 0, 0), "사기", { Game.buyDeco(dc.id) }, dc.name + "을(를) 달았어요"))
                }
            }
            2 -> {
                for (p in Data.ports) {
                    val u = Data.unlock(p.id)
                    val open = p.id == "port_haesol" || Game.has(p.id)
                    val info = p.name + "\n나무 " + fmt(p.buyW) + " · 돌 " + fmt(p.buyS) + "금화에 사요\n해적 " + (p.risk * 100).toInt() + "% · 거리 " + fmt(p.distance) + "\n" + p.look
                    l.add(when {
                        open && Game.route == p.id -> Tile("anchor", p.name, "지금 항로", St.ON, info)
                        open && Game.routeOk(p) -> Tile("anchor", p.name, "고르기", St.NORMAL, info, null, "이 항로로", { Game.setRoute(p.id) }, p.name + "로 다음 출항부터 가요")
                        open -> Tile("lock", p.name, "배 " + p.minShip + "단계", St.LOCKED, info + "\n\n배 " + p.minShip + "단계부터 갈 수 있어요")
                        u != null -> buyTile("anchor", p.name, Game.unlockReady(u), u.cost, info, u.requires, "항로 열기", { Game.buyUnlock(p.id) }, p.name + " 항로가 열렸어요")
                        else -> Tile("lock", p.name, "잠김", St.LOCKED, info)
                    })
                }
            }
            3 -> {
                val now = System.currentTimeMillis()
                // 배에 탄 선원 → 대기 선원 → 승진할 수 있는 알바
                val order = Game.aboard + Game.promoted.filter { it !in Game.aboard }
                for (id in order) {
                    val c = Fleet.crewOf(id) ?: continue
                    val on = id in Game.aboard
                    val lv = Game.crewLevel(id)
                    val next = Fleet.xpTo.getOrNull(lv - 1)
                    val skills = c.skills.joinToString("\n") { (if (it.level <= lv) "● " else "○ ") + "Lv" + it.level + " " + it.name + " (" + eff(it.effect, it.value) + ")" }
                    val syn = Fleet.synergies.filter { it.a == id || it.b == id }.joinToString("\n") { s ->
                        val other = Fleet.crewOf(if (s.a == id) s.b else s.a)?.name ?: ""
                        "짝 " + other + ": " + eff(s.effect, s.value) }
                    val pi = Data.workers.indexOfFirst { it.id == id }
                    val full = !on && Game.aboard.size >= Game.slots()
                    l.add(Tile("face:$pi", c.name + " Lv" + lv, if (on) "탑승" else "대기", if (on) St.ON else St.NORMAL,
                        c.role + "\n경험치 " + (if (next != null) Game.crewXp(id).toString() + "/" + next else "최고") + " (항해마다 +" + Fleet.xpPerVoyage + ")\n\n" + skills +
                            (if (syn.isNotEmpty()) "\n\n$syn" else "") + (if (full) "\n\n자리가 꽉 찼어요. 다른 선원을 내리면 태울 수 있어요." else ""),
                        null, if (on) "내리기" else if (full) null else "태우기", { Game.toggleAboard(id) }, if (on) c.name + " 다음 항해는 쉬어요" else c.name + " 다음 항해에 타요"))
                }
                for (w in Data.workers) {
                    if (w.id in Game.promoted || !Game.canPromote(w.id)) continue
                    val c = Fleet.crewOf(w.id) ?: continue
                    val pi = Data.workers.indexOf(w)
                    l.add(Tile("face:$pi", w.name, "선원 승진!", St.READY,
                        w.name + "이(가) 일을 " + Game.completedOf(w.id) + "번 마쳤어요. 선원이 되면 배에 타서 항해할수록 자라요.\n\n" + c.role + " · " + c.roleDesc + "\n" +
                            c.skills.joinToString("\n") { "Lv" + it.level + " " + it.name + " (" + eff(it.effect, it.value) + ")" },
                        Data.Cost(Fleet.promoteGold, 0, 0), "선원으로", { Game.promote(w.id) }, w.name + "이(가) 선원이 됐어요"))
                }
                if (Game.promoted.isEmpty()) l.add(Tile("part:4", "선원 되는 법", "자리 " + Game.slots() + "칸", St.NORMAL,
                    "알바가 섬에서 일을 " + Fleet.promoteContracts + "번 마치면 금화 " + Fleet.promoteGold + "으로 선원이 될 수 있어요. 선원은 배에 타고 항해할 때마다 경험치를 얻어 Lv10까지 자라고, 레벨마다 새 스킬이 열려요. 짝이 맞는 둘을 같이 태우면 보너스가 더 붙어요."))
                for (i in 0 until 3) {
                    val wk = Game.workers.getOrNull(i)
                    l.add(when {
                        wk != null -> { val d = Data.worker(wk.id); Tile("person", d?.name ?: "알바", mmss(wk.expire - now), St.DONE,
                            (d?.name ?: "") + " · " + (d?.role ?: "") + "\n" + (d?.trait ?: "") + "\n" + (d?.look ?: "")) }
                        i < Game.houses -> Tile("house", "빈 숙소", "구하는 중", St.NORMAL, "배가 항구에 가면 빈 숙소만큼 알바를 데려와요.")
                        else -> Tile("lock", "숙소 없음", "짓기 탭", St.LOCKED, "숙소를 지으면 알바가 묵을 수 있어요.")
                    })
                }
                l.add(Tile("auto", "자동 고용", if (Game.autoHire) "켜짐" else "꺼짐", if (Game.autoHire) St.ON else St.OFF,
                    "고용비 " + Data.singleLimit + "금화까지, 하루 " + Data.dailyLimit + "금화, 금화 " + Data.reserveGold + "은 남겨 둬요.", null,
                    if (Game.autoHire) "끄기" else "켜기", { Game.autoHire = !Game.autoHire; Game.save(); true }))
                l.add(Tile("gold", "비싼 알바", if (Game.hireAny) "켜짐" else "꺼짐", if (Game.hireAny) St.ON else St.OFF,
                    "먼 항구의 알바는 고용비가 더 들지만 솜씨가 좋아요.", null, if (Game.hireAny) "끄기" else "켜기", { Game.hireAny = !Game.hireAny; Game.save(); true }))
            }
            4 -> {
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
    private fun fmt(f: Float) = if (f % 1f == 0f) f.toInt().toString() else "%.2f".format(f).trimEnd('0').trimEnd('.')
    private fun mmss(ms: Long): String { val t = (ms / 1000).coerceAtLeast(0); return if (t >= 3600) (t / 3600).toString() + "시간 " + (t % 3600 / 60) + "분" else (t / 60).toString() + "분" }

    // ---------- 그리기 ----------
    fun draw() {
        val k = Pix
        k.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), k.col("#0d0a08"))
        // 위: 장면 (바다 띠 부분을 잘라서)
        val sy = (Scene.WL - sceneH * .55f).toInt()
        src.set(0, sy * Scene.SS, Scene.W * Scene.SS, (sy + sceneH.toInt()) * Scene.SS)
        dst.set(0, top.toInt(), W, (top + sceneH).toInt())
        c.drawBitmap(scene.bitmap, src, dst, bp)
        label(c, 6f, top + 14)
        // 탭
        val ty = top + sceneH + 4
        tabRects.clear()
        for ((i, t) in tabs.withIndex()) {
            val x = 3f + i * 27f + (if (i == tabs.size - 1) 6f else 0f)
            k.rect(c, x, ty, 25f, 25f, k.INK)
            k.rect(c, x + 1, ty + 1, 23f, 23f, if (i == tab) k.col("#c8903a") else k.col("#8a6030"))
            k.rect(c, x + 1, ty + 1, 23f, 2f, if (i == tab) k.col("#f0c060") else k.col("#a87444"))
            k.icon(c, t.first, x + 2.5f, ty + 3f, 2f)
            if (i == 5 && Game.unread() > 0) k.rect(c, x + 20, ty - 1, 5f, 5f, k.RED)
            tabRects.add(Rect(x.toInt(), ty.toInt(), (x + 25).toInt(), (ty + 25).toInt()))
        }
        // 판넬
        val py = ty + 30; val ph = H - py - 26
        k.panel(c, 2f, py, W - 4f, ph)
        k.text(c, tabs[tab].second, W / 2f, py + 17, 12, k.CREAM, Paint.Align.CENTER, true)
        val list = tiles()
        tileRects.clear()
        val tw = 58f; val th = 60f; val gx = 6f
        val x0 = (W - (tw * 3 + gx * 2)) / 2f; val y0 = py + 24
        val perPage = (((ph - 30) + gx) / (th + gx)).toInt().coerceAtLeast(1) * 3
        val pages = (list.size + perPage - 1) / perPage
        if (page >= pages) page = 0
        for ((n, t) in list.drop(page * perPage).take(perPage).withIndex()) {
            val x = x0 + (n % 3) * (tw + gx); val y = y0 + (n / 3) * (th + gx)
            drawTile(t, x, y, tw, th)
            tileRects.add(Rect(x.toInt(), y.toInt(), (x + tw).toInt(), (y + th).toInt()) to t)
        }
        pageRects.clear()
        if (pages > 1) for (pg in 0 until pages) {
            val x = W / 2f - pages * 6 + pg * 12; val y = py + ph - 9
            k.rect(c, x, y, 7f, 5f, if (pg == page) k.GOLD else k.col("#4a3420"))
            pageRects.add(Rect(x.toInt() - 4, y.toInt() - 6, x.toInt() + 11, y.toInt() + 9) to pg)
        }
        // 자원 막대
        val by = H - 22f
        k.rect(c, 20f, by, W - 40f, 18f, k.INK); k.rect(c, 21f, by + 1, W - 42f, 16f, k.col("#5d3a1f")); k.rect(c, 21f, by + 1, W - 42f, 1f, k.col("#a87444"))
        val items = listOf("wood" to Game.wood.toString(), "stone" to Game.stone.toString(), "gold" to "%,d".format(Game.gold), "map" to Game.mapPieces.toString())
        // 금화는 자리를 넓게 (5단계 배는 18만 금화라 숫자가 길다)
        val gap = floatArrayOf(34f, 34f, 60f, 0f)
        var x = 26f
        for ((i, iv) in items.withIndex()) { val (ic, v) = iv; k.icon(c, ic, x, by + 4, 1f); k.text(c, v, x + 12, by + 13, 10, k.GOLD); x += gap[i] }
        popup?.let { drawPopup(it) }
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
        k.text(c, k.fit(t.name, 10, w - 4), x + w / 2, y + 37, 10, if (t.st == St.LOCKED) k.DIM else k.CREAM, Paint.Align.CENTER)
        val subColor = when (t.st) { St.READY -> k.GOLD; St.LOCKED, St.OFF -> k.DIM; St.ON -> k.GREEN; else -> k.col("#c8b896") }
        k.text(c, k.fit(t.sub, 10, w - 4), x + w / 2, y + 49, 10, subColor, Paint.Align.CENTER)
        if (t.cost != null && t.st != St.DONE) k.bar(c, x + 5, y + h - 7, w - 10, 4f, prog(t.cost), if (t.st == St.READY) k.GOLD else k.col("#c8903a"))
    }

    private fun drawPopup(t: Tile) {
        val k = Pix
        k.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), android.graphics.Color.argb(150, 0, 0, 0))
        val lines = k.wrap(t.detail, 10, 150f)
        val costH = if (t.cost != null) 16f else 0f
        val ph = 54f + lines.size * 12f + costH + (if (t.btn != null) 26f else 6f)
        val px = 14f; val pw = W - 28f; val py = ((H - ph) / 2f).coerceAtLeast(20f)
        k.panel(c, px, py, pw, ph)
        k.slot(c, px + 10, py + 10, 28f, 28f)
        k.icon(c, t.icon, px + 14, py + 14, 2f)
        k.text(c, t.name, px + 46, py + 24, 12, k.CREAM, Paint.Align.LEFT, true)
        k.text(c, t.sub, px + 46, py + 36, 10, k.GOLD)
        var y = py + 54
        for (ln in lines) { k.text(c, ln, px + 12, y, 10, k.CREAM); y += 12 }
        t.cost?.let { cc ->
            var x = px + 12
            for ((ic, need, have) in listOf(Triple("wood", cc.wood, Game.wood), Triple("stone", cc.stone, Game.stone), Triple("gold", cc.gold, Game.gold))) {
                if (need <= 0) continue
                k.icon(c, ic, x, y - 4, 1f)
                val s = "%,d".format(need)
                k.text(c, s, x + 12, y + 4, 10, if (have >= need) k.GREEN else k.RED)
                x += 18 + k.width(s, 10)
            }
            y += costH
        }
        btnRect = null
        if (t.btn != null && t.act != null) {
            val ok = t.cost == null || Game.canPay(t.cost)
            val bw = 70f; val bx = px + (pw - bw) / 2
            k.button(c, bx, y - 4, bw, 18f, t.btn, ok)
            if (ok) btnRect = Rect(bx.toInt() - 4, (y - 8).toInt(), (bx + bw).toInt() + 4, (y + 18).toInt())
        }
    }

    // ---------- 누르기 ----------
    fun tap(x: Float, y: Float) {
        val ix = x.toInt(); val iy = y.toInt()
        val pop = popup
        if (pop != null) {
            val b = btnRect
            if (b != null && b.contains(ix, iy)) {
                val ok = pop.act?.invoke() ?: false
                if (ok && pop.done != null) host.toast(pop.done)
                popup = null
            } else popup = null
            return
        }
        for ((i, r) in tabRects.withIndex()) if (r.contains(ix, iy)) { tab = i; page = 0; return }
        for ((r, pg) in pageRects) if (r.contains(ix, iy)) { page = pg; return }
        for ((r, t) in tileRects) if (r.contains(ix, iy)) { popup = t; return }
    }

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
