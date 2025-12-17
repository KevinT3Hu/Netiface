#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <cstring>
#include <fcntl.h>
#include <sys/stat.h>
#include <nfsc/libnfs.h>

#define LOG_TAG "NfsWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Default file permissions for created files
#define DEFAULT_FILE_MODE 0644

// NFS connection state
static struct nfs_context *nfs = nullptr;

extern "C" {

JNIEXPORT jint JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeConnect(
        JNIEnv *env,
        jobject thiz,
        jstring server,
        jstring exportPath,
        jint uid,
        jint gid) {
    
    const char *serverStr = env->GetStringUTFChars(server, nullptr);
    const char *exportStr = env->GetStringUTFChars(exportPath, nullptr);
    
    LOGD("Connecting to NFS server: %s:%s (uid=%d, gid=%d)", serverStr, exportStr, uid, gid);
    
    // Clean up any existing connection
    if (nfs != nullptr) {
        nfs_destroy_context(nfs);
        nfs = nullptr;
    }
    
    // Create a new NFS context
    nfs = nfs_init_context();
    if (nfs == nullptr) {
        LOGE("Failed to initialize NFS context");
        env->ReleaseStringUTFChars(server, serverStr);
        env->ReleaseStringUTFChars(exportPath, exportStr);
        return -1;
    }
    
    // Set UID and GID
    nfs_set_uid(nfs, uid);
    nfs_set_gid(nfs, gid);
    
    // Mount the NFS share
    int ret = nfs_mount(nfs, serverStr, exportStr);
    if (ret != 0) {
        LOGE("Failed to mount NFS share: %s", nfs_get_error(nfs));
        nfs_destroy_context(nfs);
        nfs = nullptr;
        env->ReleaseStringUTFChars(server, serverStr);
        env->ReleaseStringUTFChars(exportPath, exportStr);
        return ret;
    }
    
    env->ReleaseStringUTFChars(server, serverStr);
    env->ReleaseStringUTFChars(exportPath, exportStr);
    
    LOGD("Successfully connected to NFS server");
    return 0; // Success
}

JNIEXPORT jint JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeDisconnect(
        JNIEnv *env,
        jobject thiz) {
    
    LOGD("Disconnecting from NFS server");
    
    if (nfs != nullptr) {
        nfs_umount(nfs);
        nfs_destroy_context(nfs);
        nfs = nullptr;
    }
    
    LOGD("Disconnected successfully");
    return 0; // Success
}

JNIEXPORT jobjectArray JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeListDirectory(
        JNIEnv *env,
        jobject thiz,
        jstring path) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    LOGD("Listing directory: %s", pathStr);
    
    if (nfs == nullptr) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Open the directory
    struct nfsdir *dir = nullptr;
    int ret = nfs_opendir(nfs, pathStr, &dir);
    if (ret != 0) {
        LOGE("Failed to open directory %s: %s", pathStr, nfs_get_error(nfs));
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Collect directory entries
    std::vector<std::string> fileNames;
    struct nfsdirent *entry;
    
    while ((entry = nfs_readdir(nfs, dir)) != nullptr) {
        // Skip "." and ".." entries
        if (strcmp(entry->name, ".") != 0 && strcmp(entry->name, "..") != 0) {
            fileNames.push_back(entry->name);
        }
    }
    
    nfs_closedir(nfs, dir);
    
    // Create Java string array
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(fileNames.size(), stringClass, nullptr);
    
    for (size_t i = 0; i < fileNames.size(); i++) {
        jstring fileName = env->NewStringUTF(fileNames[i].c_str());
        env->SetObjectArrayElement(result, i, fileName);
        env->DeleteLocalRef(fileName);
    }
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("Listed %zu files", fileNames.size());
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeGetFileInfo(
        JNIEnv *env,
        jobject thiz,
        jstring path) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    LOGD("Getting file info: %s", pathStr);
    
    if (nfs == nullptr) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Get file stats
    struct nfs_stat_64 st;
    int ret = nfs_stat64(nfs, pathStr, &st);
    if (ret != 0) {
        LOGE("Failed to stat file %s: %s", pathStr, nfs_get_error(nfs));
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Return [size, modifiedTime]
    jlongArray result = env->NewLongArray(2);
    jlong info[2] = {
        static_cast<jlong>(st.nfs_size),
        static_cast<jlong>(st.nfs_mtime)
    };
    env->SetLongArrayRegion(result, 0, 2, info);
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("File size: %lld, mtime: %lld", (long long)st.nfs_size, (long long)st.nfs_mtime);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeReadFile(
        JNIEnv *env,
        jobject thiz,
        jstring path,
        jlong offset,
        jint count) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    LOGD("Reading file: %s (offset=%lld, count=%d)", pathStr, (long long)offset, count);
    
    if (nfs == nullptr) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Open the file for reading
    struct nfsfh *fh = nullptr;
    int ret = nfs_open(nfs, pathStr, O_RDONLY, &fh);
    if (ret != 0) {
        LOGE("Failed to open file %s: %s", pathStr, nfs_get_error(nfs));
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Seek to the offset if needed
    if (offset > 0) {
        uint64_t current_pos;
        ret = nfs_lseek(nfs, fh, offset, SEEK_SET, &current_pos);
        if (ret != 0) {
            LOGE("Failed to seek in file: %s", nfs_get_error(nfs));
            nfs_close(nfs, fh);
            env->ReleaseStringUTFChars(path, pathStr);
            return nullptr;
        }
    }
    
    // Allocate buffer for reading
    std::vector<char> buffer(count);
    
    // Read the file
    int bytesRead = nfs_read(nfs, fh, count, buffer.data());
    if (bytesRead < 0) {
        LOGE("Failed to read file: %s", nfs_get_error(nfs));
        nfs_close(nfs, fh);
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Close the file
    nfs_close(nfs, fh);
    
    // Create Java byte array
    jbyteArray result = env->NewByteArray(bytesRead);
    env->SetByteArrayRegion(result, 0, bytesRead, 
                           reinterpret_cast<const jbyte*>(buffer.data()));
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("Read %d bytes", bytesRead);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeWriteFile(
        JNIEnv *env,
        jobject thiz,
        jstring path,
        jbyteArray data,
        jlong offset) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    jsize dataLen = env->GetArrayLength(data);
    LOGD("Writing file: %s (offset=%lld, length=%d)", pathStr, (long long)offset, dataLen);
    
    if (nfs == nullptr) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return -1;
    }
    
    // Open the file for writing
    struct nfsfh *fh = nullptr;
    int ret = nfs_open(nfs, pathStr, O_WRONLY | O_CREAT, &fh);
    if (ret != 0) {
        LOGE("Failed to open file for writing %s: %s", pathStr, nfs_get_error(nfs));
        env->ReleaseStringUTFChars(path, pathStr);
        return -1;
    }
    
    // Set file permissions to 0644 after creation
    nfs_chmod(nfs, pathStr, DEFAULT_FILE_MODE);
    
    // Seek to the offset if needed
    if (offset > 0) {
        uint64_t current_pos;
        ret = nfs_lseek(nfs, fh, offset, SEEK_SET, &current_pos);
        if (ret != 0) {
            LOGE("Failed to seek in file: %s", nfs_get_error(nfs));
            nfs_close(nfs, fh);
            env->ReleaseStringUTFChars(path, pathStr);
            return -1;
        }
    }
    
    // Get the data from Java byte array
    jbyte *dataPtr = env->GetByteArrayElements(data, nullptr);
    
    // Write the data
    int bytesWritten = nfs_write(nfs, fh, dataLen, reinterpret_cast<char*>(dataPtr));
    
    // Release the byte array
    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);
    
    // Close the file
    nfs_close(nfs, fh);
    
    if (bytesWritten < 0) {
        LOGE("Failed to write file: %s", nfs_get_error(nfs));
        env->ReleaseStringUTFChars(path, pathStr);
        return -1;
    }
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("Wrote %d bytes", bytesWritten);
    return bytesWritten; // Return number of bytes written
}

JNIEXPORT jboolean JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeIsDirectory(
        JNIEnv *env,
        jobject thiz,
        jstring path) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    
    if (nfs == nullptr) {
        env->ReleaseStringUTFChars(path, pathStr);
        return JNI_FALSE;
    }
    
    // Get file stats
    struct nfs_stat_64 st;
    int ret = nfs_stat64(nfs, pathStr, &st);
    if (ret != 0) {
        LOGE("Failed to stat file %s: %s", pathStr, nfs_get_error(nfs));
        env->ReleaseStringUTFChars(path, pathStr);
        return JNI_FALSE;
    }
    
    env->ReleaseStringUTFChars(path, pathStr);
    
    // Check if it's a directory using S_ISDIR macro
    return S_ISDIR(st.nfs_mode) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
