# Netiface

An Android NFS (Network File System) client application built on top of [sahlberg/libnfs](https://github.com/sahlberg/libnfs).

## Features

- **NFS Connection**: Connect to NFS servers using server address and share path
- **File Browser**: Browse directories and files on the NFS share
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Native Performance**: Uses JNI wrapper for libnfs C library

## Architecture

- **UI Layer**: Jetpack Compose with Material3 components
- **Business Logic**: Kotlin with coroutines and StateFlow
- **Native Layer**: C++ JNI wrapper for libnfs
- **MVVM Pattern**: ViewModel manages state and business logic

## Building

### Requirements
- Android Studio Electric Eel or later
- Android SDK 24+ (minimum)
- NDK for native library compilation
- Gradle 8.0+

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/KevinT3Hu/Netiface.git
cd Netiface
```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run on an emulator or device

## Usage

1. Launch the app
2. Enter the NFS server address (e.g., `192.168.1.100`)
3. Enter the share path (e.g., `/export/share`)
4. Optionally enter username and password for authentication
5. Tap "Connect" to connect to the NFS server
6. Browse files and directories in the file browser

## Technical Details

### Components

- **MainActivity**: Entry point with Compose UI
- **ConnectionScreen**: UI for NFS server connection
- **FileBrowserScreen**: UI for browsing files and directories
- **NfsViewModel**: Manages connection state and file operations
- **NfsClient**: Kotlin wrapper for native NFS operations
- **nfs_wrapper.cpp**: JNI bridge to libnfs library

### Current Implementation

The current version includes a **mock implementation** of the NFS operations for demonstration purposes. The native layer provides a simulated file system response. To integrate the actual libnfs library:

1. Clone and build libnfs for Android
2. Update CMakeLists.txt to link against libnfs
3. Replace mock implementations in nfs_wrapper.cpp with actual libnfs calls

## License

This project is provided as-is for educational and development purposes.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.
