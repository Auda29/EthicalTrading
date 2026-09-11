"""Read-only Modrinth preflight/read-back. Never creates projects or uploads files."""
import hashlib
import json
import os
import re
import sys
import time
import urllib.request
from release_tools import ROOT, local_metadata, verify_asset, verify_modrinth_record

def get(path):
    headers={'User-Agent':'Auda29/EthicalTrading release-verification (github.com/Auda29/EthicalTrading)'}
    if os.environ.get('MODRINTH_TOKEN'): headers['Authorization']=os.environ['MODRINTH_TOKEN']
    request=urllib.request.Request('https://api.modrinth.com/v2/'+path,headers=headers)
    with urllib.request.urlopen(request,timeout=30) as response: return json.load(response)

def main():
    project=os.environ['MODRINTH_PROJECT_ID']
    if not re.fullmatch(r'[A-Za-z0-9_-]+',project): raise ValueError('Invalid project ID or slug')
    meta=local_metadata(); verify_asset(ROOT/'dist',meta)
    target=get('project/'+project)
    if target['title'] != 'Ethical Trading' or target['client_side'] != 'optional' or target['server_side'] != 'required':
        raise ValueError('Project must be Ethical Trading, client optional / server required')
    sha512=hashlib.sha512((ROOT/'dist'/meta['jar']).read_bytes()).hexdigest()
    mode=sys.argv[1]
    if mode not in ('preflight','verify'): raise ValueError('Unknown mode')
    for attempt in range(4 if mode=='verify' else 1):
        matches=[v for v in get('project/'+project+'/version') if v['version_number']==meta['version']]
        if mode=='preflight':
            if matches: raise ValueError('Version already exists; refusing a duplicate upload')
            print('Preflight passed; no existing version.'); return
        if len(matches)==1:
            fabric=get('project/fabric-api')['id']
            verify_modrinth_record(matches[0],meta,sha512,target['id'],fabric)
            print(json.dumps({'verified':True,'version_id':matches[0]['id'],
                              'url':'https://modrinth.com/mod/'+project+'/version/'+matches[0]['id'],
                              'sha512':sha512})); return
        if len(matches)>1: raise ValueError('Ambiguous duplicated Modrinth versions')
        if attempt<3: time.sleep(2**attempt)
    raise ValueError('Upload is not readable on Modrinth; do not blindly re-upload')

if __name__=='__main__': main()
