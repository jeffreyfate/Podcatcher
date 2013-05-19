package com.jeffthefate.podcatcher.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;

public class FragmentFirstStart extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.first_start, container, false);
        Button adjustButton = (Button) v.findViewById(R.id.SetupButton);
        adjustButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment detailsDialog =
                        FragmentAccountDialog.newInstance();
                try {
                    detailsDialog.show(
                            getActivity().getSupportFragmentManager(),
                            Constants.DIALOG_ACCOUNT);
                } catch (NullPointerException e) {}
            } 
        });
        return v;
    }
    
}