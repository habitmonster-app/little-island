# 리틀 아일랜드: 아스트라에게 열한 번째 부탁 (큰 묶음)

아스트라, 10차 자료는 build-37에 모두 넣었어.
- 선원 항해 사건 36개: 항해 도중 머리 위에 감정 말풍선이 뜨고, 귀항 카드에 얼굴 배지가 붙어.
- 훈련 퍼즐은 39개가 됐어 (3×3 판 15개 포함).
- 돌고래, 고래, 거북, 날치가 홈 화면 바다를 지나가.

이번에는 **한 번에 많이** 부탁할게. 데이터 6개, 그림 6장이야.
모두 **지금 있는 자료와 같은 모양**이라 앱이 바로 읽을 수 있어. 새 규칙은 거의 없어.

공통 규칙
- 화면에 나오는 글은 **짧게**. 제목 12자, 한 줄 24자, 짧은 설명 16자 안쪽.
- 숫자와 %는 글에 넣지 마. 앱이 붙여.
- 다치거나 죽는 일 없이, 지금 이야기 톤과 같게.
- 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터 (6개)

### 35_crew_sea_events_2.json (선원 항해 사건 2탄, 36개)
33번과 **같은 모양**이야. 33번 다음에 이어 붙여 써.
- **24개**: 선원마다 Lv12와 Lv15에 하나씩. 다 자란 선원다운 일로 써 줘.
- **12개**: 짝 사건. 두 선원이 같이 탔을 때만 일어나. 33번 모양에 `"with": "다른 선원 id"`만 더해 줘.
  - 짝은 20_crew.json의 synergy 짝을 먼저 쓰고, 모자라면 잘 어울리는 둘로.
  - card_line에 두 사람 이름이 다 나오게.
- effect 크기와 none 비율(3분의 1쯤)은 33번과 같게.
- emote는 33번의 8가지 중에서만.

### 36_daily_orders_2.json (오늘의 주문 2탄, 30개)
31번과 **같은 모양, 같은 counter 10가지**로 30개 더. 31번의 id와 겹치지 않게.
- 쉬움 15, 보통 10, 어려움 5.
- 보상 한도는 31번과 같아 (하루 3개를 다 해도 쪽지 6장 안쪽).
- 같은 일이라도 제목과 설명을 다르게. 예: "아침 귀항", "단골 항로", "부두 인사".

### 37_crew_charms_2.json (부적 2탄, 12개)
28번의 charms와 **같은 모양**으로 12개. 일반 4, 희귀 4, 영웅 4.
- 28번에 없는 조합 위주로. 특히 아직 적은 효과(wood_price_bonus, treasure_find_bonus, event_good_bonus, repair_after_loss)를 써 줘.
- value_by_level 5칸은 28번의 같은 등급과 비슷한 크기로.
- best_for_worker_ids는 한 명씩.
- icon_index는 아래 그림 1의 0~11칸.
- desc는 16자 안쪽 한 줄.
- 확률, 천장, 중복 규칙은 28번 그대로야. 새로 쓰지 마.

### 38_ship_cosmetics_3.json (배 꾸미기 3탄, 16개)
27번과 **같은 모양**이야. 깃발, 뱃머리, 돛 무늬, 갑판 물건이 4개씩.
얻는 조건은 **아래 5가지 type만** 써 줘 (앱이 이것만 판정해).

| type | 예 |
|---|---|
| boss_first_win | `{"type":"boss_first_win","boss_id":"boss_xxx"}` |
| souvenir_milestone_complete | `{"type":"souvenir_milestone_complete","milestone_id":"souvenir_count_20"}` |
| game_flag | `{"type":"game_flag","key":"ship_5"}` |
| counter_at_least | `{"type":"counter_at_least","counter":"voyages","value":200}` (counter는 voyages, treasures만) |
| any_promoted_crew_level_at_least | `{"type":"any_promoted_crew_level_at_least","value":15}` |

- 4개는 아래 39번 새 우두머리 첫 승리 보상 (id는 trophy_로 시작).
- 4개는 오래 한 기록 보상: 항해 100번, 300번, 보물 20번, 선원 Lv15.
- 4개는 금화로 사는 고급 장식 (금화 10,000~30,000). mode는 27번의 금화 구매와 같게.
- 4개는 자유롭게. 위 5가지 type 안에서.
- icon_index는 아래 그림 2·3의 0~15칸.
- desc는 16자 안쪽, how_to_get_text는 12자 안쪽.

### 39_pirate_bosses_2.json (우두머리 2탄, 4척)
25번과 **같은 모양**으로 4척. 5단계 배 이후의 맞수야.
- min_ship_level 5, ports는 갈대만, 종이물, 백선항 중에서.
- power는 2.4~2.8.
- unlock은 ship_5.
- first_win_reward의 cosmetic_id는 38번의 trophy 4개와 하나씩 짝지어 줘.
- 그림 번호: sprite와 face는 0~3이야. 아래 그림 4·5의 칸 번호를 써.
- crew_lines는 2~4명, story는 300~500자.

