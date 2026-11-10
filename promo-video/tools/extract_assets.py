#!/usr/bin/env python3
"""Ritaglia gli asset dalle immagini di riferimento (storyboard + schermate) in public/img.
Uso: python3 tools/extract_assets.py <cartella-immagini-sorgente>
Richiede: pillow, numpy, scipy, rembg (modello isnet-general-use)."""
import sys, os, json
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as ndi

SRC = sys.argv[1] if len(sys.argv) > 1 else 'reference'
OUT = os.path.join(os.path.dirname(__file__), '..', 'public', 'img')
os.makedirs(OUT, exist_ok=True)
F = dict(story='4e0f32cd', s6='2a9f17a1', s8='58ee08b4', s7='3242324c', s2='e332c041', s1='8a4cfb05',
         s3='6c70b388', s4='bb491383', s5='d4bde26d', progressi='e8192233', chat='f67fdeea',
         cucina='44819a58', piano='fd3ee800', oggi='973e1674', profilo='dadab2d0', icon='794a393a')
def load(k): return Image.open(os.path.join(SRC, F[k] + '-image.png')).convert('RGB')
meta = {}
def save(im, name, q=None):
    p = os.path.join(OUT, name)
    if name.endswith('.jpg'): im.convert('RGB').save(p, quality=q or 93)
    else: im.save(p)
    meta[name] = im.size

# ---------- 1. crop rettangolari (foto, strisce UI, icone su bianco) ----------
RECT = {
 'oggi': {'header.png': (0, 0, 941, 160), 'nav_oggi.png': (0, 1512, 941, 1672),
          'ph_colazione.jpg': (62, 976, 238, 1052), 'ph_spuntino.jpg': (62, 1106, 238, 1182),
          'ph_pranzo.jpg': (62, 1238, 238, 1316), 'ph_cena.jpg': (62, 1371, 238, 1453),
          'ic_aggiungi.png': (112, 618, 190, 694), 'ic_foto.png': (328, 620, 406, 690),
          'ic_frigo.png': (550, 618, 616, 694), 'ic_digiuno.png': (762, 616, 834, 694),
          'ic_idra.png': (44, 800, 100, 868), 'ic_vit.png': (376, 800, 440, 866), 'ic_att.png': (672, 796, 740, 868)},
 'piano': {'nav_piano.png': (0, 1512, 941, 1672), 'li_banane.png': (120, 1245, 222, 1312), 'li_salmone.png': (118, 1322, 232, 1378),
           'li_pomodorini.png': (125, 1388, 222, 1452), 'li_zucchine.png': (560, 1250, 665, 1312), 'li_uova.png': (568, 1322, 662, 1380),
           'li_yogurt.png': (572, 1390, 662, 1452), 'ic_cart.png': (60, 1172, 130, 1232), 'ic_target.png': (412, 382, 468, 438)},
 's8': {'play_badge.png': (213, 1133, 818, 1328)},
 'cucina': {'nav_cucina.png': (0, 1512, 941, 1672), 'ph_hero.jpg': (33, 519, 907, 822),
            'ph_bowl.jpg': (57, 1003, 298, 1120), 'ph_toast.jpg': (57, 1172, 298, 1288), 'ph_insalata.jpg': (57, 1341, 298, 1458)},
 'progressi': {'nav_progressi.png': (0, 1528, 941, 1672), 'ic_peso.png': (106, 776, 174, 830),
               'ic_grasso.png': (336, 772, 394, 832), 'ic_muscolo.png': (552, 772, 622, 832), 'ic_acqua.png': (776, 772, 836, 832)},
 'chat': {'nav_jarvis.png': (0, 1528, 941, 1672), 'avatar.png': (20, 490, 100, 570), 'ph_chat.jpg': (147, 1212, 346, 1296)},
 's5': {'ph_hero_big.jpg': (232, 462, 895, 780)},
}
for k, d in RECT.items():
    im = load(k)
    for n, b in d.items(): save(im.crop(b), n)

