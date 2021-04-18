
#ifndef ORG_ZEROPRISM_TUN2SOCKS_H
#define ORG_ZEROPRISM_TUN2SOCKS_H

jint runTun2SocksInterface(
        JNIEnv *env,
        jobject thiz,
        jint vpnInterfaceFileDescriptor,
        jint vpnInterfaceMTU,
        jstring vpnIpAddress,
        jstring vpnNetMask,
        jstring socksServerAddress,
        jstring udpgwServerAddress,
        jint udpgwTransparentDNS);

void destroyTun2SocksInterface(JNIEnv *env, jobject thiz);

#endif //ORG_ZEROPRISM_TUN2SOCKS_H
