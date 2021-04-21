// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information

package com.alloc64.torlib.control;

import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;

/**
 * A connection to a running Tor process as specified in control-spec.txt.
 * <p>
 * TODO: @mj simplify & make a bit pretty in future
 */
public class TorControlConnection implements TorControlCommands, Runnable
{
    public static TorControlConnection create(ParcelFileDescriptor pfd, EventHandler eventHandler)
    {
        FileDescriptor fd = pfd.getFileDescriptor();

        return new TorControlConnection(new FileInputStream(fd), new FileOutputStream(fd), eventHandler);
    }

    private final LinkedList<Waiter> waiters;
    private final BufferedReader input;
    private final Writer output;

    private final EventHandler eventHandler;
    private volatile IOException parseThreadException;

    static class Waiter
    {
        List<ReplyLine> response; // Locking: this
        boolean interrupted;

        synchronized List<ReplyLine> getResponse() throws InterruptedException
        {
            while (response == null)
            {
                wait();

                if (interrupted)
                    throw new InterruptedException();
            }

            return response;
        }

        synchronized void setResponse(List<ReplyLine> response)
        {
            this.response = response;
            notifyAll();
        }

        synchronized void interrupt()
        {
            interrupted = true;
            notifyAll();
        }
    }

    static class ReplyLine
    {
        final String status;
        final String msg;
        final String rest;

        ReplyLine(String status, String msg, String rest)
        {
            this.status = status;
            this.msg = msg;
            this.rest = rest;
        }
    }

    /**
     * Create a new TorControlConnection to communicate with Tor over
     * an arbitrary pair of data streams.
     */
    private TorControlConnection(InputStream inputStream, OutputStream outputStream, EventHandler eventHandler)
    {
        this.input = new BufferedReader(new InputStreamReader(inputStream));
        this.output = new OutputStreamWriter(outputStream);
        this.eventHandler = eventHandler;

        this.waiters = new LinkedList<>();
    }

    public void start()
    {
        Executors.newSingleThreadExecutor().execute(this);
    }

    protected void writeEscaped(String s) throws IOException
    {
        StringTokenizer st = new StringTokenizer(s, "\n");
        while (st.hasMoreTokens())
        {
            String line = st.nextToken();
            if (line.startsWith("."))
                line = "." + line;
            if (line.endsWith("\r"))
                line += "\n";
            else
                line += "\r\n";

            output.write(line);
        }
        output.write(".\r\n");
    }

