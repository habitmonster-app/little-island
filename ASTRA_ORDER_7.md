# 리틀 아일랜드: 아스트라에게 일곱 번째 부탁

아스트라, 6차 자료는 build-28에 모두 넣었어.
- 선원 특별 항해 24개: 배 탭에서 예약하면 다음 출항 한 번이 임무가 되고, 첫 성공에 보상과 이야기가 열려.
- 5단계 배와 부품 Lv25가 들어갔어.
- 배에 붙이는 꾸미기 그림이 들어갔어.
- 일지의 선원 그림이 들어갔어.

이번엔 **오래 하는 사람이 계속 모을 거리**를 채워 줘. 배와 선원이 중심인 건 그대로야.
마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터

### 25_pirate_bosses.json (해적 우두머리 배 8척)
지금 해적은 8종이고 모두 보통 해적이야. 가끔 나오는 **우두머리 배**를 만들어 줘.
배 단계 2~5에 2척씩, 모두 8척이야.
```json
{ "bosses": [ {
  "id": "boss_xxx",
  "name": "배 이름 (10자 안쪽)",
  "captain": "선장 이름",
  "min_ship_level": 2,
  "unlock": "ship_2 같은 기존 깃발",
  "ports": ["port_xxx"],
  "chance": 0.04,
  "power": 1.8,
  "win_gold": [80, 120],
  "lose": 0.3,
  "first_win_reward": { "gold": 300, "map_pieces": 2, "cosmetic_id": "trophy_xxx" },
  "lines": { "appear": "나타날 때 한 줄 (18자 안쪽)", "lose": "우리가 이겼을 때 선장의 한 줄", "win": "우리가 졌을 때 선장의 한 줄" },
  "crew_lines": { "mungi": "포술장이 같이 탔을 때 한 줄 (18자 안쪽)" },
  "story": { "title": "...", "body": "처음 이겼을 때 열리는 짧은 이야기 (300~500자)" }
} ] }
```

**규칙**
- 한 항해에 해적은 지금처럼 하나만 나와. 우두머리가 뽑히면 보통 해적 대신 나와.
- 지면 보통 해적처럼 짐만 조금 잃고, 다음에 다시 만날 수 있어.
- 첫 승리 보상은 한 번만 줘.
- 세기(power)는 지금 해적이 1.0~1.6이니까, 우두머리는 1.7~2.4 사이로 해 줘.
- 선원 대사는 지금 12명 선원 id를 써 줘. 한 우두머리에 2~4명만 있어도 돼.
- 다치거나 죽는 일 없이, 지금 이야기 톤과 같게 해 줘.

### 26_souvenirs.json (항해 도감: 항구 기념품 33개)
항구 11곳에서 3개씩, 모두 33개의 기념품을 모으는 도감이야.
- 그 항구에 도착할 때 가끔 하나를 얻어 (확률 15~25%, 아직 없는 것부터).
- 능력치는 없어. 모으는 재미용이야.
```json
{ "souvenirs": [ {
  "id": "souv_saltflat_1",
  "port": "port_saltflat",
  "name": "기념품 이름 (10자 안쪽)",
  "desc": "한 줄 설명",
  "icon_index": 0,
  "chance": 0.2,
  "rarity": "common|rare"
} ],
  "milestones": [ { "count": 5, "reward": { "gold": 200, "cosmetic_id": null }, "card": "카드 한 줄 (24자 안쪽)" } ] }
```
- 항구 id와 특산물: port_haesol(해솔항, 없음), port_saltflat(소금여울, 소금), port_pottery(도기포, 도자기), port_windneck(바람목, 돛천), port_pineshade(솔그늘항, 목재), port_redanchor(붉은 닻, 밧줄), port_fogferry(안개나루, 말린 생선), port_glasscape(유리곶, 유리), port_reedbay(갈대만, 곡식), port_paperwater(종이물, 종이), port_whitesail(백선항, 연장)
- 이정표(milestones)는 5, 10, 15, 20, 25, 30, 33개, 그리고 "11개 항구에서 하나씩" 이렇게 8개로 해 줘. 이 중 4개는 아래 새 꾸미기를 보상으로 줘.

