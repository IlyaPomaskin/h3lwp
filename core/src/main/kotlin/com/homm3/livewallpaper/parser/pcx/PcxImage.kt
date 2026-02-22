package com.homm3.livewallpaper.parser.pcx

data class PcxImage(
    val width: Int,
    val height: Int,
    val palette: ByteArray?,
    val data: ByteArray
)
