package com.alloc64.vpn.messenger;

import android.app.Service;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public abstract class ServiceIncomingMessageHandler extends Handler
{
    private static final String TAG = ServiceIncomingMessageHandler.class.toString();

    private final Service service;

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
            Log.e(TAG, "Received invalid handler message");
            return;
        }

        if(msg.replyTo != null)
            this.clientMessenger = msg.replyTo;

        try
        {
            processMessage(msg.what, msg, (BasicMessage) msg.obj);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected abstract void processMessage(int messageType, Message m, BasicMessage basicMessage);
}
