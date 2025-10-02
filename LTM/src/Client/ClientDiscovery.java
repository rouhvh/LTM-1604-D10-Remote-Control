package Client;

import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ClientDiscovery {

    private static final int DISCOVERY_PORT = 12345;
    private static final String REQUEST_MESSAGE = "REMOTE_DESKTOP_DISCOVERY_REQUEST";
    private static final String RESPONSE_MESSAGE = "REMOTE_DESKTOP_DISCOVERY_RESPONSE";

    public static List<String> findServers() {
        List<String> foundServers = new ArrayList<>();
        // Logic tìm kiếm bằng UDP broadcast (không hiển thị dialog ở đây)
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] sendData = REQUEST_MESSAGE.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
            socket.send(sendPacket);

            // Đặt thời gian chờ phản hồi (ví dụ: 3 giây)
            socket.setSoTimeout(3000); 

            // Vòng lặp để nhận tất cả các phản hồi trong thời gian chờ
            while (true) {
                try {
                    byte[] receiveBuffer = new byte[15000];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);

                    String message = new String(receivePacket.getData()).trim();
                    if (message.equals(RESPONSE_MESSAGE)) {
                        String serverIp = receivePacket.getAddress().getHostAddress();
                        // Thêm vào danh sách nếu chưa có
                        if (!foundServers.contains(serverIp)) {
                            foundServers.add(serverIp);
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // Hết thời gian chờ, thoát khỏi vòng lặp
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Client Discovery Error: " + e.getMessage());
            // Không cần in stack trace đầy đủ trừ khi đang gỡ lỗi
        }
        return foundServers;
    }
}