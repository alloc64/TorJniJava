/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package com.alloc64.torlib.control;

import java.util.ArrayList;
import java.util.List;

/**
 * See https://gitweb.torproject.org/torspec.git/tree/control-spec.txt
 * <p>
 * 4.1. Asynchronous events
 * <p>
 * These replies can be sent after a corresponding SETEVENTS command has been
 * received.  They will not be interleaved with other Reply elements, but they
 * can appear between a command and its corresponding reply.  For example,
 * this sequence is possible:
 * <p>
 * C: SETEVENTS CIRC
 * S: 250 OK
 * C: GETCONF SOCKSPORT ORPORT
 * S: 650 CIRC 1000 EXTENDED moria1,moria2
 * S: 250-SOCKSPORT=9050
 * S: 250 ORPORT=0
 * <p>
 * But this sequence is disallowed:
 * <p>
 * C: SETEVENTS CIRC
 * S: 250 OK
 * C: GETCONF SOCKSPORT ORPORT
 * S: 250-SOCKSPORT=9050
 * S: 650 CIRC 1000 EXTENDED moria1,moria2
 * S: 250 ORPORT=0
 * <p>
 * Clients MUST tolerate more arguments in an asynchronous reply than
 * expected, and MUST tolerate more lines in an asynchronous reply than
 * expected.  For instance, a client that expects a CIRC message like:
 * <p>
 * 650 CIRC 1000 EXTENDED moria1,moria2
 * <p>
 * must tolerate:
 * <p>
 * 650-CIRC 1000 EXTENDED moria1,moria2 0xBEEF
 * 650-EXTRAMAGIC=99
 * 650 ANONYMITY=high
 * <p>
 * If clients receives extended events (selected by USEFEATUERE
 * EXTENDED_EVENTS in Tor 0.1.2.2-alpha..Tor-0.2.1.x, and always-on in
 * Tor 0.2.2.x and later), then each event line as specified below may be
 * followed by additional arguments and additional lines.  Additional
 * lines will be of the form:
 * <p>
 * "650" ("-"/" ") KEYWORD ["=" ARGUMENTS] CRLF
 * <p>
 * Additional arguments will be of the form
 * <p>
 * SP KEYWORD ["=" ( QuotedString / * NonSpDquote ) ]
 * <p>
 * Clients MUST tolerate events with arguments and keywords they do not
 * recognize, and SHOULD process those events as if any unrecognized
 * arguments and keywords were not present.
 * <p>
 * Clients SHOULD NOT depend on the order of keyword=value arguments,
 * and SHOULD NOT depend on there being no new keyword=value arguments
 * appearing between existing keyword=value arguments, though as of this
 * writing (Jun 2011) some do.  Thus, extensions to this protocol should
 * add new keywords only after the existing keywords, until all
 * controllers have been fixed.  At some point this "SHOULD NOT" might
 * become a "MUST NOT".
 * <p>
 * 4.1.1. Circuit status changed
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "CIRC" SP CircuitID SP CircStatus [SP Path]
 * [SP "BUILD_FLAGS=" BuildFlags] [SP "PURPOSE=" Purpose]
 * [SP "HS_STATE=" HSState] [SP "REND_QUERY=" HSAddress]
 * [SP "TIME_CREATED=" TimeCreated]
 * [SP "REASON=" Reason [SP "REMOTE_REASON=" Reason]]
 * [SP "SOCKS_USERNAME=" EscapedUsername]
 * [SP "SOCKS_PASSWORD=" EscapedPassword]
 * CRLF
 * <p>
 * CircStatus =
 * "LAUNCHED" / ; circuit ID assigned to new circuit
 * "BUILT"    / ; all hops finished, can now accept streams
 * "GUARD_WAIT" / ; all hops finished, waiting to see if a
 * ;  circuit with a better guard will be usable.
 * "EXTENDED" / ; one more hop has been completed
 * "FAILED"   / ; circuit closed (was not built)
 * "CLOSED"     ; circuit closed (was built)
 * <p>
 * Path = LongName *("," LongName)
 * ; In Tor versions 0.1.2.2-alpha through 0.2.2.1-alpha with feature
 * ; VERBOSE_NAMES turned off and before version 0.1.2.2-alpha, Path
 * ; is as follows:
 * ; Path = ServerID *("," ServerID)
 * <p>
 * BuildFlags = BuildFlag *("," BuildFlag)
 * BuildFlag = "ONEHOP_TUNNEL" / "IS_INTERNAL" /
 * "NEED_CAPACITY" / "NEED_UPTIME"
 * <p>
 * Purpose = "GENERAL" / "HS_CLIENT_INTRO" / "HS_CLIENT_REND" /
 * "HS_SERVICE_INTRO" / "HS_SERVICE_REND" / "TESTING" /
 * "CONTROLLER" / "MEASURE_TIMEOUT" /
 * "HS_VANGUARDS" / "PATH_BIAS_TESTING" /
 * "CIRCUIT_PADDING"
 * <p>
 * HSState = "HSCI_CONNECTING" / "HSCI_INTRO_SENT" / "HSCI_DONE" /
 * "HSCR_CONNECTING" / "HSCR_ESTABLISHED_IDLE" /
 * "HSCR_ESTABLISHED_WAITING" / "HSCR_JOINED" /
 * "HSSI_CONNECTING" / "HSSI_ESTABLISHED" /
 * "HSSR_CONNECTING" / "HSSR_JOINED"
 * <p>
 * EscapedUsername = QuotedString
 * EscapedPassword = QuotedString
 * <p>
 * HSAddress = 16*Base32Character / 56*Base32Character
 * Base32Character = ALPHA / "2" / "3" / "4" / "5" / "6" / "7"
 * <p>
 * TimeCreated = ISOTime2Frac
 * Seconds = 1*DIGIT
 * Microseconds = 1*DIGIT
 * <p>
 * Reason = "NONE" / "TORPROTOCOL" / "INTERNAL" / "REQUESTED" /
 * "HIBERNATING" / "RESOURCELIMIT" / "CONNECTFAILED" /
 * "OR_IDENTITY" / "OR_CONN_CLOSED" / "TIMEOUT" /
 * "FINISHED" / "DESTROYED" / "NOPATH" / "NOSUCHSERVICE" /
 * "MEASUREMENT_EXPIRED"
 * <p>
 * The path is provided only when the circuit has been extended at least one
 * hop.
 * <p>
 * The "BUILD_FLAGS" field is provided only in versions 0.2.3.11-alpha
 * and later.  Clients MUST accept build flags not listed above.
 * Build flags are defined as follows:
 * <p>
 * ONEHOP_TUNNEL   (one-hop circuit, used for tunneled directory conns)
 * IS_INTERNAL     (internal circuit, not to be used for exiting streams)
 * NEED_CAPACITY   (this circuit must use only high-capacity nodes)
 * NEED_UPTIME     (this circuit must use only high-uptime nodes)
 * <p>
 * The "PURPOSE" field is provided only in versions 0.2.1.6-alpha and
 * later, and only if extended events are enabled (see 3.19).  Clients
 * MUST accept purposes not listed above.  Purposes are defined as
 * follows:
 * <p>
 * GENERAL         (circuit for AP and/or directory request streams)
 * HS_CLIENT_INTRO (HS client-side introduction-point circuit)
 * HS_CLIENT_REND  (HS client-side rendezvous circuit; carries AP streams)
 * HS_SERVICE_INTRO (HS service-side introduction-point circuit)
 * HS_SERVICE_REND (HS service-side rendezvous circuit)
 * TESTING         (reachability-testing circuit; carries no traffic)
 * CONTROLLER      (circuit built by a controller)
 * MEASURE_TIMEOUT (circuit being kept around to see how long it takes)
 * HS_VANGUARDS    (circuit created ahead of time when using
 * HS vanguards, and later repurposed as needed)
 * PATH_BIAS_TESTING (circuit used to probe whether our circuits are
 * being deliberately closed by an attacker)
 * CIRCUIT_PADDING (circuit that is being held open to disguise its
 * true close time)
 * <p>
 * The "HS_STATE" field is provided only for hidden-service circuits,
 * and only in versions 0.2.3.11-alpha and later.  Clients MUST accept
 * hidden-service circuit states not listed above.  Hidden-service
 * circuit states are defined as follows:
 * <p>
 * HSCI_*      (client-side introduction-point circuit states)
 * HSCI_CONNECTING          (connecting to intro point)
 * HSCI_INTRO_SENT          (sent INTRODUCE1; waiting for reply from IP)
 * HSCI_DONE                (received reply from IP relay; closing)
 * <p>
 * HSCR_*      (client-side rendezvous-point circuit states)
 * HSCR_CONNECTING          (connecting to or waiting for reply from RP)
 * HSCR_ESTABLISHED_IDLE    (established RP; waiting for introduction)
 * HSCR_ESTABLISHED_WAITING (introduction sent to HS; waiting for rend)
 * HSCR_JOINED              (connected to HS)
 * <p>
 * HSSI_*      (service-side introduction-point circuit states)
 * HSSI_CONNECTING          (connecting to intro point)
 * HSSI_ESTABLISHED         (established intro point)
 * <p>
 * HSSR_*      (service-side rendezvous-point circuit states)
 * HSSR_CONNECTING          (connecting to client's rend point)
 * HSSR_JOINED              (connected to client's RP circuit)
 * <p>
 * The "SOCKS_USERNAME" and "SOCKS_PASSWORD" fields indicate the credentials
 * that were used by a SOCKS client to connect to Tor's SOCKS port and
 * initiate this circuit. (Streams for SOCKS clients connected with different
 * usernames and/or passwords are isolated on separate circuits if the
 * IsolateSOCKSAuth flag is active; see Proposal 171.) [Added in Tor
 * 0.4.3.1-alpha.]
 * <p>
 * The "REND_QUERY" field is provided only for hidden-service-related
 * circuits, and only in versions 0.2.3.11-alpha and later.  Clients
 * MUST accept hidden service addresses in formats other than that
 * specified above. [Added in Tor 0.4.3.1-alpha.]
 * <p>
 * The "TIME_CREATED" field is provided only in versions 0.2.3.11-alpha and
 * later.  TIME_CREATED is the time at which the circuit was created or
 * cannibalized. [Added in Tor 0.4.3.1-alpha.]
 * <p>
 * The "REASON" field is provided only for FAILED and CLOSED events, and only
 * if extended events are enabled (see 3.19).  Clients MUST accept reasons
 * not listed above. [Added in Tor 0.4.3.1-alpha.]  Reasons are as given in
 * tor-spec.txt, except for:
 * <p>
 * NOPATH              (Not enough nodes to make circuit)
 * MEASUREMENT_EXPIRED (As "TIMEOUT", except that we had left the circuit
 * open for measurement purposes to see how long it
 * would take to finish.)
 * IP_NOW_REDUNDANT    (Closing a circuit to an introduction point that
 * has become redundant, since some other circuit
 * opened in parallel with it has succeeded.)
 * <p>
 * The "REMOTE_REASON" field is provided only when we receive a DESTROY or
 * TRUNCATE cell, and only if extended events are enabled.  It contains the
 * actual reason given by the remote OR for closing the circuit. Clients MUST
 * accept reasons not listed above.  Reasons are as listed in tor-spec.txt.
 * [Added in Tor 0.4.3.1-alpha.]
 * <p>
 * 4.1.2. Stream status changed
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "STREAM" SP StreamID SP StreamStatus SP CircuitID SP Target
 * [SP "REASON=" Reason [ SP "REMOTE_REASON=" Reason ]]
 * [SP "SOURCE=" Source] [ SP "SOURCE_ADDR=" Address ":" Port ]
 * [SP "PURPOSE=" Purpose] [SP "SOCKS_USERNAME=" EscapedUsername]
 * [SP "SOCKS_PASSWORD=" EscapedPassword]
 * [SP "CLIENT_PROTOCOL=" ClientProtocol] [SP "NYM_EPOCH=" NymEpoch]
 * [SP "SESSION_GROUP=" SessionGroup] [SP "ISO_FIELDS=" IsoFields]
 * CRLF
 * <p>
 * StreamStatus =
 * "NEW"          / ; New request to connect
 * "NEWRESOLVE"   / ; New request to resolve an address
 * "REMAP"        / ; Address re-mapped to another
 * "SENTCONNECT"  / ; Sent a connect cell along a circuit
 * "SENTRESOLVE"  / ; Sent a resolve cell along a circuit
 * "SUCCEEDED"    / ; Received a reply; stream established
 * "FAILED"       / ; Stream failed and not retriable
 * "CLOSED"       / ; Stream closed
 * "DETACHED"     / ; Detached from circuit; still retriable
 * "CONTROLLER_WAIT"  ; Waiting for controller to use ATTACHSTREAM
 * ; (new in 0.4.5.1-alpha)
 * <p>
 * Target = TargetAddress ":" Port
 * Port = an integer from 0 to 65535 inclusive
 * TargetAddress = Address / "(Tor_internal)"
 * <p>
 * EscapedUsername = QuotedString
 * EscapedPassword = QuotedString
 * <p>
 * ClientProtocol =
 * "SOCKS4"      /
 * "SOCKS5"      /
 * "TRANS"       /
 * "NATD"        /
 * "DNS"         /
 * "HTTPCONNECT" /
 * "UNKNOWN"
 * <p>
 * NymEpoch = a nonnegative integer
 * SessionGroup = an integer
 * <p>
 * IsoFields = a comma-separated list of IsoField values
 * <p>
 * IsoField =
 * "CLIENTADDR" /
 * "CLIENTPORT" /
 * "DESTADDR" /
 * "DESTPORT" /
 * the name of a field that is valid for STREAM events
 * <p>
 * The circuit ID designates which circuit this stream is attached to.  If
 * the stream is unattached, the circuit ID "0" is given.  The target
 * indicates the address which the stream is meant to resolve or connect to;
 * it can be "(Tor_internal)" for a virtual stream created by the Tor program
 * to talk to itself.
 * <p>
 * Reason = "MISC" / "RESOLVEFAILED" / "CONNECTREFUSED" /
 * "EXITPOLICY" / "DESTROY" / "DONE" / "TIMEOUT" /
 * "NOROUTE" / "HIBERNATING" / "INTERNAL"/ "RESOURCELIMIT" /
 * "CONNRESET" / "TORPROTOCOL" / "NOTDIRECTORY" / "END" /
 * "PRIVATE_ADDR"
 * <p>
 * The "REASON" field is provided only for FAILED, CLOSED, and DETACHED
 * events, and only if extended events are enabled (see 3.19).  Clients MUST
 * accept reasons not listed above.  Reasons are as given in tor-spec.txt,
 * except for:
 * <p>
 * END          (We received a RELAY_END cell from the other side of this
 * stream.)
 * PRIVATE_ADDR (The client tried to connect to a private address like
 * 127.0.0.1 or 10.0.0.1 over Tor.)
 * [XXXX document more. -NM]
 * <p>
 * The "REMOTE_REASON" field is provided only when we receive a RELAY_END
 * cell, and only if extended events are enabled.  It contains the actual
 * reason given by the remote OR for closing the stream. Clients MUST accept
 * reasons not listed above.  Reasons are as listed in tor-spec.txt.
 * <p>
 * "REMAP" events include a Source if extended events are enabled:
 * <p>
 * Source = "CACHE" / "EXIT"
 * <p>
 * Clients MUST accept sources not listed above.  "CACHE" is given if
 * the Tor client decided to remap the address because of a cached
 * answer, and "EXIT" is given if the remote node we queried gave us
 * the new address as a response.
 * <p>
 * The "SOURCE_ADDR" field is included with NEW and NEWRESOLVE events if
 * extended events are enabled.  It indicates the address and port
 * that requested the connection, and can be (e.g.) used to look up the
 * requesting program.
 * <p>
 * Purpose = "DIR_FETCH" / "DIR_UPLOAD" / "DNS_REQUEST" /
 * "USER" /  "DIRPORT_TEST"
 * <p>
 * The "PURPOSE" field is provided only for NEW and NEWRESOLVE events, and
 * only if extended events are enabled (see 3.19).  Clients MUST accept
 * purposes not listed above.  The purposes above are defined as:
 * <p>
 * "DIR_FETCH" -- This stream is generated internally to Tor for
 * fetching directory information.
 * "DIR_UPLOAD" -- An internal stream for uploading information to
 * a directory authority.
 * "DIRPORT_TEST" -- A stream we're using to test our own directory
 * port to make sure it's reachable.
 * "DNS_REQUEST" -- A user-initiated DNS request.
 * "USER" -- This stream is handling user traffic, OR it's internal
 * to Tor, but it doesn't match one of the purposes above.
 * <p>
 * The "SOCKS_USERNAME" and "SOCKS_PASSWORD" fields indicate the credentials
 * that were used by a SOCKS client to connect to Tor's SOCKS port and
 * initiate this stream. (Streams for SOCKS clients connected with different
 * usernames and/or passwords are isolated on separate circuits if the
 * IsolateSOCKSAuth flag is active; see Proposal 171.)
 * <p>
 * The "CLIENT_PROTOCOL" field indicates the protocol that was used by a client
 * to initiate this stream. (Streams for clients connected with different
 * protocols are isolated on separate circuits if the IsolateClientProtocol
 * flag is active.)  Controllers MUST tolerate unrecognized client protocols.
 * <p>
 * The "NYM_EPOCH" field indicates the nym epoch that was active when a client
 * initiated this stream. The epoch increments when the NEWNYM signal is
 * received. (Streams with different nym epochs are isolated on separate
 * circuits.)
 * <p>
 * The "SESSION_GROUP" field indicates the session group of the listener port
 * that a client used to initiate this stream. By default, the session group is
 * different for each listener port, but this can be overridden for a listener
 * via the "SessionGroup" option in torrc. (Streams with different session
 * groups are isolated on separate circuits.)
 * <p>
 * The "ISO_FIELDS" field indicates the set of STREAM event fields for which
 * stream isolation is enabled for the listener port that a client used to
 * initiate this stream.  The special values "CLIENTADDR", "CLIENTPORT",
 * "DESTADDR", and "DESTPORT", if their correspondingly named fields are not
 * present, refer to the Address and Port components of the "SOURCE_ADDR" and
 * Target fields.
 * <p>
 * 4.1.3. OR Connection status changed
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "ORCONN" SP (LongName / Target) SP ORStatus [ SP "REASON="
 * Reason ] [ SP "NCIRCS=" NumCircuits ] [ SP "ID=" ConnID ] CRLF
 * <p>
 * ORStatus = "NEW" / "LAUNCHED" / "CONNECTED" / "FAILED" / "CLOSED"
 * <p>
 * ; In Tor versions 0.1.2.2-alpha through 0.2.2.1-alpha with feature
 * ; VERBOSE_NAMES turned off and before version 0.1.2.2-alpha, OR
 * ; Connection is as follows:
 * "650" SP "ORCONN" SP (ServerID / Target) SP ORStatus [ SP "REASON="
 * Reason ] [ SP "NCIRCS=" NumCircuits ] CRLF
 * <p>
 * NEW is for incoming connections, and LAUNCHED is for outgoing
 * connections. CONNECTED means the TLS handshake has finished (in
 * either direction). FAILED means a connection is being closed that
 * hasn't finished its handshake, and CLOSED is for connections that
 * have handshaked.
 * <p>
 * A LongName or ServerID is specified unless it's a NEW connection, in
 * which case we don't know what server it is yet, so we use Address:Port.
 * <p>
 * If extended events are enabled (see 3.19), optional reason and
 * circuit counting information is provided for CLOSED and FAILED
 * events.
 * <p>
 * Reason = "MISC" / "DONE" / "CONNECTREFUSED" /
 * "IDENTITY" / "CONNECTRESET" / "TIMEOUT" / "NOROUTE" /
 * "IOERROR" / "RESOURCELIMIT" / "PT_MISSING"
 * <p>
 * NumCircuits counts both established and pending circuits.
 * <p>
 * The ORStatus values are as follows:
 * <p>
 * NEW -- We have received a new incoming OR connection, and are starting
 * the server-side handshake.
 * LAUNCHED -- We have launched a new outgoing OR connection, and are
 * starting the client-side handshake.
 * CONNECTED -- The OR connection has been connected and the handshake is
 * done.
 * FAILED -- Our attempt to open the OR connection failed.
 * CLOSED -- The OR connection closed in an unremarkable way.
 * <p>
 * The Reason values for closed/failed OR connections are:
 * <p>
 * DONE -- The OR connection has shut down cleanly.
 * CONNECTREFUSED -- We got an ECONNREFUSED while connecting to the target
 * OR.
 * IDENTITY -- We connected to the OR, but found that its identity was
 * not what we expected.
 * CONNECTRESET -- We got an ECONNRESET or similar IO error from the
 * connection with the OR.
 * TIMEOUT -- We got an ETIMEOUT or similar IO error from the connection
 * with the OR, or we're closing the connection for being idle for too
 * long.
 * NOROUTE -- We got an ENOTCONN, ENETUNREACH, ENETDOWN, EHOSTUNREACH, or
 * similar error while connecting to the OR.
 * IOERROR -- We got some other IO error on our connection to the OR.
 * RESOURCELIMIT -- We don't have enough operating system resources (file
 * descriptors, buffers, etc) to connect to the OR.
 * PT_MISSING -- No pluggable transport was available.
 * MISC -- The OR connection closed for some other reason.
 * <p>
 * [First added ID parameter in 0.2.5.2-alpha]
 * <p>
 * 4.1.4. Bandwidth used in the last second
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "BW" SP BytesRead SP BytesWritten *(SP Type "=" Num) CRLF
 * BytesRead = 1*DIGIT
 * BytesWritten = 1*DIGIT
 * Type = "DIR" / "OR" / "EXIT" / "APP" / ...
 * Num = 1*DIGIT
 * <p>
 * BytesRead and BytesWritten are the totals. [In a future Tor version,
 * we may also include a breakdown of the connection types that used
 * bandwidth this second (not implemented yet).]
 * <p>
 * 4.1.5. Log messages
 * <p>
 * The syntax is:
 * <p>
 * "650" SP Severity SP ReplyText CRLF
 * <p>
 * or
 * <p>
 * "650+" Severity CRLF Data 650 SP "OK" CRLF
 * <p>
 * Severity = "DEBUG" / "INFO" / "NOTICE" / "WARN"/ "ERR"
 * <p>
 * Some low-level logs may be sent from signal handlers, so their destination
 * logs must be signal-safe. These low-level logs include backtraces,
 * logging function errors, and errors in code called by logging functions.
 * Signal-safe logs are never sent as control port log events.
 * <p>
 * Control port message trace debug logs are never sent as control port log
 * events, to avoid modifying control output when debugging.
 * <p>
 * 4.1.6. New descriptors available
 * <p>
 * This event is generated when new router descriptors (not microdescs or
 * extrainfos or anything else) are received.
 * <p>
 * Syntax:
 * <p>
 * "650" SP "NEWDESC" 1*(SP LongName) CRLF
 * ; In Tor versions 0.1.2.2-alpha through 0.2.2.1-alpha with feature
 * ; VERBOSE_NAMES turned off and before version 0.1.2.2-alpha, it
 * ; is as follows:
 * "650" SP "NEWDESC" 1*(SP ServerID) CRLF
 * <p>
 * 4.1.7. New Address mapping
 * <p>
 * These events are generated when a new address mapping is entered in
 * Tor's address map cache, or when the answer for a RESOLVE command is
 * found.  Entries can be created by a successful or failed DNS lookup,
 * a successful or failed connection attempt, a RESOLVE command,
 * a MAPADDRESS command, the AutomapHostsOnResolve feature, or the
 * TrackHostExits feature.
 * <p>
 * Syntax:
 * <p>
 * "650" SP "ADDRMAP" SP Address SP NewAddress SP Expiry
 * [SP "error=" ErrorCode] [SP "EXPIRES=" UTCExpiry] [SP "CACHED=" Cached]
 * [SP "STREAMID=" StreamId] CRLF
 * <p>
 * NewAddress = Address / "<error>"
 * Expiry = DQUOTE ISOTime DQUOTE / "NEVER"
 * <p>
 * ErrorCode = "yes" / "internal" / "Unable to launch resolve request"
 * UTCExpiry = DQUOTE IsoTime DQUOTE
 * <p>
 * Cached = DQUOTE "YES" DQUOTE / DQUOTE "NO" DQUOTE
 * StreamId = DQUOTE StreamId DQUOTE
 * <p>
 * Error and UTCExpiry are only provided if extended events are enabled.
 * The values for Error are mostly useless.  Future values will be
 * chosen to match 1*(ALNUM / "_"); the "Unable to launch resolve request"
 * value is a bug in Tor before 0.2.4.7-alpha.
 * <p>
 * Expiry is expressed as the local time (rather than UTC).  This is a bug,
 * left in for backward compatibility; new code should look at UTCExpiry
 * instead.  (If Expiry is "NEVER", UTCExpiry is omitted.)
 * <p>
 * Cached indicates whether the mapping will be stored until it expires, or if
 * it is just a notification in response to a RESOLVE command.
 * <p>
 * StreamId is the global stream identifier of the stream or circuit from which
 * the address was resolved.
 * <p>
 * 4.1.8. Descriptors uploaded to us in our role as authoritative dirserver
 * <p>
 * [NOTE: This feature was removed in Tor 0.3.2.1-alpha.]
 * <p>
 * Tor generates this event when it's an directory authority, and
 * somebody has just uploaded a server descriptor.
 * <p>
 * Syntax:
 * <p>
 * "650" "+" "AUTHDIR_NEWDESCS" CRLF Action CRLF Message CRLF
 * Descriptor CRLF "." CRLF "650" SP "OK" CRLF
 * Action = "ACCEPTED" / "DROPPED" / "REJECTED"
 * Message = Text
 * <p>
 * The Descriptor field is the text of the server descriptor; the Action
 * field is "ACCEPTED" if we're accepting the descriptor as the new
 * best valid descriptor for its router, "REJECTED" if we aren't taking
 * the descriptor and we're complaining to the uploading relay about
 * it, and "DROPPED" if we decide to drop the descriptor without
 * complaining.  The Message field is a human-readable string
 * explaining why we chose the Action.  (It doesn't contain newlines.)
 * <p>
 * 4.1.9. Our descriptor changed
 * <p>
 * Syntax:
 * <p>
 * "650" SP "DESCCHANGED" CRLF
 * <p>
 * [First added in 0.1.2.2-alpha.]
 * <p>
 * 4.1.10. Status events
 * <p>
 * Status events (STATUS_GENERAL, STATUS_CLIENT, and STATUS_SERVER) are sent
 * based on occurrences in the Tor process pertaining to the general state of
 * the program.  Generally, they correspond to log messages of severity Notice
 * or higher.  They differ from log messages in that their format is a
 * specified interface.
 * <p>
 * Syntax:
 * <p>
 * "650" SP StatusType SP StatusSeverity SP StatusAction
 * [SP StatusArguments] CRLF
 * <p>
 * StatusType = "STATUS_GENERAL" / "STATUS_CLIENT" / "STATUS_SERVER"
 * StatusSeverity = "NOTICE" / "WARN" / "ERR"
 * StatusAction = 1*ALPHA
 * StatusArguments = StatusArgument *(SP StatusArgument)
 * StatusArgument = StatusKeyword '=' StatusValue
 * StatusKeyword = 1*(ALNUM / "_")
 * StatusValue = 1*(ALNUM / '_')  / QuotedString
 * <p>
 * StatusAction is a string, and StatusArguments is a series of
 * keyword=value pairs on the same line.  Values may be space-terminated
 * strings, or quoted strings.
 * <p>
 * These events are always produced with EXTENDED_EVENTS and
 * VERBOSE_NAMES; see the explanations in the USEFEATURE section
 * for details.
 * <p>
 * Controllers MUST tolerate unrecognized actions, MUST tolerate
 * unrecognized arguments, MUST tolerate missing arguments, and MUST
 * tolerate arguments that arrive in any order.
 * <p>
 * Each event description below is accompanied by a recommendation for
 * controllers.  These recommendations are suggestions only; no controller
 * is required to implement them.
 * <p>
 * Compatibility note: versions of Tor before 0.2.0.22-rc incorrectly
 * generated "STATUS_SERVER" as "STATUS_SEVER".  To be compatible with those
 * versions, tools should accept both.
 * <p>
 * Actions for STATUS_GENERAL events can be as follows:
 * <p>
 * CLOCK_JUMPED
 * "TIME=NUM"
 * Tor spent enough time without CPU cycles that it has closed all
 * its circuits and will establish them anew. This typically
 * happens when a laptop goes to sleep and then wakes up again. It
 * also happens when the system is swapping so heavily that Tor is
 * starving. The "time" argument specifies the number of seconds Tor
 * thinks it was unconscious for (or alternatively, the number of
 * seconds it went back in time).
 * <p>
 * This status event is sent as NOTICE severity normally, but WARN
 * severity if Tor is acting as a server currently.
 * <p>
 * {Recommendation for controller: ignore it, since we don't really
 * know what the user should do anyway. Hm.}
 * <p>
 * DANGEROUS_VERSION
 * "CURRENT=version"
 * "REASON=NEW/OBSOLETE/UNRECOMMENDED"
 * "RECOMMENDED=\"version, version, ...\""
 * Tor has found that directory servers don't recommend its version of
 * the Tor software.  RECOMMENDED is a comma-and-space-separated string
 * of Tor versions that are recommended.  REASON is NEW if this version
 * of Tor is newer than any recommended version, OBSOLETE if
 * this version of Tor is older than any recommended version, and
 * UNRECOMMENDED if some recommended versions of Tor are newer and
 * some are older than this version. (The "OBSOLETE" reason was called
 * "OLD" from Tor 0.1.2.3-alpha up to and including 0.2.0.12-alpha.)
 * <p>
 * {Controllers may want to suggest that the user upgrade OLD or
 * UNRECOMMENDED versions.  NEW versions may be known-insecure, or may
 * simply be development versions.}
 * <p>
 * TOO_MANY_CONNECTIONS
 * "CURRENT=NUM"
 * Tor has reached its ulimit -n or whatever the native limit is on file
 * descriptors or sockets.  CURRENT is the number of sockets Tor
 * currently has open.  The user should really do something about
 * this. The "current" argument shows the number of connections currently
 * open.
 * <p>
 * {Controllers may recommend that the user increase the limit, or
 * increase it for them.  Recommendations should be phrased in an
 * OS-appropriate way and automated when possible.}
 * <p>
 * BUG
 * "REASON=STRING"
 * Tor has encountered a situation that its developers never expected,
 * and the developers would like to learn that it happened. Perhaps
 * the controller can explain this to the user and encourage her to
 * file a bug report?
 * <p>
 * {Controllers should log bugs, but shouldn't annoy the user in case a
 * bug appears frequently.}
 * <p>
 * CLOCK_SKEW
 * SKEW="+" / "-" SECONDS
 * MIN_SKEW="+" / "-" SECONDS.
 * SOURCE="DIRSERV:" IP ":" Port /
 * "NETWORKSTATUS:" IP ":" Port /
 * "OR:" IP ":" Port /
 * "CONSENSUS"
 * If "SKEW" is present, it's an estimate of how far we are from the
 * time declared in the source.  (In other words, if we're an hour in
 * the past, the value is -3600.)  "MIN_SKEW" is present, it's a lower
 * bound.  If the source is a DIRSERV, we got the current time from a
 * connection to a dirserver.  If the source is a NETWORKSTATUS, we
 * decided we're skewed because we got a v2 networkstatus from far in
 * the future.  If the source is OR, the skew comes from a NETINFO
 * cell from a connection to another relay.  If the source is
 * CONSENSUS, we decided we're skewed because we got a networkstatus
 * consensus from the future.
 * <p>
 * {Tor should send this message to controllers when it thinks the
 * skew is so high that it will interfere with proper Tor operation.
 * Controllers shouldn't blindly adjust the clock, since the more
 * accurate source of skew info (DIRSERV) is currently
 * unauthenticated.}
 * <p>
 * BAD_LIBEVENT
 * "METHOD=" libevent method
 * "VERSION=" libevent version
 * "BADNESS=" "BROKEN" / "BUGGY" / "SLOW"
 * "RECOVERED=" "NO" / "YES"
 * Tor knows about bugs in using the configured event method in this
 * version of libevent.  "BROKEN" libevents won't work at all;
 * "BUGGY" libevents might work okay; "SLOW" libevents will work
 * fine, but not quickly.  If "RECOVERED" is YES, Tor managed to
 * switch to a more reliable (but probably slower!) libevent method.
 * <p>
 * {Controllers may want to warn the user if this event occurs, though
 * generally it's the fault of whoever built the Tor binary and there's
 * not much the user can do besides upgrade libevent or upgrade the
 * binary.}
 * <p>
 * DIR_ALL_UNREACHABLE
 * Tor believes that none of the known directory servers are
 * reachable -- this is most likely because the local network is
 * down or otherwise not working, and might help to explain for the
 * user why Tor appears to be broken.
 * <p>
 * {Controllers may want to warn the user if this event occurs; further
 * action is generally not possible.}
 * <p>
 * Actions for STATUS_CLIENT events can be as follows:
 * <p>
 * BOOTSTRAP
 * "PROGRESS=" num
 * "TAG=" Keyword
 * "SUMMARY=" String
 * ["WARNING=" String]
 * ["REASON=" Keyword]
 * ["COUNT=" num]
 * ["RECOMMENDATION=" Keyword]
 * ["HOST=" QuotedString]
 * ["HOSTADDR=" QuotedString]
 * <p>
 * Tor has made some progress at establishing a connection to the
 * Tor network, fetching directory information, or making its first
 * circuit; or it has encountered a problem while bootstrapping. This
 * status event is especially useful for users with slow connections
 * or with connectivity problems.
 * <p>
 * "Progress" gives a number between 0 and 100 for how far through
 * the bootstrapping process we are. "Summary" is a string that can
 * be displayed to the user to describe the *next* task that Tor
 * will tackle, i.e., the task it is working on after sending the
 * status event. "Tag" is a string that controllers can use to
 * recognize bootstrap phases, if they want to do something smarter
 * than just blindly displaying the summary string; see Section 5
 * for the current tags that Tor issues.
 * <p>
 * The StatusSeverity describes whether this is a normal bootstrap
 * phase (severity notice) or an indication of a bootstrapping
 * problem (severity warn).
 * <p>
 * For bootstrap problems, we include the same progress, tag, and
 * summary values as we would for a normal bootstrap event, but we
 * also include "warning", "reason", "count", and "recommendation"
 * key/value combos. The "count" number tells how many bootstrap
 * problems there have been so far at this phase. The "reason"
 * string lists one of the reasons allowed in the ORCONN event. The
 * "warning" argument string with any hints Tor has to offer about
 * why it's having troubles bootstrapping.
 * <p>
 * The "reason" values are long-term-stable controller-facing tags to
 * identify particular issues in a bootstrapping step.  The warning
 * strings, on the other hand, are human-readable. Controllers
 * SHOULD NOT rely on the format of any warning string. Currently
 * the possible values for "recommendation" are either "ignore" or
 * "warn" -- if ignore, the controller can accumulate the string in
 * a pile of problems to show the user if the user asks; if warn,
 * the controller should alert the user that Tor is pretty sure
 * there's a bootstrapping problem.
 * <p>
 * The "host" value is the identity digest (in hex) of the node we're
 * trying to connect to; the "hostaddr" is an address:port combination,
 * where 'address' is an ipv4 or ipv6 address.
 * <p>
 * Currently Tor uses recommendation=ignore for the first
 * nine bootstrap problem reports for a given phase, and then
 * uses recommendation=warn for subsequent problems at that
 * phase. Hopefully this is a good balance between tolerating
 * occasional errors and reporting serious problems quickly.
 * <p>
 * ENOUGH_DIR_INFO
 * Tor now knows enough network-status documents and enough server
 * descriptors that it's going to start trying to build circuits now.
 * [Newer versions of Tor (0.2.6.2-alpha and later):
 * If the consensus contains Exits (the typical case), Tor will build
 * both exit and internal circuits. If not, Tor will only build internal
 * circuits.]
 * <p>
 * {Controllers may want to use this event to decide when to indicate
 * progress to their users, but should not interrupt the user's browsing
 * to tell them so.}
 * <p>
 * NOT_ENOUGH_DIR_INFO
 * We discarded expired statuses and server descriptors to fall
 * below the desired threshold of directory information. We won't
 * try to build any circuits until ENOUGH_DIR_INFO occurs again.
 * <p>
 * {Controllers may want to use this event to decide when to indicate
 * progress to their users, but should not interrupt the user's browsing
 * to tell them so.}
 * <p>
 * CIRCUIT_ESTABLISHED
 * Tor is able to establish circuits for client use. This event will
 * only be sent if we just built a circuit that changed our mind --
 * that is, prior to this event we didn't know whether we could
 * establish circuits.
 * <p>
 * {Suggested use: controllers can notify their users that Tor is
 * ready for use as a client once they see this status event. [Perhaps
 * controllers should also have a timeout if too much time passes and
 * this event hasn't arrived, to give tips on how to troubleshoot.
 * On the other hand, hopefully Tor will send further status events
 * if it can identify the problem.]}
 * <p>
 * CIRCUIT_NOT_ESTABLISHED
 * "REASON=" "EXTERNAL_ADDRESS" / "DIR_ALL_UNREACHABLE" / "CLOCK_JUMPED"
 * We are no longer confident that we can build circuits. The "reason"
 * keyword provides an explanation: which other status event type caused
 * our lack of confidence.
 * <p>
 * {Controllers may want to use this event to decide when to indicate
 * progress to their users, but should not interrupt the user's browsing
 * to do so.}
 * [Note: only REASON=CLOCK_JUMPED is implemented currently.]
 * <p>
 * CONSENSUS_ARRIVED
 * Tor has received and validated a new consensus networkstatus.
 * (This event can be delayed a little while after the consensus
 * is received, if Tor needs to fetch certificates.)
 * <p>
 * DANGEROUS_PORT
 * "PORT=" port
 * "RESULT=" "REJECT" / "WARN"
 * A stream was initiated to a port that's commonly used for
 * vulnerable-plaintext protocols. If the Result is "reject", we
 * refused the connection; whereas if it's "warn", we allowed it.
 * <p>
 * {Controllers should warn their users when this occurs, unless they
 * happen to know that the application using Tor is in fact doing so
 * correctly (e.g., because it is part of a distributed bundle). They
 * might also want some sort of interface to let the user configure
 * their RejectPlaintextPorts and WarnPlaintextPorts config options.}
 * <p>
 * DANGEROUS_SOCKS
 * "PROTOCOL=" "SOCKS4" / "SOCKS5"
 * "ADDRESS=" IP:port
 * A connection was made to Tor's SOCKS port using one of the SOCKS
 * approaches that doesn't support hostnames -- only raw IP addresses.
 * If the client application got this address from gethostbyname(),
 * it may be leaking target addresses via DNS.
 * <p>
 * {Controllers should warn their users when this occurs, unless they
 * happen to know that the application using Tor is in fact doing so
 * correctly (e.g., because it is part of a distributed bundle).}
 * <p>
 * SOCKS_UNKNOWN_PROTOCOL
 * "DATA=string"
 * A connection was made to Tor's SOCKS port that tried to use it
 * for something other than the SOCKS protocol. Perhaps the user is
 * using Tor as an HTTP proxy?   The DATA is the first few characters
 * sent to Tor on the SOCKS port.
 * <p>
 * {Controllers may want to warn their users when this occurs: it
 * indicates a misconfigured application.}
 * <p>
 * SOCKS_BAD_HOSTNAME
 * "HOSTNAME=QuotedString"
 * Some application gave us a funny-looking hostname. Perhaps
 * it is broken? In any case it won't work with Tor and the user
 * should know.
 * <p>
 * {Controllers may want to warn their users when this occurs: it
 * usually indicates a misconfigured application.}
 * <p>
 * Actions for STATUS_SERVER can be as follows:
 * <p>
 * EXTERNAL_ADDRESS
 * "ADDRESS=IP"
 * "HOSTNAME=NAME"
 * "METHOD=CONFIGURED/CONFIGURED_ORPORT/DIRSERV/RESOLVED/
 * INTERFACE/GETHOSTNAME"
 * Our best idea for our externally visible IP has changed to 'IP'.  If
 * 'HOSTNAME' is present, we got the new IP by resolving 'NAME'.  If the
 * method is 'CONFIGURED', the IP was given verbatim as the Address
 * configuration option.  If the method is 'CONFIGURED_ORPORT', the IP was
 * given verbatim in the ORPort configuration option. If the method is
 * 'RESOLVED', we resolved the Address configuration option to get the IP.
 * If the method is 'GETHOSTNAME', we resolved our hostname to get the IP.
 * If the method is 'INTERFACE', we got the address of one of our network
 * interfaces to get the IP.  If the method is 'DIRSERV', a directory
 * server told us a guess for what our IP might be.
 * <p>
 * {Controllers may want to record this info and display it to the user.}
 * <p>
 * CHECKING_REACHABILITY
 * "ORADDRESS=IP:port"
 * "DIRADDRESS=IP:port"
 * We're going to start testing the reachability of our external OR port
 * or directory port.
 * <p>
 * {This event could affect the controller's idea of server status, but
 * the controller should not interrupt the user to tell them so.}
 * <p>
 * REACHABILITY_SUCCEEDED
 * "ORADDRESS=IP:port"
 * "DIRADDRESS=IP:port"
 * We successfully verified the reachability of our external OR port or
 * directory port (depending on which of ORADDRESS or DIRADDRESS is
 * given.)
 * <p>
 * {This event could affect the controller's idea of server status, but
 * the controller should not interrupt the user to tell them so.}
 * <p>
 * GOOD_SERVER_DESCRIPTOR
 * We successfully uploaded our server descriptor to at least one
 * of the directory authorities, with no complaints.
 * <p>
 * {Originally, the goal of this event was to declare "every authority
 * has accepted the descriptor, so there will be no complaints
 * about it." But since some authorities might be offline, it's
 * harder to get certainty than we had thought. As such, this event
 * is equivalent to ACCEPTED_SERVER_DESCRIPTOR below. Controllers
 * should just look at ACCEPTED_SERVER_DESCRIPTOR and should ignore
 * this event for now.}
 * <p>
 * SERVER_DESCRIPTOR_STATUS
 * "STATUS=" "LISTED" / "UNLISTED"
 * We just got a new networkstatus consensus, and whether we're in
 * it or not in it has changed. Specifically, status is "listed"
 * if we're listed in it but previous to this point we didn't know
 * we were listed in a consensus; and status is "unlisted" if we
 * thought we should have been listed in it (e.g. we were listed in
 * the last one), but we're not.
 * <p>
 * {Moving from listed to unlisted is not necessarily cause for
 * alarm. The relay might have failed a few reachability tests,
 * or the Internet might have had some routing problems. So this
 * feature is mainly to let relay operators know when their relay
 * has successfully been listed in the consensus.}
 * <p>
 * [Not implemented yet. We should do this in 0.2.2.x. -RD]
 * <p>
 * NAMESERVER_STATUS
 * "NS=addr"
 * "STATUS=" "UP" / "DOWN"
 * "ERR=" message
 * One of our nameservers has changed status.
 * <p>
 * {This event could affect the controller's idea of server status, but
 * the controller should not interrupt the user to tell them so.}
 * <p>
 * NAMESERVER_ALL_DOWN
 * All of our nameservers have gone down.
 * <p>
 * {This is a problem; if it happens often without the nameservers
 * coming up again, the user needs to configure more or better
 * nameservers.}
 * <p>
 * DNS_HIJACKED
 * Our DNS provider is providing an address when it should be saying
 * "NOTFOUND"; Tor will treat the address as a synonym for "NOTFOUND".
 * <p>
 * {This is an annoyance; controllers may want to tell admins that their
 * DNS provider is not to be trusted.}
 * <p>
 * DNS_USELESS
 * Our DNS provider is giving a hijacked address instead of well-known
 * websites; Tor will not try to be an exit node.
 * <p>
 * {Controllers could warn the admin if the relay is running as an
 * exit node: the admin needs to configure a good DNS server.
 * Alternatively, this happens a lot in some restrictive environments
 * (hotels, universities, coffeeshops) when the user hasn't registered.}
 * <p>
 * BAD_SERVER_DESCRIPTOR
 * "DIRAUTH=addr:port"
 * "REASON=string"
 * A directory authority rejected our descriptor.  Possible reasons
 * include malformed descriptors, incorrect keys, highly skewed clocks,
 * and so on.
 * <p>
 * {Controllers should warn the admin, and try to cope if they can.}
 * <p>
 * ACCEPTED_SERVER_DESCRIPTOR
 * "DIRAUTH=addr:port"
 * A single directory authority accepted our descriptor.
 * // actually notice
 * <p>
 * {This event could affect the controller's idea of server status, but
 * the controller should not interrupt the user to tell them so.}
 * <p>
 * REACHABILITY_FAILED
 * "ORADDRESS=IP:port"
 * "DIRADDRESS=IP:port"
 * We failed to connect to our external OR port or directory port
 * successfully.
 * <p>
 * {This event could affect the controller's idea of server status.  The
 * controller should warn the admin and suggest reasonable steps to take.}
 * <p>
 * HIBERNATION_STATUS
 * "STATUS=" "AWAKE" | "SOFT" | "HARD"
 * Our bandwidth based accounting status has changed, and we are now
 * relaying traffic/rejecting new connections/hibernating.
 * <p>
 * {This event could affect the controller's idea of server status.  The
 * controller MAY inform the admin, though presumably the accounting was
 * explicitly enabled for a reason.}
 * <p>
 * [This event was added in tor 0.2.9.0-alpha.]
 * <p>
 * 4.1.11. Our set of guard nodes has changed
 * <p>
 * Syntax:
 * <p>
 * "650" SP "GUARD" SP Type SP Name SP Status ... CRLF
 * Type = "ENTRY"
 * Name = ServerSpec
 * (Identifies the guard affected)
 * Status = "NEW" | "UP" | "DOWN" | "BAD" | "GOOD" | "DROPPED"
 * <p>
 * The ENTRY type indicates a guard used for connections to the Tor
 * network.
 * <p>
 * The Status values are:
 * <p>
 * "NEW"  -- This node was not previously used as a guard; now we have
 * picked it as one.
 * "DROPPED" -- This node is one we previously picked as a guard; we
 * no longer consider it to be a member of our guard list.
 * "UP"   -- The guard now seems to be reachable.
 * "DOWN" -- The guard now seems to be unreachable.
 * "BAD"  -- Because of flags set in the consensus and/or values in the
 * configuration, this node is now unusable as a guard.
 * "GOOD" -- Because of flags set in the consensus and/or values in the
 * configuration, this node is now usable as a guard.
 * <p>
 * Controllers must accept unrecognized types and unrecognized statuses.
 * <p>
 * 4.1.12. Network status has changed
 * <p>
 * Syntax:
 * <p>
 * "650" "+" "NS" CRLF 1*NetworkStatus "." CRLF "650" SP "OK" CRLF
 * <p>
 * The event is used whenever our local view of a relay status changes.
 * This happens when we get a new v3 consensus (in which case the entries
 * we see are a duplicate of what we see in the NEWCONSENSUS event,
 * below), but it also happens when we decide to mark a relay as up or
 * down in our local status, for example based on connection attempts.
 * <p>
 * [First added in 0.1.2.3-alpha]
 * <p>
 * 4.1.13. Bandwidth used on an application stream
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "STREAM_BW" SP StreamID SP BytesWritten SP BytesRead SP
 * Time CRLF
 * BytesWritten = 1*DIGIT
 * BytesRead = 1*DIGIT
 * Time = ISOTime2Frac
 * <p>
 * BytesWritten and BytesRead are the number of bytes written and read
 * by the application since the last STREAM_BW event on this stream.
 * <p>
 * Note that from Tor's perspective, *reading* a byte on a stream means
 * that the application *wrote* the byte. That's why the order of "written"
 * vs "read" is opposite for stream_bw events compared to bw events.
 * <p>
 * The Time field is provided only in versions 0.3.2.1-alpha and later. It
 * records when Tor created the bandwidth event.
 * <p>
 * These events are generated about once per second per stream; no events
 * are generated for streams that have not written or read. These events
 * apply only to streams entering Tor (such as on a SOCKSPort, TransPort,
 * or so on). They are not generated for exiting streams.
 * <p>
 * 4.1.14. Per-country client stats
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "CLIENTS_SEEN" SP TimeStarted SP CountrySummary SP
 * IPVersions CRLF
 * <p>
 * We just generated a new summary of which countries we've seen clients
 * from recently. The controller could display this for the user, e.g.
 * in their "relay" configuration window, to give them a sense that they
 * are actually being useful.
 * <p>
 * Currently only bridge relays will receive this event, but once we figure
 * out how to sufficiently aggregate and sanitize the client counts on
 * main relays, we might start sending these events in other cases too.
 * <p>
 * TimeStarted is a quoted string indicating when the reported summary
 * counts from (in UTCS).
 * <p>
 * The CountrySummary keyword has as its argument a comma-separated,
 * possibly empty set of "countrycode=count" pairs. For example (without
 * linebreak),
 * 650-CLIENTS_SEEN TimeStarted="2008-12-25 23:50:43"
 * CountrySummary=us=16,de=8,uk=8
 * <p>
 * The IPVersions keyword has as its argument a comma-separated set of
 * "protocol-family=count" pairs. For example,
 * IPVersions=v4=16,v6=40
 * <p>
 * Note that these values are rounded, not exact.  The rounding
 * algorithm is specified in the description of "geoip-client-origins"
 * in dir-spec.txt.
 * <p>
 * 4.1.15. New consensus networkstatus has arrived
 * <p>
 * The syntax is:
 * <p>
 * "650" "+" "NEWCONSENSUS" CRLF 1*NetworkStatus "." CRLF "650" SP
 * "OK" CRLF
 * <p>
 * A new consensus networkstatus has arrived. We include NS-style lines for
 * every relay in the consensus. NEWCONSENSUS is a separate event from the
 * NS event, because the list here represents every usable relay: so any
 * relay *not* mentioned in this list is implicitly no longer recommended.
 * <p>
 * [First added in 0.2.1.13-alpha]
 * <p>
 * 4.1.16. New circuit buildtime has been set
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "BUILDTIMEOUT_SET" SP Type SP "TOTAL_TIMES=" Total SP
 * "TIMEOUT_MS=" Timeout SP "XM=" Xm SP "ALPHA=" Alpha SP
 * "CUTOFF_QUANTILE=" Quantile SP "TIMEOUT_RATE=" TimeoutRate SP
 * "CLOSE_MS=" CloseTimeout SP "CLOSE_RATE=" CloseRate
 * CRLF
 * Type = "COMPUTED" / "RESET" / "SUSPENDED" / "DISCARD" / "RESUME"
 * Total = Integer count of timeouts stored
 * Timeout = Integer timeout in milliseconds
 * Xm = Estimated integer Pareto parameter Xm in milliseconds
 * Alpha = Estimated floating point Paredo parameter alpha
 * Quantile = Floating point CDF quantile cutoff point for this timeout
 * TimeoutRate = Floating point ratio of circuits that timeout
 * CloseTimeout = How long to keep measurement circs in milliseconds
 * CloseRate = Floating point ratio of measurement circuits that are closed
 * <p>
 * A new circuit build timeout time has been set. If Type is "COMPUTED",
 * Tor has computed the value based on historical data. If Type is "RESET",
 * initialization or drastic network changes have caused Tor to reset
 * the timeout back to the default, to relearn again. If Type is
 * "SUSPENDED", Tor has detected a loss of network connectivity and has
 * temporarily changed the timeout value to the default until the network
 * recovers. If type is "DISCARD", Tor has decided to discard timeout
 * values that likely happened while the network was down. If type is
 * "RESUME", Tor has decided to resume timeout calculation.
 * <p>
 * The Total value is the count of circuit build times Tor used in
 * computing this value. It is capped internally at the maximum number
 * of build times Tor stores (NCIRCUITS_TO_OBSERVE).
 * <p>
 * The Timeout itself is provided in milliseconds. Internally, Tor rounds
 * this value to the nearest second before using it.
 * <p>
 * [First added in 0.2.2.7-alpha]
 * <p>
 * 4.1.17. Signal received
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "SIGNAL" SP Signal CRLF
 * <p>
 * Signal = "RELOAD" / "DUMP" / "DEBUG" / "NEWNYM" / "CLEARDNSCACHE"
 * <p>
 * A signal has been received and actions taken by Tor. The meaning of each
 * signal, and the mapping to Unix signals, is as defined in section 3.7.
 * Future versions of Tor MAY generate signals other than those listed here;
 * controllers MUST be able to accept them.
 * <p>
 * If Tor chose to ignore a signal (such as NEWNYM), this event will not be
 * sent.  Note that some options (like ReloadTorrcOnSIGHUP) may affect the
 * semantics of the signals here.
 * <p>
 * Note that the HALT (SIGTERM) and SHUTDOWN (SIGINT) signals do not currently
 * generate any event.
 * <p>
 * [First added in 0.2.3.1-alpha]
 * <p>
 * 4.1.18. Configuration changed
 * <p>
 * The syntax is:
 * <p>
 * StartReplyLine *(MidReplyLine) EndReplyLine
 * <p>
 * StartReplyLine = "650-CONF_CHANGED" CRLF
 * MidReplyLine = "650-" KEYWORD ["=" VALUE] CRLF
 * EndReplyLine = "650 OK"
 * <p>
 * Tor configuration options have changed (such as via a SETCONF or RELOAD
 * signal). KEYWORD and VALUE specify the configuration option that was changed.
 * Undefined configuration options contain only the KEYWORD.
 * <p>
 * 4.1.19. Circuit status changed slightly
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "CIRC_MINOR" SP CircuitID SP CircEvent [SP Path]
 * [SP "BUILD_FLAGS=" BuildFlags] [SP "PURPOSE=" Purpose]
 * [SP "HS_STATE=" HSState] [SP "REND_QUERY=" HSAddress]
 * [SP "TIME_CREATED=" TimeCreated]
 * [SP "OLD_PURPOSE=" Purpose [SP "OLD_HS_STATE=" HSState]] CRLF
 * <p>
 * CircEvent =
 * "PURPOSE_CHANGED" / ; circuit purpose or HS-related state changed
 * "CANNIBALIZED"      ; circuit cannibalized
 * <p>
 * Clients MUST accept circuit events not listed above.
 * <p>
 * The "OLD_PURPOSE" field is provided for both PURPOSE_CHANGED and
 * CANNIBALIZED events.  The "OLD_HS_STATE" field is provided whenever
 * the "OLD_PURPOSE" field is provided and is a hidden-service-related
 * purpose.
 * <p>
 * Other fields are as specified in section 4.1.1 above.
 * <p>
 * [First added in 0.2.3.11-alpha]
 * <p>
 * 4.1.20. Pluggable transport launched
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "TRANSPORT_LAUNCHED" SP Type SP Name SP TransportAddress SP Port
 * Type = "server" | "client"
 * Name = The name of the pluggable transport
 * TransportAddress = An IPv4 or IPv6 address on which the pluggable
 * transport is listening for connections
 * Port = The TCP port on which it is listening for connections.
 * <p>
 * A pluggable transport called 'Name' of type 'Type' was launched
 * successfully and is now listening for connections on 'Address':'Port'.
 * <p>
 * 4.1.21. Bandwidth used on an OR or DIR or EXIT connection
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "CONN_BW" SP "ID=" ConnID SP "TYPE=" ConnType
 * SP "READ=" BytesRead SP "WRITTEN=" BytesWritten CRLF
 * <p>
 * ConnType = "OR" /  ; Carrying traffic within the tor network. This can
 * either be our own (client) traffic or traffic we're
 * relaying within the network.
 * "DIR" / ; Fetching tor descriptor data, or transmitting
 * descriptors we're mirroring.
 * "EXIT"  ; Carrying traffic between the tor network and an
 * external destination.
 * <p>
 * BytesRead = 1*DIGIT
 * BytesWritten = 1*DIGIT
 * <p>
 * Controllers MUST tolerate unrecognized connection types.
 * <p>
 * BytesWritten and BytesRead are the number of bytes written and read
 * by Tor since the last CONN_BW event on this connection.
 * <p>
 * These events are generated about once per second per connection; no
 * events are generated for connections that have not read or written.
 * These events are only generated if TestingTorNetwork is set.
 * <p>
 * [First added in 0.2.5.2-alpha]
 * <p>
 * 4.1.22. Bandwidth used by all streams attached to a circuit
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "CIRC_BW" SP "ID=" CircuitID SP "READ=" BytesRead SP
 * "WRITTEN=" BytesWritten SP "TIME=" Time SP
 * "DELIVERED_READ=" DeliveredBytesRead SP
 * "OVERHEAD_READ=" OverheadBytesRead SP
 * "DELIVERED_WRITTEN=" DeliveredBytesWritten CRLF
 * "OVERHEAD_WRITTEN=" OverheadBytesWritten SP
 * BytesRead = 1*DIGIT
 * BytesWritten = 1*DIGIT
 * OverheadBytesRead = 1*DIGIT
 * OverheadBytesWritten = 1*DIGIT
 * DeliveredBytesRead = 1*DIGIT
 * DeliveredBytesWritten = 1*DIGIT
 * Time = ISOTime2Frac
 * <p>
 * BytesRead and BytesWritten are the number of bytes read and written
 * on this circuit since the last CIRC_BW event. These bytes have not
 * necessarily been validated by Tor, and can include invalid cells,
 * dropped cells, and ignored cells (such as padding cells). These
 * values include the relay headers, but not circuit headers.
 * <p>
 * Circuit data that has been validated and processed by Tor is further
 * broken down into two categories: delivered payloads and overhead.
 * DeliveredBytesRead and DeliveredBytesWritten are the total relay cell
 * payloads transmitted since the last CIRC_BW event, not counting relay
 * cell headers or circuit headers. OverheadBytesRead and
 * OverheadBytesWritten are the extra unused bytes at the end of each
 * cell in order for it to be the fixed CELL_LEN bytes long.
 * <p>
 * The sum of DeliveredBytesRead and OverheadBytesRead MUST be less than
 * BytesRead, and the same is true for their written counterparts. This
 * sum represents the total relay cell bytes on the circuit that
 * have been validated by Tor, not counting relay headers and cell headers.
 * Subtracting this sum (plus relay cell headers) from the BytesRead
 * (or BytesWritten) value gives the byte count that Tor has decided to
 * reject due to protocol errors, or has otherwise decided to ignore.
 * <p>
 * The Time field is provided only in versions 0.3.2.1-alpha and later. It
 * records when Tor created the bandwidth event.
 * <p>
 * These events are generated about once per second per circuit; no events
 * are generated for circuits that had no attached stream writing or
 * reading.
 * <p>
 * [First added in 0.2.5.2-alpha]
 * <p>
 * [DELIVERED_READ, OVERHEAD_READ, DELIVERED_WRITTEN, and OVERHEAD_WRITTEN
 * were added in Tor 0.3.4.0-alpha]
 * <p>
 * 4.1.23. Per-circuit cell stats
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "CELL_STATS"
 * [ SP "ID=" CircuitID ]
 * [ SP "InboundQueue=" QueueID SP "InboundConn=" ConnID ]
 * [ SP "InboundAdded=" CellsByType ]
 * [ SP "InboundRemoved=" CellsByType SP
 * "InboundTime=" MsecByType ]
 * [ SP "OutboundQueue=" QueueID SP "OutboundConn=" ConnID ]
 * [ SP "OutboundAdded=" CellsByType ]
 * [ SP "OutboundRemoved=" CellsByType SP
 * "OutboundTime=" MsecByType ] CRLF
 * CellsByType, MsecByType = CellType ":" 1*DIGIT
 * 0*( "," CellType ":" 1*DIGIT )
 * CellType = 1*( "a" - "z" / "0" - "9" / "_" )
 * <p>
 * Examples are:
 * <p>
 * 650 CELL_STATS ID=14 OutboundQueue=19403 OutboundConn=15
 * OutboundAdded=create_fast:1,relay_early:2
 * OutboundRemoved=create_fast:1,relay_early:2
 * OutboundTime=create_fast:0,relay_early:0
 * 650 CELL_STATS InboundQueue=19403 InboundConn=32
 * InboundAdded=relay:1,created_fast:1
 * InboundRemoved=relay:1,created_fast:1
 * InboundTime=relay:0,created_fast:0
 * OutboundQueue=6710 OutboundConn=18
 * OutboundAdded=create:1,relay_early:1
 * OutboundRemoved=create:1,relay_early:1
 * OutboundTime=create:0,relay_early:0
 * <p>
 * ID is the locally unique circuit identifier that is only included if the
 * circuit originates at this node.
 * <p>
 * Inbound and outbound refer to the direction of cell flow through the
 * circuit which is either to origin (inbound) or from origin (outbound).
 * <p>
 * InboundQueue and OutboundQueue are identifiers of the inbound and
 * outbound circuit queues of this circuit.  These identifiers are only
 * unique per OR connection.  OutboundQueue is chosen by this node and
 * matches InboundQueue of the next node in the circuit.
 * <p>
 * InboundConn and OutboundConn are locally unique IDs of inbound and
 * outbound OR connection.  OutboundConn does not necessarily match
 * InboundConn of the next node in the circuit.
 * <p>
 * InboundQueue and InboundConn are not present if the circuit originates
 * at this node.  OutboundQueue and OutboundConn are not present if the
 * circuit (currently) ends at this node.
 * <p>
 * InboundAdded and OutboundAdded are total number of cells by cell type
 * added to inbound and outbound queues.  Only present if at least one cell
 * was added to a queue.
 * <p>
 * InboundRemoved and OutboundRemoved are total number of cells by
 * cell type processed from inbound and outbound queues.  InboundTime and
 * OutboundTime are total waiting times in milliseconds of all processed
 * cells by cell type.  Only present if at least one cell was removed from
 * a queue.
 * <p>
 * These events are generated about once per second per circuit; no
 * events are generated for circuits that have not added or processed any
 * cell.  These events are only generated if TestingTorNetwork is set.
 * <p>
 * [First added in 0.2.5.2-alpha]
 * <p>
 * 4.1.24. Token buckets refilled
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "TB_EMPTY" SP BucketName [ SP "ID=" ConnID ] SP
 * "READ=" ReadBucketEmpty SP "WRITTEN=" WriteBucketEmpty SP
 * "LAST=" LastRefill CRLF
 * <p>
 * BucketName = "GLOBAL" / "RELAY" / "ORCONN"
 * ReadBucketEmpty = 1*DIGIT
 * WriteBucketEmpty = 1*DIGIT
 * LastRefill = 1*DIGIT
 * <p>
 * Examples are:
 * <p>
 * 650 TB_EMPTY ORCONN ID=16 READ=0 WRITTEN=0 LAST=100
 * 650 TB_EMPTY GLOBAL READ=93 WRITTEN=93 LAST=100
 * 650 TB_EMPTY RELAY READ=93 WRITTEN=93 LAST=100
 * <p>
 * This event is generated when refilling a previously empty token
 * bucket.  BucketNames "GLOBAL" and "RELAY" keywords are used for the
 * global or relay token buckets, BucketName "ORCONN" is used for the
 * token buckets of an OR connection.  Controllers MUST tolerate
 * unrecognized bucket names.
 * <p>
 * ConnID is only included if the BucketName is "ORCONN".
 * <p>
 * If both global and relay buckets and/or the buckets of one or more OR
 * connections run out of tokens at the same time, multiple separate
 * events are generated.
 * <p>
 * ReadBucketEmpty (WriteBucketEmpty) is the time in millis that the read
 * (write) bucket was empty since the last refill.  LastRefill is the
 * time in millis since the last refill.
 * <p>
 * If a bucket went negative and if refilling tokens didn't make it go
 * positive again, there will be multiple consecutive TB_EMPTY events for
 * each refill interval during which the bucket contained zero tokens or
 * less.  In such a case, ReadBucketEmpty or WriteBucketEmpty are capped
 * at LastRefill in order not to report empty times more than once.
 * <p>
 * These events are only generated if TestingTorNetwork is set.
 * <p>
 * [First added in 0.2.5.2-alpha]
 * <p>
 * 4.1.25. HiddenService descriptors
 * <p>
 * The syntax is:
 * <p>
 * "650" SP "HS_DESC" SP Action SP HSAddress SP AuthType SP HsDir
 * [SP DescriptorID] [SP "REASON=" Reason] [SP "REPLICA=" Replica]
 * [SP "HSDIR_INDEX=" HSDirIndex]
 * <p>
 * Action =  "REQUESTED" / "UPLOAD" / "RECEIVED" / "UPLOADED" / "IGNORE" /
 * "FAILED" / "CREATED"
 * HSAddress = 16*Base32Character / 56*Base32Character / "UNKNOWN"
 * AuthType = "NO_AUTH" / "BASIC_AUTH" / "STEALTH_AUTH" / "UNKNOWN"
 * HsDir = LongName / Fingerprint / "UNKNOWN"
 * DescriptorID = 32*Base32Character / 43*Base64Character
 * Reason = "BAD_DESC" / "QUERY_REJECTED" / "UPLOAD_REJECTED" / "NOT_FOUND" /
 * "UNEXPECTED" / "QUERY_NO_HSDIR" / "QUERY_RATE_LIMITED"
 * Replica = 1*DIGIT
 * HSDirIndex = 64*HEXDIG
 * <p>
 * These events will be triggered when required HiddenService descriptor is
 * not found in the cache and a fetch or upload with the network is performed.
 * <p>
 * If the fetch was triggered with only a DescriptorID (using the HSFETCH
 * command for instance), the HSAddress only appears in the Action=RECEIVED
 * since there is no way to know the HSAddress from the DescriptorID thus
 * the value will be "UNKNOWN".
 * <p>
 * If we already had the v0 descriptor, the newly fetched v2 descriptor
 * will be ignored and a "HS_DESC" event with "IGNORE" action will be
 * generated.
 * <p>
 * For HsDir, LongName is always preferred. If HsDir cannot be found in node
 * list at the time event is sent, Fingerprint will be used instead.
 * <p>
 * If Action is "FAILED", Tor SHOULD send Reason field as well. Possible
 * values of Reason are:
 * - "BAD_DESC" - descriptor was retrieved, but found to be unparsable.
 * - "QUERY_REJECTED" - query was rejected by HS directory.
 * - "UPLOAD_REJECTED" - descriptor was rejected by HS directory.
 * - "NOT_FOUND" - HS descriptor with given identifier was not found.
 * - "UNEXPECTED" - nature of failure is unknown.
 * - "QUERY_NO_HSDIR" - No suitable HSDir were found for the query.
 * - "QUERY_RATE_LIMITED" - query for this service is rate-limited
 * <p>
 * For "QUERY_NO_HSDIR" or "QUERY_RATE_LIMITED", the HsDir will be set to
 * "UNKNOWN" which was introduced in tor 0.3.1.0-alpha and 0.4.1.0-alpha
 * respectively.
 * <p>
 * If Action is "CREATED", Tor SHOULD send Replica field as well. The Replica
 * field contains the replica number of the generated descriptor. The Replica
 * number is specified in rend-spec.txt section 1.3 and determines the
 * descriptor ID of the descriptor.
 * <p>
 * For hidden service v3, the following applies:
 * <p>
 * The "HSDIR_INDEX=" is an optional field that is only for version 3
 * which contains the computed index of the HsDir the descriptor was
 * uploaded to or fetched from.
 * <p>
 * The "DescriptorID" key is the descriptor blinded key used for the index
 * value at the "HsDir".
 * <p>
 * The "REPLICA=" field is not used for the "CREATED" event because v3
 * doesn't use the replica number in the descriptor ID computation.
 * <p>
 * Because client authentication is not yet implemented, the "AuthType"
 * field is always "NO_AUTH".
 * <p>
 * [HS v3 support added 0.3.3.1-alpha]
 * <p>
 * 4.1.26. HiddenService descriptors content
 * <p>
 * The syntax is:
 * <p>
 * "650" "+" "HS_DESC_CONTENT" SP HSAddress SP DescId SP HsDir CRLF
 * Descriptor CRLF "." CRLF "650" SP "OK" CRLF
 * <p>
 * HSAddress = 16*Base32Character / 56*Base32Character / "UNKNOWN"
 * DescId = 32*Base32Character / 32*Base64Character
 * HsDir = LongName / "UNKNOWN"
 * Descriptor = The text of the descriptor formatted as specified in
 * rend-spec.txt section 1.3 (v2) or rend-spec-v3.txt
 * section 2.4 (v3) or empty string on failure.
 * <p>
 * This event is triggered when a successfully fetched HS descriptor is
 * received. The text of that descriptor is then replied. If the HS_DESC
 * event is enabled, it is replied just after the RECEIVED action.
 * <p>
 * If a fetch fails, the Descriptor is an empty string and HSAddress is set
 * to "UNKNOWN". The HS_DESC event should be used to get more information on
 * the failed request.
 * <p>
 * If the fetch fails for the QUERY_NO_HSDIR or QUERY_RATE_LIMITED reason from
 * the HS_DESC event, the HsDir is set to "UNKNOWN". This was introduced in
 * 0.3.1.0-alpha and 0.4.1.0-alpha respectively.
 * <p>
 * It's expected to receive a reply relatively fast as in it's the time it
 * takes to fetch something over the Tor network. This can be between a
 * couple of seconds up to 60 seconds (not a hard limit). But, in any cases,
 * this event will reply either the descriptor's content or an empty one.
 * <p>
 * [HS_DESC_CONTENT was added in Tor 0.2.7.1-alpha]
 * [HS v3 support added 0.3.3.1-alpha]
 * <p>
 * 4.1.27. Network liveness has changed
 * <p>
 * Syntax:
 * <p>
 * "650" SP "NETWORK_LIVENESS" SP Status CRLF
 * Status = "UP" /  ; The network now seems to be reachable.
 * "DOWN" /  ; The network now seems to be unreachable.
 * <p>
 * Controllers MUST tolerate unrecognized status types.
 * <p>
 * [NETWORK_LIVENESS was added in Tor 0.2.7.2-alpha]
 * <p>
 * 4.1.28. Pluggable Transport Logs
 * <p>
 * Syntax:
 * <p>
 * "650" SP "PT_LOG" SP PT=Program SP Message
 * <p>
 * Program = The program path as defined in the *TransportPlugin
 * configuration option. Tor accepts relative and full path.
 * Message = The log message that the PT sends back to the tor parent
 * process minus the "LOG" string prefix. Formatted as
 * specified in pt-spec.txt section "3.3.4. Pluggable
 * Transport Log Message".
 * <p>
 * This event is triggered when tor receives a log message from the PT.
 * <p>
 * Example:
 * <p>
 * PT (obfs4): LOG SEVERITY=debug MESSAGE="Connected to bridge A"
 * <p>
 * the resulting control port event would be:
 * <p>
 * Tor: 650 PT_LOG PT=/usr/bin/obs4proxy SEVERITY=debug MESSAGE="Connected to bridge A"
 * <p>
 * [PT_LOG was added in Tor 0.4.0.1-alpha]
 * <p>
 * 4.1.29. Pluggable Transport Status
 * <p>
 * Syntax:
 * <p>
 * "650" SP "PT_STATUS" SP PT=Program SP TRANSPORT=Transport SP Message
 * <p>
 * Program = The program path as defined in the *TransportPlugin
 * configuration option. Tor accepts relative and full path.
 * Transport = This value indicate a hint on what the PT is such as the
 * name or the protocol used for instance.
 * Message = The status message that the PT sends back to the tor parent
 * process minus the "STATUS" string prefix. Formatted as
 * specified in pt-spec.txt section "3.3.5 Pluggable
 * Transport Status Message".
 * <p>
 * This event is triggered when tor receives a log message from the PT.
 * <p>
 * Example:
 * <p>
 * PT (obfs4): STATUS TRANSPORT=obfs4 CONNECT=Success
 * <p>
 * the resulting control port event would be:
 * <p>
 * Tor: 650 PT_STATUS PT=/usr/bin/obs4proxy TRANSPORT=obfs4 CONNECT=Success
 * <p>
 * [PT_STATUS was added in Tor 0.4.0.1-alpha]
 */
