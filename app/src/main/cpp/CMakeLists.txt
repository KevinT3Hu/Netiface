cmake_minimum_required(VERSION 3.22.1)

project("netiface")

# Add libnfs sources (we'll use a simplified version for this implementation)
# In a production app, you would clone and build the actual libnfs library
# For this demo, we'll create a wrapper that simulates the NFS operations

add_library(netiface SHARED
        nfs_wrapper.cpp)

# Link with Android log library
find_library(log-lib log)

target_link_libraries(netiface
        ${log-lib})
