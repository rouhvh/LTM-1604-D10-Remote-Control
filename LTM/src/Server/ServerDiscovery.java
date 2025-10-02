package Server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerDiscovery extends Thread {
    private static final int DISCOVERY_PORT = 12345;
    private static final String REQUEST_MESSAGE = "REMOTE_DESKTOP_DISCOVERY_REQUEST";
    private static final String RESPONSE_MESSAGE = "REMOTE_DESKTOP_DISCOVERY_RESPONSE";
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            System.out.println("Server: Discovery service started on port " + DISCOVERY_PORT);

            while (true) {
                byte[] receiveBuffer = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData()).trim();
                if (message.equals(REQUEST_MESSAGE)) {
                    System.out.println("Server: Received discovery request from " + receivePacket.getAddress().getHostAddress());
                    byte[] sendData = RESPONSE_MESSAGE.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(sendPacket);
                    System.out.println("Server: Sent discovery response to " + receivePacket.getAddress().getHostAddress());
                }
            }
        } catch (Exception e) {
            System.err.println("Server: Discovery service error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}