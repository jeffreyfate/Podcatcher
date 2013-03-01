package com.jeffthefate.pod_dev;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadAdapter extends BaseAdapter {
    
    private Context mContext;
    private ArrayList<Episode> mEpisodes;
    private int mCurrEpId;
    
    private ViewHolder holder;
    
    private int contextPosition;
    private boolean contextFailed;
    private boolean contextDownloading;
    private int currentProgress;
    
    private TextView mEmptyText;
    private ProgressBar mProgress;
    
    protected static class ViewHolder {
        TextView text1;
        TextView text2;
        RelativeLayoutEx button;
        int id;
        ProgressBar progress;
    }
    
    public DownloadAdapter(Context context, ArrayList<Episode> episodes, 
            TextView emptyText, int currEpId) {
        mContext = context;
        mEpisodes = episodes;
        mCurrEpId = currEpId;
        mEmptyText = emptyText;
        mEmptyText.setText(R.string.emptyDownloads);
        mEmptyText.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        mEmptyText.setPadding(5, 20, 5, 5);
        mEmptyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
    }
    
    @Override
    public int getCount() {
        return mEpisodes.size();
    }

    @Override
    public Object getItem(int position) {
        return mEpisodes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) mEpisodes.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row_download, parent, false);
            holder = new ViewHolder();
            holder.text1 = (TextView)convertView.findViewById(R.id.text1);
            holder.text2 = (TextView)convertView.findViewById(R.id.text2);
            holder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            holder.id = (int) getItemId(position);
            holder.progress = (ProgressBar)convertView.findViewById(
                    R.id.downloadprogress);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
            if (holder.id != (int) getItemId(position)) {
                holder.id = (int) getItemId(position);
            }
        }
        holder.text1.setTag(position);
        holder.text1.setText(getTitle(position));
        if (getRead(position))
            holder.text1.setTextColor(Color.WHITE);
        else
            holder.text1.setTextColor(Color.GRAY);
        holder.text2.setTag(position);
        holder.text2.setText(getFeedTitle(position));
        final String url = getUrl(position);
        convertView.setBackgroundResource(R.drawable.color_btn_context_menu);
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {}
        });
        final View view = convertView;
        holder.button.setBackgroundResource(R.drawable.color_btn_context_menu);
        holder.button.setTag(position);
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                contextPosition = pos;
                contextFailed = getFailed(pos);
                contextDownloading = getDownloading(pos);
                view.showContextMenu();
            }
        });
        holder.progress.setTag(position);
        if (mEpisodes.get(position).getId() == mCurrEpId) {
            holder.progress.setVisibility(View.VISIBLE);
            mProgress = holder.progress;
        }
        else
            holder.progress.setVisibility(View.INVISIBLE);
        return convertView;
    }
    
    public void setProgress(int progress) {
        if (mProgress != null) {
            mProgress.setProgress(progress);
        }
    }
    
    public void setCurrentProgress(int progress) {
        currentProgress = progress;
        setProgress(progress);
    }
    
    public int getCurrentProgress() {
        return currentProgress;
    }
    
    public int getContextId() {
        return (int) getItemId(contextPosition);
    }
    
    public boolean getContextFailed() {
        return contextFailed;
    }
    
    public boolean getContextDownloading() {
        return contextDownloading;
    }
    
    public void updateData(ArrayList<Episode> podcasts, int currEpId) {
        mEpisodes = podcasts;
        mCurrEpId = currEpId;
        notifyDataSetChanged();
    }
    
    private String getUrl(int position) {
        return mEpisodes.get(position).getUrl();
    }
    
    private String getTitle(int position) {
        return mEpisodes.get(position).getTitle();
    }
    
    private String getFeedTitle(int position) {
        return mEpisodes.get(position).getSubtitle();
    }
    
    private boolean getRead(int position) {
        return mEpisodes.get(position).getUnread();
    }
    
    private boolean getFailed(int position) {
        return ApplicationEx.dbHelper.getEpisodeFailed(
                mEpisodes.get(position).getId());
    }
    
    private boolean getDownloading(int position) {
        return ApplicationEx.dbHelper.getCurrentDownload() ==
            mEpisodes.get(position).getId();
    }

}
