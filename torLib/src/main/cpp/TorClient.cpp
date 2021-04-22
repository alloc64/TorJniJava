#include <jni.h>
#include <string>
#include <fcntl.h>
#include <poll.h>
#include <unistd.h>
#include <cerrno>
#include <sys/socket.h>
#include <sys/un.h>

#include "TorClient.h"
#include "Logger.h"

TorClient* TorClient::instance = nullptr;

jstring TorClient::torVersion(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(tor_api_get_provider_version());
}

bool TorClient::createTorConfig(JNIEnv *env, jobject thiz) {
    auto torConfig = getInstance()->getTorConfig();

    if(torConfig != nullptr) {
        tor_main_configuration_free(torConfig);
        torConfig = nullptr;
    }

    torConfig = tor_main_configuration_new();
    if (torConfig == nullptr) {
        Logger::e(TAG, "Creating a new configuration failed.");
        return false;
    }

    getInstance()->setTorConfig(torConfig);

    return true;
}

void TorClient::destroyTorConfig(JNIEnv *env, jobject thiz) {
    auto torConfig = getInstance()->getTorConfig();

    if(torConfig != nullptr)
        tor_main_configuration_free(torConfig);
}

bool TorClient::setTorCommandLine(JNIEnv *env, jobject thiz, jobjectArray arrArgv) {
    auto torConfig = getInstance()->getTorConfig();
    if (torConfig == nullptr) {
        Logger::e(TAG, "Config must be first created, to use this method.");
        return false;
    }

    jsize arrArgvLen = env->GetArrayLength(arrArgv);
    if (arrArgvLen > (INT_MAX - 1)) {
        Logger::e(TAG, "Invalid argument count.");
        return false;
    }

    int argc = (int) arrArgvLen;
    char ** argv = (char **) malloc(argc * sizeof(char *)); //TODO: original call was to tor_malloc
    if (argv == nullptr)
        return false;

    for (jsize i = 0; i < argc; i++) {
        jobject argElem = env->GetObjectArrayElement(arrArgv, i);
        const char *arg = env->GetStringUTFChars((jstring) argElem, nullptr);
        argv[i] = strdup(arg);
        env->DeleteLocalRef(argElem);
    }

    if (tor_main_configuration_set_command_line(torConfig, argc, argv) < 0) {
        Logger::e(TAG,  "Unable to set cmd config.");
        return false;
    }

    return true;
}

void TorClient::startTor(JNIEnv *env, jobject thiz) {
    getInstance()->start();
}

void TorClient::run() {
    auto torConfig = getInstance()->getTorConfig();
    if (torConfig == nullptr) {
        Logger::e(TAG, "Config must be first created, to start.");
        return;
    }

    int rv = tor_run_main(torConfig);

    if (rv != 0)
        Logger::e(TAG, "An error occured while starting daemon: %d", rv);
}
