package Server;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import Shared.IClientCallback;
import Shared.IRemoteControl;

/**
 * Lớp Server triển khai các logic điều khiển.
 * Nó tạo ra một đối tượng Robot để thực thi các lệnh và gửi ảnh màn hình đã được nén.
 */
public class RemoteControlServer extends UnicastRemoteObject implements IRemoteControl {

    private Robot robot;

    protected RemoteControlServer() throws Exception {
        super();
        this.robot = new Robot();
    }

    @Override
    public void registerClientCallback(IClientCallback clientCallback) throws RemoteException {
        System.out.println("Server: A client has registered. Starting screen stream...");
        
        // Bắt đầu một luồng mới để gửi ảnh màn hình liên tục cho client này
        new Thread(() -> {
            while (true) { // Vòng lặp sẽ tiếp tục cho đến khi có lỗi (client ngắt kết nối)
                try {
                    // 1. Chụp ảnh màn hình
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    
                    // 2. Nén ảnh thành mảng byte JPEG
                    byte[] screenData = compressImage(screenCapture, 0.7f); // Chất lượng 70%
                    
                    // 3. Gọi hàm callback ở client để gửi mảng byte qua
                    clientCallback.receiveScreen(screenData);
                    
                    // 4. Tạm dừng để đạt được FPS mong muốn (khoảng 20 FPS)
                    Thread.sleep(50);
                } catch (Exception e) {
                    System.err.println("Server: Client disconnected or error during streaming. Stopping stream for this client.");
                    // Khi có lỗi xảy ra (thường là client đóng cửa sổ), luồng này sẽ kết thúc.
                    break;
                }
            }
        }).start();
    }

    /**
     * Phương thức tiện ích để nén một BufferedImage thành mảng byte JPEG.
     * @param image Ảnh cần nén.
     * @param quality Chất lượng nén (từ 0.0f đến 1.0f).
     * @return Mảng byte chứa dữ liệu ảnh JPEG.
     * @throws Exception
     */
    private byte[] compressImage(BufferedImage image, float quality) throws Exception {
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
        
        // Dọn dẹp tài nguyên
        baos.close();
        ios.close();
        writer.dispose();
        
        return data;
    }

    // --- Triển khai các phương thức điều khiển (đã thêm log) ---
    
    @Override
    public void mouseMove(int x, int y) throws RemoteException {
        System.out.println("Server: Received MOUSE_MOVE to (" + x + ", " + y + ")");
        robot.mouseMove(x, y);
    }

    @Override
    public void mousePress(int buttons) throws RemoteException {
        System.out.println("Server: Received MOUSE_PRESS with buttons mask: " + buttons);
        robot.mousePress(buttons);
    }

    @Override
    public void mouseRelease(int buttons) throws RemoteException {
        System.out.println("Server: Received MOUSE_RELEASE with buttons mask: " + buttons);
        robot.mouseRelease(buttons);
    }

    @Override
    public void keyPress(int keyCode) throws RemoteException {
        System.out.println("Server: Received KEY_PRESS with code: " + keyCode);
        robot.keyPress(keyCode);
    }

    @Override
    public void keyRelease(int keyCode) throws RemoteException {
        System.out.println("Server: Received KEY_RELEASE with code: " + keyCode);
        robot.keyRelease(keyCode);
    }
    
    public static void main(String[] args) {
        try {
            // Tự khởi tạo RMI Registry trên cổng 1099 để dễ dàng chạy và gỡ lỗi.
            LocateRegistry.createRegistry(1099);
            System.out.println("Server: RMI Registry created on port 1099.");

            // Tạo và đăng ký đối tượng Server
            RemoteControlServer server = new RemoteControlServer();
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("RemoteControlService", server);
            
            System.out.println("Server is ready and waiting for client connections...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}