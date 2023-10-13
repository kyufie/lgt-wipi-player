package android.lgt.wipi;

import android.net.ConnectivityManager;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
/* loaded from: classes.dex */
public class WipiSocket {
    static final int ENOSPACE = -13;
    static final int ENOTCONN = -14;
    static final int ENOTSUP = -16;
    static final int ERROR = -1;
    static final int SOCK_DATAGRAM = 2;
    static final int SOCK_STREAM = 1;
    static final int SUCCESS = 0;
    int addressJustReceived;
    int portJustReceived;
    ServerSocket serverSocket;
    Socket tcpSocket;
    int type;
    DatagramSocket udpSocket;
    int usingNetworkFeatureResult = ERROR;

    public static String convertAddressToString(int addr) {
        int[] ip = {(addr >> 24) & 255, (addr >> 16) & 255, (addr >> 8) & 255, addr & 255};
        return new String(String.valueOf(ip[3]) + "." + ip[2] + "." + ip[1] + "." + ip[0]);
    }

    public static int convertAddressToInt(String addr) {
        int[] ip = new int[4];
        if (addr == null) {
            return ERROR;
        }
        int end = addr.indexOf(".", 0);
        ip[0] = Integer.parseInt(addr.substring(0, end));
        int start = end + 1;
        int end2 = addr.indexOf(".", start);
        ip[1] = Integer.parseInt(addr.substring(start, end2));
        int start2 = end2 + 1;
        int end3 = addr.indexOf(".", start2);
        ip[2] = Integer.parseInt(addr.substring(start2, end3));
        ip[3] = Integer.parseInt(addr.substring(end3 + 1));
        return (ip[3] << 24) + (ip[2] << 16) + (ip[1] << 8) + ip[0];
    }

    public WipiSocket(int type) {
        this.type = type;
    }

