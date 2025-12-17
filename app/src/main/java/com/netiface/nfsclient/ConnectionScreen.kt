package com.netiface.nfsclient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    onConnect: (String, String) -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    var serverAddress by remember { mutableStateOf("192.168.1.100") }
    var sharePath by remember { mutableStateOf("/export/share") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Show error dialog
    connectionState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = onErrorDismissed,
            title = { Text("Connection Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = onErrorDismissed) {
                    Text("OK")
                }
            }
        )
    }
    
    // Navigate to file browser when connected
    LaunchedEffect(connectionState.isConnected) {
        if (connectionState.isConnected) {
            onNavigateToFiles()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFS Connection") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Server Address
            OutlinedTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it },
                label = { Text("Server Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                singleLine = true,
                enabled = !connectionState.isConnecting,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Share Path
            OutlinedTextField(
                value = sharePath,
                onValueChange = { sharePath = it },
                label = { Text("Share Path") },
                placeholder = { Text("/export/share") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                singleLine = true,
                enabled = !connectionState.isConnecting,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Username (Optional)
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (optional)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                enabled = !connectionState.isConnecting,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Password (Optional)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (optional)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !connectionState.isConnecting,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Connect Button
            Button(
                onClick = {
                    if (serverAddress.isNotBlank() && sharePath.isNotBlank()) {
                        onConnect(serverAddress, sharePath)
                    }
                },
                enabled = !connectionState.isConnecting && 
                         serverAddress.isNotBlank() && 
                         sharePath.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (connectionState.isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (connectionState.isConnecting) "Connecting..." else "Connect")
            }
            
            // Status Text
            if (connectionState.isConnecting) {
                Text(
                    text = "Connecting to ${connectionState.serverAddress}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
