package com.homm3.livewallpaper.parser.h3c

import com.homm3.livewallpaper.parser.BinaryReader

/**
 * Parses stream 0 of an .h3c (the campaign metadata stream) and returns the
 * list of per-scenario mapName strings, in scenario order.
 *
 * Field layout transcribed from VCMI's lib/campaign/CampaignHandler.cpp.
 * Anything not needed for naming the per-scenario .h3m output is read just
 * far enough to advance the cursor to the next scenario.
 */
internal object CampaignHeaderReader {

    /**
     * Fixed byte count of the travelOptions block per scenario when both
     * length-prefixed sub-sections (startingOptions, startingBonuses) are empty.
     * Source: VCMI CampaignHandler::readScenarioTravel.
     *
     *   startingBonus: u8                                              =  1
     *   crossoverHeroes:    19 bytes                                   = 19
     *   crossoverCreatures: 20 bytes                                   = 20
     *   crossoverArtifacts: 18 bytes                                   = 18
     *   startingOptionsType: u8 (=0 in our corpus -> no sub-bytes)     =  1
     *   numStartingBonuses: u32 (=0 in our corpus -> no entries)       =  4
     */
    const val TRAVEL_OPTIONS_FIXED_BYTES = 1 + 19 + 20 + 18 + 1 + 4   // = 63

    fun readScenarioNames(stream0: ByteArray): List<String> {
        val r = BinaryReader(stream0.inputStream())

        val versionInt = r.readInt()
        val version = CampaignVersion.fromInt(versionInt)

        val numberOfScenarios: Int
        @Suppress("UNUSED_VARIABLE")
        val campaignMapId: Int

        if (version == CampaignVersion.HOTA) {
            val formatVersion = r.readInt()
            if (formatVersion == 2) {
                r.readInt()      // hotaMajor
                r.readInt()      // hotaMinor
                r.readInt()      // hotaPatch
                r.readByte()     // forceMatchingVersion u8
            }
            r.readByte()         // unknownB i8
            r.readInt()          // unknownC i32
            numberOfScenarios = r.readInt()
            campaignMapId = r.readByte()
        } else {
            campaignMapId = r.readByte()
            numberOfScenarios = CampaignRegionTable.scenarioCount(campaignMapId)
                ?: throw IllegalArgumentException(
                    "Unknown non-HotA campaignMapId=$campaignMapId; cannot determine scenario count"
                )
        }

        readBaseString(r)        // name
        readBaseString(r)        // description

        if (version != CampaignVersion.ROE) r.readByte()  // difficultyChosenByPlayer
        r.readByte()             // music

        val names = ArrayList<String>(numberOfScenarios)
        for (i in 0 until numberOfScenarios) {
            val mapName = readBaseString(r)
            r.readInt()                                                   // packedMapSize (ignored)
            if (numberOfScenarios > 8) r.readBytes(2) else r.readByte()   // preconditionRegions
            r.readByte()                                                  // regionColor
            r.readByte()                                                  // difficulty
            readBaseString(r)                                             // regionText
            // prolog
            r.readByte(); r.readByte(); r.readByte(); readBaseString(r)
            // epilog
            r.readByte(); r.readByte(); r.readByte(); readBaseString(r)
            skipTravelOptions(r)
            names += mapName
        }
        return names
    }

    private fun readBaseString(r: BinaryReader): String {
        val n = r.readInt()
        if (n == 0) return ""
        return String(r.readBytes(n), Charsets.UTF_8)
    }

    private fun skipTravelOptions(r: BinaryReader) {
        r.readByte()                  // startingBonus u8
        r.readBytes(19)               // crossoverHeroes
        r.readBytes(20)               // crossoverCreatures
        r.readBytes(18)               // crossoverArtifacts
        r.readByte()                  // startingOptionsType u8 (0=none, no further bytes)
        val bonusCount = r.readInt()  // numStartingBonuses u32
        repeat(bonusCount) { r.readBytes(5) }
    }
}
