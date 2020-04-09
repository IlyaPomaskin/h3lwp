package com.heroes3.livewallpaper.AssetsParser;

import java.io.IOException;
import java.io.InputStream;

public class LodReader {
    protected Lod lod = new Lod();
    protected Reader rdr;

    Lod read(InputStream stream) throws IOException {
        rdr = new Reader(stream);

        lod.magic = rdr.readBytes(lod.magic.length);
        lod.filesCount = rdr.readInt();
        lod.unknown = rdr.readBytes(lod.unknown.length);
        lod.files = new Lod.File[lod.filesCount];

        for (int i = 0; i < lod.filesCount; i++) {
            lod.files[i] = readFile();
        }

        return lod;
    }

    private Lod.File readFile() throws IOException {
        Lod.File file = new Lod.File();
        file.name = rdr.readString(16);
        file.offset = rdr.readInt();
        file.size = rdr.readInt();
        file.fileType = Lod.FileType.get(rdr.readInt());
        file.compressedSize = rdr.readInt();
        return file;
    }
}
