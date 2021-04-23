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

    if (JNIRegistrar::registerWithJNI(uenv.env) != JNI_TRUE)
        return -2;

    return JNI_VERSION_1_4;
}

int JNIRegistrar::registerWithJNI(JNIEnv *env) {
    new Logger(env);

    // Register JNIAware instances here
    new TorClient(env);
    new PdnsdClient(env);
    new Tun2SocksClient(env);

    return JNI_TRUE;
}
