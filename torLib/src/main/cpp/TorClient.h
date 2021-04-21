#ifndef TORCLIENT_H
#define TORCLIENT_H

#include <tor_api.h>
#include "JNIAware.h"
#include "Thread.h"

#define TAG "zeroprism/TC"

class TorClient :
        public JNIAware,
        public Thread {
public:
    TorClient(JNIEnv *env) : JNIAware(env, "com/alloc64/jni/TLJNIBridge", std::vector<JNINativeMethod> {
            {"a1", "()Ljava/lang/String;",                         (void *) (TorClient::torVersion)},
            {"a2", "()Z",                                          (void *) (TorClient::createTorConfig)},
            {"a3", "()V",                                          (void *) (TorClient::destroyTorConfig)},
            {"a4", "()I",                                          (void *) (TorClient::setupTorControlSocket)},
            {"a5", "([Ljava/lang/String;)Z",                       (void *) (TorClient::setTorCommandLine)},
            {"a6", "(Ljava/lang/String;)Ljava/io/FileDescriptor;", (void *) (TorClient::prepareFileDescriptor)},
            {"a7", "()V",                                          (void *) (TorClient::startTor)},
    }) {
        this->instance = this;
    }

private:
    static TorClient *getInstance() {
        return instance;
    }

    static jstring torVersion(JNIEnv *env, jobject thiz);

    static bool createTorConfig(JNIEnv *env, jobject thiz);

    static void destroyTorConfig(JNIEnv *env, jobject thiz);

    static int setupTorControlSocket(JNIEnv *env, jobject thiz);

    static bool setTorCommandLine(JNIEnv *env, jobject thiz, jobjectArray arrArgv);

    static void startTor(JNIEnv *env, jobject thiz);

    static jobject prepareFileDescriptor(JNIEnv *env, jclass thiz, jstring arg);

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
