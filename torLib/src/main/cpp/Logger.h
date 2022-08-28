/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#ifndef LOGGER_H
#define LOGGER_H

#include <mutex>
#include <condition_variable>
#include <deque>

#include <jni.h>
#include "JNIAware.h"
#include "JNILogger.h"
#include "Thread.h"

class LogEntry {
public:
    LogEntry(LogPriority priority, const char *tag, const char *msg) {
        this->priority = priority;
        this->tag = tag;
        this->msg = msg;
    }

    LogPriority priority;
    const char *tag;
    const char *msg;
};

class Logger : public JNIAware, public Thread {

public:
    Logger(JavaVM *vm, JNIEnv *env) : JNIAware(vm, "com/alloc64/jni/TLJNIBridge",
                                               std::vector<JNINativeMethod>{
                                                       {"a13", "(Lcom/alloc64/jni/TLJNIBridge;)V", (void *) (Logger::setJNIBridgeInstance)},
                                               }, env) {
        this->instance = this;
    }

    ~Logger() {
        auto env = getJNIEnv();

        if (jniBridgeInstance != nullptr && env != nullptr) {
            env->DeleteGlobalRef(jniBridgeInstance);
            this->jniBridgeInstance = nullptr;
        }

        this->terminate();
    }

    static void d(const char *tag, const char *msg, ...);

    static void e(const char *tag, const char *msg, ...);

    static void i(const char *tag, const char *msg, ...);

    static void wtf(const char *tag, const char *msg, ...);

    static void v(const char *tag, const char *msg, ...);

    static void w(const char *tag, const char *msg, ...);

    static void log(LogPriority priority, const char *tag, const char *msg, va_list params);

protected:
    void run() override;

private:
    static Logger *instance;

    static Logger *getInstance() {
        return instance;
    }

    static void setJNIBridgeInstance(JNIEnv *env, jobject instance);

    void enqueueLog(LogEntry *logEntry);

    jobject jniBridgeInstance = nullptr;

    std::mutex mutex;
    std::condition_variable condition;
    std::deque<LogEntry*> queue;

};

#endif //LOGGER_H
