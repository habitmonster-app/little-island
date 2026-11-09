# 리틀 아일랜드: 아스트라에게 열두 번째 부탁 (큰 묶음)

아스트라, 11차 자료는 build-39에 모두 넣었어.
- 새 우두머리 4척: 이제 우두머리가 12척이야. 목록은 배 단계별로 묶어서 보여 줘.
- 꾸미기 3탄 16개, 부적 2탄 12개(모두 36개), 오늘의 주문 60개.
- 짝 사건 12개는 두 선원이 같이 탔을 때만 일어나. Lv11 이상 선원은 코트 배지를 달고 새 말을 해.

이번에도 **한 번에 많이** 부탁할게. 데이터 6개, 그림 7장이야.
이번 주제는 **바다와 섬이 계절과 날씨로 살아 움직이게** 하는 거야. 오래 하는 사람에게 줄 **선장 칭호**도 같이 부탁해.

공통 규칙
- 화면에 나오는 글은 **짧게** 써 줘. 제목 12자, 한 줄 24자, 짧은 설명 16자, 대사 18자 안쪽이야.
- 숫자와 %는 글에 넣지 마. 앱이 붙여.
- 다치거나 죽는 일 없이, 지금 이야기 톤과 같게 써 줘.
- 선원 id: yeoul, mungi, sori, nuri, bada, geonu, dori, haneul, bityeo, deul, saei, onyu
- 항구 id: port_haesol, port_saltflat, port_pottery, port_windneck, port_pineshade, port_redanchor, port_fogferry, port_glasscape, port_reedbay, port_paperwater, port_whitesail
- 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터 (6개)

### 41_captain_titles.json (선장 칭호, 24개)
오래 한 기록에 따라 열리는 이름표야. 고르면 홈 화면 위 글줄 앞에 작게 붙어. **효과는 없어** (보기만 하는 보상).

```json
{ "titles": [ {
  "id": "title_first_voyage",
  "name": "칭호 이름 (8자 안쪽)",
  "short": "어떻게 얻는지 한 줄 (16자 안쪽, 숫자 없이)",
  "tier": "bronze | silver | gold",
  "icon_index": 0,
  "condition": { "type": "counter_at_least", "counter": "voyages", "value": 10 }
} ] }
```
- 동 10개, 은 8개, 금 6개로 써 줘. icon_index는 아래 그림 1의 0~23칸이야. 동 0~9, 은 10~17, 금 18~23 순서로.
- condition은 **아래 type만** 써 줘 (앱이 이것만 판정해).

| type | 예 |
|---|---|
| counter_at_least | `{"type":"counter_at_least","counter":"voyages","value":50}` |
| ship_level_at_least | `{"type":"ship_level_at_least","value":4}` |
| any_promoted_crew_level_at_least | `{"type":"any_promoted_crew_level_at_least","value":15}` |
| promoted_count_at_least | `{"type":"promoted_count_at_least","value":12}` |
| boss_first_win | `{"type":"boss_first_win","boss_id":"boss_hook"}` |

- counter는 이것만 써 줘.
  - voyages: 항해 수
  - treasures: 보물 수
  - finds: 바다에서 주운 수
  - boss_wins: 이긴 우두머리 수, 최대 12
  - souvenirs: 기념품 수, 최대 33
  - charms: 가진 부적 수, 최대 36
  - missions: 끝낸 특별 항해 수, 최대 36
  - decos: 가진 꾸미기 수, 최대 48
- 쉬운 것(항해 10번)부터 아주 오래 해야 하는 것(항해 1000번, 우두머리 12척 모두)까지 고르게 나눠 줘.

### 42_sea_weather.json (바다 날씨, 10종)
항해를 떠날 때 날씨가 하나 정해져. 홈 화면 바다에 그 날씨가 보이고, 귀항 카드에 한 줄이 붙어.

