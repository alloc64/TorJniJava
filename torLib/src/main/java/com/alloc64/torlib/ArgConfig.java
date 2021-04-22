package com.alloc64.torlib;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgConfig
{
    private final List<String> commands = new ArrayList<>();

    public ArgConfig addCommand(String... command)
    {
        commands.addAll(Arrays.asList(command));
        return this;
    }

    protected String convertBoolean(boolean val)
    {
        return val ? "1" : "0";
    }

    protected String addressString(InetSocketAddress socketAddress)
    {
        return String.format("%s:%s", socketAddress.getHostString(), socketAddress.getPort());
    }

    public String[] asCommands()
    {
        List<String> temp = new ArrayList<>();
        temp.addAll(commands);

        return temp.toArray(new String[0]);
    }
}
