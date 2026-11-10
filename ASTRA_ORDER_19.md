# 리틀 아일랜드: 아스트라에게 열아홉 번째 부탁 (출렁이는 수면)

아스트라, 18번 그림은 build-81에 모두 넣었어. 마을 건물, 새 선원 카드, 강화 반짝임 다 잘 나와.

이번에는 **바다 수면 물결** 한 가지야.
지금 배경(`bg_sea.png`)의 수면은 일자로 곧아. 그래서 build-82에서 코드로 출렁이는 물결을 임시로 그려 덮었어.
이걸 진짜 그림으로 바꾸고 싶어.

참고 그림 (저장소에 있어)
- 지금 임시 물결: `screenshots/build-82/wave_surface.png`
- 배경: `app/src/main/assets/art/bg_sea.png` (1536×1024, 수면이 정확히 높이 50%)

공통 규칙
- 배경은 **마젠타 #FF00FF** 단색 (물결 위 하늘 자리만 마젠타).
- **글자, 숫자는 넣지 마.**
- 마지막에 zip 하나로 돌려줘 (`art/`). 코드는 고치지 마.

공통 스타일 문장 (그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, limited palette, side view, no text, no letters, no numbers, no watermark

---

## sea_surface.png (흘러가는 수면 물결, 4장면)
- **1024×512**, 1열 × 4행 (한 줄 1024×128). 위에서 아래로 1→2→3→4장면.
- 한 줄 = 옆으로 끝없이 이어 붙이는 수면 띠. **왼쪽 끝과 오른쪽 끝이 딱 이어져야 해** (반복 무늬).
- 한 줄 안의 모양:
  - 위쪽: 마젠타 (하늘이 비칠 자리).
  - 가운데(줄 높이 40~60% 근처): 부드럽게 출렁이는 **물마루**. 한 줄에 물결 4~5개, 높이 차이는 줄 높이의 15~20%.
  - 물마루 꼭대기: 하얀 거품 몇 점.
  - 물결 아래: 청록 띠 **#02C2CF** → 맨 아래 줄은 짙은 파랑 **#0174C5**로 끝나야 해 (배경 바다색과 이어지게).
- 4장면은 물결이 **오른쪽에서 왼쪽으로 조금씩 밀려가는** 순서야. 4장면 다음에 1장면으로 돌아와도 자연스럽게.
> A 4-frame horizontally seamless tiling strip of a gently rolling sea surface, 1024x512, 4 rows of 1024x128, each row: flat #FF00FF magenta sky area on top, soft rolling wave crests in the middle with small white foam on the crest tops, a turquoise #02C2CF water band under the crests fading to deep blue #0174C5 at the very bottom edge, four or five waves per row, the waves shift slightly to the left from frame to frame so the 4 frames loop smoothly, left and right edges tile seamlessly. (+ 공통 스타일 문장)

## 저녁·밤 색 (같은 모양, 색만 다르게)
- `sea_surface_dusk.png`: 같은 규격. 물 띠는 저녁놀이 비친 분홍빛 보라, 아래는 짙은 남보라.
- `sea_surface_night.png`: 같은 규격. 물 띠는 짙은 청록, 아래는 거의 검은 남색, 거품은 은은한 하늘색.
- 물결 모양과 장면 순서는 낮 그림과 **똑같이** 맞춰 줘 (시간이 바뀔 때 겹쳐서 섞어).

---

## 돌려줄 것
- `art/sea_surface.png`, `art/sea_surface_dusk.png`, `art/sea_surface_night.png`
