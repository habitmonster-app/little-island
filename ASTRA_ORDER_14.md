# 리틀 아일랜드: 아스트라에게 열네 번째 부탁 (바닷속)

아스트라, 13차 자료는 build-43에 모두 넣었어.
- 6단계 배 "귀항호 · 먼바다의 집"과 부품 Lv26~30이 들어갔어.
- 바다 동물 도감(톡 치면 기록), 주간 바다 소식 12개, 손님 이야기 24편도 다 돼.
- 2·3단계 섬도 계절에 따라 나무가 바뀌고, 항로 칸에 항구 문장이 붙어.

이번 부탁은 **바닷속**이야.
홈 화면 아래쪽 절반은 물속인데, 지금은 파란색만 있어서 비어 보여.
여기에 **바닥 모래, 산호, 해초, 거품, 작은 물고기**를 넣어서 늘 조금씩 움직이게 하고 싶어.

공통 규칙
- 화면에 나오는 글은 **짧게**. 이름 6자, 한 줄 16자, 대사 18자 안쪽.
- 숫자와 %는 화면 글에 넣지 마. 앱이 붙여.
- 다치거나 죽는 일 없이, 지금 이야기 톤과 같게 써 줘.
- 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

**바닷속 화면 구조** (그림을 그릴 때 참고해 줘)
- 장면은 가로 200칸, 세로 467칸이야. 수면은 위에서 252칸, 바닷속은 그 아래 약 200칸이야.
- 바닥은 화면 맨 아래에 깔고, 그 위로 산호와 해초가 서 있어. 거품과 물고기는 그 사이 물속을 지나가.
- 낮에는 위에서 빛줄기가 비치고, 밤에는 앱이 전체를 어둡게 덮어. 그러니 그림은 **낮 기준**으로만 그려 줘.

---

## 1부. 데이터 (2개)

### 52_seabed.json (바닷속 친구와 정원)
바닷속 친구들은 지금 도감(48번)에 **이어서** 들어가. 톡 치면 도감에 기록돼.
바닷속 정원은 항해를 많이 할수록 산호와 해초가 조금씩 늘어나.

```json
{
  "creatures": [ {
    "id": "crit_crab",
    "kind": 8,
    "name": "이름 (6자 안쪽)",
    "short": "한 줄 (16자 안쪽)",
    "first_line": "처음 봤을 때 선원 한마디 (18자 안쪽)",
    "seen_by": "선원 id",
    "reward": { "tickets": 1, "gold": 50 },
    "icon_index": 0
  } ],
  "garden": [
    { "voyages": 0,   "coral": 2, "seaweed": 2, "news": "" },
    { "voyages": 30,  "coral": 3, "seaweed": 3, "news": "정원 소식 한 줄 (24자 안쪽)" }
  ],
  "pearl": { "chance_per_hour": 0.5, "reward": { "tickets": 1, "gold": 80 }, "line": "진주를 주웠을 때 한마디 (18자 안쪽)" }
}
```
- **creatures**: 바닷속 친구 **6마리**를 써 줘. kind는 아래 번호 그대로야.
  - 8 꽃게, 9 흰동가리 떼, 10 불가사리, 11 해마, 12 거북복, 13 작은 가오리
  - 첫 보상은 48번처럼 쪽지 1장과 금화 30~80이야.
  - icon_index는 아래 그림 5의 0~5칸이야.
- **garden**: 정원이 자라는 단계 **6개**야.
  - voyages는 0, 30, 80, 150, 300, 500으로 해 줘.
  - coral과 seaweed는 화면에 놓이는 개수야. 각각 2부터 6까지, 단계마다 늘려 줘.
  - news는 정원이 자랄 때 귀항 카드에 붙는 한 줄이야. 첫 단계는 빈칸으로 둬.
- **pearl**: 바닥의 조개가 가끔 입을 열고 진주를 보여 줘. 그때 톡 치면 보상을 받아.
  - chance_per_hour는 한 시간에 열릴 확률이야. 0.3~0.6으로 해 줘.
  - 보상은 쪽지 1장과 금화 50~100이야.

### 53_seabed_lines.json (선원 바닷속 한마디)
배가 섬에 있을 때 선원이 물속을 내려다보며 하는 말이야.

```json
{ "lines": { "yeoul": ["18자 안쪽", "...3개"], "...": "12명 모두" } }
```
- 12명 × 3개 = **36줄**이야.
- 산호, 거품, 물고기, 진주 조개를 보고 하는 말로 써 줘.

---

## 2부. 그림 (6장)

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

1~5번 그림은 배경이 순수한 마젠타 #FF00FF야. 물은 그리지 마. 앱이 물색을 깔아.

### 1. seabed.png (바다 밑바닥)
- 2048×256, 한 장짜리 가로로 긴 띠야. **왼쪽 끝과 오른쪽 끝이 이어지게** 그려 줘 (배가 갈 때 옆으로 흘러가).
- 띠 아래쪽 절반은 고운 모래와 작은 조약돌이야. 위쪽에는 낮은 바위 몇 개와 조개껍데기만 그려 줘.
- **산호, 해초, 동물은 넣지 마.** 2·3번 그림에서 따로 올려.
- 띠 위쪽 3분의 1은 비워 줘 (마젠타).

> A seamless horizontal tile strip, 2048x256, solid pure magenta #FF00FF background, a sandy sea floor seen from the side: fine pale sand with small pebbles and a few shells in the lower half, a few low rounded rocks, the top third left empty. The left and right edges must tile seamlessly. No water, no coral, no seaweed, no animals. (+ 공통 스타일 문장)

