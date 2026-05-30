# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System) - v1.0.0

Chào mừng bạn đến với **Hệ Thống Đấu Giá Trực Tuyến (Online Auction System) v1.0.0**, một ứng dụng được thiết kế theo kiến trúc Client-Server đa luồng (Multi-threaded TCP Socket) kết hợp với giao diện đồ họa hiện đại xây dựng trên nền tảng JavaFX 21 và cơ sở dữ liệu SQLite. Phiên bản v1.0.0 đánh dấu sự tích hợp hoàn chỉnh của hệ thống thời gian thực (Real-time), biểu đồ lịch sử và cơ chế chốt đơn tự động.

Hệ thống cho phép người dùng đăng ký làm Người bán (Seller) để tạo và đưa các tác phẩm nghệ thuật, đồ điện tử hoặc phương tiện lên sàn đấu giá, hoặc đăng ký làm Người mua (Bidder) để tìm kiếm, gửi yêu cầu sản phẩm và tham gia đấu giá trực tiếp theo thời gian thực.

---

## Mục Lục
1. [Mô Tả Bài Toán & Phạm Vi Hệ Thống](#mô-tả-bài-toán--phạm-vi-hệ-thống)
2. [Công Nghệ Sử Dụng & Yêu Cầu Cài Đặt](#công-nghệ-sử-dụng--yêu-cầu-cài-đặt)
3. [Cấu Trúc Thư Mục & Các Module Chính](#cấu-trúc-thư-mục--các-module-chính)
4. [Hướng Dẫn Cài Đặt & Chạy Chương Trình](#hướng-dẫn-cài-đặt--chạy-chương-trình)
5. [Danh Sách Chức Năng Cốt Lõi (v1.0.0)](#danh-sách-chức-năng-cốt-lõi-v100)

---

## Mô Tả Bài Toán & Phạm Vi Hệ Thống

### 1. Bài toán đặt ra
Trong thời đại số hóa, việc mua bán và đấu giá tài sản cần một môi trường minh bạch, bảo mật, và phản hồi tức thời. Bài toán đặt ra là thiết kế một hệ thống đấu giá trực tuyến tối ưu hóa trải nghiệm tương tác giữa người mua và người bán, đảm bảo an toàn thông tin tài khoản và tránh xung đột dữ liệu khi nhiều người thầu giá cùng một sản phẩm ở một thời điểm.

### 2. Phạm vi hệ thống
* **Đối tượng sử dụng**:
  * **Khách vãng lai**: Có thể xem danh sách sản phẩm đấu giá và tìm kiếm thông tin sản phẩm.
  * **Người mua (Bidder)**: Tham gia trả giá sản phẩm (Bidding), xem lịch sử đấu giá qua biểu đồ, cập nhật thông tin cá nhân và gửi yêu cầu đấu giá.
  * **Người bán (Seller)**: Tạo và quản lý sản phẩm đấu giá mới (Nghệ thuật, Điện tử, Phương tiện), quản lý giao dịch bán.
  * **Quản trị viên (Admin)**: Toàn quyền quản trị hệ thống, phê duyệt, giám sát, ngưng hoặc xóa phiên đấu giá.
* **Hình thức kết nối**: Kết nối Client-Server tập trung qua giao thức TCP/IP Socket (Port 8080).
* **Môi trường hoạt động**: Chạy đa nền tảng trên môi trường hỗ trợ máy ảo Java JVM và có hỗ trợ hiển thị giao diện đồ họa.

---

## Công Nghệ Sử Dụng & Yêu Cầu Cài Đặt

### 1. Công nghệ & Thư viện sử dụng
* **Ngôn ngữ phát triển**: Java 21 (JDK 21)
* **Giao diện người dùng (GUI)**: JavaFX 21.0.2 & FXML
* **Cơ sở dữ liệu**: SQLite JDBC (`sqlite-jdbc` 3.45.1.0)
* **Công cụ build & Test**: Maven, JUnit 5
* **Thư viện JSON**: Google Gson 2.10.1 & Jackson Databind 2.17.0
* **Bảo mật**: Mã hóa băm mật khẩu chuẩn SHA-256 (Native Java).

### 2. Yêu cầu cài đặt
Để chạy được chương trình trên thiết bị của bạn, hãy đảm bảo hệ thống đã cài đặt:
1. **Java Development Kit (JDK)**: Phiên bản **21** trở lên.
2. **Apache Maven**: Phiên bản **3.8** trở lên.

---

## Cấu Trúc Thư Mục & Các Module Chính

Hệ thống được tổ chức theo chuẩn cấu trúc của một dự án Maven đa năng:

```text
Online Auction System/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── app/
│   │           ├── controller/      # Controllers tương tác giao diện FXML
│   │           ├── database/        # Kết nối CSDL SQLite (AppDatabase, Session)
│   │           ├── model/           # Models đa hình (Art, Electronics, Vehicle)
│   │           ├── network/         # Kết nối TCP phía Client
│   │           ├── server/          # Kiến trúc TCP Socket Server đa luồng
│   │           └── utils/           # Mã hóa bảo mật (PasswordUtil)
│   └── test/                        # Bộ Test tự động (JUnit 5)
├── target/                          # Mã nguồn được biên dịch
├── auction_app.db                   # Tập tin Cơ sở dữ liệu SQLite
└── pom.xml                          # Quản lý thư viện Maven
```

---

## Hướng Dẫn Cài Đặt & Chạy Chương Trình

Hệ thống hoạt động theo mô hình Client-Server. Cần khởi động **Server trước**, sau đó mới khởi động **Client**.

### Bước 1: Biên dịch và chạy Test
Sử dụng Maven để dọn dẹp, biên dịch và chạy các bài test:
```bash
mvn clean test
```

### Bước 2: Khởi động Server (Cổng 8080)
Mở terminal tại thư mục gốc của dự án:
```bash
mvn exec:java -Dexec.mainClass="app.server.ServerMain"
```

### Bước 3: Khởi động Client
Mở một Terminal khác tại thư mục gốc và chạy Client:
```bash
mvn javafx:run
```

---

## Danh Sách Chức Năng Cốt Lõi (v1.0.0)

Phiên bản **v1.0.0** tự hào mang đến các tính năng mạnh mẽ sau:

| Phân hệ / Chức năng | Mô tả chi tiết chức năng |
| :--- | :--- |
| **Bảo Mật Tài Khoản** | Quản lý người dùng theo vai trò (Admin/Seller/Bidder). Mật khẩu được mã hóa một chiều bằng SHA-256 cực kỳ an toàn. |
| **Máy Chủ Thời Gian Thực** | Kết nối Socket TCP đa luồng. Máy chủ quét liên tục mỗi giây để giám sát trạng thái của các phiên đấu giá. |
| **Sản Phẩm Đa Hình** | Hỗ trợ đăng bán đa dạng: Đồ điện tử (bảo hành, sửa chữa), Nghệ thuật (họa sĩ, năm sáng tác), Phương tiện (số Km, bảo dưỡng). |
| **Đếm Ngược Real-time** | Giao diện tự động đếm lùi thời gian (Giờ:Phút:Giây) cho mỗi phiên đấu giá và tự động vô hiệu hóa nút đặt giá khi hết giờ. |
| **Gia Hạn Chống Snipping** | Tự động cộng thêm 60 giây vào thời gian kết thúc nếu có người đặt giá ở những giây cuối cùng. |
| **Biểu Đồ Lịch Sử Giá** | Vẽ biểu đồ đường (Line Chart) thời gian thực ngay trên giao diện để theo dõi sự cạnh tranh và biến động giá của sản phẩm. |
| **Chốt Phiên Tự Động** | Máy chủ tự động đóng phiên khi hết giờ, tìm ra người trả giá cao nhất và lưu trạng thái thành công vào Database. |
| **Quyền Lực Quản Trị Viên** | Quản trị viên (Admin) có quyền ngưng phiên (Force Stop) ngay lập tức hoặc xóa các phiên đấu giá vi phạm. |
