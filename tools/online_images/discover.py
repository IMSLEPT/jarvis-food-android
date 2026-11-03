"""Find reusable Wikimedia candidates; never assigns an unreviewed result to a recipe.
Usage: python3 discover.py 'orange slices' output.json
Only discovery: review ingredients, quality and license before using an image.
"""
import json,subprocess,sys,urllib.parse
query,out=sys.argv[1:]
url='https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(dict(action='query',format='json',generator='search',gsrsearch=query+' filetype:bitmap',gsrnamespace=6,gsrlimit=8,prop='imageinfo',iiprop='url|extmetadata',iiurlwidth=960))
data=json.loads(subprocess.check_output(['curl','-fsSL','--connect-timeout','10','--max-time','40',url]))
rows=[]
for p in data.get('query',{}).get('pages',{}).values():
 for im in p.get('imageinfo',[]):
  m=im.get('extmetadata',{});val=lambda k:m.get(k,{}).get('value','')
  lic=val('LicenseShortName')
  if not (lic.startswith('CC BY') or lic in ('CC0','Public domain')):continue
  rows.append(dict(title=p['title'],source=im['descriptionurl'],download=im.get('thumburl',im['url']),author=val('Artist'),license=lic,licenseUrl=val('LicenseUrl'),description=val('ImageDescription')))
open(out,'w').write(json.dumps(rows,ensure_ascii=False,indent=2))
print(query,len(rows),'candidates')
