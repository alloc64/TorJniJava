/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#ifndef TUN2SOCKSCLIENT_H
#define TUN2SOCKSCLIENT_H

#include <jni.h>
#include "JNIAware.h"
#include "Thread.h"

#define TAG "T2"

class Tun2SocksClient : public JNIAware, public Thread {
public:
    Tun2SocksClient(JavaVM *vm, JNIEnv *env) : JNIAware(vm, "com/alloc64/jni/TLJNIBridge",
                                                        std::vector<JNINativeMethod>{
                                                                {"a10", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void *) (Tun2SocksClient::createInterface)},
                                                                {"a11", "()V",                                                                           (void *) (Tun2SocksClient::destroyInterface)},
                                                                {"a14", "()Z",                                                                           (void *) (Tun2SocksClient::isInterfaceRunning)}

                                                        }, env) {
        this->instance = this;
    }

    void terminate() override;

    void cleanup() override;

protected:
    void run() override;

private:
    static Tun2SocksClient *getInstance() {
        return instance;
    }

    void setArguments(jint vpnInterfaceFileDescriptor,
                      jint vpnInterfaceMTU, jstring vpnIpAddress,
                      jstring vpnNetMask, jstring socksServerAddress,
                      jstring udpgwServerAddress);

    static void createInterface(
            JNIEnv *env, jobject thiz, jint vpnInterfaceFileDescriptor, jint vpnInterfaceMTU,
            jstring vpnIpAddress, jstring vpnNetMask, jstring socksServerAddress,
            jstring udpgwServerAddress);

    static void destroyInterface(JNIEnv *env, jobject thiz);

    static bool isInterfaceRunning(JNIEnv *env, jobject thiz);

    jint vpnInterfaceFileDescriptor;
    jint vpnInterfaceMTU;
    const char *vpnIpAddress;
    const char *vpnNetMask;
    const char *socksServerAddress;
    const char *udpgwServerAddress;

    static Tun2SocksClient *instance;
};


#endif //TUN2SOCKSCLIENT_H
