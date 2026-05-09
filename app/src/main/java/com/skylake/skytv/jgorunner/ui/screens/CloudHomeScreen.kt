package com.skylake.skytv.jgorunner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skylake.skytv.jgorunner.ui.components.cloud.ServerCard
import com.skylake.skytv.jgorunner.ui.viewmodel.CloudViewModel

@Composable
fun CloudHomeScreen(
    onNavigateToMain: () -> Unit,
    viewModel: CloudViewModel = viewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val countdown by viewModel.autoplayCountdown.collectAsState()
    var autoplayEnabled by remember { mutableStateOf(viewModel.isAutoplayEnabled()) }

    LaunchedEffect(viewModel.selectedServer.collectAsState().value) {
        if (viewModel.selectedServer.value != null) {
            onNavigateToMain()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cloud Play",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select a server to start streaming",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (servers.isEmpty()) {
                CircularProgressIndicator()
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(servers) { server ->
                        ServerCard(
                            server = server,
                            onClick = { viewModel.selectServer(server) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Autoplay first server")
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = autoplayEnabled,
                    onCheckedChange = {
                        autoplayEnabled = it
                        viewModel.setAutoplayEnabled(it)
                    }
                )
            }

            if (countdown != null && countdown!! > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Autostarting in $countdown seconds...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
