# 작은 섬 — 그림 넣는 법 (ART GUIDE)

> For the AI artist: this is an Android live-wallpaper idle game written in Kotlin. **You do not need to edit any code.**
> Draw the PNG files below and put them in `app/src/main/assets/art/` with the exact file names.
> The game loads them automatically (see `Art.kt`); any file that is missing falls back to the built-in code-drawn art.
> Then return the whole project as a zip.

## 모든 그림 공통 규칙 (rules for every file)

- PNG. Size is free (around 1000–1600 px wide is fine). The game shrinks each picture to about 14–200 game pixels wide, so **big clear shapes and strong outlines** work best.
- Style: cozy 16-bit pixel art, crisp hard-edged pixels, limited warm palette, soft daylight, **side view**, no text, no watermark.
- Every sprite (everything except `bg_sea.png`) must be on a **solid flat #FF00FF magenta background**. The game deletes magenta and makes it transparent. Do not use magenta or pink inside the art.
- Sprite sheets are grids of **equal-size cells**. Each object is centered horizontally in its cell, and its bottom (feet, hull, foundation) **touches the bottom edge of the cell**.
- Keep one consistent art style across all files.

## 파일 목록 (files)

| file | layout | what to draw |
| --- | --- | --- |
| `bg_sea.png` | one landscape picture, 3:2 | Sky with clouds on top. **Horizon line at 48% of the height.** Below: sparkling sea, then a darker deep-sea cross-section with rocks, seaweed and bubbles at the bottom. Left and right edges must tile seamlessly (it scrolls sideways). No magenta here. |
| `island.png` | one picture | A small island seen exactly from the side, filling the width: sand edges, green grass top, 3 round trees, rocks. **The waterline must be exactly at the vertical middle of the picture** (top half above water, bottom half the rocky underwater base). No buildings, no people. |
| `buildings.png` | 7 cells in 1 row | 0 small cream cottage with red roof · 1 wooden storehouse · 2 wooden pier with posts and a lantern (pier on the right side of its cell) · 3 small vegetable field · 4 white-red lighthouse · 5 market stall with striped awning · 6 broken old pier |
| `ships.png` | 4 cells in 1 row | The same wooden sailing ship in 4 upgrade stages, **facing right**, smallest to largest: 1 mast patched sail → bigger sail + 1 cannon → 2 masts + 2 cannons → 3 masts, red flag, 3 cannons. All keep a **blue cord on the bow**. Hull bottom touches the cell bottom. |
| `pirates.png` | 3 cells in 1 row | Pirate ships **facing left**, cute not scary, **hook symbol, no skulls**: 0 small black-sail ship · 1 dark-red-sail ship · 2 large grey ship with two black sails |
| `residents.png` | 6 columns × 3 rows | Three chibi villagers **facing right**. Columns 0–3 walking cycle, columns 4–5 swinging an axe. Row 0 **Dan**: short spiky brown hair, brick-red work clothes, hammer on belt. Row 1 **Bori**: round dark-brown bob, mustard apron, teal skirt. Row 2 **Haeon**: dark navy side-part hair, light-blue shirt, brown vest, small ledger. Feet on the cell bottom. |
| `worker.png` | 6 columns × 1 row | One part-time worker with a wide straw hat and green clothes, facing right. Columns 0–3 walking, 4–5 carrying a crate. |
| `port.png` | one picture | A small harbor town seen from the side: long wooden pier at the bottom edge, a crane with a crate, 4 small houses with striped awnings (orange, sky blue, cream), crates and barrels. |
| `icons.png` | 4 × 4 grid | In this order, left to right, top to bottom: wood logs, stone, gold coin, treasure map, red-roof house, storehouse, pier, field sprout, lighthouse, market stall, sailing ship, blue anchor, straw-hat worker face, puzzle piece, open book, gear. |

## 확인 (check before returning)

1. All 9 files are in `app/src/main/assets/art/` with these exact lowercase names.
2. Backgrounds of sprites are pure #FF00FF.
3. Grids have equal cells, with nothing crossing cell borders.
4. Zip the whole project folder and return it.

The first three files to make if time is short: `bg_sea.png`, `ships.png`, `residents.png`.
