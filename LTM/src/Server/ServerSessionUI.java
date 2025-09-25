package Server;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import Client.ChatWindow; // Tái sử dụng ChatWindow từ package client
import Shared.IClientCallback;
import Shared.IRemoteControl;

public class ServerSessionUI extends JFrame {
    private IClientCallback clientCallback;
    private ChatWindow chatWindow;

    public ServerSessionUI(IClientCallback clientCallback, IRemoteControl serverControl, String clientIp) {
        this.clientCallback = clientCallback;

        setTitle("Managing Connection from " + clientIp);
        setSize(500, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Chỉ đóng cửa sổ này, không thoát ứng dụng
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Tạo cửa sổ chat cho Server, sử dụng lại lớp ChatWindow
        this.chatWindow = new ChatWindow(this, serverControl, "You (Server)");

        // Panel chính
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel infoLabel = new JLabel("Actively connected to client at: " + clientIp);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton chatButton = new JButton("Open Chat");
        chatButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton disconnectButton = new JButton("Disconnect Client");
        disconnectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        disconnectButton.setForeground(Color.RED);

        mainPanel.add(infoLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Khoảng cách
        mainPanel.add(chatButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(disconnectButton);

        add(mainPanel, BorderLayout.CENTER);

        // --- XỬ LÝ SỰ KIỆN ---
        chatButton.addActionListener(e -> chatWindow.setVisible(true));

        disconnectButton.addActionListener(e -> {
            try {
                // Gọi hàm RMI để ra lệnh cho Client ngắt kết nối
                clientCallback.forceDisconnect("Session terminated by server operator.");
                // Đóng cửa sổ quản lý này
                this.dispose();
            } catch (RemoteException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to disconnect client.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    // Phương thức để server đẩy tin nhắn vào cửa sổ chat
    public void displayMessage(String message) {
        // Tự động mở cửa sổ chat nếu có tin nhắn đến
        if (!chatWindow.isVisible()) {
            chatWindow.setVisible(true);
        }
        chatWindow.displayMessage(message);
    }
}