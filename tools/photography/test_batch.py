"""Regression checks for preservation, incomplete batches and invalidated reviews.

Run: python -m unittest discover -s tools/photography -p 'test_batch.py'
All writes use a temporary fixture; no generation, network or app compilation.
"""
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image
import batch


class BatchSafetyTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        source = self.root / 'tools/photography'
        assets = self.root / 'app/src/main/assets'
        source.mkdir(parents=True)
        assets.mkdir(parents=True)
        (assets / 'photography').mkdir()
        for name in ('approved.json', 'README.md', 'PROGRESS.md'):
            shutil.copy2(batch.SOURCE / name, source / name)
        for name in ('recipes.json', 'foods.json'):
            shutil.copy2(batch.ASSETS / name, assets / name)
        index = self.root / 'EditorialPhotos.kt'
        shutil.copy2(batch.INDEX, index)
        for name, value in dict(ROOT=self.root, SOURCE=source, MANIFEST=source / 'approved.json',
                                INDEX=index, ASSETS=assets).items():
            p = patch.object(batch, name, value)
            p.start(); self.addCleanup(p.stop)
        self.directory = self.root / 'work'
        batch.create(self.directory, 1)
        self.state = batch.read(self.directory / 'batch.json')
        self.key = self.state['tasks'][0]['id']
        self.photo = self.root / 'candidate.png'
        Image.effect_noise((900, 900), 25).convert('RGB').save(self.photo)

    def register(self, key=None):
        batch.candidate(self.directory, key or self.key, self.photo, 'Actual test prompt')

    def approve(self, key=None):
        batch.review(self.directory, key or self.key, 'approve', 'test reviewer',
                     'Explicit fixture review, not a real photograph.', list(batch.CHECKS))

    def test_plan_covers_every_missing_slot_without_duplicate_group_requests(self):
        ids = {r['id'] for r in self.state['recipes']}
        missing = {b['id'] for b in batch.briefs()
                   if b['recipeId'] in ids and b['id'] not in self.state['baseline']}
        covered = {t['id'] for t in self.state['tasks']}
        covered.update(a for t in self.state['tasks'] for a in t['aliases'])
        covered.update(r['id'] for r in self.state['reuses'])
        self.assertEqual(missing, covered)
        self.assertEqual(len({t['group'] for t in self.state['tasks']}), len(self.state['tasks']))

    def test_cannot_overwrite_in_flight_batch(self):
        original = (self.directory / 'batch.json').read_bytes()
        with self.assertRaisesRegex(ValueError, 'already exists'):
            batch.create(self.directory, 1)
        self.assertEqual(original, (self.directory / 'batch.json').read_bytes())

    def test_new_candidate_invalidates_previous_review(self):
        self.register(); self.approve(); self.register()
        with self.assertRaisesRegex(ValueError, 'Unreviewed'):
            batch.reviewed_inputs(batch.read(self.directory / 'batch.json'))

    def test_changed_candidate_cannot_keep_approval(self):
        self.register(); self.approve()
        Image.new('RGB', (900, 900), 'blue').save(self.photo)
        with self.assertRaisesRegex(ValueError, 'no longer matches'):
            batch.reviewed_inputs(batch.read(self.directory / 'batch.json'))

    def test_missing_quality_check_blocks_approval(self):
        self.register()
        with self.assertRaisesRegex(ValueError, 'Explicit checks'):
            batch.review(self.directory, self.key, 'approve', 'reviewer', 'notes', ['ingredients'])

    def test_incomplete_batch_never_calls_packager(self):
        original = batch.MANIFEST.read_bytes()
        with patch.object(batch.subprocess, 'run') as run:
            with self.assertRaisesRegex(ValueError, 'Unreviewed'):
                batch.integrate(self.directory)
            run.assert_not_called()
        self.assertEqual(original, batch.MANIFEST.read_bytes())

    def test_new_remote_records_are_not_overwritten(self):
        records = batch.read(batch.MANIFEST)
        records['new-record-from-another-session'] = {'asset': 'photography/preserve.webp'}
        batch.write(batch.MANIFEST, records)
        original = batch.MANIFEST.read_bytes()
        with self.assertRaisesRegex(ValueError, 'Library changed'):
            batch.integrate(self.directory)
        self.assertEqual(original, batch.MANIFEST.read_bytes())

    def test_failed_verification_restores_mutated_metadata_and_removes_new_assets(self):
        for task in self.state['tasks']:
            self.register(task['id']); self.approve(task['id'])
        backups = {p: p.read_bytes() for p in (batch.MANIFEST, batch.INDEX,
                                             batch.SOURCE / 'README.md', batch.SOURCE / 'PROGRESS.md')}
        def fail_after_partial_write(*args, **kwargs):
            batch.MANIFEST.write_text('{}')
            batch.INDEX.write_text('partial write')
            (batch.ASSETS / 'photography/new.webp').write_bytes(b'partial image')
            raise subprocess.CalledProcessError(1, args[0])
        with patch.object(batch.subprocess, 'run', side_effect=fail_after_partial_write):
            with self.assertRaises(subprocess.CalledProcessError):
                batch.integrate(self.directory)
        self.assertTrue(all(p.read_bytes() == content for p, content in backups.items()))
        self.assertEqual([], list((batch.ASSETS / 'photography').iterdir()))
        self.assertFalse(batch.read(self.directory / 'batch.json')['integrated'])


if __name__ == '__main__':
    unittest.main()
