# Building Netiface Android NFS Client

## Project Overview

Netiface is an Android application that provides NFS (Network File System) client functionality using a native library wrapper around sahlberg/libnfs. The app is built with:

- **UI Framework**: Jetpack Compose with Material Design 3
- **Language**: Kotlin for Android app, C++ for native NFS wrapper
- **Architecture**: MVVM pattern with StateFlow
- **Build System**: Gradle with Android Gradle Plugin
- **Native Build**: CMake for JNI library compilation

## Project Structure

```
Netiface/
├── app/
│   ├── build.gradle                 # App module build configuration
│   ├── proguard-rules.pro          # ProGuard rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  # App manifest with permissions
│           ├── cpp/                 # Native C++ code
│           │   ├── CMakeLists.txt  # CMake build script
│           │   └── nfs_wrapper.cpp # JNI wrapper for libnfs
│           ├── java/com/netiface/nfsclient/
│           │   ├── MainActivity.kt          # Main activity with Compose
│           │   ├── ConnectionScreen.kt     # Connection UI (Compose)
│           │   ├── FileBrowserScreen.kt    # File browser UI (Compose)
│           │   ├── NfsClient.kt           # JNI wrapper class
│           │   ├── NfsViewModel.kt        # ViewModel for state management
│           │   ├── NfsModels.kt           # Data models
│           │   └── ui/theme/              # Compose theme files
│           │       ├── Color.kt
│           │       ├── Theme.kt
│           │       └── Type.kt
│           └── res/                       # Android resources
│               ├── values/
│               │   ├── strings.xml
│               │   └── colors.xml
│               └── mipmap-*/              # App icons
├── build.gradle                    # Root build configuration
├── settings.gradle                # Project settings
├── gradle.properties              # Gradle properties
└── README.md                      # Project documentation

```

## Build Requirements

### Required Software
- **Android Studio**: Electric Eel (2022.1.1) or later
- **JDK**: Java 17 or later
- **Android SDK**: API Level 34 (Android 14)
- **Android NDK**: For native library compilation
- **CMake**: Version 3.22.1 or later (bundled with Android Studio)

### SDK Components
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0+
- NDK (Side by side) 25.0.0+
- CMake 3.22.1

## Build Steps

### Option 1: Android Studio

1. **Install Android Studio**
   - Download from https://developer.android.com/studio
   - Install required SDK components

2. **Open Project**
   - Launch Android Studio
   - Open the Netiface project directory
   - Wait for Gradle sync to complete

3. **Configure NDK**
   - Go to Tools → SDK Manager
   - Select SDK Tools tab
   - Install NDK (Side by side) and CMake

4. **Build APK**
   - Select Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK will be generated in `app/build/outputs/apk/debug/`

5. **Install on Device**
   - Connect Android device or start emulator
   - Run → Run 'app' or use Install APK option

### Option 2: Command Line

1. **Setup Environment**
   ```bash
   export ANDROID_HOME=/path/to/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

2. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Install on Device**
   ```bash
   ./gradlew installDebug
   ```

5. **Run Tests**
   ```bash
   ./gradlew test
   ```

## Key Features Implemented

### 1. Connection Screen (Jetpack Compose)
- Server address input
- Share path input
- Optional username/password authentication
- Connection status display
- Error handling with dialogs

### 2. File Browser Screen (Jetpack Compose)
- Directory navigation
- File and folder listing
- File information display (size, modified time)
- Swipe to refresh
- Back navigation

### 3. Native NFS Wrapper (C++/JNI)
- Connection to NFS servers
- Directory listing
- File information retrieval
- File read operations
- Mock implementation for demonstration

### 4. State Management (ViewModel)
- Connection state management
- File listing state
- Loading states
- Error handling

## Current Implementation Notes

The current version includes a **mock/demo implementation** of the NFS operations. The native C++ layer (`nfs_wrapper.cpp`) simulates NFS functionality with hardcoded file listings. This allows the app to be built and tested without requiring actual libnfs library integration.

### To Integrate Real libnfs:

1. **Build libnfs for Android**
   ```bash
   git clone https://github.com/sahlberg/libnfs.git
   cd libnfs
   # Build for Android architectures using NDK
   ```

2. **Update CMakeLists.txt**
   ```cmake
   # Add libnfs as a prebuilt library
   add_library(nfs SHARED IMPORTED)
   set_target_properties(nfs PROPERTIES IMPORTED_LOCATION
       ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libnfs.so)
   
   # Link against libnfs
   target_link_libraries(netiface nfs ${log-lib})
   ```

3. **Replace Mock Implementation**
   - Update `nfs_wrapper.cpp` to use actual libnfs API calls
   - Replace simulated functions with real NFS operations

## Dependencies

### Kotlin/Android
- AndroidX Core KTX: 1.12.0
- Lifecycle Runtime KTX: 2.7.0
- Activity Compose: 1.8.2
- Compose BOM: 2024.02.00
- Material3 (Compose)
- Navigation Compose: 2.7.7
- ViewModel Compose: 2.7.0

### Build Tools
- Android Gradle Plugin: 8.1.2
- Kotlin: 1.9.0
- Gradle: 8.2

## Permissions

The app requests the following permissions (declared in AndroidManifest.xml):
- `INTERNET`: Network access for NFS connections
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `READ_EXTERNAL_STORAGE`: Read files (API < 33)
- `WRITE_EXTERNAL_STORAGE`: Write files (API < 33)
- `READ_MEDIA_IMAGES/VIDEO/AUDIO`: Media access (API 33+)

## Testing

### Manual Testing
1. Install APK on device/emulator
2. Launch app
3. Enter NFS server details
4. Test connection
5. Browse mock file system
6. Verify navigation and UI

### Automated Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Troubleshooting

### Common Issues

1. **Gradle Sync Fails**
   - Ensure Android SDK is properly installed
   - Check internet connection for dependency downloads
   - Invalidate Caches: File → Invalidate Caches → Invalidate and Restart

2. **NDK Not Found**
   - Install NDK via SDK Manager
   - Verify NDK path in local.properties

3. **CMake Build Fails**
   - Ensure CMake version matches CMakeLists.txt
   - Check NDK is properly configured

4. **App Crashes on Launch**
   - Check logcat for errors
   - Verify minimum SDK version (API 24+)
   - Ensure native library loads correctly

## Future Enhancements

- Integrate actual libnfs library
- Add file upload/download functionality
- Implement file operations (delete, rename, etc.)
- Add caching for better performance
- Support for multiple NFS connections
- Bookmark favorite shares
- File search functionality
- Progress indicators for large file operations

## License

This project is provided as-is for educational purposes.
