"""Fail when an original functional or data asset changes during this UI-only redesign."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
functional=json.loads((root/'scripts/functional-source-manifest.json').read_text())
for filename,expected in functional.items():
    assert hashlib.sha256((root/filename).read_bytes()).hexdigest()==expected, f'Functional/data source changed: {filename}'
print(f'Boundary OK: {len(functional)} original functional/data files unchanged.')
