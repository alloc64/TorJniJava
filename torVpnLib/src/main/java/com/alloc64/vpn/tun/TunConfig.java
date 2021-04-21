package com.alloc64.vpn.tun;

import org.json.JSONObject;

import java.net.InetAddress;

public class TunConfig
{
	private String serverAddress;
	private int mtu;

	private String tunnelIp;
	private int tunnelMask = 32;

	private String routeIp;
	private String dnsIp;

	public TunConfig()
	{

	}

	public String getServerAddress()
	{
		return serverAddress;
	}

	public int getMtu()
	{
		return mtu;
	}

	public String getTunnelIp()
	{
		return tunnelIp;
	}

	public int getTunnelMask()
	{
		return tunnelMask;
	}

	public String getRouteIp()
	{
		return routeIp;
	}

	public String getDnsIp()
	{
		return dnsIp;
	}

	private boolean deserializeInternal(JSONObject json)
	{
		if(json == null)
			return false;

		try
		{
			serverAddress = json.optString("serverAddress");
			mtu = json.optInt("mtu");
			tunnelIp = json.optString("tunnelIp");
			String tunnelMaskIp = json.optString("tunnelMaskIp");
			routeIp = json.optString("routeIp");
			dnsIp = json.optString("dnsIp");

			if(tunnelMaskIp != null)
			{
				try
				{
					InetAddress inet = InetAddress.getByName(tunnelMaskIp);

					byte[] netmaskBytes = inet.getAddress();
					int cidr = 0;

					boolean zero = false;
					for(byte b : netmaskBytes)
					{
						int mask = 0x80;

						for(int i = 0; i < 8; i++)
						{
							int result = b & mask;
							if(result == 0)
								zero = true;
							else if(zero)
								throw new IllegalArgumentException("Invalid netmask.");
							else
								cidr++;

							mask >>>= 1;
						}
					}

					tunnelMask = cidr;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public static TunConfig deserialize(JSONObject json)
	{
		if(json == null)
			return null;

		TunConfig tunConfig = new TunConfig();

		return tunConfig.deserializeInternal(json) ? tunConfig : null;
	}
}
