package com.sensi.analytics;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import rikka.shizuku.Shizuku;

/**
 * ShellExecutor chịu trách nhiệm:
 *  1. Kiểm tra / xin quyền Shizuku
 *  2. Bind tới UserService (chạy với quyền shell)
 *  3. Cung cấp hàm run() để các phần khác của app gọi lệnh shell
 *
 * Toàn bộ lệnh thực thi ở đây là các lệnh CHỈNH SỬA CÀI ĐẶT HỆ THỐNG AN TOÀN
 * (animation scale, background process limit, trim memory, clear cache...),
 * KHÔNG chỉnh sửa file của bất kỳ ứng dụng game nào và không can thiệp vào
 * bộ nhớ tiến trình của ứng dụng khác.
 */
public class ShellExecutor {

    private static final String TAG = "ShellExecutor";
    public static final int REQUEST_CODE = 1001;

    private IUserService userService;
    private final Context context;
    private final Callback callback;

    public interface Callback {
        void onServiceReady();
        void onPermissionDenied();
    }

    public ShellExecutor(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    /** Kết nối tới UserService của Shizuku */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            userService = IUserService.Stub.asInterface(binder);
            Log.d(TAG, "UserService đã kết nối");
            if (callback != null) callback.onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            userService = null;
            Log.d(TAG, "UserService đã ngắt kết nối");
        }
    };

    /** Kiểm tra Shizuku đã chạy chưa và app đã có quyền chưa */
    public boolean isShizukuAvailable() {
        return Shizuku.pingBinder();
    }

    public boolean hasPermission() {
        return Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** Xin quyền Shizuku (kết quả trả về qua Shizuku.OnRequestPermissionResultListener) */
    public void requestPermission() {
        Shizuku.requestPermission(REQUEST_CODE);
    }

    /** Bind UserService sau khi đã có quyền */
    public void bindService() {
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                new ComponentName(context, UserService.class.getName()))
                .daemon(false)
                .processNameSuffix("sensi_service")
                .debuggable(false)
                .version(1);
        Shizuku.bindUserService(args, connection);
    }

    public void unbindService() {
        try {
            Shizuku.unbindUserService(
                    new Shizuku.UserServiceArgs(new ComponentName(context, UserService.class.getName())),
                    connection, true);
        } catch (Exception e) {
            Log.w(TAG, "Không thể unbind service: " + e.getMessage());
        }
    }

    /**
     * Chạy 1 lệnh shell thông qua UserService.
     * Trả về output dạng String, hoặc thông báo lỗi nếu service chưa sẵn sàng.
     */
    public String run(String command) {
        if (userService == null) {
            return "Lỗi: UserService chưa được kết nối. Hãy cấp quyền Shizuku trước.";
        }
        try {
            return userService.execCommand(command);
        } catch (Exception e) {
            return "Lỗi khi gọi UserService: " + e.getMessage();
        }
    }

    public boolean isServiceReady() {
        return userService != null;
    }
}
