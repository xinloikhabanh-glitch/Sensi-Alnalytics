# Sensi Analytics 1.0

Ứng dụng Android (Java) phân tích cấu hình thiết bị và đề xuất thông số tối ưu
cho Free Fire, sử dụng Shizuku API để thực thi một số lệnh cài đặt hệ thống an toàn.

## Cách mở project
1. Mở Android Studio → **Open** → chọn thư mục `SensiAnalytics`.
2. Đợi Gradle sync (project cần kết nối mạng để tải các thư viện `dev.rikka.shizuku:api`
   và `dev.rikka.shizuku:provider`).
3. Build & Run trên thiết bị/máy ảo Android 8.0 (API 26) trở lên.

## Yêu cầu để dùng chức năng "Áp dụng tối ưu" / "Phục hồi cài đặt"
Các chức năng này cần quyền do **Shizuku** cấp (không cần root máy):
1. Cài đặt và khởi chạy Shizuku trên thiết bị (theo hướng dẫn tại
   https://shizuku.rikka.app), thường kích hoạt qua ADB bằng máy tính hoặc
   qua Wireless debugging (Android 11+).
2. Mở "Sensi Analytics 1.0" → bấm **Áp dụng tối ưu** → app sẽ hiện hộp thoại
   xin quyền Shizuku → chọn **Cho phép**.
3. Sau khi cấp quyền, app sẽ tự kết nối `UserService` và có thể chạy lệnh.

## Giới hạn phạm vi có chủ đích
- App **không** chỉnh sửa file cấu hình hay bộ nhớ tiến trình của Free Fire
  hay bất kỳ ứng dụng nào khác.
- Các lệnh shell trong `AnalyticsService.java` chỉ thay đổi cài đặt hệ thống
  Android tiêu chuẩn (animation scale, giới hạn tiến trình nền, ẩn overlay
  điểm chạm, thời gian nhận long-press, ưu tiên tần số quét tối đa mà màn
  hình vật lý hỗ trợ...) — tương tự những gì người dùng có thể tự bật trong
  "Tùy chọn nhà phát triển".
- App **không** và **không thể** tạo ra tần số quét (Hz) giả trên phần cứng
  không hỗ trợ — nếu máy chỉ có màn 60Hz thì lệnh ưu tiên 90/120Hz sẽ không
  có tác dụng gì.
- App **không** làm thay đổi độ giật súng, độ hồi tâm ngắm, quỹ đạo đạn hay
  bất kỳ cơ chế nào của game Free Fire — những thứ đó do code phía client/
  server của game quyết định, hệ điều hành không can thiệp được.
- Bộ 3 thông số đề xuất (Tổng quát/Ngắm/DPI) chỉ mang tính **tham khảo**;
  người chơi tự nhập vào phần Cài đặt độ nhạy bên trong game.

## Cấu trúc mã nguồn
| File | Vai trò |
|---|---|
| `MainActivity.java` | Giao diện chính, điều phối luồng UI |
| `DeviceAnalyzer.java` | Lấy Model/RAM/CPU, tính điểm benchmark |
| `ProfileManager.java` | Quy đổi điểm → bộ 3 thông số đề xuất |
| `ShellExecutor.java` | Quản lý quyền + kết nối Shizuku UserService |
| `UserService.java` / `IUserService.aidl` | Tiến trình thực thi lệnh shell với quyền shell |
| `AnalyticsService.java` | Áp dụng / phục hồi cài đặt hệ thống |
| `OptimizationHistory.java` | Lưu tối đa 3 lần lịch sử gần nhất |
