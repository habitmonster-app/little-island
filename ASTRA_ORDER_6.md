# 리틀 아일랜드: 아스트라에게 여섯 번째 부탁

아스트라, 5차 자료는 build-25~26에 모두 넣었어.
- 새 바다 배경 4장: 수면을 배가 떠 있는 물선에 맞췄어.
- 선원 이야기 36편: 항해일지에 들어갔어.
- 갑판·싸움·짝 대사: 항해 중 말풍선으로 나와.
- 배 꾸미기 16종: 얻기, 사기, 달기, 떼기가 다 돼.

영어판은 앱 글자 구조를 먼저 바꿔야 해서 Claude가 따로 할 거야. 번역 파일은 보관해 뒀어.

이번에도 중심은 **배와 선원**이야. 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터

### 23_crew_missions.json (선원 특별 항해)
지금 선원은 항해를 따라가서 경험치만 받아. 선원마다 **자기 이름이 걸린 특별 항해**가 있으면 좋겠어.
- 조건이 되면 배 탭에 "특별 항해"가 떠.
- 고르면 다음 출항 한 번이 그 임무가 돼.
- 성공하면 보상과 짧은 이야기 한 편이 열려.

선원 12명 × 임무 2개씩, 모두 **24개**.
```json
{ "missions": [ {
  "id": "mission_yeoul_1",
  "crew": "yeoul",
  "title": "임무 이름 (12자 안쪽)",
  "summary": "한두 줄 설명",
  "requires": {
    "crew_level_at_least": 3,
    "crew_aboard": ["yeoul"],
    "parts_at_least": { "hull": 5 },
    "ship_level_at_least": 1,
    "port": "port_haesol",
    "game_flag": null
  },
  "voyage": { "duration_mult": 1.2, "pirate_chance_add": 0.05 },
  "success_rule": "항상 성공 | 해적을 만나면 이겨야 성공",
  "reward": { "gold": 200, "wood": 0, "stone": 0, "map_pieces": 1, "crew_xp": 30, "cosmetic_id": null },
  "story": { "title": "...", "body": "성공했을 때 열리는 짧은 이야기 (300~500자)" },
  "home_line": "임무 중 홈 말풍선 한 줄 (18자 안쪽)",
  "result_card": "귀항 카드 한 줄 (24자 안쪽)"
} ] }
```

**쓸 수 있는 이름**
- 선원 id: yeoul(여울, 갑판장), mungi(문기, 포술장), sori(소리, 보급 담당), nuri(누리, 화물 관리인), bada(바다, 돛지기), geonu(건우, 선박 목수), dori(도리, 조타수), haneul(하늘, 항해 통신원), bityeo(빛여, 망루지기), deul(들, 갑판 요리사), saei(새이, 교역사), onyu(온유, 안전 관리인)
- 부품 id: hull, sail, cannon, helm (레벨 1~20)
- 배 단계: 1~4. 선원 자리는 2, 3, 4, 5칸이야.
- 항구 id: port_haesol(해솔항), port_saltflat(소금여울), port_pottery(도기포), port_windneck(바람목), port_pineshade(솔그늘항), port_redanchor(붉은 닻), port_fogferry(안개나루), port_glasscape(유리곶), port_reedbay(갈대만), port_paperwater(종이물), port_whitesail(백선항)

**규칙**
- 임무 1은 선원 Lv3, 임무 2는 선원 Lv7쯤에서 열리게.
- 임무 2개 중 하나는 그 선원의 역할에 맞는 부품 레벨을 조건으로 걸어 줘. 예: 포술장은 cannon, 조타수는 helm.
- 보상은 보통 항해 1~2번 몫 정도로. 너무 크지 않게.
- 서린 선장은 이야기에만 나와도 돼. 선원 명단에는 넣지 마.

### 24_ship_stage5.json (5단계 배, 마지막 큰 배)
지금 배는 4단계가 끝이야. 오래 하는 사람을 위해 **5단계**를 하나 더 줘.
- 기존 02_ships.json과 같은 모양으로 5단계 한 칸을 써 줘. 이름, 설명, 비용, 짐칸, 필요 조건을 넣어 줘.
- 부품은 Lv20까지 다 올려야 열리게 하고, 선원 자리는 6칸으로.
- 19_ship_parts.json의 `max_level_per_ship_level`에 5단계 값도 같이 적어 줘 (예: 25).
- 부품 Lv21~25의 효과와 비용도 같은 모양으로 이어서 써 줘. 효과 상한은 지금 상한을 넘지 않게.

---

## 2부. 그림

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, limited warm palette, side view, no text, no letters, no numbers, no watermark

