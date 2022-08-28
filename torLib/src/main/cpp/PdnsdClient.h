/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#ifndef PDNSDCLIENT_H
#define PDNSDCLIENT_H

#include "JNIAware.h"
#include "Thread.h"
#include <vector>

#define TAG "PD"

class PdnsdClient : public JNIAware, Thread {
public:
    PdnsdClient(JavaVM *vm, JNIEnv *env) : JNIAware(vm, "com/alloc64/jni/TLJNIBridge",
                                                    std::vector<JNINativeMethod>{
                                                            {"a8", "([Ljava/lang/String;)V", (void *) (PdnsdClient::startDnsd)},
                                                            {"a9", "()V",                    (void *) (PdnsdClient::destroyPdnsd)},
                                                            {"a7", "()Z",                    (void *) (PdnsdClient::isPdnsdRunning)}
                                                    }, env) {
        this->instance = this;
    }

    void terminate() override;

    void cleanup() override;

protected:
    void run() override;

private:
    static PdnsdClient *getInstance() {
        return instance;
    }

    static void startDnsd(JNIEnv *env, jobject thiz, jobjectArray argv);

    static void destroyPdnsd(JNIEnv *env, jobject thiz);

    static bool isPdnsdRunning(JNIEnv *env, jobject thiz);

    static PdnsdClient *instance;

    void setArguments(jobjectArray pArray);

    std::vector<const char *> args;
};


#endif //PDNSDCLIENT_H
