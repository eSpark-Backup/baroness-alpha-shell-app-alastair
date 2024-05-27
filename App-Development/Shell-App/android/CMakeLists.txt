cmake_minimum_required(VERSION 3.4.1)
project(SimpleJsi)

set (CMAKE_VERBOSE_MAKEFILE ON)
set (CMAKE_CXX_STANDARD 14)

# Path to the React Native Android SDK
set(REACT_NATIVE_DIR ../example/node_modules/react-native/ReactAndroid)
set(REACT_NATIVE_COMMON_DIR ../example/node_modules/react-native/ReactCommon)

add_library(react-native-simple-jsi            SHARED
            ../cpp/react-native-simple-jsi.cpp
            cpp-adapter.cpp
)

# Specifies a path to native header files.
include_directories(
            ../cpp
            ${REACT_NATIVE_COMMON_DIR}/jsi
)

# Specify the Android logging and Android libraries
target_link_libraries(
    react-native-simple-jsi
    log      # Include the Android logging library
    android  # Include the Android library
)