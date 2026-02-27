#!/usr/bin/env python3
"""List all entries from a LOD file with sizes and frame info for DEFs.

Usage:
    python3 lod_list.py <file.lod> [--frames]

Options:
    --frames    Show individual frame filenames for each DEF
"""
import struct
import sys
import warnings
from io import BytesIO

from homm3data import lodfile, deffile

warnings.filterwarnings("ignore")


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <file.lod> [--frames]")
        sys.exit(1)

    path = sys.argv[1]
    show_frames = "--frames" in sys.argv

    with lodfile.open(path) as lod:
        files = lod.get_filelist()

        for name in files:
            data = lod.get_file(name)
            ext = name.rsplit(".", 1)[-1].lower() if "." in name else ""
            line = f"{name:<30} size={len(data)}"

            if ext == "def" and len(data) >= 16:
                try:
                    magic = struct.unpack_from("<I", data, 0)[0]
                    is_d32 = magic == 0x46323344
                    with deffile.open(BytesIO(data)) as d:
                        w, h = d.get_size()
                        groups = d.get_groups()
                        raw = d.get_raw_data()
                        ftype = "D32" if is_d32 else d.get_type().name
                        line += f"  {ftype} {w}x{h} groups={len(groups)} frames={len(raw)}"
                        if show_frames:
                            seen = []
                            for r in raw:
                                fn = r["name"]
                                if fn not in seen:
                                    seen.append(fn)
                            line += f"  files={','.join(seen)}"
                except Exception as e:
                    line += f"  (parse error: {e})"

            print(line)

        print(f"\nTotal entries: {len(files)}")


if __name__ == "__main__":
    main()
