package com.example.musicplayer.loader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.util.Log;

import com.example.musicplayer.MainApplication;
import com.example.musicplayer.R;
import com.example.musicplayer.bean.MediaInfo;

import java.util.ArrayList;

/*
*读取设备中音乐文件
 */
public class MusicLoader {
    private static final String TAG = "MusicLoader";
    private static ArrayList<MediaInfo> musicList = new ArrayList<MediaInfo>();         // 音乐队列
    private static MusicLoader loader;          // 声明一个音乐加载器对象
    private static ContentResolver resolver;    // 声明一个内容解析器对象
    private static Uri audioUri = Audio.Media.EXTERNAL_CONTENT_URI; // 音频库的Uri
    private static String[] mediaColumn = new String[]{
            Audio.Media._ID,        // 编号
            Audio.Media.TITLE,      // 乐曲名
            Audio.Media.ALBUM,      // 专辑名
            Audio.Media.DURATION,   // 播放时长
            Audio.Media.SIZE,       // 文件大小
            Audio.Media.ARTIST,     // 演唱者
            Audio.Media.DATA,       // 文件路径
            Audio.Media.ALBUM_ID
    };

    // 利用单例模式获取音乐加载器的唯一实例
    public static MusicLoader getInstance(ContentResolver cResolver) {
        if (loader == null) {
            resolver = cResolver;
            loader = new MusicLoader();
        }
        return loader;
    }

    // 音乐加载器的构造函数，从系统的音频库中获取音乐文件列表
    private MusicLoader() {

        Context context = MainApplication.getContext();

        Cursor cursor = resolver.query(audioUri, mediaColumn, null, null, null);
        if (cursor == null) {
            return;
        }
        // 下面遍历结果集，并添加到音乐队列
        while (cursor.moveToNext()) {
            MediaInfo music = new MediaInfo();
            music.setId(cursor.getLong(0));
            music.setTitle(cursor.getString(1));
            music.setAlbum(cursor.getString(2));
            music.setDuration(cursor.getInt(3));
            music.setSize(cursor.getLong(4));
            music.setArtist(cursor.getString(5));
            music.setUrl(cursor.getString(6));
            music.setAlbumBip(getAlbumArt(cursor.getInt(7)));
            Log.d(TAG, music.getTitle() + " " + music.getDuration());
            musicList.add(music);
        }
        cursor.close(); // 关闭游标
    }

    // 获取音乐队列
    public static ArrayList<MediaInfo> getMusicList() {
        return musicList;
    }

    //获取专辑图片的方法
    private Bitmap getAlbumArt(int album_id) {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = MainApplication.getContext().getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
            cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        Bitmap bm = null;
        if (album_art != null) {
            bm = BitmapFactory.decodeFile(album_art);
        } else {
            bm = BitmapFactory.decodeResource(MainApplication.getContext().getResources(), R.mipmap.icon_other);
        }
        return bm;
    }

}