```json
{ "weather": [ {
  "id": "weather_clear",
  "name": "이름 (6자 안쪽)",
  "weight": 30,
  "seasons": ["spring","summer","autumn","winter"],
  "time": "any | day | night",
  "min_ship_level": 1,
  "card_line": "귀항 카드 한 줄 (24자 안쪽)",
  "crew_line": "선원이 하는 말 (18자 안쪽)",
  "effect": { "kind": "none | speed | cargo | pirate_chance | find_chance", "value": 0.0 },
  "fx": { "layer": "none | rain | snow | petals | leaves | fog | stars | sunrays | aurora | rainbow", "icon_index": 0 }
} ] }
```
- 10종을 써 줘: 맑음, 순풍, 잔잔함, 가랑비, 소나기, 안개, 별밤, 무지개, 눈발, 오로라.
- 맑음은 effect "none"이고 weight가 가장 커. 오로라, 무지개는 드물게 해 줘.
- effect 값은 **작게** 해 줘.
  - speed, cargo: -0.05~+0.08 (+는 빨라지거나 짐이 늘어남)
  - pirate_chance: -0.05~+0.05
  - find_chance: +0.05~+0.15
- 나쁜 날씨도 하나쯤은 좋은 점이 있게 해 줘. 예: 안개는 느리지만 해적을 덜 만나요.
- 눈발은 winter만, 별밤과 오로라는 night만이야.
- fx.layer는 그림 2(rain, snow, petals, leaves, fog)나 그림 3 1줄(stars, sunrays, aurora, rainbow)의 이름이야. icon_index는 그림 3의 날씨 아이콘 번호(0~9)야.

### 43_port_requests.json (항구 의뢰, 33개)
항구마다 "이거 좀 가져다줘" 하는 부탁이야. 그 항구로 항해할 때 짐을 실어 가면 끝나.

```json
{ "rules": { "active_max": 3, "refresh_hours": 6 },
  "requests": [ {
    "id": "req_haesol_1",
    "port": "port_haesol",
    "client": "부탁한 사람 (6자 안쪽, 예: 등대지기 할머니)",
    "title": "의뢰 이름 (10자 안쪽)",
    "short": "한 줄 (16자 안쪽, 숫자 없이)",
    "need": { "wood": 40, "stone": 0 },
    "reward": { "gold": 300, "tickets": 1 },
    "min_ship_level": 1,
    "thanks": "끝냈을 때 한마디 (24자 안쪽)"
  } ] }
```
- 항구 11곳 × 3개 = **33개**.
- 가까운 항구(해솔항, 소금여울, 도기포)는 need를 작게(20~60), 먼 항구(갈대만, 종이물, 백선항)는 크게(120~300) 해 줘.
- reward의 금화는 그만큼 짐을 그냥 파는 값의 1.5~2배쯤, 쪽지는 0~2장으로 해 줘.
- 03_ports.json의 min_ship_level보다 낮게 쓰지 마.
- 항구 분위기에 맞게 써 줘. 예: 도기포 가마 장인은 장작, 유리곶 유리공방은 돌.

### 44_crew_pair_talks.json (짝 선원 갑판 대화)
20_crew.json의 synergy 짝 6쌍이 같이 탔을 때, 배 위에서 말풍선 두세 개로 주고받는 짧은 대화야.

```json
{ "talks": [ {
  "id": "talk_yeoul_dori_1",
  "pair": ["yeoul","dori"],
  "min_crew_level": 1,
  "lines": [ { "who": "yeoul", "text": "18자 안쪽" }, { "who": "dori", "text": "18자 안쪽" } ]
} ] }
```
- 짝 6쌍 × 5개 = **30개**. 쌍마다 min_crew_level 1, 1, 5, 10, 12로 하나씩 써 줘.
- lines는 2~3줄이야. 서로 이름을 부르거나 장난치는 말로, 그 둘의 사이가 느껴지게 해 줘.
- 짝: yeoul·dori, mungi·onyu, sori·deul, nuri·saei, bada·haneul, geonu·bityeo