### 2. coral_sway.png (흔들리는 산호와 해초)
- 1024×1024, 4열 × 4행 (한 칸 256×256). 한 줄이 한 식물의 움직임 4장이야. 4장을 이어 붙이면 **부드럽게 반복**되게 그려 줘 (4장 다음 1장으로 자연스럽게).
- 모든 식물의 **밑동은 칸 아래 가운데**에 오게 해 줘. 높이는 칸의 60~90%.
- 1줄: 키 큰 초록 해초. 물결에 좌우로 흔들려.
- 2줄: 갈색 다시마 두세 줄기. 천천히 흔들려.
- 3줄: 분홍 가지 산호. 작은 촉수가 열렸다 닫혀.
- 4줄: 주황 말미잘. 촉수가 꿈틀거려.

> A 4x4 sprite sheet, 1024x1024, each cell 256x256, solid pure magenta #FF00FF background, underwater plants seen from the side, the base of every plant at the bottom center of its cell, four looping frames per row: row 1 a tall green seaweed swaying left and right, row 2 two or three brown kelp stalks swaying slowly, row 3 a pink branching coral whose tiny polyps open and close, row 4 an orange sea anemone with wiggling tentacles. No water. (+ 공통 스타일 문장)

### 3. seabed_life.png (거품, 물고기, 바닥 친구)
- 1024×1536, 4열 × 6행 (한 칸 256×256). 한 줄이 한 움직임 4장이야.
- 1줄: 거품 방울 무리가 올라가는 모습 4장 (크고 작은 방울 서너 개, 반짝이는 하이라이트).
- 2줄: 작은 물고기 떼가 **왼쪽으로** 헤엄치는 모습 4장 (노란 줄무늬 물고기 다섯 마리쯤).
- 3줄: 꽃게가 옆으로 걷는 모습 4장. 발은 칸 아래에 닿아.
- 4줄: 흰동가리 두세 마리가 왼쪽으로 헤엄치는 모습 4장.
- 5줄: 해마가 위아래로 둥실거리는 모습 4장.
- 6줄: 조개 4장 (닫힘 → 조금 열림 → 활짝 열려 진주가 반짝 → 다시 닫힘). 아래가 칸 바닥에 닿아.

> A 4x6 sprite sheet, 1024x1536, each cell 256x256, solid pure magenta #FF00FF background, underwater life, four animation frames per row: row 1 a cluster of rising air bubbles with shiny highlights, row 2 a small school of five yellow striped fish swimming left, row 3 a red crab walking sideways with its feet on the bottom of the cell, row 4 two or three clownfish swimming left, row 5 a seahorse bobbing up and down, row 6 a clam on the bottom of the cell: closed, slightly open, wide open showing a glowing pearl, closed again. No water. (+ 공통 스타일 문장)

### 4. seabed_life_2.png (바닥 친구 2)
- 1024×768, 4열 × 3행 (한 칸 256×256). 한 줄이 한 움직임 4장이야.
- 1줄: 불가사리가 바닥에서 팔을 살짝 움직이는 모습 4장 (아래가 칸 바닥에 닿아).
- 2줄: 노란 거북복이 왼쪽으로 헤엄치는 모습 4장.
- 3줄: 작은 가오리가 왼쪽으로 날개를 물결치며 헤엄치는 모습 4장.

> A 4x3 sprite sheet, 1024x768, each cell 256x256, solid pure magenta #FF00FF background, four animation frames per row: row 1 an orange starfish on the bottom of the cell gently moving its arms, row 2 a yellow boxfish swimming left, row 3 a small friendly ray swimming left with rippling fins. No water. (+ 공통 스타일 문장)

### 5. seabed_book.png (도감 그림 6개)
- 1024×512, 4×2칸 (한 칸 256×256). 칸 순서는 52번 kind 순서와 같게 해 줘 (꽃게, 흰동가리, 불가사리, 해마 / 거북복, 가오리). 마지막 두 칸은 비워 둬.
- `creature_book.png`와 같은 둥근 물빛 틀이야. 동물 생김새는 그림 3·4와 같게 해 줘.

> A 4x2 icon sheet, 1024x512, each cell 256x256, solid pure magenta #FF00FF background, six round sea-blue framed portraits like the supplied creature_book sheet: a red crab, clownfish, an orange starfish, a seahorse; a yellow boxfish, a small ray; the last two cells empty. (+ 공통 스타일 문장)

### 6. light_rays.png (물속 빛줄기)
- 1024×512, 4열 × 1행 (한 칸 256×512, 세로로 긴 칸). 4장이 천천히 반복되는 움직임이야.
- 위에서 아래로 비스듬히 내려오는 연한 빛줄기 두세 줄이야. **반투명처럼 보이게 점무늬(디더)**로 그려 줘. 아래로 갈수록 옅어져.
- 이 그림만 배경을 **순수한 검정 #000000**으로 해 줘. 앱이 검정을 지우고 밝게 겹쳐.

> A 1x4 animation strip, 1024x512, each cell 256x512, solid pure black #000000 background, two or three soft slanted underwater light shafts coming down from the top, drawn with a pale cyan-white dither pattern so they look translucent, fading toward the bottom, four slowly looping frames. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order14.zip
  data/52_seabed.json, 53_seabed_lines.json
  art/seabed.png, coral_sway.png, seabed_life.png, seabed_life_2.png,
      seabed_book.png, light_rays.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
