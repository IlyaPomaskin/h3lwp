package com.homm3.livewallpaper.parser.h3c

internal enum class CampaignVersion(val value: Int) {
    ROE(4),
    AB(5),
    SOD(6),
    CHR(7),    // Chronicles
    WOG(8),
    HOTA(10);

    companion object {
        fun fromInt(value: Int): CampaignVersion =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown campaign version: $value")
    }
}
