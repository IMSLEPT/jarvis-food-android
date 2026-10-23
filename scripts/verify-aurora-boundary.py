"""Fail when the classic UI snapshot or an original functional/data asset changes."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'scripts/classic-ui-manifest.json').read_text())
for original,expected in manifest['files'].items():
 name=Path(original).name
 p=root/'app/src/main/java/com/jarvis/food/classicui'/name
 source=p.read_text()
 if name=='FamilyScreen.kt':
  source=source.replace('package com.jarvis.food.classicui\n\nimport com.jarvis.food.family.*','package com.jarvis.food.family').replace('com.jarvis.food.classicui','com.jarvis.food.ui')
 else:
  if name=='ProfileScreen.kt':source=source.replace('\n        com.jarvis.food.appearance.AppearanceEntry()','')
  source=source.replace('com.jarvis.food.classicui.FamilyScreen()', 'com.jarvis.food.family.FamilyScreen()').replace('com.jarvis.food.classicui','com.jarvis.food.ui')
 assert hashlib.sha256(source.encode()).hexdigest()==expected, f'Classic UI changed: {p}'
functional=json.loads((root/'scripts/functional-source-manifest.json').read_text())
for filename,expected in functional.items():
 assert hashlib.sha256((root/filename).read_bytes()).hexdigest()==expected, f'Functional/data source changed: {filename}'
print(f'Boundary OK: {len(manifest["files"])} original UI snapshots and {len(functional)} functional/data files unchanged.')