    public int connect(int addr, int port) {
        Log.d(WipiPlayer.TAG, "connect()");
        String address = convertAddressToString(addr);
        int port2 = ((port << 8) & 65280) | ((port >> 8) & 255);
        Log.v(WipiPlayer.TAG, "connecting to " + address + ":" + port2 + "...");
        if (this.type != 1) {
            return ENOTSUP;
        }
        ConnectivityManager cm = (ConnectivityManager) WipiPlayer.getContext().getSystemService("connectivity");
        String target = String.valueOf(address) + ":" + port2;
        boolean haveToUse3G = isBillGatewayIP(target.substring(0, target.indexOf(58)));
        if (haveToUse3G) {
            if (!HandsetManager.getInstance(WipiPlayer.getContext()).isSubscribed()) {
                Log.v(WipiPlayer.TAG, "NOT Subscribed...");
                Message msg = new Message();
                msg.arg1 = 5;
                ((WipiPlayer) WipiPlayer.getContext()).getHandler().sendMessage(msg);
                return ENOTCONN;
            } else if (HandsetManager.getInstance(WipiPlayer.getContext()).isDataNetworkLocked().equals("1")) {
                Log.v(WipiPlayer.TAG, "Mobile Network is BLOCKED by user...");
                Message msg2 = new Message();
                msg2.arg1 = 2;
                ((WipiPlayer) WipiPlayer.getContext()).getHandler().sendMessage(msg2);
                return ENOTCONN;
            } else if (!HandsetManager.getInstance(WipiPlayer.getContext()).getRoamingArea().equals("0")) {
                Log.v(WipiPlayer.TAG, "Here is ROAMING area, and bill socket cannot be connected...");
                Message msg3 = new Message();
                msg3.arg1 = 3;
                ((WipiPlayer) WipiPlayer.getContext()).getHandler().sendMessage(msg3);
                return ENOTCONN;
            } else if (HandsetManager.getInstance(WipiPlayer.getContext()).isAirplaneMode().equals("1")) {
                Log.v(WipiPlayer.TAG, "Bill socket cannot be connected in Airplane Mode...");
                Message msg4 = new Message();
                msg4.arg1 = 6;
                ((WipiPlayer) WipiPlayer.getContext()).getHandler().sendMessage(msg4);
                return ENOTCONN;
            }
        }
        if (cm.getNetworkInfo(1).isConnected()) {
            if (haveToUse3G) {
                Log.v(WipiPlayer.TAG, "connect with Mobile Network FORCELY...");
                if (addr == ERROR) {
                    return ERROR;
                }
                try {
                    int type_hipri = ConnectivityManager.class.getField("TYPE_MOBILE_HIPRI").getInt(null);
                    this.usingNetworkFeatureResult = cm.startUsingNetworkFeature(0, "enableHIPRI");
                    boolean routingSucceeded = false;
                    while (!routingSucceeded) {
                        if (cm.getNetworkInfo(type_hipri).isConnected()) {
                            routingSucceeded = cm.requestRouteToHost(type_hipri, addr);
                        } else {
                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IllegalAccessException e2) {
                    Log.e(WipiPlayer.TAG, "IllegalAccessException : " + e2.getMessage());
                } catch (IllegalArgumentException e3) {
                    Log.e(WipiPlayer.TAG, "IllegalArgumentException : " + e3.getMessage());
                } catch (NoSuchFieldException e4) {
                    Log.e(WipiPlayer.TAG, "NoSuchFieldException : " + e4.getMessage());
                } catch (SecurityException e5) {
                    Log.e(WipiPlayer.TAG, "SecurityException : " + e5.getMessage());
                }
            } else {
                Log.v(WipiPlayer.TAG, "connect with WiFi Network...");
            }
        } else if (cm.getNetworkInfo(0).isConnected()) {
            Log.v(WipiPlayer.TAG, "connect with Mobile Network...");
        } else {
            Log.v(WipiPlayer.TAG, "Mobile network is out of service...");
            Message msg5 = new Message();
            msg5.arg1 = 4;
            ((WipiPlayer) WipiPlayer.getContext()).getHandler().sendMessage(msg5);
            return ENOTCONN;
        }
        try {
            this.tcpSocket = new Socket(address, port2);
            this.tcpSocket.setSoTimeout(30000);
            Log.v(WipiPlayer.TAG, "connected to " + address + ":" + port2);
            return 0;
        } catch (UnknownHostException e6) {
            Log.e(WipiPlayer.TAG, "connect : UnknownHostException - " + e6.getMessage());
            return ERROR;
        } catch (IOException e7) {
            Log.e(WipiPlayer.TAG, "connect : IOException - " + e7.getMessage());
            return ENOTCONN;
        }
    }

    private boolean isBillGatewayIP(String ip) {
        HandsetManager hm = HandsetManager.getInstance(WipiPlayer.getContext());
        if (ip.equals(hm.getDns1()) || ip.equals(hm.getDns2())) {
            return false;
        }
        String gw = hm.getBillGateway();
        if (gw == null) {
            return false;
        }
        String gwdn = gw.substring(0, gw.indexOf(58));
        int gwaddr = Network.lookupAddressN(convertAddressToInt(hm.getDns1()), gwdn);
        String gwip = convertAddressToString(gwaddr);
        return ip.equalsIgnoreCase(gwip);
    }

    public int read(byte[] buf, int len) {
        Log.d(WipiPlayer.TAG, "read()");
        if (this.type != 1) {
            return ERROR;
        }
        try {
            InputStream is = this.tcpSocket.getInputStream();
            Log.v(WipiPlayer.TAG, "try to read " + len + " bytes ...");
            int read = is.read(buf, 0, len);
            if (read == ERROR) {
                read = 0;
            }
            Log.v(WipiPlayer.TAG, String.valueOf(read) + " bytes read");
            return read;
        } catch (IOException e) {
            Log.e(WipiPlayer.TAG, "read : IOException - " + e.getMessage());
            return ENOTCONN;
        }
    }

    public int write(byte[] buf, int len) {
        Log.d(WipiPlayer.TAG, "write()");
        if (this.type != 1) {
            return ERROR;
        }
        try {
            OutputStream os = this.tcpSocket.getOutputStream();
            os.flush();
            os.write(buf, 0, len);
            Log.v(WipiPlayer.TAG, String.valueOf(len) + " bytes written");
            os.flush();
            return len;
        } catch (IOException e) {
            Log.e(WipiPlayer.TAG, "write : IOException - " + e.getMessage());
            return ENOTCONN;
        }
    }

    public int bind(int addr, int port) {
        Log.d(WipiPlayer.TAG, "bind()");
        try {
            this.serverSocket = new ServerSocket();
            this.serverSocket.bind(new InetSocketAddress(convertAddressToString(addr), port));
            return 0;
        } catch (IOException e) {
            return ENOTCONN;
        }
    }

    public int accept() {
        Log.d(WipiPlayer.TAG, "accept()");
        try {
            this.tcpSocket = this.serverSocket.accept();
            return 0;
        } catch (IOException e) {
            return ENOTCONN;
        }
    }

    public int send(byte[] buf, int len, int addr, int port) {
        Log.d(WipiPlayer.TAG, "send()");
        String address = convertAddressToString(addr);
        try {
            this.udpSocket = new DatagramSocket(port, InetAddress.getByName(address));
            DatagramPacket packet = new DatagramPacket(buf, len);
            this.udpSocket.send(packet);
            return packet.getLength();
        } catch (SocketException e) {
            return ERROR;
        } catch (UnknownHostException e2) {
            return ERROR;
        } catch (IOException e3) {
            return ENOTCONN;
        }
    }

    public int receive(byte[] buf, int len) {
        Log.d(WipiPlayer.TAG, "receive()");
        try {
            this.udpSocket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(buf, len);
            this.udpSocket.receive(packet);
            this.addressJustReceived = convertAddressToInt(packet.getAddress().getHostAddress());
            this.portJustReceived = packet.getPort();
            return packet.getLength();
        } catch (SocketException e) {
            return ERROR;
        } catch (IOException e2) {
            return ENOTCONN;
        }
    }

    public int getAddressJustReceived() {
        return this.addressJustReceived;
    }

    public int getPortJustReceived() {
        return this.portJustReceived;
    }

    public int close() {
        Log.d(WipiPlayer.TAG, "close()");
        if (this.tcpSocket != null && !this.tcpSocket.isClosed()) {
            try {
                this.tcpSocket.close();
            } catch (IOException e) {
                Log.e(WipiPlayer.TAG, e.getMessage());
                return ENOTCONN;
            }
        }
        if (this.udpSocket != null && !this.udpSocket.isClosed()) {
            this.udpSocket.close();
        }
        if (this.usingNetworkFeatureResult != ERROR) {
            try {
                ConnectivityManager cm = (ConnectivityManager) WipiPlayer.getContext().getSystemService("connectivity");
                cm.stopUsingNetworkFeature(0, "enableHIPRI");
                this.usingNetworkFeatureResult = ERROR;
            } catch (IllegalArgumentException e2) {
                Log.e(WipiPlayer.TAG, "IllegalArgumentException : " + e2.getMessage());
            } catch (SecurityException e3) {
                Log.e(WipiPlayer.TAG, "SecurityException : " + e3.getMessage());
            }
        }
        return 0;
    }
}
