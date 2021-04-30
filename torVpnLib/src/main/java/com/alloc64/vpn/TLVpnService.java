package com.alloc64.vpn;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.alloc64.vpn.messenger.ConnectionStateMessage;
import com.alloc64.vpn.messenger.IBasicMessage;
import com.alloc64.vpn.messenger.ProtoconfidConnectionStateMessage;
import com.alloc64.vpn.messenger.ReplyHandler;
import com.alloc64.vpn.messenger.ServiceIncomingMessageHandler;
import com.alloc64.vpn.tor.TorVpnProvider;

public class TLVpnService extends VpnService implements IVPNService
{
    private static final String TAG = TLVpnService.class.getName();

    private TorVpnProvider torVpnProvider = new TorVpnProvider();

    private ParcelFileDescriptor tunInterface;

    private VPNConnectionState state = VPNConnectionState.Disconnected;

    private Messenger serviceMessenger;

    private ServiceIncomingMessageHandler serviceIncomingMessageHandler;

    private VPNError error;

    public VPNConnectionState getState()
    {
        return state;
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate()
    {
        super.onCreate();

        this.serviceIncomingMessageHandler = new ServiceIncomingMessageHandler(this)
        {
            @Override
            protected void processMessage(int messageType, Message m, IBasicMessage basicMessage)
            {
                switch (messageType)
                {
                    case VPNMessageTypes.GetState:

                        ReplyHandler.reply(messageType, m, new ConnectionStateMessage(getState()));

                        break;

                    case VPNMessageTypes.ServiceStateChange:

                        ConnectionStateMessage csm = (ConnectionStateMessage)basicMessage;

                        if(csm != null)
                        {
                            VPNConnectionState state = csm.getPayload();

                            if(getState() == state)
                            {
                                Log.e(TAG, "Ignoring duplicate state " + state);
                                return;
                            }

                            setStateInternal(state);

                            switch (state)
                            {
                                case Connecting:
                                    connect((ProtoconfidConnectionStateMessage) csm);
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
    public void onDestroy()
    {
        serviceIncomingMessageHandler = null;
        serviceMessenger = null;
    }

    public void connect(ProtoconfidConnectionStateMessage csm)
    {
        torVpnProvider.start(this, "VPN", new VpnService.Builder());

        //TODO: process connection
        /*
        VPNSocketAddress connectionAddress = csm.getConnectionAddress();
        int protocolType = csm.getProtocolType();
        JSONObject handshakePayload = csm.getHandshakePayload();

        if (connectionAddress == null)
        {
            setError(VPNError.InvalidAddress);
            return;
        }

        if(protocolType != VPNProtocolType.DTLS && protocolType != VPNProtocolType.SSL)
        {
            setError(VPNError.InvalidProtocolType);
            return;
        }

        if(handshakePayload == null)
        {
            setError(VPNError.InvalidHandshakePayload);
            return;
        }

        if (protoconfidThread != null)
        {
            protoconfidThread.disconnect();
            protoconfidThread = null;
        }

        protoconfidThread = new ProtoconfidVPNTunnelThread(this, connectionAddress, protocolType, handshakePayload, TAG)
        {
            @Override
            protected String getAuthToken()
            {
                return TLVpnService.this.getAuthToken();
            }

            @Override
            protected String getRequestSecretKey()
            {
                return TLVpnService.this.getRequestSecretKey();
            }

            @Override
            protected void onConnectFailed(String message)
            {
                super.onConnectFailed(message);

                TLVpnService.this.onConnectFailed(message);
            }
        };

        protoconfidThread.connect();

         */
    }

    protected void onConnectFailed(String message)
    {

    }

    public void disconnect()
    {
        /*
        if (protoconfidThread != null)
        {
            protoconfidThread.disconnect();
            protoconfidThread = null;
        }

         */

        setStateInternal(VPNConnectionState.Disconnected);
    }

    public void setStateInternal(VPNConnectionState state)
    {
        VPNConnectionState oldState = this.state;
        this.state = state;

        if (state == VPNConnectionState.Connecting)
            setError(VPNError.None);

        if(serviceIncomingMessageHandler == null)
            return;

        Messenger clientMessenger = serviceIncomingMessageHandler.getClientMessenger();

        if (clientMessenger != null)
        {
            ConnectionStateMessage cm = new ConnectionStateMessage(state);

            if(oldState != VPNConnectionState.Disconnected)
                cm.setError(error);

            ReplyHandler.reply(VPNMessageTypes.ServiceStateChange, clientMessenger, cm);
        }
    }

    public void setError(VPNError error)
    {
        this.error = error;

        if (error != VPNError.None)
            setStateInternal(VPNConnectionState.Disconnected);
    }
}
