package Shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface định nghĩa phương thức mà Client sẽ cung cấp cho Server gọi ngược lại (callback).
 * Server sẽ gọi hàm này để đẩy (push) dữ liệu màn hình về cho Client.
 */
public interface IClientCallback extends Remote {

    /**
     * Server gọi phương thức này để gửi một khung hình cho client.
     * Dữ liệu được gửi dưới dạng mảng byte đã được nén theo định dạng JPEG.
     * @param screenData Mảng byte chứa dữ liệu ảnh màn hình.
     * @throws RemoteException
     */
    void receiveScreen(byte[] screenData) throws RemoteException;
}