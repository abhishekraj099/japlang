"""Builds app/src/main/assets/videos.json from clips.py: word groups with furigana, romaji and kana."""
import json, re, sys
from fugashi import Tagger
from clips import CLIPS
from clips_more import SHORTS, MORE

from clips_jlpt import JLPT_CLIPS, JLPT, DEFAULT_JLPT

CLIPS = CLIPS + MORE + JLPT_CLIPS + SHORTS

OUT = 'D:/jap2/app/src/main/assets/videos.json'
META = json.load(open('meta.json', encoding='utf-8'))
tagger = Tagger()

KANJI = re.compile(r'[\u4e00-\u9fff々〆ヶ]')
PUNCT = set('、。，．？！?!…・「」『』（）()～〜⸺—―-　 ')
ATTACH = {'助詞', '助動詞', '接尾辞'}

def hira(s):
    return ''.join(chr(ord(c) - 0x60) if 'ァ' <= c <= 'ヶ' else c for c in s)

ROMA = {}
base = dict(zip('あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんがぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽ',
    'a i u e o ka ki ku ke ko sa shi su se so ta chi tsu te to na ni nu ne no ha hi fu he ho ma mi mu me mo ya yu yo ra ri ru re ro wa o n ga gi gu ge go za ji zu ze zo da ji zu de do ba bi bu be bo pa pi pu pe po'.split()))
ROMA.update(base)
ROMA.update({'Ⓦ': 'wa', 'Ⓔ': 'e', 'Ⓞ': 'o', ' ': ' ', 'ぁ': 'a', 'ぃ': 'i', 'ぅ': 'u', 'ぇ': 'e', 'ぉ': 'o', 'ゔ': 'vu'})
for k, r in {'き': 'ky', 'ぎ': 'gy', 'し': 'sh', 'じ': 'j', 'ち': 'ch', 'ぢ': 'j', 'に': 'ny', 'ひ': 'hy', 'び': 'by', 'ぴ': 'py', 'み': 'my', 'り': 'ry'}.items():
    for small, v in zip('ゃゅょ', 'auo'):
        ROMA[k + small] = r + v
ROMA.update({'てぃ': 'ti', 'でぃ': 'di', 'ふぁ': 'fa', 'ふぃ': 'fi', 'ふぇ': 'fe', 'ふぉ': 'fo', 'しぇ': 'she', 'じぇ': 'je', 'ちぇ': 'che', 'うぃ': 'wi', 'うぇ': 'we', 'つぁ': 'tsa'})

def romaji(kana):
    s, out, i = hira(kana), '', 0
    while i < len(s):
        two, one = s[i:i + 2], s[i]
        if one == 'っ':
            nxt = ROMA.get(s[i + 1:i + 3]) or ROMA.get(s[i + 1:i + 2]) or ''
            out += nxt[:1] if nxt else ''
            i += 1
        elif one == 'ー':
            out += out[-1] if out and out[-1] in 'aeiou' else ''
            i += 1
        elif two in ROMA:
            out += ROMA[two]; i += 2
        elif one in ROMA:
            out += ROMA[one]; i += 1
        else:
            out += {'、': ',', '。': '.', '？': '?', '！': '!', '…': '…', '～': '~', '〜': '~'}.get(one, one if one.isascii() else '')
            i += 1
    return out

def ruby(surface, reading):
    """Splits a word into [text, furigana] pieces so okurigana carry no furigana."""
    if not (KANJI.search(surface) or re.search(r'[0-9０-９]', surface)) or not reading:
        return [[surface, '']]
    parts = re.findall(r'[\u4e00-\u9fff々〆ヶ0-9０-９]+|[^\u4e00-\u9fff々〆ヶ0-9０-９]+', surface)
    pattern = ''.join('(.+?)' if KANJI.search(p) or p[0].isdigit() else '(' + re.escape(hira(p)) + ')' for p in parts)
    m = re.fullmatch(pattern, hira(reading))
    if not m:
        return [[surface, hira(reading)]]
    return [[p, g if (KANJI.search(p) or p[0].isdigit()) else ''] for p, g in zip(parts, m.groups())]

FIX = {'私': 'ワタシ', '日本': 'ニホン', '米': 'コメ'}
JOINERS = {'動詞', '形容詞', '助動詞'}
PARTICLE = {'は': 'Ⓦ', 'へ': 'Ⓔ', 'を': 'Ⓞ'}

# Words MeCab splits and misreads, fixed wherever they appear outside {…} overrides.
WHOLE = {'日本人': 'にほんじん'}

