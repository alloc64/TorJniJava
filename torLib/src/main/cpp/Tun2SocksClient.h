
#ifndef TUN2SOCKSCLIENT_H
#define TUN2SOCKSCLIENT_H

#include <jni.h>
#include "JNIAware.h"
#include "Thread.h"

class Tun2SocksClient : public JNIAware, public Thread {
public:
    Tun2SocksClient(JavaVM *vm, JNIEnv *env) : JNIAware(vm, "com/alloc64/jni/TLJNIBridge",
                                                        std::vector<JNINativeMethod>{
                                                                {"a10", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", (void *) (&Tun2SocksClient::createInterface)},
                                                                {"a11", "()V",                                                                            (void *) (&Tun2SocksClient::destroyInterface)},

                                                        }, env) {
        this->instance = this;
    }

private:
    static Tun2SocksClient *getInstance() {
        return instance;
    }

    static void createInterface(
            JNIEnv *env, jobject thiz, jint vpnInterfaceFileDescriptor, jint vpnInterfaceMTU,
            jstring vpnIpAddress, jstring vpnNetMask, jstring socksServerAddress,
            jstring udpgwServerAddress, jint udpgwTransparentDNS);

    static void destroyInterface(JNIEnv *env, jobject thiz);

protected:
    void run() override;

private:
    void setArguments(jint vpnInterfaceFileDescriptor,
                      jint vpnInterfaceMTU, jstring vpnIpAddress,
                      jstring vpnNetMask, jstring socksServerAddress,
                      jstring udpgwServerAddress, jint udpgwTransparentDNS);

    jint vpnInterfaceFileDescriptor;
    jint vpnInterfaceMTU;
    const char *vpnIpAddress;
    const char *vpnNetMask;
    const char *socksServerAddress;
    const char *udpgwServerAddress;
    jint udpgwTransparentDNS;

    static Tun2SocksClient *instance;
};


#endif //TUN2SOCKSCLIENT_H
