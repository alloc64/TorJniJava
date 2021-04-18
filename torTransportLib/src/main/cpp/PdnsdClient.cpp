#include <pdnsd_client.h>

#include "PdnsdClient.h"

PdnsdClient *PdnsdClient::instance = nullptr;

void PdnsdClient::run() {
    runPdnsd(this->argc, this->argv);
}

void PdnsdClient::runDnsd(JNIEnv *env, jobject thiz, jobjectArray argv) {
    getInstance()->setArguments(argv);
    getInstance()->start();
}

void PdnsdClient::destroyDnsd(JNIEnv *env, jobject thiz) {
    getInstance()->terminate();
}

void PdnsdClient::setArguments(jobjectArray argv) {
    auto env = getJNIEnv();

    this->argc = env->GetArrayLength(argv);
    this->argv = static_cast<char **>(malloc(this->argc * sizeof(char *)));

    for (int i = 0; i < this->argc; i++) {
        auto string = (jstring) env->GetObjectArrayElement(argv, i);
        auto *rawString = env->GetStringUTFChars(string, 0);
        this->argv[i] = strdup(rawString);
        env->ReleaseStringUTFChars(string, rawString);
    }
}
