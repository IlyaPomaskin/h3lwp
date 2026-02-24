#!/usr/bin/env python3
"""Detect type, content, and internal names for ALL entries in a LOD file (classic or HotA 1.8)."""
import struct, sys, zlib, lzma

def xor_decrypt(data, key):
    return bytes(data[i] ^ key[i % len(key)] for i in range(len(data)))

def extract_lzma(data):
    filters = [{"id": lzma.FILTER_LZMA1, "dict_size": 262144, "lc": 3, "lp": 0, "pb": 2}]
    return lzma.LZMADecompressor(format=lzma.FORMAT_RAW, filters=filters).decompress(data[1:])

def read_fixed_string(data, offset, length):
    if offset + length > len(data): return None
    raw = data[offset:offset+length]
    s = raw.split(b'\0')[0].decode('ascii', errors='replace')
    return s if s and all(0x20 <= ord(c) <= 0x7E for c in s) else None

def read_all_frame_names(data, is_d32):
    """Read first frame name from each group."""
    names = []
    if is_d32:
        if len(data) < 48 + 13: return names
        gc = struct.unpack_from("<I", data, 16)[0]
        if gc <= 0 or gc > 256: return names
        pos = 32
        for g in range(gc):
            if pos + 16 > len(data): break
            _, fc, _, _ = struct.unpack_from("<IIII", data, pos)
            pos += 16
            if fc < 0 or fc > 10000: break
            if fc >= 1:
                name = read_fixed_string(data, pos, 13)
                if name: names.append(name)
            skip = fc * 17
            if skip < 0 or pos + skip > len(data): break
            pos += skip
    else:
        if len(data) < 800: return names
        fw = struct.unpack_from("<I", data, 4)[0]
        fh = struct.unpack_from("<I", data, 8)[0]
        if fw <= 0 or fw > 4096 or fh <= 0 or fh > 4096: return names
        gc = struct.unpack_from("<I", data, 12)[0]
        if gc <= 0 or gc > 256: return names
        pos = 784
        for g in range(gc):
            if pos + 16 > len(data): break
            _, fc = struct.unpack_from("<II", data, pos)
            pos += 16
            if fc < 0 or fc > 10000: break
            if fc >= 1:
                name = read_fixed_string(data, pos, 13)
                if name: names.append(name)
            skip = fc * 17
            if skip < 0 or pos + skip > len(data): break
            pos += skip
    return names

def detect_type(data):
    if len(data) < 4:
        return "TINY", f"{len(data)} bytes", []

    magic4 = data[:4]

    # D32 format
    if magic4 == b'\x44\x33\x32\x46':
        names = read_all_frame_names(data, True)
        if len(data) >= 32:
            w = struct.unpack_from("<I", data, 4)[0]
            h = struct.unpack_from("<I", data, 8)[0]
            gc = struct.unpack_from("<I", data, 16)[0]
            return "D32", f"{w}x{h}, {gc} groups", names
        return "D32", "", names

    # DEF format
    if len(data) >= 800:
        dtype = struct.unpack_from("<I", data, 0)[0]
        fw = struct.unpack_from("<I", data, 4)[0]
        fh = struct.unpack_from("<I", data, 8)[0]
        gc = struct.unpack_from("<I", data, 12)[0]
        if 0 < fw <= 4096 and 0 < fh <= 4096 and 0 < gc <= 256:
            names = read_all_frame_names(data, False)
            return "DEF", f"type={dtype}, {fw}x{fh}, {gc} groups", names

    # PCX
    if len(data) >= 12:
        fsize, w, h = struct.unpack_from("<III", data, 0)
        if 0 < w <= 4096 and 0 < h <= 4096:
            if fsize + 12 + 768 == len(data) and fsize == w * h:
                return "PCX8", f"{w}x{h}", []
            if fsize + 12 == len(data) and fsize == w * h * 3:
                return "PCX24", f"{w}x{h}", []

    # TEXT
    printable = sum(1 for b in data[:500] if 0x20 <= b <= 0x7E or b in (0x0A, 0x0D, 0x09))
    if len(data) > 0 and printable > len(data[:500]) * 0.9:
        preview = data[:120].decode('ascii', errors='replace').replace('\r\n', '\\n').replace('\n', '\\n')
        return "TEXT", f"{len(data)} bytes", [preview]

    # WAV
    if magic4 == b'RIFF':
        return "WAV", f"{len(data)} bytes", []

    # FNT
    if magic4 == b'\x46\x4e\x54\x00':
        return "FNT", f"{len(data)} bytes", []

    # BMP
    if data[:2] == b'BM':
        return "BMP", f"{len(data)} bytes", []

    # MSK-like small binary
    if len(data) <= 20:
        return "BIN", f"{len(data)} bytes, hex={data.hex()}", []

    return "UNK", f"{len(data)} bytes, magic={magic4.hex()}", []

