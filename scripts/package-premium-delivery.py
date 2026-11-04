from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import os,json,time,urllib.request,hashlib,re,zipfile
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen.canvas import Canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.lib.colors import HexColor
REPO='IMSLEPT/jarvis-food-android'
RUN=int(os.environ['ANDROID_RUN']);SHA=os.environ['ANDROID_SHA']
root=Path('delivery-work');root.mkdir(exist_ok=True);out=Path('delivery');out.mkdir(exist_ok=True)
def api(path):
 req=urllib.request.Request('https://api.github.com/repos/'+REPO+path,headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Accept':'application/vnd.github+json'})
 with urllib.request.urlopen(req,timeout=90) as r:return json.load(r)
for attempt in range(100):
 run=api('/actions/runs/'+str(RUN))
 if run['status']=='completed':break
 time.sleep(12)
else:raise RuntimeError('Android verification not finished')
assert run['conclusion']=='success',run['conclusion']
assert run['head_sha']==SHA
meta=api('/releases/tags/preview-v1.0.227-redesign')
assert meta['target_commitish']==SHA,meta['target_commitish']
names={'JarvisFood-v1.0.227.apk','screenshots.zip','android-tests.log','unit-tests.txt','assets-unchanged.txt','original-certificate.txt','new-certificate.txt','SHA256.txt'}
def fetch(a,folder):
 folder.mkdir(exist_ok=True);p=folder/a['name']
 with urllib.request.urlopen(a['browser_download_url'],timeout=150) as r,p.open('wb') as f:
  while chunk:=r.read(1024*1024):f.write(chunk)
 assert 'sha256:'+hashlib.sha256(p.read_bytes()).hexdigest()==a['digest']
 return p
with ThreadPoolExecutor(max_workers=4) as pool:
 list(pool.map(lambda a:fetch(a,root/'final'),[a for a in meta['assets'] if a['name'] in names]))
previous=api('/releases/tags/preview-v1.0.226-redesign')
for a in previous['assets']:
 if a['name'] in {'screenshots.zip','android-tests.log','unit-tests.txt'}:fetch(a,root/'v226')
for v in ('final','v226'):
 with zipfile.ZipFile(root/v/'screenshots.zip') as z:z.extractall(root/v/'shots')
shots=root/'final/shots/files/shots';oldshots=root/'v226/shots/files/shots'
log=(root/'final/android-tests.log').read_text()
assert not re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=',log)
counts=[int(v) for v in re.findall(r'^OK \((\d+) tests?\)',log,re.M)]
assert counts==[2,2,2,2],counts
assert '164 test unitari, 0 falliti' in (root/'final/unit-tests.txt').read_text()
for label in ('original','latest-preview','new-release'):assert 'PASS '+label+':' in log
cert='a039be1f4e694272f35ac964d46d28185535ce28b2dcfb5bbbedda264b4d80ca'
for n in ('original-certificate.txt','new-certificate.txt'):assert cert in (root/'final'/n).read_text()
assert (root/'final/assets-unchanged.txt').read_text().startswith('671 ')
apk=root/'final/JarvisFood-v1.0.227.apk';sha=hashlib.sha256(apk.read_bytes()).hexdigest()
assert sha in (root/'final/SHA256.txt').read_text()
vpath=Path('reports/premium-227-visual-verdict.json')
verdict=json.loads(vpath.read_text()) if vpath.exists() else {'result':'pending'}
if verdict['result']=='passed':
 assert verdict['source_sha']==SHA and verdict['run_id']==RUN and verdict['source_and_final_compared_together']
labels={'main-0':'Oggi','main-1':'Piano','main-2':'Cucina','main-3':'Progressi','main-5':'Jarvis','profile-top':'Profilo','profile-tools':'Profilo - strumenti','settings-profile':'Dati personali','settings-goals':'Calorie e macronutrienti','settings-diet':'Preferenze dieta','settings-training-goals':'Giorni ON e OFF','settings-ai':'Intelligenza artificiale','settings-reminders':'Promemoria','settings-measurements':'Misure corporee','settings-backup':'Backup e dati','settings-backup-bottom':'Backup / gesti Android','settings-watch-bottom':'Smartwatch / collegamento'}
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20)
c=Canvas(str(out/'JarvisFood-v1.0.227-Prima-Dopo.pdf'),pagesize=A4);W,H=A4
c.setTitle('Jarvis Food 1.0.227 - confronti Android reali')
def page(label,a,b,before='PRIMA - v224',after='DOPO - v227',note='Stessi dati, stesso emulatore Android 15, testo al 100%.'):
 c.setFillColor(HexColor('#F7F8FA'));c.rect(0,0,W,H,fill=1,stroke=0)
 c.setFillColor(HexColor('#09194D'));size=20
 while c.stringWidth('Jarvis Food / '+label,'Helvetica-Bold',size)>W-56:size-=1
 c.setFont('Helvetica-Bold',size);c.drawString(28,H-42,'Jarvis Food / '+label)
 c.setFont('Helvetica',10);c.setFillColor(HexColor('#5E6678'));c.drawString(28,H-62,note)
 c.setFont('Helvetica-Bold',12);c.drawString(28,H-94,before);c.drawString(W/2+12,H-94,after)
 for col,p in enumerate((a,b)):
  im=Image.open(p);iw,ih=im.size;scale=min((W-80)/2/iw,(H-150)/ih);w,h=iw*scale,ih*scale
  c.drawImage(ImageReader(im),28+col*(W/2-16),H-108-h,width=w,height=h)
 c.setFont('Helvetica',9);c.drawString(28,24,'Screenshot Android reali; immagini ridotte per il confronto.');c.showPage()
