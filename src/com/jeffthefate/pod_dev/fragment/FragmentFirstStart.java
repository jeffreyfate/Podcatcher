package com.jeffthefate.pod_dev.fragment;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.activity.ActivityFirstStart;
import com.jeffthefate.pod_dev.activity.ActivitySetup;

public class FragmentFirstStart extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.first_start, container, false);
        Button adjustButton = (Button) v.findViewById(R.id.SetupButton);
        adjustButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        ActivitySetup.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivityForResult(intent, 0);
            } 
        });
        return v;
    }
    
}