package com.example.musicplayer.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.example.musicplayer.MainApplication;
import com.example.musicplayer.R;
import com.example.musicplayer.util.DateUtil;

public class AudioController extends RelativeLayout implements OnClickListener, OnSeekBarChangeListener {
    private Context context;            // 声明一个上下文对象
    private ImageView imagePlay;        // 声明用于播放控制的图像视图对象
    private TextView currentTime;       // 声明用于展示当前时间的文本视图对象
    private TextView totalTime;         // 声明用于展示播放时长的文本视图对象
    private SeekBar seekBar;            // 声明一个拖动条对象
    private MainApplication app;        // 声明一个全局应用对象
    private int beginViewId = 0x7F24FFF0; // 临时视图的起始视图编号
    private int dip_10, dip_40;
    private int current = 0;            // 当前的播放时间，单位毫秒
    private int buffer = 0;             // 缓冲进度
    private int duration = 0;           // 音频的播放时长，单位毫秒
    private boolean isPaused = false;   // 是否暂停

    public AudioController(Context context) {
        this(context, null);
    }

    public AudioController(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        dip_10 = dip2px(context, 10);
        dip_40 = dip2px(context, 40);
        initView(); // 初始化视图
        app = MainApplication.getInstance();
    }

    // 创建一个新的文本视图
    private TextView newTextView(Context context, int id) {
        TextView tv = new TextView(context);
        tv.setId(id);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14);
        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        // 该视图在上级布局的垂直居中
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        tv.setLayoutParams(params);
        return tv;
    }

    // 初始化视图
    private void initView() {
        // 初始化一个用于播放控制（暂停/恢复）的图像视图
        imagePlay = new ImageView(context);
        LayoutParams imageParams1 = new LayoutParams(dip_40, dip_40);
        // 该视图在上级布局的垂直居中
        imageParams1.addRule(RelativeLayout.CENTER_VERTICAL);
        imagePlay.setLayoutParams(imageParams1);
        imagePlay.setId(beginViewId);
        imagePlay.setOnClickListener(this);

        // 初始化一个用于展示当前时间的文本视图
        currentTime = newTextView(context, beginViewId + 1);
        LayoutParams currentParams = (LayoutParams) currentTime.getLayoutParams();
        currentParams.setMargins(dip_10, 0, 0, 0);
        currentParams.addRule(RelativeLayout.RIGHT_OF, imagePlay.getId());
        currentTime.setLayoutParams(currentParams);

        // 初始化一个用于展示播放时长的文本视图
        totalTime = newTextView(context, beginViewId + 2);
        LayoutParams totalParams = (LayoutParams) totalTime.getLayoutParams();
        totalParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        totalTime.setLayoutParams(totalParams);

        // 创建一个新的拖动条
        seekBar = new SeekBar(context);
        LayoutParams seekParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        totalParams.setMargins(dip_10, 0, dip_10, 0);
        seekParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        seekParams.addRule(RelativeLayout.RIGHT_OF, currentTime.getId());
        seekParams.addRule(RelativeLayout.LEFT_OF, totalTime.getId());
        seekBar.setLayoutParams(seekParams);
        seekBar.setMax(100);
        seekBar.setMinimumHeight(100);
        seekBar.setThumbOffset(0);
        seekBar.setId(beginViewId + 3);
        seekBar.setOnSeekBarChangeListener(this);
    }

    // 重置播放控制条
    private void reset() {
        if (current == 0 || isPaused) { // 在开头或者处于暂停状态
            // 控制图像显示播放图标
            imagePlay.setImageResource(R.mipmap.ic_play_btn_play);
        } else { // 处于播放状态
            // 控制图像显示暂停图标
            imagePlay.setImageResource(R.mipmap.ic_play_btn_pause);
        }
        // 在文本视图上显示当前时间
        currentTime.setText(DateUtil.formatTime(current));
        // 显示拖动条的缓冲进度
        seekBar.setSecondaryProgress(buffer);
        // 在文本视图上显示播放时长
        totalTime.setText(DateUtil.formatTime(duration));
        if (duration == 0) { // 播放时长为零
            // 设置拖动条的当前进度为零
            seekBar.setProgress(0);
        } else { // 播放时长非零
            // 设置拖动条的当前进度为播放进度
            seekBar.setProgress((current == 0) ? 0 : (current * 100 / duration));
        }
    }

    // 刷新播放控制条
    private void refresh() {
        invalidate();       // 立即刷新视图
        requestLayout();    // 立即调整布局
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        removeAllViews();       // 移除所有的下级视图
        reset();                // 重置播放控制条
        addView(imagePlay);
        addView(currentTime);
        addView(totalTime);
        addView(seekBar);
    }

    // 在进度变更时触发。第三个参数为true表示用户拖动，为false表示代码设置进度
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    // 在开始拖动进度时触发
    public void onStartTrackingTouch(SeekBar seekBar) {}

    // 在停止拖动进度时触发
    public void onStopTrackingTouch(SeekBar seekBar) {
        // 计算拖动后的当前时间进度
        int time = seekBar.getProgress() * duration / 100;
        app.mMediaPlayer.seekTo(time);
        if (mSeekListener != null) {
            mSeekListener.onMusicSeek(app.mMediaPlayer.getCurrentPosition(), time);
        }
    }

    private OnSeekChangeListener mSeekListener; // 声明一个拖动条变更的监听器对象
    // 设置拖动条变更监听器
    public void setOnSeekChangeListener(OnSeekChangeListener listener) {
        mSeekListener = listener;
    }

    // 定义一个拖动条变更的监听器接口
    public interface OnSeekChangeListener {
        void onMusicSeek(int current, int seekto); // 拖动音乐到指定的播放进度
        void onMusicPause();    // 音乐暂停播放
        void onMusicResume();   // 音乐恢复播放
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == imagePlay.getId()) {
            if (app.mMediaPlayer.getDuration() <= 0) { // 播放时长为0，则不进行任何操作
                return;
            }
            if (app.mMediaPlayer.isPlaying()) {         // 播放器正在播放
                app.mMediaPlayer.pause();               // 播放器暂停播放
                isPaused = true;
                if (mSeekListener != null) {
                    mSeekListener.onMusicPause();       // 触发监听器的暂停操作
                }
            } else { // 播放器不在播放
                if ((current==0 || current>app.mMediaPlayer.getDuration()-500)
                        && mSeekListener != null) {
                    mSeekListener.onMusicSeek(0, 0);
                }
                app.mMediaPlayer.start();
                isPaused = false;
                if (mSeekListener != null) {
                    mSeekListener.onMusicResume();
                }
            }
        }
        refresh(); // 刷新播放控制条
    }

    // 根据手机的分辨率从 dp 的单位 转成为 px(像素)
    public static int dip2px(Context context, float dpValue) {
        // 获取当前手机的像素密度
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    // 设置当前的播放时间
    public void setCurrentTime(int current_time, int buffer_time) {
        // 获得媒体播放器的播放时长
        duration = app.mMediaPlayer.getDuration();
        current = current_time;
        buffer = buffer_time;
        // 媒体播放器是否正在播放
        isPaused = !app.mMediaPlayer.isPlaying();
        refresh(); // 刷新播放控制条
    }


}
