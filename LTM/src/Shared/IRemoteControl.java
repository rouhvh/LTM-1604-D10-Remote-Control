	package Shared;

	import java.rmi.Remote;
	import java.rmi.RemoteException;

	public interface IRemoteControl extends Remote {
	    void registerClientCallback(IClientCallback clientCallback) throws RemoteException;
	    void mouseMove(int x, int y) throws RemoteException;
	    void mousePress(int buttons) throws RemoteException;
	    void mouseRelease(int buttons) throws RemoteException;
	    void keyPress(int keyCode) throws RemoteException;
	    void keyRelease(int keyCode) throws RemoteException;
	    void sendMessage(String message) throws RemoteException;
	    void sendFile(String filename, byte[] data) throws RemoteException;
	    void mouseWheel(int wheelAmt) throws RemoteException;
	}
		