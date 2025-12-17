cmake_minimum_required(VERSION 3.22.1)

project("netiface")

# Include FetchContent module for downloading libnfs
include(FetchContent)

# Fetch libnfs from GitHub
FetchContent_Declare(
    libnfs
    GIT_REPOSITORY https://github.com/sahlberg/libnfs.git
    GIT_TAG libnfs-5.0.3
)

# Configure libnfs build options
set(ENABLE_TESTS OFF CACHE BOOL "" FORCE)
set(ENABLE_DOCUMENTATION OFF CACHE BOOL "" FORCE)
set(ENABLE_UTILS OFF CACHE BOOL "" FORCE)
set(ENABLE_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

# Make libnfs available
FetchContent_MakeAvailable(libnfs)

add_library(netiface SHARED
        nfs_wrapper.cpp)

# Include libnfs headers
target_include_directories(netiface PRIVATE
    ${libnfs_SOURCE_DIR}/include
    ${libnfs_BINARY_DIR}/include
)

# Link with Android log library and libnfs
find_library(log-lib log)

target_link_libraries(netiface
        nfs
        ${log-lib})
