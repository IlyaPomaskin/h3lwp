#!/usr/bin/env python3
"""Analyze frame name patterns in HotA 1.8 LOD to understand obfuscation."""
import struct, sys, zlib, lzma, re
from collections import Counter

def xor_decrypt(data, key):
    return bytes(data[i] ^ key[i % len(key)] for i in range(len(data)))

def extract_lzma(data):
    filters = [{"id": lzma.FILTER_LZMA1, "dict_size": 262144, "lc": 3, "lp": 0, "pb": 2}]
    return lzma.LZMADecompressor(format=lzma.FORMAT_RAW, filters=filters).decompress(data[1:])

def read_all_frame_names_flat(data, is_d32):
    """Read ALL frame names (not just first per group)."""
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
            for i in range(fc):
                if pos + 13 > len(data): break
                name = data[pos:pos+13].split(b'\0')[0].decode('ascii', errors='replace')
                if name and all(0x20 <= ord(c) <= 0x7E for c in name):
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
                if name and all(0x20 <= ord(c) <= 0x7E for c in name):
                    names.append(name)
                pos += 13
            pos += fc * 4
    return names

def main():
    lod_path = sys.argv[1] if len(sys.argv) > 1 else "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"

    # Also load hota17 for comparison
    hota17_path = "/Users/ilyapomaskin/temp/h3lwp-claude/hota17.lod"

    # Read hota17 frame names for comparison
    hota17_frames = {}  # base_name -> list of frame names
    try:
        with open(hota17_path, "rb") as f:
            f.seek(8)
            total, = struct.unpack("<I", f.read(4))
            f.seek(92)
            for i in range(total):
                raw = f.read(32)
                name = raw[:16].split(b'\0')[0].decode('ascii', errors='replace')
                offset, size, ftype, csize = struct.unpack_from("<IIII", raw, 16)
                if not name.lower().endswith('.def'): continue
                f_pos = f.tell()
                f.seek(offset)
                if csize != 0:
                    compressed = f.read(csize)
                    try:
                        data = zlib.decompress(compressed)
                    except:
                        f.seek(f_pos); continue
                else:
                    data = f.read(size)
                f.seek(f_pos)
                frames = read_all_frame_names_flat(data, False)
                if frames:
                    hota17_frames[name.lower()] = frames
        print(f"Loaded {len(hota17_frames)} DEFs from HotA 1.7 for comparison\n")
    except FileNotFoundError:
        print("HotA 1.7 LOD not found, skipping comparison\n")

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

        pattern_standard = 0  # NAME + A\d{3}
        pattern_dot_ext = 0   # NAME + .xxx
        pattern_0w = 0        # 0w_ prefix
        pattern_other = 0

        dot_ext_examples = []
        standard_examples = []

        # Compare with hota17
        same_frames = 0
        different_frames = 0
        comparison_examples = []

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

            magic4 = data[:4] if len(data) >= 4 else b''
            is_d32 = magic4 == b'\x44\x33\x32\x46'

            # Only look at DEF/D32
            if not is_d32 and len(data) >= 800:
                fw = struct.unpack_from("<I", data, 4)[0]
                fh = struct.unpack_from("<I", data, 8)[0]
                gc = struct.unpack_from("<I", data, 12)[0]
                if not (0 < fw <= 4096 and 0 < fh <= 4096 and 0 < gc <= 256):
                    continue
            elif not is_d32:
                continue

            frames = read_all_frame_names_flat(data, is_d32)
            if not frames: continue

            first = frames[0]
            has_dot = '.' in first
            has_standard = bool(re.search(r'[Aa]\d{3}', first))
            has_0w = first.startswith('0w_')

            if has_0w:
                pattern_0w += 1
            elif has_dot and not has_standard:
                pattern_dot_ext += 1
                if len(dot_ext_examples) < 30:
                    dot_ext_examples.append((first, len(frames), frames[:3]))
            elif has_standard:
                pattern_standard += 1
                if len(standard_examples) < 10:
                    standard_examples.append((first, len(frames), frames[:3]))
            else:
                pattern_other += 1

            # Compare with hota17 if available
            # Try to find matching DEF by base name
            base = re.sub(r'[Aa]\d{3}.*$', '', first)
            base = re.sub(r'\..*$', '', base)
            if base.startswith('0w_'): base = base[3:]
            base_lower = base.lower()
            for h17_name, h17_frames in hota17_frames.items():
                h17_base = h17_name.replace('.def', '')
                if base_lower == h17_base or base_lower in h17_base or h17_base in base_lower:
                    if frames[0] == h17_frames[0]:
                        same_frames += 1
                    else:
                        different_frames += 1
                        if len(comparison_examples) < 20:
                            comparison_examples.append((base_lower, h17_frames[0], frames[0], len(h17_frames), len(frames)))
                    break

        print("=== Frame name patterns in HotA 1.8 ===")
        print(f"Standard (A000 suffix):  {pattern_standard}")
        print(f"Dot extension (.xxx):    {pattern_dot_ext}")
        print(f"0w_ prefix:              {pattern_0w}")
        print(f"Other:                   {pattern_other}")

        print(f"\n=== Dot extension examples ===")
        for name, count, sample in dot_ext_examples:
            print(f"  {name:<25} ({count} frames) {sample}")

        print(f"\n=== Standard examples ===")
        for name, count, sample in standard_examples:
            print(f"  {name:<25} ({count} frames) {sample}")

        if comparison_examples:
            print(f"\n=== HotA 1.7 vs 1.8 comparison ===")
            print(f"Same frame names: {same_frames}")
            print(f"Different frame names: {different_frames}")
            print(f"\n{'BASE':<20} {'HOTA 1.7 FRAME':<25} {'HOTA 1.8 FRAME':<25} 1.7#  1.8#")
            for base, h17, h18, h17c, h18c in comparison_examples:
                print(f"  {base:<18} {h17:<25} {h18:<25} {h17c:<5} {h18c}")

if __name__ == "__main__":
    main()
