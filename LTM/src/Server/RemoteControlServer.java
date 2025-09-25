package Server;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import Shared.IClientCallback;
import Shared.IRemoteControl;

public class RemoteControlServer extends UnicastRemoteObject implements IRemoteControl {

    private Robot robot;
    private IClientCallback clientCallback;
    private ClientConnectionListener listener; // BIẾN MỚI: Listener để giao tiếp với UI

    // Interface lồng nhau để MainUI có thể lắng nghe các sự kiện từ Server
    public interface ClientConnectionListener {
        void onClientConnected(IClientCallback clientCallback, String clientIp, IRemoteControl serverControl);
        void onMessageReceived(String message);
    }

    public RemoteControlServer() throws Exception {
        super();
        this.robot = new Robot();
    }

    // Phương thức để MainUI đăng ký chính nó làm listener
    public void setConnectionListener(ClientConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void registerClientCallback(IClientCallback clientCallback) throws RemoteException {
        this.clientCallback = clientCallback;
        
        String clientIp = "Unknown";
        try {
            clientIp = UnicastRemoteObject.getClientHost();
        } catch (Exception e) { e.printStackTrace(); }

        // Thông báo cho listener (MainUI) biết có client vừa kết nối
        if (listener != null) {
            listener.onClientConnected(clientCallback, clientIp, this);
        }
        
        // Bắt đầu luồng gửi màn hình
        new Thread(() -> {
            while (true) {
                try {
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    byte[] screenData = compressImage(screenCapture, 0.7f);
                    this.clientCallback.receiveScreen(screenData);
                    Thread.sleep(500);
                } catch (Exception e) {
                    System.err.println("Server: Client disconnected or error during streaming. Stopping stream.");
                    break;
                }
            }
        }).start();
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        // Thông báo cho listener (MainUI) để hiển thị tin nhắn trên cửa sổ chat của Server
        if (listener != null) {
            listener.onMessageReceived(message);
        }
        // Gửi lại tin nhắn cho client để cả hai bên đều thấy
        if (this.clientCallback != null) {
            this.clientCallback.receiveMessage(message);
        }
    }

    @Override
    public void sendFile(String filename, byte[] data) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(null, "Client wants to send you a file: " + filename + "\nDo you want to accept?", "File Transfer", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(filename));
                if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        Files.write(fileChooser.getSelectedFile().toPath(), data);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private byte[] compressImage(BufferedImage image, float quality) throws Exception {
        // ... (phương thức này giữ nguyên) ...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        byte[] data = baos.toByteArray();
        baos.close();
        ios.close();
        writer.dispose();
        return data;
    }

    // --- CÁC PHƯƠNG THỨC ĐIỀU KHIỂN (giữ nguyên) ---
    @Override
    public void mouseMove(int x, int y) throws RemoteException { robot.mouseMove(x, y); }
    @Override
    public void mousePress(int buttons) throws RemoteException { robot.mousePress(buttons); }
    @Override
    public void mouseRelease(int buttons) throws RemoteException { robot.mouseRelease(buttons); }
    @Override
    public void keyPress(int keyCode) throws RemoteException { robot.keyPress(keyCode); }
    @Override
    public void keyRelease(int keyCode) throws RemoteException { robot.keyRelease(keyCode); }
    @Override
    public void mouseWheel(int wheelAmt) throws RemoteException { robot.mouseWheel(wheelAmt); }
}