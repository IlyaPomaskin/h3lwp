package com.homm3.livewallpaper.parser.sit

/**
 * Huffman code read from an [LsbBitReader]. Codes up to [PRIMARY_TABLE_BITS] long decode with one table lookup,
 * longer ones fall back to a per-length search.
 */
internal class PrefixCode private constructor(patterns: IntArray, lengths: IntArray) {
    private val maxLength = lengths.max()
    private val tableBits = minOf(maxLength, PRIMARY_TABLE_BITS)
    private val table = IntArray(1 shl tableBits) { NO_ENTRY }
    private val longCodes = HashMap<Long, Int>()

    init {
        require(maxLength in 1..MAX_CODE_LENGTH) { "Invalid prefix code length: $maxLength" }
        lengths.indices.filter { lengths[it] > 0 }.forEach { symbol ->
            val length = lengths[symbol]
            if (length > tableBits) {
                longCodes[longCodeKey(length, patterns[symbol])] = symbol
                return@forEach
            }
            val step = 1 shl length
            for (index in patterns[symbol] until table.size step step) {
                require(table[index] == NO_ENTRY) { "Ambiguous prefix code" }
                table[index] = (symbol shl LENGTH_BITS) or length
            }
        }
    }

    fun decode(reader: LsbBitReader): Int {
        val entry = table[reader.peek(tableBits)]
        if (entry != NO_ENTRY) {
            reader.skip(entry and LENGTH_MASK)
            return entry ushr LENGTH_BITS
        }
        for (length in tableBits + 1..maxLength) {
            val symbol = longCodes[longCodeKey(length, reader.peek(length))] ?: continue
            reader.skip(length)
            return symbol
        }
        throw IllegalArgumentException("Invalid prefix code in compressed data")
    }

    companion object {
        private const val PRIMARY_TABLE_BITS = 12
        private const val MAX_CODE_LENGTH = 32
        private const val NO_ENTRY = -1
        private const val LENGTH_BITS = 8
        private const val LENGTH_MASK = (1 shl LENGTH_BITS) - 1
        private const val UINT_MASK = 0xFFFF_FFFFL

        /** Symbol `i` has code [codes]`[i]`, whose lowest bit is read first. */
        fun fromLowBitFirstCodes(codes: IntArray, lengths: IntArray): PrefixCode {
            require(codes.size == lengths.size) { "codes and lengths differ in size" }
            return PrefixCode(codes, lengths)
        }

        /** Canonical code: shorter codes first, ties by symbol, the first code all zeros. Lengths <= 0 mark unused symbols. */
        fun fromLengths(lengths: IntArray): PrefixCode {
            val usedSymbols = lengths.indices.filter { lengths[it] > 0 }.sortedWith(compareBy({ lengths[it] }, { it }))
            require(usedSymbols.isNotEmpty()) { "Prefix code has no symbols" }
            require(lengths.all { it <= MAX_CODE_LENGTH }) { "Prefix code length over $MAX_CODE_LENGTH" }
            val kraftSum = usedSymbols.sumOf { 1L shl (MAX_CODE_LENGTH - lengths[it]) }
            require(kraftSum <= 1L shl MAX_CODE_LENGTH) { "Oversubscribed prefix code" }

            val patterns = IntArray(lengths.size)
            var code = 0L
            var codeLength = lengths[usedSymbols.first()]
            usedSymbols.forEach { symbol ->
                code = code shl (lengths[symbol] - codeLength)
                codeLength = lengths[symbol]
                patterns[symbol] = Integer.reverse(code.toInt()) ushr (Int.SIZE_BITS - codeLength)
                code++
            }
            return PrefixCode(patterns, lengths)
        }

        private fun longCodeKey(length: Int, pattern: Int): Long = (length.toLong() shl Int.SIZE_BITS) or (pattern.toLong() and UINT_MASK)
    }
}
