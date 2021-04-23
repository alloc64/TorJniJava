#include <tun2socks_client.h>

#include "Tun2SocksClient.h"
#include "Logger.h"

Tun2SocksClient *Tun2SocksClient::instance = nullptr;

void Tun2SocksClient::createInterface(JNIEnv *env, jobject thiz, jint vpnInterfaceFileDescriptor,
                                      jint vpnInterfaceMTU, jstring vpnIpAddress,
                                      jstring vpnNetMask, jstring socksServerAddress,
                                      jstring udpgwServerAddress) {
    getInstance()->setArguments(vpnInterfaceFileDescriptor,
                                vpnInterfaceMTU, vpnIpAddress,
                                vpnNetMask, socksServerAddress,
                                udpgwServerAddress);
    getInstance()->start();
}

void Tun2SocksClient::setArguments(jint vpnInterfaceFileDescriptor,
                                   jint vpnInterfaceMTU, jstring vpnIpAddress,
                                   jstring vpnNetMask, jstring socksServerAddress,
                                   jstring udpgwServerAddress) {
    auto env = getJNIEnv();

    this->vpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
    this->vpnInterfaceMTU = vpnInterfaceMTU;
    this->vpnIpAddress = env->GetStringUTFChars(vpnIpAddress, nullptr);
    this->vpnNetMask = env->GetStringUTFChars(vpnNetMask, nullptr);
    this->socksServerAddress = env->GetStringUTFChars(socksServerAddress, nullptr);
    this->udpgwServerAddress = env->GetStringUTFChars(udpgwServerAddress, nullptr);

    env->ReleaseStringUTFChars(vpnIpAddress, this->vpnIpAddress);
    env->ReleaseStringUTFChars(vpnNetMask, this->vpnNetMask);
    env->ReleaseStringUTFChars(socksServerAddress, this->socksServerAddress);
    env->ReleaseStringUTFChars(udpgwServerAddress, this->udpgwServerAddress);
}

void Tun2SocksClient::run() {
    runTun2Socks(vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, vpnNetMask,
                 socksServerAddress, udpgwServerAddress, 1);

    Thread::run();
}

void Tun2SocksClient::cleanup() {
    Thread::cleanup();

    this->vpnInterfaceFileDescriptor = 0;
    this->vpnInterfaceMTU = 0;

    delete vpnIpAddress;
    this->vpnIpAddress = nullptr;

    delete vpnNetMask;
    this->vpnNetMask = nullptr;

    delete socksServerAddress;
    this->socksServerAddress = nullptr;

    delete udpgwServerAddress;
    this->udpgwServerAddress = nullptr;
}

void Tun2SocksClient::terminate() {
    if (!this->isRunning()) {
        Logger::e(TAG, "Unable to terminate non-running T2 client.");
        return;
    }

    terminateTun2Socks();
    Thread::terminate();
}

bool Tun2SocksClient::isInterfaceRunning(JNIEnv *env, jobject thiz) {
    return getInstance()->isRunning();
}
void Tun2SocksClient::destroyInterface(JNIEnv *env, jobject thiz) {
    getInstance()->terminate();
}
