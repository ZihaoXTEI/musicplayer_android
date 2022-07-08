package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.adapter.MediaListAdapter;
import com.example.musicplayer.bean.MediaInfo;
import com.example.musicplayer.loader.MusicLoader;
import com.example.musicplayer.widget.AudioController;

import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    String path;

    private static final String TAG = "MainActivity";
    private AudioController ac_play;            // 声明一个音频控制条对象
    private MainApplication app;                // 声明一个全局应用对象
    private MusicLoader loader;                 // 声明一个音乐加载器对象
    private Handler mHandler = new Handler();   // 声明一个处理器对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 1);
        } else {
            initMusicList(); // 初始化音乐列表
        }

        // 从布局文件中获取名叫ac_play的音频控制条
        ac_play = findViewById(R.id.ac_play);
        // 获取全局应用的唯一实例
        app = MainApplication.getInstance();

    }

    //获取权限
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMusicList();
                } else {
                    Toast.makeText(this, "拒绝权限无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_item:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");      //选择音频文件
                intent.putExtra(EXTRA_ALLOW_MULTIPLE, true);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
                break;
            case R.id.playmode_item:
                playmodeDialog();
                break;
            case R.id.about_item:
                aboutDialog();
                break;
            default:
                break;
        }
        return true;
    }


    //关于对话框内容
    public void aboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("关于");
        builder.setMessage("学号：201710098064\n班级：软件工程1班\n制作人：黎梓豪");
        builder.setNegativeButton("确认", null);
        builder.show();
    }

    //播放模式对话框内容
    public void playmodeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("请选择播放模式");
        final String[] items = {"单曲循环", "顺序播放", "列表循环"};
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i){
                    case 0:
                        app.mPlayMode = 1;
                        Toast.makeText(MainActivity.this, "单曲循环模式", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        app.mPlayMode = 2;
                        Toast.makeText(MainActivity.this, "顺序播放模式", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        app.mPlayMode = 0;
                        Toast.makeText(MainActivity.this, "列表循环模式", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        builder.setNegativeButton("确认", null);
        builder.show();
    }


    //调用系统文件管理器（只支持4.4以后的系统）
    //参考自：https://blog.csdn.net/bzlj2912009596/article/details/80994628
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
                path = uri.getPath();
                //tv.setText(path);
                Toast.makeText(this, path + "11111", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                path = getPath(this, uri);
                //tv.setText(path);
                Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                // 拼接文件的完整路径
                //String file_path = absolutePath + "/" + fileName;
                //Toast.makeText(this,file_path,Toast.LENGTH_SHORT).show();
                // 创建一个媒体信息实例

                //path.substring(path.lastIndexOf("/")+1);
                MediaInfo music = new MediaInfo(path.substring(path.lastIndexOf("/") + 1), "未知", path);
                gotoPlay(music, 0); // 跳转到音乐播放页面
            }
        }
    }


    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    // 初始化音乐列表
    private void initMusicList() {
        // 从布局文件中获取名叫lv_music的列表视图
        ListView lv_music = findViewById(R.id.lv_music);
        // 获得音乐加载器的唯一实例
        loader = MusicLoader.getInstance(getContentResolver());
        // 构建一个音乐信息的列表适配器
        MediaListAdapter adapter = new MediaListAdapter(this, loader.getMusicList());
        // 给lv_music设置音乐列表适配器
        lv_music.setAdapter(adapter);
        // 给lv_music设置单项点击监听器
        lv_music.setOnItemClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initController(); // 初始化音频控制条
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 移除所有的处理器任务
        mHandler.removeCallbacksAndMessages(null);
    }

    // 初始化音频控制条
    private void initController() {
        TextView tv_song = findViewById(R.id.tv_song);
        if (app.mSong != null) {
            tv_song.setText(app.mSong + " 正在播放");
        } else {
            tv_song.setText("当前暂无歌曲播放");
        }
        // 延迟100毫秒后启动控制条刷新任务
        mHandler.postDelayed(mRefreshCtrl, 100);
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
                initController();
            }

            // 延迟500毫秒后再次启动控制条刷新任务
            mHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        gotoPlay(loader.getMusicList().get(position), position);  // 跳转到音乐播放页面
    }

    // 跳转到音乐播放页面
    private void gotoPlay(MediaInfo media, int position) {
        // 以下携带媒体信息跳转到音乐播放详情页面
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("music", media);
        intent.putExtra("position", position);
        startActivity(intent);
    }

}

