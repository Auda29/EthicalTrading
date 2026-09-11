"""Run two Fabric GameTest JVMs; optionally test the packaged production JAR."""
import argparse
import hashlib
import json
import os
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--jar', type=Path, help='Replace main classes/resources with this built mod JAR')
options = parser.parse_args()
wrapper = str(root / ('gradlew.bat' if os.name == 'nt' else 'gradlew'))
subprocess.run([wrapper, '--stop'], cwd=root, check=True)
subprocess.run([wrapper, '--no-daemon', '-PexportGameTestCommand', 'runGameTest'], cwd=root, check=True)
data = json.loads((root/'build/gametest-launch.json').read_text())
Path(data['cwd']).mkdir(parents=True, exist_ok=True)
if options.jar:
    jar = options.jar.resolve(strict=True)
    command = data['command']
    index = command.index('-cp') + 1
    entries = command[index].split(os.pathsep)
    replaced = {str(root/'build/classes/java/main'), str(root/'build/resources/main')}
    assert replaced.issubset(set(entries)), 'Unexpected launch classpath; refusing an unverified replacement'
    command[index] = os.pathsep.join([str(jar)] + [p for p in entries if p not in replaced])
    assert not any(p in command[index].split(os.pathsep) for p in replaced)
    print('PACKAGED MOD: ' + str(jar) + ' SHA256=' + hashlib.sha256(jar.read_bytes()).hexdigest(), flush=True)
# Two separate server JVMs and the same on-disk world. No entity-object reuse between phases.
for phase in ('write', 'read'):
    command = [data['command'][0], '-DethicalTrading.restartPhase=' + phase, *data['command'][1:]]
    print('=== ACTUAL SERVER PROCESS: ' + phase + ' ===', flush=True)
    result = subprocess.run(command, cwd=data['cwd'])
    if result.returncode:
        raise SystemExit(result.returncode)