### 27_ship_cosmetics_2.json (배 꾸미기 2탄 16개)
22_ship_cosmetics.json과 **똑같은 모양**으로 16개를 더 만들어 줘. 깃발, 뱃머리, 돛 무늬, 갑판 물건이 4개씩이야.
- 우두머리 첫 승리 보상 8개 (trophy_로 시작하는 id)
- 도감 이정표 보상 4개
- 금화로 사는 것 4개 (금화 2,000~10,000, 5단계 배를 위한 고급 장식)
- `icon_index`는 아래 새 그림 시트의 0~15칸이야.
- 능력치는 없어. 모습만 바뀌어.

### 28_crew_charms.json (선원 부적 뽑기)
선원 스킬을 더 키우는 재미로, **부적 뽑기**와 **훈련 퍼즐**을 넣고 싶어.
- **부적**: 선원 한 명에게 하나씩 다는 작은 물건이야. 스킬 효과를 조금 더해 줘.
- **뽑기**: 진짜 돈은 쓰지 않아. 게임 안에서 모은 "항해 쪽지"나 금화로만 뽑아.
- **같은 부적**이 또 나오면 그 부적이 한 단계 올라가 (최대 5단계).
- **훈련 퍼즐**: 선원 탭에서 하루 몇 번 풀 수 있는 작은 퍼즐이야. 풀면 항해 쪽지를 받아.

```json
{ "charms": [ {
  "id": "charm_xxx",
  "name": "부적 이름 (8자 안쪽)",
  "rarity": "common|rare|epic",
  "effect": "cargo_bonus|speed_bonus|pirate_win_bonus|sale_bonus|event_good_bonus|repair_after_loss|treasure_find_bonus|wood_price_bonus|stone_price_bonus",
  "value_by_level": [0.01, 0.015, 0.02, 0.025, 0.03],
  "best_for_role": "갑판장 같은 선원 역할 (이 역할 선원이 달면 1.5배)",
  "icon_index": 0,
  "desc": "한 줄"
} ],
  "gacha": {
    "ticket_name": "항해 쪽지",
    "ticket_per_voyage": 1,
    "cost_tickets": 10,
    "cost_gold": 500,
    "rates": { "common": 0.7, "rare": 0.25, "epic": 0.05 },
    "pity": "epic이 30번 안 나오면 다음은 epic",
    "duplicate": "같은 부적은 단계 +1, 5단계면 쪽지 3장으로 돌려줌"
  },
  "training_puzzle": {
    "kind": "어떤 퍼즐인지 (예: 매듭 잇기, 짐 맞추기, 돛 줄 당기기 순서)",
    "rules": "규칙 설명 3~5줄",
    "plays_per_day": 3,
    "reward_tickets": [1, 3]
  } }
```

**규칙**
- 부적은 모두 **24개**로 해 줘. common 12개, rare 8개, epic 4개야.
- 효과 이름은 지금 선원 스킬과 같은 9가지만 써 줘. 부적 효과도 지금 선원 효과 상한(effect_caps) 안에서 더해져.
- 뽑기 확률은 화면에 그대로 보여 줄 거라 정확히 적어 줘.
- 훈련 퍼즐은 손가락 몇 번으로 1분 안에 끝나는 것으로 골라 줘. 그림 없이 지금 있는 상자·밧줄·아이콘으로 그릴 수 있으면 더 좋아.

---

## 2부. 그림

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, limited warm palette, side view, no text, no letters, no numbers, no watermark

### 1. pirate_bosses.png (우두머리 배 8척)
`pirates_all.png`와 같은 크기와 그림체로 그려 줘. 1536×1024, 4×2칸 (한 칸 384×512), 배경은 순수한 마젠타 #FF00FF.
- 배는 **왼쪽**을 봐. 우리 배와 마주 보는 방향이야.
- 보통 해적선보다 크고, 배마다 색과 상징이 분명하게 달라야 해.
- 수면선은 모든 칸에서 같은 높이로 맞춰 줘 (칸 아래에서 약 4%).

> A 4x2 sprite sheet of eight pirate boss ships, 1536x1024, each cell 384x512, solid pure magenta #FF00FF background, ships facing left, each bigger and more ornate than a normal pirate ship, each with its own clear color and emblem on the sail (a hook, a red crab, a black kraken, a storm cloud, a golden skull-free coin, a reef rock, a moon, a serpent), the waterline at the same height in every cell about 4% above the bottom, cute and not scary. (+ 공통 스타일 문장)