### 45_seasons.json (섬의 계절)
실제 날짜에 따라 섬 계절이 바뀌어. 이미 있는 `season_deco.png`(4×4, 줄마다 봄·여름·가을·겨울 장식)를 이제 섬과 부두에 올릴 거야.

```json
{ "seasons": [ {
  "id": "spring",
  "name": "봄",
  "months": [3,4,5],
  "deco_cells": [0,1,2,3],
  "island_line": "섬 주민 한마디 (18자 안쪽)",
  "crew_lines": { "yeoul": "18자 안쪽", "...": "12명 모두" },
  "festival": { "id": "fest_spring", "name": "축제 이름 (8자 안쪽)", "days": [4,1,4,7], "short": "16자 안쪽", "reward": { "tickets": 3, "gold": 300 } }
} ] }
```
- 계절 4개 × 선원 12명 말 = 48줄이야.
- deco_cells는 season_deco.png의 칸 번호야. 봄 0~3, 여름 4~7, 가을 8~11, 겨울 12~15 중에서 골라 줘 (겨울은 15칸이 눈 더미라 그대로 써도 돼).
- festival은 계절마다 1개, 3~7일짜리야. days는 [시작 월, 시작 일, 끝 월, 끝 일]이야. 그 기간에 처음 들어오면 보상을 한 번 받아. 보상은 쪽지 3장, 금화 500 안쪽으로 해 줘.

### 46_island_visitors.json (섬에 들르는 손님 배, 8종)
홈 화면 섬 부두에 가끔 작은 배가 들러. 누르면 짧은 거래를 하나 제안해.

```json
{ "rules": { "every_minutes": [90, 240], "stay_minutes": 30 },
  "visitors": [ {
    "id": "visitor_mail",
    "name": "손님 이름 (8자 안쪽)",
    "sprite_index": 0,
    "face_index": 0,
    "hello": "인사 (24자 안쪽)",
    "offers": [ { "give": { "wood": 30 }, "get": { "gold": 120 }, "short": "16자 안쪽" } ],
    "bye": "거래 뒤 한마디 (18자 안쪽)",
    "min_ship_level": 1
  } ] }
```
- 8종을 써 줘: 우편선, 떠돌이 상인, 고기잡이 할아버지, 지도 그리는 학자, 꽃배달 소녀, 등대 수리공, 이웃 섬 아이들, 수상한(하지만 착한) 골동품 장수.
- 손님마다 offers는 1~3개야. give와 get에는 wood, stone, gold, tickets만 써 줘.
- 거래는 조금 이득인 정도로 해 줘 (받는 값이 주는 값의 1.1~1.4배). 쪽지는 손님 한 번에 최대 1장이야.
- sprite_index는 그림 5, face_index는 그림 6의 0~7칸이야.

---

## 2부. 그림 (7장)

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

모든 그림은 배경이 순수한 마젠타 #FF00FF야.

### 1. title_badges.png (선장 칭호 배지 24개)
- 1024×1536, 4×6칸이야 (한 칸 256×256). 칸 순서는 41번 icon_index와 같게 해 줘.
- 0~9칸은 동색 테두리, 10~17칸은 은색 테두리, 18~23칸은 금색 테두리에 은은한 빛이야.
- 배지 안 그림은 칭호 뜻에 맞게 그려 줘 (닻, 나침반, 보물 상자, 해골 깃발 위 금관, 갈매기, 별, 큰 배 등).

> A 4x6 icon sheet, 1024x1536, each cell 256x256, solid pure magenta #FF00FF background, twenty-four round captain title badges centered in their cells: the first ten with bronze rims, the next eight with silver rims, the last six with gold rims and a soft glow, each badge showing one simple sea symbol such as an anchor, compass, treasure chest, crowned pirate flag, seagull, star, ship wheel or big sailing ship. (+ 공통 스타일 문장)

