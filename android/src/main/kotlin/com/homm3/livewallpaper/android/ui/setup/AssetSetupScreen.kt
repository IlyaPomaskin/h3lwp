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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
private val MAC_DEMO_MIME_TYPES = arrayOf("*/*")
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
        onSelectMacDemo = { pickBaseLod.launch(MAC_DEMO_MIME_TYPES) },
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
    onSelectMacDemo: () -> Unit,
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
            Disclaimer()
            AssetCard(
                title = stringResource(R.string.asset_setup_base_title),
                description = stringResource(R.string.asset_setup_base_description),
                choicePrompt = stringResource(R.string.asset_setup_base_choose),
                replaceButtonText = stringResource(R.string.asset_setup_base_replace_button),
                isInstalled = uiState.status.isBaseInstalled,
                task = uiState.base,
                isSelectEnabled = !uiState.isBusy,
                options = listOf(
                    AssetOption(
                        title = stringResource(R.string.asset_setup_base_gog_title),
                        description = stringResource(R.string.asset_setup_base_gog_description),
                        descriptionLink = DescriptionLink(
                            label = stringResource(R.string.gog_site_label),
                            url = stringResource(R.string.gog_site_url),
                        ),
                        buttonText = stringResource(R.string.asset_setup_base_gog_button),
                        onSelect = onSelectGogInstaller,
                    ),
                    AssetOption(
                        title = stringResource(R.string.asset_setup_base_demo_title),
                        description = stringResource(R.string.asset_setup_base_demo_description),
                        descriptionLink = DescriptionLink(
                            label = stringResource(R.string.demo_site_label),
                            url = stringResource(R.string.demo_site_url),
                        ),
                        buttonText = stringResource(R.string.asset_setup_base_demo_button),
                        onSelect = onSelectMacDemo,
                    ),
                    AssetOption(
                        title = stringResource(R.string.asset_setup_base_lod_title),
                        description = stringResource(R.string.asset_setup_base_lod_description),
                        buttonText = stringResource(R.string.asset_setup_base_button),
                        onSelect = onSelectBaseLod,
                    ),
                ),
            )
            AssetCard(
                title = stringResource(R.string.asset_setup_hota_title),
                description = stringResource(R.string.asset_setup_hota_description),
                descriptionLink = DescriptionLink(
                    label = stringResource(R.string.hota_site_label),
                    url = stringResource(R.string.hota_site_url),
                ),
                isInstalled = uiState.status.isHotaInstalled,
                task = uiState.hota,
                isSelectEnabled = uiState.canInstallHota,
                options = listOf(
                    AssetOption(
                        buttonText = stringResource(R.string.asset_setup_hota_button),
                        onSelect = onSelectHotaInstaller,
                    ),
                ),
                modifier = Modifier.greyedOut(!uiState.status.isBaseInstalled),
            )
        }
    }
}

@Composable
private fun AssetCard(
    title: String,
    description: String,
    isInstalled: Boolean,
    task: InstallTaskState,
    isSelectEnabled: Boolean,
    options: List<AssetOption>,
    modifier: Modifier = Modifier,
    descriptionLink: DescriptionLink? = null,
    choicePrompt: String? = null,
    replaceButtonText: String? = null,
) {
    var isReplacing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(task.isRunning) {
        val hasSucceeded = !task.isRunning && task.message != null && !task.isFailure
        if (hasSucceeded) isReplacing = false
    }
    val isCollapsed = replaceButtonText != null && isInstalled && !isReplacing

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (isInstalled) InstalledBadge()
            }
            if (isCollapsed) {
                TaskStatus(task)
                OutlinedButton(onClick = { isReplacing = true }, enabled = isSelectEnabled) {
                    Text(replaceButtonText.orEmpty())
                }
                return@Column
            }

            DescriptionText(description, descriptionLink)
            choicePrompt?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            TaskStatus(task)
            options.forEachIndexed { index, option ->
                if (index > 0) OrDivider()
                OptionSection(option, isSelectEnabled)
            }
        }
    }
}

@Composable
private fun OptionSection(option: AssetOption, isSelectEnabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        option.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
        option.description?.let { DescriptionText(it, option.descriptionLink) }
        OutlinedButton(onClick = option.onSelect, enabled = isSelectEnabled) { Text(option.buttonText) }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.asset_setup_or),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun DescriptionText(description: String, link: DescriptionLink?) {
    Text(
        text = linkifyDescription(description, listOfNotNull(link)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun Modifier.greyedOut(isGreyedOut: Boolean): Modifier {
    if (!isGreyedOut) return this
    return alpha(DISABLED_CARD_ALPHA)
}

private data class AssetOption(
    val buttonText: String,
    val onSelect: () -> Unit,
    val title: String? = null,
    val description: String? = null,
    val descriptionLink: DescriptionLink? = null,
)

private data class DescriptionLink(val label: String, val url: String)

@Composable
private fun linkifyDescription(description: String, links: List<DescriptionLink>): AnnotatedString {
    val placedLinks = links
        .map { link -> link to description.indexOf(link.label) }
        .filter { (_, start) -> start >= 0 }
        .sortedBy { (_, start) -> start }
    if (placedLinks.isEmpty()) return AnnotatedString(description)

    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    return buildAnnotatedString {
        val textEnd = placedLinks.fold(0) { textStart, (link, linkStart) ->
            append(description.substring(textStart, linkStart))
            withLink(LinkAnnotation.Url(link.url, linkStyles)) { append(link.label) }
            linkStart + link.label.length
        }
        append(description.substring(textEnd))
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
private fun Disclaimer() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.disclaimer_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = stringResource(R.string.disclaimer_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
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
            onSelectMacDemo = {},
            onSelectGogInstaller = {},
            onSelectHotaInstaller = {},
        )
    }
}
