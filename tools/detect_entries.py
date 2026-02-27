#!/usr/bin/env python3
"""Detect type and content of specific HotA 1.8 LOD entries."""
import struct, sys, zlib, lzma

ENTRIES_TO_FIND = [
    "53044fae19cdc8effe85cc9cccb42d73",
    "53edb5a7e99e18889ecfe6fb8e2e5692",
    "73640d77c093d65f6dfd1338c4842169",
    "d73b937b76346b73445c897ddfc0c319",
    "a9e1749e40568de14eb64883d884aaf9",
    "bd3440d9a1da1e2f603dd31db1175c53",
    "902cafba22d4597cbbce273a5251fc42",
    "6c6d7b8898ce60cc5aa51d845b9a1e21",
    "2520ce9bb8a6157d86bc690c4401f829",
    "d39b21ff0c95021cbdfca6d83dbe8aea",
    "07c13e62797a4087afb51f79ae2afde0",
    "a2275bc6b2571f3c5f753fb737ce0fe7",
    "e1ab10e86adbb7628ea83fecba1c0eb6",
    "60d7cd446e5aac56a25475927d40e181",
    "8ffee8644af215bd735e210edc832287",
    "ba8516de32361ae16cf20fc1f3fd4e66",
    "da0bb405d21d0ae8796fe38c26336cc1",
    "441605f5d9fd5c0d27ae7a98a63b7d00",
    "c136a8945841907421774bb3951a0f83",
    "4e214daf700d1f319e75a7a05c8c33c6",
]

def xor_decrypt(data, key):
    return bytes(data[i] ^ key[i % len(key)] for i in range(len(data)))

def extract_lzma(data):
    filters = [{"id": lzma.FILTER_LZMA1, "dict_size": 262144, "lc": 3, "lp": 0, "pb": 2}]
    return lzma.LZMADecompressor(format=lzma.FORMAT_RAW, filters=filters).decompress(data[1:])

def read_frame_names(data, is_d32):
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
            if fc >= 1 and pos + 13 <= len(data):
                name = data[pos:pos+13].split(b'\0')[0].decode('ascii', errors='replace')
                names.append(name)
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
            if fc >= 1 and pos + 13 <= len(data):
                name = data[pos:pos+13].split(b'\0')[0].decode('ascii', errors='replace')
                names.append(name)
            skip = fc * 17
            if skip < 0 or pos + skip > len(data): break
            pos += skip
    return names

def detect_type(data):
    if len(data) < 4:
        return f"too small ({len(data)} bytes)"

    magic4 = data[:4]

    # D32 format
    if magic4 == b'\x44\x33\x32\x46':
        frame_names = read_frame_names(data, True)
        if len(data) >= 32:
            w = struct.unpack_from("<I", data, 4)[0]
            h = struct.unpack_from("<I", data, 8)[0]
            gc = struct.unpack_from("<I", data, 16)[0]
            return f"D32 ({w}x{h}, {gc} groups, frames: {frame_names})"
        return f"D32 (frames: {frame_names})"

    # DEF format: check via header validation
    if len(data) >= 800:
        dtype = struct.unpack_from("<I", data, 0)[0]
        fw = struct.unpack_from("<I", data, 4)[0]
        fh = struct.unpack_from("<I", data, 8)[0]
        gc = struct.unpack_from("<I", data, 12)[0]
        if 0 < fw <= 4096 and 0 < fh <= 4096 and 0 < gc <= 256:
            frame_names = read_frame_names(data, False)
            return f"DEF (type={dtype}, {fw}x{fh}, {gc} groups, frames: {frame_names})"

    # PCX: check if fSize + 12 + 768 == total size (8-bit) or fSize + 12 == total (24-bit)
    if len(data) >= 12:
        fsize, w, h = struct.unpack_from("<III", data, 0)
        if w > 0 and w <= 4096 and h > 0 and h <= 4096:
            if fsize + 12 + 768 == len(data) and fsize == w * h:
                return f"PCX 8-bit ({w}x{h}, with palette)"
            if fsize + 12 == len(data) and fsize == w * h * 3:
                return f"PCX 24-bit ({w}x{h}, no palette)"

    # TXT/CSV: check if mostly printable ASCII
    printable = sum(1 for b in data[:500] if 0x20 <= b <= 0x7E or b in (0x0A, 0x0D, 0x09))
    if printable > len(data[:500]) * 0.9:
        preview = data[:200].decode('ascii', errors='replace').replace('\r\n', '\n')
        lines = preview.split('\n')
        return f"TEXT ({len(data)} bytes, first lines: {lines[:3]})"

    # MSK format (6 bytes)
    if len(data) == 6:
        return f"MSK (6 bytes: {data.hex()})"

    # FNT format
    if len(data) > 20 and magic4 == b'\x46\x4e\x54\x00':
        return f"FNT (font, {len(data)} bytes)"

    # WAV
    if magic4 == b'RIFF':
        return f"WAV ({len(data)} bytes)"

    # BMP
    if data[:2] == b'BM':
        return f"BMP ({len(data)} bytes)"

    return f"UNKNOWN ({len(data)} bytes, magic={magic4.hex()}, first 32: {data[:32].hex()})"

def main():
    lod_path = sys.argv[1] if len(sys.argv) > 1 else "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"
    target_set = set(ENTRIES_TO_FIND)

    with open(lod_path, "rb") as f:
        f.seek(8)
        total, = struct.unpack("<I", f.read(4))
        f.seek(0x0C)
        key = f.read(4)

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

        for hex_name, offset, size, csize, compression in entries:
            if hex_name not in target_set:
                continue

            f.seek(offset)
            if csize != 0:
                raw = f.read(csize)
                try:
                    if compression == 2:
                        data = extract_lzma(raw)
                    elif compression == 3:
                        data = zlib.decompress(raw)
                    else:
                        data = raw
                except Exception as e:
                    print(f"{hex_name}: DECOMPRESS ERROR ({e})")
                    continue
            else:
                data = f.read(size)

            type_info = detect_type(data)
            print(f"{hex_name}: {type_info}")

if __name__ == "__main__":
    main()
