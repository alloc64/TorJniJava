
#include <cstdarg>
#include <cstddef>
#include <vector>
#include <cstdio>

#include <android/log.h>
#include "Logger.h"

Logger *Logger::instance = nullptr;

void logFallback(LogPriority priority, const char *tag, const char *string) {
#if ANDROID
    __android_log_print(priority, tag, "%s", string);
#else
    fprintf(stdout, "%s: %s", tag, string);
#endif
}

void JNILogOverride(LogPriority priority, const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    Logger::log(priority, tag, msg, ap);
    va_end(ap);
}

JNILogPtr JNILog = JNILogOverride;

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

/**
 * Internal logging method, which transforms va_args into full string message.
 *
 * This method may be called from any thread, so any calls to managed world are made in separate thread via queue.
 *
 */
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

    auto string = temp.data();
    auto thiz = getInstance();

    if (thiz != nullptr) {
        thiz->enqueueLog(new LogEntry(priority, tag, string));
    } else {
        logFallback(priority, tag, string);
    }
}

void Logger::enqueueLog(LogEntry *logEntry) {
    {
        std::unique_lock<std::mutex> lock(this->mutex);
        this->queue.push_front(logEntry);
    }

    this->condition.notify_one();
}

void Logger::setJNIBridgeInstance(JNIEnv *env, jobject instance) {
    auto thiz = getInstance();

    thiz->jniBridgeInstance = env->NewGlobalRef(instance);

    if(!thiz->isRunning())
        thiz->start();
}

void Logger::run() {
    if (jniBridgeInstance == nullptr)
        return;

    auto vm = this->getVM();

    if (vm == nullptr)
        return;

    JNIEnv *env;
    vm->AttachCurrentThread(&env, nullptr);

    jclass clazz = env->GetObjectClass(jniBridgeInstance);
    auto logMethod = env->GetMethodID(clazz, "a12",
                                      "(ILjava/lang/String;Ljava/lang/String;)V");

    while (isRunning()) {
        std::unique_lock<std::mutex> lock(this->mutex);
        this->condition.wait(lock, [=] { return !this->queue.empty(); });

        LogEntry *logEntry = this->queue.back();
        this->queue.pop_back();

        if (logEntry == nullptr)
            return;

        if (logMethod == nullptr) {
            logFallback(logEntry->priority, logEntry->tag, logEntry->msg);
        } else {
            jstring jtag = env->NewStringUTF((const char *) logEntry->tag);
            jstring jstring = env->NewStringUTF((const char *) logEntry->msg);

            env->CallVoidMethod(jniBridgeInstance, logMethod, logEntry->priority, jtag, jstring);

            env->DeleteLocalRef(jtag);
            env->DeleteLocalRef(jstring);
        }
    }

    vm->DetachCurrentThread();

    Thread::run();
}
