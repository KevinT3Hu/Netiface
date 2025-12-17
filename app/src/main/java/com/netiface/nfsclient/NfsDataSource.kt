package com.netiface.nfsclient

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException

/**
 * Custom ExoPlayer DataSource that streams video from NFS
 */
class NfsDataSource(
    private val nfsClient: NfsClient
) : BaseDataSource(/* isNetwork= */ true) {
    
    private var uri: Uri? = null
    private var filePath: String? = null
    private var bytesRemaining: Long = 0
    private var opened = false
    private var position: Long = 0
    
    companion object {
        private const val TAG = "NfsDataSource"
        const val SCHEME = "nfs"
    }
    
    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        filePath = dataSpec.uri.path
        
        if (filePath == null) {
            throw IOException("Invalid NFS path")
        }
        
        // Get file size
        val fileInfo = getFileInfo(filePath!!)
        val fileSize = fileInfo?.size ?: C.LENGTH_UNSET.toLong()
        
        position = dataSpec.position
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (fileSize != C.LENGTH_UNSET.toLong()) {
            fileSize - dataSpec.position
        } else {
            C.LENGTH_UNSET.toLong()
        }
        
        opened = true
        transferStarted(dataSpec)
        
        return bytesRemaining
    }
    
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }
        
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }
        
        val filePath = this.filePath ?: throw IOException("Source not opened")
        
        try {
            val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                minOf(length.toLong(), bytesRemaining).toInt()
            } else {
                length
            }
            
            val result = nfsClient.readFile(filePath, position, bytesToRead)
            
            return when (result) {
                is NfsResult.Success -> {
                    val data = result.data
                    if (data.isEmpty()) {
                        if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
                            C.RESULT_END_OF_INPUT
                        } else {
                            throw IOException("Unexpected end of file")
                        }
                    } else {
                        System.arraycopy(data, 0, buffer, offset, data.size)
                        position += data.size
                        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                            bytesRemaining -= data.size
                        }
                        bytesTransferred(data.size)
                        data.size
                    }
                }
                is NfsResult.Error -> {
                    Log.e(TAG, "Error reading from NFS: ${result.message}")
                    throw IOException("NFS read error: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during read", e)
            throw IOException("Read failed", e)
        }
    }
    
    override fun getUri(): Uri? = uri
    
    override fun close() {
        if (opened) {
            uri = null
            filePath = null
            bytesRemaining = 0
            opened = false
            transferEnded()
        }
    }
    
    private fun getFileInfo(path: String): NfsFileInfo? {
        // We need to get file info to determine size
        // This is a helper method that extracts file info
        val result = nfsClient.listDirectory(path.substringBeforeLast("/", "/").ifEmpty { "/" })
        return when (result) {
            is NfsResult.Success -> {
                val fileName = path.substringAfterLast("/")
                result.data.find { it.name == fileName }
            }
            is NfsResult.Error -> null
        }
    }
    
    /**
     * Factory for creating NfsDataSource instances
     */
    class Factory(private val nfsClient: NfsClient) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return NfsDataSource(nfsClient)
        }
    }
}
