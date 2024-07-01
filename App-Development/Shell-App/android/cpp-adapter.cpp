#include <jni.h>
#include "react-native-simple-jsi.h"
#include <iostream>
#include <string>
#include <thread>
#include <jsi/jsi.h>

#ifdef BUILD_DEBUG

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, __FILE__, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, __FILE__, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, __FILE__, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, __FILE__, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__)

#else

#define LOGV(...)
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)
#define LOGE(...)

#endif

extern "C" JNIEXPORT jdouble JNICALL
Java_com_simplejsi_SimpleJsiModule_nativeMultiply(JNIEnv *env, jclass type, jdouble a, jdouble b)
{
    return simplejsi::multiply(a, b);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_simplejsi_SimpleJsiModule_nativeAdd(JNIEnv *env, jclass type, jdouble a, jdouble b)
{

    double result = simplejsi::add(a, b);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_simplejsi_SimpleJsiModule_nativeInstall(JNIEnv *env, jobject thiz, jlong jsi)
{
    auto runtime = reinterpret_cast<facebook::jsi::Runtime *>(jsi);

    if (runtime)
    {
        simplejsi::install(*runtime);
    }
}
