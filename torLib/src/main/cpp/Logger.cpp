
#include <cstdarg>
#include <cstddef>
#include <vector>
#include <cstdio>

#include <android/log.h>
#include "Logger.h"

Logger *Logger::instance = nullptr;

void Logger::d(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    log(LOG_DEBUG, tag, msg, ap);
    va_end(ap);
}

void Logger::e(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    log(LOG_ERROR, tag, msg, ap);
    va_end(ap);
}

void Logger::i(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    log(LOG_INFO, tag, msg, ap);
    va_end(ap);
}

void Logger::wtf(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    log(LOG_ERROR, tag, msg, ap);
    va_end(ap);
}

void Logger::v(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    log(LOG_VERBOSE, tag, msg, ap);
    va_end(ap);
}

void Logger::w(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    log(LOG_WARN, tag, msg, ap);
    va_end(ap);
}

void Logger::log(LogPriority priority, const char *tag, const char *msg, va_list args) {
    auto temp = std::vector<char>{};
    auto length = std::size_t{63};

    while (temp.size() <= length) {
        temp.resize(length + 1);
        const auto status = std::vsnprintf(temp.data(), temp.size(), msg, args);
        if (status < 0)
            break;

        length = static_cast<std::size_t>(status);
    }

    const char *string = temp.data();

    auto thiz = getInstance();

    if (thiz != nullptr) {
        auto jniBridgeInstance = thiz->jniBridgeInstance;

        if (jniBridgeInstance != nullptr) {
            auto vm = thiz->getVM();

            if (vm != nullptr) {
                JNIEnv *env;
                vm->AttachCurrentThread(&env, nullptr);

                jclass clazz = env->GetObjectClass(jniBridgeInstance);
                auto logMethod = env->GetMethodID(clazz, "a12", "(ILjava/lang/String;Ljava/lang/String;)V");

                if (logMethod != nullptr) {
                    jstring jtag = env->NewStringUTF((const char *) tag);
                    jstring jstring = env->NewStringUTF((const char *) string);

                    env->CallVoidMethod(jniBridgeInstance, logMethod, priority, jtag, jstring);
                } else {
#if ANDROID
                    __android_log_print(priority, tag, "%s", string);
#endif
                }

                vm->DetachCurrentThread();
            } else {
#if ANDROID
                __android_log_print(priority, tag, "%s", string);
#endif
            }
        }
    } else {
#if ANDROID
        __android_log_print(priority, tag, "%s", string);
#endif
    }
}

void Logger::setJNIBridgeInstance(JNIEnv *env, jobject instance) {
    getInstance()->jniBridgeInstance = env->NewGlobalRef(instance);
}

void JNILogOverride(LogPriority priority, const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    Logger::log(priority, tag, msg, ap);
    va_end(ap);
}

JNILogPtr JNILog = JNILogOverride;