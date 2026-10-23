package com.homm3.livewallpaper.core.install

sealed interface InstallResult {
    val message: String

    data class Success(override val message: String) : InstallResult

    data class Failure(override val message: String) : InstallResult
}
