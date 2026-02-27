#!/usr/bin/env python3
"""Unpack HotA 1.8 LOD archive to a directory."""

import struct
import sys
import os
import zlib
import lzma


def xor_decrypt(data, key):
    return bytes(data[i] ^ key[i % len(key)] for i in range(len(data)))


def extract_lzma(data):
    filters = [{
        "id": lzma.FILTER_LZMA1,
        "dict_size": 262144,
        "lc": 3,
        "lp": 0,
        "pb": 2,
    }]
    decompressor = lzma.LZMADecompressor(format=lzma.FORMAT_RAW, filters=filters)
    return decompressor.decompress(data[1:])


def unpack(lod_path, out_dir):
    with open(lod_path, "rb") as f:
        header = f.read(4)
        if header != b"LOD\0":
            print(f"Not a LOD file: {header}")
            return

        f.seek(8)
        total, = struct.unpack("<I", f.read(4))

        f.seek(0x0C)
        key = f.read(4)

        if key[0] != 135:
            print(f"Not HotA 1.8 format (key[0]={key[0]}, expected 135)")
            return

        print(f"Entries: {total}")
        os.makedirs(out_dir, exist_ok=True)

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
            f.seek(offset)
            if csize != 0:
                raw = f.read(csize)
                if compression == 2:
                    data = extract_lzma(raw)
                elif compression == 3:
                    data = zlib.decompress(raw)
                else:
                    data = raw
            else:
                data = f.read(size)

            out_path = os.path.join(out_dir, hex_name)
            with open(out_path, "wb") as out:
                out.write(data)

            print(f"  {hex_name}  size={len(data)}  compression={compression}")

    print(f"\nDone. {total} files extracted to {out_dir}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <HotA.lod> [output_dir]")
        sys.exit(1)

    lod_file = sys.argv[1]
    output = sys.argv[2] if len(sys.argv) > 2 else "hota18_unpacked"
    unpack(lod_file, output)
