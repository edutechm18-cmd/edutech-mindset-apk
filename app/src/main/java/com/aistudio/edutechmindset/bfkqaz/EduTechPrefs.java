package com.aistudio.edutechmindset.bfkqaz;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EduTechPrefs {

    private static final String PREFS_NAME = "edutech_mindset_prefs";
    private static final String KEY_LAST_URL = "last_url";
    private static final String KEY_LAST_TITLE = "last_title";
    private static final String KEY_HISTORY = "history_json";
    private static final String KEY_FAVORITES = "favorites_json";
    private static final String KEY_FLASHCARDS = "flashcards_json";
    private static final String KEY_EXAM_NAME = "exam_name";
    private static final String KEY_EXAM_DATE = "exam_date_ms";
    private static final String KEY_STUDY_DATE = "study_date";
    private static final String KEY_STUDY_MINUTES = "study_minutes_today";
    private static final String KEY_PDF_PAGES_READ = "pdf_pages_read";
    private static final String KEY_PDF_PROGRESS = "pdf_progress_json";
    private static final String KEY_SOUND_MUTED = "sound_muted";
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    private static final String KEY_EYE_FILTER = "eye_filter_on";
    private static final String KEY_TEXT_ZOOM = "text_zoom";
    private static final String KEY_COMPACT_ASSISTANT = "compact_assistant";
    private static final int MAX_HISTORY = 10;

    private final SharedPreferences prefs;

    public EduTechPrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getLastUrl() {
        return prefs.getString(KEY_LAST_URL, "");
    }

    public String getLastTitle() {
        return prefs.getString(KEY_LAST_TITLE, "");
    }

    public void saveLastLesson(String url, String title) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        String safeTitle = title != null ? title : url;
        prefs.edit()
                .putString(KEY_LAST_URL, url)
                .putString(KEY_LAST_TITLE, safeTitle)
                .apply();
        addToHistory(url, safeTitle);
    }

    private void addToHistory(String url, String title) {
        JSONArray current = getHistory();
        JSONArray updated = new JSONArray();
        try {
            JSONObject entry = new JSONObject();
            entry.put("url", url);
            entry.put("title", title);
            entry.put("timestamp", System.currentTimeMillis());
            updated.put(entry);
            for (int i = 0; i < current.length() && updated.length() < MAX_HISTORY; i++) {
                JSONObject item = current.optJSONObject(i);
                if (item != null && !url.equals(item.optString("url"))) {
                    updated.put(item);
                }
            }
            prefs.edit().putString(KEY_HISTORY, updated.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public JSONArray getHistory() {
        String raw = prefs.getString(KEY_HISTORY, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public boolean toggleFavorite(String url, String title) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        JSONArray current = getFavorites();
        JSONArray updated = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item != null && url.equals(item.optString("url"))) {
                removed = true;
            } else if (item != null) {
                updated.put(item);
            }
        }
        if (!removed) {
            try {
                JSONObject entry = new JSONObject();
                entry.put("url", url);
                entry.put("title", title != null ? title : url);
                entry.put("timestamp", System.currentTimeMillis());
                updated.put(entry);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_FAVORITES, updated.toString()).apply();
        return !removed;
    }

    public JSONArray getFavorites() {
        String raw = prefs.getString(KEY_FAVORITES, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public boolean isFavorite(String url) {
        if (url == null) {
            return false;
        }
        JSONArray favorites = getFavorites();
        for (int i = 0; i < favorites.length(); i++) {
            JSONObject item = favorites.optJSONObject(i);
            if (item != null && url.equals(item.optString("url"))) {
                return true;
            }
        }
        return false;
    }

    public JSONArray getFlashcards() {
        String raw = prefs.getString(KEY_FLASHCARDS, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public void saveFlashcards(JSONArray flashcards) {
        String data = flashcards != null ? flashcards.toString() : "[]";
        prefs.edit().putString(KEY_FLASHCARDS, data).apply();
    }

    public void saveFlashcards(String flashcardsJson) {
        prefs.edit().putString(KEY_FLASHCARDS, flashcardsJson != null ? flashcardsJson : "[]").apply();
    }

    public void setExamCountdown(String examName, long examDateMillis) {
        prefs.edit()
                .putString(KEY_EXAM_NAME, examName != null ? examName : "")
                .putLong(KEY_EXAM_DATE, examDateMillis)
                .apply();
    }

    public String getExamName() {
        return prefs.getString(KEY_EXAM_NAME, "");
    }

    public int getExamDaysLeft() {
        long target = prefs.getLong(KEY_EXAM_DATE, 0L);
        if (target <= 0L) {
            return 0;
        }
        long diff = target - System.currentTimeMillis();
        if (diff <= 0L) {
            return 0;
        }
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }

    private String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public void addStudyMinutes(int minutes) {
        if (minutes <= 0) {
            return;
        }
        String today = getTodayKey();
        int current = getTodayMinutes();
        prefs.edit()
                .putString(KEY_STUDY_DATE, today)
                .putInt(KEY_STUDY_MINUTES, current + minutes)
                .apply();
    }

    public int getTodayMinutes() {
        String today = getTodayKey();
        String savedDate = prefs.getString(KEY_STUDY_DATE, "");
        if (!today.equals(savedDate)) {
            return 0;
        }
        return prefs.getInt(KEY_STUDY_MINUTES, 0);
    }

    public int getPdfPagesRead() {
        return prefs.getInt(KEY_PDF_PAGES_READ, 0);
    }

    public void addPdfPagesRead(int pages) {
        if (pages <= 0) {
            return;
        }
        int current = getPdfPagesRead();
        prefs.edit().putInt(KEY_PDF_PAGES_READ, current + pages).apply();
    }

    public void savePdfPage(String pdfUrl, int page) {
        if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject map = new JSONObject(prefs.getString(KEY_PDF_PROGRESS, "{}"));
            map.put(pdfUrl, page);
            prefs.edit().putString(KEY_PDF_PROGRESS, map.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public int getPdfPage(String pdfUrl) {
        if (pdfUrl == null) {
            return 0;
        }
        try {
            JSONObject map = new JSONObject(prefs.getString(KEY_PDF_PROGRESS, "{}"));
            return map.optInt(pdfUrl, 0);
        } catch (JSONException e) {
            return 0;
        }
    }

    public boolean isSoundMuted() {
        return prefs.getBoolean(KEY_SOUND_MUTED, false);
    }

    public void setSoundMuted(boolean muted) {
        prefs.edit().putBoolean(KEY_SOUND_MUTED, muted).apply();
    }

    public boolean isKeepScreenOn() {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, false);
    }

    public void setKeepScreenOn(boolean keepOn) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepOn).apply();
    }

    public boolean isEyeFilterOn() {
        return prefs.getBoolean(KEY_EYE_FILTER, false);
    }

    public void setEyeFilterOn(boolean on) {
        prefs.edit().putBoolean(KEY_EYE_FILTER, on).apply();
    }

    public int getTextZoom() {
        return prefs.getInt(KEY_TEXT_ZOOM, 100);
    }

    public void setTextZoom(int zoom) {
        prefs.edit().putInt(KEY_TEXT_ZOOM, zoom).apply();
    }

    public boolean isCompactAssistant() {
        return prefs.getBoolean(KEY_COMPACT_ASSISTANT, false);
    }

    public void setCompactAssistant(boolean compact) {
        prefs.edit().putBoolean(KEY_COMPACT_ASSISTANT, compact).apply();
    }

    public String exportBackupJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("lastUrl", getLastUrl());
            root.put("lastTitle", getLastTitle());
            root.put("history", getHistory());
            root.put("favorites", getFavorites());
            root.put("flashcards", getFlashcards());
            root.put("examName", getExamName());
            root.put("examDateMs", prefs.getLong(KEY_EXAM_DATE, 0L));
            root.put("studyDate", prefs.getString(KEY_STUDY_DATE, ""));
            root.put("todayMinutes", getTodayMinutes());
            root.put("pdfPagesRead", getPdfPagesRead());
            root.put("pdfProgress", new JSONObject(prefs.getString(KEY_PDF_PROGRESS, "{}")));
            root.put("soundMuted", isSoundMuted());
            root.put("keepScreenOn", isKeepScreenOn());
            root.put("eyeFilterOn", isEyeFilterOn());
            root.put("textZoom", getTextZoom());
            root.put("compactAssistant", isCompactAssistant());
            return root.toString(2);
        } catch (JSONException e) {
            return "{}";
        }
    }
}
