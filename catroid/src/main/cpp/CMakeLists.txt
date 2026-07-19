cmake_minimum_required(VERSION 3.22.1)
project("catroid" C CXX ASM)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(onnx)
include_directories(include)

add_library(onnxruntime SHARED IMPORTED)

set_target_properties(onnxruntime PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libonnxruntime.so)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    message(STATUS "Python Support: ENABLED for ${ANDROID_ABI}")
    include_directories(src/main/cpp/include)
    add_library(libpython SHARED IMPORTED)
    set_target_properties(libpython PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libpython3.12.so)
    set(PYTHON_LINK_LIBS libpython)

    message(STATUS "MNN Support: ENABLED for ${ANDROID_ABI}")
    set(MNN_DIR ${CMAKE_SOURCE_DIR}/mnn)
    include_directories(${MNN_DIR}/include)

    add_library(MNN SHARED IMPORTED)
    set_target_properties(MNN PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libMNN.so)
    set(MNN_LINK_LIBS MNN)

    add_compile_definitions(USE_MNN=1)
else()
    message(STATUS "Python and MNN Support: DISABLED for ${ANDROID_ABI}")
    set(PYTHON_LINK_LIBS "")
    set(MNN_LINK_LIBS "")
endif()

find_library(log-lib log)
find_library(android-lib android)
find_library(dl-lib dl)
find_library(egl-lib EGL)
find_library(glesv2-lib GLESv2)

add_library(
        example_core
        SHARED
        example_core.cpp
)

add_library(${CMAKE_PROJECT_NAME} SHARED catroid.cpp)

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        onnxruntime
        ${PYTHON_LINK_LIBS}
        ${log-lib}
        ${android-lib}
        ${dl-lib}
        unwind
        ${MNN_LINK_LIBS}
)

target_link_libraries(
        example_core
        log
        android
        EGL
        GLESv2
)

#
# llama.cpp + AI Agent JNI
#

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_CPU_ARM_ARCH "armv8-a" CACHE STRING "ARM arch for Android")
endif()
set(GGML_NATIVE OFF CACHE BOOL "Disable native optimizations for cross-compilation")
set(GGML_ACCELERATE OFF CACHE BOOL "Disable Accelerate framework (not available on Android)")
set(GGML_OPENMP OFF CACHE BOOL "Disable OpenMP for Android")
set(GGML_LLAMAFILE OFF CACHE BOOL "Disable llamafile for Android")
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static libs for llama")
set(GGML_CUDA OFF CACHE BOOL "Disable CUDA")
set(GGML_METAL OFF CACHE BOOL "Disable Metal")
set(GGML_VULKAN OFF CACHE BOOL "Disable Vulkan")
set(GGML_SYCL OFF CACHE BOOL "Disable SYCL")
set(GGML_CPU_HBM OFF CACHE BOOL "Disable HBM")

add_subdirectory(llama/llama.cpp-master)

add_library(ai_agent_jni SHARED ai_agent_jni.cpp)

target_include_directories(ai_agent_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    llama/llama.cpp-master/include
)

target_link_libraries(ai_agent_jni
    PRIVATE
    llama
    ${log-lib}
    ${dl-lib}
)
