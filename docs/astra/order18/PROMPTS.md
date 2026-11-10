# 리틀 아일랜드 18차 — 이미지 제작 프롬프트

이미지는 한 장씩 순차로 생성했습니다. 랜드마크에 한해 등불 기둥 높이와 갈고리 문양을 수정했습니다. 원본 내보내기의 단색 키·규격 정규화 과정은 ART18_MANIFEST.json에 기록했습니다.

## 새 선원 카드

```text
Use case: stylized-concept.
Asset type: finished collectible crew card FRAME for the cozy pixel-art game Little Island.
Create a single portrait image with aspect ratio 4:5, intended export 512x640. This is ONE EMPTY FRAME, not a character portrait or a complete card with text.

A tall dark polished warm-brown wooden card with a rich gold border, strong dark outlines, and EXACTLY FOUR small gold anchor ornaments, one on each outer corner. An empty ivory-gold ribbon banner sits at the TOP CENTER. The ribbon is completely blank, and all its folds remain above the portrait opening. The card is centered with a narrow visible flat magenta margin around it. Flat front-facing card, no perspective tilt.

Most important: two LARGE EMPTY RECTANGULAR FIELDS inside the frame. Upper half: one broad rectangular portrait window, filled with ONE SINGLE FLAT LIGHT SKY-BLUE color, approximately RGB #9CDFF2. Nothing at all inside: no person, face, silhouette, scenery, clouds, pattern, sparkle or inner shadow. Lower half: one broad rectangular flat DARK BROWN text panel, approximately RGB #43291E. Nothing at all inside: no words, letters, numbers, lines, placeholders, grain, patterns or ornaments. A thin gold/wood divider separates upper and lower fields.

On the intended 512x640 canvas, card roughly spans x24..488,y16..624. Empty portrait field roughly x64..448,y116..344; lower blank field roughly x64..448,y368..568. This coordinate guidance is invisible: no rulers or labels. Ornamentation stays in the BORDER only. Four tiny anchor ornaments must not obstruct either empty field. Keep the entire frame and ribbon visible, no cropping.

Art is cheerful cozy 16-bit pixel art with crisp hard stair-step pixel clusters, thick dark-brown outlines, warm honey/gold highlights and a small controlled palette. Match the game's warm wooden ships, ivory sails and playful illustrated objects. Gold edge looks dimensional by simple pixel shading, while both inner fields stay strictly solid single colors. Not photorealistic, not vector-smooth, no blur or fuzzy glow.

Every pixel outside the card is a uniform pure MAGENTA #FF00FF color key. No gradient, texture, floor, ground, drop shadow, halo or cast shadow on that background. No extra objects. No text, numbers, logos or watermarks anywhere.
same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark
```

## 마을 랜드마크 최초 생성

```text
Use case: stylized-concept.
Asset type: game environment sprite sheet, six new TALL village landmarks for Little Island.
Reference image: existing buildings.png is ONLY a visual style, scale and bottom alignment reference. Replace all seven old objects with the SIX new landmarks below. Do not reproduce the old houses, crops, dock or lighthouse.

Create one wide sprite sheet, exact intended canvas 1536x512, aspect 3:1. SIX equally sized invisible columns, ONE row. Each cell is 256 wide and 512 tall. Cell centers x128,384,640,896,1152,1408. All structures have their supporting feet/base ON THE SAME BOTTOM EDGE y511, fully visible, not chopped off. ZERO ground margin below their feet. Use bottom alignment like the reference. Leave 12+ px magenta at left/right of every cell so objects never touch or overlap adjacent cells. No grid, borders, labels or guides.
The main structures must be TALL, 70 to90 percent of the cell height, roughly height390..450px. Tops around y60..112; bases y511. The regular reference house is only225px tall: these new landmarks are significantly taller than that house. Straight-on SIDE VIEW with a little depth only as in reference, no overhead view.

Left to right exactly:
1. ONE tall warm wooden flagpole, tall waving teal-and-ivory striped flag, bright gold finial at top. One simple stout pole foot at bottom, no ground. Flag around upper third and clearly teal/ivory horizontal stripes, no emblem.
2. ONE tall round WHITE STONE WINDMILL tower, warm RED conical roof. EXACTLY FOUR long wooden windmill blades form a clear diagonal X centered high on tower. Keep full tips inside its cell. Cream stone masonry, tiny brown door at bottom; no base ground patch.
3. TWO equally tall wooden POSTS with a visibly sagging rope between their tops. EXACTLY FIVE glowing paper lanterns hung from the rope, alternating red/yellow/red/yellow/red. Each is a separate readable round paper lantern. Warm filled lantern colors, crisp edges, no fuzzy glow or surrounding aura. Both post feet at the bottom. No floor beam or ground.
4. ONE tall GREY STONE BELL TOWER with a pointed BLUE roof, an OPEN ARCH showing ONE GOLD BELL in its upper middle. Grey masonry, sturdy proportions, tiny door below, bottom tower stones at bottom edge.
5. A trophy group of EXACTLY THREE black pirate flags on THREE wooden poles of clearly DIFFERENT heights, like captured trophies, each pole reaching bottom. Cute large ivory emblems: LEFT flag has a SHELL, MIDDLE flag a HOOK, RIGHT flag a CRAB. These are cartoon pictograms, not brand logos. NO skulls, bones, crossbones, letters, numbers or shields. Black flag cloth with a few gentle folds, gold pole finials.
6. ONE GOLDEN CAPTAIN STATUE standing on a small rectangular GREY STONE PEDESTAL. Captain wears a wide seafaring hat and holds a spyglass to one eye, looking right. Golden metal silhouette and warm gold highlights; exactly a few small isolated gold sparkles near shoulders/head. Pedestal base rests at bottom edge. No surrounding floor.

Flat pure MAGENTA #FF00FF throughout every empty pixel, including all gaps and arch/rope openings where there is no object. No shaded background, cast shadows, drop shadows, floor, grass, dirt, ground strips, haze, halos or scenery. Each entire landmark stays separate inside its own cell.
Match reference cozy thick dark outlines and large crisp pixel clusters, honey-colored wood, warm stone, controlled limited palette. Intentional low-resolution pixel art magnified with nearest-neighbor edges. No soft gradients, no smoothing, no realistic material noise. No words anywhere.
same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark
```

