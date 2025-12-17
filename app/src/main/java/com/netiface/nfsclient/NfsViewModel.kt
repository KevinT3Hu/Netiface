package com.netiface.nfsclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NfsViewModel : ViewModel() {
    private val nfsClient = NfsClient()
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _files = MutableStateFlow<List<NfsFileInfo>>(emptyList())
    val files: StateFlow<List<NfsFileInfo>> = _files.asStateFlow()
    
    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun connect(server: String, sharePath: String) {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(
                isConnecting = true,
                errorMessage = null
            )
            
            val result = withContext(Dispatchers.IO) {
                nfsClient.connect(server, sharePath)
            }
            
            when (result) {
                is NfsResult.Success -> {
                    _connectionState.value = ConnectionState(
                        isConnected = true,
                        isConnecting = false,
                        serverAddress = server,
                        sharePath = sharePath
                    )
                    loadDirectory("/")
                }
                is NfsResult.Error -> {
                    _connectionState.value = ConnectionState(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                nfsClient.disconnect()
            }
            _connectionState.value = ConnectionState()
            _files.value = emptyList()
            _currentPath.value = "/"
        }
    }
    
    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = withContext(Dispatchers.IO) {
                nfsClient.listDirectory(path)
            }
            
            when (result) {
                is NfsResult.Success -> {
                    _files.value = result.data.sortedWith(
                        compareBy<NfsFileInfo> { !it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
                    _currentPath.value = path
                }
                is NfsResult.Error -> {
                    // Handle error - could add error state
                    _connectionState.value = _connectionState.value.copy(
                        errorMessage = result.message
                    )
                }
            }
            
            _isLoading.value = false
        }
    }
    
    fun navigateUp() {
        val current = _currentPath.value
        if (current != "/" && current.isNotEmpty()) {
            val parentPath = current.substringBeforeLast("/", "/")
            loadDirectory(parentPath.ifEmpty { "/" })
        }
    }
    
    fun clearError() {
        _connectionState.value = _connectionState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        nfsClient.disconnect()
    }
}
