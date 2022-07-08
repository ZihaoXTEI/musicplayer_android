package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.example.musicplayer.bean.LrcContent;
import com.example.musicplayer.bean.MediaInfo;
import com.example.musicplayer.loader.LyricsLoader;
import com.example.musicplayer.task.AudioPlayTask;
import com.example.musicplayer.util.MediaUtil;
import com.example.musicplayer.widget.AudioController;
import com.example.musicplayer.widget.AudioController.OnSeekChangeListener;

import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity implements
        Animator.AnimatorListener, OnSeekChangeListener{
    private static final String TAG = "MusicDetailActivity";
    private MediaInfo music;            // 声明一个媒体信息对象
    private TextView tv_music;          // 声明一个用于展示歌词内容的文本视图对象
    private AudioController ac_play;    // 声明一个音频控制条对象
    private LyricsLoader loader;        // 声明一个歌词加载器对象
    private ArrayList<LrcContent> lrcList;      // 歌词内容队列
    private MainApplication app;        // 声明一个全局应用对象
    private Handler handler = new Handler();    // 声明一个处理器对象
    private int frequence = 8000;       // 音轨的频率
    private int channel = AudioFormat.CHANNEL_IN_STEREO; // 音轨的声道
    private int format = AudioFormat.ENCODING_PCM_16BIT; // 音轨的格式

    private int count = 0;              // 已经滚动的歌词行数
    private float currentHeight = 0;    // 当前已经滚动的高度
    private float lineHeight = 0;       // 每行歌词的高度
    private int prePos = -1, nextPos = 0;   // 上一行歌词与下一行歌词的位置
    private String lrcStr;                  // 当前行的歌词文本
    private ObjectAnimator animTranY;       // 声明一个用于歌词滚动的属性动画对象

    private static DetailActivity detailActivity;

    public static DetailActivity getInstance() {
        return detailActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        detailActivity=this;
        // 从前一个页面传来的意图中获取媒体信息
        music = getIntent().getParcelableExtra("music");
        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText(music.getTitle());
        TextView tv_artist = findViewById(R.id.tv_artist);
        tv_artist.setText(music.getArtist());
        // 从布局文件中获取名叫ac_play的音频控制条
        ac_play = findViewById(R.id.ac_play);
        // 给音频控制条设置拖动变更监听器
        ac_play.setOnSeekChangeListener(this);
        // 获取全局应用的唯一实例
        app = MainApplication.getInstance();
        initLrc();              // 初始化歌词内容
        playMusic();            // 开始播放音乐
        initPauseReceiver();    // 初始化音乐暂停/恢复的广播接收器
    }

    //Activity页面刷新方法
    public void refresh(MediaInfo mediaInfo) {
        // 从前一个页面传来的意图中获取媒体信息
        music = mediaInfo;
        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText(music.getTitle());
        TextView tv_artist = findViewById(R.id.tv_artist);
        tv_artist.setText(music.getArtist());

        initLrc();              // 初始化歌词内容
        playMusic();            // 开始播放音乐
        initPauseReceiver();    // 初始化音乐暂停/恢复的广播接收器
    }

    // 初始化音乐暂停/恢复的广播接收器
    private void initPauseReceiver() {
        // 创建一个暂停/恢复播放的广播接收器
        pauseReceiver = new PauseReceiver();
        // 创建一个意图过滤器，只处理指定事件来源的广播
        IntentFilter filter = new IntentFilter(getString(R.string.pause_event));
        // 注册广播接收器，注册之后才能正常接收广播
        registerReceiver(pauseReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除所有的处理器任务
        handler.removeCallbacksAndMessages(null);
        // 注销广播接收器，注销之后就不再接收广播
        unregisterReceiver(pauseReceiver);
    }

    // 初始化歌词内容
    private void initLrc() {
        tv_music = findViewById(R.id.tv_music);
        // 获得歌词加载器的唯一实例
        loader = LyricsLoader.getInstance(music.getUrl());
        // 通过歌词加载器获取歌词内容队列
        lrcList = loader.getLrcList();
        // 计算一行歌词的高度
        lineHeight = Math.round(getTextHeight("好", tv_music.getTextSize()));
    }


    // 开始播放音乐
    private void playMusic() {
        Log.d(TAG, "song=" + music.getTitle());
        if (MediaUtil.getExtendName(music.getUrl()).equals("pcm")) { // 音轨格式
            ac_play.setVisibility(View.GONE);
            // 创建一个音轨播放线程
            AudioPlayTask playTask = new AudioPlayTask();
            // 执行音轨播放线程的事务处理
            playTask.execute(music.getUrl(), "" + frequence, "" + channel, "" + format);
        } else { // 非音轨格式
            // 将歌词内容队列从上向下依次展开
            if (loader.getLrcList() != null && lrcList.size() > 0) {
                lrcStr = "";
                for (int i = 0; i < lrcList.size(); i++) {
                    LrcContent item = lrcList.get(i);
                    lrcStr = lrcStr + item.getLrcStr() + "\n";
                }
                tv_music.setText(lrcStr);
                // 刚进入播放页面时，让歌词显示淡入动画
                tv_music.setAnimation(AnimationUtils.loadAnimation(this, R.anim.alpha_music));
            }
            if (app.mFilePath == null || !app.mFilePath.equals(music.getUrl())) { // 首次播放音乐，或者音乐发生变更
                // 下面启动音乐播放服务。具体的播放操作在后台服务中完成
                Intent intent = new Intent(this, MusicService.class);
                intent.putExtra("is_play", true);
                intent.putExtra("music", music);
                startService(intent);
                // 延迟150毫秒后启动歌词刷新任务
                handler.postDelayed(mRefreshLrc, 150);
            } else { // 音乐已经在播放当中了
                // 触发音乐播放进度的变更处理
                onMusicSeek(0, app.mMediaPlayer.getCurrentPosition());
            }
            // 延迟100毫秒后启动控制条刷新任务
            handler.postDelayed(mRefreshCtrl, 100);
        }
    }

    // 定义一个控制条刷新任务
    private Runnable mRefreshCtrl = new Runnable() {
        @Override
        public void run() {
            // 设置音频控制条的播放进度
            ac_play.setCurrentTime(app.mMediaPlayer.getCurrentPosition(), 0);
            if (app.mMediaPlayer.getCurrentPosition() >= app.mMediaPlayer.getDuration()) { // 播放结束
                // 重置音频控制条的播放进度
                ac_play.setCurrentTime(0, 0);
            }
            // 延迟500毫秒后再次启动控制条刷新任务
            handler.postDelayed(this, 500);
        }
    };

    // 定义一个歌词刷新任务
    private Runnable mRefreshLrc = new Runnable() {
        @Override
        public void run() {
            if (loader.getLrcList() == null || lrcList.size() <= 0) {
                return;
            }
            // 计算每行歌词的动画
            int offset = lrcList.get(count).getLrcTime()
                    - ((count == 0) ? 0 : lrcList.get(count - 1).getLrcTime()) - 50;
            if (offset <= 0) {
                return;
            }
            // 开始播放该行的歌词滚动动画
            startAnimation(currentHeight - lineHeight, offset);
        }
    };

    // 在指定歌词处开始播放滚动动画
    public void startAnimation(float aimHeight, int offset) {
        // 构造一个在纵轴上平移的属性动画
        animTranY = ObjectAnimator.ofFloat(tv_music, "translationY", currentHeight, aimHeight);
        animTranY.setDuration(offset);      // 设置动画的播放时长
        animTranY.setRepeatCount(0);        // 重播次数为0表示只播放一次
        animTranY.addListener(this);        // 给属性动画添加动画事件监听器
        animTranY.start();                  // 开始播放属性动画
        currentHeight = aimHeight;
        if (!app.mMediaPlayer.isPlaying()) { // 媒体播放器不在播放
            // 延迟若干时间后启动歌词暂停滚动任务
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    animTranY.pause(); // 歌词滚动动画暂停播放
                }
            }, offset + 100);
        }
    }

    // 在属性动画开始播放时触发
    public void onAnimationStart(Animator animation) {}

    // 在属性动画结束播放时触发
    public void onAnimationEnd(Animator animation) {
        if (count < lrcList.size()) {
            nextPos = lrcStr.indexOf("\n", prePos + 1);
            // 创建一个可变字符串
            SpannableString spanText = new SpannableString(lrcStr);
            // 高亮显示当前正在播放的歌词文本
            spanText.setSpan(new ForegroundColorSpan(Color.RED), prePos + 1,
                    nextPos > 0 ? nextPos : lrcStr.length() - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // 增大显示当前正在播放的歌词文本
            spanText.setSpan(new AbsoluteSizeSpan(110), prePos + 1,
                    nextPos > 0 ? nextPos : lrcStr.length() - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            count++;
            // 在文本视图中显示高亮处理后的可变字符串
            tv_music.setText(spanText);
            if (nextPos > 0 && nextPos < lrcStr.length() - 1) {
                prePos = lrcStr.indexOf("\n", nextPos);
                // 延迟50毫秒后启动歌词刷新任务
                handler.postDelayed(mRefreshLrc, 50);
            }
        }
    }

    // 在属性动画取消播放时触发
    public void onAnimationCancel(Animator animation) {}

    // 在属性动画重复播放时触发
    public void onAnimationRepeat(Animator animation) {}

    // 在音乐播放进度拖动时触发
    public void onMusicSeek(int current, int seekto) {
        if (animTranY != null) {
            animTranY.cancel(); // 歌词滚动动画取消播放
        }
        // 移除歌词刷新任务
        handler.removeCallbacks(mRefreshLrc);
        int i;
        for (i = 0; i < lrcList.size(); i++) {
            LrcContent item = lrcList.get(i);
            if (item.getLrcTime() > seekto) {
                break;
            }
        }
        count = i;
        prePos = -1;
        nextPos = 0;
        if (count > 0) {
            for (int j = 0; j < count; j++) {
                nextPos = lrcStr.indexOf("\n", prePos + 1);
                prePos = lrcStr.indexOf("\n", nextPos);
            }
        }
        // 在指定歌词处开始播放滚动动画
        startAnimation(-lineHeight * i, 100);
    }

    // 在音乐暂停播放时触发
    public void onMusicPause() {
        animTranY.pause(); // 歌词滚动动画暂停播放
    }

    // 在音乐恢复播放时触发
    public void onMusicResume() {
        animTranY.resume(); // 歌词滚动动画恢复播放
    }


    // 声明一个暂停/恢复播放的广播接收器
    private PauseReceiver pauseReceiver;
    // 定义一个广播接收器，用于处理音乐的暂停/恢复播放事件
    public class PauseReceiver extends BroadcastReceiver {
        // 一旦接收到暂停/恢复播放的广播，马上触发接收器的onReceive方法
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (animTranY.isPaused()) { // 歌词动画暂停滚动
                    animTranY.resume(); // 恢复滚动歌词
                } else { // 歌词动画正在滚动
                    animTranY.pause(); // 暂停滚动歌词
                }
            }
        }
    }

    // 获取指定文本的高度
    public static float getTextHeight(String text, float textSize) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);        // 设置画笔的文本大小
        Paint.FontMetrics fm = paint.getFontMetrics(); // 获取画笔默认字体的度量衡
        return fm.descent - fm.ascent;      // 返回文本自身的高度
    }

}

