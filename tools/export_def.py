#!/usr/bin/env python3
"""Find DEF entries by frame name pattern in HotA 1.8 LOD, list frames, and export as PNG."""
import sys, struct, os, warnings
from io import BytesIO
from homm3data import lodfile, deffile
from PIL import Image

warnings.filterwarnings('ignore')

def main():
    patterns = sys.argv[1:] if len(sys.argv) > 1 else ["avmcrsn"]
    lod_path = "/Users/ilyapomaskin/temp/h3lwp-claude/hota18.lod"
    out_dir = "/Users/ilyapomaskin/temp/h3lwp-claude/tools/exported_pngs"
    os.makedirs(out_dir, exist_ok=True)

    with lodfile.open(lod_path) as lod:
        files = lod.get_filelist()
        for name in files:
            data = lod.get_file(name)
            if len(data) < 16:
                continue
            try:
                with deffile.open(BytesIO(data)) as d:
                    raw = d.get_raw_data()
                    frame_names = [r["name"].lower() for r in raw]
                    matched = any(
                        p.lower() in fn for p in patterns for fn in frame_names
                    )
                    if not matched:
                        continue

                    groups = d.get_groups()
                    size = d.get_size()
                    filelist = d.get_filelist()
                    magic = struct.unpack_from("<I", data, 0)[0]
                    ftype = "D32" if magic == 0x46323344 else "DEF"

                    print(f"\n{'='*60}")
                    print(f"LOD entry: {name}")
                    print(f"Type: {ftype}  Size: {size[0]}x{size[1]}  Groups: {len(groups)}")
                    print(f"Frame names ({len(raw)}): {[r['name'] for r in raw]}")
                    print(f"Filelist: {filelist[:20]}")
                    print(f"Groups count: {len(groups)}")

                    # Try exporting via raw data - each raw entry has pixel data
                    for i, entry in enumerate(raw):
                        fname = entry["name"]
                        w = entry.get("width", 0)
                        h = entry.get("height", 0)
                        print(f"  [{i}] {fname}  keys={list(entry.keys())}  w={w} h={h}")

                    # Try exporting via group iteration
                    frame_idx = 0
                    for gi, group in enumerate(groups):
                        print(f"\n  Group {gi}: type={type(group)} len={len(group) if hasattr(group, '__len__') else '?'}")
                        try:
                            for fi in range(len(group)):
                                img = group[fi]
                                fname = raw[frame_idx]["name"] if frame_idx < len(raw) else f"g{gi}_f{fi}"
                                safe = fname.replace("/", "_").replace("\\", "_").replace(".", "_")
                                png_path = os.path.join(out_dir, f"{name[:16]}_{safe}.png")
                                if isinstance(img, Image.Image):
                                    img.save(png_path)
                                    print(f"    [{fi}] {fname} -> saved {img.size}")
                                else:
                                    print(f"    [{fi}] {fname} -> type={type(img)}")
                                frame_idx += 1
                        except Exception as e:
                            print(f"    Group {gi} iteration error: {e}")

            except Exception as e:
                import traceback
                traceback.print_exc()

    print(f"\nDone. PNGs saved to {out_dir}")

if __name__ == "__main__":
    main()
