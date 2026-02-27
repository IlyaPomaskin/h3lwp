#!/usr/bin/env python3
"""Use homm3data library to read HotA 1.8 LOD and list all files with their real names."""
import sys, warnings
from io import BytesIO
from homm3data import lodfile, deffile, pcxfile
import struct

warnings.filterwarnings('ignore')

def detect_type_simple(data):
    if len(data) < 4:
        return "TINY", ""
    if data[:4] == b'\x44\x33\x32\x46':
        return "D32", ""
    if len(data) >= 12:
        fsize, w, h = struct.unpack_from("<III", data, 0)
        if 0 < w <= 4096 and 0 < h <= 4096:
            if fsize + 12 + 768 == len(data) and fsize == w * h:
                return "PCX8", f"{w}x{h}"
            if fsize + 12 == len(data) and fsize == w * h * 3:
                return "PCX24", f"{w}x{h}"
    if data[:4] == b'RIFF':
        return "WAV", ""
    if len(data) <= 20:
        return "BIN", ""
    return "UNK", ""

def main():
    lod_path = sys.argv[1] if len(sys.argv) > 1 else "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"

    with lodfile.open(lod_path) as lod:
        files = lod.get_filelist()
        print(f"Total files: {len(files)}")
        print(f"{'ENTRY':<34} {'TYPE':<6} {'DETAILS':<20} FILELIST")
        print("-" * 120)

        for name in files:
            data = lod.get_file(name)

            # Try as DEF
            try:
                with deffile.open(BytesIO(data)) as d:
                    fl = d.get_filelist()
                    if fl:
                        print(f"{name:<34} DEF    {len(fl)} frames{'':<13} {fl[:5]}")
                        continue
            except:
                pass

            # Try as D32
            if len(data) >= 4 and data[:4] == b'\x44\x33\x32\x46':
                print(f"{name:<34} D32    {len(data)} bytes")
                continue

            # Other types
            ftype, details = detect_type_simple(data)
            print(f"{name:<34} {ftype:<6} {details}")

if __name__ == "__main__":
    main()
