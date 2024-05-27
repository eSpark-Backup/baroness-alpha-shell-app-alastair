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
Java_com_simplejsi_SimpleJsiModule_nativeAdd(JNIEnv *env, jclass type, jdouble a, jdouble b, jlong jsi)
{

    double result = simplejsi::add(a, b);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_simplejsi_SimpleJsiModule_nativeRunCPP(JNIEnv *env, jclass type, jobject callback)
{

    LOGE("calling nativeRunCPP");
    // auto runtime = reinterpret_cast<jsi::Runtime *>(jsi);
    std::string messsage = simplejsi::runCPP();
    // Get the class of the Callback object
    jclass callbackClass = env->GetObjectClass(callback);
    std::cout << "jclass obj " << callbackClass;

    // Get the method ID of the invoke method of the Callback object
    jmethodID methodId = env->GetMethodID(callbackClass, "invoke", "(Ljava/lang/String;)V");
    if (methodId == nullptr)
    {
        return;
    }
    /// Convert the C++ string to a Java string
    jstring javaMessage = env->NewStringUTF(messsage.c_str());
    // Call the invoke method of the Callback object
    env->CallVoidMethod(callback, methodId, javaMessage);
    // Release the local reference to the Java string
    env->DeleteLocalRef(javaMessage);
}

// Helper function to execute JNI calls
void executeJNICallback(JNIEnv *env, jobject callback, const std::string &message)
{
    // Get the class of the Callback object
    jclass callbackClass = env->GetObjectClass(callback);
    if (callbackClass == nullptr)
    {
        LOGE("Failed to find callback class");
        return;
    }

    // Get the method ID of the invoke method of the Callback object
    jmethodID methodId = env->GetMethodID(callbackClass, "invoke", "(Ljava/lang/String;)V");
    if (methodId == nullptr)
    {
        LOGE("Failed to find invoke method");
        return;
    }

    // Convert the C++ string to a Java string
    jstring javaMessage = env->NewStringUTF(message.c_str());
    if (javaMessage == nullptr)
    {
        LOGE("Failed to create Java string");
        return;
    }

    // Call the invoke method of the Callback object
    env->CallVoidMethod(callback, methodId, javaMessage);

    // Release the local reference to the Java string
    env->DeleteLocalRef(javaMessage);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_simplejsi_SimpleJsiModule_nativeInstallAPK(JNIEnv *env, jclass type, jstring path, jobject callback)
{
    // Start the installation process asynchronously in a separate thread
    std::thread([&]() {
        // Execute the installation process here
        const char *nativePath = env->GetStringUTFChars(path, nullptr);
        std::string result = simplejsi::installAPKCMD(std::string(nativePath), [&](std::string message) {
            executeJNICallback(env, callback, message);
        });

        // Release JNI resources
        env->ReleaseStringUTFChars(path, nativePath);
    }).detach();

    // Return a dummy result string
    return env->NewStringUTF("Installation process started");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_simplejsi_SimpleJsiModule_nativeUninstallAPK(JNIEnv *env, jclass type, jstring packageName)
{
    const char *nativePackageName = env->GetStringUTFChars(packageName, nullptr);
    std::string result = simplejsi::uninstallAPKCMD(std::string(nativePackageName));
    env->ReleaseStringUTFChars(packageName, nativePackageName);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_reactnativesimplejsi_SimpleJsiModule_nativeInstall(JNIEnv *env, jobject thiz, jlong jsi)
{
    auto runtime = reinterpret_cast<facebook::jsi::Runtime *>(jsi);

    if (runtime)
    {
        simplejsi::install(*runtime);
    }
}
