# 리틀 아일랜드: 아스트라에게 여덟 번째 부탁

아스트라, 7차 자료는 build-30에 모두 넣었어.
- 해적 우두머리 8척: 해적 대신 가끔 나오고, 처음 이기면 장식과 이야기가 열려.
- 항해 도감 33개: 항구에 닿을 때 가끔 기념품을 얻어.
- 배 꾸미기 2 (16종), 선원 부적 뽑기, 훈련 퍼즐도 다 돼.

build-31에서는 직접 해 본 사람이 **"글이 너무 많고, 도트 글씨가 읽기 어렵다"**고 해서 앱 글씨를 또렷한 글씨로 바꾸고 설명을 줄였어.
그래서 이번 부탁의 첫째는 **짧은 글**이야. 둘째는 오래 하는 사람을 위한 **선원 Lv11~15**야.

마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터

### 29_short_texts.json (짧은 설명)
앱의 설명 창은 이제 3~6줄만 보여 줘. 지금 자료의 설명은 길어서 앱이 많이 잘라 쓰고 있어.
아래 항목마다 **짧은 설명 한 줄**을 써 줘.

**규칙**
- 한 줄, **16자 안쪽** (띄어쓰기 포함).
- 숫자와 %는 넣지 마. 앱이 따로 계산해서 보여 줘.
- "~해요" 말투로. 쉬운 낱말만.
- 예: "짐을 더 많이 실어요", "해적과 싸울 때 강해요"

```json
{
  "parts":     { "hull": "...", "sail": "...", "cannon": "...", "helm": "..." },
  "crew_roles":{ "yeoul": "...", "mungi": "...", ... 12명 },
  "skills":    { "tight_knots": "...", ... 모든 스킬 id },
  "decos":     { "flag_red": "...", ... 22번, 27번 꾸미기 32개 모두 },
  "charms":    { "charm_rope_bracelet": "...", ... 24개 },
  "souvenirs": { "souv_haesol_1": "...", ... 33개 },
  "bosses":    { "boss_hook": "...", ... 8척 },
  "missions":  { "mission_yeoul_1": "...", ... 24개 },
  "buildings": { "house": "...", "store": "...", "dock": "...", "field": "...", "lighthouse": "...", "market": "..." }
}
```
id는 지금 자료(19, 20, 22, 23, 25, 26, 27, 28번)에 있는 그대로 써 줘. 하나도 빠뜨리지 마.

### 30_crew_levels_2.json (선원 Lv11~15)
지금 선원은 Lv10이 끝이야. 배는 5단계, 부품은 Lv25까지 늘었으니 선원도 조금 더 자라게 해 줘.

```json
{
  "max_level": 15,
  "xp_to_next_11_15": [800, 950, 1100, 1300, 1500],
  "skills": [
    { "crew": "yeoul", "level": 12, "id": "...", "name": "8자 안쪽", "effect": "cargo_bonus", "value": 0.03, "short": "16자 안쪽", "icon_index": 0 },
    { "crew": "yeoul", "level": 15, "id": "...", "name": "...", "effect": "...", "value": 0.04, "short": "...", "icon_index": 0 }
  ]
}
```
- 선원 12명 × 스킬 2개 (Lv12, Lv15), 모두 **24개**.
- xp_to_next_11_15 숫자는 예시야. 보통 하루 정도 하면 한 레벨이 오르게 맞춰 줘 (지금 Lv9→10은 660).
- effect는 지금 쓰는 것만: cargo_bonus, speed_bonus, pirate_win_bonus, sale_bonus, event_good_bonus, repair_after_loss, treasure_find_bonus, wood_price_bonus, stone_price_bonus
- 그 선원의 역할과 맞는 효과로 해 줘. 예: 포술장은 pirate_win_bonus, 조타수는 speed_bonus.
- value는 작게 (0.02~0.05). 지금 상한(effect_caps)은 그대로 둘 거야.
- icon_index는 skill_icons.png(4×4)에서 가장 비슷한 칸 번호를 골라 줘.

---

## 2부. 그림

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, limited warm palette, no text, no letters, no numbers, no watermark

### 1. crew_portraits_15.png (Lv11 이상 선원 얼굴)
선원이 Lv11이 되면 얼굴 그림이 이걸로 바뀌어. 조금 더 멋진 선원 옷이야.
- 1024×768, 4×3칸 (한 칸 256×256). 칸 순서는 `worker_portraits.png`와 똑같이.
- 얼굴, 머리 모양, 색은 `worker_portraits.png`와 **똑같이** 맞춰 줘. 옷만 바꿔 줘.
- 옷: 진한 남색 선원 코트, 금빛 단추, 어깨에 작은 금빛 견장. 모자는 원래 쓰던 사람만 그대로.
- 배경은 지금 얼굴 그림과 같은 색 동그라미.

> A 4x3 grid of twelve pixel art character portraits, 1024x768, each cell 256x256, bust shots facing slightly right. The same twelve young sailors as the supplied worker_portraits sheet, same faces, hair and skin colors, same cell order, now wearing upgraded navy sailor coats with small gold buttons and gold shoulder marks, proud happy expressions. (+ 공통 스타일 문장)

### 2. training_crates.png (훈련 퍼즐 상자)
지금 훈련 퍼즐은 같은 상자에 작은 그림을 붙여서 나무·돌·곡식을 나눠. 한눈에 알아보기 어려워.
- 1024×256, 4칸 (한 칸 256×256), 배경은 마젠타 #FF00FF.
- 1칸 나무 상자: 통나무가 보이는 나무 상자, 갈색.
- 2칸 돌 상자: 회색 돌이 담긴 상자, 회색 띠.
- 3칸 곡식 상자: 노란 곡식 자루가 담긴 상자, 노란 띠.
- 4칸 빈 자리: 상자 없이 갑판 바닥 칸만.
- 세 상자는 **색과 모양이 확 달라서** 작게 봐도 바로 구별되게.

> A 1x4 sprite sheet, 1024x256, each cell 256x256, solid pure magenta #FF00FF background, front view. Cell 1: a wooden crate full of brown logs, brown straps. Cell 2: a crate full of grey stones, grey straps. Cell 3: a crate full of yellow grain sacks, bright yellow straps. Cell 4: an empty square of wooden deck floor. The three crates must be very easy to tell apart by color and shape even when small. (+ 공통 스타일 문장)

### 3. upgrade_fx.png (올리기 반짝임)
부품을 올릴 때 칸 위에 잠깐 나오는 반짝 효과야.
- 1024×256, 4칸 (한 칸 256×256), 배경은 마젠타 #FF00FF.
- 4칸은 시간 순서: 작은 빛 → 커지는 금빛 별 → 사방으로 퍼지는 반짝이 → 사라지는 작은 점들.
- 금빛과 크림색만. 칸 가운데에 그려 줘.

> A 1x4 animation strip, 1024x256, each cell 256x256, solid pure magenta #FF00FF background, centered golden sparkle burst effect in four frames: a small glint, a growing gold star, sparkles spreading outward, fading tiny dots. Gold and cream colors only. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order8.zip
  data/29_short_texts.json, 30_crew_levels_2.json
  art/crew_portraits_15.png, training_crates.png, upgrade_fx.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
