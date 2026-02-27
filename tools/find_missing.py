#!/usr/bin/env python3
"""Find specific missing sprites in HotA 1.8 LOD."""
import struct, sys, zlib, lzma, re

MISSING = [
    "4lvlxshrn", "ava0128w", "avxwgtwh", "avxwbor8", "avxl1sh0w",
    "avxwgtbr", "avgself", "avwcoat", "avmgosn1", "avgmamm",
    "avxseek0", "avgjotu", "avxamsn1", "sanct_wt", "avxwgtrd",
    "avxl3sh0w", "avxptw_3", "avgkobo", "avgsham", "avxmn2pink0",
]

def xor_decrypt(data, key):
    return bytes(data[i] ^ key[i % len(key)] for i in range(len(data)))

def extract_lzma(data):
    filters = [{"id": lzma.FILTER_LZMA1, "dict_size": 262144, "lc": 3, "lp": 0, "pb": 2}]
    return lzma.LZMADecompressor(format=lzma.FORMAT_RAW, filters=filters).decompress(data[1:])

def read_all_frame_names(data):
    """Read ALL frame names from DEF or D32."""
    names = []
    if data[:4] == b'\x44\x33\x32\x46':
        if len(data) < 61: return names
        gc = struct.unpack_from("<I", data, 16)[0]
        if gc <= 0 or gc > 256: return names
        pos = 32
        for g in range(gc):
            if pos + 16 > len(data): break
            _, fc, _, _ = struct.unpack_from("<IIII", data, pos)
            pos += 16
            if fc < 0 or fc > 10000: break
            for i in range(fc):
                if pos + 13 > len(data): break
                name = data[pos:pos+13].split(b'\0')[0].decode('ascii', errors='replace')
                names.append(name)
                pos += 13
            pos += fc * 4
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
            for i in range(fc):
                if pos + 13 > len(data): break
                name = data[pos:pos+13].split(b'\0')[0].decode('ascii', errors='replace')
                names.append(name)
                pos += 13
            pos += fc * 4
    return names

def main():
    lod_path = sys.argv[1] if len(sys.argv) > 1 else "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"
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

        # Search for each missing object
        for missing in MISSING:
            found = False
            patterns = [missing.upper(), missing.lower()]
            # Also try without avx/avg/avm/avw prefix
            for prefix in ["avx", "avg", "avm", "avw", "ava", "avl"]:
                if missing.lower().startswith(prefix):
                    patterns.append(missing[len(prefix):].upper())
                    patterns.append(missing[len(prefix):].lower())
            # Try without leading digits
            if missing[0].isdigit():
                patterns.append(missing.lstrip("0123456789").upper())

            for hex_name, offset, size, csize, compression in entries:
                f.seek(offset)
                if csize != 0:
                    raw = f.read(csize)
                    try:
                        if compression == 2: data = extract_lzma(raw)
                        elif compression == 3: data = zlib.decompress(raw)
                        else: data = raw
                    except: continue
                else:
                    data = f.read(size)

                if len(data) < 4: continue

                # Search raw bytes for the patterns
                for pat in patterns:
                    pat_bytes = pat.encode('ascii')
                    if pat_bytes in data[:3000]:
                        names = read_all_frame_names(data)
                        first = names[0] if names else "N/A"
                        print(f"  {missing}: FOUND pattern '{pat}' in {hex_name} "
                              f"(size={len(data)}, first_frame='{first}', frames={len(names)})")
                        found = True
                        break
                if found:
                    break

            if not found:
                print(f"  {missing}: NOT FOUND in any entry")

if __name__ == "__main__":
    main()
