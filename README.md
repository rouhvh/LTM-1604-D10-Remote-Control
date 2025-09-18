<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   REMOTE 
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

## 📖 1. Giới thiệu hệ thống
# Ứng dụng điều khiển máy tính từ xa (Remote Control)

Ứng dụng cho phép người dùng **truy cập, theo dõi và điều khiển máy tính từ xa** thông qua mạng **Internet hoặc LAN**.

## 🖥️ Kiến trúc hệ thống
- **Máy chủ (Remote Server)**  
  - Là máy tính bị điều khiển.  
  - Chia sẻ màn hình theo thời gian thực.  
  - Tiếp nhận và thực thi các lệnh điều khiển (chuột, bàn phím) từ Client.  

- **Máy khách (Remote Client)**  
  - Là máy tính điều khiển.  
  - Hiển thị màn hình từ xa.  
  - Gửi thao tác chuột, bàn phím đến Server.  

## 🚀 Tính năng chính
- Xem toàn bộ màn hình máy tính từ xa.  
- Thao tác chuột và bàn phím như ngồi trực tiếp trước máy.  
- Giao diện đơn giản, dễ sử dụng.  
- Có thể mở rộng thêm:
  - Bảo mật và xác thực người dùng.  
  - Kết nối qua Internet (WAN).  

## 🛠️ Ứng dụng
- **Làm việc từ xa (Remote Work).**  
- **Quản trị hệ thống & server.**  

---
## 🛠️ 2. Công Nghệ Sử Dụng Trong Đề Tài

Đề tài chủ yếu dựa trên các công nghệ của **Java Platform, Standard Edition (Java SE)**:

---

### 1. Giao Tiếp Mạng: Java RMI (Remote Method Invocation) 🖥️➡️🖥️
- **Vai trò**:  
  Cho phép Client gọi các phương thức trên đối tượng của Server từ xa như thể đang chạy cục bộ. Đơn giản hóa lập trình mạng so với dùng Socket.  
- **Ứng dụng trong hệ thống**:  
  - Client gọi các hàm như `mouseMove()`, `keyPress()` để gửi lệnh điều khiển.  
  - Server dùng callback gọi `receiveScreen()` trên Client để đẩy dữ liệu hình ảnh về.  

---

### 2. Giao Diện Người Dùng: Java Swing 🖼️
- **Vai trò**: Tạo giao diện đồ họa cho ứng dụng Client.  
- **Các lớp đã dùng**:  
  - `JFrame`: Cửa sổ chính của ứng dụng.  
  - `JLabel`: Hiển thị ảnh màn hình của Server.  
  - `JScrollPane`: Thêm thanh cuộn khi màn hình lớn hơn cửa sổ Client.  
  - `JOptionPane`: Hộp thoại nhập địa chỉ IP của Server.  

---

### 3. Điều Khiển & Chụp Màn Hình: Java AWT 📸🖱️
- **Vai trò**:  
  - **Chụp ảnh màn hình**: Ghi lại hình ảnh hiện tại trên màn hình Server.  
  - **Mô phỏng điều khiển**: Thực hiện lệnh di chuyển chuột, nhấn chuột, gõ phím.  
- **Lớp chính**:  
  - `java.awt.Robot`: Tạo sự kiện đầu vào (chuột, bàn phím) và chụp màn hình.  

---

### 4. Xử Lý Hình Ảnh: Java Image I/O và AWT Image 🎨
- **Vai trò**:  
  - Nén ảnh: Chuyển `BufferedImage` thành mảng `byte[]` (JPEG) để gửi qua mạng.  
  - Hiển thị ảnh: Chuyển đổi dữ liệu ảnh nhận được để hiển thị mượt mà trên Client.  
- **Các lớp đã dùng**:  
  - `javax.imageio.ImageIO`: Đọc/ghi các định dạng ảnh.  
  - `java.awt.image.BufferedImage`: Biểu diễn ảnh trong bộ nhớ.  
  - `java.awt.Image`: Lớp cơ sở cho đối tượng đồ họa hình ảnh.  

---

### 5. Lập Trình Đa Luồng: Java Thread ⚙️
- **Vai trò**: Cho phép thực hiện nhiều tác vụ song song, tránh treo ứng dụng.  
- **Ứng dụng trong hệ thống**:  
  - Server chạy một luồng riêng để **chụp & gửi màn hình liên tục** cho Client.  
  - Vẫn đảm bảo Server có thể nhận lệnh điều khiển đồng thời.  
- **Lớp chính**:  
  - `java.lang.Thread`: Tạo và quản lý luồng thực thi mới.  

---

