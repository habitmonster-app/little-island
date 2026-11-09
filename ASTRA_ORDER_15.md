# 리틀 아일랜드: 아스트라에게 열다섯 번째 부탁 (배 7~10단계)

아스트라, 14차 바닷속 자료는 build-45에 모두 넣었어.

이번 부탁은 **배를 더 키우는 것**이야.
배를 키우고 선원 능력을 올려서 해적과 싸우는 것이 이 게임의 중심이야. 그래서 이번에 이렇게 바꿨어.
- 배 큰 단계를 6에서 **10단계**로 늘렸어. 부품은 Lv50까지 올라가.
- 새 부품 2개를 넣었어. **망루**는 해적을 먼저 보고 피하는 부품이고, **선실**은 선원 능력치를 키우는 부품이야.
- 다 올린 부품은 금화로 **별 강화**(★1~5)를 할 수 있어.
- 기본 승률을 낮췄어. 지면 짐과 금화를 **약탈**당해.

지금 7~10단계 배는 6단계 그림을 그대로 쓰고 있어. 그래서 새 그림이 필요해.

공통 규칙
- 화면에 나오는 글은 **짧게** 써 줘. 이름은 "귀항호 · ○○" 꼴, 소식 한 줄은 24자 안쪽이야.
- 숫자와 %는 화면 글에 넣지 마. 앱이 붙여.
- 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터 (1개)

### 54_ship_stage7_10.json (7~10단계 배 이름과 소식)
수치는 앱에 이미 넣었어. **이름, 생김새, 소식만** 써 줘.

```json
{ "ships": [
  { "level": 7, "name": "귀항호 · ○○", "look": "생김새 한 줄 (30자 안쪽)", "home_news": "배를 고쳤을 때 소식 (24자 안쪽)", "desc": "배 설명 (16자 안쪽)" }
] }
```
- 7, 8, 9, 10단계 **4척**을 써 줘.
- 지금 임시 이름은 별을 잇는 돛, 바다의 등불, 폭풍을 넘는 배, 돌아오는 바다의 왕이야. 더 좋은 이름이 있으면 바꿔 줘.
- 이야기 톤은 지금과 같게 써 줘. 싸움보다 "멀리 갔다가 함께 돌아오는 배"라는 느낌이야.

---

## 2부. 그림 (5장)

모든 그림은 배경이 순수한 마젠타 #FF00FF야.

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

### 1~4. ship_stage7.png, ship_stage8.png, ship_stage9.png, ship_stage10.png
`ship_stage6.png`와 **똑같은 규칙**이야.
- 각 1024×256, 4×1칸이야 (한 칸 256×256).
- 칸 0~3은 부품을 올리며 바뀌는 겉모습 4단계야.
  - 0: 막 산 모습
  - 1: 선체 보강
  - 2: 돛 정비
  - 3: 대포와 깃발까지 다 꾸민 모습
- 오른쪽을 보게 해 줘. **수면선, 갑판 높이, 칸 안의 배 바닥 위치는 ship_stage6.png와 똑같이** 맞춰 줘. 배 위에 선원과 장식을 겹쳐 그리기 때문이야.
- 꿀갈색 선체, 상아색 돛, 파란 끈, 이름판은 계속 이어 가.
- 단계마다 조금씩 더 크고 당당해져야 해. 칸 안에서 배 폭은 6단계 90% → 7단계 92% → 10단계 98%쯤이야.
  - 7단계 (별을 잇는 돛): 돛대 넷, 돛에 작은 별자리 무늬
  - 8단계 (바다의 등불): 선미에 큰 등불탑, 뱃머리에 철판 덧댐
  - 9단계 (폭풍을 넘는 배): 두꺼운 철판 선체, 대포 구멍 세 줄
  - 10단계 (돌아오는 바다의 왕): 금빛 장식, 돛 다섯 장, 큰 깃발, 가장 화려한 모습

> A 4x1 sprite sheet, 1024x256, each cell 256x256, solid pure magenta #FF00FF background, the same trading ship as the supplied ship_stage6 sheet grown one step bigger: (단계 설명을 영어로), facing right, honey-brown hull, ivory sails, a blue cord and a small name plate kept, four upgrade looks from plain to fully decorated, waterline and deck height exactly matching the supplied ship_stage6 sheet. (+ 공통 스타일 문장)

### 5. part_icons_2.png (새 부품 아이콘 2개)
- 512×256, 2×1칸이야 (한 칸 256×256). `part_icons.png`와 같은 크기와 느낌으로 그려 줘.
- 칸 0: **망루** (돛대 꼭대기의 작은 나무 망대와 망원경)
- 칸 1: **선실** (둥근 창이 있는 아늑한 나무 선실 문)

> A 2x1 icon sheet, 512x256, each cell 256x256, solid pure magenta #FF00FF background, matching the supplied part_icons sheet: a small wooden crow's nest lookout with a brass spyglass; a cozy wooden ship cabin door with a round porthole window. (+ 공통 스타일 문장)

---

## 돌려줄 것
- `data/54_ship_stage7_10.json`
- `art/ship_stage7.png`, `art/ship_stage8.png`, `art/ship_stage9.png`, `art/ship_stage10.png`, `art/part_icons_2.png`
