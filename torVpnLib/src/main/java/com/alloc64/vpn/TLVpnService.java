package com.alloc64.vpn;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.alloc64.vpn.messenger.ConnectionRequestMessage;
import com.alloc64.vpn.messenger.ConnectionStateMessage;
import com.alloc64.vpn.messenger.BasicMessage;
import com.alloc64.vpn.messenger.ReplyHandler;
import com.alloc64.vpn.messenger.ServiceIncomingMessageHandler;
import com.alloc64.vpn.tor.TorVpnProvider;

public class TLVpnService extends VpnService implements IVpnService
{
    private static final String TAG = TLVpnService.class.getName();

    private TorVpnProvider torVpnProvider;

    private ParcelFileDescriptor tunInterface;

    private VpnConnectionState state = VpnConnectionState.Disconnected;

    private Messenger serviceMessenger;

    private ServiceIncomingMessageHandler serviceIncomingMessageHandler;

    private VpnError error;

    public VpnConnectionState getState()
    {
        return state;
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate()
    {
        super.onCreate();

        this.torVpnProvider = new TorVpnProvider(this)
        {
            @Override
            public void onConnecting()
            {
                // ignored
            }

            @Override
            public void onConnected()
            {
                setStateInternal(VpnConnectionState.Connected);
            }

            @Override
            public void onDisconnected()
            {
                // ignored
            }

            @Override
            public void onException(Exception exception)
            {
                VpnException e;

                if (exception instanceof VpnException)
                    e = (VpnException) exception;
                else
                    e = new VpnException(VpnError.FatalException, exception.getMessage(), exception);

                e.printStackTrace();
                
                setError(e.getVpnError());
            }
        };

        this.serviceIncomingMessageHandler = new ServiceIncomingMessageHandler(this)
        {
            @Override
            protected void processMessage(int messageType, Message m, BasicMessage basicMessage)
            {
                switch (messageType)
                {
                    case VpnMessageTypes.GetState:

                        ReplyHandler.reply(messageType, m, new ConnectionStateMessage(getState()));

                        break;

                    case VpnMessageTypes.ServiceStateChange:

                        ConnectionStateMessage csm = (ConnectionStateMessage)basicMessage;

                        if(csm != null)
                        {
                            VpnConnectionState state = csm.getPayload();

                            if(getState() == state)
                            {
                                Log.e(TAG, "Ignoring duplicate state " + state);
                                return;
                            }

                            setStateInternal(state);

                            switch (state)
                            {
                                case Connecting:
                                    connect((ConnectionRequestMessage) csm);
                                    break;

                                case Disconnected:
                                    disconnect();
                                    break;
                            }
                        }

                        break;
                }
            }
        };

        this.serviceMessenger = this.serviceIncomingMessageHandler.createMessenger();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return serviceMessenger.getBinder();
    }

    @Override
    public void onRevoke()
    {
        super.onRevoke();

        disconnect();
    }

    @Override
    public void onDestroy()
    {
        this.serviceIncomingMessageHandler = null;
        this.serviceMessenger = null;
    }

    public void connect(ConnectionRequestMessage message)
    {
        ConnectionRequestMessage.Request request = message.getRequest();
        
        torVpnProvider.connect(new TorVpnProvider.VpnConfiguration(new VpnService.Builder())
                .setSessionName(request.getSessionName())
                .setGatewayIp("172.168.21.1")
                .setClientIp("172.168.21.2")
                .setClientIpMask("255.255.255.0")
                .setCountryIso(request.getCountryIso())
        );
    }

    public void disconnect()
    {
        torVpnProvider.disconnect();

        setStateInternal(VpnConnectionState.Disconnected);
    }

    public void setStateInternal(VpnConnectionState state)
    {
        VpnConnectionState oldState = this.state;
        this.state = state;

        if (state == VpnConnectionState.Connecting)
            setError(VpnError.None);

        if(serviceIncomingMessageHandler == null)
            return;

        Messenger clientMessenger = serviceIncomingMessageHandler.getClientMessenger();

        if (clientMessenger != null)
        {
            ConnectionStateMessage cm = new ConnectionStateMessage(state);

            if(oldState != VpnConnectionState.Disconnected)
                cm.setError(error);

            ReplyHandler.reply(VpnMessageTypes.ServiceStateChange, clientMessenger, cm);
        }
    }

    public void setError(VpnError error)
    {
        this.error = error;

        if (error != VpnError.None)
            setStateInternal(VpnConnectionState.Disconnected);
    }
}
