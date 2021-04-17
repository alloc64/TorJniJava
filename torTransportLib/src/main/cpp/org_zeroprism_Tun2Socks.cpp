#include <jni.h>
#include "tun2socks_client.h"

jint runTun2SocksInterface(
        JNIEnv *env,
        jobject thiz,
        jint vpnInterfaceFileDescriptor,
        jint vpnInterfaceMTU,
        jstring vpnIpAddress,
        jstring vpnNetMask,
        jstring socksServerAddress,
        jstring udpgwServerAddress,
        jint udpgwTransparentDNS) {
    const char *vpnIpAddressStr = env->GetStringUTFChars(vpnIpAddress, nullptr);
    const char *vpnNetMaskStr = env->GetStringUTFChars(vpnNetMask, nullptr);
    const char *socksServerAddressStr = env->GetStringUTFChars(socksServerAddress, nullptr);
    const char *udpgwServerAddressStr = env->GetStringUTFChars(udpgwServerAddress, nullptr);

    runTun2Socks(vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddressStr, vpnNetMaskStr,
                 socksServerAddressStr, udpgwServerAddressStr, udpgwTransparentDNS);

    env->ReleaseStringUTFChars(vpnIpAddress, vpnIpAddressStr);
    env->ReleaseStringUTFChars(vpnNetMask, vpnNetMaskStr);
    env->ReleaseStringUTFChars(socksServerAddress, socksServerAddressStr);
    env->ReleaseStringUTFChars(udpgwServerAddress, udpgwServerAddressStr);

    return 1;
}

void destroyTun2SocksInterface(JNIEnv *env, jobject thiz) {
    terminateTun2Socks();
}