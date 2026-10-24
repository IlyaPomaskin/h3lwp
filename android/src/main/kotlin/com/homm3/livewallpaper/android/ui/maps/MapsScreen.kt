package com.homm3.livewallpaper.android.ui.maps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.appContainer
import com.homm3.livewallpaper.android.data.MapImportResult
import com.homm3.livewallpaper.android.ui.components.BackButton
import com.homm3.livewallpaper.android.ui.theme.H3WallpaperTheme

private const val ANY_FILE_MIME_TYPE = "*/*"

@Composable
fun MapsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel { MapsViewModel(context.appContainer.mapsRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickMap = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importMap(uri)
    }

    MapsScreen(
        uiState = uiState,
        onBack = onBack,
        onAddMap = { pickMap.launch(arrayOf(ANY_FILE_MIME_TYPE)) },
        onDeleteMap = viewModel::deleteMap,
        onImportResultShown = viewModel::onImportResultShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    uiState: MapsUiState,
    onBack: () -> Unit,
    onAddMap: () -> Unit,
    onDeleteMap: (String) -> Unit,
    onImportResultShown: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var mapPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }

    uiState.importResult?.let { result ->
        val message = importResultMessage(result)
        LaunchedEffect(result) {
            snackbarHostState.showSnackbar(message)
            onImportResultShown()
        }
    }

    mapPendingDeletion?.let { mapName ->
        DeleteMapDialog(
            mapName = mapName,
            onConfirm = {
                mapPendingDeletion = null
                onDeleteMap(mapName)
            },
            onDismiss = { mapPendingDeletion = null },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.maps_title)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMap) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.maps_add_button))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            if (uiState.isImporting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Text(
                text = stringResource(R.string.maps_note),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (uiState.maps.isEmpty()) {
                EmptyMaps()
            } else {
                MapList(
                    maps = uiState.maps,
                    canDelete = uiState.canDeleteMaps,
                    onDelete = { mapPendingDeletion = it },
                )
            }
        }
    }
}

@Composable
private fun MapList(maps: List<String>, canDelete: Boolean, onDelete: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(maps, key = { it }) { mapName ->
            ListItem(
                headlineContent = { Text(mapName) },
                modifier = Modifier.animateItem(),
                leadingContent = { Icon(painterResource(R.drawable.ic_map), contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onDelete(mapName) }, enabled = canDelete) {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.maps_delete_description, mapName),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyMaps() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.maps_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DeleteMapDialog(mapName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.maps_delete_title)) },
        text = { Text(stringResource(R.string.maps_delete_text, mapName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun importResultMessage(result: MapImportResult): String = when (result) {
    is MapImportResult.Success -> stringResource(R.string.maps_import_success, result.mapName)
    MapImportResult.CannotOpen -> stringResource(R.string.maps_import_error_cannot_open)
    MapImportResult.TooSmall -> stringResource(R.string.maps_import_error_too_small)
    is MapImportResult.UnknownVersion -> stringResource(R.string.maps_import_error_unknown_version, result.versionHex)
    is MapImportResult.Invalid -> stringResource(R.string.maps_import_error_invalid, result.details)
    is MapImportResult.TooLarge -> stringResource(R.string.maps_import_error_too_large, result.size, result.maxSize)
}

@Preview
@Composable
private fun MapsScreenPreview() {
    H3WallpaperTheme {
        MapsScreen(
            uiState = MapsUiState(maps = listOf("Arrogance.h3m", "Dragon Orb.h3m")),
            onBack = {},
            onAddMap = {},
            onDeleteMap = {},
            onImportResultShown = {},
        )
    }
}
