package com.example.musicplayer;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import com.example.musicplayer.util.NotifyUtil;

public class MainApplication extends Application {
    // 声明一个当前应用的静态实例
    private static MainApplication mApp;
    public MediaPlayer mMediaPlayer;    // 声明一个媒体播放器对象
    public String mSong;                // 音乐名称
    public String mFilePath;            // 文件路径
    public Bitmap mAlbumBip;            // 专辑图片
    public int mPlayMode = 0;
    private static Context mContext;

    private Intent mResultIntent = null;    // 结果意图
    private int mResultCode = 0;            // 结果代码
    private MediaProjectionManager mMpMgr;  // 声明一个媒体投影管理器对象

    // 利用单例模式获取当前应用的唯一实例
    public static MainApplication getInstance() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 在打开应用时对静态的应用实例赋值
        mApp = this;
        // 创建一个媒体播放器
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setLooping(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotifyUtil.createNotifyChannel(this, getString(R.string.app_name));
        }
        //获取context
        mContext = getApplicationContext();
    }

    public Intent getResultIntent() {
        return mResultIntent;
    }

    public void setResultIntent(Intent mResultIntent) {
        this.mResultIntent = mResultIntent;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public void setResultCode(int mResultCode) {
        this.mResultCode = mResultCode;
    }

    public MediaProjectionManager getMpMgr() {
        return mMpMgr;
    }

    public void setMpMgr(MediaProjectionManager mMpMgr) {
        this.mMpMgr = mMpMgr;
    }

    //创建一个静态的方法，以便获取context对象
    public static Context getContext(){
        return mContext;
    }
}
