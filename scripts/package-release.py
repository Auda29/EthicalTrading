"""Package only the authored project and verified deliverables, never the execution workspace."""
import hashlib
import json
import shutil
import stat
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
version = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line)['version']
name = f'ethical-trading-{version}'
jar = root / 'build/libs' / (name + '.jar')
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    metadata = json.loads(archive.read('fabric.mod.json'))
    assert metadata['version'] == version and metadata['depends']['minecraft'] == '26.2'
    assert 'LICENSE' in archive.namelist(), 'Distribution must carry its license'
    assert 'dev/ethicaltrading/mixin/VillagerMixin.class' in archive.namelist()
    assert not any('/test/' in p or p.startswith('net/minecraft/') for p in archive.namelist())

required_root = [
    '.gitignore', 'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
    'README.md', 'CONFIGURATION.md', 'PLAN.md', 'TESTING.md', 'LICENSE', 'THIRD_PARTY.md'
]
files = [root / p for p in required_root]
for directory in ('src', 'gradle', 'scripts', 'licenses', 'evidence'):
    files.extend(p for p in (root / directory).rglob('*')
                 if p.is_file() and '__pycache__' not in p.parts and p.suffix != '.pyc')
assert all(p.is_file() and p.stat().st_size for p in files), 'Missing or empty required source/documentation'
assert (root / 'evidence/final-packaged-tests.log') in files, 'Test evidence is required'
assert len(files) == len(set(files)), 'No duplicate source entries'

output = root / 'dist'
output.mkdir(exist_ok=True)
shutil.copy2(jar, output / jar.name)
for doc in ('README.md', 'CONFIGURATION.md', 'TESTING.md'):
    shutil.copy2(root / doc, output / doc)
source = output / (name + '-project.zip')
with zipfile.ZipFile(source, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(files):
        relative = path.relative_to(root)
        info = zipfile.ZipInfo('ethical-trading/' + relative.as_posix(), date_time=(2026, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        mode = 0o755 if relative.as_posix() == 'gradlew' else 0o644
        info.external_attr = (stat.S_IFREG | mode) << 16
        archive.writestr(info, path.read_bytes())
with zipfile.ZipFile(source) as archive:
    assert archive.testzip() is None
    assert len(archive.namelist()) == len(files)
    assert all(not any(part in ('..', '.git', 'world', 'inspection', 'toolchains') for part in Path(p).parts)
               for p in archive.namelist())

artifacts = [output / jar.name, source, *(output / p for p in ('README.md', 'CONFIGURATION.md', 'TESTING.md'))]
manifest = {'mod_version': version, 'source_files': len(files), 'artifacts': []}
lines = []
for path in artifacts:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    manifest['artifacts'].append({'file': path.name, 'bytes': path.stat().st_size, 'sha256': digest})
    lines.append(f'{digest}  {path.name}')
(output / 'SHA256SUMS').write_text('\n'.join(lines) + '\n')
(output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(json.dumps(manifest, indent=2))
