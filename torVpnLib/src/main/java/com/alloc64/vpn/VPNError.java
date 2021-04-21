package com.alloc64.vpn;

public enum VPNError
{
	None,
	NoContext,
	InvalidAddress,
	ConnectionFailed,
	HandshakeFailed,
	VPNInterfaceCreationDenied,
	SocketException,
	TunCreateFailed,
	ServiceNotBound,
	GetServerFailed,
	NoServerSet,
	InvalidProtocolType,
	InvalidHandshakePayload,
	SignatureInvalid,
	ValidationFailed,
	MalformedControlPacket,
	MissingAuthKeyPair,
	FatalException,
	Disabled,
	FailedToSaveVPNPreferences,
	FailedToLoadVPNPreferences,
	FailedToLoadVPNPreferencesList,
	UserAuthenticationFailed,
	InvalidToken,
	InvalidChecksum;
}
