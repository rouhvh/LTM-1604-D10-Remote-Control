package Client;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.*;
import Shared.IClientCallback;
import Shared.IRemoteControl;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public class RemoteControlClient extends UnicastRemoteObject implements IClientCallback {

    private JFrame frame;
    private JLabel screenLabel;
    private IRemoteControl remoteControl;

    // --- THAY ĐỔI 1: Bỏ hằng số SCREEN_SCALE và thêm biến lưu kích thước gốc ---
    // private static final double SCREEN_SCALE = 0.8; // BỎ ĐI
    private int originalScreenWidth;
    private int originalScreenHeight;
    private boolean isOriginalSizeReceived = false;

    protected RemoteControlClient(IRemoteControl remoteControl) throws RemoteException {
        super();
        this.remoteControl = remoteControl;
        
        frame = new JFrame("Remote Desktop - Client View");
        screenLabel = new JLabel();
        JScrollPane scrollPane = new JScrollPane(screenLabel);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
        
        frame.setFocusable(true);
        frame.requestFocusInWindow();

        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent("MOUSE_MOVE", e);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                 sendMouseEvent("MOUSE_MOVE", e);
            }
        });

        screenLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent("MOUSE_PRESS", e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent("MOUSE_RELEASE", e);
            }
        });

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent("KEY_PRESS", e);
            }
            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent("KEY_RELEASE", e);
            }
        });
    }
    
    // --- THAY ĐỔI 2: Cập nhật hàm sendMouseEvent để tính toán động ---
    private void sendMouseEvent(String eventType, MouseEvent e) {
        // Chỉ gửi sự kiện nếu đã nhận được ảnh (và kích thước gốc)
        if (!isOriginalSizeReceived || screenLabel.getIcon() == null) {
            return;
        }

        try {
            // Lấy kích thước ảnh đang hiển thị trên label
            int scaledWidth = screenLabel.getIcon().getIconWidth();
            int scaledHeight = screenLabel.getIcon().getIconHeight();

            // Tính toán tỷ lệ chuyển đổi X và Y
            double scaleX = (double) originalScreenWidth / scaledWidth;
            double scaleY = (double) originalScreenHeight / scaledHeight;

            // Chuyển đổi tọa độ từ client -> server
            int serverX = (int) (e.getX() * scaleX);
            int serverY = (int) (e.getY() * scaleY);
            
            System.out.println("Client: Sending " + eventType + " to server coords (" + serverX + ", " + serverY + ")");

            if (eventType.equals("MOUSE_MOVE")) {
                remoteControl.mouseMove(serverX, serverY);
            } else if (eventType.equals("MOUSE_PRESS")) {
                remoteControl.mousePress(InputEvent.getMaskForButton(e.getButton()));
            } else if (eventType.equals("MOUSE_RELEASE")) {
                remoteControl.mouseRelease(InputEvent.getMaskForButton(e.getButton()));
            }
        } catch (RemoteException ex) {
            System.err.println("Client: Error sending mouse event to server.");
            ex.printStackTrace();
        }
    }
    
    private void sendKeyEvent(String eventType, KeyEvent e) {
        try {
            System.out.println("Client: Sending " + eventType);
            if (eventType.equals("KEY_PRESS")) {
                remoteControl.keyPress(e.getKeyCode());
            } else if (eventType.equals("KEY_RELEASE")) {
                remoteControl.keyRelease(e.getKeyCode());
            }
        } catch (RemoteException ex) {
            System.err.println("Client: Error sending key event to server.");
            ex.printStackTrace();
        }
    }

    // --- THAY ĐỔI 3: Cập nhật hàm receiveScreen để lấy kích thước gốc ---
    @Override
    public void receiveScreen(byte[] screenData) throws RemoteException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(screenData);
            BufferedImage image = ImageIO.read(bais);
            
            if (image != null) {
                // Lấy và lưu kích thước gốc của màn hình server (chỉ lần đầu tiên)
                if (!isOriginalSizeReceived) {
                    originalScreenWidth = image.getWidth();
                    originalScreenHeight = image.getHeight();
                    isOriginalSizeReceived = true;
                    System.out.println("Client: Received original server screen size: " + originalScreenWidth + "x" + originalScreenHeight);
                }

                // Lấy kích thước của khung chứa (viewport của JScrollPane)
                int viewWidth = frame.getContentPane().getWidth();
                int viewHeight = frame.getContentPane().getHeight();

                // Tính toán kích thước mới của ảnh để vừa với khung chứa mà vẫn giữ tỷ lệ
                double scale = Math.min((double) viewWidth / originalScreenWidth, (double) viewHeight / originalScreenHeight);
                int scaledWidth = (int) (originalScreenWidth * scale);
                int scaledHeight = (int) (originalScreenHeight * scale);

                Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                
                screenLabel.setIcon(new ImageIcon(scaledImage));
                frame.revalidate();
                frame.repaint();
            }
        } catch (Exception e) {
            System.err.println("Client: Error decoding received screen data.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            String serverIp = JOptionPane.showInputDialog("Enter Server IP Address:", "localhost");
            if (serverIp == null || serverIp.trim().isEmpty()) {
                System.out.println("Client: No IP address entered. Exiting.");
                return;
            }
            
            Registry registry = LocateRegistry.getRegistry(serverIp, 1099);
            IRemoteControl remoteControl = (IRemoteControl) registry.lookup("RemoteControlService");
            RemoteControlClient client = new RemoteControlClient(remoteControl);
            remoteControl.registerClientCallback(client);
            
            System.out.println("Client: Connected to server and callback registered successfully.");

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Cannot connect to server RMI service.\n" +
                    "Details: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}