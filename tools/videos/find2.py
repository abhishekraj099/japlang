import sys, yt_dlp
seen = {}
flat = yt_dlp.YoutubeDL({'quiet': True, 'skip_download': True, 'extract_flat': True, 'no_warnings': True})
for q in sys.argv[1:]:
    for e in flat.extract_info(f"ytsearch15:{q}", download=False)['entries']:
        seen.setdefault(e['id'], e)
full = yt_dlp.YoutubeDL({'quiet': True, 'skip_download': True, 'no_warnings': True})
for vid in seen:
    try: info = full.extract_info(f"https://www.youtube.com/watch?v={vid}", download=False)
    except Exception as ex: continue
    subs = [k for k in (info.get('subtitles') or {}) if k.startswith('ja')]
    if not subs or not info.get('playable_in_embed'): continue
    print(vid, '|', info.get('duration'), '|', f"{info.get('width')}x{info.get('height')}", '|', info.get('channel'), '|', info['title'][:70])
