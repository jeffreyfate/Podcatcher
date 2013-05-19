package com.jeffthefate.podcatcher;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.podcatcher.service.DownloadService;

public class DownloadAdapter extends BaseAdapter {
    
    private Context mContext;
    private ArrayList<Episode> mEpisodes;
    private int mCurrEpId;
    
    private ViewHolder holder;
    
    private int contextPosition;
    private boolean contextFailed;
    private boolean contextDownloading;
    
    private TextView mEmptyText;
    private ProgressBar mProgress;
    
    private SparseBooleanArray contextChecked = new SparseBooleanArray();
    private ArrayList<View> listViews = new ArrayList<View>();
    
    private boolean multiSelect = false;
    
    private Resources res;
    
    protected static class ViewHolder {
        TextView text1;
        TextView text2;
        RelativeLayoutEx button;
        int id;
        ProgressBar progress;
    }
    
    public interface AdapterChange {
        public void showDetailsDialog(String[] values);
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
        res = context.getResources();
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
    
    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }
    
    public void resetViews() {
        for (View view : listViews) {
            view.setBackgroundResource(R.drawable.color_btn_context_menu);
        }
        contextChecked.clear();
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
            listViews.add(convertView);
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
        if (!contextChecked.get(holder.id))
            convertView.setBackgroundResource(
                    R.drawable.color_btn_context_menu);
        else
            convertView.setBackgroundResource(R.drawable.color_btn_row_checked);
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (multiSelect) {
                    if (!contextChecked.get(holder.id)) {
                        view.setBackgroundResource(
                                R.drawable.color_btn_row_checked);
                        contextChecked.put(holder.id, true);
                    }
                    else {
                        contextChecked.delete(holder.id);
                        view.setBackgroundResource(
                                R.drawable.color_btn_context_menu);
                    }
                }
            }
        });
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
            if (getFailed(position)) {
                holder.progress.setProgress(100);
                holder.progress.setProgressDrawable(
                        res.getDrawable(R.drawable.progress_fail));
            }
            else {
                holder.progress.setProgress(0);
                holder.progress.setProgressDrawable(res.getDrawable(
                        R.drawable.progress_horizontal_holo_dark));
            }
            holder.progress.setVisibility(View.VISIBLE);
            mProgress = holder.progress;
            if (ApplicationEx.currEpTotal != 0)
                mProgress.setProgress(ApplicationEx.currEpProgress);
        }
        else if (getFailed(position)) {
            holder.progress.setProgress(100);
            holder.progress.setProgressDrawable(
                    res.getDrawable(R.drawable.progress_fail));
            holder.progress.setVisibility(View.VISIBLE);
        }
        else
            holder.progress.setVisibility(View.INVISIBLE);
        return convertView;
    }
    
    private void setProgress(int progress) {
        if (mProgress != null) {
            if (progress < 0) {
                mProgress.setIndeterminate(true);
                mProgress.postInvalidate();
            }
            else {
                mProgress.setIndeterminate(false);
                mProgress.postInvalidate();
                mProgress.setProgress(progress);
            }
        }
    }
    
    public void setCurrentProgress(int progress) {
        setProgress(progress);
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
    
    public void updateData(ArrayList<Episode> podcasts, int currEpId,
            boolean resetProgress) {
        mEpisodes = podcasts;
        mCurrEpId = currEpId;
        notifyDataSetChanged();
        if (resetProgress)
            setCurrentProgress(0);
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
    
    public void removeSelected() {
        ArrayList<String> urlList = new ArrayList<String>();
        for (int i = 0; i < contextChecked.size(); i++) {
            urlList.add(ApplicationEx.dbHelper.getEpisodeUrl(
                    contextChecked.keyAt(i)));
        }
        String[] urls = new String[urlList.size()];
        urlList.toArray(urls);
        Intent intent = new Intent(Constants.ACTION_REMOVE_DOWNLOAD);
        intent.putExtra(Constants.URLS, urls);
        if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            mContext.sendBroadcast(intent);
        }
    }

}
