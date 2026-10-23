package com.homm3.livewallpaper.parser.h3m

enum class SeerHutRewardType(val id: Int) {
    NOTHING(0),
    EXPERIENCE(1),
    MANA_POINTS(2),
    MORALE_BONUS(3),
    LUCK_BONUS(4),
    RESOURCES(5),
    PRIMARY_SKILL(6),
    SECONDARY_SKILL(7),
    ARTIFACT(8),
    SPELL(9),
    CREATURE(10);

    companion object {
        fun fromInt(value: Int?): SeerHutRewardType {
            return entries.find { it.id == value }
                ?: throw IllegalArgumentException("Unknown SeerHutRewardType: $value")
        }
    }
}
