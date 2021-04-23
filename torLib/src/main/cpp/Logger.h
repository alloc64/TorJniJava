#ifndef LOGGER_H
#define LOGGER_H

#include <jni.h>
#include "JNIAware.h"
#include "JNILogger.h"

class Logger : public JNIAware {

public:
    Logger(JavaVM *vm, JNIEnv *env) : JNIAware(vm, "com/alloc64/jni/TLJNIBridge",
                                   std::vector<JNINativeMethod>{
                                           {"a13", "(Lcom/alloc64/jni/TLJNIBridge;)V", (void *) (Logger::setJNIBridgeInstance)},
                                   }, env) {
        this->instance = this;
    }
    ~Logger() {
        auto env = getJNIEnv();

        if(jniBridgeInstance != nullptr && env != nullptr) {
            env->DeleteGlobalRef(jniBridgeInstance);
            this->jniBridgeInstance = nullptr;
        }
    }
    static void d(const char *tag, const char *msg, ...);

    static void e(const char *tag, const char *msg, ...);

    static void i(const char *tag, const char *msg, ...);

    static void wtf(const char *tag, const char *msg, ...);

    static void v(const char *tag, const char *msg, ...);

    static void w(const char *tag, const char *msg, ...);

    static void log(LogPriority priority, const char *tag, const char *msg, va_list params);

private:
    static Logger *instance;

    static Logger *getInstance() {
        return instance;
    }

    static void setJNIBridgeInstance(JNIEnv *env, jobject instance);

    jobject jniBridgeInstance = nullptr;
};

#endif //LOGGER_H
