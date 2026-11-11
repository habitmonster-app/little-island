import sys
import numpy as np
from PIL import Image
D, A, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
def isbg(a):
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    return (r > 110) & (b > 110) & (g * 2 < np.minimum(r, b))
def bbox(a):
    m = ~isbg(a); ys, xs = np.nonzero(m); return xs.min(), ys.min(), xs.max(), ys.max()
def orig(lv):
    src = A + ('ship_stages.png' if lv <= 4 else 'ship_stage%d.png' % lv)
    im = Image.open(src).convert('RGBA'); cw = im.width // 4
    box = (3*cw, (lv-1)*cw, 4*cw, lv*cw) if lv <= 4 else (3*cw, 0, 4*cw, im.height)
    c = im.crop(box); bg = Image.new('RGBA', c.size, (255, 0, 255, 255)); bg.alpha_composite(c); return np.array(bg.convert('RGB'))
def down(a, sc, dx, dy):
    """원본 큰 그림을 128칸으로: 칸마다 배경이 반 넘으면 마젠타, 아니면 배경 아닌 점들의 가운데값 색"""
    H, W, _ = a.shape; bg = isbg(a)
    out = np.zeros((128, 128, 3), np.uint8); out[...] = (255, 0, 255)
    inv = 1 / sc
    for ty in range(128):
        y0 = (ty - dy) * inv; y1 = y0 + inv
        if y1 <= 0 or y0 >= H: continue
        ya, yb = max(0, int(round(y0))), min(H, int(round(y1)))
        if yb <= ya: continue
        for tx in range(128):
            x0 = (tx - dx) * inv; x1 = x0 + inv
            if x1 <= 0 or x0 >= W: continue
            xa, xb = max(0, int(round(x0))), min(W, int(round(x1)))
            if xb <= xa: continue
            blk = a[ya:yb, xa:xb].reshape(-1, 3); m = ~bg[ya:yb, xa:xb].reshape(-1)
            if m.mean() < .5: continue
            p = blk[m]
            # 가장 흔한 색 (가운데 쪽 점에 가중) → 번지지 않은 또렷한 색
            q = (p // 16).astype(np.int32); key = q[:, 0] * 256 + q[:, 1] * 16 + q[:, 2]
            vals, cnt = np.unique(key, return_counts=True); k = vals[cnt.argmax()]
            out[ty, tx] = np.median(p[key == k], axis=0).astype(np.uint8)
    return out
for lv in range(2, 8):
    raw = np.array(Image.open('%s/raw_%d.png' % (D, lv)).convert('RGB'))
    H, W, _ = raw.shape; cw = W // 2
    o = orig(lv); ob = bbox(o)
    L = raw[:, :cw]; lb = bbox(L)
    sc = (ob[2] - ob[0]) / (lb[2] - lb[0])
    dx = ob[0] - lb[0] * sc; dy = ob[3] + 1 - (lb[3] + 1) * sc
    sheet = np.zeros((128, 256, 3), np.uint8)
    for k in range(2): sheet[:, k*128:(k+1)*128] = down(raw[:, k*cw:(k+1)*cw], sc, dx, dy)
    Image.fromarray(sheet).save('%s/ship_refit_%d.png' % (OUT, lv)); print(lv, 'ok %.3f' % sc)
