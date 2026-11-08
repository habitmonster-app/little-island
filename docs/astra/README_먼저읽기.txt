작은 섬: 돌아오는 바다 — 4차 전달 자료

구성
art/: PNG 9장. 마젠타(#FF00FF)를 배경 키 색으로 사용합니다.
data/: JSON 4개. 기존 프로그램 코드는 수정하지 않았습니다.

먼저 확인할 사항
- 19_ship_parts.json, 20_crew.json: 4차 주문에 맞춰 만든 새 설계 데이터입니다. 기존 02_ships.json의 큰 성장 4단계와 비용은 유지합니다. 실제 게임 연결과 기기 테스트는 아직 수행하지 않았습니다.
- 15_legal.json, 16_sounds.json: to-astra-round2.md 원문을 확보하지 못해 기존 프로젝트와 4차 주문을 바탕으로 작성한 새 초안입니다. 해당 원문과 스키마를 대조해야 합니다.
- 15_legal.json은 바로 게시할 문서가 아닙니다. 운영자·문의처·시행일·공개 URL, 실제 출시본의 데이터 처리 내용을 채우고 확인해야 합니다.
- 16_sounds.json은 제작할 소리 목록과 재생 규칙입니다. 실제 음원 파일은 포함되지 않았습니다.
- 그림 파일명을 그대로 사용하고, 로더에서 순수 마젠타를 투명 처리하세요. app_icon_fg.png도 적용할 때 마젠타 배경을 제거해야 합니다.

시트 규격 (열 × 행, 인덱스는 0부터)
ship_stages.png: 1024×1024 / 4×4 / 셀256×256. 행=배 큰 단계1~4, 열=외형단계0~3. 선체 하단 기준 y247. 오른쪽 방향. 4단계 그림의 세 돛대는 4차 주문의 외형이며 기존 02_ships.json 수치는 바꾸지 않았습니다.
crew_deck_a.png: 1024×1536 / 4×6 / 셀256×256. 행=여울, 문기, 소리, 누리, 바다, 건우.
crew_deck_b.png: 1024×1536 / 4×6 / 셀256×256. 행=도리, 하늘, 빛여, 들, 새이, 온유.
선원 두 시트의 열=로프 당기기, 키 돌리기, 대포 장전, 한 팔을 들어 환호. 이는 네 가지 작업 포즈이며 연속 걷기 애니메이션 4프레임이 아닙니다. 인물의 왼쪽 발 영역 기준 하단 y247에 맞췄고, 소품 일부는 그 아래까지 내려옵니다.
part_icons.png: 1024×512 / 4×2 / 셀256×256.
  선체, 돛, 대포, 키 / 선원모자, 초록 위 화살표, 별 스킬책, 금빛 별 배지.
skill_icons.png: 1024×1024 / 4×4 / 셀256×256.
  매듭, 바람, 포탄, 코인 / 네잎클로버, 망치와 판자, 보물상자, 통나무 /
  돌, 나침반, 망원경, 냄비 / 붕대, 등불, 지도, 악수.
worker_portraits.png: 1024×768 / 4×3 / 셀256×256. 05_workers.json의 12명 순서, 왼쪽→오른쪽→다음 행.
portraits.png: 1024×1024 / 4×4 / 셀256×256.
  서린, 모라, 연두, 가람 / 구록, 우란, 소아, 이솔 /
  도겸, 수빈, 태루, 새나 / 무진, 갈대만 사공, 빈칸, 빈칸.
events.png: 1448×1086 / 4×3 / 셀362×362. 칸 사이 얇은 마젠타 간격 포함.
  보물섬 탐색, 보물 금화, 폭풍 대비, 폭풍 항해 /
  등대 점등, 첫 시장, 떠돌이 상인, 봄 편지 /
  여름 차양, 가을 배 수리, 겨울 불빛, 빈칸.
app_icon_fg.png: 1024×1024. 전경이 중앙60% 영역 안에 있음. 배경 키 제거 뒤 Android 아이콘 전경으로 사용.

제작 및 검수
새 그림 5장(배/선원A/선원B/부품아이콘/스킬아이콘)과 사건 그림 수정1장, 기존 초상화2장 및 아이콘 전경1장을 활용했습니다. 생성형 이미지 도구로 각 이미지를 만들었으며, 마지막에 정수 격자·발선/선체선·키 색을 맞추는 기술적 출력 처리를 했습니다. PNG 디코딩과 빈칸, 격자, JSON 구조 및 주요 데이터 참조를 확인했습니다.

아래는 신규·수정 이미지의 최종 생성 프롬프트입니다.

--- crew_deck_a.png ---
Use case: identity-preserve.
Asset: crew_deck_a.png for a cozy 16-bit pixel art idle island game.
Use the supplied image ONLY as the character identity, clothing and pixel-art style reference. Create a NEW sprite sheet with exactly FOUR columns and SIX rows (24 complete sprites), portrait aspect ratio 2:3. All cells equal square size with generous pure flat #FF00FF magenta gaps, no grid lines. DO NOT copy the reference's six-column layout.
Each ROW depicts one of the exact six characters in the supplied reference, preserving order and outfits:
row 1: brown-haired worker in wide tan straw hat with brown band, dark green work clothes, brown belt and boots.
row 2: dark-brown very curly-haired worker, orange/brown vest over cream rolled sleeves, dark trousers, pouches and tools at belt.
row 3: auburn-haired worker wearing a tied blue headscarf, cream shirt and long beige apron, brown belt and boots.
row 4: auburn bob-haired worker in dark teal work clothes, cream cuffs, brown tool belt and boots.
row 5: navy-haired worker in cream tied headscarf, dark navy vest with little gold buttons over cream sleeves, brown trousers, belt pouches and boots.
row 6: spiky black-haired worker with olive headband, mustard/orange work apron over cream sleeves, dark trousers, brown tool belt and boots.
The FOUR COLUMNS repeat these same actions for EVERY row:
Column 1: facing RIGHT, leaning slightly back and PULLING A ROPE with both hands, short rope extends to right, no crate.
Column 2: facing RIGHT, TURNING A WOODEN SHIP'S WHEEL immediately in front of them, both hands on wheel.
Column 3: facing RIGHT, LOADING A SMALL CARTOON CANNON with a round black cannonball; cannon muzzle points right. No firing, no smoke, no combat.
Column 4: facing RIGHT, CHEERING WITH ONE ARM RAISED HIGH and the other bent, happy expression. No tools or objects in this column.
Side view chibi proportions EXACTLY like reference, same face shape, hair, headwear, garments and colors. All characters equally sized across every row and column, same foot baseline within each row. Props fit wholly inside each cell and cannot overlap neighbours. Show complete characters and complete props without cropping. All rows equally tall and columns equally wide; all poses centered. Crisp hard-edged pixel blocks, strong dark outline, warm limited palette, minimal shading. Entire background pure solid #FF00FF, no ground plane, no scene, no shadows. No text, labels, letters, numbers, border, watermark. Only the specified 24 sprites.

--- crew_deck_b.png ---
Use case: identity-preserve. Asset type: production pixel-art crew sprite sheet for the game Little Island.
Input image is the authoritative character identity/style reference workers_all_b.png. Create a NEW sheet containing the SAME six people in the SAME top-to-bottom row order, with new shipboard actions. Do not copy the six-column walking layout: the output must have exactly FOUR COLUMNS and SIX ROWS (24 cells total), equal square cells, portrait 2:3 canvas ideally 1024x1536. No labels or drawn grid.
Preserve each reference person's hairstyle, headwear, skin, outfit colors, gear, proportions, eye style, thick dark pixel outlines and crisp visible square pixels. Cozy 16-bit pixel-art, limited warm palette. All characters face RIGHT in every cell, including the cheer. Same body scale within all cells. Align feet to a shared baseline within each row. Include entire person and each tool inside their own cell, clear magenta separation, no overlap or cropping.
Rows, top to bottom:
1) Dori: tied rusty red headscarf, long dark braid behind, light gray rolled-sleeve shirt, dark gray vest and trousers, rope coil on brown belt, brown boots.
2) Haneul: tousled muted purple hair, cream rolled-sleeve shirt with dark vest, teal shoulder satchel with brass buckle, dark trousers and brown boots.
3) Bityeo: golden-orange hair tied in low short ponytail, cream work overalls and cream short sleeve shirt, brown belt, blue work gloves, brown boots.
4) Deul: tan woven rounded straw work hat, chestnut short hair, cream rolled-sleeve shirt, moss green vest, brown trousers, beige sack backpack, brown boots.
5) Saei: short straight black bob, cream rolled-sleeve shirt, dark teal apron with front pocket and ties, brown belt and boots, dark trousers, dark blue fingerless gloves.
6) Onyu: short tousled warm gray-brown hair, dark navy vest and navy trousers, cream rolled-sleeve shirt, brown belt with brass buckle and a cream cloth hanging from it, brown boots.
Exactly the same four actions in each row, left to right:
Column 1: leaning back slightly while pulling a thick tan ship rope with BOTH hands, rope extends a short distance to the right and stays within cell. Keep headwear/gear.
Column 2: standing immediately LEFT of a small wooden ship steering wheel mounted on a short post, BOTH hands turning the wheel, body and face directed right.
Column 3: standing immediately LEFT of a small compact cannon pointing RIGHT on a low wooden carriage, one hand loading a single round cannonball, clear complete cannon, friendly nonviolent work pose.
Column 4: cheering with ONE arm raised, other arm down, joyful expression, body and face still directed RIGHT, no tool.
Background is uniform pure flat #FF00FF magenta everywhere outside the sprite figures and props. Never use pink or magenta within characters or props. No scene, no ship deck, no floor, no grounding shadow. No text, no letters, no numbers, no watermark. Exactly 24 action sprites, 6 rows by 4 columns.