for name,label in labels.items():
 a=shots/'whole-before'/(name+'.png');b=shots/'whole-pixel9'/(name+'.png')
 assert a.exists() and b.exists(),name
 page(label,a,b)
 if name in ('main-0','main-1','main-2','main-3','main-5','profile-top'):
  board=Image.new('RGB',(940,1080),'#F7F8FA');d=ImageDraw.Draw(board)
  for i,p in enumerate((a,b)):
   im=Image.open(p);im.thumbnail((448,998));board.paste(im,(12+i*470,60))
   d.text((16+i*470,18),('Prima / v224' if i==0 else 'Dopo / v227')+' - '+label,font=font,fill='#09194D')
  board.save(out/('confronto-'+name+'.png'))
page('Smartwatch / testo al 130%',oldshots/'whole-large-text/settings-watch-bottom.png',shots/'whole-large-text/settings-watch-bottom.png','PRIMA - v226 / 130%','DOPO - v227 / 130%','Confronto del diagramma e della parola Connect con caratteri ingranditi.')
c.save()
pngs=list(shots.rglob('*.png'))
score='Verifica visiva finale in attesa; i soli test automatici non certificano la qualita estetica.'
if verdict['result']=='passed':
 keys=['coerenza','proporzioni','leggibilita','gerarchia','fedelta']
 score='Valutazione visiva interna: '+str(sum(verdict[k] for k in keys)/5)+'/10. '+', '.join(k+': '+str(verdict[k])+'/10' for k in keys)+'. Giudizio sui percorsi e sulle configurazioni documentate, non un test con utenti reali.'
if verdict['result']=='passed':
 score += ' Nessun problema bloccante o sovrapposizione rilevato nei percorsi verificati.'
