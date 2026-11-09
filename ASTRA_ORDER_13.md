# 리틀 아일랜드: 아스트라에게 열세 번째 부탁 (큰 묶음)

아스트라, 12차 자료는 build-41에 모두 넣었어.
- 바다 날씨 10종: 출항할 때 정해지고 비·눈·안개·별·오로라·무지개가 바다에 보여.
- 섬의 계절: 4단계 섬은 나무가 계절 따라 바뀌고, 모든 섬에 계절 장식이 놓여. 축제 보상도 돼.
- 손님 배 8종, 항구 의뢰 33개, 선장 칭호 24개, 짝 대화 30개, 바다 동물 2탄도 다 들어갔어.

이번에도 **한 번에 많이** 부탁할게. 데이터 5개, 그림 7장이야.
이번 주제는 두 가지야.
1. **게임의 핵심인 배를 한 단계 더**: 6단계 배와 부품 Lv26~30.
2. **바다를 오래 보고 싶게**: 바다 동물 도감, 주간 바다 소식, 손님 이야기.

공통 규칙
- 화면에 나오는 글은 **짧게**. 제목 12자, 한 줄 24자, 짧은 설명 16자, 대사 18자 안쪽.
- 숫자와 %는 화면 글에 넣지 마. 앱이 붙여.
- 다치거나 죽는 일 없이, 지금 이야기 톤과 같게.
- 선원 id: yeoul, mungi, sori, nuri, bada, geonu, dori, haneul, bityeo, deul, saei, onyu
- 항구 id: port_haesol, port_saltflat, port_pottery, port_windneck, port_pineshade, port_redanchor, port_fogferry, port_glasscape, port_reedbay, port_paperwater, port_whitesail
- 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터 (5개)

### 47_ship_stage6.json (6단계 배, 부품 Lv26~30)
**24_ship_stage5.json과 같은 모양**으로 써 줘. 5단계 배 다음 단계야.
- ships에 level 6 배 하나만 써 줘. 이름은 "귀항호 · ○○" 꼴로 해 줘.
  - cargo_wood, cargo_stone, speed, pirate_win은 5단계보다 조금씩 좋게 (짐 +15~20%, speed +0.05, pirate_win +0.02 안쪽).
  - upgrade_cost는 5단계로 올릴 때의 1.6~2배쯤으로 해 줘.
- parts: 4개 부품(hull, sail, cannon, helm)마다 Lv26~30의 5칸이야. value는 Lv21~25의 늘어나는 폭을 이어 가되 **조금 더 작게** 해 줘 (상한이 있어서).
- max_level_per_ship_level에 6단계 → 30을 더해 줘. ship_upgrade_needs_parts_level은 6단계에 25로 해 줘.
- slots_by_ship_level의 6단계는 5단계와 같게 둬 (선원 자리는 늘리지 않아).
- look_stages_patch에는 6단계 배의 겉모습 4단계를 그림 1의 칸 0~3과 맞춰 써 줘.
- home_news는 24자 안쪽이야.
- 새 항구나 새 해적은 만들지 마. 6단계 배는 지금 있는 모든 항구를 다녀.

### 48_sea_creature_book.json (바다 동물 도감)
홈 화면 바다에 지나가는 동물을 **톡 치면** 도감에 기록돼. 처음 볼 때 한 번 보상을 받아.

```json
{ "milestones": [ { "count": 4, "reward": { "tickets": 3, "gold": 300 } }, { "count": 8, "reward": { "tickets": 5, "gold": 800, "title_name": "8자 안쪽" } } ],
  "creatures": [ {
    "id": "crit_dolphin",
    "kind": 0,
    "name": "이름 (6자 안쪽)",
    "short": "한 줄 (16자 안쪽)",
    "first_line": "처음 봤을 때 선원 한마디 (18자 안쪽)",
    "seen_by": "yeoul",
    "reward": { "tickets": 1, "gold": 50 },
    "icon_index": 0
  } ] }
```
- **8마리**를 써 줘. kind는 앱의 동물 번호야.
  - 0 돌고래, 1 고래, 2 바다거북, 3 날치 떼
  - 4 물개, 5 해파리 떼, 6 갈매기 떼, 7 작은 문어
- seen_by는 그 동물을 좋아할 것 같은 선원 한 명이야.
- 첫 보상은 쪽지 1장과 금화 30~80으로 해 줘.
- milestones는 위 예처럼 2개야. title_name은 8마리를 다 봤을 때 주는 보기용 칭호 이름이야.
- icon_index는 아래 그림 4의 0~7칸이야.

### 49_weekly_tides.json (주간 바다 소식, 12개)
월요일마다 이번 주 바다 소식이 하나 정해져. 그 주 내내 작은 변화가 있어. 12개를 돌아가며 써.

