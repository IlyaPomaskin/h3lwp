package com.homm3.livewallpaper.parser.h3c

import com.homm3.livewallpaper.parser.BinaryReader
import java.util.logging.Logger

/**
 * Parses stream 0 of an .h3c (the campaign metadata stream) and returns the
 * list of per-scenario mapName strings, in scenario order.
 *
 * Field layout transcribed from VCMI's lib/campaign/CampaignHandler.cpp.
 * Anything not needed for naming the per-scenario .h3m output is read just
 * far enough to advance the cursor to the next scenario.
 */
internal object CampaignHeaderReader {
    private val log = Logger.getLogger(CampaignHeaderReader::class.java.name)

    /** Hard upper bound on expected scenarios — any HotA campaign in the wild has ≤ 8. */
    private const val MAX_SCENARIOS = 64

    /** Hard upper bound on the per-scenario startingBonus count (VCMI's enum has ~10 entries). */
    private const val MAX_BONUS_COUNT = 256

    /** Hard upper bound on any base_string length — protects against bogus u32s. */
    private const val MAX_STRING_LEN = 64 * 1024

    /**
     * Fixed byte count of the travelOptions block per scenario when both
     * length-prefixed sub-sections (startingOptions, startingBonuses) are empty.
     */
    const val TRAVEL_OPTIONS_FIXED_BYTES = 1 + 19 + 20 + 18 + 1 + 4   // = 63

    fun readScenarioNames(stream0: ByteArray): List<String> {
        log.info("readScenarioNames: stream0=${stream0.size} bytes; first16=${stream0.take(16).joinToString { "%02x".format(it) }}")
        val r = BinaryReader(stream0.inputStream())

        val versionInt = r.readInt()
        val version = CampaignVersion.fromInt(versionInt)
        log.info("  version=$version (raw=$versionInt)")

        val numberOfScenarios: Int
        val campaignMapId: Int

        if (version == CampaignVersion.HOTA) {
            val formatVersion = r.readInt()
            log.info("  formatVersion=$formatVersion")
            if (formatVersion == 2) {
                val major = r.readInt()
                val minor = r.readInt()
                val patch = r.readInt()
                val force = r.readByte()
                log.info("  hota version: $major.$minor.$patch force=$force")
            }
            val unknownB = r.readByte()
            val unknownC = r.readInt()
            numberOfScenarios = r.readInt()
            campaignMapId = r.readByte()
            log.info("  unknownB=$unknownB unknownC=$unknownC numberOfScenarios=$numberOfScenarios campaignMapId=$campaignMapId")
        } else {
            campaignMapId = r.readByte()
            numberOfScenarios = CampaignRegionTable.scenarioCount(campaignMapId)
                ?: throw IllegalArgumentException(
                    "Unknown non-HotA campaignMapId=$campaignMapId; cannot determine scenario count"
                )
            log.info("  non-HotA: campaignMapId=$campaignMapId → numberOfScenarios=$numberOfScenarios")
        }

        require(numberOfScenarios in 1..MAX_SCENARIOS) {
            "Bogus numberOfScenarios=$numberOfScenarios; field layout is likely wrong"
        }

        val name = readBaseString(r, "name")
        val description = readBaseString(r, "description")
        log.info("  name='$name' description.len=${description.length}")

        if (version != CampaignVersion.ROE) {
            val diff = r.readByte()
            log.info("  difficultyChosenByPlayer=$diff")
        }
        val music = r.readByte()
        log.info("  music=$music")

        val names = ArrayList<String>(numberOfScenarios)
        for (i in 0 until numberOfScenarios) {
            log.info("  scenario $i:")
            val mapName = readBaseString(r, "  mapName")
            val packedMapSize = r.readInt()
            val preconditionRegions = if (numberOfScenarios > 8) {
                r.readBytes(2).joinToString(" ") { "%02x".format(it) }
            } else "%02x".format(r.readByte())
            val regionColor = r.readByte()
            val difficulty = r.readByte()
            val regionText = readBaseString(r, "    regionText")
            log.info(
                "    mapName='$mapName' packedMapSize=$packedMapSize preconditionRegions=$preconditionRegions " +
                    "regionColor=$regionColor difficulty=$difficulty regionText.len=${regionText.length}"
            )
            // prolog
            val proV = r.readByte(); val proM = r.readByte(); val proVo = r.readByte()
            val proText = readBaseString(r, "    prolog.text")
            log.info("    prolog: video=$proV music=$proM voice=$proVo text.len=${proText.length}")
            // epilog
            val epV = r.readByte(); val epM = r.readByte(); val epVo = r.readByte()
            val epText = readBaseString(r, "    epilog.text")
            log.info("    epilog: video=$epV music=$epM voice=$epVo text.len=${epText.length}")
            skipTravelOptions(r)
            names += mapName
        }
        log.info("readScenarioNames done; ${names.size} names")
        return names
    }

    private fun readBaseString(r: BinaryReader, label: String): String {
        val n = r.readInt()
        require(n in 0..MAX_STRING_LEN) {
            "Bogus base_string length at '$label': $n bytes (max $MAX_STRING_LEN); field misalignment"
        }
        if (n == 0) return ""
        return String(r.readBytes(n), Charsets.UTF_8)
    }

    private fun skipTravelOptions(r: BinaryReader) {
        val startingBonus = r.readByte()
        r.readBytes(19)               // crossoverHeroes
        r.readBytes(20)               // crossoverCreatures
        r.readBytes(18)               // crossoverArtifacts
        val startingOptionsType = r.readByte()
        val bonusCount = r.readInt()
        log.info(
            "    travelOptions: startingBonus=$startingBonus startingOptionsType=$startingOptionsType " +
                "bonusCount=$bonusCount"
        )
        require(bonusCount in 0..MAX_BONUS_COUNT) {
            "Bogus bonusCount=$bonusCount in travelOptions (max $MAX_BONUS_COUNT); field misalignment"
        }
        repeat(bonusCount) { r.readBytes(5) }
    }
}
