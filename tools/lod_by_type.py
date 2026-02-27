#!/usr/bin/env python3
"""List all entries from a LOD file grouped by file type, with frames for DEFs.

Usage:
    python3 lod_by_type.py <file.lod>
"""
import struct
import sys
import warnings
from collections import defaultdict
from io import BytesIO

from homm3data import lodfile, deffile

warnings.filterwarnings("ignore")


def parse_def(data):
    try:
        magic = struct.unpack_from("<I", data, 0)[0]
        is_d32 = magic == 0x46323344
        with deffile.open(BytesIO(data)) as d:
            w, h = d.get_size()
            groups = d.get_groups()
            raw = d.get_raw_data()
            ftype = "D32" if is_d32 else d.get_type().name
            seen = []
            for r in raw:
                fn = r["name"]
                if fn not in seen:
                    seen.append(fn)
            return {
                "type": ftype,
                "width": w,
                "height": h,
                "groups": len(groups),
                "frames": len(raw),
                "filenames": seen,
            }
    except Exception as e:
        return {"error": str(e)}


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <file.lod>")
        sys.exit(1)

    path = sys.argv[1]

    by_type = defaultdict(list)

    with lodfile.open(path) as lod:
        files = lod.get_filelist()
        for name in files:
            data = lod.get_file(name)
            ext = name.rsplit(".", 1)[-1].lower() if "." in name else "(none)"
            entry = {"name": name, "size": len(data)}
            if ext == "def" and len(data) >= 16:
                entry["def"] = parse_def(data)
            by_type[ext].append(entry)

    for ext in sorted(by_type.keys()):
        entries = by_type[ext]
        print(f"\n{'='*60}")
        print(f" {ext.upper()} — {len(entries)} files")
        print(f"{'='*60}")

        for e in entries:
            info = e.get("def")
            if info and "error" not in info:
                print(f"\n  {e['name']}  ({e['size']} bytes)")
                print(f"    {info['type']} {info['width']}x{info['height']}  groups={info['groups']} frames={info['frames']}")
                for fn in info["filenames"]:
                    print(f"      {fn}")
            elif info and "error" in info:
                print(f"\n  {e['name']}  ({e['size']} bytes)")
                print(f"    (parse error: {info['error']})")
            else:
                print(f"  {e['name']}  ({e['size']} bytes)")

    total = sum(len(v) for v in by_type.values())
    print(f"\n{'='*60}")
    print(f" SUMMARY: {total} total entries")
    for ext in sorted(by_type.keys()):
        print(f"   {ext.upper()}: {len(by_type[ext])}")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
