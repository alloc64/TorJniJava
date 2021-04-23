#ifndef TORCLIENT_H
#define TORCLIENT_H

#include <tor_api.h>
#include "JNIAware.h"
#include "Thread.h"

#define TAG "TC"

class TorClient :
        public JNIAware,
        public Thread {
public:
    TorClient(JavaVM *vm, JNIEnv *env) : JNIAware(vm, "com/alloc64/jni/TLJNIBridge",
                                                  std::vector<JNINativeMethod>{
                                                          {"a1", "()Ljava/lang/String;",   (void *) (TorClient::torVersion)},
                                                          {"a2", "()Z",                    (void *) (TorClient::createTorConfig)},
                                                          {"a3", "()V",                    (void *) (TorClient::destroyTor)},
                                                          {"a4", "()Z",                    (void *) (TorClient::isTorRunning)},
                                                          {"a5", "([Ljava/lang/String;)Z", (void *) (TorClient::setTorCommandLine)},
                                                          {"a6", "()V",                    (void *) (TorClient::startTor)},
                                                  }, env) {
        this->instance = this;
    }

    void terminate() override;
    void cleanup() override;

private:
    static TorClient *getInstance() {
        return instance;
    }

    static jstring torVersion(JNIEnv *env, jobject thiz);

    static bool createTorConfig(JNIEnv *env, jobject thiz);

    static void destroyTor(JNIEnv *env, jobject thiz);

    static bool isTorRunning(JNIEnv *env, jobject thiz);

    static bool setTorCommandLine(JNIEnv *env, jobject thiz, jobjectArray arrArgv);

    static void startTor(JNIEnv *env, jobject thiz);

    tor_main_configuration_t *getTorConfig() const {
        return torConfig;
    }

    void setTorConfig(tor_main_configuration_t *torConfig) {
        this->torConfig = torConfig;
    }

protected:
    void run() override;

private:

    static TorClient *instance;
    tor_main_configuration_t *torConfig = nullptr;
};

#endif //TORCLIENT_H
