package com.sensi.analytics;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    // ==== View ====
    private TextView tvDeviceInfo;
    private TextView tvResult;
    private TextView tvHistory;
    private ProgressBar progressBar;
    private Button btnAnalyze;
    private Button btnApply;
    private Button btnRestore;

    // ==== Logic ====
    private DeviceAnalyzer deviceAnalyzer;
    private ProfileManager profileManager;
    private OptimizationHistory history;
    private ShellExecutor shellExecutor;
    private AnalyticsService analyticsService;

    // Lưu kết quả phân tích gần nhất để dùng khi bấm "Áp dụng tối ưu"
    private DeviceAnalyzer.DeviceInfo lastDeviceInfo;
    private ProfileManager.Profile lastProfile;

    // Lắng nghe kết quả xin quyền Shizuku
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (requestCode == ShellExecutor.REQUEST_CODE) {
                    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Đã cấp quyền Shizuku", Toast.LENGTH_SHORT).show();
                        shellExecutor.bindService();
                    } else {
                        Toast.makeText(this, "Bạn cần cấp quyền Shizuku để áp dụng tối ưu", Toast.LENGTH_LONG).show();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupLogic();
        setupListeners();

        renderHistory();
    }

    private void bindViews() {
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvResult = findViewById(R.id.tvResult);
        tvHistory = findViewById(R.id.tvHistory);
        progressBar = findViewById(R.id.progressBar);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnApply = findViewById(R.id.btnApply);
        btnRestore = findViewById(R.id.btnRestore);
    }

    private void setupLogic() {
        deviceAnalyzer = new DeviceAnalyzer(this);
        profileManager = new ProfileManager();
        history = new OptimizationHistory(this);

        shellExecutor = new ShellExecutor(this, new ShellExecutor.Callback() {
            @Override
            public void onServiceReady() {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Đã sẵn sàng thực thi lệnh tối ưu", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onPermissionDenied() {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Quyền Shizuku bị từ chối", Toast.LENGTH_SHORT).show());
            }
        });
        analyticsService = new AnalyticsService(shellExecutor);

        Shizuku.addRequestPermissionResultListener(permissionListener);
    }

    private void setupListeners() {
        btnAnalyze.setOnClickListener(v -> runAnalysis());
        btnApply.setOnClickListener(v -> applyOptimization());
        btnRestore.setOnClickListener(v -> restoreDefaults());
    }

    /** Nút "Phân tích thiết bị": hiển thị loading rồi tính toán kết quả */
    private void runAnalysis() {
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);
        tvResult.setText("");

        // Giả lập thời gian phân tích để người dùng thấy rõ thanh loading
        // (bản thân phép tính chỉ mất vài mili-giây)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            lastDeviceInfo = deviceAnalyzer.analyze();
            lastProfile = profileManager.getRecommendedProfile(lastDeviceInfo.score);

            tvDeviceInfo.setText(lastDeviceInfo.toString());
            tvResult.setText("Đề xuất (Tổng quát / Ngắm / DPI):\n" + lastProfile.toString());

            progressBar.setVisibility(View.GONE);
            btnAnalyze.setEnabled(true);
            btnApply.setEnabled(true);
        }, 1200);
    }

    /** Nút "Áp dụng tối ưu": xin quyền Shizuku (nếu chưa có) rồi chạy lệnh */
    private void applyOptimization() {
        if (lastProfile == null) {
            Toast.makeText(this, "Hãy phân tích thiết bị trước", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!shellExecutor.isShizukuAvailable()) {
            Toast.makeText(this, "Shizuku chưa chạy. Vui lòng khởi động Shizuku trước.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!shellExecutor.hasPermission()) {
            shellExecutor.requestPermission();
            return;
        }

        if (!shellExecutor.isServiceReady()) {
            shellExecutor.bindService();
            Toast.makeText(this, "Đang kết nối dịch vụ, hãy bấm Áp dụng lại sau 1-2 giây", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String log = analyticsService.applyOptimization(lastProfile);

            history.addEntry(lastDeviceInfo.score, lastProfile);
            renderHistory();

            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.note_apply_disclaimer), Toast.LENGTH_LONG).show();
        }, 800);
    }

    /** Nút "Phục hồi cài đặt": đưa các thông số về mặc định gốc */
    private void restoreDefaults() {
        if (!shellExecutor.isServiceReady()) {
            Toast.makeText(this, "Cần cấp quyền Shizuku trước khi phục hồi", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            analyticsService.restoreDefaults();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Đã phục hồi cài đặt mặc định", Toast.LENGTH_SHORT).show();
        }, 500);
    }

    /** Hiển thị 3 lần lịch sử gần nhất lên giao diện */
    private void renderHistory() {
        List<OptimizationHistory.Entry> entries = history.getEntries();
        if (entries.isEmpty()) {
            tvHistory.setText("Chưa có lịch sử tối ưu nào.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (OptimizationHistory.Entry e : entries) {
            sb.append("• ").append(e.timestamp)
                    .append(" - Điểm: ").append(e.score)
                    .append(" - ").append(e.profileSummary)
                    .append("\n");
        }
        tvHistory.setText(sb.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        shellExecutor.unbindService();
    }
}
