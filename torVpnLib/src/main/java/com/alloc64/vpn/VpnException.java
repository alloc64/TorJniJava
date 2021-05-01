package com.alloc64.vpn;

public class VpnException extends Exception
{
    private final VpnError vpnError;

    public VpnException(VpnError vpnError, String message)
    {
        super(message);
        this.vpnError = vpnError;
    }

    public VpnException(VpnError vpnError, String message, Throwable cause)
    {
        super(message, cause);
        this.vpnError = vpnError;
    }

    public VpnError getVpnError()
    {
        return vpnError;
    }
}
