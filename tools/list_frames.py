#!/usr/bin/env python3
"""List all frame names from a LOD file using homm3data library."""
import sys, struct, warnings
from io import BytesIO
from homm3data import lodfile, deffile

warnings.filterwarnings('ignore')

def main():
    lod_path = sys.argv[1] if len(sys.argv) > 1 else "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"

    with lodfile.open(lod_path) as lod:
        files = lod.get_filelist()
        print(f"Total entries: {len(files)}")
        print(f"{'ENTRY':<34} {'TYPE':<10} {'SIZE':<12} {'GROUPS':<10} FRAME NAMES")
        print("-" * 130)

        for name in files:
            data = lod.get_file(name)
            if len(data) < 16:
                continue

            magic = struct.unpack_from("<I", data, 0)[0]
            is_d32 = magic == 0x46323344

            try:
                with deffile.open(BytesIO(data)) as d:
                    if is_d32:
                        ftype = "D32"
                        size_str = f"{d.get_size()[0]}x{d.get_size()[1]}"
                    else:
                        ftype = d.get_type().name
                        size_str = f"{d.get_size()[0]}x{d.get_size()[1]}"

                    groups = d.get_groups()
                    raw = d.get_raw_data()
                    frame_names = [r["name"] for r in raw]
                    # Deduplicate while preserving order
                    seen = set()
                    unique = []
                    for fn in frame_names:
                        if fn not in seen:
                            seen.add(fn)
                            unique.append(fn)

                    print(f"{name:<34} {ftype:<10} {size_str:<12} {len(groups):<10} {', '.join(unique[:10])}")
            except:
                pass

if __name__ == "__main__":
    main()
