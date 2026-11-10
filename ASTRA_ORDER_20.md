# 리틀 아일랜드: 아스트라에게 스무 번째 부탁 (배 장식 다시 그리기)

아스트라, 19번 물결 그림은 build-84에 잘 들어갔어. 고마워!

이번에는 **배 장식**이야.
build-86부터 배를 "개조"하면 배에 장식이 하나씩 달려 (돛 무늬 → 깃발 → 뱃머리 → 갑판 물건 순서).
그런데 지금 장식 그림이 너무 작고 대충 붙은 느낌이라, 배에 딱 맞는 **크고 또렷한 장식**으로 다시 그려 줬으면 해.

참고 그림 (저장소에 있어)
- 지금 모습: `screenshots/build-88/refit_decos.png` (빨간 동그라미 친 깃발, 뱃머리 자리가 어색해)
- 배 그림: `app/src/main/assets/art/ship_stage6.png` (512×128, 4장면). 이 배에 붙는다고 생각하고 그려 줘.
- 지금 장식 그림: `app/src/main/assets/art/ship_deco_on.png`, `ship_deco_on_2.png`, `ship_deco_on_3.png` (무늬와 색은 이걸 따라 줘)

공통 규칙
- 배경은 **마젠타 #FF00FF** 단색. 그림자, 바닥, 배 몸통은 그리지 마 (**장식만**).
- **글자, 숫자, 로고는 넣지 마.**
- 배는 **오른쪽을 보고** 있어 (뱃머리가 오른쪽).
- 굵고 진한 테두리로, 배 그림 위에 올려도 **작은 화면에서 잘 보이게** 단순하고 큼직하게.
- 한 번에 한 장씩 그려 줘. 마지막에 zip 하나로 돌려줘 (`art/`). 코드는 고치지 마.

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, facing right, no text, no letters, no numbers, no watermark

---

## 1. deco_flags.png (깃발 12개)
- **1024×768**, 4열 × 3행 (한 칸 256×256). 왼쪽 위부터 오른쪽으로, 줄 바꿔서 1→12.
- **깃발 천만** 그려 (깃대 없음). 천의 **왼쪽 가운데**가 칸 왼쪽 끝 가운데에 딱 붙어 있고, 천은 **오른쪽으로 펄럭여**. 칸 가로의 80% 정도 크기.
- 순서:
  1. 붉은 깃발 (첫 출항) 2. 파란 깃발 3. 노란 바탕에 파란 물결 4. 초록 바탕에 마름모
  5. 남색 천에 은빛 갈고리 6. 하얀 천에 붉은 게 집게 7. 색색 조각을 이은 깃발 8. 남색 천에 금빛 닻과 술
  9. 산호빛 천에 해마 10. 하늘색 천에 돌아오는 물길 무늬 11. 분홍·금빛 아침놀 비단 12. 연두 천에 갈대 세 줄기
> A sprite sheet of 12 ship flags, 1024x768, 4 columns and 3 rows, each cell 256x256. Only the flag cloth, no pole. In every cell the left middle edge of the cloth touches the left edge of the cell at mid height and the cloth waves to the right, about 80% of the cell width. In order: plain red flag; plain blue flag; yellow flag with a blue wave stripe; green flag with a diamond; navy flag with a silver hook; white flag with a red crab claw; patchwork flag of many colored pieces; navy flag with a gold anchor and tassels; coral flag with a seahorse; sky blue flag with a returning wave path; pink and gold silk dawn flag; light green flag with three reeds. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

## 2. deco_figureheads.png (뱃머리 장식 12개)
- **1024×768**, 4열 × 3행 (한 칸 256×256).
- 뱃머리 끝에 붙이는 **조각 장식**. 오른쪽을 보고 있고, **왼쪽 아래 모서리**가 배에 붙는 자리 (칸 왼쪽 아래에 딱 붙게). 칸의 70% 정도 크기.
- 순서:
  1. 웃는 갈매기 2. 주홍 물고기 3. 금빛 보물별 4. 작은 등불
  5. 청록 문어 6. 번개를 품은 둥근 구름 7. 청록 리본으로 묶은 조개 꽃다발 8. 뛰어오르는 금빛 돌고래
  9. 지느러미를 편 날치 10. 날개를 편 갈매기 11. 진주빛 돌고래 12. 분홍 소라
> A sprite sheet of 12 carved ship figurehead ornaments, 1024x768, 4 columns and 3 rows, each cell 256x256. Each ornament faces right and its bottom left corner touches the bottom left corner of the cell where it attaches to the bow, about 70% of the cell size, painted carved wood and gold look. In order: smiling seagull; orange fish; golden treasure star; small glowing lantern; teal octopus; round cloud holding a small lightning bolt; bouquet of shells tied with a teal ribbon; leaping golden dolphin; flying fish with wide fins; seagull with open wings; pearl white dolphin; pink conch shell. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

## 3. deco_sails.png (돛 무늬 12개)
- **1024×768**, 4열 × 3행 (한 칸 256×256).
- 크림색 돛 위에 그려지는 **무늬만** (돛은 그리지 마). 칸 **가운데**에, 칸의 60% 정도 크기. 납작한 그림 (그림자 없이).
- 순서:
  1. 가로 파란 띠 2. 붉은 해 3. 초록 잎새 4. 금빛 물결
  5. 금빛 동전 6. 구릿빛 물결과 암초 7. 여덟 방향 나침반 별 8. 금빛 선으로 이은 별자리
  9. 저녁 등불 10. 별과 작은 열쇠 11. 물결 위 금빛 해 12. 종이새
> A sprite sheet of 12 flat sail emblems, 1024x768, 4 columns and 3 rows, each cell 256x256. Only the painted emblem, no sail, centered in each cell, about 60% of the cell, flat colors without shading. In order: a horizontal blue stripe; a red sun; a green leaf; golden waves; a gold coin; copper waves with a small reef; an eight point compass star; a gold line constellation; an evening lantern; a star with a small key; a golden sun over ripples; a folded paper bird. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

## 4. deco_deck.png (갑판 물건 12개)
- **1024×768**, 4열 × 3행 (한 칸 256×256).
- 갑판 위에 놓는 물건. 칸 **아래쪽 가운데**에 딱 붙게 세워. 칸의 60% 정도 크기.
- 순서:
  1. 나무 걸이에 단 작은 종 2. 노란 불빛 등잔 3. 꽃 세 송이 화분 4. 받침 위 망원경
  5. 초승달 매단 남색 등불 6. 바다뱀이 두른 초록 상자 7. 작은 지구본 8. 금빛 선장 시계
  9. 웃는 민트 거북 장식 10. 찻주전자 11. 황동 천문환 12. 지도 상자
> A sprite sheet of 12 small ship deck props, 1024x768, 4 columns and 3 rows, each cell 256x256, each standing on the bottom center of its cell, about 60% of the cell. In order: a small bell hanging on a wooden post; a lantern with a warm yellow glow; a pot with three flowers; a spyglass on a stand; a navy lantern with a silver crescent moon; a green chest wrapped by a white sea serpent; a small globe; a golden captain clock; a smiling mint turtle ornament; a teapot; a brass astrolabe; a box of folded sea charts. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

---

## 돌려줄 것
- `art/deco_flags.png`, `art/deco_figureheads.png`, `art/deco_sails.png`, `art/deco_deck.png`
