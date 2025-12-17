package com.netiface.nfsclient

import android.util.Log

/**
 * NFS Client wrapper that interfaces with the native libnfs library
 * This class provides a Kotlin-friendly API for NFS operations
 */
class NfsClient {
    companion object {
        private const val TAG = "NfsClient"
        
        init {
            try {
                System.loadLibrary("netiface")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    // Native methods that will be implemented in C++
    private external fun nativeConnect(server: String, exportPath: String, uid: Int, gid: Int): Int
    private external fun nativeDisconnect(): Int
    private external fun nativeListDirectory(path: String): Array<String>?
    private external fun nativeGetFileInfo(path: String): LongArray?
    private external fun nativeReadFile(path: String, offset: Long, count: Int): ByteArray?
    private external fun nativeWriteFile(path: String, data: ByteArray, offset: Long): Int
    private external fun nativeIsDirectory(path: String): Boolean
    
    private var isConnected = false
    private var currentServer = ""
    private var currentExport = ""
    
    /**
     * Connect to an NFS server
     * @param server Server IP or hostname
     * @param exportPath NFS export path (e.g., "/export/share")
     * @param uid User ID (default 1000)
     * @param gid Group ID (default 1000)
     * @return NfsResult indicating success or error
     */
    fun connect(server: String, exportPath: String, uid: Int = 1000, gid: Int = 1000): NfsResult<Boolean> {
        return try {
            Log.d(TAG, "Attempting to connect to $server:$exportPath")
            val result = nativeConnect(server, exportPath, uid, gid)
            if (result == 0) {
                isConnected = true
                currentServer = server
                currentExport = exportPath
                Log.d(TAG, "Successfully connected")
                NfsResult.Success(true)
            } else {
                Log.e(TAG, "Connection failed with code: $result")
                NfsResult.Error("Failed to connect to NFS server (error code: $result)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connection", e)
            NfsResult.Error("Connection error: ${e.message}")
        }
    }
    
    /**
     * Disconnect from the NFS server
     */
    fun disconnect(): NfsResult<Boolean> {
        return try {
            if (isConnected) {
                nativeDisconnect()
                isConnected = false
                currentServer = ""
                currentExport = ""
                Log.d(TAG, "Disconnected from NFS server")
            }
            NfsResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
            NfsResult.Error("Disconnect error: ${e.message}")
        }
    }
    
    /**
     * List files in a directory
     * @param path Directory path relative to the mount point
     * @return List of file information or error
     */
    fun listDirectory(path: String = "/"): NfsResult<List<NfsFileInfo>> {
        if (!isConnected) {
            return NfsResult.Error("Not connected to NFS server")
        }
        
        return try {
            val files = nativeListDirectory(path)
            if (files != null) {
                val fileInfoList = files.mapNotNull { fileName ->
                    val fullPath = if (path.endsWith("/")) "$path$fileName" else "$path/$fileName"
                    val info = nativeGetFileInfo(fullPath)
                    if (info != null && info.size >= 2) {
                        NfsFileInfo(
                            name = fileName,
                            path = fullPath,
                            isDirectory = nativeIsDirectory(fullPath),
                            size = info[0],
                            modifiedTime = info[1]
                        )
                    } else {
                        null
                    }
                }
                NfsResult.Success(fileInfoList)
            } else {
                NfsResult.Error("Failed to list directory")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing directory", e)
            NfsResult.Error("Error listing directory: ${e.message}")
        }
    }
    
    /**
     * Read file contents
     * @param path File path
     * @param offset Starting offset
     * @param count Number of bytes to read
     * @return File data or error
     */
    fun readFile(path: String, offset: Long = 0, count: Int = 1024 * 1024): NfsResult<ByteArray> {
        if (!isConnected) {
            return NfsResult.Error("Not connected to NFS server")
        }
        
        return try {
            val data = nativeReadFile(path, offset, count)
            if (data != null) {
                NfsResult.Success(data)
            } else {
                NfsResult.Error("Failed to read file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file", e)
            NfsResult.Error("Error reading file: ${e.message}")
        }
    }
    
    fun isConnectedToServer(): Boolean = isConnected
    
    fun getServerInfo(): Pair<String, String> = Pair(currentServer, currentExport)
}
