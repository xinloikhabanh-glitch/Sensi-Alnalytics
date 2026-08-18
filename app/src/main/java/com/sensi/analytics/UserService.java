package com.sensi.analytics;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * UserService chạy trong tiến trình riêng với quyền "shell" do Shizuku cấp.
 * Đây là nơi DUY NHẤT được phép gọi Runtime.exec() để chạy lệnh hệ thống,
 * vì tiến trình này được Shizuku khởi chạy bằng UID của shell (2000),
 * không phải UID thường của app.
 *
 * Lưu ý: Class này KHÔNG được gọi trực tiếp từ MainActivity, mà được
 * Shizuku bind tới thông qua ServiceConnection (xem MainActivity.java).
 */
public class UserService extends IUserService.Stub {

    // Constructor rỗng bắt buộc phải có (Shizuku yêu cầu)
    public UserService() {
    }

    @Override
    public String execCommand(String command) {
        StringBuilder result = new StringBuilder();
        try {
            // Chạy lệnh shell với quyền được Shizuku cấp
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});

            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = stdout.readLine()) != null) {
                result.append(line).append("\n");
            }
            while ((line = stderr.readLine()) != null) {
                result.append("[err] ").append(line).append("\n");
            }

            process.waitFor();
        } catch (Exception e) {
            result.append("Lỗi khi thực thi lệnh: ").append(e.getMessage());
        }
        return result.toString();
    }

    @Override
    public void destroy() {
        // Shizuku gọi hàm này khi cần dừng service - có thể thoát tiến trình
        System.exit(0);
    }
}
