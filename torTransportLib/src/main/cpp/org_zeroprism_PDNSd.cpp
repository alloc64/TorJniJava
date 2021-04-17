
#include <pthread.h>
#include <jni.h>
#include <malloc.h>
#include <string.h>

#include "pdnsd_client.h"

struct DnsArgs {
    int argc;
    char **argv;
};

pthread_t running_thread;

void *thread_runner(void *a) {
    auto *args = (struct DnsArgs *) a;
    runPdnsd(args->argc, args->argv);
    return nullptr;
}

jint runDnsd(JNIEnv *env, jobject thiz, jobjectArray argv) {
    //TODO: staticize + sanitize to allow only single instance

    auto *a = static_cast<DnsArgs *>(malloc(sizeof(struct DnsArgs *)));
    a->argc = env->GetArrayLength(argv);
    a->argv = static_cast<char **>(malloc(a->argc * sizeof(char *)));

    for (int i = 0; i < a->argc; i++) {
        auto string = (jstring) env->GetObjectArrayElement(argv, i);
        auto *rawString = env->GetStringUTFChars(string, 0);
        a->argv[i] = strdup(rawString);
        env->ReleaseStringUTFChars(string, rawString);
    }

    pthread_create(&running_thread, nullptr, thread_runner, a);

    return 0;
}

jint destroyDnsd(JNIEnv *env, jobject thiz) {
    if (running_thread == 0)
        return -1;

    return pthread_kill(running_thread, 0);
}
