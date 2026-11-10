# 리틀 아일랜드: 아스트라에게 열여덟 번째 부탁 (마을 건물 6개, 새 선원 카드, 강화 반짝임)

아스트라, build-79에서 게임이 이렇게 바뀌었어.
- 배가 커질 때마다 섬에 **큰 건물이 하나씩** 생겨. 지금은 코드로 대충 그린 임시 그림이야.
- 견습선원이 승진하면 **"새 선원!" 카드**가 뽑기처럼 튀어나와. 카드 틀도 지금은 임시야.
- 강화할 때마다 **"+12%"** 숫자가 크게 튀어. 뒤에 반짝이는 터짐 효과가 있으면 좋겠어.

참고 그림 (저장소에 있어)
- 지금 임시 건물: `screenshots/build-79/village_growth.png` (배 1~10단계 섬 모습)
- 지금 건물 그림: `app/src/main/assets/art/buildings.png` (이것과 **같은 크기감, 같은 바닥선**으로)
- 새 선원 연출: `screenshots/build-79/new_crew.png`
- 선원 얼굴: `app/src/main/assets/art/worker_portraits.png`

공통 규칙
- 배경은 **마젠타 #FF00FF** 단색. 그림자나 바닥 땅은 그리지 마 (섬 풀밭 위에 올려).
- **글자, 숫자, 로고는 넣지 마.** 글은 앱이 써.
- 한 칸 안에서 **아래쪽 바닥선에 딱 붙게** 세워 줘 (지금 buildings.png와 같게).
- 마지막에 zip 하나로 돌려줘 (`art/`). 코드는 고치지 마.
- 한 번에 한 장씩 그려 줘 (여러 장을 한꺼번에 하면 잘 안 됐어).

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

---

## 1. landmarks.png (배 단계마다 생기는 마을 건물 6개)
- **1536×512**, 6열 × 1행 (한 칸 256×512, 세로로 길어). 배경 마젠타.
- 집(buildings.png의 집)보다 **훨씬 높게**, 지붕 위로 솟아 보이게. 칸 높이의 70~90%.
- 왼쪽부터 순서대로:
  1. **섬 깃발** (배 2단계): 키 큰 나무 깃대에 청록·상아 줄무늬 깃발이 바람에 펄럭임. 깃대 꼭대기에 금색 장식.
  2. **풍차** (배 4단계): 하얀 돌로 쌓은 둥근 탑, 빨간 지붕, 나무 날개 4장 (날개는 X 모양).
  3. **등불 기둥** (배 5단계): 나무 기둥 두 개 사이에 늘어진 줄, 줄에 빨강·노랑 종이 등불 5개가 켜져 있음.
  4. **종탑** (배 7단계): 회색 돌탑, 파란 뾰족 지붕, 가운데 열린 창에 금색 종.
  5. **해적 깃발 트로피** (배 8단계): 이긴 해적들의 검은 깃발 3개가 높이가 다른 깃대에 꽂혀 있음 (해골 대신 **조개·갈고리·게** 같은 귀여운 문양).
  6. **황금 동상** (배 10단계): 돌 받침 위에 망원경을 든 선장 금빛 동상, 반짝임 몇 개.
> A sprite sheet of six tall village landmark buildings in a row, 6 columns and 1 row, 1536x512, each cell 256x512, each standing on the bottom edge of its cell: a tall wooden flagpole with a waving teal and ivory striped flag and a gold finial; a round white stone windmill with a red roof and four wooden sails in an X shape; two wooden posts with a sagging rope of five glowing red and yellow paper lanterns; a grey stone bell tower with a pointed blue roof and a gold bell in an open arch; three black pirate trophy flags on poles of different heights with cute shell, hook and crab emblems; a golden captain statue holding a spyglass on a stone pedestal with a few sparkles. Much taller than a small house. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

## 2. new_crew_card.png (새 선원! 카드 틀)
- **512×640** 한 장. 배경 마젠타.
- 세로로 긴 카드. 진한 나무 판에 **금색 테두리**, 네 귀퉁이에 작은 닻 장식, 위쪽 가운데에 빈 리본(글자 없음).
- 카드 위쪽 **절반은 비워 둔 창** (선원 얼굴이 들어가). 창 안은 밝은 하늘색 단색.
- 아래쪽 절반은 짙은 갈색 단색 (이름과 한마디를 앱이 써).
> A tall collectible crew card frame, 512x640, dark polished wood panel with an ornate gold border, small gold anchor ornaments in the four corners, an empty blank ribbon banner at the top center, the upper half is an empty window filled with flat light sky blue for a portrait, the lower half is a flat dark brown area for text, nothing drawn inside the window. Solid flat #FF00FF magenta background around the card. (+ 공통 스타일 문장)

## 3. upgrade_burst.png (강화 반짝 터짐, 4장면)
- **1024×256**, 4열 × 1행 (한 칸 256×256). 배경 마젠타.
- 강화 숫자 뒤에서 "팡!" 하고 터지는 금빛 별 모양. 1장면 작게 → 2·3장면 크게 퍼짐 → 4장면 작은 반짝이만 남음.
- 가운데는 비워 둬 (숫자가 올라가).
> A 4-frame animation strip, 1024x256, each cell 256x256, a golden starburst pop effect centered in each cell: frame 1 a small bright gold star burst, frame 2 a big burst of gold rays and coins sparkles, frame 3 the rays spreading wider and fading, frame 4 only a few small sparkles left, the very center of each frame kept empty. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

---

## 아직 남은 것 (16번 부탁)
- 16번 부탁(선원 Lv20~50 스킬 이름, Lv30·Lv50 얼굴 2장)도 아직 기다리고 있어. 같이 보내 주면 한 번에 넣을게.

## 돌려줄 것
- `art/landmarks.png`, `art/new_crew_card.png`, `art/upgrade_burst.png`
