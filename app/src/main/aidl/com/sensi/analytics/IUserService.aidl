// IUserService.aidl
package com.sensi.analytics;

// Interface AIDL để MainActivity giao tiếp với UserService
// UserService chạy với quyền shell (do Shizuku cấp) nên có thể thực thi lệnh
// mà app thường không có quyền chạy trực tiếp.
interface IUserService {

    // Thực thi 1 lệnh shell và trả về kết quả output (stdout + stderr)
    String execCommand(String command);

    // Hủy tiến trình UserService (Shizuku sẽ tự gọi khi cần)
    void destroy();
}
