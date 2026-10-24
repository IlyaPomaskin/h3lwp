package com.homm3.livewallpaper.core.library

/** A change to the shown maps; [MapCommand]s run one at a time, in submission order. */
sealed interface MapCommand {
    /** Loads a random map unless one is loaded; shows asset setup when the LOD is missing. */
    data object Load : MapCommand

    /** Picks up added/removed map files; reloads everything when HotA availability changed. */
    data object Refresh : MapCommand

    /** Drops all maps and sprites and loads them again. */
    data object Reinit : MapCommand

    /** Replaces the map with another random one. */
    data object Rotate : MapCommand

    /** Loads the map file next to the current one in [direction] in its place. */
    data class LoadNext(val direction: Direction) : MapCommand
}
