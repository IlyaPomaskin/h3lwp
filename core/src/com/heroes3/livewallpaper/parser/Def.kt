package com.heroes3.livewallpaper.parser

internal class Def {
    lateinit var lodFile: Lod.File
    var type = 0
    var fullWidth = 0
    var fullHeight = 0
    var groupsCount = 0
    var rawPalette = ByteArray(256 * 3)
    var groups: MutableList<Group> = mutableListOf()

    internal class Group {
        lateinit var parentDef: Def
        var groupType = 0
        var framesCount = 0
        var unknown = ByteArray(8)
        var filenames: MutableList<String> = mutableListOf()
        var framesOffsets: IntArray = intArrayOf()
        var frames: MutableList<Frame> = mutableListOf()
        var legacy = false
    }

    internal class Frame {
        lateinit var parentGroup: Group
        lateinit var frameName: String
        var size = 0
        var compression = 0
        var fullWidth = 0
        var fullHeight = 0
        var width = 0
        var height = 0
        var x = 0
        var y = 0
        var dataOffset: Long = 0
        lateinit var data: ByteArray
    }
}