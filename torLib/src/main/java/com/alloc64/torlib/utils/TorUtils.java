/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package com.alloc64.torlib.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;

public class TorUtils
{
    public static int checkLocalPort(int port)
    {
        boolean isPortUsed;

        do
        {
            isPortUsed = isPortOpen("127.0.0.1", port, 500);

            if (isPortUsed)
                port++;
        }
        while (isPortUsed);

        return port;
    }

    public static boolean isPortOpen(String ip, int port, int timeout)
    {
        try
        {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        }
        catch (Exception ce)
        {
            return false;
        }
    }
}