### 2. boss_captains.png (우두머리 선장 얼굴 8개)
결과 카드와 일지에 쓸 얼굴이야. 1024×512, 4×2칸 (한 칸 256×256), 배경은 마젠타. 칸 순서는 위 배와 같게.
`portraits.png`와 같은 크기, 같은 그림체로 그려 줘.

> A 4x2 grid of eight pirate captain head-and-shoulder portraits, 1024x512, each cell 256x256, solid pure magenta #FF00FF background, each matching one boss ship above in colors and emblem, cheeky rivals rather than villains, friendly cartoon faces, no weapons pointed at the viewer. (+ 공통 스타일 문장)

### 3. souvenir_icons.png (기념품 아이콘 33개)
1536×1536, 6×6칸 (한 칸 256×256), 배경은 마젠타. 0~32칸만 쓰고 33~35칸은 비워 둬.
칸 순서는 26_souvenirs.json의 icon_index와 같게. 항구 하나에 3개씩 이어서 그려 줘.

> A 6x6 icon sheet, 1536x1536, each cell 256x256, solid pure magenta #FF00FF background, 33 small harbor souvenir items in the first 33 cells and the last 3 cells empty: three souvenirs per harbor in this order: a pine harbor, a salt flat, a pottery town, a windy cape with sailcloth, a pine forest port, a rope-making harbor with a red anchor, a foggy ferry town with dried fish, a glass-making cape, a reed bay with grain, a paper-making river town, a white-sail tool harbor. Each item centered with a dark outline. (+ 공통 스타일 문장)

### 4. ship_deco_2.png와 ship_deco_on_2.png (배 꾸미기 2탄)
1탄과 똑같은 규칙으로 두 장을 그려 줘. 둘 다 1024×1024, 4×4칸이고 배경은 마젠타야.
칸 순서는 27_ship_cosmetics_2.json의 icon_index와 같게 해 줘. 1줄 깃발, 2줄 뱃머리, 3줄 돛 무늬, 4줄 갑판 물건이야.
- **ship_deco_2.png**: 고르는 화면에 쓰는 아이콘. 깃대와 받침까지 있어도 돼.
- **ship_deco_on_2.png**: 배에 바로 붙이는 모양. 1탄의 ship_deco_on.png와 같은 규칙이야.
  - 깃발은 천만 그려 줘. 천의 왼쪽 가운데가 칸의 왼쪽 가운데에 붙게.
  - 뱃머리와 갑판 물건은 받침 없이 칸 아래쪽 가운데에 놓아 줘.
  - 돛 무늬는 무늬만 칸 가운데에 그려 줘.
- 우두머리 보상 8개는 그 우두머리 배의 색과 상징을 따라 그려 줘.

### 5. charm_icons.png (부적 아이콘 24개)
1536×1024, 6×4칸 (한 칸 256×256), 배경은 마젠타. 칸 순서는 28_crew_charms.json의 icon_index와 같게 해 줘.
희귀도가 보이게 테두리 색을 다르게 해 줘. common은 나무색, rare는 청록, epic은 금색이야.

> A 6x4 icon sheet, 1536x1024, each cell 256x256, solid pure magenta #FF00FF background, 24 small sailor charms and keepsakes (knotted rope bracelet, tiny brass compass, seashell pendant, little anchor pin, lucky fish token, wind chime, mini spyglass, star-shaped badge and more), each centered in a round frame: use wooden frames for the first 12, teal frames for the next 8, gold frames with a soft glow for the last 4. (+ 공통 스타일 문장)

### 6. gacha_chest.png (뽑기 상자 연출 4칸)
512×128, 4칸 (한 칸 128×128), 배경은 마젠타. 왼쪽부터 닫힌 상자, 흔들리는 상자, 열리는 상자(빛), 활짝 열린 상자야.

> One row of four pixel art frames, 512x128, each cell 128x128, solid pure magenta #FF00FF background, a small sea chest wrapped with a blue cord: closed, shaking, opening with warm light, fully open with sparkles. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order7.zip
  data/25_pirate_bosses.json, 26_souvenirs.json, 27_ship_cosmetics_2.json, 28_crew_charms.json
  art/pirate_bosses.png, boss_captains.png, souvenir_icons.png, ship_deco_2.png, ship_deco_on_2.png, charm_icons.png, gacha_chest.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **영어판**: Claude가 앱 글자 구조를 바꾼 뒤에 새 글자만 따로 부탁할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
