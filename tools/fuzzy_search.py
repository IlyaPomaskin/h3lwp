#!/usr/bin/env python3
"""Fuzzy search for missing DEF names in hota18_by_type.txt frame names."""
import re
from difflib import get_close_matches

missing = [
    'avcgarw1', 'avgarha', 'avgself', 'avgyeti',
    'avlklp50', 'avlklp60', 'avlklp70', 'avlklp80',
    'avmcrsn1', 'avxl1sh0w', 'avxl4sh0', 'avxptw_3',
    'avxwgtbr', 'avxwgtrd', 'avxwgtwh', 'mntswpgl', 'sanct_wt'
]

# Parse DEF entries and their frames from hota18_by_type.txt
defs = {}  # hash -> {info, frames}
frames = set()
current_hash = None
in_def = False

with open('tools/hota18_by_type.txt') as f:
    for line in f:
        if re.match(r'^=+$', line.strip()):
            continue
        if re.match(r'^ (DEF|D32) ', line):
            in_def = True
            continue
        if re.match(r'^ (PCX|P32|TXT|WAV|REF|UNKNOWN) ', line):
            in_def = False
            current_hash = None
            continue

        if not in_def:
            continue

        m = re.match(r'^  ([0-9a-f]{32}) (.+)$', line)
        if m:
            current_hash = m.group(1)
            defs[current_hash] = {'info': line.strip(), 'frames': []}
            continue

        if line.startswith('    ') and current_hash:
            fname = line.strip()
            defs[current_hash]['frames'].append(fname)
            frames.add(fname.lower())

# Build cleaned frame base set
cleaned_frames = set()
for f in frames:
    c = re.sub(r'[Aa]\d{3}.*$', '', f)
    dot = c.find('.')
    if dot > 0:
        c = c[:dot]
    if c:
        cleaned_frames.add(c)

print(f'Total unique frames: {len(frames)}')
print(f'Total cleaned bases: {len(cleaned_frames)}')
print()

for name in missing:
    print(f'--- {name}.def ---')
    found_anything = False

    # 1. Substring matches
    substr = sorted([c for c in cleaned_frames if name in c or c in name])
    if substr:
        print(f'  Substring: {substr[:10]}')
        found_anything = True

    # 2. Fuzzy match (high cutoff)
    close_hi = get_close_matches(name, list(cleaned_frames), n=5, cutoff=0.6)
    if close_hi:
        print(f'  Fuzzy >=0.6: {close_hi}')
        found_anything = True

    # 3. Fuzzy match (lower cutoff)
    close_lo = get_close_matches(name, list(cleaned_frames), n=8, cutoff=0.5)
    extra = [c for c in close_lo if c not in (close_hi or [])]
    if extra:
        print(f'  Fuzzy >=0.5: {extra}')
        found_anything = True

    # 4. Strip known prefix and search core
    prefixes = ('avl', 'avg', 'avw', 'avm', 'avx', 'ava', 'avc', 'avs', '4lvl')
    core = name
    for p in prefixes:
        if name.startswith(p):
            core = name[len(p):]
            break
    if core != name and len(core) >= 3:
        core_matches = sorted([c for c in cleaned_frames if core in c])[:8]
        if core_matches:
            print(f'  Core "{core}" in: {core_matches}')
            found_anything = True

    # 5. Show which DEF entry contains matched frames
    all_matches = set((close_hi or []) + (close_lo or []) + substr)
    for match in sorted(all_matches)[:5]:
        for h, info in defs.items():
            frame_bases = set()
            for fname in info['frames']:
                c = re.sub(r'[Aa]\d{3}.*$', '', fname.lower())
                dot = c.find('.')
                if dot > 0:
                    c = c[:dot]
                frame_bases.add(c)
            if match in frame_bases:
                print(f'    -> "{match}" found in {h[:16]}...  {info["info"][:90]}')
                break

    if not found_anything:
        print(f'  NO MATCHES')
    print()
