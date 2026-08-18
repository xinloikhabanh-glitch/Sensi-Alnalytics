package com.sensi.analytics;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * OptimizationHistory lưu lại tối đa 3 lần phân tích/áp dụng tối ưu gần nhất,
 * dùng SharedPreferences + JSON (không cần thư viện ngoài, không cần database).
 */
public class OptimizationHistory {

    private static final String PREFS_NAME = "sensi_history_prefs";
    private static final String KEY_HISTORY = "history_json";
    private static final int MAX_ENTRIES = 3;

    private final SharedPreferences prefs;

    public static class Entry {
        public String timestamp;
        public int score;
        public String profileSummary;

        public Entry(String timestamp, int score, String profileSummary) {
            this.timestamp = timestamp;
            this.score = score;
            this.profileSummary = profileSummary;
        }
    }

    public OptimizationHistory(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Thêm 1 bản ghi mới, tự động xoá bản ghi cũ nhất nếu vượt quá 3 */
    public void addEntry(int score, ProfileManager.Profile profile) {
        List<Entry> entries = getEntries();

        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        Entry newEntry = new Entry(time, score, profile.toString());

        // Thêm vào đầu danh sách (mới nhất lên trước)
        entries.add(0, newEntry);

        // Giữ tối đa MAX_ENTRIES bản ghi
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }

        saveEntries(entries);
    }

    /** Lấy danh sách lịch sử hiện tại (mới nhất trước) */
    public List<Entry> getEntries() {
        List<Entry> result = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return result;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new Entry(
                        obj.getString("timestamp"),
                        obj.getInt("score"),
                        obj.getString("profile")
                ));
            }
        } catch (JSONException e) {
            // Nếu dữ liệu lỗi, trả về danh sách rỗng thay vì crash app
            return new ArrayList<>();
        }
        return result;
    }

    private void saveEntries(List<Entry> entries) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : entries) {
                JSONObject obj = new JSONObject();
                obj.put("timestamp", e.timestamp);
                obj.put("score", e.score);
                obj.put("profile", e.profileSummary);
                arr.put(obj);
            }
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }

    /** Xoá toàn bộ lịch sử */
    public void clear() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
