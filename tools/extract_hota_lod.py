#!/usr/bin/env python3
"""Extract every entry from a HotA.lod into a directory.

Handles both formats:
- Classic LOD (HotA <= 1.7.x) — entries have real filenames.
- HotA 1.8 encrypted LOD (key byte 135) — entries have 16-byte hashed names
  stored hex-encoded; output filenames are the hex string.

Usage:
    python3 extract_hota_lod.py <input.lod> <output_dir>
"""
import lzma
import os
import struct
import sys
import zlib

from homm3data import lodfile


def _xor(data, key):
    return bytes(b ^ key[i % len(key)] for i, b in enumerate(data))


def _lzma_dec(data):
    filters = [{
        "id": lzma.FILTER_LZMA1, "dict_size": 262144,
        "lc": 3, "lp": 0, "pb": 2,
    }]
    return lzma.LZMADecompressor(
        format=lzma.FORMAT_RAW, filters=filters
    ).decompress(data[1:])


def is_hota18(path):
    with open(path, "rb") as f:
        if f.read(4) != b"LOD\0":
            raise ValueError(f"not a LOD: {path}")
        f.seek(0x0C)
        return f.read(4)[0] == 135


def extract_classic(path, out_dir):
    written = 0
    with lodfile.open(path) as lod:
        for name in lod.get_filelist():
            data = lod.get_file(name)
            target = os.path.join(out_dir, name)
            os.makedirs(os.path.dirname(target) or out_dir, exist_ok=True)
            with open(target, "wb") as o:
                o.write(data)
            written += 1
    return written


def extract_hota18(path, out_dir):
    written = 0
    with open(path, "rb") as f:
        f.seek(8)
        total, = struct.unpack("<I", f.read(4))
        f.seek(0x0C)
        key = f.read(4)
        f.seek(80)
        entries = []
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
                    data = _lzma_dec(raw)
                elif csize and compression == 3:
                    data = zlib.decompress(raw)
                else:
                    data = raw
            except Exception as e:
                sys.stderr.write(f"  decompress failed for {hex_name}: {e}\n")
                continue
            target = os.path.join(out_dir, hex_name)
            with open(target, "wb") as o:
                o.write(data)
            written += 1
    return written


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <input.lod> <output_dir>")
        sys.exit(1)
    src, dst = sys.argv[1], sys.argv[2]
    os.makedirs(dst, exist_ok=True)
    if is_hota18(src):
        kind = "HotA 1.8 (encrypted, hashed names)"
        n = extract_hota18(src, dst)
    else:
        kind = "classic LOD"
        n = extract_classic(src, dst)
    print(f"{src} → {dst}  ({kind})  wrote {n} files")


if __name__ == "__main__":
    main()
