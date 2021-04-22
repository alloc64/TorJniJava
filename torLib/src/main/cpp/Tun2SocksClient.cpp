#include <tun2socks_client.h>

#include "Tun2SocksClient.h"

Tun2SocksClient* Tun2SocksClient::instance = nullptr;

void Tun2SocksClient::createInterface(JNIEnv *env, jobject thiz, jint vpnInterfaceFileDescriptor,
                                      jint vpnInterfaceMTU, jstring vpnIpAddress,
                                      jstring vpnNetMask, jstring socksServerAddress,
                                      jstring udpgwServerAddress, jint udpgwTransparentDNS) {
    getInstance()->setArguments(vpnInterfaceFileDescriptor,
                                vpnInterfaceMTU, vpnIpAddress,
                                vpnNetMask, socksServerAddress,
                                udpgwServerAddress, udpgwTransparentDNS);
    getInstance()->start();
}

void Tun2SocksClient::run() {
    runTun2Socks(vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, vpnNetMask,
                 socksServerAddress, udpgwServerAddress, udpgwTransparentDNS);
}

void Tun2SocksClient::destroyInterface(JNIEnv *env, jobject thiz) {
    terminateTun2Socks();
}

void Tun2SocksClient::setArguments(jint vpnInterfaceFileDescriptor,
                                   jint vpnInterfaceMTU, jstring vpnIpAddress,
                                   jstring vpnNetMask, jstring socksServerAddress,
                                   jstring udpgwServerAddress, jint udpgwTransparentDNS) {
    auto env = getJNIEnv();

    this->vpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
    this->vpnInterfaceMTU = vpnInterfaceMTU;
    this->vpnIpAddress = env->GetStringUTFChars(vpnIpAddress, nullptr);
    this->vpnNetMask = env->GetStringUTFChars(vpnNetMask, nullptr);
    this->socksServerAddress = env->GetStringUTFChars(socksServerAddress, nullptr);
    this->udpgwServerAddress = env->GetStringUTFChars(udpgwServerAddress, nullptr);
    this->udpgwTransparentDNS = udpgwTransparentDNS; //TODO: fix string leak
}
