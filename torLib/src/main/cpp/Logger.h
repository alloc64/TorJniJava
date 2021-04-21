#ifndef LOGGER_H
#define LOGGER_H

#include <jni.h>

class Logger {

public:

    static void d(const char* tag, const char* msg, ...);

    static void e(const char* tag, const char* msg, ...);

    static void i(const char* tag, const char* msg, ...);

    static void wtf(const char* tag, const char* msg, ...);

    static void v(const char* tag, const char* msg, ...);

    static void w(const char* tag, const char* msg, ...);

    static void setEnv(JNIEnv *e) {
        Logger::env = e;
    }

private:
    static JNIEnv *env;
};

#endif //LOGGER_H
