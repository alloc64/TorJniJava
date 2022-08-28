/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package com.alloc64.vpn;

public enum VpnError
{
	None,
	NoContext,
	ConnectionTimeout,
	VPNInterfaceCreationDenied,
	ServiceNotBound,
	FatalException,
	InvalidRequest
}
