package com.jeffthefate.podcatcher.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;

public class FragmentFeedDetailsDialog extends DialogFragment {
    
    private String[] values;
    
    public static FragmentFeedDetailsDialog newInstance(String[] values) {
        FragmentFeedDetailsDialog dialog = new FragmentFeedDetailsDialog();
        Bundle bundle = new Bundle();
        bundle.putStringArray(Constants.VALUES, values);
        dialog.setArguments(bundle);
        return dialog;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        values = getArguments().getStringArray(Constants.VALUES);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View v = inflater.inflate(R.layout.details_feed, null);
        TextView feedTitle = (TextView) v.findViewById(R.id.DialogDetailsTitle);
        feedTitle.setText(values[0]);
        TextView feedDescription = (TextView) v.findViewById(
                R.id.DialogDetailsDescription);
        feedDescription.setText(values[1]);
        feedDescription.setMovementMethod(new ScrollingMovementMethod());
        TextView feedEpisodeTime = (TextView) v.findViewById(
                R.id.DialogDetailsLatestEpisode);
        feedEpisodeTime.setText(values[2]);
        TextView feedFeedUpdateTime = (TextView) v.findViewById(
                R.id.DialogDetailsLastUpdate);
        feedFeedUpdateTime.setText(values[3]);
        builder.setView(v).setNeutralButton(R.string.doneButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}