```json
{ "tides": [ {
  "id": "tide_whale_week",
  "name": "소식 이름 (8자 안쪽)",
  "short": "한 줄 (16자 안쪽)",
  "news": "할 일 탭 설명 (24자 안쪽)",
  "effect": { "kind": "critter_more | visitor_more | weather_boost | request_bonus | find_more | souvenir_more | none", "value": 0.0, "target": "" },
  "icon_index": 0
} ] }
```
- effect.kind는 **위 7가지만** 써 줘.
  - critter_more: 바다 동물이 더 자주 와. value는 1.5~2.0 (배수).
  - visitor_more: 손님 배가 더 자주 와. value는 1.5~2.0 (배수).
  - weather_boost: target 날씨(42번 id)가 더 자주 나와. value는 2.0~4.0 (배수).
  - request_bonus: 항구 의뢰 금화가 늘어. value는 0.1~0.3.
  - find_more: 바다에 뜬 병·상자가 더 자주 와. value는 1.3~1.6 (배수).
  - souvenir_more: 항구 기념품 확률이 늘어. value는 0.05~0.1 (더하는 값).
  - none: 효과 없는 이야기 주간.
- 12개 중 none은 2개 이하로 해 줘. weather_boost는 오로라·무지개를 빼고 써 줘.
- icon_index는 아래 그림 5의 0~11칸이야.

### 50_visitor_stories.json (손님 이야기, 24개)
46번 손님 8명과 **거래한 횟수**에 따라 짧은 이야기 카드가 열려. 일지의 "손님" 장에 쌓여.

```json
{ "stories": [ {
  "id": "vstory_mail_1",
  "visitor": "visitor_mail",
  "trades": 1,
  "title": "제목 (12자 안쪽)",
  "card_line": "카드 한 줄 (24자 안쪽)",
  "story": "일지 본문 (150~250자)",
  "reward": { "tickets": 1, "gold": 0 },
  "art_index": 0
} ] }
```
- 손님 8명 × 3편 = **24편**이야. trades는 1, 3, 6으로 해 줘.
- 3편이 이어지는 짧은 이야기야. 마지막 편에는 손님이 섬에 작은 선물을 남기고 가.
- reward는 1편 쪽지 1장, 2편 쪽지 1장 + 금화 100, 3편 쪽지 2장 + 금화 300 안쪽으로 해 줘.
- art_index는 아래 그림 6의 손님 칸 번호(0~7)야. 손님마다 그림 하나를 3편이 같이 써.

### 51_crew_weather_lines.json (선원 날씨 한마디)
지금은 날씨마다 선원 말이 하나뿐이라 누가 말해도 같아. 선원마다 다르게 해 줘.

```json
{ "lines": { "yeoul": { "weather_clear": "18자 안쪽", "...": "10종 모두" }, "...": "12명 모두" } }
```
- 12명 × 날씨 10종 = **120줄**, 모두 18자 안쪽이야.
- 날씨 id는 42번 그대로 써 줘 (weather_clear, weather_fair_wind, weather_calm, weather_drizzle, weather_shower, weather_fog, weather_starry, weather_rainbow, weather_snow, weather_aurora).
- 선원의 역할과 말투가 드러나게 써 줘. 예: 요리사는 비 오는 날 국물, 망루지기는 안개 속 불빛.

---

## 2부. 그림 (7장)

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

모든 그림은 배경이 순수한 마젠타 #FF00FF야.

### 1. ship_stage6.png (6단계 배)
`ship_stage5.png`와 **똑같은 규칙**이야.
- 1024×256, 4×1칸 (한 칸 256×256). 칸 0~3은 부품을 올리며 바뀌는 겉모습 4단계야 (0 막 산 모습 → 3 다 꾸민 모습).
- 오른쪽을 보게 해 줘. **수면선과 배 크기 비율은 ship_stage5.png와 똑같이** 맞춰 줘 (배 위에 선원과 장식을 겹쳐 그리기 때문이야).
- 5단계보다 더 크고 당당한 큰 범선이야. 돛대 셋, 꿀갈색 선체, 상아색 돛은 그대로 이어 가.
- 파란 끈, 이름판 같은 귀항호의 표시도 남겨 줘.
- 갑판 높이는 5단계와 같게 해 줘.

> A 4x1 sprite sheet, 1024x256, each cell 256x256, solid pure magenta #FF00FF background, the same trading ship as the supplied ship_stage5 sheet grown into a grand three-masted flagship, facing right, honey-brown hull, ivory sails, a blue cord and a small name plate kept, four upgrade looks from plain to fully decorated, waterline and deck height exactly matching the supplied ship_stage5 sheet. (+ 공통 스타일 문장)

### 2. island_seasons_2.png (2단계 섬 계절 덮개)
### 3. island_seasons_3.png (3단계 섬 계절 덮개)
12차 `island_seasons.png`와 **똑같은 규칙**이야. 이번에는 `island_2.png`와 `island_3.png`에 겹칠 덮개야.
- 각 1536×4096, 1열 × 4행 (한 칸 1536×1024, 섬 원본과 같은 크기). 줄은 봄, 여름, 가을, 겨울.
- 섬 모양과 나무 위치는 각 섬 그림과 **픽셀 위치까지 같게** 해 줘. 덮개만 그리고 나머지는 마젠타로 남겨 줘.
- 봄 분홍 꽃, 여름 진한 잎과 해바라기, 가을 단풍, 겨울 지붕·나무·땅의 눈.

