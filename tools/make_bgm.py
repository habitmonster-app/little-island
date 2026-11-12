import numpy as np, wave
SR=32000; BPM=96; beat=60/BPM; e=beat/2  # 6/8: 6 eighths per bar
def hz(n): return 440*2**((n-69)/12)
N={'C':0,'D':2,'E':4,'F':5,'G':7,'A':9,'B':11}
def m(s):  # "E5" -> midi
    return 12*(int(s[-1])+1)+N[s[0]]+(1 if '#' in s else 0)
# 16 bars of 6/8, chords per bar (C F G Am progression, gentle)
chords=['C','Am','F','G','C','Am','F','G', 'F','C','Dm','G','F','C','G','C']
CH={'C':[48,52,55],'Am':[45,48,52],'F':[41,45,48],'G':[43,47,50],'Dm':[38,41,45]}
# melody: list of (note or None, eighths) per bar, each bar sums to 6
mel=[
 [('E5',3),('D5',1),('C5',2)], [('E5',2),('A4',1),('C5',3)],
 [('A4',2),('C5',1),('F5',3)], [('D5',4),('B4',2)],
 [('G5',3),('E5',1),('C5',2)], [('E5',2),('D5',1),('C5',3)],
 [('A4',2),('C5',1),('D5',2),('F5',1)], [('E5',4),('D5',2)],
 [('F5',3),('E5',1),('D5',2)], [('E5',3),('G4',3)],
 [('F5',2),('E5',1),('D5',3)], [('B4',4),('G4',2)],
 [('A4',2),('C5',1),('F5',3)], [('E5',2),('D5',1),('C5',3)],
 [('D5',2),('E5',1),('B4',3)], [('C5',6)],
]
bars=len(chords); total=int(bars*6*e*SR)
out=np.zeros(total)
def tone(f,dur,kind,vol):
    n=int(dur*SR); t=np.arange(n)/SR
    if kind=='pulse':
        ph=(t*f)%1; w=np.where(ph<0.25,1.0,-1.0)
        # soften: mix with sine
        w=0.45*w+0.55*np.sin(2*np.pi*f*t)
        env=np.minimum(1,t/0.01)*np.exp(-t*2.2)
        vib=1
    elif kind=='tri':
        ph=(t*f)%1; w=4*np.abs(ph-0.5)-1; env=np.minimum(1,t/0.01)*np.exp(-t*1.5)
    else:
        w=np.sin(2*np.pi*f*t)+0.3*np.sin(4*np.pi*f*t); env=np.minimum(1,t/0.005)*np.exp(-t*6)
    rel=np.minimum(1,(n-np.arange(n))/(0.03*SR)); return w*env*rel*vol
def add(start,sig):
    i=int(start*SR); j=min(total,i+len(sig)); out[i:j]+=sig[:j-i]
for b in range(bars):
    t0=b*6*e
    c=CH[chords[b]]
    # bass: root on 1 and 4 (6/8 lilt)
    add(t0,tone(hz(c[0]-12+12),3*e,'tri',0.30)); add(t0+3*e,tone(hz(c[0]+7 if chords[b]!='Dm' else c[0]+7),3*e,'tri',0.22))
    # arpeggio soft bells on eighths 1..5
    arp=[c[0]+12,c[1]+12,c[2]+12,c[1]+12,c[2]+12,c[1]+12]
    for k,n in enumerate(arp): add(t0+k*e,tone(hz(n),e*1.6,'bell',0.07))
    tt=t0
    for nn,d in mel[b]:
        add(tt,tone(hz(m(nn)),d*e*0.95,'pulse',0.16)); tt+=d*e
# gentle wave noise swell
rng=np.random.default_rng(3); noise=rng.standard_normal(total)
k=np.ones(400)/400; noise=np.convolve(noise,k,'same')*6
sw=0.5-0.5*np.cos(2*np.pi*np.arange(total)/SR/(6*e*2))
out+=noise*sw*0.05
# loop-friendly: wrap tails
tail=int(1.5*SR); out[:tail]+=0  # notes already decay
out=out/np.max(np.abs(out))*0.6
pcm=(out*32767).astype(np.int16)
w=wave.open('bgm.wav','wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes()); w.close()
print(total/SR,'sec')
