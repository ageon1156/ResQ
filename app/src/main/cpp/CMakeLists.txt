cmake_minimum_required(VERSION 3.22.1)
project(codec2_jni)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DFLOATING_POINT -DVAR_ARRAYS")

file(GLOB CODEC2_SOURCES codec2/*.c)

add_library(codec2 STATIC ${CODEC2_SOURCES})
target_include_directories(codec2 PUBLIC codec2/ .)

add_library(codec2_jni SHARED codec2_jni.cpp)
target_link_libraries(codec2_jni codec2 log)
