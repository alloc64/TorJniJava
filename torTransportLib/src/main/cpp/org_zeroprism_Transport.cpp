#include "tor_api.h"

#include <jni.h>
#include <string>
#include <fcntl.h>
#include <poll.h>
#include <unistd.h>
#include <cerrno>
#include <sys/socket.h>
#include <sys/un.h>

#include "org_zeroprism_Pdnsd.h"
#include "org_zeroprism_Tun2Socks.h"

#ifdef ANDROID
#include <android/log.h>
#define fprintf(ignored, ...)  __android_log_print(ANDROID_LOG_ERROR, "ZPC", ##__VA_ARGS__)
#endif // ANDROID


static const char *className = "org/zeroprism/Transport";

static char **argv = nullptr;
static int argc = 0;

static jfieldID
GetConfigurationFieldID(JNIEnv *env, jclass torApiClass) {
    return env->GetFieldID(torApiClass, "torConfiguration", "J");
}

static jlong
GetConfigurationObject(JNIEnv *env, jobject thiz) {
    jclass torApiClass = env->GetObjectClass(thiz);
    if (torApiClass == nullptr) {
        fprintf(stderr, "GetObjectClass returned nullptr\n");
        return 0;
    }

    jfieldID torConfigurationField = GetConfigurationFieldID(env, torApiClass);
    if (torConfigurationField == nullptr) {
        fprintf(stderr, "The fieldID is nullptr\n");
        return 0;
    }

    return env->GetLongField(thiz, torConfigurationField);
}

static bool
SetConfiguration(JNIEnv *env, jobject thiz,
                 const tor_main_configuration_t *torConfiguration) {
    jclass torApiClass = env->GetObjectClass(thiz);
    if (torApiClass == nullptr) {
        return false;
    }

    jfieldID torConfigurationField = GetConfigurationFieldID(env, torApiClass);
    if (torConfigurationField == nullptr) {
        return false;
    }

    auto cfg = (jlong) torConfiguration;

    env->SetLongField(thiz, torConfigurationField, cfg);
    return true;
}

static tor_main_configuration_t *
getConfiguration(JNIEnv *env, jobject thiz) {
    jlong torConfiguration = GetConfigurationObject(env, thiz);
    if (torConfiguration == 0) {
        fprintf(stderr, "The long is 0\n");
        return nullptr;
    }

    return (tor_main_configuration_t *) torConfiguration;
}

static jfieldID
GetControlSocketFieldID(JNIEnv *const env, jclass torApiClass) {
    return env->GetFieldID(torApiClass, "torControlFd", "I");
}

static bool
SetControlSocket(JNIEnv *env, jobject thiz, int socket) {
    jclass torApiClass = env->GetObjectClass(thiz);
    if (torApiClass == nullptr) {
        fprintf(stderr, "SetControlSocket: GetObjectClass returned nullptr\n");
        return false;
    }

    jfieldID controlFieldId = GetControlSocketFieldID(env, torApiClass);

    env->SetIntField(thiz, controlFieldId, socket);
    return true;
}

static bool
createTorConfiguration(JNIEnv *env, jobject thiz) {
    jlong torConfiguration = GetConfigurationObject(env, thiz);
    if (torConfiguration == 0)
        return false;

    tor_main_configuration_t *tor_config = tor_main_configuration_new();
    if (tor_config == nullptr) {
        fprintf(stderr, "Allocating and creating a new configuration structure failed.\n");
        return false;
    }

    if (!SetConfiguration(env, thiz, tor_config)) {
        tor_main_configuration_free(tor_config);
        return false;
    }

    return true;
}

static void destroyTorConfiguration(JNIEnv *env, jobject thiz) {
    tor_main_configuration_t *cfg = getConfiguration(env, thiz);
    if (cfg == nullptr) {
        fprintf(stderr, "ConfigurationFree: The Tor configuration is nullptr!\n");
        return;
    }

    tor_main_configuration_free(cfg);
}

static bool
setCommandLine(JNIEnv *env, jobject thiz, jobjectArray arrArgv) {
    tor_main_configuration_t *cfg = getConfiguration(env, thiz);
    if (cfg == nullptr) {
        fprintf(stderr, "setCommandLine: The Tor configuration is nullptr!\n");
        return -1;
    }

    jsize arrArgvLen = env->GetArrayLength(arrArgv);
    if (arrArgvLen > (INT_MAX - 1)) {
        fprintf(stderr, "Too many args\n");
        return false;
    }

    argc = (int) arrArgvLen;
    argv = (char **) malloc(argc * sizeof(char *)); //TODO: original call was to tor_malloc
    if (argv == nullptr) {
        return false;
    }

    for (jsize i = 0; i < argc; i++) {
        jobject objElm = env->GetObjectArrayElement(arrArgv, i);
        auto argElm = (jstring) objElm;
        const char *arg = env->GetStringUTFChars(argElm, nullptr);
        argv[i] = strdup(arg);
    }

    if (tor_main_configuration_set_command_line(cfg, argc, argv)) {
        fprintf(stderr, "Setting the command line config failed\n");
        return false;
    }
    return true;
}

