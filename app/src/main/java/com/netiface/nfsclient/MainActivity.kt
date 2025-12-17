package com.netiface.nfsclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netiface.nfsclient.ui.theme.NetifaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetifaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NetifaceApp()
                }
            }
        }
    }
}

@Composable
fun NetifaceApp(viewModel: NfsViewModel = viewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showFileBrowser by remember { mutableStateOf(false) }
    
    if (connectionState.isConnected && showFileBrowser) {
        FileBrowserScreen(
            files = files,
            currentPath = currentPath,
            isLoading = isLoading,
            onFileClick = { file ->
                if (file.isDirectory && file.name != "..") {
                    viewModel.loadDirectory(file.path)
                }
            },
            onNavigateUp = {
                if (currentPath == "/" || currentPath.isEmpty()) {
                    // At root, go back to connection screen
                    showFileBrowser = false
                    viewModel.disconnect()
                } else {
                    viewModel.navigateUp()
                }
            },
            onDisconnect = {
                showFileBrowser = false
                viewModel.disconnect()
            },
            onRefresh = {
                viewModel.loadDirectory(currentPath)
            }
        )
    } else {
        ConnectionScreen(
            connectionState = connectionState,
            onConnect = { server, share ->
                viewModel.connect(server, share)
            },
            onDisconnect = {
                viewModel.disconnect()
            },
            onNavigateToFiles = {
                showFileBrowser = true
            },
            onErrorDismissed = {
                viewModel.clearError()
            }
        )
    }
}
