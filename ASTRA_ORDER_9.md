# 리틀 아일랜드: 아스트라에게 아홉 번째 부탁

아스트라, 8차 자료는 build-33에 모두 넣었어.
- 짧은 설명 203개: 설명 창에 그대로 나와.
- 선원 Lv15: Lv12·Lv15 새 스킬 24개가 열려.
- Lv11 얼굴, 훈련 상자, 올리기 반짝임도 들어갔어.

이번 부탁은 두 가지야.
1. **매일 할 거리**: 들어올 때마다 "오늘 할 일" 3개가 있으면 좋겠어.
2. **Lv12 이상 선원**: 다 자란 선원에게 줄 세 번째 특별 항해와, 코트를 입은 갑판 그림이 필요해.

글은 **짧게** 부탁해. 앱 설명 창은 3~6줄만 보여 줘.
마지막에 zip 하나로 돌려줘 (`data/`, `art/`). 코드는 고치지 마.

---

## 1부. 데이터

### 31_daily_orders.json (오늘의 항구 주문)
- 하루에 주문 3개를 무작위로 골라서 보여 줘. 다 하면 보상을 받아.
- 7일 동안 주문을 15개 끝내면 "주간 상자"를 하나 더 받아.
- 주문은 **30개** 써 줘. 쉬운 것 15개, 보통 10개, 조금 어려운 것 5개.

```json
{
  "daily_count": 3,
  "weekly_goal": 15,
  "weekly_reward": { "tickets": 10, "gold": 500 },
  "orders": [ {
    "id": "order_voyage_2",
    "counter": "voyage_return",
    "target": 2,
    "difficulty": "easy | normal | hard",
    "title": "주문 이름 (10자 안쪽)",
    "short": "한 줄 설명 (16자 안쪽, 숫자 없이)",
    "reward": { "tickets": 1, "gold": 0 },
    "min_ship_level": 1
  } ]
}
```

**counter에 쓸 수 있는 것** (이것만 써 줘)
| counter | 뜻 |
|---|---|
| voyage_return | 항해를 마치고 돌아오기 |
| pirate_win | 해적 이기기 (우두머리 포함) |
| boss_win | 우두머리 이기기 |
| part_upgrade | 부품 레벨 올리기 |
| souvenir_get | 기념품 얻기 |
| gacha_draw | 부적 뽑기 |
| training_clear | 훈련 퍼즐 성공 |
| mission_clear | 특별 항해 성공 |
| floater_pick | 바다에 뜬 병·상자 줍기 |
| cargo_pack | 짐 정리 퍼즐 성공 |

**규칙**
- 보상은 쪽지 1~3장, 금화는 조금만. 하루 3개를 다 해도 쪽지 **6장 안쪽**이 되게.
- 하루에 끝낼 수 있는 양으로. 항해 한 번은 약 10분이야. boss_win은 운이 필요하니 **hard에만 1개**, target 1로.
- 우두머리는 배 2단계부터 나와. 그런 주문은 min_ship_level을 2로 해 줘.
- title과 short에는 숫자를 넣지 마. 앱이 "0/2"처럼 붙여.

### 32_crew_missions_3.json (세 번째 특별 항해)
23번 자료와 **똑같은 모양**으로 선원마다 임무 하나씩, 모두 **12개** 써 줘.
- id는 `mission_<선원id>_3`. 예: `mission_yeoul_3`
- 조건: 선원 Lv12, 배 4단계 이상, 그리고 그 선원의 임무 2를 먼저 끝냈을 것.
- 가는 곳은 먼 항구로 (안개나루, 유리곶, 갈대만, 종이물, 백선항 중에서).
- 보상은 항해 2~3번 몫. 꾸미기 보상(cosmetic_id)은 넣지 마.
- story는 그 선원 이야기의 **마지막 편**이야. 다 자란 선원이 처음 섬에 왔을 때를 돌아보는 이야기로 써 줘 (300~500자).
- summary는 한두 줄, home_line은 18자 안쪽, result_card는 24자 안쪽.

---

## 2부. 그림

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark

### 1. crew_deck_15_a.png, crew_deck_15_b.png (코트 입은 갑판 선원)
Lv11 이상 선원이 배 위에서 일할 때 쓸 그림이야. 지금 `crew_deck_a.png`, `crew_deck_b.png`와 **칸 배치가 똑같아야** 해.
- 각 1024×1536, 4열 × 6행 (한 칸 256×256), 배경은 마젠타 #FF00FF.
- a는 여울·문기·소리·누리·바다·건우, b는 도리·하늘·빛여·들·새이·온유 순서야.
- 열: 밧줄 당기기, 키 돌리기, 대포 장전, 한 팔 들고 환호.
- 얼굴, 머리, 모자는 지금 갑판 그림과 같게. 옷만 `crew_portraits_15.png`처럼 남색 코트, 금빛 단추, 작은 견장으로.
- 발 위치는 지금 그림과 같은 높이로 맞춰 줘. 배 위에 겹쳐 그리기 때문이야.

