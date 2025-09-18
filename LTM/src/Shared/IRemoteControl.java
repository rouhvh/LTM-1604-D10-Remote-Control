	package Shared;
	
	import java.rmi.Remote;
	import java.rmi.RemoteException;
	
	/**
	 * Interface định nghĩa các phương thức mà Server sẽ cung cấp cho Client gọi.
	 * Client sẽ gọi các hàm này để gửi lệnh điều khiển đến Server.
	 */
	public interface IRemoteControl extends Remote {
	
	    /**
	     * Client gọi phương thức này để đăng ký nhận ảnh màn hình từ Server.
	     * Đây là bước đầu tiên Client phải làm sau khi kết nối.
	     * @param clientCallback một đối tượng Remote của Client để Server có thể gọi ngược lại.
	     * @throws RemoteException
	     */
	    void registerClientCallback(IClientCallback clientCallback) throws RemoteException;
	
	    /**
	     * Gửi lệnh di chuyển chuột đến tọa độ (x, y) trên màn hình Server.
	     * @param x Tọa độ trục hoành.
	     * @param y Tọa độ trục tung.
	     * @throws RemoteException
	     */
	    void mouseMove(int x, int y) throws RemoteException;
	
	    /**
	     * Gửi lệnh nhấn chuột.
	     * @param buttons Mã của nút chuột (ví dụ: InputEvent.BUTTON1_DOWN_MASK).
	     * @throws RemoteException
	     */
	    void mousePress(int buttons) throws RemoteException;
	
	    /**
	     * Gửi lệnh nhả chuột.
	     * @param buttons Mã của nút chuột.
	     * @throws RemoteException
	     */
	    void mouseRelease(int buttons) throws RemoteException;
	
	    /**
	     * Gửi lệnh nhấn một phím.
	     * @param keyCode Mã của phím được nhấn.
	     * @throws RemoteException
	     */
	    void keyPress(int keyCode) throws RemoteException;
	
	    /**
	     * Gửi lệnh nhả một phím.
	     * @param keyCode Mã của phím được nhả.
	     * @throws RemoteException
	     */
	    void keyRelease(int keyCode) throws RemoteException;
	}