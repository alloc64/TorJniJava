cmake_minimum_required(VERSION 3.10.2)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_STANDARD 17)

project("transport")

#
# tor - precompiled .so
#
find_library(torlib tor PATHS ../libs/${CMAKE_ANDROID_ARCH_ABI}/)
if (NOT torlib)
    message(FATAL_ERROR "libtor.so not found. ")
endif ()

#
# pdnsd
#
set(PDNSD_FOLDER "../../../../external/pdnsd")
add_library(pdnsd STATIC
        ${PDNSD_FOLDER}/src/cache.c
        ${PDNSD_FOLDER}/src/conf-parser.c
        ${PDNSD_FOLDER}/src/conff.c
        ${PDNSD_FOLDER}/src/consts.c
        ${PDNSD_FOLDER}/src/debug.c
        ${PDNSD_FOLDER}/src/dns_answer.c
        ${PDNSD_FOLDER}/src/dns_query.c
        ${PDNSD_FOLDER}/src/dns.c
        ${PDNSD_FOLDER}/src/error.c
        ${PDNSD_FOLDER}/src/hash.c
        ${PDNSD_FOLDER}/src/helpers.c
        ${PDNSD_FOLDER}/src/icmp.c
        ${PDNSD_FOLDER}/src/list.c
        ${PDNSD_FOLDER}/src/netdev.c
        ${PDNSD_FOLDER}/src/rr_types.c
        ${PDNSD_FOLDER}/src/servers.c
        ${PDNSD_FOLDER}/src/status.c
        ${PDNSD_FOLDER}/src/thread.c
        pdnsd/pdnsd_client.c)
target_compile_options(pdnsd PRIVATE "-Wall")
target_compile_definitions(pdnsd PRIVATE "-DHAVE_STPCPY")
target_include_directories(pdnsd PRIVATE ${PDNSD_FOLDER})

#
# libancillary
#
set(LIBANCILLARY_FOLDER "../../../../external/libancillary")
add_library(libancillary STATIC ${LIBANCILLARY_FOLDER}/fd_recv.c ${LIBANCILLARY_FOLDER}/fd_send.c)
target_compile_options(libancillary PRIVATE "-O2")
target_include_directories(libancillary PRIVATE ${LIBANCILLARY_FOLDER})

#
# badvpn (tun2socks)
#
set(BADVPN_FOLDER "../../../../external/badvpn-guardian")
add_library(badvpn STATIC
        ${BADVPN_FOLDER}/base/BLog_syslog.c
        ${BADVPN_FOLDER}/system/BReactor_badvpn.c
        ${BADVPN_FOLDER}/system/BSignal.c
        ${BADVPN_FOLDER}/system/BConnection_unix.c
        ${BADVPN_FOLDER}/system/BTime.c
        ${BADVPN_FOLDER}/system/BUnixSignal.c
        ${BADVPN_FOLDER}/system/BNetwork.c
        ${BADVPN_FOLDER}/flow/StreamRecvInterface.c
        ${BADVPN_FOLDER}/flow/PacketRecvInterface.c
        ${BADVPN_FOLDER}/flow/PacketPassInterface.c
        ${BADVPN_FOLDER}/flow/StreamPassInterface.c
        ${BADVPN_FOLDER}/flow/SinglePacketBuffer.c
        ${BADVPN_FOLDER}/flow/BufferWriter.c
        ${BADVPN_FOLDER}/flow/PacketBuffer.c
        ${BADVPN_FOLDER}/flow/PacketStreamSender.c
        ${BADVPN_FOLDER}/flow/PacketPassConnector.c
        ${BADVPN_FOLDER}/flow/PacketProtoFlow.c
        ${BADVPN_FOLDER}/flow/PacketPassFairQueue.c
        ${BADVPN_FOLDER}/flow/PacketProtoEncoder.c
        ${BADVPN_FOLDER}/flow/PacketProtoDecoder.c
        ${BADVPN_FOLDER}/socksclient/BSocksClient.c
        ${BADVPN_FOLDER}/tuntap/BTap.c
        ${BADVPN_FOLDER}/lwip/src/core/timers.c
        ${BADVPN_FOLDER}/lwip/src/core/udp.c
        ${BADVPN_FOLDER}/lwip/src/core/memp.c
        ${BADVPN_FOLDER}/lwip/src/core/init.c
        ${BADVPN_FOLDER}/lwip/src/core/pbuf.c
        ${BADVPN_FOLDER}/lwip/src/core/tcp.c
        ${BADVPN_FOLDER}/lwip/src/core/tcp_out.c
        ${BADVPN_FOLDER}/lwip/src/core/netif.c
        ${BADVPN_FOLDER}/lwip/src/core/def.c
        ${BADVPN_FOLDER}/lwip/src/core/mem.c
        ${BADVPN_FOLDER}/lwip/src/core/tcp_in.c
        ${BADVPN_FOLDER}/lwip/src/core/stats.c
        ${BADVPN_FOLDER}/lwip/src/core/inet_chksum.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv4/icmp.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv4/igmp.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv4/ip4_addr.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv4/ip_frag.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv4/ip4.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv4/autoip.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/ethip6.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/inet6.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/ip6_addr.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/mld6.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/dhcp6.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/icmp6.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/ip6.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/ip6_frag.c
        ${BADVPN_FOLDER}/lwip/src/core/ipv6/nd6.c
        ${BADVPN_FOLDER}/lwip/custom/sys.c
        ${BADVPN_FOLDER}/base/DebugObject.c
        ${BADVPN_FOLDER}/base/BLog.c
        ${BADVPN_FOLDER}/base/BPending.c
        ${BADVPN_FOLDER}/system/BDatagram_unix.c
        ${BADVPN_FOLDER}/flowextra/PacketPassInactivityMonitor.c
        ${BADVPN_FOLDER}/tun2socks/SocksUdpGwClient.c
        ${BADVPN_FOLDER}/udpgw_client/UdpGwClient.c
        tun2socks/tun2socks_client.c)