> A character sprite sheet, 6 rows and 4 columns of equal cells, 1024x1536, chibi sailors facing right, exactly the same characters, order, poses and foot positions as the supplied crew_deck_a sheet, now wearing navy sailor coats with small gold buttons and gold shoulder marks like the supplied crew_portraits_15 sheet. Columns: pulling a rope, turning a ship's wheel, loading a small cannon, cheering with one arm up. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)
>
> (b는 같은 문장에서 crew_deck_a를 crew_deck_b로 바꿔 줘)

### 2. daily_icons.png (오늘의 주문 아이콘)
- 1024×768, 4열 × 3행 (한 칸 256×256), 배경은 마젠타 #FF00FF. 아이콘은 칸 가운데에.
- 순서:
  1. 주문 게시판 (나무판에 종이 세 장)
  2. 주간 상자 (리본 묶인 작은 보물 상자)
  3. voyage_return: 항구로 돌아오는 작은 배
  4. pirate_win: 엇갈린 두 칼
  5. boss_win: 금관 쓴 해적 해골 깃발
  6. part_upgrade: 망치와 초록 위 화살표
  7. souvenir_get: 작은 선물 꾸러미
  8. gacha_draw: 반짝이는 부적
  9. training_clear: 상자 두 개와 엇갈린 화살표
  10. mission_clear: 별이 그려진 두루마리 지도
  11. floater_pick: 물에 뜬 유리병
  12. cargo_pack: 꽉 채운 나무 상자 더미

> A grid of twelve game icons, 4 columns and 3 rows, 1024x768, each cell 256x256, each icon centered: a wooden notice board with three paper notes, a small treasure chest tied with a ribbon, a small ship returning to a harbor, two crossed sabers, a pirate skull flag with a gold crown, a hammer with a green up arrow, a small gift bundle, a sparkling charm amulet, two crates with swap arrows, a scroll map with a star, a glass bottle floating on water, a neat stack of wooden crates. Solid flat #FF00FF magenta background. (+ 공통 스타일 문장)

### 3. crew_story_art_3.png (마지막 이야기 그림 12장)
32번 이야기를 일지에서 열면 맨 위에 보일 그림이야. `crew_story_art.png`와 같은 모양이야.
- 1536×1536, 4×3칸 (한 칸 384×512, 세로로 긴 그림), 얇은 어두운 테두리. 칸 순서는 선원 id 순서와 같게.
- 각 칸: 코트를 입은 그 선원이 큰 배(5단계) 갑판에서 노을 진 섬을 돌아보는 장면. 선원마다 손에 자기 물건을 하나 들게 해 줘 (밧줄, 대포 솔, 장부, 짐표, 돛 천, 망치, 키 손잡이, 편지, 망원경, 국자, 금화 주머니, 구명 튜브).
- 얼굴과 옷은 `crew_portraits_15.png`와 똑같이.

> A 4x3 grid of twelve vertical pixel art vignettes, 1536x1536, each cell 384x512 with a thin dark frame. Each cell shows one grown-up young sailor from our crew in a navy coat with gold buttons, standing on the deck of a big three-masted ship and looking back at a small island at sunset, holding their own item: (1) a coiled rope, (2) a cannon brush, (3) a ledger book, (4) a cargo tag, (5) a folded sail cloth, (6) a hammer, (7) a ship wheel handle, (8) a letter, (9) a spyglass, (10) a ladle, (11) a coin pouch, (12) a life ring. Characters match the supplied crew_portraits_15 sheet exactly. (+ 공통 스타일 문장)

---

## 돌려줄 zip
```
little-island-order9.zip
  data/31_daily_orders.json, 32_crew_missions_3.json
  art/crew_deck_15_a.png, crew_deck_15_b.png, daily_icons.png, crew_story_art_3.png
```

## 아직 하지 말 것
- **18 수치 다시 맞추기**: 며칠 써 본 기록을 Claude가 정리해서 줄 거야.
- **15 출시 서류**: 운영자 이름, 문의 이메일, 시행일이 정해지면 할게.
- **소리**: 지금은 소리 파일을 쓰지 않아.
- **영어판**: Claude가 앱 글자 구조를 먼저 바꾼 다음에 할게.
