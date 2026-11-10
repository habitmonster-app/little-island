# 리틀 아일랜드: 아스트라에게 스물한 번째 부탁 (개조된 배, 통째로 다시 그리기)

아스트라, 20번 장식 그림 고마워! 게임에 넣어 봤는데, 장식을 배 위에 따로 얹으니 배와 그림체가 따로 놀아서 어색했어.
그래서 이번에는 **장식이 붙은 배를 통째로** 그려 줬으면 해.

배를 "개조"하면 (배 단계마다 2번) 배 그림이 이 그림으로 바뀌어.
- **개조 1**: 원래 배 + 새 돛 무늬 + 새 깃발
- **개조 2**: 개조 1 + 뱃머리 장식 + 갑판 물건 + 금빛 테두리 장식 조금

참고 그림 (저장소 `docs/astra/order21/`에 있어. **배마다 그 배 그림을 꼭 같이 올려 줘**)
- `ref_ship_2.png` … `ref_ship_10.png`: 지금 배 2~10단계 그림 (512×512, 마젠타 배경). 이 배를 **그대로 두고** 장식만 자연스럽게 더해 줘.
- 장식 모양은 20번에 그려 준 `deco_flags`, `deco_sails`, `deco_figureheads`, `deco_deck`을 따라 줘.

공통 규칙
- 그림 한 장 = 한 배 단계. **1024×512**, 2열 × 1행 (한 칸 512×512). **왼쪽 = 개조 1, 오른쪽 = 개조 2**.
- 배의 **크기, 위치, 모양, 색, 돛대 수는 참고 그림과 똑같이**. 배 바닥이 칸 아래쪽의 같은 자리에 오게. 배가 더 커지거나 옮겨지면 안 돼 (게임에서 바꿔 끼워).
- 배는 **오른쪽을 보고** 있어 (뱃머리가 오른쪽).
- 장식은 배의 일부처럼: 돛 무늬는 돛에 칠한 것처럼, 깃발은 돛대 꼭대기에 달린 것처럼, 뱃머리 장식은 뱃머리 끝에 조각한 것처럼.
- 배경은 **마젠타 #FF00FF** 단색. 바다, 물결, 그림자는 그리지 마.
- **글자, 숫자, 로고는 넣지 마.**
- 한 번에 한 장씩. 마지막에 zip 하나로 돌려줘 (`art/`). 코드는 고치지 마.

공통 스타일 문장 (모든 그림 끝에 붙여 줘):
> Keep the ship from the reference image exactly the same size, position, shape and colors, only add the decorations naturally as part of the ship. same style as the reference ship, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, facing right, solid flat #FF00FF magenta background, no sea, no shadow, no text, no letters, no numbers, no watermark

공통 프롬프트 틀 (배마다 [ ] 부분만 바꿔서):
> Using the attached ship as the base, draw a 1024x512 image with 2 cells of 512x512 side by side. Left cell: the same ship with [개조 1 영어]. Right cell: the same ship with [개조 1 영어], plus [개조 2 영어], and a little gold trim on the hull. (+ 공통 스타일 문장)

---

| 파일 | 참고 그림 | 개조 1 (왼쪽) | 개조 2 (오른쪽, 개조 1에 더해서) |
|---|---|---|---|
| `ship_refit_2.png` | ref_ship_2 | 큰 돛에 파란 가로 띠, 파란 깃발 | 웃는 갈매기 뱃머리, 갑판에 작은 종 |
| `ship_refit_3.png` | ref_ship_3 | 큰 돛에 붉은 해, 노란 바탕 파란 물결 깃발 | 주홍 물고기 뱃머리, 갑판 등잔 |
| `ship_refit_4.png` | ref_ship_4 | 큰 돛에 초록 잎새, 초록 마름모 깃발 | 금빛 보물별 뱃머리, 꽃 화분 |
| `ship_refit_5.png` | ref_ship_5 | 큰 돛에 금빛 물결, 남색 은빛 갈고리 깃발 | 청록 문어 뱃머리, 망원경 |
| `ship_refit_6.png` | ref_ship_6 | 큰 돛에 금화, 하얀 천 붉은 게 집게 깃발 | 번개 구름 뱃머리, 초승달 등불 |
| `ship_refit_7.png` | ref_ship_7 | 큰 돛에 나침반 별, 색색 조각 깃발 | 조개 꽃다발 뱃머리, 작은 지구본 |
| `ship_refit_8.png` | ref_ship_8 | 큰 돛에 금빛 별자리, 남색 금빛 닻 깃발 | 금빛 돌고래 뱃머리, 금빛 시계 |
| `ship_refit_9.png` | ref_ship_9 | 큰 돛에 저녁 등불, 산호빛 해마 깃발 | 날치 뱃머리, 황동 천문환 |
| `ship_refit_10.png` | ref_ship_10 | 큰 돛에 물결 위 금빛 해, 분홍·금빛 아침놀 깃발 | 진주빛 돌고래 뱃머리, 지도 상자 |

영어 (프롬프트 [ ]에 넣을 말)
- 2: [a blue horizontal stripe painted on the main sail and a plain blue flag on the top of the main mast] / [a smiling seagull figurehead on the bow and a small bell on the deck]
- 3: [a red sun painted on the main sail and a yellow flag with a blue wave] / [an orange fish figurehead and a glowing deck lantern]
- 4: [a green leaf painted on the main sail and a green flag with a diamond] / [a golden treasure star figurehead and a flower pot on the deck]
- 5: [golden waves painted on the main sail and a navy flag with a silver hook] / [a teal octopus figurehead and a spyglass on a stand on the deck]
- 6: [a gold coin painted on the main sail and a white flag with a red crab claw] / [a cloud with a small lightning bolt figurehead and a navy lantern with a crescent moon]
- 7: [an eight point compass star painted on the main sail and a patchwork flag] / [a shell bouquet figurehead with a teal ribbon and a small globe on the deck]
- 8: [a gold constellation painted on the main sail and a navy flag with a gold anchor and tassels] / [a leaping golden dolphin figurehead and a golden captain clock]
- 9: [an evening lantern painted on the main sail and a coral flag with a seahorse] / [a flying fish figurehead and a brass astrolabe]
- 10: [a golden sun over ripples painted on the main sail and a pink and gold dawn silk flag] / [a pearl white dolphin figurehead and a box of sea charts]

---

## 돌려줄 것
- `art/ship_refit_2.png` … `art/ship_refit_10.png` (9장)
