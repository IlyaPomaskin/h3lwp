package com.homm3.livewallpaper.fhParser

data class FhSpriteRoot(
    val boundarySize: Size,
    val groups: List<Group>
)

data class Size(
    val h: Int,
    val w: Int
)

data class Group(
    val key: Int,
    val value: GroupValue
)

data class GroupValue(
    val frames: List<Frame>,
    val params: Map<String, Any> = emptyMap()
)

data class Frame(
    val bitmapSize: Size? = Size(0, 0),
    val boundarySize: Size? = Size(0, 0),
    val bitmapOffset: Offset? = Offset(0, 0),
    val padding: Offset? = Offset(0, 0)
)

data class Offset(
    val x: Int = 0,
    val y: Int = 0
)