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
import android.util.Log;

import com.alloc64.vpn.VPNConnectionState;
import com.alloc64.vpn.VPNError;
import com.alloc64.vpn.VPNMessageTypes;
import com.alloc64.vpn.messenger.ConnectionStateMessage;
import com.alloc64.vpn.messenger.IBasicMessage;

import androidx.annotation.Nullable;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class AbstractVPNClient
{
	public interface StateCallback
	{
		void onMessageReceived(int messageType, IBasicMessage message);
		void onError(VPNError error);
	}

	private static final String SOCKET_ADDRESS = "socket_address";
	private static final String PROTOCOL_TYPE = "proto_type";

	private static final int CONNECT_REQUEST_CODE = 0x115;

	private Messenger vpnServiceMessenger = null;

	private Messenger responseMessenger = null;

	private StateCallback stateCallback;

	private boolean serviceBound;

	private VPNConnectionState state = VPNConnectionState.Disconnected;

	private int tmpProtocolType = 0;

	private ServiceConnection serviceConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			vpnServiceMessenger = new Messenger(service);
			serviceBound = true;

			sendMessage(VPNMessageTypes.GetState);
		}

		public void onServiceDisconnected(ComponentName className)
		{
			vpnServiceMessenger = null;
			serviceBound = false;
		}
	};

	public void create(Context ctx)
	{
		if(ctx == null)
			return;

		responseMessenger = new Messenger(new Handler(msg ->
		{
			try
			{
				onMessageReceived(msg, (IBasicMessage) msg.obj);
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
		if(ctx == null)
			return;

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
				ctx.unbindService(serviceConnection);
			}
		}
		catch (Exception e)
		{
			serviceBound = false;
			onLogException(e);
		}
	}

	public void connect(Activity activity, int protocolType)
	{
		if(activity == null)
		{
			onError(VPNError.NoContext);
			return;
		}

		Intent intent = VpnService.prepare(activity.getApplicationContext());

		int rc = CONNECT_REQUEST_CODE;

		if (intent != null)
		{
			this.tmpProtocolType = protocolType;

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
			//intent.putExtra(SOCKET_ADDRESS, socketAddress);
			intent.putExtra(PROTOCOL_TYPE, protocolType);

			onActivityResult(activity, rc, RESULT_OK, intent);
		}
	}

	protected void onVPNPrepared(Activity activity, int protocolType)
	{
	}

	public void disconnect()
	{
		sendMessage(VPNMessageTypes.ServiceStateChange, new ConnectionStateMessage(VPNConnectionState.Disconnected));
	}

	public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data)
	{
		try
		{
			if (requestCode == CONNECT_REQUEST_CODE)
			{
				int protocolType = tmpProtocolType;

				if(protocolType <= 0)
				{
					if(data != null)
					{
						//socketAddress = (VPNSocketAddress) data.getSerializableExtra(SOCKET_ADDRESS);
						//protocolType = data.getIntExtra(PROTOCOL_TYPE, VPNProtocolType.DTLS);
					}
				}
				else
				{
					//tmpSocketAddress = null;
					tmpProtocolType = 0;
				}

				if (resultCode == RESULT_OK)
				{
					onVPNPrepared(activity, protocolType);
				}
				else
				{
					onError(VPNError.VPNInterfaceCreationDenied);
				}

				return true;
			}
		}
		catch (Exception e)
		{
			onLogException(e);
			onError(VPNError.FatalException);
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

	protected void sendMessage(int messageType, IBasicMessage payload)
	{
		if (!serviceBound)
		{
			onError(VPNError.ServiceNotBound);
			return;
		}

		sendMessageNoCheck(messageType, payload);
	}

	protected void sendMessageNoCheck(int messageType)
	{
		sendMessageNoCheck(messageType, null);
	}

	protected void sendMessageNoCheck(int messageType, IBasicMessage payload)
	{
		Message msg = Message.obtain(null, messageType, 0, 0, payload);

		try
		{
			msg.replyTo = responseMessenger;

			if(vpnServiceMessenger != null)
				vpnServiceMessenger.send(msg);
		}
		catch (Exception e)
		{
			onLogException(e);
		}
	}

	public void refreshState()
	{
		sendMessageNoCheck(VPNMessageTypes.GetState);
	}

	public VPNConnectionState getConnectionState()
	{
		return state;
	}

	// region Callbacks

	private void onMessageReceived(Message m, @Nullable IBasicMessage message)
	{
		onMessageReceived(m.what, message);
	}

	protected void onMessageReceived(int messageType, @Nullable IBasicMessage message)
	{
		switch (messageType)
		{
			case VPNMessageTypes.GetState:
			case VPNMessageTypes.ServiceStateChange:

				if(message instanceof ConnectionStateMessage)
				{
					ConnectionStateMessage csm = (ConnectionStateMessage)message;
					this.state = csm.getPayload();

					VPNError error = csm.getError();

					if(error != null && error != VPNError.None)
						onError(csm.getError());
				}

				break;
		}

		if(stateCallback != null)
			stateCallback.onMessageReceived(messageType, message);
	}

	protected void onError(VPNError error)
	{
		if (error == null)
			return;

		if(stateCallback != null)
			stateCallback.onError(error);
	}

	protected void onLogException(Exception e)
	{
		if(e != null)
			e.printStackTrace();
	}

	// endregion
}
