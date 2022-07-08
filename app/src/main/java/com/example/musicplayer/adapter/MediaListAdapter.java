package com.example.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.bean.MediaInfo;
import com.example.musicplayer.util.MediaUtil;

import java.util.ArrayList;

public class MediaListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<MediaInfo> mMediaList;

    public MediaListAdapter(Context context, ArrayList<MediaInfo> mediaList) {
        mContext = context;
        mMediaList = mediaList;

    }

    @Override
    public int getCount() {
        return mMediaList.size();
    }

    @Override
    public Object getItem(int arg0) {
        return mMediaList.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_media, null);
            holder.iv_format = convertView.findViewById(R.id.iv_format);
            holder.tv_title = convertView.findViewById(R.id.tv_title);
            holder.tv_artist = convertView.findViewById(R.id.tv_artist);
            holder.tv_duration = convertView.findViewById(R.id.tv_duration);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MediaInfo item = mMediaList.get(position);
        holder.iv_format.setImageBitmap(item.getAlbumBip());                        // 文件图标
        holder.tv_title.setText(item.getTitle());                                   // 乐曲名
        holder.tv_artist.setText(item.getArtist());                                 // 演唱者
        holder.tv_duration.setText(MediaUtil.formatDuration(item.getDuration()));   // 播放时长
        return convertView;
    }

    public final class ViewHolder {
        public ImageView iv_format;
        public TextView tv_title;
        public TextView tv_artist;
        public TextView tv_duration;
    }

}