set_property(TARGET badvpn PROPERTY C_STANDARD 99)
target_compile_definitions(badvpn PRIVATE "-DBADVPN_THREADWORK_USE_PTHREAD -DBADVPN_LINUX -DBADVPN_BREACTOR_BADVPN -D_GNU_SOURCE -DBADVPN_USE_SELFPIPE -DBADVPN_USE_EPOLL -DBADVPN_LITTLE_ENDIAN -DBADVPN_THREAD_SAFE -DNDEBUG -DANDROID -DTUN2SOCKS_JNI -DPSIPHON")
target_include_directories(badvpn PRIVATE
        ${LIBANCILLARY_FOLDER}
        ${BADVPN_FOLDER}
        ${BADVPN_FOLDER}/lwip/src/include/ipv4
        ${BADVPN_FOLDER}/lwip/src/include/ipv6
        ${BADVPN_FOLDER}/lwip/src/include
        ${BADVPN_FOLDER}/lwip/custom)

#
# android logcat
#
find_library(androidlog log)

#
# dynamic link lib
#
find_library(dllib dl)

#
# transport
#
add_library(transport SHARED
        JNIAware.cpp
        JNIRegistrar.cpp
        Logger.cpp
        TorClient.cpp
        PdnsdClient.cpp
        Thread.cpp
        Tun2SocksClient.cpp)
target_include_directories(transport PRIVATE "tor" "pdnsd" "tun2socks")
target_link_libraries(transport pdnsd libancillary badvpn ${torlib} ${androidlog} ${dllib})

#
# build hardening
#
target_compile_options(pdnsd PRIVATE "$<$<CONFIG:RELEASE>:-fvisibility=hidden>" "$<$<CONFIG:RELEASE>:-O3>" "$<$<CONFIG:RELEASE>:-ffunction-sections>" "$<$<CONFIG:RELEASE>:-fdata-sections>")
target_compile_options(libancillary PRIVATE "$<$<CONFIG:RELEASE>:-fvisibility=hidden>" "$<$<CONFIG:RELEASE>:-O3>" "$<$<CONFIG:RELEASE>:-ffunction-sections>" "$<$<CONFIG:RELEASE>:-fdata-sections>")
target_compile_options(badvpn PRIVATE "$<$<CONFIG:RELEASE>:-fvisibility=hidden>" "$<$<CONFIG:RELEASE>:-O3>" "$<$<CONFIG:RELEASE>:-ffunction-sections>" "$<$<CONFIG:RELEASE>:-fdata-sections>")
target_compile_options(transport PRIVATE "$<$<CONFIG:RELEASE>:-fvisibility=hidden>" "$<$<CONFIG:RELEASE>:-O3>" "$<$<CONFIG:RELEASE>:-ffunction-sections>" "$<$<CONFIG:RELEASE>:-fdata-sections>")
set_target_properties(transport PROPERTIES LINK_FLAGS_RELEASE -s)