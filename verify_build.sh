#!/bin/bash

# Build verification script for Netiface Android app
# This script checks that all required files are in place

echo "=== Netiface Android NFS Client Build Verification ==="
echo ""

# Check project structure
echo "1. Checking project structure..."
required_files=(
    "build.gradle"
    "settings.gradle"
    "gradle.properties"
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/cpp/CMakeLists.txt"
    "app/src/main/cpp/nfs_wrapper.cpp"
)

all_exist=true
for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (MISSING)"
        all_exist=false
    fi
done

echo ""
echo "2. Checking Kotlin source files..."
kotlin_files=(
    "app/src/main/java/com/netiface/nfsclient/MainActivity.kt"
    "app/src/main/java/com/netiface/nfsclient/ConnectionScreen.kt"
    "app/src/main/java/com/netiface/nfsclient/FileBrowserScreen.kt"
    "app/src/main/java/com/netiface/nfsclient/NfsClient.kt"
    "app/src/main/java/com/netiface/nfsclient/NfsViewModel.kt"
    "app/src/main/java/com/netiface/nfsclient/NfsModels.kt"
    "app/src/main/java/com/netiface/nfsclient/ui/theme/Color.kt"
    "app/src/main/java/com/netiface/nfsclient/ui/theme/Theme.kt"
    "app/src/main/java/com/netiface/nfsclient/ui/theme/Type.kt"
)

for file in "${kotlin_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (MISSING)"
        all_exist=false
    fi
done

echo ""
echo "3. Checking resource files..."
resource_files=(
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
)

for file in "${resource_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (MISSING)"
        all_exist=false
    fi
done

echo ""
echo "4. Code statistics..."
echo "  Kotlin files: $(find app/src/main/java -name "*.kt" | wc -l)"
echo "  C++ files: $(find app/src/main/cpp -name "*.cpp" -o -name "*.h" | wc -l)"
echo "  Total lines of Kotlin code: $(find app/src/main/java -name "*.kt" -exec cat {} \; | wc -l)"
echo "  Total lines of C++ code: $(find app/src/main/cpp -name "*.cpp" -o -name "*.h" -exec cat {} \; | wc -l)"

echo ""
echo "=== Build Instructions ==="
echo ""
echo "To build this Android app with an actual Android SDK:"
echo "  1. Install Android Studio and Android SDK"
echo "  2. Open this project in Android Studio"
echo "  3. Sync Gradle (File -> Sync Project with Gradle Files)"
echo "  4. Build the APK (Build -> Build Bundle(s)/APK(s) -> Build APK(s))"
echo "  5. Install on device or emulator"
echo ""
echo "Or use command line with gradle:"
echo "  ./gradlew assembleDebug"
echo ""

if [ "$all_exist" = true ]; then
    echo "✓ All required files are present!"
    exit 0
else
    echo "✗ Some files are missing!"
    exit 1
fi
