cmake_minimum_required(VERSION 3.10.2)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)

project("zeroprism")

add_library(zeroprism SHARED
        org_zeroprism_Transport.cpp
        org_zeroprism_Transport.h)

include_directories(tor)

find_library(LOG_LIB log)
find_library(TOR_LIB tor PATHS ../libs/${CMAKE_ANDROID_ARCH_ABI}/)

if (NOT TOR_LIB)
    message(FATAL_ERROR "libtor.so not found. ")
endif()

target_link_libraries(zeroprism ${TOR_LIB} ${LOG_LIB})