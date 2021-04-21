package com.alloc64.vpn.messenger;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ReplyHandler
{
    public static void reply(int messageType, Message m, IBasicMessage payload)
    {
        if(m != null)
            reply(messageType, m.replyTo, payload);
    }

    public static void reply(int messageType, Messenger msgr, IBasicMessage payload)
    {
        if(msgr != null)
        {
            try
            {
                msgr.send(Message.obtain(null, messageType, 0, 0, payload));
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }
    }
}
