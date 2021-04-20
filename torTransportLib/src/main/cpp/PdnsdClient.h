#ifndef PDNSDCLIENT_H
#define PDNSDCLIENT_H

#include "JNIAware.h"
#include "Thread.h"
#include <vector>

class PdnsdClient : public JNIAware, Thread {
public:
    PdnsdClient(JNIEnv *env) : JNIAware(env, "org/zeroprism/JNIBridge", std::vector<JNINativeMethod> {
            {"a8", "([Ljava/lang/String;)V", (void *) (PdnsdClient::startDnsd)},
            {"a9", "()V",                    (void *) (PdnsdClient::destroyDnsd)},
    }) {
        this->instance = this;
    }

protected:
    void run() override;

private:
    static PdnsdClient *getInstance() {
        return instance;
    }

    static void startDnsd(JNIEnv *env, jobject thiz, jobjectArray argv);

    static void destroyDnsd(JNIEnv *env, jobject thiz);

    static PdnsdClient *instance;

    void setArguments(jobjectArray pArray);

    std::vector<const char*> args;
};


#endif //PDNSDCLIENT_H
