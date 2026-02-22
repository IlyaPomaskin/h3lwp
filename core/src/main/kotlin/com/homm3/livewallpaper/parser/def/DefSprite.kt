package com.homm3.livewallpaper.parser.def

data class DefSprite(
    val type: Int,
    val fullWidth: Int,
    val fullHeight: Int,
    val rawPalette: ByteArray,
    val groups: List<DefGroup>
)

data class DefGroup(
    val groupType: Int,
    val filenames: List<String>,
    val frames: List<DefFrame>
)

data class DefFrame(
    val frameName: String,
    val width: Int,
    val height: Int,
    val fullWidth: Int,
    val fullHeight: Int,
    val x: Int,
    val y: Int,
    val data: ByteArray
)
