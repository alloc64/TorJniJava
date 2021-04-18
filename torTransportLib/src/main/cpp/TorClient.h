#ifndef TORCLIENT_H
#define TORCLIENT_H

#include <tor_api.h>
#include "JNIAware.h"

#define TAG "zeroprism/TC"

class TorClient : public JNIAware {
public:
    //TODO: trampoline + rename native methods to something not useful
    TorClient(JNIEnv *env) : JNIAware(env, "org/zeroprism/Transport", (JNINativeMethod[]) {
            {"createTorConfiguration",          "()Z",                                          (void *) (TorClient::createTorConfig)},
            {"mainConfigurationFree",           "()V",                                          (void *) (TorClient::destroyTorConfig)},

            {"setupControlSocket",              "()Z",                                          (void *) (TorClient::setupTorControlSocket)},

            {"mainConfigurationSetCommandLine", "([Ljava/lang/String;)Z",                       (void *) (TorClient::setCommandLine)},

            {"prepareFileDescriptor",           "(Ljava/lang/String;)Ljava/io/FileDescriptor;", (void *) (TorClient::prepareFileDescriptor)},
            {"version",                         "()Ljava/lang/String;",                         (void *) (TorClient::version)},
            {"runMain",                         "()I",                                          (void *) (TorClient::runMain)},
    }) {
        this->instance = this;
    }

private:
    static TorClient *getInstance() {
        return instance;
    }

    static void initialize() {
        instance = nullptr;
    }

    static jstring version(JNIEnv *env, jobject thiz);

    static bool createTorConfig(JNIEnv *env, jobject thiz);

    static void destroyTorConfig(JNIEnv *env, jobject thiz);

    static int setupTorControlSocket(JNIEnv *env, jobject thiz);

    static bool setCommandLine(JNIEnv *env, jobject thiz, jobjectArray arrArgv);

    static int runMain(JNIEnv *env, jobject thiz);

    static jobject prepareFileDescriptor(JNIEnv *env, jclass thiz, jstring arg);

    tor_main_configuration_t * getTorConfig() const {
        return torConfig;
    }

    void setTorConfig(tor_main_configuration_t *torConfig) {
        this->torConfig = torConfig;
    }

    static TorClient *instance;
    tor_main_configuration_t *torConfig = nullptr;

};

#endif //TORCLIENT_H