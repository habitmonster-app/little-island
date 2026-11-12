# 리틀 아일랜드: 아스트라에게 스물두 번째 부탁 (개조된 배 8~10단계)

아스트라, 21번 개조 배 그림 (2~7단계) 고마워! 게임에 넣었더니 아주 자연스러워.
이번에는 21번에서 남은 **배 8, 9, 10단계**만 그려 줘. 방식은 21번과 똑같아.

참고 그림 (저장소 `docs/astra/order22/`, zip 안에도 있어. **배마다 그 배 그림을 꼭 같이 올려 줘**)
- `ref_ship_8.png`, `ref_ship_9.png`, `ref_ship_10.png`: 지금 배 8~10단계 그림 (512×512, 마젠타 배경). 이 배를 **그대로 두고** 장식만 자연스럽게 더해 줘.
- `example_refit_7.png`: 21번에 그려 준 7단계 개조 배. **이 느낌 그대로** 맞춰 줘.

공통 규칙 (21번과 같아)
- 그림 한 장 = 한 배 단계. **1024×512**, 2열 × 1행 (한 칸 512×512). **왼쪽 = 개조 1, 오른쪽 = 개조 2**.
- 배의 **크기, 위치, 모양, 색, 돛대 수는 참고 그림과 똑같이**. 배 바닥이 칸 아래쪽의 같은 자리에 오게. 배가 더 커지거나 옮겨지면 안 돼.
- 배는 **오른쪽을 보고** 있어 (뱃머리가 오른쪽).
- 장식은 배의 일부처럼: 돛 무늬는 돛에 칠한 것처럼, 깃발은 돛대 꼭대기에, 뱃머리 장식은 뱃머리 끝에 조각한 것처럼.
- 배경은 **마젠타 #FF00FF** 단색. 바다, 물결, 그림자는 그리지 마.
- **글자, 숫자, 로고는 넣지 마.**
- 한 번에 한 장씩. 마지막에 zip 하나로 돌려줘 (`art/`). 코드는 고치지 마.

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> Keep the ship from the reference image exactly the same size, position, shape and colors, only add the decorations naturally as part of the ship. same style as the reference ship and the example refit ship, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, facing right, solid flat #FF00FF magenta background, no sea, no shadow, no text, no letters, no numbers, no watermark

---

## 1. ship_refit_8.png (참고: ref_ship_8)
- 왼쪽 개조 1: 큰 돛에 금빛 별자리, 남색 천에 금빛 닻과 술 깃발
- 오른쪽 개조 2: 개조 1 + 뛰어오르는 금빛 돌고래 뱃머리, 갑판에 금빛 선장 시계, 금빛 테두리 조금
> Using the attached ship as the base, draw a 1024x512 image with 2 cells of 512x512 side by side. Left cell: the same ship with a gold constellation painted on the main sail and a navy flag with a gold anchor and tassels on the top of the main mast. Right cell: the same ship with a gold constellation painted on the main sail and a navy flag with a gold anchor and tassels, plus a leaping golden dolphin figurehead on the bow and a golden captain clock on the deck, and a little gold trim on the hull. (+ 공통 스타일 문장)

## 2. ship_refit_9.png (참고: ref_ship_9)
- 왼쪽 개조 1: 큰 돛에 저녁 등불, 산호빛 천에 해마 깃발
- 오른쪽 개조 2: 개조 1 + 지느러미를 편 날치 뱃머리, 갑판에 황동 천문환, 금빛 테두리 조금
> Using the attached ship as the base, draw a 1024x512 image with 2 cells of 512x512 side by side. Left cell: the same ship with an evening lantern painted on the main sail and a coral flag with a seahorse on the top of the main mast. Right cell: the same ship with an evening lantern painted on the main sail and a coral flag with a seahorse, plus a flying fish figurehead with wide fins on the bow and a brass astrolabe on the deck, and a little gold trim on the hull. (+ 공통 스타일 문장)

## 3. ship_refit_10.png (참고: ref_ship_10)
- 왼쪽 개조 1: 큰 돛에 물결 위 금빛 해, 분홍·금빛 아침놀 비단 깃발
- 오른쪽 개조 2: 개조 1 + 진주빛 돌고래 뱃머리, 갑판에 지도 상자, 금빛 테두리 조금
> Using the attached ship as the base, draw a 1024x512 image with 2 cells of 512x512 side by side. Left cell: the same ship with a golden sun over ripples painted on the main sail and a pink and gold dawn silk flag on the top of the main mast. Right cell: the same ship with a golden sun over ripples painted on the main sail and a pink and gold dawn silk flag, plus a pearl white dolphin figurehead on the bow and a box of folded sea charts on the deck, and a little gold trim on the hull. (+ 공통 스타일 문장)

---

## 돌려줄 것
- `art/ship_refit_8.png`, `art/ship_refit_9.png`, `art/ship_refit_10.png` (3장)
