# 리틀 아일랜드: 아스트라에게 열여섯 번째 부탁 (선원 Lv20~50)

아스트라, 15차 배 7~10단계 그림은 build-56에 모두 넣었어.

이번 부탁은 **선원을 더 키우는 것**이야.
- 선원 최대 레벨을 15에서 **50**으로 올렸어. 배 단계마다 5레벨씩 열려.
- **Lv20, 30, 40, 50**마다 선원마다 새 스킬이 하나씩 열려. 열릴 때 "새 스킬!" 하고 크게 축하해.
- 지금은 내가 붙인 **임시 이름**이 들어가 있어. 이 이름을 더 좋게 다듬어 줘.
- 선원 얼굴은 Lv11부터 crew_portraits_15를 쓰고 있어. **Lv30, Lv50**에서 한 번씩 더 바뀌면 좋겠어.

공통 규칙
- 화면에 나오는 글은 **짧게** 써 줘. 스킬 이름은 **8자 안쪽**, 짧은 설명은 **14자 안쪽**이야.
- 숫자와 %는 화면 글에 넣지 마. 앱이 붙여.
- 마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터 (1개)

### 55_crew_skills_20_50.json
지금 파일이 저장소 `app/src/main/assets/55_crew_skills_20_50.json`에 있어. 같은 모양으로 돌려줘.
**`crew`, `level`, `id`, `effect`, `value`, `icon_index`는 그대로 두고 `name`, `short`만 고쳐 줘.**

```json
{ "skills": [
  { "crew": "yeoul", "level": 20, "id": "yeoul_lv20", "name": "단단한 갑판", "effect": "cargo_bonus", "value": 0.03, "short": "짐을 더 많이 실어요", "icon_index": 0 }
] }
```
- 선원 12명 × 4개 = **48개**야.
- 이름은 그 선원의 일(갑판장, 포술장, 돛지기 …)과 성격에 맞게 지어 줘.
- Lv50 스킬은 그 선원 이야기의 끝 느낌이 나게 해 줘. "멀리 갔다가 함께 돌아오는" 톤이야.

---

## 2부. 그림 (2장)

모든 그림은 배경이 순수한 마젠타 #FF00FF야.

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, no text, no letters, no numbers, no watermark

### 1. crew_portraits_30.png (Lv30 선원 얼굴)
- 512×384, 4×3칸이야 (한 칸 128×128). **crew_portraits_15.png와 칸 순서, 크기, 얼굴 위치가 똑같아야 해.**
- 같은 사람이 조금 더 노련해진 모습이야. 바다 바람에 그을린 얼굴, 작은 흉터나 새 장신구 하나, 직업 도구가 더 좋아져.

> A 4x3 portrait sheet, 512x384, each cell 128x128, solid pure magenta #FF00FF background, the same twelve crew members in the same order and framing as the supplied crew_portraits_15 sheet, now seasoned sailors: sun-tanned faces, one new keepsake or small scar each, better work tools. (+ 공통 스타일 문장)

### 2. crew_portraits_50.png (Lv50 선원 얼굴)
- 512×384, 4×3칸이야. 칸 순서와 얼굴 위치는 위와 똑같아.
- 가장 멋진 모습이야. 금빛 장식이 달린 옷, 귀항호 휘장, 자신 있는 웃음.

> A 4x3 portrait sheet, 512x384, each cell 128x128, solid pure magenta #FF00FF background, the same twelve crew members in the same order and framing as the supplied crew_portraits_15 sheet, now legendary veterans: coats with small gold trim, a ship crest badge, confident warm smiles. (+ 공통 스타일 문장)

---

## 돌려줄 것
- `data/55_crew_skills_20_50.json`
- `art/crew_portraits_30.png`, `art/crew_portraits_50.png`
