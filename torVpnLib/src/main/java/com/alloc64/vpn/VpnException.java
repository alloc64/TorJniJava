/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

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