### 1. ship_deco_on.png (배에 바로 붙이는 꾸미기 그림)
지금 `ship_deco.png`는 **아이콘**이라 깃대 받침, 돛 틀까지 같이 그려져 있어. 배 위에 겹치면 어색해.
같은 16개를 **배에 붙이는 모양**으로 다시 그려 줘. 칸 순서는 ship_deco.png와 똑같이 해 줘.
- 1024×1024, 4×4칸 (한 칸 256×256), 배경은 순수한 마젠타 #FF00FF
- 1줄 깃발 4개: **천만** 그려 줘. 깃대와 받침은 빼고, 오른쪽으로 날리게. 천의 왼쪽 끝이 칸의 왼쪽 가운데에 붙게.
- 2줄 뱃머리 장식 4개: 받침 없이 장식만 그려 줘. 오른쪽을 보게. 칸 아래쪽 가운데에 놓아 줘.
- 3줄 돛 무늬 4개: **무늬만** 그려 줘 (파란 줄, 해, 잎, 금빛 물결). 돛 천과 돛 틀은 빼고, 주변은 마젠타로. 흰 돛 위에 찍는 도장처럼.
- 4줄 갑판 물건 4개: 종, 등잔, 꽃화분, 망원경을 작게 그려 줘. 칸 아래쪽 가운데에 놓아 줘.

> A 4x4 sprite sheet on a solid pure magenta #FF00FF background, 1024x1024, each cell 256x256, items facing right. Row 1: four small ship flags, cloth only, no pole, no stand, waving to the right, attached at the left middle of the cell: red swallowtail, light blue pennant, yellow flag with a blue wave stripe, green flag with a cream diamond. Row 2: four small ship figureheads with no base, facing right, at the bottom center of the cell: smiling seagull, orange-red fish, smiling yellow star on a blue wave curl, hanging bow lantern. Row 3: four sail emblems only, no cloth, no frame, like stamps on a white sail: a horizontal blue stripe, a red sun with rays, a green leaf, golden wavy lines. Row 4: four small deck items at the bottom center of the cell: brass bell on a little wooden post, small glowing deck lantern, terracotta flowerpot with orange and white flowers, brass spyglass on a tripod. (+ 공통 스타일 문장)

### 2. crew_story_art.png (선원 이야기 그림 12장)
선원 이야기를 일지에서 열면 맨 위에 보일 그림이야.
- 1536×1536, 4×3칸 (한 칸 384×512, 세로로 긴 그림), 칸 순서는 위 선원 id 순서와 같게.
- 각 칸에는 그 선원이 자기 일을 하는 장면을 하나씩 그려 줘.
  - 여울: 갑판에서 매듭 묶기
  - 문기: 대포 닦기
  - 소리: 식량 상자 세기
  - 누리: 짐 쌓기
  - 바다: 돛 올리기
  - 건우: 망치질
  - 도리: 키 잡기
  - 하늘: 갈매기에게 편지 맡기
  - 빛여: 망루에서 망원경 보기
  - 들: 갑판 요리
  - 새이: 항구에서 흥정
  - 온유: 구명 튜브 점검
- 얼굴과 옷은 지금 `worker_portraits.png`, `crew_deck_a/b.png`와 똑같이 맞춰 줘.

> A 4x3 grid of twelve vertical pixel art vignettes, 1536x1536, each cell 384x512 with a thin dark frame. Each cell shows one young sailor from our crew doing their job on a small wooden ship or harbor, warm daylight: (1) tying knots on deck, (2) polishing a small cannon, (3) counting food crates, (4) stacking cargo, (5) hoisting a sail, (6) hammering a plank, (7) steering at the wheel, (8) handing a letter to a seagull, (9) looking through a spyglass from the crow's nest, (10) cooking on deck, (11) bargaining at a market stall by the pier, (12) checking a life ring. Characters match the supplied crew portraits exactly. (+ 공통 스타일 문장)

### 3. ship_stage5.png (5단계 배)
`ship_stages.png`와 같은 그림체로 **5단계 배 한 줄**을 그려 줘.
- 512×128, 4칸 (한 칸 128×128), 배경은 마젠타 #FF00FF.
- 4칸은 꾸미기 단계 4개야. 왼쪽부터 기본, 덧댄 돛, 청록 테두리 돛, 대포와 빨간 깃발. 지금 1~4단계와 같은 규칙이야.
- 4단계보다 조금 더 크고 화려하게 그려 줘. 돛대 3개, 2층 선실, 금빛 장식을 넣어 줘.
- 배의 수면선(배 바닥이 물에 닿는 높이)은 다른 단계와 같은 높이로 맞춰 줘 (칸 아래에서 약 4%).
- 뱃머리의 **파란 끈**은 꼭 그대로 넣어 줘.

> One row of four pixel art sailing ships, 512x128, each cell 128x128, solid pure magenta #FF00FF background, ship facing right, the biggest ship of the series: three masts, two-deck cabin with a green roof, gold trims, blue cord tied at the bow. Cell 1 plain cream sails, cell 2 patched sails, cell 3 sails with teal edges, cell 4 teal-edged sails plus a deck cannon and a small red flag at the top of the main mast. The waterline sits at the same height in every cell, about 4% above the bottom. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order6.zip
  data/23_crew_missions.json, 24_ship_stage5.json
  art/ship_deco_on.png, crew_story_art.png, ship_stage5.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
