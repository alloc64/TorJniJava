package com.alloc64.vpn.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnError;
import com.alloc64.vpn.VpnMessageTypes;
import com.alloc64.vpn.messenger.BasicMessage;
import com.alloc64.vpn.messenger.ConnectionRequestMessage;
import com.alloc64.vpn.messenger.ConnectionStateMessage;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class AbstractVpnClient
{
    public interface StateCallback
    {
        void onMessageReceived(int messageType, BasicMessage message);

        void onStateChanged(VpnConnectionState state);

        void onError(VpnError error);
    }

    private static final String REQUEST = "req";
    private static final int CONNECT_REQUEST_CODE = 0x115;

    private Messenger vpnServiceMessenger = null;

    private Messenger responseMessenger = null;

    private StateCallback stateCallback;

    private boolean serviceBound;

    private VpnConnectionState state = VpnConnectionState.Disconnected;

    private ConnectionRequestMessage.Request tmpRequest;

    private final ServiceConnection serviceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            vpnServiceMessenger = new Messenger(service);
            serviceBound = true;

            sendMessage(VpnMessageTypes.GetState);
        }

        public void onServiceDisconnected(ComponentName className)
        {
            vpnServiceMessenger = null;
            serviceBound = false;
        }
    };

    public void create(Context ctx)
    {
        if (ctx == null)
            return;

        this.responseMessenger = new Messenger(new Handler(msg ->
        {
            try
            {
                onMessageReceived(msg, (BasicMessage) msg.obj);
                return true;
            }
            catch (Exception e)
            {
                onLogException(e);
            }

            return false;
        }));
    }

    public void rebind(Context ctx, Class<?> cls)
    {
        if (ctx == null)
            return;

        ctx = ctx.getApplicationContext();

        unbind(ctx);

        ctx.bindService(new Intent(ctx, cls), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbind(Context ctx)
    {
        try
        {
            if (serviceBound && ctx != null)
            {
                serviceBound = false;
                ctx.getApplicationContext()
                        .unbindService(serviceConnection);
            }
        }
        catch (Exception e)
        {
            serviceBound = false;
            onLogException(e);
        }
    }

    public void connect(Activity activity, ConnectionRequestMessage.Request request)
    {
        if (activity == null)
        {
            onError(VpnError.NoContext);
            return;
        }

        Intent intent = VpnService.prepare(activity.getApplicationContext());

        int rc = CONNECT_REQUEST_CODE;

        if (intent != null)
        {
            this.tmpRequest = request;

            try
            {
                activity.startActivityForResult(intent, rc);
            }
            catch (Exception e)
            {
                onLogException(e);

                onActivityResult(activity, rc, RESULT_CANCELED, null);
            }
        }
        else
        {
            intent = new Intent();
            intent.putExtra(REQUEST, request);

            onActivityResult(activity, rc, RESULT_OK, intent);
        }
    }

    protected void onVpnPrepared(Activity activity, ConnectionRequestMessage.Request request)
    {
        sendMessage(VpnMessageTypes.ServiceStateChange, new ConnectionRequestMessage(
                VpnConnectionState.Connecting,
                request
        ));
    }

    public void disconnect()
    {
        sendMessage(VpnMessageTypes.ServiceStateChange, new ConnectionStateMessage(VpnConnectionState.Disconnected));
    }

    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data)
    {
        try
        {
            if (requestCode == CONNECT_REQUEST_CODE)
            {
                ConnectionRequestMessage.Request request = tmpRequest;

                if (request == null)
                    request = data.getParcelableExtra(REQUEST);

                if (request == null)
                    onError(VpnError.InvalidRequest);

                if (resultCode == RESULT_OK)
                    onVpnPrepared(activity, request);
                else
                    onError(VpnError.VPNInterfaceCreationDenied);

                return true;
            }
        }
        catch (Exception e)
        {
            onLogException(e);
            onError(VpnError.FatalException);
        }

        return false;
    }

    public void destroy(Context ctx)
    {
        unbind(ctx);
    }

    //

    public void setStateCallback(StateCallback stateCallback)
    {
        this.stateCallback = stateCallback;
    }

    protected void sendMessage(int messageType)
    {
        sendMessage(messageType, null);
    }

    protected void sendMessage(int messageType, BasicMessage payload)
    {
        if (!serviceBound)
        {
            onError(VpnError.ServiceNotBound);
            return;
        }

        sendMessageNoCheck(messageType, payload);
    }

    protected void sendMessageNoCheck(int messageType)
    {
        sendMessageNoCheck(messageType, null);
    }

    protected void sendMessageNoCheck(int messageType, BasicMessage payload)
    {
        Message msg = Message.obtain(null, messageType, 0, 0, payload);

        try
        {
            msg.replyTo = responseMessenger;

            if (vpnServiceMessenger != null)
                vpnServiceMessenger.send(msg);
        }
        catch (Exception e)
        {
            onLogException(e);
        }
    }

    public void refreshState()
    {
        sendMessageNoCheck(VpnMessageTypes.GetState);
    }

    public VpnConnectionState getConnectionState()
    {
        return state;
    }

    // region Callbacks

    private void onMessageReceived(Message m, BasicMessage message)
    {
        onMessageReceived(m.what, message);
    }

    protected void onMessageReceived(int messageType, BasicMessage message)
    {
        switch (messageType)
        {
            case VpnMessageTypes.GetState:
            case VpnMessageTypes.ServiceStateChange:

                if (message instanceof ConnectionStateMessage)
                {
                    ConnectionStateMessage csm = (ConnectionStateMessage) message;
                    this.state = csm.getPayload();

                    VpnError error = csm.getError();

                    if (error != null && error != VpnError.None)
                        onError(csm.getError());

                    if (stateCallback != null)
                        stateCallback.onStateChanged(state);
                }

                break;
        }

        if (stateCallback != null)
            stateCallback.onMessageReceived(messageType, message);
    }

    protected void onError(VpnError error)
    {
        if (error == null)
            return;

        if (stateCallback != null)
            stateCallback.onError(error);
    }

    protected void onLogException(Exception e)
    {
        if (e != null)
            e.printStackTrace();
    }

    // endregion
}
