package com.homm3.livewallpaper.core.library

/** A change to the shown maps; [MapCommand]s run one at a time, in submission order. */
sealed interface MapCommand {
    /** Loads the initial batch unless maps are already loaded; shows asset setup when the LOD is missing. */
    data object Load : MapCommand

    /** Picks up added/removed map files; reloads everything when HotA availability changed. */
    data object Refresh : MapCommand

    /** Drops all maps and sprites and loads them again. */
    data object Reinit : MapCommand

    /** Swaps in the next batch if its rotation is due, or right away when [force]d. */
    data class Rotate(val force: Boolean) : MapCommand

    /** Loads the next not yet loaded map file in [direction], or cycles shown maps when all are loaded. */
    data class LoadNext(val direction: Direction) : MapCommand
}