## 마을 랜드마크 수정

```text
Use case: precise-object-edit. Edit this SIX-COLUMN landmark sprite sheet. Preserve the six-column one-row layout, all six identities, current thick-outline cozy pixel art, the flag stripes, the four X windmill blades, the FIVE lanterns, the bell and statue.

Only fix the THIRD landmark: its two wooden posts and hanging lanterns are too low. Make BOTH POSTS tall, rising to the same upper line as the neighboring windmill roof. Move the entire sagging rope and its exact FIVE alternating red/yellow/red/yellow/red lanterns UP with the tops of the posts, leaving longer clear wood posts underneath. Its top must be at about17% canvas height, its feet at95% height, so its full landmark height is78% of the canvas. Keep this third landmark inside its own sixth-width column. Keep the two feet at the current same baseline as all other landmarks. Do not make the lanterns or pole width larger. Nothing else should change.

The middle pirate flag emblem should clearly be a SEAFARING METAL HOOK with a curved pointed barbed end and a small closed attachment ring, not a number or punctuation mark. Preserve shell and crab on the other two black flags, and all three flags/poles.

Background remains perfectly uniform pure #FF00FF magenta, including between posts, no shadows, gradients, halos, ground or floor. Do not crop the six landmarks, do not merge columns, do not add a seventh object.
same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark
```

## 강화 반짝임

```text
Use case: stylized-concept.
Asset type: four-frame game upgrade pop effect sprite strip for Little Island.
Create ONE horizontal strip, exactly FOUR equal SQUARE cells in ONE ROW, intended export 1024x256, each cell256x256. Do not make a 2x2 grid. Effects are exactly centered at local x128,y128 in each cell. Invisible dividers at x256,512,768; absolutely no visible grid lines, labels or numbers.

Four successive moments of ONE cheerful GOLDEN STARBURST:
FRAME1, leftmost: a SMALL bright golden starburst opening, compact pointed rays in a loose star arrangement around the center, about116px outer width, with a completely EMPTY middle. A few short crisp angular rays only, no filled central star.
FRAME2: the BIGGEST brightest dense pop, strong gold starburst rays radiating outward around the empty center, overall width212px and height212px, warm gold and ivory highlights, a few small gold diamond sparkles/coin-like glints. Empty center remains open.
FRAME3: rays have spread a little farther apart and become THINNER and LIGHTER, overall width228px and height228px. Much more sparse than frame2. Pale ivory/gold angular streaks, a few outward sparkles. Same open middle.
FRAME4, rightmost: burst has disappeared; ONLY FOUR small isolated gold four-point sparkles remain at different offsets around the empty center. NO ring and NO long rays, no big central shape. A visibly quiet fading end frame.

ESSENTIAL EMPTY CENTER: in EVERY cell reserve a completely plain magenta rectangular center approx x84..172,y100..156. It is invisible except for the absence of drawing; no outline around this space. This clear opening is for the app's overlaid numbers. No coin, star, glow, flare, dot or light in the very center. Rays end outside this center.
Keep every ray and sparkle at least12px from its cell's edges. Never overlap across cell boundaries. Do not stretch the gold effects vertically. All cells same256x256 scale.

Style: cozy16-bit pixel effects with crisp chunky angular stair-step edges, bold dark amber outlines and simple gold/ivory highlights. Solid color clusters, no smooth vector curves, bloom, blur, fuzzy glow, haze, particles like dust, background rays or drop shadow. Warm limited palette matching a wood-and-gold cozy sailing game.
Every empty pixel, including the centers and space between effects, must be exact pure MAGENTA #FF00FF, completely uniform opaque color key. No gradients, grain, floor, ground, backdrop or shadows. No text or numbers even though the effect is intended behind an upgrade number. Only the effect itself.
same style as the previous pictures, cozy 16-bit pixel art, crisp hard-edged pixels, strong dark outlines, limited warm palette, side view, no text, no letters, no numbers, no watermark
```