def read_classic_string(data, offset, length):
    raw = data[offset:offset+length]
    return raw.split(b'\0')[0].decode('ascii', errors='replace')

def detect_lod_format(f):
    """Read header and return (is_hota18, total_count, key_bytes)."""
    f.seek(0)
    magic = f.read(8)
    if magic[:4] != b'LOD\x00':
        raise ValueError(f"Not a LOD file (magic: {magic[:4].hex()})")
    total, = struct.unpack("<I", f.read(4))
    key = f.read(4)
    is_hota18 = (key[0] & 0xFF) == 135
    return is_hota18, total, key

def read_hota18_entries(f, total, key):
    entries = []
    f.seek(80)
    for i in range(total):
        raw_name = f.read(16)
        hex_name = raw_name.hex()
        encrypted = f.read(16)
        decrypted = xor_decrypt(encrypted, key)
        offset, size, csize = struct.unpack("<III", decrypted[:12])
        compression = encrypted[12]
        entries.append((hex_name, offset, size, csize, compression))
    return entries

def read_classic_entries(f, total):
    """Classic LOD: entries start at offset 92, each is 32 bytes (16 name + 4 offset + 4 size + 4 type + 4 csize)."""
    entries = []
    f.seek(92)
    for i in range(total):
        raw = f.read(32)
        name = raw[:16].split(b'\0')[0].decode('ascii', errors='replace')
        offset, size, ftype, csize = struct.unpack_from("<IIII", raw, 16)
        # Classic LOD uses zlib compression
        entries.append((name, offset, size, csize, 3))
    return entries

def decompress_entry(f, offset, size, csize, compression):
    f.seek(offset)
    if csize != 0:
        raw = f.read(csize)
        if compression == 2:
            return extract_lzma(raw)
        elif compression == 3:
            return zlib.decompress(raw)
        else:
            return raw
    else:
        return f.read(size)

def process_lod(lod_path):
    with open(lod_path, "rb") as f:
        is_hota18, total, key = detect_lod_format(f)
        fmt = "HotA 1.8" if is_hota18 else "Classic"

        if is_hota18:
            entries = read_hota18_entries(f, total, key)
        else:
            entries = read_classic_entries(f, total)

        print(f"\n=== {lod_path} ===")
        print(f"LOD format: {fmt}, total entries: {total}")
        print(f"{'ENTRY':<34} {'TYPE':<6} {'DETAILS':<40} NAMES")
        print("-" * 130)

        for name, offset, size, csize, compression in entries:
            try:
                data = decompress_entry(f, offset, size, csize, compression)
            except Exception as e:
                print(f"{name:<34} ERR    decompress failed: {e}")
                continue

            ftype, details, names = detect_type(data)
            names_str = ", ".join(names) if names else ""
            print(f"{name:<34} {ftype:<6} {details:<40} {names_str}")

def main():
    import os, glob

    if len(sys.argv) >= 3 and sys.argv[1] == "-f":
        folder = sys.argv[2]
        lod_files = sorted(glob.glob(os.path.join(folder, "*.lod")) + glob.glob(os.path.join(folder, "*.LOD")))
        if not lod_files:
            print(f"No .lod files found in {folder}")
            return
        for path in lod_files:
            try:
                process_lod(path)
            except Exception as e:
                print(f"\n=== {path} ===")
                print(f"ERROR: {e}")
    else:
        lod_path = sys.argv[1] if len(sys.argv) > 1 else "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"
        process_lod(lod_path)

if __name__ == "__main__":
    main()
