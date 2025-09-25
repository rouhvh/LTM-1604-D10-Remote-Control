package Client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Thêm các import cần thiết
import Server.RemoteControlServer;
import Server.RemoteControlServer.ClientConnectionListener;
import Server.ServerDiscovery;
import Server.ServerSessionUI;
import Shared.IClientCallback;
import Shared.IRemoteControl;

/**
 * Lớp giao diện chính của ứng dụng.
 * Triển khai ClientConnectionListener để nhận sự kiện từ backend Server.
 */
public class MainUI extends JFrame implements ClientConnectionListener {

    private ServerSessionUI sessionUI; // Thêm tham chiếu đến cửa sổ quản lý phiên của Server

    public MainUI() {
        setTitle("Remote Control RMI");
        setSize(650, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setEnabled(false);

        // --- Panel bên trái: Cho phép điều khiển (Server) ---
        JPanel allowPanel = new JPanel(new GridBagLayout());
        allowPanel.setBorder(new TitledBorder("Allow Remote Control"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        allowPanel.add(new JLabel("Your ID (Your IP):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField yourIdField = new JTextField();
        yourIdField.setEditable(false);
        yourIdField.setFont(new Font("Arial", Font.BOLD, 14));
        try {
            yourIdField.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) { yourIdField.setText("Error getting IP"); }
        allowPanel.add(yourIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        allowPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField yourPasswordField = new JTextField();
        yourPasswordField.setEditable(false);
        yourPasswordField.setFont(new Font("Arial", Font.BOLD, 14));
        yourPasswordField.setText(String.format("%06d", (int) (Math.random() * 1000000)));
        allowPanel.add(yourPasswordField, gbc);

        splitPane.setLeftComponent(allowPanel);

        // --- Panel bên phải: Điều khiển máy khác (Client) ---
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(new TitledBorder("Control a Remote Computer"));
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        controlPanel.add(new JLabel("Partner ID (IP):"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField partnerIdField = new JTextField();
        controlPanel.add(partnerIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        controlPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField partnerPasswordField = new JPasswordField();
        controlPanel.add(partnerPasswordField, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.LINE_END;
        JButton connectButton = new JButton("Connect to partner");
        controlPanel.add(connectButton, gbc);

        splitPane.setRightComponent(controlPanel);

        JLabel statusBar = new JLabel(" Ready to connect.");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
        // --- XỬ LÝ SỰ KIỆN KẾT NỐI ---
        connectButton.addActionListener(e -> {
            String partnerIp = partnerIdField.getText().trim();
            if (partnerIp.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Partner ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Tạo và hiển thị hộp thoại chờ
            JDialog connectingDialog = new JDialog(this, "Connecting", true);
            connectingDialog.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
            connectingDialog.add(new JLabel("Connecting to " + partnerIp + "..."));
            connectingDialog.setSize(300, 100);
            connectingDialog.setLocationRelativeTo(this);
            connectingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            // Chạy tác vụ kết nối trong một luồng mới để không làm treo giao diện
            new Thread(() -> {
                try {
                    connectToServer(partnerIp);
                } finally {
                    SwingUtilities.invokeLater(connectingDialog::dispose);
                }
            }).start();

            connectingDialog.setVisible(true);
        });
    }

    private void connectToServer(String serverIp) {
        try {
            String clientIp = InetAddress.getLocalHost().getHostAddress();
            Registry registry = LocateRegistry.getRegistry(serverIp, 1099);
            IRemoteControl remoteControl = (IRemoteControl) registry.lookup("RemoteControlService");
            
            SwingUtilities.invokeLater(() -> {
                this.dispose(); // Đóng cửa sổ MainUI
                try {
                    // Mở cửa sổ điều khiển
                    RemoteControlClient client = new RemoteControlClient(remoteControl, clientIp, serverIp);
                    remoteControl.registerClientCallback(client);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Cannot connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    // --- TRIỂN KHAI CÁC PHƯƠNG THỨC CỦA LISTENER ---
    @Override
    public void onClientConnected(IClientCallback clientCallback, String clientIp, IRemoteControl serverControl) {
        // Khi có client kết nối, mở cửa sổ ServerSessionUI trên luồng giao diện
        SwingUtilities.invokeLater(() -> {
            if (sessionUI != null && sessionUI.isVisible()) {
                sessionUI.dispose(); // Đóng phiên cũ nếu đang tồn tại
            }
            sessionUI = new ServerSessionUI(clientCallback, serverControl, clientIp);
        });
    }

    @Override
    public void onMessageReceived(String message) {
        // Đẩy tin nhắn nhận được vào cửa sổ quản lý phiên
        if (sessionUI != null) {
            sessionUI.displayMessage(message);
        }
    }

    /**
     * Điểm khởi động chính của toàn bộ ứng dụng.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI mainUI = new MainUI(); // Tạo đối tượng UI
            mainUI.setVisible(true);

            // Chạy Server RMI trong một luồng nền
            new Thread(() -> {
                try {
                    RemoteControlServer server = new RemoteControlServer();
                    server.setConnectionListener(mainUI); // ĐĂNG KÝ LISTENER ĐỂ GIAO TIẾP

                    LocateRegistry.createRegistry(1099);
                    Registry registry = LocateRegistry.getRegistry();
                    registry.rebind("RemoteControlService", server);
                    System.out.println("RMI Server part is running in the background...");
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(null, "Could not start RMI Server.", "Server Error", JOptionPane.ERROR_MESSAGE)
                    );
                }
            }).start();

            // Chạy dịch vụ dò tìm IP trong một luồng nền khác
            new ServerDiscovery().start();
        });
    }
}