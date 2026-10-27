#!/usr/bin/env python3
"""Deliver only the pinned, successful APK; never rebuild or change app data."""
from pathlib import Path
import base64, glob, hashlib, io, json, re, subprocess, tarfile, xml.etree.ElementTree as ET, zipfile
from PIL import Image
REPO = "IMSLEPT/jarvis-food-android"
SOURCE = "fc02422ed2be4f7251518b5fe441a704b5eb1fb1"
RUN = 40576657959
NUMBER = 118
TAG = "preview-v1.0.118"
def command(args, **kw):
    return subprocess.run(args, check=True, **kw)
def api(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", endpoint]))
run = api(f"repos/{REPO}/actions/runs/{RUN}")
assert run["status"] == "completed" and run["conclusion"] == "success"
assert run["run_number"] == NUMBER and run["head_sha"] == SOURCE
items = api(f"repos/{REPO}/actions/runs/{RUN}/artifacts")["artifacts"]
artifact = next(a for a in items if a["name"] == f"Aurora-APK-and-QA-{NUMBER}")
assert not artifact["expired"] and artifact["workflow_run"]["head_sha"] == SOURCE
archive = Path("verified-source.zip")
with archive.open("wb") as target:
    command(["gh", "api", f"repos/{REPO}/actions/artifacts/{artifact['id']}/zip"], stdout=target)
digest = hashlib.file_digest(archive.open("rb"), "sha256").hexdigest()
assert artifact["digest"] == "sha256:" + digest
root = Path("verified-source"); root.mkdir()
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert all(not n.startswith("/") and ".." not in Path(n).parts for n in z.namelist())
    z.extractall(root)
unit = 0
for file in (root / "app/build/test-results").rglob("*.xml"):
    xml = ET.parse(file).getroot()
    assert int(xml.get("failures", 0)) == 0 and int(xml.get("errors", 0)) == 0
    unit += int(xml.get("tests", 0))
assert unit == 94
counts = {"layout-standard":2,"layout-large-text":2,"layout-buttons":1,"layout-buttons-large-text":1,"layout-gestures":1,"photo-ai":3,"layout-features-standard":2,"layout-features-large-text":2,"layout-options-standard":3,"layout-options-large-text":3,"layout-profile-standard":2,"layout-profile-large-text":2,"layout-profile-android10":1,"regression":3,"emu":1,"layout-android10-buttons":1}
for name, count in counts.items():
    assert f"OK ({count} {'test' if count == 1 else 'tests'})" in (root / f"{name}.log").read_text(), name
for name in ["crash.txt", "layout-crash.txt"]:
    assert "FATAL EXCEPTION" not in (root / name).read_text()
apk = root / f"dist/JarvisFood-v1.0.{NUMBER}.apk"
assert apk.is_file()
with zipfile.ZipFile(apk) as z:
    assert z.testzip() is None
    assert z.getinfo("assets/food.tflite").file_size == 21151551
    expected = subprocess.check_output(["git", "show", SOURCE + ":app/src/main/assets/body-guide/index.html"])
    assert z.read("assets/body-guide/index.html") == expected
signers = glob.glob("/usr/local/lib/android/sdk/build-tools/*/apksigner")
assert signers
signer = sorted(signers, key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p)))[-1]
proof = subprocess.check_output([signer, "verify", "--verbose", "--print-certs", str(apk)], text=True)
cert = re.search(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-f]+)", proof).group(1)
assert cert == "a039be1f4e694272f35ac964d46d28185535ce28b2dcfb5bbbedda264b4d80ca"
assert "Verified using v2 scheme (APK Signature Scheme v2): true" in proof
command(["python3", "scripts/verify-aurora-boundary.py"])
command(["python3", "tools/photography/verify.py"])
report = {"file_name":apk.name,"size":apk.stat().st_size,"sha256":hashlib.file_digest(apk.open("rb"), "sha256").hexdigest(),"signer_sha256":cert,"apk_v2_signature_verified":True,"unit_tests":unit,"android_executions":sum(counts.values()),"offline_model_bytes":21151551,"photo_mappings_verified":821,"source_sha":SOURCE,"source_run":RUN}
deliver = Path("delivery"); deliver.mkdir()
verification = deliver / "verification.json"; verification.write_text(json.dumps(report, indent=2) + "\n")
notes = deliver / "release-notes.md"
notes.write_text("Modello corporeo 3D ruotabile con zone di misura evidenziate, istruzioni e storico per data. Schermate del profilo e sostituzione alimento riorganizzate nello stile navy e arancione di Jarvis Food.\n\nAPK firmato con la chiave originale, installabile come aggiornamento. Verificati 94 test unitari e 30 esecuzioni Android su Android 10 e 15, incluso testo ingrandito e controllo dell'immagine 3D realmente visibile. Modello AI offline e foto delle ricette inclusi.\n")
assert subprocess.run(["gh", "release", "view", TAG, "--repo", REPO], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode != 0, "Release already exists; do not overwrite"
command(["gh", "release", "create", TAG, str(apk), str(verification), "--repo", REPO, "--target", SOURCE, "--title", f"Jarvis Food 1.0.{NUMBER}", "--notes-file", str(notes), "--prerelease", "--latest=false"])
release = api(f"repos/{REPO}/releases/tags/{TAG}")
assert release["prerelease"] and not release["draft"]
asset = next(a for a in release["assets"] if a["name"] == apk.name)
assert asset["state"] == "uploaded" and asset["size"] == report["size"]
if asset.get("digest"): assert asset["digest"] == "sha256:" + report["sha256"]
report.update(download_url=asset["browser_download_url"], release_url=release["html_url"])
print("DELIVERY_RESULT=" + json.dumps(report), flush=True)
shots = root / "screenshots"; shots.mkdir()
for name in ["layout-shots.tar", "shots.tar"]:
    with tarfile.open(root / name) as tar: tar.extractall(shots, filter="data")
for label, relative in [
    ("body-standard","profile-standard/body-3d-waist.png"),
    ("body-large","profile-large-text/body-3d-waist.png"),
    ("body-android10","profile-android10/body-3d-waist.png"),
    ("replacement","options-standard/replacement-overview.png")]:
    with Image.open(shots / "files/shots" / relative) as im:
        im = im.convert("RGB"); im.thumbnail((540,1170))
        out = io.BytesIO(); im.save(out, format="JPEG", quality=84)
        print("QA_IMAGE_" + label + "=" + base64.b64encode(out.getvalue()).decode(), flush=True)
