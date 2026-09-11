"""Small, strict helpers for immutable GitHub Release assets. No platform credentials here."""
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.compile(r'[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.]+)?\+mc26\.2')

def metadata(version, release_type, tag=None):
    if not VERSION.fullmatch(version) or release_type not in ('alpha', 'beta', 'release'):
        raise ValueError('Invalid literal version or release type')
    expected = 'v' + version
    if tag is not None and tag != expected:
        raise ValueError(f'Tag must exactly match {expected}')
    return dict(version=version, tag=expected, jar=f'ethical-trading-{version}.jar',
                minecraft='26.2', loader='fabric', release_type=release_type)

def local_metadata():
    properties = dict(line.split('=', 1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line)
    settings = json.loads((ROOT/'release.json').read_text())
    return metadata(properties['version'], settings['release_type'], os.environ.get('MANUAL_TAG') or None)

def verify_asset(directory, meta):
    directory = Path(directory)
    jar = directory / meta['jar']
    checksum = (directory/(meta['jar']+'.sha256')).read_text()
    match = re.fullmatch(r'([0-9a-f]{64})  ' + re.escape(meta['jar']) + r'\n?', checksum)
    if not match or not jar.is_file() or jar.is_symlink():
        raise ValueError('Checksum must name exactly the expected regular JAR, not a path or wildcard')
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    if digest != match[1]:
        raise ValueError('JAR checksum mismatch')
    with zipfile.ZipFile(jar) as archive:
        data = json.loads(archive.read('fabric.mod.json'))
        if (data['id'], data['version'], data['depends']['minecraft']) != ('ethical_trading', meta['version'], '26.2'):
            raise ValueError('JAR metadata does not match this release')
    return digest

def curseforge_metadata(meta, changelog):
    return {'changelog':changelog, 'changelogType':'markdown',
            'displayName':f"Ethical Trading {meta['version']} (Fabric)",
            'gameVersionNames':['26.2','Fabric','Server'], 'releaseType':meta['release_type'],
            'isMarkedForManualRelease':False,
            'relations':{'projects':[{'slug':'fabric-api','type':'requiredDependency'}]}}

def verify_modrinth_record(record, meta, sha512, project_id, fabric_id):
    correct = (record['project_id'] == project_id and record['version_number'] == meta['version']
               and record['version_type'] == meta['release_type'] and record['game_versions'] == ['26.2']
               and record['loaders'] == ['fabric'])
    primary = [f for f in record['files'] if f.get('primary')]
    correct = correct and len(primary) == 1 and primary[0]['filename'] == meta['jar'] and primary[0]['hashes']['sha512'] == sha512
    correct = correct and any(d.get('project_id') == fabric_id and d['dependency_type'] == 'required' for d in record['dependencies'])
    if not correct: raise ValueError('Modrinth read-back does not match the verified release')


def gh(*arguments):
    return subprocess.check_output(['gh', *arguments], text=True).strip()

def emit(values):
    text = ''.join(f'{key}={value}\n' for key,value in values.items())
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'],'a') as output: output.write(text)
    print(text, end='')

def main():
    action = sys.argv[1]
    meta = local_metadata()
    directory = ROOT/'dist'
    if action == 'info':
        if os.environ.get('GITHUB_REF_TYPE') == 'tag' and os.environ['GITHUB_REF_NAME'] != meta['tag']:
            raise ValueError('Push tag differs from the source version')
        emit(meta)
    elif action == 'checksum':
        directory.mkdir(exist_ok=True)
        jar = directory/meta['jar']
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        (directory/(meta['jar']+'.sha256')).write_text(f'{digest}  {meta["jar"]}\n')
        print(verify_asset(directory, meta))
    elif action == 'verify':
        print(verify_asset(directory, meta))
    elif action == 'download':
        repo = os.environ['GH_REPO']
        head = os.environ.get('WORKFLOW_HEAD_SHA')
        if head and gh('api',f'repos/{repo}/commits/{meta["tag"]}','--jq','.sha') != head:
            emit(dict(should_publish='false')); return
        release = json.loads(gh('release','view',meta['tag'],'--repo',repo,'--json','isDraft,tagName,isPrerelease,body,assets'))
        if release['isDraft'] or release['tagName'] != meta['tag']:
            raise ValueError('Only the exact published GitHub release can be uploaded')
        if release['isPrerelease'] != (meta['release_type'] != 'release'):
            raise ValueError('GitHub prerelease classification differs from release.json')
        if not release['body'].strip(): raise ValueError('Release notes must be present')
        names = {a['name'] for a in release['assets'] if a['size'] > 0}
        if not {meta['jar'],meta['jar']+'.sha256'}.issubset(names):
            raise ValueError('Required immutable release assets missing')
        directory.mkdir(exist_ok=True)
        gh('release','download',meta['tag'],'--repo',repo,'--pattern',meta['jar'],'--pattern',meta['jar']+'.sha256','--dir',str(directory))
        digest = verify_asset(directory, meta)
        (directory/'changelog.md').write_text(release['body'])
        (directory/'curseforge-metadata.json').write_text(json.dumps(curseforge_metadata(meta,release['body']),indent=2)+'\n')
        emit(dict(meta, should_publish='true', sha256=digest))
    else: raise ValueError('Unknown action')

if __name__ == '__main__': main()