public class TorEventSocket extends TorAbstractControlSocket
{
    public static class Event
    {
        public static final String STATUS_ENOUGH_DIR_INFO = "status/enough-dir-info";
        public static final String STATUS_REACHABILITY_SUCCEEDED_DIR = "status/reachability-succeeded/dir";
        public static final String NETWORK_LIVENESS = "network-liveness";
    }
    
    public interface EventHandler
    {
        void onEvent(TorEventSocket socket, List<Reply> replyList);

        void onException(TorEventSocket socket, Exception e);
    }

    private final List<String> registeredEvents;
    private final EventHandler eventHandler;

    public TorEventSocket(PasswordDigest password, List<String> registeredEvents, EventHandler eventHandler, MainThreadDispatcher mainThreadDispatcher)
    {
        super(password, mainThreadDispatcher);

        this.registeredEvents = registeredEvents;
        this.eventHandler = eventHandler;
    }

    @Override
    protected void onConnectedAsync()
    {
        super.onConnectedAsync();

        setEvents(registeredEvents);

        try
        {
            do
            {
                read();
            }
            while (socket.isConnected());
        }
        catch (Exception e)
        {
            onException(e);
        }
    }

    @Override
    protected void onReply(List<Reply> replyList)
    {
        super.onReply(replyList);

        List<Reply> eventReplies = new ArrayList<>();

        for (Reply r : replyList)
            if (r.getStatus() == ResponseCode.AsynchronousEventNotification.getValue())
                eventReplies.add(r);

        //TODO: naparsovat eventy do separatnich classes

        if (eventHandler != null && eventReplies.size() > 0)
            eventHandler.onEvent(TorEventSocket.this, eventReplies);
    }

    @Override
    protected void onException(Exception e)
    {
        super.onException(e);

        if (eventHandler != null)
            eventHandler.onException(TorEventSocket.this, e);
    }

    /**
     * Request that the server inform the client about interesting events.
     * Each element of <b>events</b> is one of the following Strings:
     * ["CIRC" | "STREAM" | "ORCONN" | "BW" | "DEBUG" |
     * "INFO" | "NOTICE" | "WARN" | "ERR" | "NEWDESC" | "ADDRMAP"] .
     * <p>
     * Any events not listed in the <b>events</b> are turned off; thus, calling
     * setEvents with an empty <b>events</b> argument turns off all event reporting.
     *
     * @return
     */
    public List<Reply> setEvents(List<String> events)
    {
        StringBuilder sb = new StringBuilder("SETEVENTS");
        for (String event : events)
            sb.append(" ").append(event);

        sb.append("\r\n");

        return send(sb.toString(), null);
    }
}
