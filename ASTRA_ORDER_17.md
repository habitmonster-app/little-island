# 리틀 아일랜드: 아스트라에게 열일곱 번째 부탁 (앱 아이콘 4종)

아스트라, 앱 이름은 **리틀 아일랜드 (Little Island)**로 확정됐어.
이제 스토어에 올릴 **앱 아이콘**이 필요해. 네 가지 방향을 다 그려 보고 고를 거야.

참고 그림: 저장소 `store/icon_concepts.png` (맨 왼쪽은 지금 아이콘, 나머지는 우리가 있는 그림으로 대충 붙여 본 시안 A~D)

공통 규칙
- 각 **1024×1024**, 꽉 찬 정사각형이야. **둥근 모서리는 그리지 마.** 폰이 알아서 깎아.
- 중요한 것(배, 얼굴)은 **가운데 66% 안**에 넣어 줘. 가장자리는 폰마다 잘려.
- **글자, 숫자, 로고는 넣지 마.**
- 배경은 마젠타가 아니라 **그림 배경까지 꽉 채워** 줘 (아이콘이니까).
- 손톱만 하게(48×48) 줄여도 무엇인지 알아보게, 큰 모양과 진한 테두리로 그려 줘.
- 지금 게임 그림과 같은 느낌: 꿀갈색 선체, 상아색 돛, 파란 끈.
- 마지막에 zip 하나로 돌려줘 (`art/`). 코드는 고치지 마.

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> app icon, full-bleed square, no rounded corners, main subject inside the center 66 percent, bold simple shapes readable at 48 pixels, same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, no text, no letters, no numbers, no watermark

---

### A. icon_a_ship_sunset.png (큰 배와 석양)
- 오른쪽을 보는 큰 배 한 척이 화면을 거의 채워. 뒤에 크고 둥근 석양 해.
- 돛은 3장 정도로 단순하게. 작게 줄였을 때 돛이 뭉개지지 않게.
> A heroic trading ship with honey-brown hull and three big ivory sails, facing right, filling most of the frame, a large round orange sunset sun behind it, calm blue sea at the bottom. (+ 공통 스타일 문장)

### B. icon_b_island_ship.png (섬과 배)
- 둥글고 아담한 작은 섬 하나 (나무 한 그루, 작은 집 하나, 부두), 옆에 작은 배.
- 섬이 납작하지 않게, **위에서 조금 내려다본 둥근 섬**으로 가운데에 크게.
> A small round cozy island seen slightly from above, one big round tree, one tiny house and a little wooden dock, a small honey-brown sailboat with an ivory sail moored beside it, turquoise sea around. (+ 공통 스타일 문장)

### C. icon_c_crew_face.png (선원 얼굴)
- 밀짚모자를 쓴 여울(갑판장)의 얼굴과 어깨. 활짝 웃는 얼굴. 뒤에 파란 하늘과 바다, 작은 돛 하나.
- 지금 `worker_portraits` 그림의 여울과 같은 사람이어야 해.
> A close-up of a cheerful young sailor with a wide straw hat and short brown hair (the same crew member as the supplied portrait), big warm smile, shoulders in a navy coat, blue sky and sea behind with one tiny ivory sail on the horizon. (+ 공통 스타일 문장)

### D. icon_d_night_ship.png (밤바다 배)
- 짙은 남색 밤바다 위의 배 한 척. 배에 따뜻한 등불이 켜져 있고, 하늘에 초승달과 별 몇 개.
- 어두운 아이콘 사이에서도 보이게 **등불과 돛을 밝게**.
> A trading ship with honey-brown hull and glowing warm lanterns sailing on a deep navy night sea, bright ivory sails catching moonlight, a crescent moon and a few stars, the lantern glow reflected on the water. (+ 공통 스타일 문장)

---

## 돌려줄 것
- `art/icon_a_ship_sunset.png`, `art/icon_b_island_ship.png`, `art/icon_c_crew_face.png`, `art/icon_d_night_ship.png`