# ---------- 2. scontorni con rembg ----------
from rembg import remove, new_session
sess = new_session('isnet-general-use')
def cut(k, box, name, pad=0):
    im = load(k).crop(box)
    o = remove(im, session=sess)
    a = np.asarray(o).copy()
    al = a[..., 3].astype(np.float32)
    al = np.clip((al - 110) / 110, 0, 1) * 255            # via gli aloni semitrasparenti
    lab, n = ndi.label(al > 128)                           # tiene solo le componenti grandi
    if n > 1:
        sizes = ndi.sum(al > 128, lab, range(1, n + 1))
        keep = np.isin(lab, [i + 1 for i, s_ in enumerate(sizes) if s_ > 0.04 * sizes.max()])
        al = al * ndi.binary_dilation(keep, iterations=2)
    if name == 'robot.png':
        yy, xx = np.mgrid[0:al.shape[0], 0:al.shape[1]]
        bb0 = o.getchannel('A').point(lambda v: 255 if v > 20 else 0).getbbox()
        x = xx - bb0[0]; y = yy - bb0[1]
        al[(x > 385) & (y < 195)] = 0                      # fumetto
        al[(x < 98) & (y > 560) & (y < 748)] = 0           # scia a sinistra
        al[(y > 748) & (x > 215)] = 0; al[y > 806] = 0     # scia in basso
    a[..., 3] = al.astype(np.uint8)
    o = Image.fromarray(a)
    bb = o.getchannel('A').point(lambda v: 255 if v > 20 else 0).getbbox()
    if bb: o = o.crop(bb)
    save(o, name)
CUT = [
 ('s7', (0, 170, 580, 1030), 'robot.png'),
 ('s8', (0, 40, 330, 450), 'f_avocado.png'), ('s8', (790, 0, 1086, 300), 'f_tomatoes.png'),
 ('s8', (0, 570, 240, 890), 'f_oats.png'), ('s8', (840, 550, 1086, 810), 'f_basil.png'),
 ('s8', (810, 920, 1086, 1310), 'f_dumbbell.png'),
 ('s5', (0, 170, 215, 385), 'f_salmon.png'), ('s5', (0, 350, 150, 510), 'f_zucchini.png'),
 ('s5', (780, 0, 1070, 240), 'f_basil2.png'), ('s5', (770, 890, 1086, 1140), 'f_tomato2.png'),
 ('s2', (30, 440, 300, 680), 't_dumbbell.png'), ('s2', (30, 740, 290, 970), 't_leaf.png'),
 ('s2', (815, 250, 1070, 490), 't_fork.png'), ('s2', (815, 530, 1070, 750), 't_heart.png'),
 ('s2', (745, 750, 1010, 990), 't_chart.png'),
 ('chat', (80, 120, 262, 286), 'robot_small.png'),
]
for c in CUT: cut(*c)

# ---------- 3. logo a pezzi (chiave di luminosita' + apertura morfologica per togliere l'anello) ----------
ic = np.asarray(load('icon')).astype(np.float32)
mx = ic.max(axis=2); mn = ic.min(axis=2)
sat = (mx - mn) / (mx + 1)
orange = (ic[..., 0] > 150) & (ic[..., 0] - ic[..., 2] > 90)
white = (mx > 150) & (sat < 0.35) & (ic[..., 2] > 140)
solid = ndi.binary_opening(orange | white, structure=np.ones((3, 3)), iterations=7)
solid = ndi.binary_dilation(solid, iterations=9)
key = np.clip((mx - 95) / 70, 0, 1)
warm = np.clip((ic[..., 0] - ic[..., 2] - 30) / 60, 0, 1)
whit = np.clip((ic[..., 2] - 120) / 60, 0, 1) * np.clip((0.45 - sat) / 0.2, 0, 1)
alpha = key * np.maximum(warm, whit) * solid
alpha = ndi.gaussian_filter(alpha, 0.8)
rgba = np.dstack([ic, alpha * 255]).astype(np.uint8)
LOGO_BOX = (150, 110, 1180, 1030)
PIECES = {'hat': (380, 130, 880, 520), 'jarvis': (150, 430, 1090, 830), 'food': (300, 620, 980, 990),
          'smile': (400, 830, 900, 1030), 'sparks': (960, 110, 1180, 300)}
logo_meta = {'box': [LOGO_BOX[2] - LOGO_BOX[0], LOGO_BOX[3] - LOGO_BOX[1]], 'pieces': {}}
yy, xx = np.mgrid[0:1254, 0:1254].astype(np.float32)
from matplotlib.path import Path as MP
def poly(pts):
    pts = [(x * 1.254, y * 1.254) for x, y in pts]
    return MP(pts).contains_points(np.c_[xx.ravel(), yy.ravel()]).reshape(1254, 1254)