--- part_icons.png ---
Use case: stylized-concept. Create one production game sprite icon sheet named part_icons.png for the cozy Korean island idle game. The attached icons2 sheet is the STYLE REFERENCE only: match its warm 16-bit pixel-art technique, chunky dark almost-black outlines, crisply stair-stepped hard pixels, limited golden brown/cream/teal palette, simple readable shaded objects. Do not copy its subject set.

A wide sheet of EXACTLY eight icons, exactly FOUR columns and TWO rows, equal SQUARE cells, 2:1 overall aspect ratio. Each object centered in its cell with an even margin, no object crosses its cell or is cut off. No grid lines.
Row 1 left to right: a wooden ship hull with visible planks and no mast; a single cream sailing sail with short wooden mast; a small friendly gray cannon on a wooden carriage; a wooden ship steering wheel.
Row 2 left to right: a recognizable cream/navy sailor's cap for a crew slot; a bright green upward arrow; a closed skill book with ONE gold star on the cover; a gold star badge.
Side-view game-item icons, consistent visual weight and size. Background is completely flat pure #FF00FF magenta everywhere outside the objects. No magenta or pink within any object, no shadows extending onto the background, no text, no letters, no numbers, no watermark. Every icon appears exactly once.

--- skill_icons.png ---
Use case: stylized-concept. Create one production game sprite icon sheet named skill_icons.png for the cozy Korean island idle game. The attached icons2 sheet is the STYLE REFERENCE only: match its warm 16-bit pixel art, chunky dark almost-black outlines, clearly stair-stepped hard-edged pixels, limited golden brown/cream/teal palette and readable shaded item designs. Do not copy the reference subject set.

