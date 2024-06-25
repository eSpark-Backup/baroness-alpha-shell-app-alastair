cmake_minimum_required(VERSION 3.4.1)
project(SimpleJsi)

set (CMAKE_VERBOSE_MAKEFILE ON)
set (CMAKE_CXX_STANDARD 17)

set (PACKAGE_NAME "react-native-simple-jsi")

message("Ash: NODE_MODULES_DIR: ${NODE_MODULES_DIR}")

# Specifies a path to native header files.
include_directories(
    ../cpp
    "${NODE_MODULES_DIR}/react-native/React"
    "${NODE_MODULES_DIR}/react-native/React/Base"
    "${NODE_MODULES_DIR}/react-native/ReactCommon/jsi"
    "${NODE_MODULES_DIR}/react-native/ReactCommon/RuntimeExecutor"
    "${NODE_MODULES_DIR}/react-native/ReactCommon/CallInvoker"
    "${NODE_MODULES_DIR}/react-native/ReactAndroid"
)

file(GLOB LIBRN_DIR "${BUILD_DIR}/react-native-0*/jni/${ANDROID_ABI}")
message("ASH! LIBRN_DIR (GLOB result): ${LIBRN_DIR}")

if (EXISTS "${LIBRN_DIR}/libjsi.so")
  # React Native 0.66.x and above
  message(STATUS "React Native 0.66.x and above: ${LIBRN_DIR}/libjsi.so exists")

  find_library(
      JSI_LIB
      jsi
      PATHS ${LIBRN_DIR}
      NO_CMAKE_FIND_ROOT_PATH
  )

  add_library(${PACKAGE_NAME} SHARED
    ../cpp/react-native-simple-jsi.cpp
    cpp-adapter.cpp
  )
else()
  # React Native 0.65.x and below
  message(STATUS "React Native 0.65.x and below: ${LIBRN_DIR}/libjsi.so does not exist")

  set (JSI_LIB "")
  add_library(
      ${PACKAGE_NAME}
      SHARED
      ../example/node_modules/react-native/ReactCommon/jsi/jsi/jsi.cpp
      ../cpp/react-native-simple-jsi.cpp
      cpp-adapter.cpp
  )
endif()

find_library(
    LOG_LIB
    log
)

# find_library(
#     REACT_NATIVE_JNI_LIB
#     reactnativejni
#     PATHS ${LIBRN_DIR}
#     NO_CMAKE_FIND_ROOT_PATH
# )

# if (NOT REACT_NATIVE_JNI_LIB)
#   message(FATAL_ERROR "REACT_NATIVE_JNI_LIB not found in ${LIBRN_DIR}")
# endif()

target_link_libraries(
    ${PACKAGE_NAME}
    ${LOG_LIB}
    ${REACT_NATIVE_JNI_LIB}
    ${JSI_LIB}
    android
)
