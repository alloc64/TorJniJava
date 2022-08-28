/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#include <pdnsd_client.h>

#include "PdnsdClient.h"
#include "Logger.h"

PdnsdClient *PdnsdClient::instance = nullptr;

void PdnsdClient::startDnsd(JNIEnv *env, jobject thiz, jobjectArray argv) {
    getInstance()->setArguments(argv);
    getInstance()->start();
}

void PdnsdClient::destroyPdnsd(JNIEnv *env, jobject thiz) {
    getInstance()->terminate();
}

void PdnsdClient::setArguments(jobjectArray argv) {
    auto env = getJNIEnv();

    int length = env->GetArrayLength(argv);

    for (int i = 0; i < length; i++) {
        auto string = (jstring) env->GetObjectArrayElement(argv, i);
        auto *rawString = env->GetStringUTFChars(string, nullptr);
        this->args.push_back(strdup(rawString));
        env->ReleaseStringUTFChars(string, rawString);
    }
}

void PdnsdClient::run() {
    runPdnsd(this->args.size(), const_cast<char **>(this->args.data()));

    Thread::run();
}

void PdnsdClient::cleanup() {
    Thread::cleanup();

    for(auto &arg : this->args) {
        delete arg;
        arg = nullptr;
    }

    this->args.clear();
}

void PdnsdClient::terminate() {
    if(!this->isRunning()) {
        Logger::e(TAG, "Unable to terminate non-running T2 client.");
        return;
    }

    terminatePdnsd();
    Thread::terminate();
}

bool PdnsdClient::isPdnsdRunning(JNIEnv *env, jobject thiz) {
    return getInstance()->isRunning();
}
