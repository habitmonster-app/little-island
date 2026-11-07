# 작은 섬 — 그림 2차 주문 (ART GUIDE 2: everything still needed)

> For the AI artist: the first 9 files from `ART_GUIDE.md` are already in the game and look great. Keep **exactly the same style** (look at the PNGs in `app/src/main/assets/art/`).
> This file lists **every remaining picture** the game will need until release. Draw them all, put them in `app/src/main/assets/art/` with the exact names, and return the whole project as a zip.
> **Do not edit Kotlin code.** Claude will connect each new file to the game.
> Character and place descriptions are in `app/src/main/assets/story.json`, `05_workers.json`, `03_ports.json`, `06_pirates.json` and `12_seasons_events.json`. Read them when a row says "see data".

## 공통 규칙 (same rules as before)

- PNG, any size around 1000–1600 px wide. Big clear shapes, strong dark outlines, cozy 16-bit pixel art, crisp pixels, limited warm palette, **side view**, **no text, no letters, no numbers, no watermark** in any picture.
- Every sprite on **solid flat #FF00FF magenta**. Never use magenta/pink inside the art.
- Sheets are grids of **equal cells**. Each object centered horizontally, its bottom touching the bottom of its cell (except icons/portraits, which are centered).
- Characters face **right**. Pirate ships face **left**. Our ships face **right**.
- No skulls, no blood, nobody hurt. Pirates are cartoonish.

---

## A. 하늘·바다·날씨 (backgrounds, no magenta)

| file | what |
| --- | --- |
| `bg_sea_dusk.png` | Same layout as `bg_sea.png` (3:2, horizon at 48% height, left/right edges tile seamlessly) at sunset: orange-pink sky, purple clouds, warm reflections. |
| `bg_sea_night.png` | Same layout at night: deep navy sky, stars, a small moon, dark sea with soft moonlight sparkles. |
| `bg_storm.png` | Same layout in a storm: dark grey clouds, rain streaks, tall waves, no lightning bolts hitting anything. |
| `weather.png` | 4 cells in 1 row, on magenta: a rain-streak tile, a snowflake cluster, a small dark storm cloud, a falling autumn leaf. |

## B. 섬이 자라는 모습 (island growth, same rules as `island.png`: waterline exactly at the vertical middle, no buildings, no people)

| file | what |
| --- | --- |
| `island_2.png` | After part 1: same island a bit wider, flowers along the grass, a small path. |
| `island_3.png` | After part 3: more trees, a stone path, a little garden fence, lanterns on posts. |
| `island_4.png` | Ending: lush and lively, colorful bunting flags, flower bushes, the happiest version. |
| `trees.png` | 4 cells in 1 row: big round tree · small young tree · tree stump · rock pile. (Used for chopping animation, separate from the island picture.) |

## C. 항구 10곳 (ports, same rules as `port.png`: long pier at the bottom edge, on magenta)

`port.png` is already 해솔항. Draw one picture for each other port (see `03_ports.json` "look" and colors):

| file | port |
| --- | --- |
| `port_saltflat.png` | 소금여울: salt warehouse, sluice-gate keeper's house, low dike, salt drying pans, white sacks |
| `port_pottery.png` | 도기포: brick kilns with chimneys, pottery workshop, shelves of cups and jars |
| `port_windneck.png` | 바람목: sail repair shop, cloth drying racks with big sails, rope workshop, resting pavilion |
| `port_pineshade.png` | 솔그늘항: pine trees, carpentry workshop, stacked lumber, tool bench |
| `port_redanchor.png` | 붉은 닻: no land buildings, several ships tied together with ropes, people working on decks, red sails |
| `port_fogferry.png` | 안개나루: misty pier, letter house, fish drying racks, small homes, shared well |
| `port_glasscape.png` | 유리곶: small harbor lighthouse, lens workshop with glinting windows, observatory |
| `port_reedbay.png` | 갈대만: tall reeds, grain warehouse, flat-bottom boats, reed shelter |
| `port_paperwater.png` | 종이물: print shop, paper drying lines, archive house, map board |
| `port_whitesail.png` | 백선항: the biggest port, merchant guild hall, large warehouses, inspection yard, white sails |

## D. 배 (ships)

| file | what |
| --- | --- |
| `pirates_all.png` | 4 columns × 2 rows, facing left, in this order (see `06_pirates.json`): 갈고리단 정찰선, 갈고리단 약탈선, 붉은 돛 위장선, 갈고리단 봉쇄선 / 구록의 매복선, 갈고리단 잔당선, 암초 약탈선, 먼물 약탈선 |
| `ships_other.png` | 4 cells in 1 row, facing right: friendly red-sail convoy ship of 가람 · small supply barge with crates · little mail boat with a letter flag · fishing boat |
| `ship_deco.png` | 4 × 4 grid of collectible ship decorations (centered icons): 4 flags, 4 figureheads (bird, fish, star, lantern), 4 sail patterns, 4 deck items (bell, lantern, flower pot, telescope) |

## E. 사람 (characters)

