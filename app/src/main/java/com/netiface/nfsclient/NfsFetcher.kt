package com.netiface.nfsclient

import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import okio.BufferedSource
import okio.Timeout
import java.io.IOException

/**
 * Custom Coil fetcher that streams images from NFS
 */
class NfsFetcher(
    private val data: NfsImageData,
    private val options: Options,
    private val nfsClient: NfsClient
) : Fetcher {
    
    override suspend fun fetch(): FetchResult {
        val source = NfsBufferedSource(data.filePath, nfsClient)
        return SourceResult(
            source = ImageSource(
                source = source,
                context = options.context
            ),
            mimeType = null,
            dataSource = DataSource.NETWORK
        )
    }
    
    class Factory(private val nfsClient: NfsClient) : Fetcher.Factory<NfsImageData> {
        override fun create(
            data: NfsImageData,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return NfsFetcher(data, options, nfsClient)
        }
    }
}

/**
 * Data class for NFS image data
 */
data class NfsImageData(
    val filePath: String
)

/**
 * BufferedSource implementation that reads from NFS in chunks
 */
private class NfsBufferedSource(
    private val filePath: String,
    private val nfsClient: NfsClient
) : BufferedSource {
    private val buffer = Buffer()
    private var position = 0L
    private var closed = false
    private val chunkSize = 64 * 1024 // 64KB chunks
    
    private val TAG = "NfsBufferedSource"
    
    override fun buffer(): Buffer = buffer
    
    override fun close() {
        closed = true
        buffer.clear()
    }
    
    override fun exhausted(): Boolean {
        if (closed) return true
        // Try to read more data if buffer is empty
        if (buffer.exhausted()) {
            readNextChunk()
        }
        return buffer.exhausted()
    }
    
    override fun read(sink: Buffer, byteCount: Long): Long {
        if (closed) throw IOException("Source is closed")
        
        // Ensure we have data in the buffer
        while (buffer.size < byteCount && !closed) {
            val read = readNextChunk()
            if (read <= 0) break
        }
        
        return if (buffer.size > 0) {
            buffer.read(sink, minOf(byteCount, buffer.size))
        } else {
            -1L
        }
    }
    
    private fun readNextChunk(): Long {
        if (closed) return -1L
        
        return try {
            val result = nfsClient.readFile(filePath, position, chunkSize)
            when (result) {
                is NfsResult.Success -> {
                    val data = result.data
                    if (data.isEmpty()) {
                        -1L
                    } else {
                        buffer.write(data)
                        position += data.size.toLong()
                        data.size.toLong()
                    }
                }
                is NfsResult.Error -> {
                    Log.e(TAG, "Error reading chunk: ${result.message}")
                    -1L
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading chunk", e)
            -1L
        }
    }
    
    override fun timeout(): Timeout = Timeout.NONE
    
    // Delegate all other BufferedSource methods to buffer
    override fun indexOf(b: Byte) = buffer.indexOf(b)
    override fun indexOf(b: Byte, fromIndex: Long) = buffer.indexOf(b, fromIndex)
    override fun indexOf(b: Byte, fromIndex: Long, toIndex: Long) = buffer.indexOf(b, fromIndex, toIndex)
    override fun indexOf(bytes: okio.ByteString) = buffer.indexOf(bytes)
    override fun indexOf(bytes: okio.ByteString, fromIndex: Long) = buffer.indexOf(bytes, fromIndex)
    override fun indexOfElement(targetBytes: okio.ByteString) = buffer.indexOfElement(targetBytes)
    override fun indexOfElement(targetBytes: okio.ByteString, fromIndex: Long) = buffer.indexOfElement(targetBytes, fromIndex)
    override fun rangeEquals(offset: Long, bytes: okio.ByteString) = buffer.rangeEquals(offset, bytes)
    override fun rangeEquals(offset: Long, bytes: okio.ByteString, bytesOffset: Int, byteCount: Int) = 
        buffer.rangeEquals(offset, bytes, bytesOffset, byteCount)
    override fun readByte() = buffer.readByte()
    override fun readShort() = buffer.readShort()
    override fun readShortLe() = buffer.readShortLe()
    override fun readInt() = buffer.readInt()
    override fun readIntLe() = buffer.readIntLe()
    override fun readLong() = buffer.readLong()
    override fun readLongLe() = buffer.readLongLe()
    override fun readDecimalLong() = buffer.readDecimalLong()
    override fun readHexadecimalUnsignedLong() = buffer.readHexadecimalUnsignedLong()
    override fun skip(byteCount: Long) = buffer.skip(byteCount)
    override fun readByteString() = buffer.readByteString()
    override fun readByteString(byteCount: Long) = buffer.readByteString(byteCount)
    override fun select(options: okio.Options) = buffer.select(options)
    override fun readByteArray() = buffer.readByteArray()
    override fun readByteArray(byteCount: Long) = buffer.readByteArray(byteCount)
    override fun read(sink: ByteArray) = buffer.read(sink)
    override fun readFully(sink: Buffer, byteCount: Long) = buffer.readFully(sink, byteCount)
    override fun readFully(sink: ByteArray) = buffer.readFully(sink)
    override fun readAll(sink: okio.Sink) = buffer.readAll(sink)
    override fun readUtf8() = buffer.readUtf8()
    override fun readUtf8(byteCount: Long) = buffer.readUtf8(byteCount)
    override fun readUtf8Line() = buffer.readUtf8Line()
    override fun readUtf8LineStrict() = buffer.readUtf8LineStrict()
    override fun readUtf8LineStrict(limit: Long) = buffer.readUtf8LineStrict(limit)
    override fun readUtf8CodePoint() = buffer.readUtf8CodePoint()
    override fun readString(charset: java.nio.charset.Charset) = buffer.readString(charset)
    override fun readString(byteCount: Long, charset: java.nio.charset.Charset) = buffer.readString(byteCount, charset)
    override fun request(byteCount: Long): Boolean {
        while (buffer.size < byteCount && !closed) {
            val read = readNextChunk()
            if (read <= 0) break
        }
        return buffer.size >= byteCount
    }
    override fun require(byteCount: Long) {
        if (!request(byteCount)) {
            throw IOException("Required $byteCount bytes but only ${buffer.size} available")
        }
    }
    override fun peek() = buffer.peek()
    override fun inputStream() = buffer.inputStream()
}
