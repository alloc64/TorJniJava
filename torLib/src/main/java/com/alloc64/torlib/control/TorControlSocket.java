package com.alloc64.torlib.control;

import com.alloc64.jni.TLJNIBridge;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * See https://gitweb.torproject.org/torspec.git/tree/control-spec.txt
 */
public class TorControlSocket implements Runnable
{
    public enum ResponseCode
    {
        OK(250),
        OperationWasUnnecessary(251), // [Tor has declined to perform the operation, but no harm was done.]
        ResourceExhausted(451),
        SyntaxErrorProtocol(500),
        UnrecognizedCommand(510),
        UnimplementedCommand(511),
        SyntaxErrorInCommandArgument(512),
        UnrecognizedCommandArgument(513),
        AuthenticationRequired(514),
        BadAuthentication(515),
        UnspecifiedTorError(550),
        InternalError(551), // [Something went wrong inside Tor, so that the client's request couldn't be fulfilled.]
        UnrecognizedEntity(552), // [A configuration key, a stream ID, circuit ID, event, mentioned in the command did not actually exist.]
        InvalidConfigurationValue(553),
        InvalidDescriptor(554),
        UnmanagedEntity(555),
        AsynchronousEventNotification(650);

        private int val;

        ResponseCode(int val)
        {
            this.val = val;
        }

        public int getValue()
        {
            return val;
        }
    }

    public enum Signal
    {
        RELOAD("RELOAD"), // Reload: reload config items.
        SHUTDOWN("SHUTDOWN"), // Controlled shutdown: if server is an OP, exit immediately. If it's an OR, close listeners and exit after ShutdownWaitLength seconds.
        DUMP("DUMP"), // Dump stats: log information about open connections and circuits.
        DEBUG("DEBUG"), // Debug: switch all open logs to loglevel debug.
        HALT("HALT"), // Immediate shutdown: clean up and exit now.
        CLEARDNSCACHE("CLEARDNSCACHE"), // Forget the client-side cached IPs for all hostnames.
        NEWNYM("NEWNYM"), // Switch to clean circuits, so new application requests don't share any circuits with old ones.  Also clears the client-side DNS cache.  (Tor MAY rate-limit its response to this signal.)
        HEARTBEAT("HEARTBEAT"), // Make Tor dump an unscheduled Heartbeat message to log.
        DORMANT("DORMANT"), // Tell Tor to become "dormant".  A dormant Tor will try to avoid CPU and network usage until it receives user-initiated network request.  (Don't use this on relays or hidden services yet!)
        ACTIVE(""); // Tell Tor to stop being "dormant", as if it had received a user-initiated network request.

        private String val;

        Signal(String val)
        {
            this.val = val;
        }

        public String getValue()
        {
            return val;
        }
    }

    public abstract static class TorEventHandler
    {
        /**
         * Dispatched on connect thread
         * @param socket
         */
        public void onConnectedAsync(TorControlSocket socket)
        {

        }

        /**
         * Dispatched on main thread
         * @param socket
         */
        public void onConnected(TorControlSocket socket)
        {

        }

        public void onException(TorControlSocket socket, Exception e)
        {

        }
    }

    public interface Callback
    {
        void onResult(TorControlSocket socket, Reply reply);
    }

    public interface InfoCallback
    {
        void onResult(TorControlSocket socket, Map<String, String> result);
    }

    public static class Reply
    {
        private int status;
        private String message;
        private String rest;

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public String getRest()
        {
            return rest;
        }

        public void setRest(String rest)
        {
            this.rest = rest;
        }

