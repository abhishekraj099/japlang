import sys, json, yt_dlp
ids = sys.argv[1:]
opts = {'quiet': True, 'no_warnings': True, 'skip_download': True, 'writesubtitles': True, 'subtitleslangs': ['ja.*', 'ja'], 'subtitlesformat': 'json3', 'outtmpl': 'subs/%(id)s.%(ext)s'}
meta = {}
with yt_dlp.YoutubeDL(opts) as y:
    for i in ids:
        info = y.extract_info('https://www.youtube.com/watch?v=' + i, download=True)
        meta[i] = {'title': info['title'], 'channel': info.get('channel'), 'duration': info.get('duration'), 'w': info.get('width'), 'h': info.get('height'), 'subs': list((info.get('subtitles') or {}).keys())}
        print(i, meta[i])
json.dump(meta, open('meta.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
