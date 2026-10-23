package com.homm3.livewallpaper.core.assets

private data class DefAlias(val alias: String, val target: String, val archive: SpriteArchive)

/** DEFs that exist in no LOD; the game draws them with a target DEF from a specific archive. */
object DefAliases {
    private val aliases = listOf(
        DefAlias("tpbridg2.def", "tpbridg1.def", SpriteArchive.HOTA),
        DefAlias("tpbridg5.def", "tpbridg1.def", SpriteArchive.HOTA),
        DefAlias("tpbridg4.def", "tpbridg3.def", SpriteArchive.HOTA),
        DefAlias("tpbridg6.def", "tpbridg3.def", SpriteArchive.HOTA),
        DefAlias("zgenlam.def", "avtglam.def", SpriteArchive.HOTA),
        DefAlias("avwcoat.def", "avgcoatl.def", SpriteArchive.HOTA),
        DefAlias("avcgare.def", "avcgar10.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock01.def", "avlrk3d0.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock02.def", "avlrd04.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock03.def", "avlrd04.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock04.def", "avlrd04.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock05.def", "avlrk3d0.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock06.def", "avlrk3d0.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock07.def", "avlr04r0.def", SpriteArchive.H3SPRITE),
        DefAlias("udrock08.def", "avlr09r0.def", SpriteArchive.H3SPRITE),
    )

    /** Alias name → target name, both lowercase, for aliases whose target lives in [archive]. */
    fun targetsIn(archive: SpriteArchive): Map<String, String> =
        aliases.filter { it.archive == archive }.associate { it.alias to it.target }
}
