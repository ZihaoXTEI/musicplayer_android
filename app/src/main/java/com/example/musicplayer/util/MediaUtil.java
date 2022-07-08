package com.example.musicplayer.util;

import android.annotation.SuppressLint;
import java.util.Locale;

@SuppressLint("DefaultLocale")
public class MediaUtil {
    private final static String TAG = "MediaUtil";

    // 从文件的完整路径中截出扩展名
    public static String getExtendName(String path) {
        int pos = path.lastIndexOf(".");
        return path.substring(pos + 1).toLowerCase(Locale.getDefault());
    }

    // 格式化播放时长
    public static String formatDuration(int milliseconds) {
        int seconds = milliseconds / 1000;
        int hour = seconds / 3600;
        int minute = seconds / 60;
        int second = seconds % 60;
        String str;
        if (hour > 0) {
            str = String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            str = String.format("%02d:%02d", minute, second);
        }
        return str;
    }

}