### 40_crew_voices_2.json (Lv11 이상 선원 말)
21번의 갑판 대사 모양을 따라 써 줘. 선원이 Lv11 이상이면 이 말이 섞여 나와.
```json
{ "crew": [ { "worker_id": "yeoul", "deck_lines": ["...", "... 5개"], "fight_lines": ["...", "... 3개"] } ] }
```
- 12명 × 갑판 5개, 싸움 3개. 모두 **18자 안쪽**.
- 이제 믿음직한 선원이 된 말투로. 처음 섬에 온 때를 가끔 떠올려도 좋아.

---

## 2부. 그림 (6장)

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

모든 그림은 배경이 순수한 마젠타 #FF00FF야.

### 1. charm_icons_2.png (부적 2탄 아이콘 12개)
- 1024×768, 4×3칸 (한 칸 256×256). 칸 순서는 37번 icon_index와 같게.
- `charm_icons.png`처럼 둥근 틀 안에: 1줄 나무 틀(일반), 2줄 청록 틀(희귀), 3줄 금빛 틀과 은은한 빛(영웅).

> A 4x3 icon sheet, 1024x768, each cell 256x256, solid pure magenta #FF00FF background, twelve small sailor charms each centered in a round frame like the supplied charm_icons sheet: first row wooden frames, second row teal frames, third row gold frames with a soft glow. (+ 공통 스타일 문장)

### 2. ship_deco_3.png (꾸미기 3탄 고르는 아이콘)
### 3. ship_deco_on_3.png (꾸미기 3탄 배에 붙이는 모양)
2탄(ship_deco_2.png, ship_deco_on_2.png)과 **똑같은 규칙**이야.
- 둘 다 1024×1024, 4×4칸. 1줄 깃발, 2줄 뱃머리, 3줄 돛 무늬, 4줄 갑판 물건. 칸 순서는 38번 icon_index와 같게.
- ship_deco_on_3: 깃발은 천만 (왼쪽 가운데가 칸 왼쪽 가운데에), 뱃머리·갑판 물건은 받침 없이 칸 아래쪽 가운데, 돛 무늬는 무늬만 칸 가운데.
- trophy 4개는 그 우두머리 배의 색과 상징을 따라 그려 줘.

### 4. pirate_bosses_2.png (우두머리 2탄 배 4척)
- 1536×512, 4×1칸 (한 칸 384×512). `pirate_bosses.png`와 같은 크기와 그림체.
- 왼쪽을 보게. 수면선은 칸 아래에서 약 4%로 모든 칸 같게.
- 1탄보다 더 크고 화려하게. 배마다 색과 돛 상징이 확 다르게.

> A 4x1 sprite sheet of four grand pirate boss ships, 1536x512, each cell 384x512, solid pure magenta #FF00FF background, ships facing left, bigger and more ornate than the supplied pirate_bosses sheet, each with its own clear color and sail emblem, the waterline at the same height in every cell about 4% above the bottom, cute and not scary. (+ 공통 스타일 문장)

### 5. boss_captains_2.png (우두머리 2탄 선장 얼굴 4개)
- 1024×256, 4×1칸 (한 칸 256×256). 칸 순서는 그림 4와 같게.
- `boss_captains.png`처럼 얄밉지만 귀여운 맞수 얼굴. 무기를 겨누지 않게.

> A 4x1 grid of four pirate captain head-and-shoulder portraits, 1024x256, each cell 256x256, solid pure magenta #FF00FF background, each matching one boss ship of the new sheet in colors and emblem, cheeky rivals rather than villains, friendly cartoon faces. (+ 공통 스타일 문장)

### 6. crew_event_badge_15.png (코트 입은 선원 배지)
- 1024×768, 4×3칸 (한 칸 256×256). 칸 순서는 선원 id 순서와 같게.
- `crew_event_badge.png`와 같은 둥근 나무 액자와 리본. 얼굴은 `crew_portraits_15.png`처럼 남색 코트를 입은 모습.
- Lv11 이상 선원의 사건 카드에 쓸 거야.

> A 4x3 grid of twelve round wooden badge frames with a small ribbon at the bottom, 1024x768, each cell 256x256, solid pure magenta #FF00FF background, exactly like the supplied crew_event_badge sheet, but each face now wears the navy coat with gold buttons from the supplied crew_portraits_15 sheet, same faces, hair and order. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order11.zip
  data/35_crew_sea_events_2.json, 36_daily_orders_2.json, 37_crew_charms_2.json,
       38_ship_cosmetics_3.json, 39_pirate_bosses_2.json, 40_crew_voices_2.json
  art/charm_icons_2.png, ship_deco_3.png, ship_deco_on_3.png,
      pirate_bosses_2.png, boss_captains_2.png, crew_event_badge_15.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
