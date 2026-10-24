package com.homm3.livewallpaper.core.library

sealed interface NextMapResult {
    data class Loaded(val fileName: String) : NextMapResult
    data object NotLoaded : NextMapResult
}