A square sheet containing EXACTLY SIXTEEN distinct skill icons in exactly FOUR columns and FOUR rows, equal square cells. Each icon is centered, with equal margin and no overlap, no grid lines. Strict reading order:
Row 1: one clearly tied rope knot; a teal-white wind swirl; one dark gray round cannonball; a small stack of gold coins.
Row 2: one green FOUR-leaf clover; a wooden hammer and plank together as one icon; a closed wooden treasure chest with gold fittings; one brown cut log.
Row 3: one gray stone; a round nautical compass WITHOUT letters or words, just geometric pointer; one brass collapsible spyglass; one cooking pot.
Row 4: one rolled cream bandage; one warm glowing nautical lantern; one folded parchment map WITHOUT text or letters; two friendly hands shaking as one handshake icon.
Exactly one complete subject per cell, centered and similarly sized. Side-view cozy game-item icons. Pure flat #FF00FF magenta background throughout all gaps. No pink or magenta inside any icon. No background shadow, text, letters, numbers, labels or watermark.

--- events.png ---
Use case: precise-object-edit. Asset: cozy 16-bit pixel art game event illustration atlas. The supplied image is the EDIT TARGET. Preserve all eleven existing illustrated scenes, their composition, their exact row-major order, their characters, colors, crisp pixel art and dark outlines. Change ONLY the separators/background layout: the atlas is four columns by three rows of equal square cells, with thin pure flat #FF00FF magenta gaps between every cell and a thin magenta outer margin. The bottom-right twelfth cell must be completely solid flat #FF00FF magenta, with no illustration and no cream color. All eleven other cells remain full-color square scene illustrations. Reading order remains: tiny treasure island survey; gold treasure; villagers tying down crates before storm; ship in storm; lighthouse first light; harbor market; traveling merchant boat; spring letters with flowers; summer shade awning; autumn boat repair; winter warm windows; empty magenta cell. No labels, text, letters, numbers, watermark. Keep 4:3 overall canvas with 4x3 square grid. Do not alter scene content or add characters.

