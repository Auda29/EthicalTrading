"""Validate real test logs against the current JAR, then collect release evidence."""
import argparse
import datetime
import hashlib
import json
import re
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--build-log', type=Path, required=True)
parser.add_argument('--server-log', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
jar = root / 'build/libs/ethical-trading-0.1.0+mc26.2.jar'
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
server_log = args.server_log.read_text()
build_log = args.build_log.read_text()
assert 'BUILD SUCCESSFUL' in build_log, 'Build did not succeed'
assert f'SHA256={digest}' in server_log, 'GameTests did not test this exact JAR'
assert re.findall(r'ACTUAL SERVER PROCESS: (\w+)', server_log) == ['write', 'read']
suite_totals = [int(n) for n in re.findall(r'All (\d+) required tests passed', server_log)]
own_test_count = sum(len(re.findall(r'@GameTest\b', f.read_text()))
                     for f in (root / 'src/gametest/java').rglob('*.java'))
assert own_test_count == 9 and suite_totals == [own_test_count + 1] * 2, suite_totals
assert not re.search(r'required tests failed| failed at |Exception in thread', server_log), 'Server test failure in log'
measurements = [tuple(map(int, match)) for match in re.findall(
    r'\[ETHICAL-NIGHTS\] night=(\d+) clock=(\d+) loadedGameTicks=(\d+) debt=(\d+) capacity=(\d+)', server_log)]
expected = [(night, night * 24000 + 10, night * 24000, min(72000, night * 12010), cap)
            for night, cap in enumerate([10, 8, 6, 4, 2, 0], 1)]
assert measurements == expected * 2, 'Missing or unexpected real-night measurements'
written = re.findall(r'retaining fixture for restart ([0-9a-f-]{36})', server_log)
read = re.findall(r'SECOND JVM read original villager ([0-9a-f-]{36})', server_log)
assert len(written) == 1 and written == read, 'Restart did not read the original UUID'
assert '[ETHICAL-PERSISTENCE] Original entity unloaded' in server_log

xml_files = sorted((root / 'build/test-results/test').glob('TEST-*.xml'))
units = [ET.parse(p).getroot().attrib for p in xml_files]
unit_count = sum(int(row['tests']) for row in units)
assert unit_count == 10
assert all(int(row.get(key, 0)) == 0 for row in units for key in ['errors', 'failures', 'skipped'])
output = root / 'evidence'
(output / 'unit').mkdir(parents=True, exist_ok=True)
shutil.copyfile(args.build_log, output / 'final-build.log')
shutil.copyfile(args.server_log, output / 'final-packaged-tests.log')
for path in xml_files:
    shutil.copyfile(path, output / 'unit' / path.name)
summary = {
    'verified_at_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'jar': jar.name, 'jar_sha256': digest,
    'build_successful': True, 'unit_tests': unit_count, 'unit_failures': 0,
    'unit_suites': units,
    'own_minecraft_server_tests': own_test_count,
    'fabric_baseline_tests_per_process': 1,
    'server_processes': len(suite_totals), 'passed_per_process': suite_totals,
    'real_night_measurements_per_process': [dict(zip(['night', 'clock', 'game_ticks', 'debt', 'capacity'], row))
                                           for row in expected],
    'restart_same_uuid_verified': read[0],
    'manual_graphical_client_tests_performed': False,
    'large_hall_benchmark_performed': False,
}
(output / 'verification.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
