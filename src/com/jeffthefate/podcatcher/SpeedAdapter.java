package com.jeffthefate.podcatcher;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jeffthefate.podcatcher.service.PlaybackService;

public class SpeedAdapter extends ArrayAdapter<Double> {
    
    private Context mContext;
    private PlaybackService mService;
    private ArrayList<Double> mSpeeds;
    private LinearLayout mSpeedListLayout;
    
    protected static class ViewHolder {
        TextView text1;
        int id;
    }
    
    public SpeedAdapter(Context context, int resource, int textViewResourceId,
            ArrayList<Double> objects, PlaybackService service,
            LinearLayout speedListLayout) {
        super(context, resource, textViewResourceId, objects);
        mContext = context;
        mService = service;
        mSpeeds = objects;
        mSpeedListLayout = speedListLayout;
    }
    
    @Override
    public int getCount() {
        if (mSpeeds != null)
            return mSpeeds.size();
        return 0;
    }

    @Override
    public Double getItem(int position) {
        if (mSpeeds != null)
            return mSpeeds.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mSpeeds != null)
            return position;
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row_speed, parent, false);
            convertView.setBackgroundColor(Constants.HOLO_BLUE);
            holder = new ViewHolder();
            holder.text1 = (TextView) convertView.findViewById(
                    android.R.id.text1);
            holder.id = (int) getItemId(position);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
            if (holder.id != (int) getItemId(position)) {
                holder.id = (int) getItemId(position);
            }
        }
        holder.text1.setTag(position);
        holder.text1.setText(TextUtils.concat(
                Double.toString(mSpeeds.get(position)), Constants.X)
                        .toString());
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_SPEED_FREEZE));
                mService.setSpeed(pos);
                mSpeedListLayout.setVisibility(View.INVISIBLE);
            }
        });
        convertView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mSpeeds.remove(pos);
                mService.setSpeedList(mSpeeds);
                notifyDataSetChanged();
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_SPEED_CHANGED));
                return true;
            }
        });
        return convertView;
    }
    
    public void setSpeedList(ArrayList<Double> speedList) {
        mSpeeds = speedList;
        notifyDataSetChanged();
    }

}
