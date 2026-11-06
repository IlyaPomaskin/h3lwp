package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val HOTA_1_8_0 = 9
private const val HOTA_1_8_1 = 10
private const val MISSION_LEVEL = 2
private const val REWARD_EXPERIENCE = 1

class H3mObjectDataReaderHota10Test {
    private class Bytes {
        private val out = ByteArrayOutputStream()

        fun byte(value: Int) = apply { out.write(value) }
        fun bool(value: Boolean) = byte(if (value) 1 else 0)
        fun int(value: Int) = apply {
            out.write(ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
        }
        fun string(value: String) = apply {
            val encoded = value.toByteArray(Charsets.US_ASCII)
            int(encoded.size)
            out.write(encoded)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    private fun Bytes.levelQuest(hasTimeoutMessage: Boolean) = apply {
        byte(MISSION_LEVEL)
        int(5)
        int(-1)
        string("first")
        string("next")
        string("done")
        if (hasTimeoutMessage) string("timeout")
    }

    private fun Bytes.seerHut(hotaSubVersion: Int) = apply {
        val isHota10 = hotaSubVersion >= HOTA_1_8_1
        int(1)
        levelQuest(hasTimeoutMessage = isHota10)
        byte(REWARD_EXPERIENCE)
        int(1000)
        int(0)
        if (isHota10) {
            bool(true)
            string("Seer name")
        }
        byte(0)
        byte(0)
    }

    private fun assertConsumesAll(data: ByteArray, hotaSubVersion: Int, read: H3mObjectDataReader.() -> Unit) {
        val stream = BinaryReader(data)
        H3mObjectDataReader(H3mVersion.HOTA, stream, hotaSubVersion).read()
        assertEquals(data.size.toLong(), stream.position)
    }

    @Test
    fun `reads hota 1_8_1 seer hut with quest timeout message and name`() {
        val data = Bytes().seerHut(HOTA_1_8_1).toByteArray()

        assertConsumesAll(data, HOTA_1_8_1) { readSeerHut() }
    }

    @Test
    fun `reads hota 1_8_1 seer hut without name`() {
        val data = Bytes()
            .int(1).levelQuest(hasTimeoutMessage = true).byte(REWARD_EXPERIENCE).int(1000)
            .int(0).bool(false).byte(0).byte(0)
            .toByteArray()

        assertConsumesAll(data, HOTA_1_8_1) { readSeerHut() }
    }

    @Test
    fun `reads hota 1_8_1 seer hut with empty quest without timeout message`() {
        val data = Bytes().int(1).byte(0).byte(0).int(0).bool(false).byte(0).byte(0).toByteArray()

        assertConsumesAll(data, HOTA_1_8_1) { readSeerHut() }
    }

    @Test
    fun `reads hota 1_8_0 seer hut unchanged`() {
        val data = Bytes().seerHut(HOTA_1_8_0).toByteArray()

        assertConsumesAll(data, HOTA_1_8_0) { readSeerHut() }
    }

    @Test
    fun `reads hota 1_8_1 quest guard with timeout message`() {
        val data = Bytes().levelQuest(hasTimeoutMessage = true).toByteArray()

        assertConsumesAll(data, HOTA_1_8_1) { readQuestGuard() }
    }

    @Test
    fun `reads hota 1_8_0 quest guard unchanged`() {
        val data = Bytes().levelQuest(hasTimeoutMessage = false).toByteArray()

        assertConsumesAll(data, HOTA_1_8_0) { readQuestGuard() }
    }
}
