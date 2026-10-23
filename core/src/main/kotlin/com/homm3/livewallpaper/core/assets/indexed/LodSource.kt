package com.homm3.livewallpaper.core.assets.indexed

import com.homm3.livewallpaper.parser.lod.LodDataSource

/** A LOD archive to read sprites from; sources are searched in list order (HotA.lod first). */
class LodSource(val name: String, val open: () -> LodDataSource, val aliases: Map<String, String>)
