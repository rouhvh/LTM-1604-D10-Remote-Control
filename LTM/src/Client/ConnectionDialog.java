package Client;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.util.List;

@SuppressWarnings("serial")
public class ConnectionDialog extends JDialog {

    private String serverIp = null;
    private JComboBox<String> serverListComboBox;
    private JTextField serverIpField;

    public ConnectionDialog(JFrame parent) {
        super(parent, "Connect to Server", true);
        setSize(450, 220);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Hiển thị IP của Client
        String clientIp = "Unknown";
        try {
            clientIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
             System.err.println("Could not get client IP address.");
        }
        infoPanel.add(new JLabel("Your IP Address (Client): " + clientIp));

        // ComboBox và nút dò tìm server
        JPanel discoveryPanel = new JPanel(new BorderLayout(5, 0));
        serverListComboBox = new JComboBox<>();
        discoveryPanel.add(serverListComboBox, BorderLayout.CENTER);
        
        JButton findButton = new JButton("Find Again");
        discoveryPanel.add(findButton, BorderLayout.EAST);
        infoPanel.add(discoveryPanel);

        // Trường nhập IP server
        JPanel inputPanel = new JPanel(new BorderLayout(5,0));
        inputPanel.add(new JLabel("Enter Server IP:"), BorderLayout.WEST);
        serverIpField = new JTextField("localhost");
        inputPanel.add(serverIpField, BorderLayout.CENTER);
        infoPanel.add(inputPanel);
        
        add(infoPanel, BorderLayout.CENTER);

        // Panel chứa các nút bấm
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- XỬ LÝ SỰ KIỆN ---

        // Dò tìm server ngay khi cửa sổ mở ra
        findServersInBackground();

        findButton.addActionListener(e -> findServersInBackground());

        serverListComboBox.addActionListener(e -> {
            Object selected = serverListComboBox.getSelectedItem();
            // Đảm bảo không lấy giá trị của các thông báo như "Searching..."
            if (selected != null && !selected.toString().contains("...") && !selected.toString().contains("found") && !selected.toString().contains("error")) {
                serverIpField.setText(selected.toString());
            }
        });

        connectButton.addActionListener(e -> {
            serverIp = serverIpField.getText().trim();
            if (!serverIp.isEmpty()) {
                dispose(); // Đóng cửa sổ
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a Server IP.", "Input Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            serverIp = null;
            dispose(); // Đóng cửa sổ
        });
    }

    private void findServersInBackground() {
        serverListComboBox.removeAllItems();
        serverListComboBox.addItem("Searching...");
        serverListComboBox.setEnabled(false);

        // Sử dụng SwingWorker để không làm treo giao diện
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return ClientDiscovery.findServers(); // Chạy tác vụ tìm kiếm
            }

            @Override
            protected void done() {
                try {
                    List<String> foundIps = get();
                    serverListComboBox.removeAllItems();
                    if (foundIps.isEmpty()) {
                        serverListComboBox.addItem("No servers found");
                    } else {
                        foundIps.forEach(serverListComboBox::addItem);
                        // Tự động điền IP đầu tiên vào ô nhập liệu
                        serverIpField.setText(foundIps.get(0));
                    }
                } catch (Exception e) {
                    serverListComboBox.addItem("Search error");
                    e.printStackTrace();
                } finally {
                    serverListComboBox.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    // Phương thức để lớp bên ngoài lấy IP đã được chọn
    public String getServerIp() {
        return serverIp;
    }
}