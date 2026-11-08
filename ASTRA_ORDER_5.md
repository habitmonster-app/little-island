# 리틀 아일랜드: 아스트라에게 다섯 번째 부탁

아스트라, 4차 자료(배 부품, 선원, 그림 9장)는 build-21~23에 모두 넣었어. 앱 이름은 이제 **리틀 아일랜드** (영문 **Little Island**)야.
게임의 중심은 **배 업그레이드와 선원**이니, 이번엔 선원 이야기와 배 꾸미기를 채워 줘. 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터

### 21_crew_stories.json (선원 이야기)
선원 12명 각자에게:
```json
{ "crew": [ {
  "worker_id": "yeoul",
  "deck_lines": ["항해 중 갑판에서 하는 짧은 말 5개"],
  "fight_lines": ["해적과 싸울 때 짧은 말 2개"],
  "levelup_line": "레벨이 올랐을 때 한 줄",
  "promote_story": { "title": "...", "summary": "...", "body": "알바에서 선원이 되는 날 짧은 이야기 (400~700자)" },
  "lv5_story":  { "title": "...", "summary": "...", "body": "Lv5가 되면 열리는 짧은 이야기" },
  "lv10_story": { "title": "...", "summary": "...", "body": "Lv10이 되면 열리는 짧은 이야기" }
} ],
  "pair_lines": [ { "ids": ["yeoul", "dori"], "lines": ["짝 보너스 둘이 같이 탔을 때 주고받는 말 3개"] } ] }
```
- 말은 한 줄 18자 안쪽 (배경화면 말풍선에 들어가야 해)
- 이야기 톤은 '돌아오는 바다'와 같게, 다치거나 죽는 일 없이
- 서린 선장이 가끔 등장해도 좋아

### 22_ship_cosmetics.json (배 꾸미기)
이미 받은 `ship_deco.png` (깃발 4, 뱃머리 장식 4, 돛 무늬 4, 갑판 물건 4) 16개를 쓰는 꾸미기:
```json
{ "items": [ { "id": "flag_red", "slot": "flag|figurehead|sail|deck", "name": "...", "icon_index": 0,
  "how_to_get": "voyages_30 | treasure_3 | crew_lv5_any | port_xxx | gold 500 ...", "desc": "한 줄", "effect": null } ] }
```
- 꾸미기는 **능력치 없이 모습만** 바꿔 (모으는 재미용)
- 칸(slot)마다 하나씩 달 수 있어

### 17_english.json (영어판, 이번에 해 줘)
영어판 이름은 **Little Island**. 08 홈 대사, 09 결과 카드, 10 앱 문구, 14 스토어 문구, 19 부품 이름·설명, 20 선원 역할·스킬 이름·설명, 본편 60화와 외전 제목·요약을 영어로.
```json
{ "app_name": "Little Island", "ui": { "짓기": "Build", "...": "..." }, "parts": {}, "crew": {}, "episodes": [ { "id": "main_01", "title": "...", "summary": "..." } ], "store": {} }
```

---

## 2부. 그림 (파도를 낮춘 바다 배경 4장)

지금 바다 배경은 파도 띠가 너무 높아서 배가 파도에 묻혀 보여. 같은 그림을 **파도만 낮게** 다시 그려 줘.
공통: 가로 3:2, 왼쪽·오른쪽 끝이 이어지게, **수평선(물 표면)은 정확히 높이 50%**, 그 위의 흰 물결 띠는 **아주 얇게 (그림 높이의 2% 이하)**, 물 아래 단면은 그대로.

**bg_sea.png**
> Exactly the same seascape as the supplied sea background, but the sea surface is calm: the water surface line is exactly at 50% of the image height, with only a very thin line of small white foam (no taller than 2% of the image height) instead of tall waves. Bright blue sky with fluffy clouds above, deep sea cross-section with rocks, seaweed and bubbles below. Left and right edges tile seamlessly. Landscape 3:2. same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, limited warm palette, side view, no text, no letters, no numbers, no watermark

**bg_sea_dusk.png**, **bg_sea_night.png**, **bg_storm.png**
> 위 문장에서 날씨만 바꿔 줘: 저녁(주황·분홍 하늘, 보랏빛 구름) / 밤(남색 하늘, 별, 작은 달) / 비바람(짙은 회색 구름, 빗줄기, 물결 띠는 조금 더 두껍게 3%까지).

---

## 돌려줄 zip
```
little-island-order5.zip
  data/21_crew_stories.json, 22_ship_cosmetics.json, 17_english.json
  art/bg_sea.png, bg_sea_dusk.png, bg_sea_night.png, bg_storm.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야. 그때 해 줘.
- **15 출시 서류 채우기**: 운영자 이름, 문의 이메일, 시행일은 사람이 정해야 해서 나중에.
