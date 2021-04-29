package com.alloc64.vpn.tor;

import android.net.VpnService;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

import java.net.InetAddress;
import java.util.concurrent.Executors;

public class SocksServerTunInterface implements Runnable
{
    private final int port;
    private ProxyServer socksProxyServer;

    public SocksServerTunInterface(int port)
    {
        this.port = port;
        this.socksProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
    }

    public void start(VpnService vpnService)
    {
        ProxyServer.setVpnService(vpnService);

        Executors.newSingleThreadExecutor().execute(this);
    }

    @Override
    public void run()
    {
        try
        {
            socksProxyServer.start(port, 5, InetAddress.getLocalHost());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void stop()
    {
        if (socksProxyServer == null)
            return;

        socksProxyServer.stop();
        socksProxyServer = null;
    }
}