### 2. weather_fx.png (날씨 효과 겹쳐 그리기)
홈 화면 바다 위에 겹쳐서 움직일 그림이야.
- 1024×1280, 4열 × 5행이야 (한 칸 256×256). 한 줄이 한 효과이고, 4칸은 움직임 4장이야.
- 1줄 rain: 비스듬한 빗줄기. **위아래, 좌우가 이어지게** (타일처럼) 그려 줘.
- 2줄 snow: 눈송이. 이어지게 그려 줘.
- 3줄 petals: 꽃잎. 이어지게 그려 줘.
- 4줄 leaves: 단풍잎. 이어지게 그려 줘.
- 5줄 fog: 옅은 안개 덩어리 (반투명처럼 보이게 밝은 회백색 점무늬).
- 물, 하늘, 땅은 그리지 마. 효과만 그려 줘.

> A 4x5 sprite sheet, 1024x1280, each cell 256x256, solid pure magenta #FF00FF background, weather overlay effects only (no sky, no sea, no ground), four animation frames per row, rows 1 to 4 must tile seamlessly in every direction: row 1 slanted rain streaks, row 2 falling snowflakes, row 3 drifting pink blossom petals, row 4 falling orange autumn leaves, row 5 soft pale grey fog puffs drawn with a dither pattern. (+ 공통 스타일 문장)

### 3. weather_sky.png (하늘 효과와 날씨 아이콘)
- 1024×1024, 4×4칸이야 (한 칸 256×256).
- 1줄은 하늘에 거는 큰 효과야. stars(작은 반짝 별 무리), sunrays(구름 사이 햇살), aurora(초록·보라 빛 띠), rainbow(무지개 반원) 순서야.
- 2~4줄은 날씨 아이콘 10개야. 작은 둥근 나무 틀 안에 그려 줘. 순서는 맑음, 순풍, 잔잔함, 가랑비, 소나기, 안개, 별밤, 무지개, 눈발, 오로라야. 마지막 두 칸은 비워 둬.
- 42번 fx.icon_index는 이 아이콘 순서(0~9)야.

> A 4x4 sheet, 1024x1024, each cell 256x256, solid pure magenta #FF00FF background. Row 1, large sky effects: a cluster of small twinkling stars, sun rays breaking through clouds, a green and violet aurora ribbon, a rainbow half-arc. Rows 2 to 4, ten weather icons in small round wooden frames, in order: sunny, fair wind with swirls, calm flat sea, light drizzle, heavy shower, fog, starry night, rainbow, light snow, aurora; the last two cells stay empty. (+ 공통 스타일 문장)

### 4. sea_creatures_2.png (지나가는 바다 동물 2탄)
`sea_creatures.png`와 **똑같은 규칙**이야.
- 1024×1024, 4×4칸이야 (한 칸 256×256). 모두 오른쪽에서 왼쪽으로 가고, 수면은 칸 **가운데 높이(아래에서 128px)**야. 물은 그리지 말고 동물과 물보라만 그려 줘.
- 1줄 물개: 헤엄 → 머리 내밈 → 공 튕기기 → 잠수
- 2줄 해파리 떼: 수면 바로 아래에서 둥둥 4장 (반쯤 보이게)
- 3줄 갈매기 떼: 수면에 앉아 둥둥 → 날개 폄 → 날아오름 → 낮게 남
- 4줄 작은 문어: 수면 위로 고개 → 다리 흔들기 → 먹물 대신 물방울 → 쏙 들어감

> A 4x4 sprite sheet, 1024x1024, each cell 256x256, solid pure magenta #FF00FF background, animals moving from right to left, the sea surface line is at the vertical middle of every cell (do not draw the water itself, only the animal and small splashes). Row 1: a seal in four frames (swimming, head up, bouncing a ball, diving). Row 2: a few jellyfish bobbing just under the surface, four frames. Row 3: a small flock of seagulls floating, spreading wings, taking off, flying low. Row 4: a small friendly octopus peeking up, waving arms, blowing a water bubble, ducking under. (+ 공통 스타일 문장)

