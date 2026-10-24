package com.homm3.livewallpaper.android.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.appContainer
import com.homm3.livewallpaper.android.data.GameDataStatus
import com.homm3.livewallpaper.android.ui.components.BackButton
import com.homm3.livewallpaper.android.ui.theme.H3WallpaperTheme

private const val DISABLED_CARD_ALPHA = 0.38f
private val BASE_LOD_MIME_TYPES = arrayOf("*/*")
private val GOG_INSTALLER_MIME_TYPES = arrayOf("*/*")
private val HOTA_INSTALLER_MIME_TYPES = arrayOf(
    "application/x-msdownload",
    "application/x-msdos-program",
    "application/vnd.microsoft.portable-executable",
    "application/octet-stream",
    "application/x-ms-installer",
    "application/*",
)

@Composable
fun AssetSetupRoute(onBack: (() -> Unit)?, onContinue: (() -> Unit)?) {
    val context = LocalContext.current
    val viewModel = viewModel { AssetSetupViewModel(context.appContainer.gameDataRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickBaseLod = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.installBase(uri)
    }
    val pickGogInstaller = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.installBaseFromGog(uris)
    }
    val pickHotaInstaller = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.installHota(uri)
    }

    AssetSetupScreen(
        uiState = uiState,
        onBack = onBack,
        onContinue = onContinue,
        onSelectBaseLod = { pickBaseLod.launch(BASE_LOD_MIME_TYPES) },
        onSelectGogInstaller = { pickGogInstaller.launch(GOG_INSTALLER_MIME_TYPES) },
        onSelectHotaInstaller = { pickHotaInstaller.launch(HOTA_INSTALLER_MIME_TYPES) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSetupScreen(
    uiState: AssetSetupUiState,
    onBack: (() -> Unit)?,
    onContinue: (() -> Unit)?,
    onSelectBaseLod: () -> Unit,
    onSelectGogInstaller: () -> Unit,
    onSelectHotaInstaller: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.asset_setup_title)) },
                navigationIcon = { onBack?.let { BackButton(it) } },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            onContinue?.let { ContinueBar(isEnabled = uiState.canContinue, onClick = it) }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AssetCard(
                title = stringResource(R.string.asset_setup_base_title),
                description = stringResource(R.string.asset_setup_base_description),
                buttonText = stringResource(R.string.asset_setup_base_button),
                isInstalled = uiState.status.isBaseInstalled,
                task = uiState.base,
                isSelectEnabled = !uiState.isBusy,
                onSelect = onSelectBaseLod,
                secondaryAction = AssetAction(
                    text = stringResource(R.string.asset_setup_base_gog_button),
                    onClick = onSelectGogInstaller,
                ),
                descriptionLink = DescriptionLink(
                    label = stringResource(R.string.gog_site_label),
                    url = stringResource(R.string.gog_site_url),
                ),
            )
            AssetCard(
                title = stringResource(R.string.asset_setup_hota_title),
                description = stringResource(R.string.asset_setup_hota_description),
                buttonText = stringResource(R.string.asset_setup_hota_button),
                isInstalled = uiState.status.isHotaInstalled,
                task = uiState.hota,
                isSelectEnabled = uiState.canInstallHota,
                onSelect = onSelectHotaInstaller,
                modifier = Modifier.greyedOut(!uiState.status.isBaseInstalled),
                descriptionLink = DescriptionLink(
                    label = stringResource(R.string.hota_site_label),
                    url = stringResource(R.string.hota_site_url),
                ),
            )
        }
    }
}

@Composable
private fun AssetCard(
    title: String,
    description: String,
    buttonText: String,
    isInstalled: Boolean,
    task: InstallTaskState,
    isSelectEnabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: AssetAction? = null,
    descriptionLink: DescriptionLink? = null,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (isInstalled) InstalledBadge()
            }
            Text(
                text = linkifyDescription(description, descriptionLink),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TaskStatus(task)
            if (isInstalled) {
                FilledTonalButton(onClick = onSelect, enabled = isSelectEnabled) { Text(buttonText) }
            } else {
                Button(onClick = onSelect, enabled = isSelectEnabled) { Text(buttonText) }
            }
            secondaryAction?.let { action ->
                OutlinedButton(onClick = action.onClick, enabled = isSelectEnabled) { Text(action.text) }
            }
        }
    }
}

private fun Modifier.greyedOut(isGreyedOut: Boolean): Modifier {
    if (!isGreyedOut) return this
    return alpha(DISABLED_CARD_ALPHA)
}

private data class AssetAction(val text: String, val onClick: () -> Unit)

private data class DescriptionLink(val label: String, val url: String)

@Composable
private fun linkifyDescription(description: String, link: DescriptionLink?): AnnotatedString {
    val linkStart = link?.let { description.indexOf(it.label) } ?: -1
    if (link == null || linkStart < 0) return AnnotatedString(description)

    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    val linkEnd = linkStart + link.label.length
    return buildAnnotatedString {
        append(description.substring(0, linkStart))
        withLink(LinkAnnotation.Url(link.url, linkStyles)) { append(link.label) }
        append(description.substring(linkEnd))
    }
}

@Composable
private fun ContinueBar(isEnabled: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.asset_setup_continue_button))
        }
    }
}

@Composable
private fun InstalledBadge() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.asset_setup_installed),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TaskStatus(task: InstallTaskState) {
    if (task.isRunning) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    val message = task.message ?: return
    val color = if (task.isFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Text(message, color = color, style = MaterialTheme.typography.bodySmall)
}

@Preview
@Composable
private fun AssetSetupScreenPreview() {
    H3WallpaperTheme {
        AssetSetupScreen(
            uiState = AssetSetupUiState(
                status = GameDataStatus(isBaseInstalled = true, isHotaInstalled = false),
                hota = InstallTaskState(isRunning = true, message = "Extracting: 42%"),
            ),
            onBack = null,
            onContinue = {},
            onSelectBaseLod = {},
            onSelectGogInstaller = {},
            onSelectHotaInstaller = {},
        )
    }
}
