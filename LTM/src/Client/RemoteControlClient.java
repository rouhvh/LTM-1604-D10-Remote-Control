package Client;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import Shared.IClientCallback;
import Shared.IRemoteControl;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JFileChooser;

/**
 * Lớp Client tạo ra cửa sổ điều khiển chính.
 * Phiên bản này đã được tối ưu và hoàn thiện.
 */
public class RemoteControlClient extends UnicastRemoteObject implements IClientCallback {

    private final JFrame frame;
    private final JLabel screenLabel;
    private final IRemoteControl remoteControl;
    private final ChatWindow chatWindow;

    private int originalScreenWidth, originalScreenHeight;
    private boolean isOriginalSizeReceived = false;
    private volatile long lastUpdateTime;
    private boolean isStreamPaused = false;

    public RemoteControlClient(IRemoteControl remoteControl, String clientIp, String serverIp) throws RemoteException {
        super();
        this.remoteControl = remoteControl;
        
        // --- KHỞI TẠO CÁC THÀNH PHẦN UI ---
        this.frame = new JFrame();
        this.screenLabel = new JLabel();
        this.chatWindow = new ChatWindow(frame, remoteControl, "Client (" + clientIp + ")");
        
        setupUI(clientIp, serverIp);
        setupListeners();
        
        startWatchdog();
    }