| file | what |
| --- | --- |
| `workers_all.png` | 6 columns × 12 rows. One row per part-time worker in this order (see `05_workers.json` "look"): 여울, 문기, 소리, 누리, 바다, 건우, 도리, 하늘, 빛여, 들, 새이, 온유. Columns 0–3 walking, 4–5 carrying a crate. Same size and style as `worker.png`. |
| `resident_faces.png` | 6 columns × 3 rows of face close-ups (head and shoulders, centered). Rows: 단, 보리, 해온. Columns: neutral, smile, worried, focused, surprised, resting/sleepy. |
| `worker_portraits.png` | 4 columns × 3 rows of head-and-shoulders portraits of the 12 workers, same order as `workers_all.png`. |
| `portraits.png` | 4 columns × 4 rows of head-and-shoulders portraits (see `story.json`): 서린 (captain), 모라, 연두, 가람, 구록, 우란, 소아, 이솔, 도겸, 수빈, 태루, 새나, 무진, 갈대만 뱃사공, and 2 empty cells. |

## F. 효과 (effects)

| file | what |
| --- | --- |
| `fx.png` | 6 columns × 2 rows: cannon muzzle flash, smoke puff small, smoke puff medium, smoke puff big, water splash small, water splash big / gold coin sparkle, floating glass bottle with a note, floating treasure chest, seagull wings up, seagull wings down, jumping fish |

## G. 앱 화면 (app UI)

| file | what |
| --- | --- |
| `ui_panel.png` | One big wooden panel with a grey stone border, empty plank center. The border must be even on all sides (the game stretches the middle). |
| `ui_popup.png` | Smaller wooden popup frame, same style, empty center. |
| `ui_parts.png` | 4 columns × 2 rows, centered: empty dark item slot · same slot with a glowing gold border · same slot with a light selection border · square wooden tab button · same tab pressed and brighter · wide wooden button (fill the cell width) · same button greyed out · small red notification dot |
| `ui_plaque.png` | A long wooden plaque (about 5:1) for showing coins and resources, empty. |
| `ui_bar.png` | A long thin progress bar frame (about 10:1), empty inside. |
| `icons2.png` | 4 × 4 grid, centered: salt sack, pottery jar, sailcloth roll, lumber planks, rope coil, dried fish, glass lens, grain sack, paper bundle, tool box, letter, boarding ticket, small bell, storm cloud, festival lantern, trophy |

## H. 계절 장식 (seasons, small items placed on the island)

| file | what |
| --- | --- |
| `season_deco.png` | 4 columns × 4 rows. Row 0 spring: flower patch, cloth bunting, flower basket, pennant. Row 1 summer: seashells, shade awning, windsock, cold drink mug. Row 2 autumn: fallen leaves pile, harvest basket, warm cloth bunting, autumn lamp. Row 3 winter: snowy scarf on a post, pine garland, covered lantern, snow edge strip. |

## I. 퍼즐 (puzzles)

| file | what |
| --- | --- |
| `puzzle_map.png` | A square treasure map on parchment: three islands, a dotted route, a red X, a compass rose. No letters. (Used for the sliding puzzle.) |
| `crates.png` | 4 columns × 2 rows of square cargo crates in 8 different colors/patterns (wood, blue, green, red, yellow, purple, teal, striped). |

## J. 이야기 그림 (story illustrations, wide pictures, no magenta)

| file | what |
| --- | --- |
| `part_1.png` … `part_6.png` | One wide illustration (3:2) for each story part 1–6. Read the episode summaries of that part in `story.json` and draw its key moment with the three residents. |
| `ending.png` | A wide illustration (3:2) of the ending: the island full of lights, ships returning home. |
| `events.png` | 4 columns × 3 rows of small square scenes (see `12_seasons_events.json`): treasure island survey, treasure gold pile, storm preparation, storm at sea, lighthouse first light, return market first day, traveling market, spring letter bundle, summer shade day, autumn repair day, winter warm light, and one empty cell. |

## K. 출시용 (store and app icon)

| file | what |
| --- | --- |
| `app_icon.png` | 1024 × 1024 square app icon: small round island, cream sailboat with a blue cord, teal sea, warm lantern. No text. |
| `app_icon_fg.png` | The same island and boat only, centered inside the middle 60% of a 1024 × 1024 #FF00FF canvas (for Android adaptive icons). |
| `feature_graphic.png` | 1024 × 500 wide store banner: residents waiting on the pier as the ship returns at golden hour. Keep the center clear of important details at the very edges. No text. |
| `splash.png` | Tall phone picture (9:19.5) for the loading screen: island at sunrise seen from the sea, lots of sky. No text. |

---

## 확인 (check before returning)

1. Every file above is in `app/src/main/assets/art/` with the exact lowercase name.
2. Sprite backgrounds are pure #FF00FF; backgrounds (A bg_*, J, K except app_icon_fg) have no magenta.
3. Grid sheets have equal cells; nothing crosses cell borders.
4. No text anywhere.
5. Zip the whole project and return it. If you cannot finish everything at once, finish in this priority: **E workers_all, C ports, D pirates_all, A backgrounds, G UI, F fx**, then the rest.