report=f'''# Jarvis Food 1.0.227 - correzioni e verifiche

Data: 4 novembre 2026.

Applicazione originale aggiornata. Package com.jarvis.food; stessa firma originale.
Installare sopra la versione attuale senza disinstallarla.

## Correzioni applicate

- Banner compatti, gerarchia tipografica uniforme e calendari Oggi/Piano alla stessa altezza anche con nomi lunghi.
- Tipo giornata: icona 40 dp, titolo 16 sp e sottotitolo 13 sp.
- Pulsanti principali 50/52 dp; raggi, bordi, icone, ombre e padding coordinati.
- Macro da 12 sp, anelli live 32 dp e righe compatte sui display stretti con caratteri ingranditi.
- Schede pasto: foto 2:1 e titolo 20 sp; mangiato/annulla, alternative e codice a barre conservati.
- Navigazione con icone 22 dp ed elementi minimi 56 dp.
- Cucina: linguette distribuite, scorrimento sui display stretti, spazi ridotti tra sezioni.
- Profilo organizzato in tre gruppi con separatori e icone allineati.
- Componenti Material coordinati per impostazioni, campi e finestre.
- Comandi delle schermate secondarie sopra l'area dei gesti Android.
- Diagramma smartwatch: misura e resa del testo allineate, larghezze adattate alle parole, limiti delle righe verificati.
- Contrasto delle scritte bianche sulle azioni arancioni: 4,62-5,50:1.

Conservati navy, arancione, sfondi chiari, font Bricolage/DM Sans e immagini esistenti. Riferimenti ridimensionati per l'uso reale sul telefono.

## Test realmente eseguiti

- 164 test unitari, zero fallimenti nella compilazione finale.
- 6 esecuzioni Android sulla v227: 2 test a tre configurazioni, piu 2 sulla base v224. Tutte 8 passate. Un test percorre cinque sezioni e 13 destinazioni del Profilo; l'altro verifica i calendari con nomi lunghi.
- Android 15/API35, emulatore Google APIs pixel_7_pro: 1344x2992/densita480 con testo100% e115%; 945x2048/densita420 con testo130%. Larghezze logiche448 e360 dp, verticale.
- Limiti dei comandi rispetto ai gesti Android; limiti delle righe e parole intere nel diagramma smartwatch.
- Suite estesa: 44 esecuzioni Android passate sulla v226, prima dell'ultima modifica isolata al diagramma. Pasti, mangiato/annulla, alternative, barcode, dialoghi, dieta, ricerca, calendario, tastiera/chat e regressioni. Log distinto: https://github.com/{REPO}/actions/runs/41919566657
- Firma originale apksigner confermata; APK release installato e avviato.
- Aggiornamento firmato v220 -> v226 -> v227: profilo, acqua, passi e chat della fixture conservati. Nessun dato del telefono dell'utente letto.
- 671 asset applicativi identici byte per byte, esclusi due profili di compilazione.
- Verifica sorgenti precedente: 187 file funzionali/dati originali invariati; 31 modifiche precedentemente autorizzate. Nessuna nuova modifica a persistenza o motori nutrizionali.
- Catalogo fotografie: 170 ricette complete, 821 mappature, 329 riusi compatibili; controllo superato.

## Immagini e valutazione

{len(pngs)} screenshot nel passaggio finale. PDF: 17 coppie v224/v227 con gli stessi dati e una coppia smartwatch v226/v227 al130%, 18 pagine.
Screenshot da build di test degli stessi sorgenti; installazione/avvio della release firmata verificati separatamente.
La fixture senza foto profilo usa l'icona; la foto salvata continua a essere usata quando presente.
Le immagini delle interazioni della v226 riguardano componenti invariati e sono in una cartella separata nello ZIP.

{score}

## Limiti

Nessuna prova su dispositivo fisico, altre versioni Android o integrazioni esterne reali AI, Health Connect e fotocamera/scanner. Sono intenzionali lo scorrimento delle linguette strette e l'abbreviazione dei suggerimenti di ricerca quando non entrano. I moduli sono scorrevoli.

L'ambiente locale si e disconnesso durante la consegna: il codice era gia salvato. File e confronti sono ricostruiti dalle prove della compilazione remota, controllando i digest degli asset pubblicati. Lo stato della verifica visiva finale e indicato sopra.

## Tracciabilita

Codice APK: {SHA}
CI finale: https://github.com/{REPO}/actions/runs/{RUN}
Release: {meta['html_url']}
SHA256 APK: {sha}
Certificato SHA256: {cert}
'''
(out/'JarvisFood-v1.0.227-Verifiche.md').write_text(report)
with zipfile.ZipFile(out/'JarvisFood-v1.0.227-Schermate.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in pngs:z.write(p,'schermate-finali/'+str(p.relative_to(shots)))
 for p in out.glob('confronto-*.png'):z.write(p,'confronti/'+p.name)
 for p in (root/'final').iterdir():
  if p.suffix in ('.txt','.log'):z.write(p,'verifiche-finali/'+p.name)
 for p in oldshots.rglob('*.png'):
  if p.parent.name.startswith('redesign-'):z.write(p,'componenti-invariati-v226/'+str(p.relative_to(oldshots)))
 for n in ('android-tests.log','unit-tests.txt'):z.write(root/'v226'/n,'verifiche-precedenti-v226/'+n)
 z.writestr('LEGGIMI.md',report)
status=dict(source_sha=SHA,run_id=RUN,visual=verdict['result'],screenshots=len(pngs),unit=164,android=8,apk_sha256=sha)
(out/'delivery-status.json').write_text(json.dumps(status,indent=2))
print(json.dumps(status))

# Render representative PDF pages and retain byte-exact visual payloads for review without a local runtime.
import base64
import pypdfium2 as pdfium
doc=pdfium.PdfDocument(str(out/'JarvisFood-v1.0.227-Prima-Dopo.pdf'))
assert len(doc)==18
for index in (0,17):
 page=doc[index];bitmap=page.render(scale=2)
 bitmap.to_pil().save(out/f'pdf-preview-{index+1:02}.png')
 bitmap.close();page.close()
doc.close()
visual=Path('reports/visual-encoded');visual.mkdir(exist_ok=True)
items=[]
images=list(out.glob('*.png'))+[shots/'whole-large-text/settings-watch-bottom.png',shots/'whole-large-text/settings-backup-bottom.png']
for p in images:
 name=p.stem if p.parent==out else p.stem+'-text130'
 encoded=base64.b64encode(p.read_bytes()).decode('ascii');parts=[]
 for index,start in enumerate(range(0,len(encoded),60000)):
  part=name+f'-{index:03}.txt';(visual/part).write_text(encoded[start:start+60000]);parts.append(part)
 items.append(dict(name=name,parts=parts,bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
(visual/'manifest.json').write_text(json.dumps(dict(source_sha=SHA,run_id=RUN,images=items),indent=2))

if verdict['result']=='passed':
 expected=verdict['reviewed_image_sha256']
 assert len(expected)==len(items)==10
 for item in items:
  assert expected[item['name']]==item['sha256'], 'Reviewed image changed: '+item['name']
 print('Visual seal: all ten reviewed comparison/native/rendered images are byte-identical.')
