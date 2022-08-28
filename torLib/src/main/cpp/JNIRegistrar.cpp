/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#include <jni.h>

#include "Logger.h"
#include "JNIRegistrar.h"
#include "PdnsdClient.h"
#include "TorClient.h"
#include "Tun2SocksClient.h"

typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM *vm, void *) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = nullptr;

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK)
        return -1;

    if (JNIRegistrar::registerWithJNI(vm, uenv.env) != JNI_TRUE)
        return -2;

    return JNI_VERSION_1_4;
}

int JNIRegistrar::registerWithJNI(JavaVM *vm, JNIEnv *env) {
    new Logger(vm, env);

    // Register JNIAware instances here
    new TorClient(vm, env);
    new PdnsdClient(vm, env);
    new Tun2SocksClient(vm, env);

    return JNI_TRUE;
}
