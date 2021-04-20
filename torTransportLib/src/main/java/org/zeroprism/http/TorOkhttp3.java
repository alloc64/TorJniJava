package org.zeroprism.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import javax.net.SocketFactory;

import okhttp3.OkHttpClient;

public class TorOkhttp3
{
    public static OkHttpClient.Builder create()
    {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.socketFactory(new ProxiedSocketFactory(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050))));

        return builder;
    }
}


