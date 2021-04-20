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

int TorClient::setupTorControlSocket(JNIEnv *env, jobject thiz) {
    auto torConfig = getInstance()->getTorConfig();
    if (torConfig == nullptr) {
        Logger::e(TAG, "Config must be first created, to use this method.");
        return false;
    }

    tor_control_socket_t torControlSocket = tor_main_configuration_setup_control_socket(torConfig);
    fcntl(torControlSocket, F_SETFL, O_NONBLOCK);

    return torControlSocket;
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

/**
 * Android does not support UNIX Domain Sockets, but we can fake it by sending
 * the file descriptor via a java.io.FileDescriptor instance, which can be
 * used to open streams.  The field "fd" has been in Java forever.  In Android,
 * they renamed the field in 2008 to "descriptor", back when they did many
 * silly things like that.  It hasn't changed since then, e.g. Android 1.0.
 */
jobject TorClient::prepareFileDescriptor(JNIEnv *env, jclass thiz, jstring arg) {
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

