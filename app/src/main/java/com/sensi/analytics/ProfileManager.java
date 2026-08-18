package com.sensi.analytics;

/**
 * ProfileManager quy đổi điểm benchmark (từ DeviceAnalyzer) thành bộ 3
 * thông số đề xuất theo bảng quy tắc gốc:
 *
 *   Điểm >= 1000  -> 80 / 80 / 90
 *   Điểm >= 700   -> 60 / 60 / 70
 *   Điểm >= 500   -> 40 / 40 / 50
 *   Điểm <  500   -> 20 / 20 / 30
 *
 * v1.1: toàn bộ 3 thông số được nhân thêm hệ số BOOST_FACTOR (+20%) so với
 * bảng gốc, để phản ánh mức đề xuất "mạnh tay" hơn:
 *
 *   Điểm >= 1000  -> 96 / 96 / 108
 *   Điểm >= 700   -> 72 / 72 / 84
 *   Điểm >= 500   -> 48 / 48 / 60
 *   Điểm <  500   -> 24 / 24 / 36
 *
 * 3 giá trị (general, scope, dpi) là các thông số đề xuất mang tính tham khảo,
 * người dùng tự nhập thủ công vào phần Cài đặt độ nhạy trong game - app này
 * KHÔNG tự động ghi đè cấu hình của bất kỳ ứng dụng game nào.
 */
public class ProfileManager {

    /** Hệ số tăng cường đề xuất (+20%) áp dụng cho v1.1 */
    private static final double BOOST_FACTOR = 1.2;

    private static int boost(int base) {
        return (int) Math.round(base * BOOST_FACTOR);
    }

    /** Bộ 3 thông số đề xuất */
    public static class Profile {
        public final int general;   // độ nhạy tổng quát đề xuất
        public final int scope;     // độ nhạy ngắm/scope đề xuất
        public final int dpi;       // % DPI camera đề xuất
        public final String tier;   // nhãn mức hiệu năng

        /**
         * "Buff DPI" GIẢ LẬP — chỉ là con số hiển thị trên UI để minh hoạ mức
         * tối ưu, KHÔNG được dùng để đổi mật độ điểm ảnh thật (density/dpi)
         * của máy. Cố tình đổi density thật qua `settings put system
         * density_dpi` sẽ làm chữ/icon toàn hệ thống bị to/nhỏ bất thường và
         * có thể gây lỗi hiển thị hoặc crash app khác, nên ShellExecutor
         * KHÔNG bao giờ gọi lệnh đó.
         */
        public final int simulatedDpiBoostPercent;

        public Profile(int general, int scope, int dpi, String tier, int simulatedDpiBoostPercent) {
            this.general = general;
            this.scope = scope;
            this.dpi = dpi;
            this.tier = tier;
            this.simulatedDpiBoostPercent = simulatedDpiBoostPercent;
        }

        @Override
        public String toString() {
            return general + " / " + scope + " / " + dpi + "  (" + tier + ")"
                    + "\nBuff DPI (giả lập): +" + simulatedDpiBoostPercent + "%"
                    + " — chỉ mang tính hiển thị, không đổi độ phân giải/density thật của máy.";
        }
    }

    /** Mức buff DPI giả lập theo tier — chỉ để hiển thị, xem ghi chú ở Profile */
    private static int simulatedDpiBoostFor(String tier) {
        switch (tier) {
            case "Cao cấp":     return 25;
            case "Khá":         return 20;
            case "Trung bình":  return 15;
            default:            return 10; // Thấp
        }
    }

    /** Trả về profile đề xuất dựa trên điểm benchmark */
    public Profile getRecommendedProfile(int score) {
        if (score >= 1000) {
            return new Profile(boost(80), boost(80), boost(90), "Cao cấp", simulatedDpiBoostFor("Cao cấp"));
        } else if (score >= 700) {
            return new Profile(boost(60), boost(60), boost(70), "Khá", simulatedDpiBoostFor("Khá"));
        } else if (score >= 500) {
            return new Profile(boost(40), boost(40), boost(50), "Trung bình", simulatedDpiBoostFor("Trung bình"));
        } else {
            return new Profile(boost(20), boost(20), boost(30), "Thấp", simulatedDpiBoostFor("Thấp"));
        }
    }
}
