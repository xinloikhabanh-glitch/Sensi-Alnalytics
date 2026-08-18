package com.sensi.analytics;

/**
 * AnalyticsService là lớp điều phối nghiệp vụ:
 *  - Nhận DeviceInfo + Profile đã tính toán
 *  - Áp dụng một số CÀI ĐẶT HỆ THỐNG AN TOÀN thông qua ShellExecutor để máy
 *    chạy mượt hơn khi chơi game (không đụng tới file/tiến trình của game)
 *  - Cung cấp thao tác "Phục hồi cài đặt" để đưa các giá trị về mặc định
 *
 * Các lệnh dùng ở đây đều là lệnh `settings put` tiêu chuẩn của Android,
 * tương đương những gì người dùng có thể tự bật trong "Tùy chọn nhà phát triển".
 * Shizuku chỉ giúp thực hiện các lệnh này mà không cần bật thủ công từng cái.
 *
 * LƯU Ý QUAN TRỌNG VỀ GIỚI HẠN PHẠM VI:
 * Khối "Tối ưu độ mượt & phản hồi chạm" bên dưới CHỈ chỉnh các cài đặt hệ
 * thống Android tiêu chuẩn (ẩn overlay hiển thị điểm chạm, giảm thời gian
 * nhận long-press, yêu cầu hệ thống ưu tiên tần số quét CAO NHẤT MÀ MÀN HÌNH
 * VẬT LÝ ĐANG HỖ TRỢ). App KHÔNG và KHÔNG THỂ tạo ra tần số quét (Hz) giả
 * trên phần cứng không hỗ trợ — nếu máy chỉ có màn 60Hz thì lệnh này không
 * có tác dụng gì (không phải "giả lập Hz"). App cũng KHÔNG thay đổi độ giật
 * súng, độ hồi tâm ngắm hay bất kỳ cơ chế nào của game Free Fire — những thứ
 * đó do code của game quyết định, hệ điều hành không can thiệp được.
 */
public class AnalyticsService {

    private final ShellExecutor shellExecutor;

    // Giá trị mặc định gốc của Android, dùng để phục hồi
    private static final float DEFAULT_ANIMATION_SCALE = 1.0f;
    private static final int DEFAULT_BACKGROUND_LIMIT = -1; // -1 = "chuẩn" (standard limit)
    private static final int DEFAULT_LONG_PRESS_TIMEOUT_MS = 400; // mặc định gốc Android
    private static final int DEFAULT_REFRESH_RATE_OVERRIDE = 0;   // 0 = để hệ thống tự quyết định

    public AnalyticsService(ShellExecutor shellExecutor) {
        this.shellExecutor = shellExecutor;
    }

    /**
     * Áp dụng tối ưu dựa theo profile được đề xuất.
     * Mức profile càng cao (thiết bị càng mạnh) thì càng giảm animation scale
     * và tăng giới hạn tiến trình nền, giúp giải phóng tài nguyên cho game.
     */
    public String applyOptimization(ProfileManager.Profile profile) {
        StringBuilder log = new StringBuilder();

        float animationScale;
        int backgroundLimit;

        // v1.1: cường độ tối ưu được tăng thêm 20% so với bản gốc
        // (animation scale giảm thêm 20%, background limit siết chặt thêm 20%)
        switch (profile.tier) {
            case "Cao cấp":
                animationScale = 0.4f;  // gốc 0.5f x 0.8
                backgroundLimit = 1;    // gốc 2 x 0.8 (làm tròn, tối thiểu 1)
                break;
            case "Khá":
                animationScale = 0.4f;  // gốc 0.5f x 0.8
                backgroundLimit = 2;    // gốc 3 x 0.8
                break;
            case "Trung bình":
                animationScale = 0.6f;  // gốc 0.75f x 0.8
                backgroundLimit = 3;    // gốc 4 x 0.8
                break;
            default: // Thấp
                animationScale = 0.8f;  // gốc 1.0f x 0.8
                backgroundLimit = 3;    // gốc 4 x 0.8
                break;
        }

        // Giảm tốc độ hoạt ảnh hệ thống -> UI phản hồi nhanh hơn
        log.append(shellExecutor.run("settings put global window_animation_scale " + animationScale)).append("\n");
        log.append(shellExecutor.run("settings put global transition_animation_scale " + animationScale)).append("\n");
        log.append(shellExecutor.run("settings put global animator_duration_scale " + animationScale)).append("\n");

        // Giới hạn số tiến trình nền tối đa -> dành nhiều RAM hơn cho game
        log.append(shellExecutor.run("settings put global background_process_limit " + backgroundLimit)).append("\n");

        // Giải phóng bộ nhớ đệm hệ thống hiện tại (an toàn, không xoá dữ liệu người dùng)
        log.append(shellExecutor.run("sync && echo 1 > /proc/sys/vm/drop_caches 2>/dev/null || echo 'Bỏ qua drop_caches (cần quyền cao hơn)'")).append("\n");

        // ==== Tối ưu độ mượt & phản hồi chạm (KHÔNG phải Hz giả lập) ====
        // Ẩn overlay hiển thị điểm chạm trên màn hình -> giảm nhiễu hình ảnh khi thao tác
        log.append(shellExecutor.run("settings put system show_touches 0")).append("\n");
        log.append(shellExecutor.run("settings put system pointer_location 0")).append("\n");

        // Giảm thời gian hệ thống chờ để nhận diện "long press" -> cảm giác chạm phản hồi nhanh hơn
        log.append(shellExecutor.run("settings put secure long_press_timeout 250")).append("\n");

        // Yêu cầu hệ thống ưu tiên tần số quét CAO NHẤT MÀ MÀN HÌNH VẬT LÝ HỖ TRỢ
        // (Android 11+). Nếu màn hình chỉ hỗ trợ 60Hz, lệnh này không có tác dụng gì -
        // đây KHÔNG phải cách "giả lập" ra Hz không tồn tại trên phần cứng.
        log.append(shellExecutor.run("settings put system peak_refresh_rate 120")).append("\n");
        log.append(shellExecutor.run("settings put system min_refresh_rate 90")).append("\n");

        return log.toString();
    }

    /** Phục hồi toàn bộ cài đặt về giá trị mặc định gốc của Android */
    public String restoreDefaults() {
        StringBuilder log = new StringBuilder();
        log.append(shellExecutor.run("settings put global window_animation_scale " + DEFAULT_ANIMATION_SCALE)).append("\n");
        log.append(shellExecutor.run("settings put global transition_animation_scale " + DEFAULT_ANIMATION_SCALE)).append("\n");
        log.append(shellExecutor.run("settings put global animator_duration_scale " + DEFAULT_ANIMATION_SCALE)).append("\n");
        log.append(shellExecutor.run("settings put global background_process_limit " + DEFAULT_BACKGROUND_LIMIT)).append("\n");

        // Phục hồi các cài đặt độ mượt/phản hồi chạm về mặc định gốc
        log.append(shellExecutor.run("settings put system show_touches 0")).append("\n");
        log.append(shellExecutor.run("settings put secure long_press_timeout " + DEFAULT_LONG_PRESS_TIMEOUT_MS)).append("\n");
        log.append(shellExecutor.run("settings put system peak_refresh_rate " + DEFAULT_REFRESH_RATE_OVERRIDE)).append("\n");
        log.append(shellExecutor.run("settings put system min_refresh_rate " + DEFAULT_REFRESH_RATE_OVERRIDE)).append("\n");
        return log.toString();
    }
}
