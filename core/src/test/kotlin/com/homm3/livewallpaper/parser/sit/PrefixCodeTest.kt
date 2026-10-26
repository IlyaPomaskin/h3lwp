package com.homm3.livewallpaper.parser.sit

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrefixCodeTest {
    @Test
    fun canonicalCodeAssignsShortestCodesFirst() {
        // Lengths 2,1,3,3 give codes B=0, A=10, C=110, D=111; bits are packed lowest first.
        val code = PrefixCode.fromLengths(intArrayOf(2, 1, 3, 3))
        val bits = listOf(0, 1, 0, 1, 1, 0, 1, 1, 1)
        val reader = LsbBitReader(packLowBitFirst(bits).inputStream())

        val symbols = List(4) { code.decode(reader) }

        assertEquals(listOf(1, 0, 2, 3), symbols)
    }

    @Test
    fun codesLongerThanTheLookupTableDecode() {
        val lengths = IntArray(15) { it + 1 } + 15
        val code = PrefixCode.fromLengths(lengths)
        val bits = List(14) { 1 } + 0

        val symbol = code.decode(LsbBitReader(packLowBitFirst(bits).inputStream()))

        assertEquals(14, symbol)
    }

    @Test
    fun oversubscribedLengthsAreRejected() {
        assertFailsWith<IllegalArgumentException> { PrefixCode.fromLengths(intArrayOf(1, 1, 1)) }
    }

    private fun packLowBitFirst(bits: List<Int>): ByteArray =
        bits.chunked(Byte.SIZE_BITS)
            .map { chunk -> chunk.foldIndexed(0) { index, byte, bit -> byte or (bit shl index) }.toByte() }
            .toByteArray()
}