> A 1x4 overlay sheet, 1536x4096, each row 1536x1024 exactly matching the supplied island_2 picture in size and position, solid pure magenta #FF00FF everywhere except the seasonal overlay parts: row 1 spring pink blossoms on the trees, row 2 summer deep green leaves with a few sunflowers, row 3 autumn orange and red leaves, row 4 winter snow on roofs, trees and ground. Do not redraw the island itself. (+ 공통 스타일 문장)
>
> (island_seasons_3은 같은 문장에서 island_2를 island_3으로 바꿔 줘)

### 4. creature_book.png (바다 동물 도감 그림 8개)
- 1024×512, 4×2칸 (한 칸 256×256). 칸 순서는 48번 kind 순서와 같게 해 줘 (돌고래, 고래, 거북, 날치 / 물개, 해파리, 갈매기, 문어).
- 둥근 물빛 틀 안에 그 동물이 웃는 모습이야. `sea_creatures.png`, `sea_creatures_2.png`의 동물과 같은 생김새로 그려 줘.

> A 4x2 icon sheet, 1024x512, each cell 256x256, solid pure magenta #FF00FF background, eight round sea-blue framed portraits of friendly sea animals matching the supplied sea_creatures sheets: a dolphin, a whale, a sea turtle, flying fish; a seal, jellyfish, seagulls, a small octopus. (+ 공통 스타일 문장)

### 5. tide_icons.png (주간 바다 소식 아이콘 12개)
- 1024×768, 4×3칸 (한 칸 256×256). 칸 순서는 49번 icon_index와 같게 해 줘.
- 작은 나무 게시판 틀 안에 그 주의 상징을 그려 줘 (고래 꼬리, 손님 배, 비구름, 금화 주머니, 유리병, 기념품 꾸러미 등).

> A 4x3 icon sheet, 1024x768, each cell 256x256, solid pure magenta #FF00FF background, twelve weekly sea news icons, each a small wooden notice frame with one clear symbol such as a whale tail, a small visiting boat, a rain cloud, a gold coin pouch, a floating glass bottle, a gift bundle, a fair wind swirl, a fog bank. (+ 공통 스타일 문장)

### 6. visitor_story_art.png (손님 이야기 그림 8장)
- 1536×1024, 4×2칸 (한 칸 384×512, 세로로 긴 그림), 얇은 어두운 테두리. 칸 순서는 46번 손님 순서와 같게 해 줘.
- `crew_story_art.png`와 같은 그림체야. 손님이 우리 섬 부두에서 선원·주민과 함께 있는 따뜻한 장면이야. 얼굴과 배는 `visitor_faces.png`, `visitor_boats.png`와 같게 해 줘.
- 이 그림은 배경을 꽉 채운 장면이라 마젠타 배경이 아니야.

> A 4x2 grid of eight vertical pixel art vignettes, 1536x1024, each cell 384x512 with a thin dark frame, each showing one visitor at our small island's wooden dock with crew and villagers in a warm moment, in order: a mail carrier, a wandering merchant, an old fisherman, a scholar with round glasses, a flower delivery girl, a lighthouse repairer, two neighbor island children, a kind antique seller in a purple hood. Faces and boats match the supplied visitor_faces and visitor_boats sheets. (+ 공통 스타일 문장)

### 7. port_icons.png (항구 문장 12개)
항구 의뢰와 항로 칸에 쓸 작은 항구 표시야.
- 1024×768, 4×3칸 (한 칸 256×256). 칸 순서는 위 항구 id 순서 11개, 마지막 칸은 의뢰 게시판이야.
- 둥근 방패 모양 틀 안에 그 항구를 나타내는 그림 하나를 넣어 줘. 예: 해솔항 소나무와 등대, 소금여울 소금 더미, 도기포 항아리, 바람목 풍차, 솔그늘항 솔방울, 붉은 닻 빨간 닻, 안개나루 안개 속 등불, 유리곶 유리병, 갈대만 갈대, 종이물 종이배, 백선항 흰 돛.

> A 4x3 icon sheet, 1024x768, each cell 256x256, solid pure magenta #FF00FF background, twelve round shield-shaped harbor emblems: a pine tree with a lighthouse, a salt heap, a clay jar, a windmill, a pine cone, a red anchor, a lantern in fog, a glass bottle, reeds, a paper boat, a white sail, and finally a small wooden request notice board. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order13.zip
  data/47_ship_stage6.json, 48_sea_creature_book.json, 49_weekly_tides.json,
       50_visitor_stories.json, 51_crew_weather_lines.json
  art/ship_stage6.png, island_seasons_2.png, island_seasons_3.png, creature_book.png,
      tide_icons.png, visitor_story_art.png, port_icons.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야. (47번 6단계 수치만 이번에 새로 써 줘.)
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
