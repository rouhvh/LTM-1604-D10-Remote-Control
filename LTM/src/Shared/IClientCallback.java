package Shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientCallback extends Remote {
    // Chức năng cũ
    void receiveScreen(byte[] screenData) throws RemoteException;
    void receiveMessage(String message) throws RemoteException;
    void receiveFile(String filename, byte[] data) throws RemoteException;

    // DÒNG NÀY PHẢI TỒN TẠI
    void forceDisconnect(String reason) throws RemoteException;
}