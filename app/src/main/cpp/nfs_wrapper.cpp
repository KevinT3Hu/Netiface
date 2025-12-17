#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <cstring>

#define LOG_TAG "NfsWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Simulated NFS connection state
static bool isConnected = false;
static std::string connectedServer;
static std::string connectedExport;

// Simulated file system data structure
struct FileEntry {
    std::string name;
    bool isDirectory;
    long size;
    long modifiedTime;
};

// Mock file system for demonstration
static std::vector<FileEntry> mockFileSystem = {
    {"Documents", true, 0, 1638360000},
    {"Pictures", true, 0, 1638360000},
    {"Videos", true, 0, 1638360000},
    {"Music", true, 0, 1638360000},
    {"Downloads", true, 0, 1638360000},
    {"test.txt", false, 1024, 1638360000},
    {"readme.md", false, 2048, 1638360000},
    {"data.json", false, 4096, 1638360000},
};

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
    
    // In a real implementation, this would use libnfs to connect
    // For this demo, we'll simulate a successful connection
    connectedServer = serverStr;
    connectedExport = exportStr;
    isConnected = true;
    
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
    
    // In a real implementation, this would use libnfs to disconnect
    isConnected = false;
    connectedServer.clear();
    connectedExport.clear();
    
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
    
    if (!isConnected) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // In a real implementation, this would use libnfs to list directory contents
    // For this demo, we'll return the mock file system
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(mockFileSystem.size(), stringClass, nullptr);
    
    for (size_t i = 0; i < mockFileSystem.size(); i++) {
        jstring fileName = env->NewStringUTF(mockFileSystem[i].name.c_str());
        env->SetObjectArrayElement(result, i, fileName);
        env->DeleteLocalRef(fileName);
    }
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("Listed %zu files", mockFileSystem.size());
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeGetFileInfo(
        JNIEnv *env,
        jobject thiz,
        jstring path) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    LOGD("Getting file info: %s", pathStr);
    
    if (!isConnected) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // Extract file name from path
    std::string pathString(pathStr);
    size_t lastSlash = pathString.find_last_of('/');
    std::string fileName = (lastSlash != std::string::npos) ? 
                           pathString.substr(lastSlash + 1) : pathString;
    
    // Find file in mock file system
    for (const auto& entry : mockFileSystem) {
        if (entry.name == fileName) {
            jlongArray result = env->NewLongArray(2);
            jlong info[2] = {entry.size, entry.modifiedTime};
            env->SetLongArrayRegion(result, 0, 2, info);
            
            env->ReleaseStringUTFChars(path, pathStr);
            return result;
        }
    }
    
    // Default file info if not found
    jlongArray result = env->NewLongArray(2);
    jlong info[2] = {0, 0};
    env->SetLongArrayRegion(result, 0, 2, info);
    
    env->ReleaseStringUTFChars(path, pathStr);
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
    
    if (!isConnected) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    
    // In a real implementation, this would use libnfs to read file contents
    // For this demo, we'll return some sample data
    std::string sampleData = "This is sample file content from the NFS server.";
    jbyteArray result = env->NewByteArray(sampleData.length());
    env->SetByteArrayRegion(result, 0, sampleData.length(), 
                           reinterpret_cast<const jbyte*>(sampleData.c_str()));
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("Read %zu bytes", sampleData.length());
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
    
    if (!isConnected) {
        LOGE("Not connected to NFS server");
        env->ReleaseStringUTFChars(path, pathStr);
        return -1;
    }
    
    // In a real implementation, this would use libnfs to write file contents
    // For this demo, we'll just return success
    
    env->ReleaseStringUTFChars(path, pathStr);
    LOGD("Wrote %d bytes", dataLen);
    return dataLen; // Return number of bytes written
}

JNIEXPORT jboolean JNICALL
Java_com_netiface_nfsclient_NfsClient_nativeIsDirectory(
        JNIEnv *env,
        jobject thiz,
        jstring path) {
    
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    
    if (!isConnected) {
        env->ReleaseStringUTFChars(path, pathStr);
        return JNI_FALSE;
    }
    
    // Extract file name from path
    std::string pathString(pathStr);
    size_t lastSlash = pathString.find_last_of('/');
    std::string fileName = (lastSlash != std::string::npos) ? 
                           pathString.substr(lastSlash + 1) : pathString;
    
    // Check if it's a directory in mock file system
    for (const auto& entry : mockFileSystem) {
        if (entry.name == fileName) {
            env->ReleaseStringUTFChars(path, pathStr);
            return entry.isDirectory ? JNI_TRUE : JNI_FALSE;
        }
    }
    
    env->ReleaseStringUTFChars(path, pathStr);
    return JNI_FALSE;
}

} // extern "C"
