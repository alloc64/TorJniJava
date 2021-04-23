#include <jni.h>

#include <utility>

#include "Logger.h"
#include "JNIAware.h"

#define TAG "tl/JNI"

JNIAware::JNIAware(JNIEnv *env, const char* className, std::vector<JNINativeMethod> methods) {
    this->env = env;
    this->className = className;

    registerNativeMethods(className, std::move(methods));
}

int JNIAware::registerNativeMethods(const char* className, std::vector<JNINativeMethod> methods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        Logger::e(TAG, "Native method registration failed. Unable to find class %s", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods.data(), methods.size()) < 0) {
        Logger::e(TAG, "Native method registration failed for class %s", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEnv *JNIAware::getJNIEnv() const {
    return env;
}

const char *JNIAware::getClassName() const {
    return className;
}
