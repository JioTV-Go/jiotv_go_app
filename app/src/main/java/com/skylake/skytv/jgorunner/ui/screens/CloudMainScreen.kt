package com.skylake.skytv.jgorunner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skylake.skytv.jgorunner.data.model.CloudChannel
import com.skylake.skytv.jgorunner.ui.components.cloud.ChannelCard
import com.skylake.skytv.jgorunner.ui.components.cloud.FilterDialog
import com.skylake.skytv.jgorunner.ui.components.cloud.SearchColumn
import com.skylake.skytv.jgorunner.ui.viewmodel.CloudViewModel

@Composable
fun CloudMainScreen(
    onPlayChannel: (CloudChannel, List<CloudChannel>) -> Unit,
    onExit: () -> Unit,
    viewModel: CloudViewModel = viewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoadingChannels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    var showLanguageFilter by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val selectedLanguages by viewModel.selectedLanguages.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // COLUMN 1: Server List
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                IconButton(onClick = { viewModel.loadChannels(selectedServer?.url ?: "", true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                Spacer(modifier = Modifier.height(16.dp))
                servers.forEach { server ->
                    val isSelected = server == selectedServer
                    IconButton(
                        onClick = { viewModel.selectServer(server) },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(server.name.take(1))
                    }
                }
            }
        }

        // COLUMN 2: Settings + Filters
        Surface(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = selectedServer?.name ?: "No Server",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                item { CloudSettingsItem(Icons.Default.Search, "Search") { showSearch = !showSearch } }
                item { CloudSettingsItem(Icons.Default.Category, "Category Filter") { showCategoryFilter = true } }
                item { CloudSettingsItem(Icons.Default.Language, "Language Filter") { showLanguageFilter = true } }
                item {
                    CloudSettingsItem(Icons.Default.Refresh, "Refresh Channels") {
                        viewModel.loadChannels(selectedServer?.url ?: "", true)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    var autoplayFirst by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Checkbox(checked = autoplayFirst, onCheckedChange = { autoplayFirst = it })
                        Text("Autoplay first channel")
                    }
                }

                item {
                    var autoplayLast by remember { mutableStateOf(true) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Checkbox(checked = autoplayLast, onCheckedChange = { autoplayLast = it })
                        Text("Autoplay last channel")
                    }
                }

                item { CloudSettingsItem(Icons.Default.Settings, "Player Settings") { } }
                item { CloudSettingsItem(Icons.Default.Palette, "Theme Mode") { } }
                item { CloudSettingsItem(Icons.Default.ExitToApp, "Exit") { onExit() } }
            }
        }

        // COLUMN 3 (Optional): Search
        AnimatedVisibility(
            visible = showSearch,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            SearchColumn(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )
        }

        // COLUMN 4: Channels List
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Alignment.Center.let { Modifier.align(it) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onPlayChannel(channel, channels) }
                        )
                    }
                }
            }
        }
    }

    if (showCategoryFilter) {
        FilterDialog(
            title = "Categories",
            options = categories,
            selectedOptions = selectedCategories,
            onOptionToggled = { viewModel.toggleCategory(it) },
            onDismiss = { showCategoryFilter = false }
        )
    }

    if (showLanguageFilter) {
        FilterDialog(
            title = "Languages",
            options = languages,
            selectedOptions = selectedLanguages,
            onOptionToggled = { viewModel.toggleLanguage(it) },
            onDismiss = { showLanguageFilter = false }
        )
    }
}

@Composable
fun CloudSettingsItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
