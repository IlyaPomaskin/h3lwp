package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data
import com.badlogic.gdx.utils.Json
import ktx.log.logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class Etc1PageCache(private val dir: String = "cache/atlas") {
    data class Manifest(
        val pageCount: Int,
        val regionInfos: List<RegionInfo>,
        val packerRects: Map<String, PackedRect>,
    )

    fun cacheKey(neededSprites: Set<String>, lodFingerprint: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(lodFingerprint.toByteArray())
        neededSprites.sorted().forEach { md.update(it.toByteArray()) }
        return md.digest().joinToString("") { "%02x".format(it) }.substring(0, 16)
    }

    fun read(key: String): Etc1Bundle? {
        val root = Gdx.files.local("$dir/$key")
        if (!root.exists()) return null
        val manifestFile = root.child("manifest.json")
        if (!manifestFile.exists()) return null

        val json = Json()
        val manifest = json.fromJson(Manifest::class.java, manifestFile.readString())
        val pages = (0 until manifest.pageCount).map { i ->
            val color = readPkm(root.child("page_$i.color.pkm").readBytes())
            val alpha = readPkm(root.child("page_$i.alpha.pkm").readBytes())
            Etc1PageData(color, alpha)
        }
        log.info { "Etc1PageCache hit: key=$key pages=${pages.size}" }
        return Etc1Bundle(pages, manifest.regionInfos, manifest.packerRects)
    }

    fun write(key: String, bundle: Etc1Bundle) {
        val root = Gdx.files.local("$dir/$key")
        root.mkdirs()
        bundle.pages.forEachIndexed { i, page ->
            root.child("page_$i.color.pkm").writeBytes(writePkm(page.color), false)
            root.child("page_$i.alpha.pkm").writeBytes(writePkm(page.alpha), false)
        }
        val manifest = Manifest(bundle.pages.size, bundle.regionInfos, bundle.packerRects)
        root.child("manifest.json").writeString(Json().toJson(manifest), false)
        log.info { "Etc1PageCache wrote: key=$key pages=${bundle.pages.size}" }
    }

    /** Custom header (magic "E1D ", width, height) wraps the raw ETC1 payload. */
    private fun writePkm(data: ETC1Data): ByteArray {
        val payload = ByteArray(data.compressedData.capacity())
        data.compressedData.position(0); data.compressedData.get(payload); data.compressedData.position(0)
        val out = ByteArray(12 + payload.size)
        val buf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        buf.put('E'.code.toByte()); buf.put('1'.code.toByte()); buf.put('D'.code.toByte()); buf.put(' '.code.toByte())
        buf.putInt(data.width); buf.putInt(data.height)
        buf.put(payload)
        return out
    }

    private fun readPkm(raw: ByteArray): ETC1Data {
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(4); buf.get(magic)
        require(String(magic) == "E1D ") { "bad ETC1 cache magic" }
        val w = buf.int
        val h = buf.int
        val payload = ByteBuffer.allocateDirect(raw.size - 12).order(ByteOrder.nativeOrder())
        payload.put(raw, 12, raw.size - 12)
        payload.position(0)
        return ETC1Data(w, h, payload, 0)
    }

    companion object {
        private val log = logger<Etc1PageCache>()
    }
}
