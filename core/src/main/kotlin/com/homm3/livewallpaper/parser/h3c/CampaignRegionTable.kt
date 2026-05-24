package com.homm3.livewallpaper.parser.h3c

/**
 * Per-campaign scenario counts for non-HotA campaigns, keyed by campaignMapId.
 * Transcribed from VCMI's lib/campaign/CampaignHandler.cpp region tables — each
 * official campaign's region list has fixed length, so we only need the size.
 *
 * HotA campaigns supply numberOfScenarios in their own header field and never
 * consult this table.
 */
internal object CampaignRegionTable {
    // RoE (campaignMapId 0..3) + AB (4..5) + SoD (6..13). Sizes are the
    // number of regions/scenarios per campaign in VCMI's tables.
    private val SCENARIO_COUNT: Map<Int, Int> = mapOf(
        // RoE
        0 to 7, 1 to 6, 2 to 7, 3 to 7,
        // AB
        4 to 6, 5 to 6,
        // SoD
        6 to 8, 7 to 7, 8 to 7, 9 to 8, 10 to 7, 11 to 8, 12 to 7, 13 to 8,
    )

    /** @return scenario count for the given campaignMapId, or null if unknown. */
    fun scenarioCount(campaignMapId: Int): Int? = SCENARIO_COUNT[campaignMapId]
}