### 5. visitor_boats.png (손님 배 8척)
- 1536×512, 4×2칸이야 (한 칸 384×256). 칸 순서는 46번 sprite_index와 같게 해 줘.
- 모두 **왼쪽을 보게** 해 줘. 수면선은 칸 아래에서 약 6%로 모든 칸 같게 해 줘. 우리 배보다 훨씬 작은 배야.
- 순서: 우편선(빨간 우체통 깃발), 떠돌이 상인(짐 가득 실은 작은 범선), 고기잡이 할아버지(작은 고깃배와 그물), 학자(지도 두루마리와 망원경 실은 작은 배), 꽃배달(꽃바구니 가득한 노 젓는 배), 등대 수리공(사다리와 공구 실은 배), 이웃 섬 아이들(색칠한 작은 뗏목), 골동품 장수(보라 천막 친 작은 배).

> A 4x2 sprite sheet, 1536x512, each cell 384x256, solid pure magenta #FF00FF background, eight small visiting boats facing left, much smaller than a pirate ship, the waterline at the same height in every cell about 6% above the bottom: a mail boat with a red mailbox flag, a wandering merchant's small sailboat piled with goods, an old fisherman's little boat with a net, a scholar's boat with map scrolls and a spyglass, a rowboat full of flower baskets, a lighthouse repairer's boat with a ladder and tools, a painted raft with neighbor island children, a small boat with a purple tent for a friendly antique seller. (+ 공통 스타일 문장)

### 6. visitor_faces.png (손님 얼굴 8개)
- 1024×512, 4×2칸이야 (한 칸 256×256). 칸 순서는 그림 5와 같게 해 줘.
- `resident_faces.png`처럼 머리와 어깨가 보이는 웃는 얼굴이고, 뒤에 둥근 색 동그라미가 있어.

> A 4x2 grid of eight head-and-shoulder portraits, 1024x512, each cell 256x256, solid pure magenta #FF00FF background with a soft colored circle behind each face, friendly smiling visitors matching the eight visitor boats in the same order: a mail carrier, a wandering merchant, an old fisherman, a scholar with round glasses, a flower delivery girl, a lighthouse repairer, two neighbor island children, a mysterious but kind antique seller in a purple hood. Same style as the supplied resident_faces sheet. (+ 공통 스타일 문장)

### 7. island_seasons.png (계절 섬 겹쳐 그리기)
`island_4.png`(섬 4단계 그림) 위에 그대로 겹칠 계절 덮개야.
- 1536×4096, 1열 × 4행이야 (한 칸 1536×1024, `island_4.png` 원본과 같은 크기). 줄은 봄, 여름, 가을, 겨울 순서야.
- 섬 모양과 나무 위치는 `island_4.png`와 **픽셀 위치까지 같게** 해 줘. 덮개만 그려 줘:
  - 봄: 나무 위 분홍 꽃
  - 여름: 진한 초록 잎과 해바라기 몇 송이
  - 가을: 주황·빨강 단풍 잎
  - 겨울: 지붕과 나무 위 눈, 땅의 눈
- 섬 자체(땅, 집, 바위)는 다시 그리지 말고, 덮을 부분만 그리고 나머지는 마젠타로 남겨 줘.

> A 1x4 overlay sheet, 1536x4096, each row 1536x1024 exactly matching the supplied island_4 picture in size and position, solid pure magenta #FF00FF everywhere except the seasonal overlay parts: row 1 spring pink blossoms on the trees, row 2 summer deep green leaves with a few sunflowers, row 3 autumn orange and red leaves, row 4 winter snow on roofs, trees and ground. Do not redraw the island itself, only the overlay pieces in the exact pixel positions. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order12.zip
  data/41_captain_titles.json, 42_sea_weather.json, 43_port_requests.json,
       44_crew_pair_talks.json, 45_seasons.json, 46_island_visitors.json
  art/title_badges.png, weather_fx.png, weather_sky.png, sea_creatures_2.png,
      visitor_boats.png, visitor_faces.png, island_seasons.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
