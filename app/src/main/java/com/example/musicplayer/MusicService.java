package com.example.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

import com.example.musicplayer.bean.MediaInfo;
import com.example.musicplayer.loader.MusicLoader;
import com.example.musicplayer.util.DateUtil;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private int position;
    private MainApplication app;        // 声明一个全局应用对象
    private DetailActivity detailActivity;
    private String PAUSE_EVENT = "";    // “暂停/继续”事件的标识串
    private boolean isPlaying = true;   // 是否正在播放
    private MediaInfo music;            // 音乐信息
    private Handler handler = new Handler(); // 声明一个处理器对象

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (app == null) {
            app = MainApplication.getInstance();
        }
        if(detailActivity == null){
            detailActivity = DetailActivity.getInstance();
        }
        // 从资源文件中获取“暂停/继续”事件的标识串
        PAUSE_EVENT = getString(R.string.pause_event);
        pauseReceiver = new PauseReceiver();
        // 创建一个意图过滤器，只处理指定事件来源的广播
        IntentFilter filter = new IntentFilter(PAUSE_EVENT);
        registerReceiver(pauseReceiver, filter);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        // 从意图中获取是否正在播放的字段
        isPlaying = intent.getBooleanExtra("is_play", true);
        // 从意图中获取音乐信息对象
        music = intent.getParcelableExtra("music");
        position = intent.getIntExtra("position",0);
        app.mSong = music.getTitle();
        app.mAlbumBip = music.getAlbumBip();
        final String file_path = music.getUrl();
        Log.d(TAG, "isPlaying=" + isPlaying + ", file_path=" + file_path);
        if (isPlaying && app.mFilePath != null && !app.mFilePath.equals(file_path)) { // 播放音乐发生变更
            stopPlayer(); // 播放新乐曲之前，先停止旧乐曲
            // 延迟100毫秒后启动新乐曲的播放任务
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    app.mFilePath = file_path;
                    startPlayer();      // 开始播放音乐
                }
            }, 100);
        } else if (!isPlaying) {        // 不要播放
            app.mFilePath = file_path;
            stopPlayer();
        } else if (isPlaying && !app.mMediaPlayer.isPlaying()) { // 需要播放
            app.mFilePath = file_path;
            startPlayer();
        }
        // 延迟200毫秒后启动音乐播放的刷新任务
        handler.postDelayed(mPlay, 200);
        return START_STICKY;
    }

    // 开始播放音乐
    private void startPlayer() {
        try {
            app.mMediaPlayer.reset(); // 重置媒体播放器
            //app.mMediaPlayer.setVolume(0.5f, 0.5f); // 设置音量，可选
            // 设置音频流的类型为音乐
            app.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置媒体数据的文件路径
            app.mMediaPlayer.setDataSource(app.mFilePath);
            app.mMediaPlayer.prepare();         // 媒体播放器准备就绪
            app.mMediaPlayer.start();           // 媒体播放器开始播放
            app.mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if(!app.mMediaPlayer.isPlaying()){
                        setPlayMode();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 停止播放音乐
    private void stopPlayer() {
        if (app.mMediaPlayer.isPlaying()) { // 媒体播放器正在播放
            app.mMediaPlayer.stop(); // 媒体播放器停止播放
        }
    }

    //播放模式
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setPlayMode() {
        Log.d(TAG,"position"+position);
        if (app.mPlayMode == 0)//全部循环
        {
            if (position == MusicLoader.getMusicList().size() - 1)
            {
                position = 0;// 第一首
                stopPlayer();
                //app.mMediaPlayer.reset();
                app.mSong=MusicLoader.getMusicList().get(position).getTitle();
                app.mFilePath=MusicLoader.getMusicList().get(position).getUrl();
                app.mAlbumBip = music.getAlbumBip();
                detailActivity.refresh(MusicLoader.getMusicList().get(position));

                // 延迟100毫秒后启动新乐曲的播放任务
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPlayer(); // 开始播放音乐
                    }
                }, 100);

            } else {
                position++;
                stopPlayer();
                //app.mMediaPlayer.reset();
                app.mSong=MusicLoader.getMusicList().get(position).getTitle();
                app.mFilePath=MusicLoader.getMusicList().get(position).getUrl();
                app.mAlbumBip = music.getAlbumBip();
                detailActivity.refresh(MusicLoader.getMusicList().get(position));

                // 延迟100毫秒后启动新乐曲的播放任务
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPlayer(); // 开始播放音乐
                    }
                }, 100);
            }
        } else if (app.mPlayMode == 1)//单曲循环
        {
            //stopPlayer();
            app.mMediaPlayer.reset();
            // 延迟100毫秒后启动新乐曲的播放任务
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPlayer(); // 开始播放音乐
                }
            }, 100);
        }else if(app.mPlayMode == 2){   //顺序播放
            if (position == MusicLoader.getMusicList().size() - 1)
            {
                stopPlayer();

            } else {
                position++;
                stopPlayer();
                //app.mMediaPlayer.reset();
                app.mSong=MusicLoader.getMusicList().get(position).getTitle();
                app.mFilePath=MusicLoader.getMusicList().get(position).getUrl();
                app.mAlbumBip = music.getAlbumBip();
                detailActivity.refresh(MusicLoader.getMusicList().get(position));

                // 延迟100毫秒后启动新乐曲的播放任务
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPlayer(); // 开始播放音乐
                    }
                }, 100);
            }
        }
    }

    @Override
    public void onDestroy() {
        // 注销广播接收器，注销之后就不再接收广播
        unregisterReceiver(pauseReceiver);
        super.onDestroy();
    }

    // 定义一个音乐播放的刷新任务
    private Runnable mPlay = new Runnable() {
        @Override
        public void run() {
            int process = 0;
            if (app.mMediaPlayer.getDuration() > 0) { // 正在播放，则刷新播放进度
                process = app.mMediaPlayer.getCurrentPosition() * 100 / app.mMediaPlayer.getDuration();
            }
            // 延迟1秒后再次启动音乐播放的刷新任务
            handler.postDelayed(this, 1000);
            Notification notify = getNotify(MusicService.this, PAUSE_EVENT,
                    app.mSong, isPlaying, process, app.mMediaPlayer.getCurrentPosition());
            // 把服务推送到前台的通知栏
            startForeground(2, notify);
        }
    };

    private Notification getNotify(Context ctx, String event, String song, boolean isPlaying, int progress, long time) {
        // 创建一个广播事件的意图
        Intent intent1 = new Intent(event);
        // 创建一个用于广播的延迟意图
        PendingIntent broadIntent = PendingIntent.getBroadcast(
                ctx, R.string.app_name, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        // 根据布局文件notify_music.xml生成远程视图对象
        RemoteViews notify_music = new RemoteViews(ctx.getPackageName(), R.layout.notify_music);
        if (isPlaying && app.mMediaPlayer.isPlaying()) {                                        // 正在播放
            notify_music.setImageViewResource(R.id.iv_play, R.mipmap.ic_play_bar_btn_pause);    // 设置暂停图标
            notify_music.setTextViewText(R.id.tv_play, song);                                   // 设置文本文字
            notify_music.setTextViewText(R.id.tv_time, DateUtil.formatTime(time));              // 设置已播放的时间
        } else { // 不在播放
            notify_music.setImageViewResource(R.id.iv_play, R.mipmap.ic_play_bar_btn_play);
            notify_music.setTextViewText(R.id.tv_play, song + " 暂停播放");
            notify_music.setTextViewText(R.id.tv_time, DateUtil.formatTime(time));
        }

        notify_music.setOnClickPendingIntent(R.id.iv_play, broadIntent);
        // 创建一个跳转到活动页面的意图
        Intent intent2 = new Intent(ctx, DetailActivity.class);
        // 往意图中存入音乐信息
        intent2.putExtra("music", music);
        // 创建一个用于页面跳转的延迟意图
        PendingIntent clickIntent = PendingIntent.getActivity(ctx,
                R.string.app_name, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
        // 创建一个通知消息的构造器
        Notification.Builder builder = new Notification.Builder(ctx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(ctx, getString(R.string.app_name));
        }
        builder.setContentIntent(clickIntent)       // 设置内容的点击意图
                .setContent(notify_music)           // 设置内容视图
                .setTicker(song)                    // 设置状态栏里面的提示文本
                .setSmallIcon(R.drawable.icon_other);              // 设置状态栏里的小图标
        // 根据消息构造器构建并返回一个通知对象
        return builder.build();
    }

    // 声明一个暂停/恢复播放的广播接收器
    private PauseReceiver pauseReceiver;
    // 定义一个广播接收器，用于处理音乐的暂停/恢复播放事件
    public class PauseReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (app.mMediaPlayer.isPlaying()) {     // 播放器正在播放
                    app.mMediaPlayer.pause();           // 播放器暂停播放
                } else {                                // 播放器不在播放
                    app.mMediaPlayer.start();           // 播放器开始播放
                }
            }
        }
    }

}

