
#ifndef TUN2SOCKSCLIENT_H
#define TUN2SOCKSCLIENT_H

#include <jni.h>
#include "JNIAware.h"
#include "ProcessInThread.h"

class Tun2SocksClient : public JNIAware, public ProcessInThread {
public:
    //TODO: trampoline + rename native methods to something not useful
    Tun2SocksClient(JNIEnv *env) : JNIAware(env, "org/zeroprism/Transport", (JNINativeMethod[]) {
            {"runTun2SocksInterface",     "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", (void *) (&Tun2SocksClient::runTun2SocksInterface)},
            {"destroyTun2SocksInterface", "()V",                                                                            (void *) (&Tun2SocksClient::destroyTun2SocksInterface)},

    }) {
        this->instance = this;
    }

private:
    static Tun2SocksClient *getInstance() {
        return instance;
    }

    static void runTun2SocksInterface(
            JNIEnv *env, jobject thiz, jint vpnInterfaceFileDescriptor, jint vpnInterfaceMTU,
            jstring vpnIpAddress, jstring vpnNetMask, jstring socksServerAddress,
            jstring udpgwServerAddress, jint udpgwTransparentDNS);

    static void destroyTun2SocksInterface(JNIEnv *env, jobject thiz);

protected:
    void run() override;

private:
    void setArguments(jint vpnInterfaceFileDescriptor,
                      jint vpnInterfaceMTU, jstring vpnIpAddress,
                      jstring vpnNetMask, jstring socksServerAddress,
                      jstring udpgwServerAddress, jint udpgwTransparentDNS);

    jint vpnInterfaceFileDescriptor;
    jint vpnInterfaceMTU;
    const char* vpnIpAddress;
    const char*  vpnNetMask;
    const char*  socksServerAddress;
    const char*  udpgwServerAddress;
    jint udpgwTransparentDNS;

    static Tun2SocksClient *instance;
};


#endif //TUN2SOCKSCLIENT_H
