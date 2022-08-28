/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package com.alloc64.vpn.messenger;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ReplyHandler
{
    public static void reply(int messageType, Message m, BasicMessage payload)
    {
        if(m != null)
            reply(messageType, m.replyTo, payload);
    }

    public static void reply(int messageType, Messenger msgr, BasicMessage payload)
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