    private void setupUI(String clientIp, String serverIp) {
        frame.setTitle("Controlling " + serverIp + " | Client IP: " + clientIp);
        
        screenLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane(screenLabel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        
        frame.add(scrollPane, BorderLayout.CENTER);

        // --- CẬP NHẬT THANH CÔNG CỤ (TOOLBAR) ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton pauseResumeButton = new JButton("Pause");
        JButton screenshotButton = new JButton("Screenshot");
        JButton chatButton = new JButton("Chat");
        JButton sendFileButton = new JButton("Send File");
        JButton disconnectButton = new JButton("Disconnect");
        
        toolBar.add(pauseResumeButton);
        toolBar.add(screenshotButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(chatButton);
        toolBar.add(sendFileButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(disconnectButton);
        
        frame.add(toolBar, BorderLayout.NORTH);

        // --- XỬ LÝ SỰ KIỆN CHO CÁC NÚT ---
        chatButton.addActionListener(e -> chatWindow.setVisible(true));
        sendFileButton.addActionListener(e -> sendFileToServer());

        pauseResumeButton.addActionListener(e -> {
            try {
                isStreamPaused = !isStreamPaused; // Đảo trạng thái
                remoteControl.toggleScreenStream(isStreamPaused);
                pauseResumeButton.setText(isStreamPaused ? "Resume" : "Pause");
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        });
        
        screenshotButton.addActionListener(e -> takeScreenshot());

        disconnectButton.addActionListener(e -> System.exit(0));
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    private void setupListeners() {
        frame.setFocusable(true);
        frame.requestFocusInWindow();

        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { sendMouseEvent("MOUSE_MOVE", e); }
            @Override public void mouseDragged(MouseEvent e) { sendMouseEvent("MOUSE_MOVE", e); }
        });
        screenLabel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { sendMouseEvent("MOUSE_PRESS", e); }
            @Override public void mouseReleased(MouseEvent e) { sendMouseEvent("MOUSE_RELEASE", e); }
        });
        frame.addMouseWheelListener(e -> {
            try {
                remoteControl.mouseWheel(e.getWheelRotation());
            } catch (RemoteException ex) {
                System.err.println("Client: Error sending mouse wheel event.");
            }
        });

        // 🔑 Bắt phím toàn cục bằng KeyEventDispatcher
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!frame.isVisible()) return false;
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    sendKeyEvent("KEY_PRESS", e);
                } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                    sendKeyEvent("KEY_RELEASE", e);
                }
                return false;
            }
        });
    }

    private void startWatchdog() {
        lastUpdateTime = System.currentTimeMillis();
        Thread watchdogThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(200);
                    if (System.currentTimeMillis() - lastUpdateTime > 3000) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, "Connection to the server has been lost.", "Connection Lost", JOptionPane.ERROR_MESSAGE);
                            System.exit(0);
                        });
                        break;
                    }
                } catch (InterruptedException e) { break; }
            }
        });
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }
    private void takeScreenshot() {
        try {
            boolean wasPaused = isStreamPaused;
            // Tạm dừng luồng nếu nó đang chạy để đảm bảo ảnh chụp sắc nét
            if (!wasPaused) {
                remoteControl.toggleScreenStream(true);
            }

            // Gọi RMI để lấy ảnh chất lượng cao (PNG)
            byte[] pngData = remoteControl.takeScreenshot();

            // Tiếp tục luồng nếu trước đó nó đang chạy
            if (!wasPaused) {
                remoteControl.toggleScreenStream(false);
            }

            // Hỏi người dùng nơi lưu file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Screenshot");
            fileChooser.setSelectedFile(new File("screenshot-" + System.currentTimeMillis() + ".png"));
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                Files.write(fileToSave.toPath(), pngData);
                JOptionPane.showMessageDialog(frame, "Screenshot saved to:\n" + fileToSave.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to take screenshot.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // --- PHẦN NHẬN DỮ LIỆU TỪ SERVER (CALLBACKS) ---
    
    @Override
    public void receiveScreen(byte[] screenData) throws RemoteException {
        this.lastUpdateTime = System.currentTimeMillis();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenData));
            if (image != null) {
                if (!isOriginalSizeReceived) {
                    originalScreenWidth = image.getWidth();
                    originalScreenHeight = image.getHeight();
                    isOriginalSizeReceived = true;
                }
                int viewWidth = frame.getContentPane().getWidth();
                int viewHeight = frame.getContentPane().getHeight();
                double scale = Math.min((double) viewWidth / originalScreenWidth, (double) viewHeight / originalScreenHeight);
                int scaledWidth = (int) (originalScreenWidth * scale);
                int scaledHeight = (int) (originalScreenHeight * scale);
                Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                screenLabel.setIcon(new ImageIcon(scaledImage));
                frame.revalidate();
                frame.repaint();
            }
        } catch (Exception e) { System.err.println("Client: Error decoding received screen data."); }
    }
    
    @Override
    public void receiveMessage(String message) throws RemoteException {
        if (!chatWindow.isVisible()) {
            chatWindow.setVisible(true);
        }
        chatWindow.displayMessage(message);
    }

    @Override
    public void receiveFile(String filename, byte[] data) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(frame, "The other user wants to send you a file: " + filename + "\nDo you want to save it?", "File Received", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(filename));
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try {
                        Files.write(fileChooser.getSelectedFile().toPath(), data);
                        JOptionPane.showMessageDialog(frame, "File saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            // 🔑 Trả lại focus
            frame.toFront();
            frame.requestFocus();
            frame.getRootPane().requestFocusInWindow();
        });
    }

    @Override
    public void forceDisconnect(String reason) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "Disconnected by server.\nReason: " + reason, "Session Terminated", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        });
    }
    
    // --- PHẦN GỬI DỮ LIỆU ĐẾN SERVER ---
    
    private void sendFileToServer() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                remoteControl.sendFile(file.getName(), Files.readAllBytes(file.toPath()));
                JOptionPane.showMessageDialog(frame, "File sent successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error sending file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        // 🔑 Trả lại focus sau dialog
        SwingUtilities.invokeLater(() -> {
            frame.toFront();
            frame.requestFocus();
            frame.getRootPane().requestFocusInWindow();
            screenLabel.requestFocusInWindow();
        });
    }

    private void sendMouseEvent(String eventType, MouseEvent e) {
        if (!isOriginalSizeReceived || screenLabel.getIcon() == null) { return; }
        try {
            int labelWidth = screenLabel.getWidth();
            int labelHeight = screenLabel.getHeight();
            int imageWidth = screenLabel.getIcon().getIconWidth();
            int imageHeight = screenLabel.getIcon().getIconHeight();
            int offsetX = (labelWidth - imageWidth) / 2;
            int offsetY = (labelHeight - imageHeight) / 2;
            int imageX = e.getX() - offsetX;
            int imageY = e.getY() - offsetY;

            if (imageX < 0 || imageY < 0 || imageX >= imageWidth || imageY >= imageHeight) {
                return;
            }

            double scaleX = (double) originalScreenWidth / imageWidth;
            double scaleY = (double) originalScreenHeight / imageHeight;
            int serverX = (int) (imageX * scaleX);
            int serverY = (int) (imageY * scaleY);
            
            if (eventType.equals("MOUSE_MOVE")) {
                remoteControl.mouseMove(serverX, serverY);
            } else if (eventType.equals("MOUSE_PRESS")) {
                remoteControl.mousePress(InputEvent.getMaskForButton(e.getButton()));
            } else if (eventType.equals("MOUSE_RELEASE")) {
                remoteControl.mouseRelease(InputEvent.getMaskForButton(e.getButton()));
            }
        } catch (RemoteException ex) { 
            System.err.println("Client: Error sending mouse event (Connection may be lost)."); 
        }
    }
    
    private void sendKeyEvent(String eventType, KeyEvent e) {
        try {
            if (eventType.equals("KEY_PRESS")) {
                remoteControl.keyPress(e.getKeyCode());
            } else if (eventType.equals("KEY_RELEASE")) {
                remoteControl.keyRelease(e.getKeyCode());
            }
        } catch (RemoteException ex) { 
            System.err.println("Client: Error sending key event (Connection may be lost)."); 
        }
    }
}
