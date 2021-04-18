#include <jni.h>

#include "Logger.h"
#include "JNIAware.h"

#define TAG "zeroprism/JNI"

JNIAware::JNIAware(JNIEnv *env, const char* className, JNINativeMethod *methods) {
    this->env = env;

    registerNativeMethods(className, methods, sizeof(methods) / sizeof(methods[0])); //TODO: overit
}

int JNIAware::registerNativeMethods(const char* className, JNINativeMethod *methods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        Logger::e(TAG, "Native method registration failed. Unable to find class %s", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods, numMethods) < 0) {
        Logger::e(TAG, "Native method registration failed for class %s", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEnv *JNIAware::getJNIEnv() const {
    return env;
}