package com.alloc64.vpn.messenger;

import android.app.Service;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.Nullable;

public abstract class ServiceIncomingMessageHandler extends Handler
{
    private Service service;

    private Messenger clientMessenger;

    public ServiceIncomingMessageHandler(Service service)
    {
        this.service = service;
    }

    public Messenger createMessenger()
    {
        return new Messenger(this);
    }

    public Messenger getClientMessenger()
    {
        return clientMessenger;
    }

    public Service getService()
    {
        return service;
    }

    @Override
    public void handleMessage(Message msg)
    {
        if(msg == null)
        {
            Log.e(Config.tag, "Received invalid handler message");
            return;
        }

        if(msg.replyTo != null)
            this.clientMessenger = msg.replyTo;

        try
        {
            processMessage(msg.what, msg, (IBasicMessage) msg.obj);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected abstract void processMessage(int messageType, Message m, @Nullable IBasicMessage basicMessage);
}