def tokens_of(text):
    for w, r in WHOLE.items():
        text = re.sub(r'(?<!\{)' + w, '{' + w + '|' + r + '}', text)
    out = []  # (surface, kana, pos, attach)
    for piece in re.split(r'(\{[^}]+\})', text):
        if not piece:
            continue
        if piece.startswith('{'):
            bits = piece[1:-1].split('|')
            out.append((bits[0], bits[1], '名詞', len(bits) > 2))
            continue
        for w in tagger(piece):
            k = w.feature.kana
            if k is None or k == '*':
                k = '' if KANJI.search(w.surface) else w.surface
                if KANJI.search(w.surface):
                    print('  !! no reading for', w.surface, 'in', text, file=sys.stderr)
            k = FIX.get(w.surface, k)
            out.append((w.surface, k, w.feature.pos1, False))
    return out

def words(text):
    """Display groups (a word plus its particles and endings), kana for scoring, and spaced romaji."""
    toks = [t for t in tokens_of(text) if t[0].strip()]
    groups, rom, prev_pos, prefix = [], '', None, False
    for s, k, pos, attach in toks:
        punct = all(c in PUNCT for c in s)
        joined = bool(groups) and (prefix or attach or punct or pos in ('助詞', '助動詞', '接尾辞'))
        if not joined:
            groups.append({'w': [], 'k': ''})
        g = groups[-1]
        g['w'] += [[s, '']] if punct else ruby(s, k)
        if not punct:
            g['k'] += hira(k)
        # Romaji: endings stick to the verb or adjective they follow; particles stand alone.
        glue = prefix or attach or punct or pos == '接尾辞' or (
            pos == '助動詞' and prev_pos in JOINERS and not (s == 'です' and prev_pos == '形容詞')) or (
            pos == '助詞' and s in ('て', 'で', 'ば', 'ちゃ', 'たり') and prev_pos in JOINERS)
        piece = PARTICLE[s] if pos == '助詞' and s in PARTICLE else (s if punct else hira(k))
        rom += ('' if glue or not rom else ' ') + piece
        prev_pos, prefix = pos, pos == '接頭辞'
    for g in groups:
        merged = []
        for t, r in g['w']:
            if merged and not r and not merged[-1][1]:
                merged[-1][0] += t
            else:
                merged.append([t, r])
        g['w'] = merged
    r = romaji(rom)
    r = re.sub(r'\bkonnichiha\b', 'konnichiwa', r)
    r = r.replace('konbanha', 'konbanwa')
    for a, c in ((r'\bnan de mo\b', 'nandemo'), (r'\bnan de\b', 'nande'), (r'\bnan te\b', 'nante'), (r'^de mo\b', 'demo')):
        r = re.sub(a, c, r)
    r = re.sub(r'\s+([,.!?…~])', r'\1', r)
    r = re.sub(r'\s+', ' ', r).strip()
    return [g['w'] for g in groups], ''.join(g['k'] for g in groups), r

topics = [
    {"id": "anime", "name": "Anime", "icon": "🎎"},
    {"id": "daily", "name": "Daily life", "icon": "☀️"},
    {"id": "conversation", "name": "Conversation", "icon": "💬"},
    {"id": "education", "name": "Education", "icon": "📚"},
    {"id": "food", "name": "Food & Cooking", "icon": "🍙"},
    {"id": "travel", "name": "Travel", "icon": "🗾"},
    {"id": "podcast", "name": "Podcast", "icon": "🎙️"},
]
videos = []
for c in CLIPS:
    meta = META[c['youtube']]
    lines = []
    for t, d, ja, en in c['lines']:
        w, kana, rom = words(ja)
        lines.append({"t": t, "d": d, "w": w, "kana": kana, "romaji": rom, "en": en})
        print(f"{t:7.2f} {''.join(p[0] for g in w for p in g)} | {' '.join(''.join(p[0] + ('[' + p[1] + ']' if p[1] else '') for p in g) for g in w)} | {rom}")
    videos.append({
        "id": c['id'], "youtube": c['youtube'], "title": c['title'], "channel": meta['channel'],
        "source": meta['title'], "topic": c['topic'], "level": c['level'],
        "start": c['start'], "end": c['end'], "short": bool(c.get('short')),
        "jlpt": c.get('jlpt') or JLPT.get(c['id']) or DEFAULT_JLPT[c['level']], "lines": lines,
    })

data = {
    "topics": topics,
    "levels": ["Easy", "Medium", "Hard"],
    "jlpt": ["N5", "N4", "N3", "N2", "N1"],
    "sections": [{"title": "For beginners", "level": "Easy"}, {"title": "Shorts", "short": True}]
        + [{"title": "JLPT " + n, "jlpt": n} for n in ["N5", "N4", "N3", "N2", "N1"]]
        + [{"title": t["name"], "topic": t["id"]} for t in topics],
    "recommended": ["howl-breakfast", "cafe-order", "lost-umbrella", "sick-visit", "first-day", "shrine-thanks", "yokohama"],
    "reportReasons": ["Subtitle", "Audio", "Translation", "Pronunciation guide", "Other"],
    "videos": videos,
}
json.dump(data, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
print(len(videos), 'videos,', sum(len(v['lines']) for v in videos), 'lines')