    protected String quote(String s)
    {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '\r':
                case '\n':
                case '\\':
                case '\"':
                    sb.append('\\');
            }
            sb.append(c);
        }
        sb.append('\"');
        return sb.toString();
    }

    protected ArrayList<ReplyLine> readReply() throws IOException
    {
        ArrayList<ReplyLine> reply = new ArrayList<ReplyLine>();
        char c;
        do
        {
            String line = input.readLine();
            if (line == null)
            {
                // if line is null, the end of the stream has been reached, i.e.
                // the connection to Tor has been closed!
                if (reply.isEmpty())
                {
                    // nothing received so far, can exit cleanly
                    return reply;
                }
                // received half of a reply before the connection broke down
                throw new IllegalStateException("Connection to Tor broke down while receiving reply!");
            }

            if (line.length() < 4)
                throw new IllegalStateException("Line (\"" + line + "\") too short");

            String status = line.substring(0, 3);
            c = line.charAt(3);
            String msg = line.substring(4);
            String rest = null;
            if (c == '+')
            {
                StringBuilder data = new StringBuilder();
                while (true)
                {
                    line = input.readLine();

                    if (line.equals("."))
                        break;
                    else if (line.startsWith("."))
                        line = line.substring(1);
                    data.append(line).append('\n');
                }
                rest = data.toString();
            }
            reply.add(new ReplyLine(status, msg, rest));
        }
        while (c != ' ');

        return reply;
    }

    public synchronized List<ReplyLine> sendAndWaitForResponse(String s, String rest) throws IOException
    {
        if (parseThreadException != null)
            throw parseThreadException;

        Waiter w = new Waiter();

        synchronized (waiters)
        {
            output.write(s);
            if (rest != null)
                writeEscaped(rest);
            output.flush();
            waiters.addLast(w);
        }
        List<ReplyLine> lst;

        try
        {
            lst = w.getResponse();
        }
        catch (InterruptedException ex)
        {
            throw new IOException("Interrupted");
        }

        for (ReplyLine c : lst)
        {
            if (!c.status.startsWith("2"))
                throw new TorControlError("Error reply: " + c.msg);
        }

        return lst;
    }

    /**
     * Helper: decode a CMD_EVENT command and dispatch it to our
     * EventHandler (if any).
     */
    protected void handleEvent(ArrayList<ReplyLine> events)
    {
        if (eventHandler == null)
            return;

        for (ReplyLine line : events)
        {
            int idx = line.msg.indexOf(' ');
            String tp = line.msg.substring(0, idx).toUpperCase();
            String rest = line.msg.substring(idx + 1);

            List<String> lst;

            switch (tp)
            {
                case "CIRC":
                    lst = Bytes.splitStr(null, rest);
                    eventHandler.circuitStatus(lst.get(1),
                            lst.get(0),
                            lst.get(1).equals("LAUNCHED")
                                    || lst.size() < 3 ? ""
                                    : lst.get(2));
                    break;

                case "STREAM":
                    lst = Bytes.splitStr(null, rest);
                    eventHandler.streamStatus(lst.get(1),
                            lst.get(0),
                            lst.get(3));
                    // XXXX circID.
                    break;

                case "ORCONN":
                    lst = Bytes.splitStr(null, rest);
                    eventHandler.orConnStatus(lst.get(1), lst.get(0));
                    break;

                case "BW":
                    lst = Bytes.splitStr(null, rest);
                    eventHandler.bandwidthUsed(Integer.parseInt(lst.get(0)),
                            Integer.parseInt(lst.get(1)));
                    break;

                case "NEWDESC":
                    lst = Bytes.splitStr(null, rest);
                    eventHandler.newDescriptors(lst);
                    break;

                case "DEBUG":
                case "INFO":
                case "NOTICE":
                case "WARN":
                case "ERR":
                    eventHandler.message(tp, rest);
                    break;
                default:
                    eventHandler.unrecognized(tp, rest);
                    break;
            }
        }
    }

    @Override
    public void run()
    {
        try
        {
            do
            {
                ArrayList<ReplyLine> lst = readReply();
                if (lst.isEmpty())
                {
                    // interrupted queued waiters, there won't be any response.
                    synchronized (waiters)
                    {
                        if (!waiters.isEmpty())
                        {
                            for (Waiter w : waiters)
                            {
                                w.interrupt();
                            }
                        }
                    }

                    parseThreadException = new IOException("Tor is no longer running");
                    // connection has been closed remotely! end the loop!
                    return;
                }
                if ((lst.get(0)).status.startsWith("6"))
                    handleEvent(lst);
                else
                {
                    synchronized (waiters)
                    {
                        if (!waiters.isEmpty())
                        {
                            Waiter w;
                            w = waiters.removeFirst();
                            w.setResponse(lst);
                        }
                    }

                }
            }
            while (true);
        }
        catch (IOException ex)
        {
            parseThreadException = ex;
        }
    }

    /**
     * Request that the server inform the client about interesting events.
     * Each element of <b>events</b> is one of the following Strings:
     * ["CIRC" | "STREAM" | "ORCONN" | "BW" | "DEBUG" |
     * "INFO" | "NOTICE" | "WARN" | "ERR" | "NEWDESC" | "ADDRMAP"] .
     * <p>
     * Any events not listed in the <b>events</b> are turned off; thus, calling
     * setEvents with an empty <b>events</b> argument turns off all event reporting.
     */
    public void setEvents(List<String> events) throws IOException
    {
        StringBuilder sb = new StringBuilder("SETEVENTS");
        for (String event : events)
        {
            sb.append(" ").append(event);
        }
        sb.append("\r\n");
        sendAndWaitForResponse(sb.toString(), null);
    }

    /**
     * Authenticates the controller to the Tor server.
     * <p>
     * By default, the current Tor implementation trusts all local users, and
     * the controller can authenticate itself by calling authenticate(new byte[0]).
     * <p>
     * If the 'CookieAuthentication' option is true, Tor writes a "magic cookie"
     * file named "control_auth_cookie" into its data directory.  To authenticate,
     * the controller must send the contents of this file in <b>auth</b>.
     * <p>
     * If the 'HashedControlPassword' option is set, <b>auth</b> must contain the salted
     * hash of a secret password.  The salted hash is computed according to the
     * S2K algorithm in RFC 2440 (OpenPGP), and prefixed with the s2k specifier.
     * This is then encoded in hexadecimal, prefixed by the indicator sequence
     * "16:".
     * <p>
     * You can generate the salt of a password by calling
     * 'tor --hash-password <password>'
     * or by using the provided PasswordDigest class.
     * To authenticate under this scheme, the controller sends Tor the original
     * secret that was used to generate the password.
     */
    public void authenticate(byte[] auth) throws IOException
    {
        String cmd = "AUTHENTICATE " + Bytes.hex(auth) + "\r\n";
        sendAndWaitForResponse(cmd, null);
    }

    /**
     * Sends a signal from the controller to the Tor server.
     * <b>signal</b> is one of the following Strings:
     * <ul>
     * <li>"RELOAD" or "HUP" :  Reload config items, refetch directory</li>
     * <li>"SHUTDOWN" or "INT" : Controlled shutdown: if server is an OP, exit immediately.
     *     If it's an OR, close listeners and exit after 30 seconds</li>
     * <li>"DUMP" or "USR1" : Dump stats: log information about open connections and circuits</li>
     * <li>"DEBUG" or "USR2" : Debug: switch all open logs to loglevel debug</li>
     * <li>"HALT" or "TERM" : Immediate shutdown: clean up and exit now</li>
     * </ul>
     */
    public void signal(String signal) throws IOException
    {
        String cmd = "SIGNAL " + signal + "\r\n";
        sendAndWaitForResponse(cmd, null);
    }

    /**
     * Send a signal to the Tor process to shut it down or halt it.
     * Does not wait for a response.
     */
    public void shutdownTor(String signal) throws IOException
    {
        String s = "SIGNAL " + signal + "\r\n";
        Waiter w = new Waiter();

        synchronized (waiters)
        {
            output.write(s);
            output.flush();
        }
    }

    /**
     * Tells the Tor server that future SOCKS requests for connections to a set of original
     * addresses should be replaced with connections to the specified replacement
     * addresses.  Each element of <b>kvLines</b> is a String of the form
     * "old-address new-address".  This function returns the new address mapping.
     * <p>
     * The client may decline to provide a body for the original address, and
     * instead send a special null address ("0.0.0.0" for IPv4, "::0" for IPv6, or
     * "." for hostname), signifying that the server should choose the original
     * address itself, and return that address in the reply.  The server
     * should ensure that it returns an element of address space that is unlikely
     * to be in actual use.  If there is already an address mapped to the
     * destination address, the server may reuse that mapping.
     * <p>
     * If the original address is already mapped to a different address, the old
     * mapping is removed.  If the original address and the destination address
     * are the same, the server removes any mapping in place for the original
     * address.
     * <p>
     * Mappings set by the controller last until the Tor process exits:
     * they never expire. If the controller wants the mapping to last only
     * a certain time, then it must explicitly un-map the address when that
     * time has elapsed.
     */
    public Map<String, String> mapAddresses(Collection<String> kvLines) throws IOException
    {
        StringBuilder sb = new StringBuilder("MAPADDRESS");
        for (String kv : kvLines)
        {
            int i = kv.indexOf(' ');
            sb.append(" ").append(kv.substring(0, i)).append("=")
                    .append(quote(kv.substring(i + 1)));
        }
        sb.append("\r\n");
        List<ReplyLine> lst = sendAndWaitForResponse(sb.toString(), null);
        Map<String, String> result = new HashMap<String, String>();
        for (ReplyLine replyLine : lst)
        {
            String kv = replyLine.msg;
            int idx = kv.indexOf('=');
            result.put(kv.substring(0, idx),
                    kv.substring(idx + 1));
        }
        return result;
    }

    public Map<String, String> mapAddresses(Map<String, String> addresses) throws IOException
    {
        List<String> kvList = new ArrayList<String>();
        for (Map.Entry<String, String> e : addresses.entrySet())
        {
            kvList.add(e.getKey() + " " + e.getValue());
        }
        return mapAddresses(kvList);
    }

    public String mapAddress(String fromAddr, String toAddr) throws IOException
    {
        List<String> lst = new ArrayList<String>();
        lst.add(fromAddr + " " + toAddr + "\n");
        Map<String, String> m = mapAddresses(lst);
        return m.get(fromAddr);
    }

    /**
     * Queries the Tor server for keyed values that are not stored in the torrc
     * configuration file.  Returns a map of keys to values.
     * <p>
     * Recognized keys include:
     * <ul>
     * <li>"version" : The version of the server's software, including the name
     *  of the software. (example: "Tor 0.0.9.4")</li>
     * <li>"desc/id/<OR identity>" or "desc/name/<OR nickname>" : the latest server
     * descriptor for a given OR, NUL-terminated.  If no such OR is known, the
     * corresponding value is an empty string.</li>
     * <li>"network-status" : a space-separated list of all known OR identities.
     * This is in the same format as the router-status line in directories;
     * see tor-spec.txt for details.</li>
     * <li>"addr-mappings/all"</li>
     * <li>"addr-mappings/config"</li>
     * <li>"addr-mappings/cache"</li>
     * <li>"addr-mappings/control" : a space-separated list of address mappings, each
     * in the form of "from-address=to-address".  The 'config' key
     * returns those address mappings set in the configuration; the 'cache'
     * key returns the mappings in the client-side DNS cache; the 'control'
     * key returns the mappings set via the control interface; the 'all'
     * target returns the mappings set through any mechanism.</li>
     * <li>"circuit-status" : A series of lines as for a circuit status event. Each line is of the form:
     * "CircuitID CircStatus Path"</li>
     * <li>"stream-status" : A series of lines as for a stream status event.  Each is of the form:
     * "StreamID StreamStatus CircID Target"</li>
     * <li>"orconn-status" : A series of lines as for an OR connection status event.  Each is of the
     * form: "ServerID ORStatus"</li>
     * </ul>
     */
    public Map<String, String> getInfo(Collection<String> keys) throws IOException
    {
        StringBuilder sb = new StringBuilder("GETINFO");
        for (String key : keys)
            sb.append(" ").append(key);

        sb.append("\r\n");
        List<ReplyLine> lst = sendAndWaitForResponse(sb.toString(), null);
        Map<String, String> m = new HashMap<String, String>();
        for (ReplyLine line : lst)
        {
            int idx = line.msg.indexOf('=');
            if (idx < 0)
                break;
            String k = line.msg.substring(0, idx);
            String v;
            if (line.rest != null)
            {
                v = line.rest;
            }
            else
            {
                v = line.msg.substring(idx + 1);
            }
            m.put(k, v);
        }
        return m;
    }


    /**
     * Return the value of the information field 'key'
     */
    public String getInfo(String key) throws IOException
    {
        List<String> lst = new ArrayList<String>();
        lst.add(key);
        Map<String, String> m = getInfo(lst);
        return m.get(key);
    }

    /**
     * An extendCircuit request takes one of two forms: either the <b>circID</b> is zero, in
     * which case it is a request for the server to build a new circuit according
     * to the specified path, or the <b>circID</b> is nonzero, in which case it is a
     * request for the server to extend an existing circuit with that ID according
     * to the specified <b>path</b>.
     * <p>
     * If successful, returns the Circuit ID of the (maybe newly created) circuit.
     */
    public String extendCircuit(String circID, String path) throws IOException
    {
        List<ReplyLine> lst = sendAndWaitForResponse(
                "EXTENDCIRCUIT " + circID + " " + path + "\r\n", null);
        return (lst.get(0)).msg;
    }

    /**
     * Informs the Tor server that the stream specified by <b>streamID</b> should be
     * associated with the circuit specified by <b>circID</b>.
     * <p>
     * Each stream may be associated with
     * at most one circuit, and multiple streams may share the same circuit.
     * Streams can only be attached to completed circuits (that is, circuits that
     * have sent a circuit status "BUILT" event or are listed as built in a
     * getInfo circuit-status request).
     * <p>
     * If <b>circID</b> is 0, responsibility for attaching the given stream is
     * returned to Tor.
     * <p>
     * By default, Tor automatically attaches streams to
     * circuits itself, unless the configuration variable
     * "__LeaveStreamsUnattached" is set to "1".  Attempting to attach streams
     * via TC when "__LeaveStreamsUnattached" is false may cause a race between
     * Tor and the controller, as both attempt to attach streams to circuits.
     */
    public void attachStream(String streamID, String circID)
            throws IOException
    {
        sendAndWaitForResponse("ATTACHSTREAM " + streamID + " " + circID + "\r\n", null);
    }

    /**
     * Tells Tor about the server descriptor in <b>desc</b>.
     * <p>
     * The descriptor, when parsed, must contain a number of well-specified
     * fields, including fields for its nickname and identity.
     */
    // More documentation here on format of desc?
    // No need for return value?  control-spec.txt says reply is merely "250 OK" on success...
    public String postDescriptor(String desc) throws IOException
    {
        List<ReplyLine> lst = sendAndWaitForResponse("+POSTDESCRIPTOR\r\n", desc);
        return (lst.get(0)).msg;
    }

    /**
     * Tells Tor to change the exit address of the stream identified by <b>streamID</b>
     * to <b>address</b>. No remapping is performed on the new provided address.
     * <p>
     * To be sure that the modified address will be used, this event must be sent
     * after a new stream event is received, and before attaching this stream to
     * a circuit.
     */
    public void redirectStream(String streamID, String address) throws IOException
    {
        sendAndWaitForResponse("REDIRECTSTREAM " + streamID + " " + address + "\r\n",
                null);
    }

    /**
     * Tells Tor to close the stream identified by <b>streamID</b>.
     * <b>reason</b> should be one of the Tor RELAY_END reasons given in tor-spec.txt, as a decimal:
     * <ul>
     * <li>1 -- REASON_MISC           (catch-all for unlisted reasons)</li>
     * <li>2 -- REASON_RESOLVEFAILED  (couldn't look up hostname)</li>
     * <li>3 -- REASON_CONNECTREFUSED (remote host refused connection)</li>
     * <li>4 -- REASON_EXITPOLICY     (OR refuses to connect to host or port)</li>
     * <li>5 -- REASON_DESTROY        (Circuit is being destroyed)</li>
     * <li>6 -- REASON_DONE           (Anonymized TCP connection was closed)</li>
     * <li>7 -- REASON_TIMEOUT        (Connection timed out, or OR timed out while connecting)</li>
     * <li>8 -- (unallocated)</li>
     * <li>9 -- REASON_HIBERNATING    (OR is temporarily hibernating)</li>
     * <li>10 -- REASON_INTERNAL       (Internal error at the OR)</li>
     * <li>11 -- REASON_RESOURCELIMIT  (OR has no resources to fulfill request)</li>
     * <li>12 -- REASON_CONNRESET      (Connection was unexpectedly reset)</li>
     * <li>13 -- REASON_TORPROTOCOL    (Sent when closing connection because of Tor protocol violations)</li>
     * </ul>
     * <p>
     * Tor may hold the stream open for a while to flush any data that is pending.
     */
    public void closeStream(String streamID, byte reason)
            throws IOException
    {
        sendAndWaitForResponse("CLOSESTREAM " + streamID + " " + reason + "\r\n", null);
    }

    /**
     * Tells Tor to close the circuit identified by <b>circID</b>.
     * If <b>ifUnused</b> is true, do not close the circuit unless it is unused.
     */
    public void closeCircuit(String circID, boolean ifUnused) throws IOException
    {
        sendAndWaitForResponse("CLOSECIRCUIT " + circID +
                (ifUnused ? " IFUNUSED" : "") + "\r\n", null);
    }

    /**
     * Tells Tor to exit when this control connection is closed. This command
     * was added in Tor 0.2.2.28-beta.
     */
    public void takeOwnership() throws IOException
    {
        sendAndWaitForResponse("TAKEOWNERSHIP\r\n", null);
    }

    /**
     * Tells Tor to generate and set up a new onion service using the best
     * supported algorithm.
     * <p/>
     * ADD_ONION was added in Tor 0.2.7.1-alpha.
     */
    public Map<String, String> addOnion(Map<Integer, String> portLines)
            throws IOException
    {
        return addOnion("NEW:BEST", portLines, null);
    }

    /**
     * Tells Tor to generate and set up a new onion service using the best
     * supported algorithm.
     * <p/>
     * ADD_ONION was added in Tor 0.2.7.1-alpha.
     */
    public Map<String, String> addOnion(Map<Integer, String> portLines,
                                        boolean ephemeral, boolean detach)
            throws IOException
    {
        return addOnion("NEW:BEST", portLines, ephemeral, detach);
    }

    /**
     * Tells Tor to set up an onion service using the provided private key.
     * <p/>
     * ADD_ONION was added in Tor 0.2.7.1-alpha.
     */
    public Map<String, String> addOnion(String privKey,
                                        Map<Integer, String> portLines)
            throws IOException
    {
        return addOnion(privKey, portLines, null);
    }

    /**
     * Tells Tor to set up an onion service using the provided private key.
     * <p/>
     * ADD_ONION was added in Tor 0.2.7.1-alpha.
     */
    public Map<String, String> addOnion(String privKey,
                                        Map<Integer, String> portLines,
                                        boolean ephemeral, boolean detach)
            throws IOException
    {
        List<String> flags = new ArrayList<String>();
        if (ephemeral)
            flags.add("DiscardPK");
        if (detach)
            flags.add("Detach");
        return addOnion(privKey, portLines, flags);
    }

    /**
     * Tells Tor to set up an onion service.
     * <p/>
     * ADD_ONION was added in Tor 0.2.7.1-alpha.
     */
    public Map<String, String> addOnion(String privKey,
                                        Map<Integer, String> portLines,
                                        List<String> flags)
            throws IOException
    {
        if (privKey.indexOf(':') < 0)
            throw new IllegalArgumentException("Invalid privKey");
        if (portLines == null || portLines.size() < 1)
            throw new IllegalArgumentException("Must provide at least one port line");
        StringBuilder b = new StringBuilder();
        b.append("ADD_ONION ").append(privKey);
        if (flags != null && flags.size() > 0)
        {
            b.append(" Flags=");
            String separator = "";
            for (String flag : flags)
            {
                b.append(separator).append(flag);
                separator = ",";
            }
        }
        for (Map.Entry<Integer, String> portLine : portLines.entrySet())
        {
            int virtPort = portLine.getKey();
            String target = portLine.getValue();
            b.append(" Port=").append(virtPort);
            if (target != null && target.length() > 0)
                b.append(",").append(target);
        }
        b.append("\r\n");
        List<ReplyLine> lst = sendAndWaitForResponse(b.toString(), null);
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(HS_ADDRESS, (lst.get(0)).msg.split("=", 2)[1]);
        if (lst.size() > 2)
            ret.put(HS_PRIVKEY, (lst.get(1)).msg.split("=", 2)[1]);
        return ret;
    }

    /**
     * Tells Tor to take down an onion service previously set up with
     * addOnion(). The hostname excludes the .onion extension.
     * <p/>
     * DEL_ONION was added in Tor 0.2.7.1-alpha.
     */
    public void delOnion(String hostname) throws IOException
    {
        sendAndWaitForResponse("DEL_ONION " + hostname + "\r\n", null);
    }

    /**
     * Tells Tor to forget any cached client state relating to the hidden
     * service with the given hostname (excluding the .onion extension).
     */
    public void forgetHiddenService(String hostname) throws IOException
    {
        sendAndWaitForResponse("HSFORGET " + hostname + "\r\n", null);
    }

    /**
     * Abstract interface whose methods are invoked when Tor sends us an event.
     *
     * @see TorControlConnection#setEventHandler
     * @see TorControlConnection#setEvents
     */
    public interface EventHandler
    {
        /**
         * Invoked when a circuit's status has changed.
         * Possible values for <b>status</b> are:
         * <ul>
         *   <li>"LAUNCHED" :  circuit ID assigned to new circuit</li>
         *   <li>"BUILT"    :  all hops finished, can now accept streams</li>
         *   <li>"EXTENDED" :  one more hop has been completed</li>
         *   <li>"FAILED"   :  circuit closed (was not built)</li>
         *   <li>"CLOSED"   :  circuit closed (was built)</li>
         * 	</ul>
         *
         * <b>circID</b> is the alphanumeric identifier of the affected circuit,
         * and <b>path</b> is a comma-separated list of alphanumeric ServerIDs.
         */
        void circuitStatus(String status, String circID, String path);

        /**
         * Invoked when a stream's status has changed.
         * Possible values for <b>status</b> are:
         * <ul>
         *   <li>"NEW"         :  New request to connect</li>
         *   <li>"NEWRESOLVE"  :  New request to resolve an address</li>
         *   <li>"SENTCONNECT" :  Sent a connect cell along a circuit</li>
         *   <li>"SENTRESOLVE" :  Sent a resolve cell along a circuit</li>
         *   <li>"SUCCEEDED"   :  Received a reply; stream established</li>
         *   <li>"FAILED"      :  Stream failed and not retriable.</li>
         *   <li>"CLOSED"      :  Stream closed</li>
         *   <li>"DETACHED"    :  Detached from circuit; still retriable.</li>
         * 	</ul>
         *
         * <b>streamID</b> is the alphanumeric identifier of the affected stream,
         * and its <b>target</b> is specified as address:port.
         */
        void streamStatus(String status, String streamID, String target);

        /**
         * Invoked when the status of a connection to an OR has changed.
         * Possible values for <b>status</b> are ["LAUNCHED" | "CONNECTED" | "FAILED" | "CLOSED"].
         * <b>orName</b> is the alphanumeric identifier of the OR affected.
         */
        void orConnStatus(String status, String orName);

        /**
         * Invoked once per second. <b>read</b> and <b>written</b> are
         * the number of bytes read and written, respectively, in
         * the last second.
         */
        void bandwidthUsed(long read, long written);

        /**
         * Invoked whenever Tor learns about new ORs.  The <b>orList</b> object
         * contains the alphanumeric ServerIDs associated with the new ORs.
         */
        void newDescriptors(java.util.List<String> orList);

        /**
         * Invoked when Tor logs a message.
         * <b>severity</b> is one of ["DEBUG" | "INFO" | "NOTICE" | "WARN" | "ERR"],
         * and <b>msg</b> is the message string.
         */
        void message(String severity, String msg);

        /**
         * Invoked when an unspecified message is received.
         * <type> is the message type, and <msg> is the message string.
         */
        void unrecognized(String type, String msg);
    }
}