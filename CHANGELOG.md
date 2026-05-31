# Nhật Ký Thay Đổi (Changelog)

Tất cả các thay đổi đáng chú ý đối với dự án **Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)** sẽ được ghi nhận tại tập tin này.

Định dạng dựa trên [Keep a Changelog](https://keepachangelog.com/vi/1.0.0/) và dự án này tuân thủ [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.2.0] - 2026-05-31

### Changed
- Cải thiện trạng thái hiển thị của các nút chức năng trên thanh Sidebar, tối ưu hóa trải nghiệm điều hướng khi chuyển đổi giữa chế độ thông thường và chế độ lịch sử.

### Fixed
- Sửa lỗi kẹt màn hình chi tiết phiên đấu giá khi người dùng bấm nút Quay lại từ giao diện lịch sử đấu giá của Bidder.
- Khắc phục lỗi nút Quay lại tự động phát sáng và lỗi nút Lịch sử phát sáng khi bấm quay lại; đảm bảo nút chỉ sáng khi di chuột qua và tự động sáng nút Trang chủ (Tất cả) sau khi quay lại.

## [2.1.0] - 2026-05-31

### Added
- **Đồng bộ thời gian thực (Real-time Broadcast):**
  - Tích hợp mạng đa luồng, máy chủ tự động gửi thông báo Broadcast khi có giá đấu mới tới tất cả các kết nối.
  - Client tự động xử lý giao dịch trên bộ nhớ cục bộ mà không cần tải lại toàn bộ cơ sở dữ liệu.
- Bổ sung dải băng (Banner) thông báo "--- BẢN DÙNG THỬ ---" tại trung tâm giao diện dành riêng cho tài khoản Khách vãng lai (GUEST).

### Changed
- **Tối ưu hóa hiệu năng cực đại (Extreme Performance Optimization):**
  - Tách hoàn toàn tiến trình gửi dữ liệu mạng (Network I/O) sang luồng chạy nền (Background Thread), giúp giao diện không bao giờ bị khựng lại chờ mạng.
  - Tối ưu biểu đồ (LineChart): chỉ vẽ thêm các điểm giá trị mới thay vì xóa và vẽ lại toàn bộ đồ thị từ đầu.
  - Xóa bỏ hàng loạt các truy vấn SQLite thừa trên luồng giao diện UI, ngăn chặn hoàn toàn hiện tượng nghẽn nút chai (Lock Contention) khi người dùng nhấn đặt giá liên tục với cường độ cao.

### Fixed
- Sửa lỗi biểu đồ và thời gian đấu giá đóng băng không cập nhật đối với chế độ dùng thử (GUEST).
- Khắc phục triệt để lỗi ứng dụng bị kẹt "Not Responding" (Treo giao diện) trong thời gian dài khi có lượng tương tác quá lớn trên hệ thống.

---

## [2.0.0] - 2026-05-31### Added
- **Trải nghiệm Đấu giá mới:**
  - Phân chia danh sách đấu giá ở màn hình chính thành 2 tab: **"Đang diễn ra"** (mặc định) và **"Chuẩn bị diễn ra"**.
  - Hiển thị trực tiếp **Giá cao nhất hiện tại** (`Giá cao nhất: ... $`) ngay trên mỗi thẻ đấu giá (phía trên đồng hồ đếm ngược).
- **Tính năng Quản lý của Admin:**
  - Thêm 3 nút phân loại nhanh cho Quản lý tài khoản bên trái dưới nút Quay lại: **Tất cả**, **Seller**, **Bidder**, sắp xếp mặc định theo ID người dùng.
  - Tích hợp thanh tìm kiếm và bộ lọc vai trò ComboBox (Tất cả, Seller, Bidder) sắp xếp theo ID cho danh sách tài khoản vi phạm.
  - Tự động thay đổi Search Prompt Text của ô tìm kiếm phù hợp theo tab lịch sử được chọn.
- **Trạng thái Trống (Empty States):**
  - Hiển thị nhãn **"Không có"** thân thiện tại toàn bộ các danh sách trống (danh sách đấu giá, lịch sử, danh sách người dùng) thay vì để trống trơn.

### Changed
- **Quản lý Cửa sổ (Window Management):**
  - Cấu hình ứng dụng mặc định khởi động ở chế độ **Toàn màn hình (Maximized)** để hiển thị thông tin rõ ràng nhất.
- **Điều hướng Lịch sử:**
  - Di chuyển nút **Quay lại** lên vị trí trên cùng trong giao diện Lịch sử của người dùng.
  - Đổi tên nút "Trang chủ" thành nút **Tất cả** trong bộ lọc lịch sử.
  - Cho phép Seller theo dõi toàn bộ các phiên đấu giá đã được tạo (kể cả chưa có ai đặt giá).
- **Giao diện Admin:**
  - Khi Admin chọn "Quản lý tài khoản", sidebar tự động ẩn các tùy chọn khác và chỉ hiển thị nút **← Quay lại** ở trên cùng.
  - Thiết lập nút **Quay lại** của Admin luôn trở về Trang chủ chuẩn xác.
  - Đổi tên mục "Lịch sử đấu" ở sidebar của Admin thành **"Lịch sử"**.
  - Tự động ẩn nút "Quản lý tài khoản" khi Admin đang ở chế độ xem Lịch sử.

### Fixed
- Khắc phục triệt để lỗi chuyển đổi màn hình (Window Sizing Bug) trên hệ điều hành Windows khiến cửa sổ ứng dụng bị tự động thu nhỏ về góc trên bên trái hoặc chỉ hiện một nửa màn hình khi bấm Đăng nhập/Đăng ký hoặc Đăng nhập thành công.

---

## [1.0.0] - 2026-05-15

### Added
- **Bảo Mật Tài Khoản:** Quản lý người dùng theo vai trò (Admin/Seller/Bidder). Mật khẩu được mã hóa một chiều bằng SHA-256 cực kỳ an toàn.
- **Máy Chủ Thời Gian Thực:** Kết nối Socket TCP đa luồng. Máy chủ quét liên tục mỗi giây để giám sát trạng thái của các phiên đấu giá.
- **Sản Phẩm Đa Hình:** Hỗ trợ đăng bán đa dạng: Đồ điện tử (bảo hành, sửa chữa), Nghệ thuật (họa sĩ, năm sáng tác), Phương tiện (số Km, bảo dưỡng).
- **Đếm Ngược Real-time:** Giao diện tự động đếm lùi thời gian (Giờ:Phút:Giây) cho mỗi phiên đấu giá và tự động vô hiệu hóa nút đặt giá khi hết giờ.
- **Gia Hạn Chống Snipping:** Tự động cộng thêm 60 giây vào thời gian kết thúc nếu có người đặt giá ở những giây cuối cùng.
- **Biểu Đồ Lịch Sử Giá:** Vẽ biểu đồ đường (Line Chart) thời gian thực ngay trên giao diện để theo dõi sự cạnh tranh và biến động giá của sản phẩm.
- **Chốt Phiên Tự Động:** Máy chủ tự động đóng phiên khi hết giờ, tìm ra người trả giá cao nhất và lưu trạng thái thành công vào Database.
- **Quyền Lực Quản Trị Viên:** Admin có quyền ngưng phiên (Force Stop) ngay lập tức hoặc xóa các phiên đấu giá vi phạm.
