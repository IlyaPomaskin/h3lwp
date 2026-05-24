package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.log.logger
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class Etc1PageCache(private val dir: String = "cache/atlas") {

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

        val root2 = JsonReader().parse(manifestFile.readString())
        val pageCount = root2.getInt("pageCount")
        val regionInfos = root2.get("regionInfos")?.let { node ->
            (0 until node.size).map { i -> regionFromJson(node.get(i)) }
        } ?: emptyList()
        val packerRects = root2.get("packerRects")?.let { node ->
            val out = mutableMapOf<String, PackedRect>()
            var child = node.child
            while (child != null) {
                out[child.name] = rectFromJson(child)
                child = child.next
            }
            out.toMap()
        } ?: emptyMap()

        val pages = (0 until pageCount).map { i ->
            val color = readPkm(root.child("page_$i.color.pkm").readBytes())
            val alpha = readPkm(root.child("page_$i.alpha.pkm").readBytes())
            Etc1PageData(color, alpha)
        }
        log.info { "Etc1PageCache hit: key=$key pages=${pages.size}" }
        return Etc1Bundle(pages, regionInfos, packerRects)
    }

    fun write(key: String, bundle: Etc1Bundle) {
        val root = Gdx.files.local("$dir/$key")
        root.mkdirs()
        bundle.pages.forEachIndexed { i, page ->
            root.child("page_$i.color.pkm").writeBytes(writePkm(page.color), false)
            root.child("page_$i.alpha.pkm").writeBytes(writePkm(page.alpha), false)
        }
        root.child("manifest.json").writeString(writeManifest(bundle), false)
        log.info { "Etc1PageCache wrote: key=$key pages=${bundle.pages.size}" }
    }

    private fun writeManifest(bundle: Etc1Bundle): String {
        val sw = StringWriter()
        val w = JsonWriter(sw)
        w.`object`()
        w.set("pageCount", bundle.pages.size)
        w.array("regionInfos")
        for (r in bundle.regionInfos) {
            w.`object`()
            w.set("packerName", r.packerName)
            w.set("regionName", r.regionName)
            w.set("regionIndex", r.regionIndex)
            w.set("width", r.width)
            w.set("height", r.height)
            w.set("fullWidth", r.fullWidth)
            w.set("fullHeight", r.fullHeight)
            w.set("x", r.x)
            w.set("y", r.y)
            w.set("isTerrain", r.isTerrain)
            w.pop()
        }
        w.pop()
        w.`object`("packerRects")
        for ((name, rect) in bundle.packerRects) {
            w.`object`(name)
            w.set("pageIndex", rect.pageIndex)
            w.set("x", rect.x)
            w.set("y", rect.y)
            w.set("w", rect.w)
            w.set("h", rect.h)
            w.pop()
        }
        w.pop()
        w.pop()
        w.close()
        return sw.toString()
    }

    private fun regionFromJson(v: JsonValue) = RegionInfo(
        packerName = v.getString("packerName"),
        regionName = v.getString("regionName"),
        regionIndex = v.getInt("regionIndex"),
        width = v.getInt("width"),
        height = v.getInt("height"),
        fullWidth = v.getInt("fullWidth"),
        fullHeight = v.getInt("fullHeight"),
        x = v.getInt("x"),
        y = v.getInt("y"),
        isTerrain = v.getBoolean("isTerrain"),
    )

    private fun rectFromJson(v: JsonValue) = PackedRect(
        pageIndex = v.getInt("pageIndex"),
        x = v.getInt("x"),
        y = v.getInt("y"),
        w = v.getInt("w"),
        h = v.getInt("h"),
    )

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