static int
setupControlSocket(JNIEnv *env, jobject thiz) {
    jclass torApiClass = env->GetObjectClass(thiz);
    if (torApiClass == nullptr) {
        fprintf(stderr, "setupControlSocket: GetObjectClass returned nullptr\n");
        return false;
    }

    tor_main_configuration_t *cfg = getConfiguration(env, thiz);
    if (cfg == nullptr) {
        fprintf(stderr, "setupControlSocket: The Tor configuration is nullptr!\n");
        return false;
    }

    tor_control_socket_t tcs = tor_main_configuration_setup_control_socket(cfg);
    fcntl(tcs, F_SETFL, O_NONBLOCK);
    SetControlSocket(env, thiz, tcs);
    return true;
}

static int
runMain(JNIEnv *env, jobject thiz) {
    tor_main_configuration_t *cfg = getConfiguration(env, thiz);
    if (cfg == nullptr) {
        fprintf(stderr, "runMain: The Tor configuration is nullptr!\n");
        return -1;
    }

    int rv = tor_run_main(cfg);
    if (rv != 0) {
        fprintf(stderr, "Tor returned with an error\n");
    }

    return rv;
}

// region JNI methods

static jstring version(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(tor_api_get_provider_version());
}

/**
 * Android does not support UNIX Domain Sockets, but we can fake it by sending
 * the file descriptor via a java.io.FileDescriptor instance, which can be
 * used to open streams.  The field "fd" has been in Java forever.  In Android,
 * they renamed the field in 2008 to "descriptor", back when they did many
 * silly things like that.  It hasn't changed since then, e.g. Android 1.0.
 */
static jobject prepareFileDescriptor(JNIEnv *env, jclass thiz, jstring arg) {
    const char *filename = env->GetStringUTFChars(arg, nullptr);
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    struct sockaddr_un addr{};
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, filename, sizeof(addr.sun_path) - 1);
    env->ReleaseStringUTFChars(arg, filename);

    jclass io_exception = env->FindClass("java/io/IOException");
    if (io_exception == nullptr)
        return nullptr;

    if (fd < 0 || connect(fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        char buf[1024];
        snprintf(buf, 1023, "%s open: %s", filename, strerror(errno));
        env->ThrowNew(io_exception, buf);
        return nullptr;
    }

    jclass file_descriptor = env->FindClass("java/io/FileDescriptor");
    if (file_descriptor == nullptr)
        return nullptr;

    jmethodID file_descriptor_init = env->GetMethodID(file_descriptor, "<init>", "()V");
    if (file_descriptor_init == nullptr)
        return nullptr;

    jobject ret = env->NewObject(file_descriptor, file_descriptor_init);

    jfieldID field_fd = env->GetFieldID(file_descriptor, "descriptor", "I");

    if (field_fd == nullptr)
        return nullptr;

    env->SetIntField(ret, field_fd, fd);

    return ret;
}


static JNINativeMethod methods[] = {
        // tor stuff
        {"createTorConfiguration", "()Z", (void *) createTorConfiguration},
        {"setupControlSocket", "()Z", (void *) setupControlSocket},

        {"mainConfigurationSetCommandLine", "([Ljava/lang/String;)Z", (void *) setCommandLine},
        {"mainConfigurationFree", "()V", (void *) destroyTorConfiguration},

        {"prepareFileDescriptor", "(Ljava/lang/String;)Ljava/io/FileDescriptor;", (void *) prepareFileDescriptor},
        {"version", "()Ljava/lang/String;", (void *) version},
        {"runMain", "()I", (void *) runMain},

        // pdnsd stuff
        {"runDnsd", "([Ljava/lang/String;)I", (void *) runDnsd},
        {"destroyDnsd", "()I", (void *) destroyDnsd},

        // tun2socks stuff
        {"runTun2SocksInterface", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)I", (void *) runTun2SocksInterface},
        {"destroyTun2SocksInterface", "()V", (void *) destroyTun2SocksInterface},
};


static int registerNativeMethods(JNIEnv *env, JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        fprintf(stderr, "Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        fprintf(stderr, "RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static int registerNatives(JNIEnv *env) {
    return !registerNativeMethods(env, methods, sizeof(methods) / sizeof(methods[0])) ? JNI_FALSE
                                                                                      : JNI_TRUE;
}

typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM *vm, void *) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = nullptr;
    jint result = -1;
    JNIEnv *env;

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        fprintf(stderr, "vm->GetEnv failed");
        goto bail;
    }

    if (registerNatives(uenv.env) != JNI_TRUE) {
        fprintf(stderr, "registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

    bail:
    return result;
}

// endregion