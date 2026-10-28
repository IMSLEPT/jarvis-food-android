import importlib.util
import io
from pathlib import Path
import tarfile
import unittest

spec = importlib.util.spec_from_file_location('qa_adb', Path(__file__).with_name('qa-adb.py'))
qa = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qa)


class TransportEvidenceTest(unittest.TestCase):
    def test_only_transport_failure_is_retryable(self):
        self.assertTrue(qa.transport_failed(1, b'', b'adb: device offline'))
        self.assertTrue(qa.transport_failed(1, b'error: device not found', b''))
        self.assertFalse(qa.transport_failed(0, b'FAILURES!!! AssertionError: clipped text', b''))
        self.assertFalse(qa.transport_failed(1, b'', b'INSTRUMENTATION_FAILED: app crashed'))

    def test_truncated_binary_evidence_is_rejected(self):
        data = io.BytesIO()
        with tarfile.open(fileobj=data, mode='w') as archive:
            member = tarfile.TarInfo('files/shots/home.png')
            member.size = 4
            archive.addfile(member, io.BytesIO(b'test'))
        args = ['exec-out', 'run-as', 'com.jarvis.food', 'tar', 'cf', '-', 'files/shots']
        self.assertTrue(qa.valid_capture(args, data.getvalue()))
        self.assertFalse(qa.valid_capture(args, data.getvalue()[:600]))
        self.assertFalse(qa.valid_capture(args, b'adb: device offline'))

    def test_normal_test_logs_are_preserved(self):
        self.assertTrue(qa.valid_capture(['shell', 'am', 'instrument'], b'FAILURES!!!'))


if __name__ == '__main__':
    unittest.main()
