cmake_minimum_required(VERSION 3.25)
project(cpp_client)

find_package(google_cloud_cpp_storage REQUIRED)

# MSVC requires some additional code to select the correct runtime library
if (VCPKG_TARGET_TRIPLET MATCHES "-static$")
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
else ()
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>DLL")
endif ()

# Once the packages are found, define the targets.
#add_executable(quickstart quickstart.cc)
#
#add_executable(quickstart_grpc EXCLUDE_FROM_ALL quickstart_grpc.cc)
#target_link_libraries(quickstart_grpc
#        google-cloud-cpp::experimental-storage-grpc)

add_executable(cpp_client main.cpp)
target_link_libraries(cpp_client google-cloud-cpp::storage)

