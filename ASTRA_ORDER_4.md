# 작은 섬: 아스트라에게 한 번에 보내는 부탁 (4차)

아스트라, 이 파일 하나에 지금 필요한 것을 전부 모았어. 위에서부터 차례로 만들어서 **마지막에 zip 하나**로 돌려줘.

- 그림은 `art/` 폴더에 png로, 이름은 굵은 글씨 그대로
- 데이터는 `data/` 폴더에 json으로
- 그림은 한 장씩 그려서 모아 줘 (프로그램 코드는 고치지 마)
- 화풍은 함께 올린 참고 그림(배, 주민, 알바)과 똑같이

## 이번에 바뀌는 방향

게임의 중심은 **배 업그레이드**와 **선원 스킬**이야. 마을도 중요하지만 배와 선원이 먼저야.

1. **배**: 지금 큰 단계 4개(02_ships.json)는 그대로 두고, 그 사이에 부품 4개를 자주 올려. 선체(짐칸), 돛(속도), 대포(해적 승률), 키(좋은 사건 확률). 부품을 올릴 때마다 배 모습도 조금씩 달라져.
2. **선원**: 알바 12명은 섬에서 일하다가 친해지면(계약 몇 번 완료) **선원으로 승진**해서 배에 타. 배에는 선원 자리가 있고(1단계 2자리 → 4단계 5자리), 선원은 항해할 때마다 경험치를 얻어 레벨이 오르고 새 스킬이 열려.
3. 주민 3명(단, 보리, 해온)은 섬에 남아. 이야기도 그대로야.

---

## 1부. 데이터 (json)

### 19_ship_parts.json
```json
{
  "parts": [
    { "id": "hull", "name": "선체", "stat": "cargo_bonus", "desc": "짐칸이 넓어져요",
      "levels": [ { "level": 1, "value": 0.05, "cost": { "gold": 120, "wood": 30, "stone": 10 }, "requires_ship_level": 1 } ] }
  ],
  "max_level_per_ship_level": { "1": 5, "2": 10, "3": 15, "4": 20 },
  "ship_upgrade_needs_parts_level": { "2": 5, "3": 10, "4": 15 },
  "look_stage_rule": "부품 4개 중 절반 레벨을 넘은 개수(0~3)로 배 모습 단계를 정해요"
}
```
- 부품: hull(짐칸 +%), sail(항해 시간 -%), cannon(해적 승률 +), helm(좋은 사건 확률 +, 나쁜 사건 피해 -)
- 레벨 1~20. 처음 몇 레벨은 몇 분 만에 올릴 수 있게 싸게, 뒤로 갈수록 비싸게
- 지금 첫 큰 업그레이드(금화 1800)는 너무 멀어서, 부품 업그레이드가 그 사이를 자주 채워 줘야 해

### 20_crew.json
```json
{
  "slots_by_ship_level": { "1": 2, "2": 3, "3": 4, "4": 5 },
  "promote_rule": { "completed_contracts": 2, "cost_gold": 200 },
  "xp_per_voyage": 10, "xp_to_next": [30, 60, 100, 150, 220, 300, 400, 520, 660],
  "crew": [
    { "worker_id": "yeoul", "crew_role": "갑판장", "role_desc": "짐 싣기",
      "skills": [
        { "level": 1, "id": "tight_knots", "name": "단단한 매듭", "effect": "cargo_bonus", "value": 0.05, "desc": "짐을 5% 더 실어요" },
        { "level": 5, "id": "...", "name": "...", "effect": "...", "value": 0, "desc": "..." },
        { "level": 10, "id": "...", "name": "...", "effect": "...", "value": 0, "desc": "..." }
      ],
      "deck_line": "매듭은 제가 묶어 둘게요." }
  ],
  "effects_list": ["cargo_bonus", "speed_bonus", "pirate_win_bonus", "sale_bonus", "event_good_bonus", "repair_after_loss", "treasure_find_bonus", "wood_price_bonus", "stone_price_bonus"],
  "synergy": [ { "ids": ["yeoul", "dori"], "name": "...", "effect": "...", "value": 0, "desc": "같이 타면" } ]
}
```
- 알바 12명 모두에게 선원 역할과 스킬 3개(레벨 1, 5, 10)를 줘. 역할이 겹치지 않게 골고루
- 같이 태우면 생기는 짝 보너스(synergy) 4~6개
- 선원이 해적에게 지면 다치지 않아. 그냥 "짐을 잃었어요"만 (지금 규칙 그대로)
- 서린 선장은 이야기 속 인물이니, 선원 자리에 넣을지 말지는 이야기에 맞게 정해 줘