--- ship_stages.png ---
Use case: identity-preserve. Asset type: production sprite atlas for the cozy pixel game Little Island.
Input image is the EXACT visual reference of our four ships, not a layout reference. Transform it into ONE NEW square sprite atlas containing exactly 16 ships in a 4 ROW by 4 COLUMN equal-cell grid, no visible grid lines.
Every ship faces RIGHT, with the distinctive bright BLUE rope tied around the bow on the RIGHT. Use exactly the reference's chunky crisp 16-bit pixel art, strong dark outlines, warm brown wooden hulls, cream sails, green cabin roofs, and limited palette. Hard edged pixels, not smooth painting.
Each ROW uses ONE of the four reference ships, in left-to-right reference order, with identical silhouette, hull width, mast count, scale, position and baseline across all four COLUMNS of that row:
ROW 1: smallest open one-mast / one-sail boat, no cabin.
ROW 2: larger one-mast / one-sail boat with green-roof rear cabin, one hull porthole.
ROW 3: two-masted boat with green-roof rear cabin and two hull portholes.
ROW 4: largest three-masted boat with green-roof rear cabin and two hull portholes.
The same hull stays exactly the same size across each row. Preserve the incremental ship sizes across rows.
Four COLUMNS show additive upgrades from left to right:
COLUMN 1: original plain reference ship, plain worn cream sails.
COLUMN 2: SAME ship plus reinforced hull planks and modest iron corner brackets; sails still plain worn cream.
COLUMN 3: SAME reinforced hull plus clean cream sails with a clearly visible TEAL edge trim; same mast count.
COLUMN 4: SAME reinforced hull and clean teal-trimmed sails, plus one polished small cannon visible on deck and one small mast-top flag.
The existing top flag of reference ship four may remain in columns 1–3, with a visibly improved small flag in column 4. Do not add unrelated accessories or characters.
Arrange each ship entirely inside its own equal square cell with substantial pure magenta spacing, all ship bottoms aligned to the same relative baseline within every cell. Center horizontally. No boats touching cell edges or neighboring boats; no cropped masts, ropes, cannons or flags.
Background must be a perfectly solid flat #FF00FF magenta EVERYWHERE outside the ships, with no water, ground, shadows, gradient or texture. No magenta anywhere inside the ship art.
No text, labels, letters, numbers, watermarks, UI borders, or grid lines. Exactly FOUR rows and FOUR columns.
