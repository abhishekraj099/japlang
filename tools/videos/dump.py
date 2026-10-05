import json, sys
for vid in sys.argv[1:]:
    d = json.load(open(f'subs/{vid}.ja.json3', encoding='utf-8'))
    print('=====', vid)
    n = 0
    for e in d.get('events', []):
        if 'segs' not in e: continue
        t = ''.join(s.get('utf8', '') for s in e['segs']).replace('\n', ' ').strip()
        if not t: continue
        print(f"{e['tStartMs']/1000:7.2f} {e.get('dDurationMs',0)/1000:5.2f}  {t}")
        n += 1
        if n > int(sys.argv[0] and 60): break
