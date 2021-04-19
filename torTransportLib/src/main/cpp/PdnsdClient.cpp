#include <pdnsd_client.h>

#include "PdnsdClient.h"

PdnsdClient *PdnsdClient::instance = nullptr;

void PdnsdClient::run() {
    runPdnsd(this->args.size(), const_cast<char **>(this->args.data()));
}

void PdnsdClient::startDnsd(JNIEnv *env, jobject thiz, jobjectArray argv) {
    getInstance()->setArguments(argv);
    getInstance()->start();
}

void PdnsdClient::destroyDnsd(JNIEnv *env, jobject thiz) {
    getInstance()->terminate();
}

void PdnsdClient::setArguments(jobjectArray argv) {
    auto env = getJNIEnv();

    int length = env->GetArrayLength(argv);

    for (int i = 0; i < length; i++) {
        auto string = (jstring) env->GetObjectArrayElement(argv, i);
        auto *rawString = env->GetStringUTFChars(string, 0);
        this->args.push_back(strdup(rawString));
        env->ReleaseStringUTFChars(string, rawString);
    }
}
