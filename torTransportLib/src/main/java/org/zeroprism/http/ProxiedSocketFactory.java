package org.zeroprism.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import javax.net.SocketFactory;

public class ProxiedSocketFactory extends SocketFactory
{
    private final Proxy proxy;

    public ProxiedSocketFactory(Proxy proxy)
    {
        this.proxy = proxy;
    }

    public Socket createSocket()
    {
        return new Socket(proxy);
    }

    public Socket createSocket(String host, int port) throws IOException
    {
        return createProxiedSocket(new InetSocketAddress(host, port));
    }

    public Socket createSocket(InetAddress host, int port) throws IOException
    {
        return createProxiedSocket(new InetSocketAddress(host, port));
    }

    public Socket createSocket(String host, int port, InetAddress var3, int var4) throws IOException
    {
        return createProxiedSocket(new InetSocketAddress(host, port));
    }

    public Socket createSocket(InetAddress host, int port, InetAddress var3, int var4) throws IOException
    {
        return createProxiedSocket(new InetSocketAddress(host, port));
    }

    private Socket createProxiedSocket(InetSocketAddress addr) throws IOException
    {
        Socket socket = new Socket(proxy);
        socket.connect(addr);

        return socket;
    }
}
