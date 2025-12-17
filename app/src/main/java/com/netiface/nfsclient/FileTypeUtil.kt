package com.netiface.nfsclient

enum class MediaFileType {
    IMAGE,
    VIDEO,
    OTHER
}

object FileTypeUtil {
    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif"
    )
    
    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp"
    )
    
    fun getMediaFileType(fileName: String): MediaFileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            imageExtensions.contains(extension) -> MediaFileType.IMAGE
            videoExtensions.contains(extension) -> MediaFileType.VIDEO
            else -> MediaFileType.OTHER
        }
    }
    
    fun isMediaFile(fileName: String): Boolean {
        return getMediaFileType(fileName) != MediaFileType.OTHER
    }
    
    fun isImage(fileName: String): Boolean {
        return getMediaFileType(fileName) == MediaFileType.IMAGE
    }
    
    fun isVideo(fileName: String): Boolean {
        return getMediaFileType(fileName) == MediaFileType.VIDEO
    }
}
