#!/usr/bin/env python3
"""List adventure-map and terrain DEF entries in LOD files.

Adventure map objects are DEFs of FileType.MAP (0x43); terrain tiles are
FileType.TERRAIN (0x45). MAP_HERO (0x44) is also included since heroes appear
on the adventure map.

Supports classic LOD (e.g. H3sprite.lod) and HotA 1.8 encrypted LOD
(e.g. HotA.lod, where filenames are MD5 hashes — the first DEF frame name
is used as the human-readable label).

Usage:
    python3 list_advmap_terrain.py <file.lod> [<file.lod> ...]
"""
import lzma
import struct
import sys
import warnings
import zlib
from collections import defaultdict
from io import BytesIO

from homm3data import deffile, lodfile

warnings.filterwarnings("ignore")

TARGET_TYPES = {
    deffile.DefFile.FileType.MAP,
    deffile.DefFile.FileType.MAP_HERO,
    deffile.DefFile.FileType.TERRAIN,
}


def classify(data):
    if len(data) < 16:
        return None, None, None
    try:
        magic = struct.unpack_from("<I", data, 0)[0]
        if magic == 0x46323344:
            return "D32", None, None
        with deffile.open(BytesIO(data)) as d:
            t = d.get_type()
            w, h = d.get_size()
            groups = d.get_groups()
            n_frames = sum(d.get_frame_count(g) for g in groups)
            # First frame name → friendly label (strip frame index suffix)
            first_name = None
            for g in groups:
                names = d.get_image_name(g) if False else None
                # Pull from raw data — DefFile exposes per-group via internals
                break
            try:
                # get_image_name(group, index)
                first_name = d.get_image_name(groups[0], 0)
            except Exception:
                first_name = None
            return t, (w, h, len(groups), n_frames), first_name
    except Exception:
        return None, None, None


def _iter_classic(path):
    with lodfile.open(path) as lod:
        for name in lod.get_filelist():
            if name.lower().endswith(".def"):
                yield name, lod.get_file(name)


def _xor(data, key):
    return bytes(b ^ key[i % len(key)] for i, b in enumerate(data))


def _lzma_decode(data):
    filters = [{
        "id": lzma.FILTER_LZMA1,
        "dict_size": 262144,
        "lc": 3, "lp": 0, "pb": 2,
    }]
    return lzma.LZMADecompressor(
        format=lzma.FORMAT_RAW, filters=filters
    ).decompress(data[1:])


def _is_hota18(path):
    with open(path, "rb") as f:
        if f.read(4) != b"LOD\0":
            return False
        f.seek(0x0C)
        key = f.read(4)
        return key[0] == 135


def _iter_hota18(path):
    """Yield (hex_hash, data) for every DEF-shaped entry."""
    with open(path, "rb") as f:
        f.seek(8)
        total, = struct.unpack("<I", f.read(4))
        f.seek(0x0C)
        key = f.read(4)
        entries = []
        f.seek(80)
        for _ in range(total):
            raw_name = f.read(16)
            encrypted = f.read(16)
            decrypted = _xor(encrypted, key)
            offset, size, csize = struct.unpack("<III", decrypted[:12])
            compression = encrypted[12]
            entries.append((raw_name.hex(), offset, size, csize, compression))
        for hex_name, offset, size, csize, compression in entries:
            f.seek(offset)
            raw = f.read(csize) if csize else f.read(size)
            try:
                if csize and compression == 2:
                    data = _lzma_decode(raw)
                elif csize and compression == 3:
                    data = zlib.decompress(raw)
                else:
                    data = raw
            except Exception:
                continue
            # Heuristic DEF check: first 4 bytes is a known type
            if len(data) < 16:
                continue
            magic = struct.unpack_from("<I", data, 0)[0]
            if magic in (0x40, 0x41, 0x42, 0x43, 0x44, 0x45,
                         0x46, 0x47, 0x48, 0x49) or magic == 0x46323344:
                yield hex_name, data


def process(path):
    buckets = defaultdict(list)
    if _is_hota18(path):
        iterator = _iter_hota18(path)
    else:
        iterator = _iter_classic(path)

    for name, data in iterator:
        t, info, label = classify(data)
        if t in TARGET_TYPES:
            buckets[t].append((name, info, label))
    return buckets


def print_bucket(label, entries):
    print(f"\n{'='*78}")
    print(f" {label} — {len(entries)} entries")
    print(f"{'='*78}")
    rows = sorted(entries, key=lambda x: (x[2] or "").lower() or x[0].lower())
    for name, info, friendly in rows:
        w, h, g, f = info
        nm = friendly or name
        print(f"  {nm:<24}  {w:>3}x{h:<3}  groups={g:<3} frames={f:<4}  [{name}]")


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <file.lod> [<file.lod> ...]")
        sys.exit(1)

    for path in sys.argv[1:]:
        print(f"\n########## {path} ##########")
        buckets = process(path)
        for t in (
            deffile.DefFile.FileType.TERRAIN,
            deffile.DefFile.FileType.MAP,
            deffile.DefFile.FileType.MAP_HERO,
        ):
            print_bucket(t.name, buckets.get(t, []))

        total = sum(len(v) for v in buckets.values())
        print(f"\n  Totals for {path}:")
        for t in (
            deffile.DefFile.FileType.TERRAIN,
            deffile.DefFile.FileType.MAP,
            deffile.DefFile.FileType.MAP_HERO,
        ):
            print(f"    {t.name}: {len(buckets.get(t, []))}")
        print(f"    GRAND TOTAL: {total}")


if __name__ == "__main__":
    main()
