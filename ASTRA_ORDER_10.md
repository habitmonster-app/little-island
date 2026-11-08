# 리틀 아일랜드: 아스트라에게 열 번째 부탁

아스트라, 9차 자료는 build-35에 모두 넣었어.
- 오늘의 주문 30개: 퍼즐 탭 이름을 "할 일"로 바꾸고 맨 위에 넣었어. 매일 3개가 나오고, 주간 상자도 돼.
- 세 번째 특별 항해 12개와 마지막 이야기, 그림도 들어갔어.
- Lv11 이상 선원은 배 위에서도 코트를 입어.

이번 부탁은 **배 위가 더 살아 보이게** 하는 거야. 글보다 **그림과 움직임**으로.
1. 선원이 항해 중에 겪는 작은 일 (짧은 카드)
2. 훈련 퍼즐을 더 많이, 조금 더 어렵게
3. 바다 동물과 선원 감정 표시 그림

글은 **짧게** 부탁해. 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터

### 33_crew_sea_events.json (선원 항해 사건)
그 선원이 배에 타고 있을 때만 가끔 일어나는 작은 일이야. 귀항 카드에 한 줄로 나오고, 일지에는 쌓지 않아.
선원 12명 × 3개, 모두 **36개**.

```json
{ "chance_per_voyage": 0.25,
  "events": [ {
    "id": "crew_ev_yeoul_1",
    "crew": "yeoul",
    "min_crew_level": 1,
    "card_title": "카드 제목 (12자 안쪽)",
    "card_line": "카드 한 줄 (24자 안쪽)",
    "emote": "heart | note | sweat | star | zzz | surprise | question | angry",
    "effect": { "kind": "none | gold | wood | stone | crew_xp | tickets", "value": 0 }
  } ] }
```
**규칙**
- 선원마다 3개: Lv1, Lv5, Lv10에서 하나씩 열리게 (min_crew_level).
- effect는 **작게**. gold는 20~80, wood·stone은 5~15, crew_xp는 5~15, tickets는 1. 36개 중 12개쯤은 effect 없이 "none"으로.
- 그 선원의 역할과 성격에 맞게. 예: 요리사 들은 맛있는 냄새, 망루지기 빛여는 먼 섬을 봄.
- emote는 아래 그림 2의 감정 표시 이름 중 하나야. 사건이 일어나면 그 선원 머리 위에 잠깐 떠.
- card_title, card_line에는 숫자를 넣지 마. 보상은 앱이 붙여.

### 34_training_puzzles_2.json (훈련 퍼즐 더)
지금 훈련 퍼즐은 9개라 금방 반복돼. **30개** 더 써 줘. 28번 자료의 training_puzzle.puzzles와 **같은 모양**이야.
- 2×3 판(칸 6개) 15개, **3×3 판(칸 9개) 15개**.
- 3×3 판은 줄마다 나무, 돌, 곡식 순서야. target 예: `["wood","stone","grain","wood","stone","grain","wood","stone","grain"]`
- 칸 번호는 왼쪽 위부터 가로로 0, 1, 2, …야.
- optimal_swaps는 1~4. 3×3 판은 최소 2번 이상 바꿔야 풀리게.
- 모든 퍼즐은 최대 4번 바꾸기 안에 풀려야 해. solution_swaps도 같이 적어 줘.
- id는 `training_cargo_10`부터 이어서.

---

## 2부. 그림

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

### 1. sea_creatures.png (지나가는 바다 동물)
홈 화면 바다에 가끔 지나가는 동물이야. 수면(물선)에 맞춰서 그려.
- 1024×1024, 4열 × 4행 (한 칸 256×256), 배경은 마젠타 #FF00FF.
- 한 줄이 한 동물의 움직임 4장이야. 모두 **오른쪽에서 왼쪽으로** 가는 모습.
  - 1줄 돌고래: 물속 → 튀어 오름 → 가장 높이 → 물에 들어감
  - 2줄 고래: 등이 보임 → 물 뿜기 시작 → 물 높이 뿜음 → 꼬리
  - 3줄 바다거북: 물 위에서 헤엄치는 4장 (앞발 움직임)
  - 4줄 날치 떼: 물 위로 나는 작은 물고기 서너 마리 4장
- 수면은 각 칸 **가운데 높이(아래에서 128px)**에 오게. 그 아래는 물속이니 동물 몸이 반쯤 잠긴 모습으로.
- 물 자체는 그리지 말고 동물과 물보라만.

> A 4x4 sprite sheet, 1024x1024, each cell 256x256, solid pure magenta #FF00FF background, animals moving from right to left, the sea surface line is at the vertical middle of every cell (do not draw the water itself, only the animal and small splashes). Row 1: a dolphin jumping in four frames (under water, leaping up, top of the arc, diving in). Row 2: a friendly whale in four frames (back showing, starting to spout, tall water spout, tail up). Row 3: a sea turtle swimming at the surface, four frames of flipper motion. Row 4: three or four small flying fish gliding over the surface, four frames. (+ 공통 스타일 문장)

### 2. crew_emotes.png (선원 감정 표시)
선원 머리 위에 잠깐 뜨는 작은 말풍선 그림이야. 글자 대신 이걸로 기분을 보여 줄 거야.
- 512×256, 4열 × 2행 (한 칸 128×128), 배경은 마젠타 #FF00FF.
- 모두 **흰 동그란 말풍선 안에** 그림 하나, 꼬리는 아래 가운데.
- 순서: heart(빨간 하트), note(음표), sweat(파란 땀방울), star(노란 별) / zzz(잠 표시 구름 세 개, 글자 없이), surprise(노란 느낌표 모양 번개), question(물음표 모양 대신 갈고리 구름), angry(빨간 화난 표시)
- 작게 봐도 알아보게 굵고 단순하게.

> A 4x2 sprite sheet, 512x256, each cell 128x128, solid pure magenta #FF00FF background. Each cell is a small round white speech bubble with a tail at the bottom center, containing one bold simple symbol: red heart, music note, blue sweat drop, yellow star; three small sleepy puffs, a yellow lightning spark, a curly hook-shaped puff for a puzzled feeling, a red anger mark. Thick outlines, readable at small size. (+ 공통 스타일 문장)

### 3. crew_event_badge.png (귀항 카드 선원 배지)
선원 사건 카드 왼쪽에 붙일 작은 얼굴 배지야.
- 1024×768, 4열 × 3행 (한 칸 256×256), 배경은 마젠타 #FF00FF. 칸 순서는 선원 id 순서와 같게.
- 각 칸: 둥근 나무 액자 안에 그 선원의 웃는 얼굴. 얼굴은 `worker_portraits.png`와 같게.
- 액자 아래에 작은 리본.

> A 4x3 grid of twelve round wooden badge frames with a small ribbon at the bottom, 1024x768, each cell 256x256, solid pure magenta #FF00FF background, each badge showing the smiling face of one crew member, same faces, hair and order as the supplied worker_portraits sheet. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order10.zip
  data/33_crew_sea_events.json, 34_training_puzzles_2.json
  art/sea_creatures.png, crew_emotes.png, crew_event_badge.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
