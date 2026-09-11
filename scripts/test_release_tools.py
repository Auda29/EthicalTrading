"""Release safety tests; all fixtures are synthetic and perform no uploads."""
import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location('release_tools', Path(__file__).with_name('release_tools.py'))
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)

class ReleaseTests(unittest.TestCase):
    def test_version_is_literal_and_tag_must_match(self):
        self.assertEqual(r.metadata('0.1.0+mc26.2', 'beta')['tag'], 'v0.1.0+mc26.2')
        for invalid in [' 0.1.0+mc26.2', '0.1.0+mc26.2\n', '../../x', '0.1.0+mc1.21.11', '$(id)']:
            with self.assertRaises(ValueError): r.metadata(invalid, 'beta')
        with self.assertRaises(ValueError): r.metadata('0.1.0+mc26.2', 'stable')
        with self.assertRaises(ValueError): r.metadata('0.1.0+mc26.2', 'beta', 'v9.0.0')

    def test_checksum_filename_metadata_and_tampering(self):
        with tempfile.TemporaryDirectory() as directory:
            p = Path(directory); m = r.metadata('0.1.0+mc26.2', 'beta')
            jar = p / m['jar']
            with zipfile.ZipFile(jar, 'w') as z:
                z.writestr('fabric.mod.json', json.dumps({'id':'ethical_trading','version':m['version'],'depends':{'minecraft':'26.2'},'environment':'*'}))
            checksum = p / (m['jar'] + '.sha256')
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            checksum.write_text(f'{digest}  {jar.name}\n')
            self.assertEqual(r.verify_asset(p, m), digest)
            checksum.write_text(f'{digest}  ../{jar.name}\n')
            with self.assertRaises(ValueError): r.verify_asset(p, m)
            checksum.write_text(f'{"0"*64}  {jar.name}\n')
            with self.assertRaises(ValueError): r.verify_asset(p, m)

    def test_modrinth_readback_rejects_wrong_bytes_or_target(self):
        meta = r.metadata('0.1.0+mc26.2','beta')
        record = {'project_id':'ProjectFixture', 'version_number':meta['version'], 'version_type':'beta',
                  'game_versions':['26.2'], 'loaders':['fabric'],
                  'files':[{'primary':True,'filename':meta['jar'],'hashes':{'sha512':'a'*128}}],
                  'dependencies':[{'project_id':'FabricFixture','dependency_type':'required'}]}
        r.verify_modrinth_record(record, meta, 'a'*128, 'ProjectFixture', 'FabricFixture')
        with self.assertRaises(ValueError):
            r.verify_modrinth_record(record, meta, 'b'*128, 'ProjectFixture', 'FabricFixture')
        record['loaders']=['neoforge']
        with self.assertRaises(ValueError):
            r.verify_modrinth_record(record, meta, 'a'*128, 'ProjectFixture', 'FabricFixture')

    def test_curseforge_metadata_is_server_fabric_beta_only(self):
        m = r.curseforge_metadata(r.metadata('0.1.0+mc26.2','beta'), 'Real release notes')
        self.assertEqual(m['gameVersionNames'], ['26.2','Fabric','Server'])
        self.assertEqual(m['releaseType'],'beta')
        self.assertEqual(m['relations']['projects'], [{'slug':'fabric-api','type':'requiredDependency'}])
        self.assertFalse(m['isMarkedForManualRelease'])

if __name__ == '__main__': unittest.main()
