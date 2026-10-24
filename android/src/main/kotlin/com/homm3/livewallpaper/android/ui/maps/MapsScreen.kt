package com.homm3.livewallpaper.android.ui.maps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val EXPANDED_CHEVRON_ROTATION = 90f
private const val FAB_SPACER_KEY = "fab_spacer"
private val FAB_CLEARANCE = 88.dp
private val DEFAULT_COLLAPSED_GROUPS = setOf(MapGroupId.SOD_BASE, MapGroupId.HOTA_BASE)

@Composable
fun MapsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel {
        MapsViewModel(context.appContainer.mapsRepository, context.appContainer.gameDataRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickMaps = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
        viewModel::importMaps,
    )

    MapsScreen(
        uiState = uiState,
        onBack = onBack,
        onAddMap = { pickMaps.launch(arrayOf(ANY_FILE_MIME_TYPE)) },
        onDeleteMap = viewModel::deleteMap,
        onMapEnabledChange = viewModel::setMapEnabled,
        onGroupEnabledChange = viewModel::setGroupEnabled,
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
    onMapEnabledChange: (String, Boolean) -> Unit,
    onGroupEnabledChange: (MapGroup, Boolean) -> Unit,
    onImportResultShown: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var mapPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }

    val importResults = uiState.importResults
    if (importResults.isNotEmpty()) {
        val message = importResultsMessage(importResults)
        LaunchedEffect(importResults) {
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
            MapList(
                groups = uiState.groups,
                onMapEnabledChange = onMapEnabledChange,
                onGroupEnabledChange = onGroupEnabledChange,
                onDelete = { mapPendingDeletion = it },
            )
        }
    }
}

@Composable
private fun MapList(
    groups: List<MapGroup>,
    onMapEnabledChange: (String, Boolean) -> Unit,
    onGroupEnabledChange: (MapGroup, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    var collapsedGroups by rememberSaveable { mutableStateOf(DEFAULT_COLLAPSED_GROUPS) }
    val onToggleExpanded = { groupId: MapGroupId ->
        val isCollapsed = groupId in collapsedGroups
        collapsedGroups = if (isCollapsed) collapsedGroups - groupId else collapsedGroups + groupId
    }

    LazyColumn(Modifier.fillMaxSize()) {
        groups.forEach { group ->
            mapGroup(
                group = group,
                isExpanded = group.id !in collapsedGroups,
                onToggleExpanded = onToggleExpanded,
                onMapEnabledChange = onMapEnabledChange,
                onGroupEnabledChange = onGroupEnabledChange,
                onDelete = onDelete,
            )
        }
        item(key = FAB_SPACER_KEY) { Spacer(Modifier.height(FAB_CLEARANCE)) }
    }
}

private fun LazyListScope.mapGroup(
    group: MapGroup,
    isExpanded: Boolean,
    onToggleExpanded: (MapGroupId) -> Unit,
    onMapEnabledChange: (String, Boolean) -> Unit,
    onGroupEnabledChange: (MapGroup, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    item(key = group.id) {
        MapGroupHeader(
            group = group,
            isExpanded = isExpanded,
            onToggleExpanded = { onToggleExpanded(group.id) },
            onEnabledChange = { onGroupEnabledChange(group, it) },
            modifier = Modifier.animateItem(),
        )
    }
    if (!isExpanded) return

    items(group.maps, key = { it.fileName }) { map ->
        MapRow(
            map = map,
            onEnabledChange = { onMapEnabledChange(map.fileName, it) },
            onDelete = { onDelete(map.fileName) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun MapGroupHeader(
    group: MapGroup,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) EXPANDED_CHEVRON_ROTATION else 0f,
        label = "chevronRotation",
    )
    val supportingText = when {
        group.maps.isEmpty() -> stringResource(R.string.maps_user_empty)
        group.isAvailable -> stringResource(R.string.maps_group_enabled_count, group.enabledCount, group.maps.size)
        else -> stringResource(R.string.maps_hota_required)
    }
    val hasAvailableContent = !group.isMissingAssets

    ListItem(
        headlineContent = { Text(stringResource(group.id.titleRes)) },
        modifier = modifier.clickable(onClick = onToggleExpanded),
        supportingContent = { Text(supportingText) },
        leadingContent = {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.rotate(chevronRotation),
            )
        },
        trailingContent = {
            Switch(checked = group.isEnabled, onCheckedChange = onEnabledChange, enabled = group.canToggle)
        },
        colors = availabilityColors(hasAvailableContent),
    )
}

@Composable
private fun MapRow(
    map: MapItem,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isChecked = map.isAvailable && map.isEnabled
    ListItem(
        headlineContent = { Text(map.displayName) },
        modifier = modifier.toggleable(
            value = isChecked,
            enabled = map.canToggle,
            role = Role.Switch,
            onValueChange = onEnabledChange,
        ),
        leadingContent = { Icon(painterResource(R.drawable.ic_map), contentDescription = null) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (map.canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.maps_delete_description, map.displayName),
                        )
                    }
                }
                Switch(checked = isChecked, onCheckedChange = null, enabled = map.canToggle)
            }
        },
        colors = availabilityColors(map.isAvailable),
    )
}

