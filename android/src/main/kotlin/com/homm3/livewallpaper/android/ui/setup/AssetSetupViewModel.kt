package com.homm3.livewallpaper.android.ui.setup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homm3.livewallpaper.android.data.GameDataRepository
import com.homm3.livewallpaper.android.data.GameDataStatus
import com.homm3.livewallpaper.core.install.InstallProgress
import com.homm3.livewallpaper.core.install.InstallResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class InstallTaskState(
    val isRunning: Boolean = false,
    val message: String? = null,
    val isFailure: Boolean = false,
)

data class AssetSetupUiState(
    val status: GameDataStatus,
    val base: InstallTaskState = InstallTaskState(),
    val hota: InstallTaskState = InstallTaskState(),
) {
    val isBusy: Boolean get() = base.isRunning || hota.isRunning
    val canInstallHota: Boolean get() = status.isBaseInstalled && !isBusy
    val canContinue: Boolean get() = status.isBaseInstalled && !isBusy
}

private typealias TaskUpdate = (AssetSetupUiState, InstallTaskState) -> AssetSetupUiState

class AssetSetupViewModel(private val repository: GameDataRepository) : ViewModel() {

    private val mutableState = MutableStateFlow(AssetSetupUiState(status = repository.status.value))

    val uiState: StateFlow<AssetSetupUiState> = combine(repository.status, mutableState) { status, state ->
        state.copy(status = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), mutableState.value)

    fun installBase(uri: Uri) = launchInstall(
        updateTask = { state, task -> state.copy(base = task) },
        install = { progress -> repository.installBase(uri, progress) },
    )

    fun installBaseFromGog(uris: List<Uri>) = launchInstall(
        updateTask = { state, task -> state.copy(base = task) },
        install = { progress -> repository.installBaseFromGog(uris, progress) },
    )

    fun installHota(uri: Uri) = launchInstall(
        updateTask = { state, task -> state.copy(hota = task) },
        install = { progress -> repository.installHota(uri, progress) },
    )

    private fun launchInstall(
        updateTask: TaskUpdate,
        install: suspend (InstallProgress) -> InstallResult,
    ) {
        if (mutableState.value.isBusy) return

        mutableState.update { updateTask(it, InstallTaskState(isRunning = true)) }
        viewModelScope.launch {
            val result = install { message ->
                mutableState.update { updateTask(it, InstallTaskState(isRunning = true, message = message)) }
            }
            val isSuccess = result is InstallResult.Success
            mutableState.update { updateTask(it, InstallTaskState(message = result.message, isFailure = !isSuccess)) }
        }
    }
}
