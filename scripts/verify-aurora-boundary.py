"""Preserve the original baseline; allow only explicitly recorded, user-requested fixes."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
functional=json.loads((root/'scripts/functional-source-manifest.json').read_text())
approved_path=root/'scripts/authorized-functional-changes.json'
approved=json.loads(approved_path.read_text()) if approved_path.exists() else {}
assert set(approved) <= set(functional), 'Authorized changes must refer to original baseline files'
changed=[]
for filename,expected in functional.items():
    actual=hashlib.sha256((root/filename).read_bytes()).hexdigest()
    if actual != expected:
        fix=approved.get(filename,{})
        assert fix.get('sha256')==actual and fix.get('reason'), f'Unapproved functional/data change: {filename}'
        changed.append(filename)
print(f'Boundary OK: {len(functional)-len(changed)} original functional/data files unchanged; {len(changed)} explicitly authorized fixes.')