@Composable
private fun availabilityColors(isAvailable: Boolean): ListItemColors {
    if (isAvailable) return ListItemDefaults.colors()

    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    return ListItemDefaults.colors(
        headlineColor = disabledColor,
        supportingColor = disabledColor,
        leadingIconColor = disabledColor,
        trailingIconColor = disabledColor,
    )
}

private val MapGroupId.titleRes: Int
    get() = when (this) {
        MapGroupId.SOD_BASE -> R.string.maps_group_sod_base
        MapGroupId.HOTA_BASE -> R.string.maps_group_hota_base
        MapGroupId.USER -> R.string.maps_group_user
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
private fun importResultsMessage(results: List<MapImportResult>): String {
    val singleResult = results.singleOrNull()
    if (singleResult != null) return importResultMessage(singleResult)
    val addedCount = results.count { it is MapImportResult.Success }
    return stringResource(R.string.maps_import_summary, addedCount, results.size)
}

@Composable
private fun importResultMessage(result: MapImportResult): String = when (result) {
    is MapImportResult.Success -> stringResource(R.string.maps_import_success, result.mapName)
    MapImportResult.CannotOpen -> stringResource(R.string.maps_import_error_cannot_open)
    MapImportResult.TooSmall -> stringResource(R.string.maps_import_error_too_small)
    is MapImportResult.UnknownVersion -> stringResource(R.string.maps_import_error_unknown_version, result.versionHex)
    MapImportResult.UnsupportedVersion -> stringResource(R.string.maps_import_error_unsupported_version)
    is MapImportResult.Invalid -> stringResource(R.string.maps_import_error_invalid, result.details)
}

@Preview
@Composable
private fun MapsScreenPreview() {
    H3WallpaperTheme {
        MapsScreen(
            uiState = MapsUiState(
                groups = listOf(
                    MapGroup(
                        id = MapGroupId.SOD_BASE,
                        maps = listOf(
                            previewMap("[SoD] Arrogance.h3m", "Arrogance"),
                            previewMap("[SoD] Dragon Orb.h3m", "Dragon Orb", isEnabled = false),
                        ),
                        activeMapCount = PREVIEW_ACTIVE_MAP_COUNT,
                    ),
                    MapGroup(
                        id = MapGroupId.HOTA_BASE,
                        maps = listOf(previewMap("[HotA] Jebus.h3m", "Jebus", isAvailable = false)),
                        activeMapCount = PREVIEW_ACTIVE_MAP_COUNT,
                    ),
                    MapGroup(
                        id = MapGroupId.USER,
                        maps = listOf(previewMap("My map.h3m", "My map", canDelete = true)),
                        activeMapCount = PREVIEW_ACTIVE_MAP_COUNT,
                    ),
                ),
            ),
            onBack = {},
            onAddMap = {},
            onDeleteMap = {},
            onMapEnabledChange = { _, _ -> },
            onGroupEnabledChange = { _, _ -> },
            onImportResultShown = {},
        )
    }
}

private fun previewMap(
    fileName: String,
    displayName: String,
    isEnabled: Boolean = true,
    isAvailable: Boolean = true,
    canDelete: Boolean = false,
) = MapItem(
    fileName = fileName,
    displayName = displayName,
    isEnabled = isEnabled,
    isAvailable = isAvailable,
    canDelete = canDelete,
    canToggle = isAvailable,
)

private const val PREVIEW_ACTIVE_MAP_COUNT = 2
