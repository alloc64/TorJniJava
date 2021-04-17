#ifndef ZP_TEST_TUN2SOCKS_CLIENT_H
#define ZP_TEST_TUN2SOCKS_CLIENT_H

extern "C" void runTun2Socks(int vpnInterfaceFileDescriptor, int vpnInterfaceMTU, const char *vpnIpAddressStr,
             const char *vpnNetMaskStr, const char *socksServerAddressStr,
             const char *udpgwServerAddressStr, int udpgwTransparentDNS);
extern "C"  void terminateTun2Socks();

#endif //ZP_TEST_TUN2SOCKS_CLIENT_H
