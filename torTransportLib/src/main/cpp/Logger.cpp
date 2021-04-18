//
// Created by user on 18.04.2021.
//

#include <android/log.h>
#include "Logger.h"

void Logger::d(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    __android_log_vprint(ANDROID_LOG_DEBUG, tag, msg, ap);
    va_end(ap);
}

void Logger::e(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    __android_log_vprint(ANDROID_LOG_ERROR, tag, msg, ap);
    va_end(ap);
}

void Logger::i(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    __android_log_vprint(ANDROID_LOG_INFO, tag, msg, ap);
    va_end(ap);
}

void Logger::wtf(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    __android_log_vprint(ANDROID_LOG_ERROR, tag, msg, ap);
    va_end(ap);
}

void Logger::v(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    __android_log_vprint(ANDROID_LOG_VERBOSE, tag, msg, ap);
    va_end(ap);
}

void Logger::w(const char *tag, const char *msg, ...) {
    va_list ap;
    va_start(ap, msg);
    __android_log_vprint(ANDROID_LOG_WARN, tag, msg, ap);
    va_end(ap);
}
