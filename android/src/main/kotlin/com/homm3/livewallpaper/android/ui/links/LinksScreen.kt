package com.homm3.livewallpaper.android.ui.links

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.ui.components.BackButton
import com.homm3.livewallpaper.android.ui.components.Preference
import com.homm3.livewallpaper.android.ui.theme.H3WallpaperTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_links)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item { LinkPreference(stringResource(R.string.source_code_title), stringResource(R.string.source_code_url)) }
            item { LinkPreference(stringResource(R.string.icon_author_title), stringResource(R.string.icon_author_url)) }
        }
    }
}

@Composable
private fun LinkPreference(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Preference(
        title = title,
        summary = url,
        trailingContent = { Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null) },
        onClick = { uriHandler.openUri(url) },
    )
}

@Preview
@Composable
private fun LinksScreenPreview() {
    H3WallpaperTheme {
        LinksScreen(onBack = {})
    }
}
