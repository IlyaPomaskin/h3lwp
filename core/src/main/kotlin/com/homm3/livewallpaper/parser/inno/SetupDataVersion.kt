package com.homm3.livewallpaper.parser.inno

/**
 * Supported unicode setup-data versions. 5.6.1 added the ChangesEnvironment and
 * ChangesAssociations header strings (and dropped the matching option flags, which
 * leaves the option-flag set at 6 bytes), so 5.6.2 has two more header strings.
 */
enum class SetupDataVersion(val setupIdPrefix: String, val headerStringCount: Int) {
    V5_5_7("Inno Setup Setup Data (5.5.7)", 31),
    V5_6_2("Inno Setup Setup Data (5.6.2)", 33);

    companion object {
        private const val UNICODE_MARKER = "(u)"

        fun fromSetupId(setupId: String): SetupDataVersion {
            val isUnicode = setupId.contains(UNICODE_MARKER)
            val version = entries.firstOrNull { setupId.startsWith(it.setupIdPrefix) }
            require(isUnicode && version != null) {
                "Unsupported Inno Setup version. Expected 5.5.7 or 5.6.2 unicode, got: '$setupId'"
            }
            return version
        }
    }
}