# tiene solo le componenti "piene" del logo (scarta anello luminoso e pavimento)
st = ndi.generate_binary_structure(2, 1)
core = ndi.binary_opening(orange | white, structure=st, iterations=15)
core |= ndi.binary_opening(orange | white, structure=st, iterations=6) & poly([(755, 70), (920, 70), (920, 235), (790, 235)])
core |= ndi.binary_opening(orange | white, structure=st, iterations=9) & poly([(345, 702), (480, 716), (600, 704), (700, 668), (705, 790), (345, 790)])
core &= yy < 998
cl, cn = ndi.label(core)
keepc = np.zeros_like(core)
for i in range(1, cn + 1):
    m = cl == i
    if m.sum() < 400: continue
    cy, cx = ndi.center_of_mass(m)
    if (270 < cx < 1010 and 150 < cy < 1010) or (960 < cx < 1180 and 110 < cy < 300):
        keepc |= m
near = ndi.gaussian_filter(ndi.binary_dilation(keepc, iterations=4).astype(np.float32), 1.5)
alpha = alpha * near
alpha = alpha * np.clip((996 - yy) / 22, 0, 1)
alpha[(xx > 968) & (yy > 770)] = 0
rgba = np.dstack([ic, alpha * 255]).astype(np.uint8)
m_hat = poly([(318, 100), (760, 100), (760, 335), (690, 345), (655, 402), (428, 412), (418, 335), (318, 335)])
split = np.where(xx < 340, 745, np.where(xx < 900, 700 - (xx - 340) * 0.0804, np.where(xx < 962, 643, 662)))
m_main = (xx > 150) & (xx < 1100) & (yy > 380) & ~m_hat
m_smile = poly([(345, 702), (480, 716), (600, 704), (700, 668), (705, 800), (345, 800)])
m_jarvis = m_main & (yy < split)
m_food = m_main & (yy >= split) & ~m_smile & (yy < 900)
m_sparks = poly([(755, 70), (920, 70), (920, 235), (790, 235)])
MASKS = {'hat': m_hat, 'jarvis': m_jarvis, 'food': m_food, 'smile': m_smile, 'sparks': m_sparks}
allm = np.zeros((1254, 1254), bool)
clean = np.zeros((1254, 1254), np.float32)
for nme, m in MASKS.items():
    allm |= m
    pa = alpha * m
    pl_, pn_ = ndi.label(pa > 0.25)                      # via i frammenti isolati (residui di anello)
    if pn_ > 1:
        sz = ndi.sum(pa > 0.25, pl_, range(1, pn_ + 1))
        thr = 250 if nme == 'sparks' else max(1800, 0.02 * sz.max())
        good = np.isin(pl_, [i + 1 for i, v in enumerate(sz) if v >= thr])
        pa = pa * ndi.gaussian_filter(ndi.binary_dilation(good, iterations=3).astype(np.float32), 1.0)
    clean[m] = pa[m]
    a2 = rgba.copy(); a2[..., 3] = (pa * 255).astype(np.uint8)
    im = Image.fromarray(a2); bb = im.getchannel('A').point(lambda v: 255 if v > 30 else 0).getbbox()
    save(im.crop(bb), f'logo_{nme}.png')
    logo_meta['pieces'][nme] = [bb[0] - LOGO_BOX[0], bb[1] - LOGO_BOX[1], bb[2] - bb[0], bb[3] - bb[1]]
rgba[..., 3] = (clean * 255).astype(np.uint8)
json.dump(logo_meta, open(os.path.join(OUT, '..', '..', 'src', 'logoMeta.json'), 'w'))
logo_meta0 = {'box': [LOGO_BOX[2] - LOGO_BOX[0], LOGO_BOX[3] - LOGO_BOX[1]], 'pieces': {}}
full = Image.fromarray(rgba)
save(Image.fromarray(rgba).crop(LOGO_BOX), 'logo_full.png')
Image.fromarray(rgba).save('/tmp/logo_dbg.png')
json.dump(meta, open(os.path.join(OUT, 'sizes.json'), 'w'), indent=1)
print(n, 'components')