### 15_legal.json, 16_sounds.json
to-astra-round2.md에 적은 그대로야 (출시 서류, 소리 목록). 이번 zip에 같이 넣어 줘.

---

## 2부. 그림 (png)

### 배와 선원 (새로 필요)

**ship_stages.png**: 4행 × 4열
> A sprite sheet of our small wooden sailing ship facing right, 4 rows and 4 columns of equal cells, bottoms aligned. Each row is one ship size, exactly matching the four ships in the supplied ships picture (row 1 smallest one-sail boat, row 4 three-masted ship), always keeping the blue cord on the bow. Columns show gradual part upgrades of that same ship: column 1 plain; column 2 reinforced hull with new planks and metal corners; column 3 also better clean sails with a teal trim; column 4 also an extra polished cannon and a small flag on top. Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**crew_deck_a.png**: 6행 × 4열 (알바 1~6번이 배 위에서 일하는 모습)
> A character sprite sheet, 6 rows and 4 columns of equal cells, chibi sailors facing right, exactly the same six characters and order as the supplied workers_all_a sheet. Columns: pulling a rope, turning a ship's wheel, loading a small cannon, cheering with one arm up. Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**crew_deck_b.png**: 6행 × 4열 (알바 7~12번)
> Same as before but for the six characters of the supplied workers_all_b sheet, same order. Columns: pulling a rope, turning a ship's wheel, loading a small cannon, cheering with one arm up. Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**part_icons.png**: 4열 × 2행
> A grid of eight game icons, 4 columns and 2 rows of equal square cells, each centered: a ship hull with planks, a cream sail, a small cannon, a ship's wheel, a sailor's cap (crew slot), a green up arrow, a skill book with a star, a gold star badge. Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**skill_icons.png**: 4열 × 4행
> A 4x4 grid of sixteen small skill icons in equal square cells, each centered: knotted rope, wind swirl, cannonball, coin stack, four-leaf clover, hammer and plank, treasure chest, log, stone, compass, spyglass, cooking pot, bandage roll, lantern, map, handshake. Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

### 지난번에 남은 4장

**worker_portraits.png**
> Head-and-shoulders portraits of twelve different young workers, 4 columns and 3 rows of equal square cells, each face centered, matching the twelve characters of the supplied worker sprite sheets in the same order (row by row). Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**portraits.png**
> 네가 쓴 이야기 '돌아오는 바다' 속 인물 모습 그대로 그려 줘. Head-and-shoulders portraits of the story characters, 4 columns and 4 rows of equal square cells, each face centered, in this order: 서린 (captain), 모라, 연두, 가람, 구록 (cartoonish, not scary), 우란, 소아, 이솔, 도겸, 수빈, 태루, 새나, 무진, the ferryman of 갈대만, and two empty cells. Solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**events.png**
> Small square scene illustrations, 4 columns and 3 rows of equal cells, full color inside each cell: surveying a tiny treasure island, a pile of treasure gold, villagers tying down crates before a storm, a small ship in a storm at sea, a lighthouse lighting up for the first time, the first day of a harbor market, a traveling merchant boat, a bundle of spring letters with flowers, a summer shade awning day, an autumn boat repair day, a winter night with warm window lights, and one empty magenta cell. Thin magenta gaps between cells. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

**app_icon_fg.png**
> The same small island with one tree, pier lantern and cream sailboat as the supplied app icon, but only the island and boat, centered and small inside the middle 60% of a square 1:1 canvas, on a solid flat #FF00FF magenta background. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

---

## 돌려줄 zip 모양

```
little-island-order4.zip
  art/ship_stages.png, crew_deck_a.png, crew_deck_b.png, part_icons.png, skill_icons.png,
      worker_portraits.png, portraits.png, events.png, app_icon_fg.png
  data/15_legal.json, 16_sounds.json, 19_ship_parts.json, 20_crew.json
```
