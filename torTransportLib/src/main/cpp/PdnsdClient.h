#ifndef PDNSDCLIENT_H
#define PDNSDCLIENT_H

#include "JNIAware.h"
#include "ProcessInThread.h"

class PdnsdClient : public JNIAware, ProcessInThread {
public:
    //TODO: trampoline + rename native methods to something not useful
    PdnsdClient(JNIEnv *env) : JNIAware(env, "org/zeroprism/Transport", (JNINativeMethod[]) {
            {"runDnsd",     "([Ljava/lang/String;)V", (void *) (PdnsdClient::runDnsd)},
            {"destroyDnsd", "()V",                    (void *) (PdnsdClient::destroyDnsd)},
    }) {
        this->instance = this;
    }

protected:
    void run() override;

private:
    static PdnsdClient *getInstance() {
        return instance;
    }

    static void runDnsd(JNIEnv *env, jobject thiz, jobjectArray argv);

    static void destroyDnsd(JNIEnv *env, jobject thiz);

    static PdnsdClient *instance;

    void setArguments(jobjectArray pArray);

    jsize argc;
    char **argv;
};


#endif //PDNSDCLIENT_H