        public void setStatus(String val)
        {
            try
            {
                this.setStatus(Integer.parseInt(val));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private static final int CONNECTION_RETRY_COUNT = 20;

    private final PasswordDigest password;
    private final TorEventHandler eventHandler;
    private InetSocketAddress socketAddress;

    private Socket socket;

    private BufferedReader inputStream;
    private OutputStreamWriter outputStream;

    private Executor asyncSendExecutor = Executors.newSingleThreadExecutor();

    public TorControlSocket(PasswordDigest password, TorEventHandler eventHandler)
    {
        this.password = password;
        this.eventHandler = eventHandler;
    }

    public void connect(InetSocketAddress socketAddress)
    {
        this.socketAddress = socketAddress;

        Executors.newSingleThreadExecutor().execute(this);
    }

    @Override
    public void run()
    {
        try
        {
            Thread.sleep(1000); // sleep in case TOR is not ready

            boolean alreadyConnected = false;

            for (int i = 0; i < CONNECTION_RETRY_COUNT && !alreadyConnected; i++)
            {
                try
                {
                    this.socket = new Socket();

                    if (socketAddress.isUnresolved())
                        this.socketAddress = new InetSocketAddress(socketAddress.getHostName(), socketAddress.getPort());

                    socket.connect(socketAddress);
                    alreadyConnected = true;

                    this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    this.outputStream = new OutputStreamWriter(socket.getOutputStream());

                    Reply authenticationReply = authenticate(password);

                    if(authenticationReply == null || authenticationReply.getStatus() != ResponseCode.OK.getValue())
                        throw new IllegalStateException("Authentication failed");

                    eventHandler.onConnectedAsync(this);

                    TLJNIBridge.get()
                            .getMainThreadDispatcher()
                            .dispatch(() -> eventHandler.onConnected(this));
                }
                catch (IOException e)
                {
                    if (alreadyConnected)
                        throw e;

                    onException(e);
                    Thread.sleep(i * 1000);
                }
            }
        }
        catch (Exception e)
        {
            onException(e);
        }
        finally
        {
            try
            {
                close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private Reply read() throws IOException
    {
        String line = inputStream.readLine();

        if (StringUtils.isEmpty(line))
            throw new IOException("Connection closed.");

        return read(line);
    }

    private Reply read(String line) throws IOException
    {
        if (line.length() < 4)
            throw new IllegalStateException("Line (\"" + line + "\") too short");

        Reply reply = new Reply();

        reply.setStatus(line.substring(0, 3));

        char c = line.charAt(3);
        reply.setMessage(line.substring(4));
        String rest = null;

        if (c == '+')
        {
            StringBuilder data = new StringBuilder();

            while (true)
            {
                line = inputStream.readLine();

                if (line.equals("."))
                    break;
                else if (line.startsWith("."))
                    line = line.substring(1);
                data.append(line).append('\n');
            }

            rest = data.toString();
        }

        reply.setRest(rest);

        int status = reply.getStatus();

        if (status == ResponseCode.AuthenticationRequired.getValue() || status == ResponseCode.BadAuthentication.getValue())
            throw new IllegalStateException(reply.getMessage());

        return reply;
    }

    public void close() throws IOException
    {
        if (socket != null)
            socket.close();
    }

    private void onException(Exception e)
    {
        eventHandler.onException(this, e);
    }

    public Reply send(String command)
    {
        return send(command, null);
    }

    public Reply send(String command, String params)
    {
        if (outputStream == null)
            return null;

        try
        {
            outputStream.write(command);

            if (!StringUtils.isEmpty(params))
            {
                StringTokenizer st = new StringTokenizer(params, "\n");
                while (st.hasMoreTokens())
                {
                    String line = st.nextToken();

                    if (line.startsWith("."))
                        line = "." + line;
                    if (line.endsWith("\r"))
                        line += "\n";
                    else
                        line += "\r\n";

                    outputStream.write(line);
                }

                outputStream.write(".\r\n");
            }

            outputStream.flush();

            return read();
        }
        catch (Exception e)
        {
            onException(e);
        }
        return null;
    }

    public void sendAsync(String command, String params, Callback callback)
    {
        asyncSendExecutor.execute(() ->
        {
            try
            {
                Reply reply = send(command, params);

                TLJNIBridge.get().getMainThreadDispatcher().dispatch(() ->
                {
                    if(reply != null && callback != null)
                        callback.onResult(TorControlSocket.this, reply);
                });
            }
            catch (Exception e)
            {
                onException(e);
            }
        });
    }

    public void sendAsync(String command, Callback callback)
    {
        sendAsync(command, null, callback);
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
    private Reply authenticate(PasswordDigest password)
    {
        return send(String.format("AUTHENTICATE %s\r\n", password.getSecretHex()), null);
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
    public Reply signal(Signal signal)
    {
        return send(String.format("SIGNAL %s\r\n", signal));
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
     * @return
     */
    public Map<String, String> getInfo(List<String> keys)
    {
        StringBuilder sb = new StringBuilder("GETINFO");
        for (String key : keys)
            sb.append(" ").append(key);

        sb.append("\r\n");

        Reply reply = send(sb.toString());

        String[] lines = reply.getMessage().split("\n");

        Map<String, String> m = new LinkedHashMap<>();

        for (String msg : lines)
        {
            int idx = msg.indexOf('=');
            if (idx < 0)
                break;
            String k = msg.substring(0, idx);

            String v;

            if (reply.getRest() != null)
                v = reply.getRest();
            else
                v = reply.getRest().substring(idx + 1);

            m.put(k, v);
        }

        return m;
    }

    public String getInfo(String key)
    {
        Map<String, String> m = getInfo(Collections.singletonList(key));

        return m.get(key);
    }
}
