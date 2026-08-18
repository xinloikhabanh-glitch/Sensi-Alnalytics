package com.sensi.analytics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

/**
 * DeviceAnalyzer thu thập thông tin phần cứng cơ bản của thiết bị
 * (Model, RAM, số nhân CPU) và tính điểm benchmark theo công thức:
 *
 *      Điểm = RAM(GB) x 100 + Số nhân CPU x 50 + 300
 *
 * Đây là công thức ước lượng đơn giản, không phải benchmark chuyên sâu
 * (không đo xung nhịp thực tế, GPU, hay điểm AnTuTu).
 */
public class DeviceAnalyzer {

    /** Đối tượng chứa toàn bộ thông tin thiết bị đã phân tích */
    public static class DeviceInfo {
        public String model;
        public String manufacturer;
        public int ramGb;
        public int cpuCores;
        public int score;

        @Override
        public String toString() {
            return "Model: " + manufacturer + " " + model +
                    "\nRAM: " + ramGb + " GB" +
                    "\nCPU: " + cpuCores + " nhân" +
                    "\nĐiểm benchmark: " + score;
        }
    }

    private final Context context;

    public DeviceAnalyzer(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Lấy dung lượng RAM tổng của thiết bị, làm tròn theo GB */
    private int getTotalRamGb() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(memInfo);
            double gb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0);
            // Làm tròn lên vì nhà sản xuất thường quảng cáo RAM làm tròn (VD: 3.7GB thực -> 4GB)
            return (int) Math.ceil(gb);
        }
        return 4; // giá trị mặc định an toàn nếu không lấy được
    }

    /** Lấy số nhân CPU khả dụng */
    private int getCpuCores() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(cores, 1);
    }

    /** Thực hiện phân tích đầy đủ và trả về đối tượng DeviceInfo */
    public DeviceInfo analyze() {
        DeviceInfo info = new DeviceInfo();
        info.model = Build.MODEL;
        info.manufacturer = Build.MANUFACTURER;
        info.ramGb = getTotalRamGb();
        info.cpuCores = getCpuCores();
        info.score = calculateScore(info.ramGb, info.cpuCores);
        return info;
    }

    /** Công thức tính điểm benchmark theo yêu cầu */
    public int calculateScore(int ramGb, int cpuCores) {
        return ramGb * 100 + cpuCores * 50 + 300;
    }
}
