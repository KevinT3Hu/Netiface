package com.netiface.nfsclient

data class NfsFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedTime: Long
)

data class ConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val serverAddress: String = "",
    val sharePath: String = ""
)

sealed class NfsResult<out T> {
    data class Success<T>(val data: T) : NfsResult<T>()
    data class Error(val message: String) : NfsResult<Nothing>()
}
