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

public class FragmentEpisodeDetailsDialog extends DialogFragment {
    
    private String[] values;
    
    public static FragmentEpisodeDetailsDialog newInstance(String[] values) {
        FragmentEpisodeDetailsDialog dialog =
                new FragmentEpisodeDetailsDialog();
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
        View v = inflater.inflate(R.layout.details, null);
        TextView feedText = (TextView) v.findViewById(R.id.DialogDetailsFeed);
        feedText.setText(values[2]);
        TextView episodeText = (TextView) v.findViewById(
                R.id.DialogDetailsEpisode);
        episodeText.setText(values[1]);
        TextView summaryText = (TextView) v.findViewById(
                R.id.DialogDetailsSummary);
        summaryText.setText(values[3]);
        summaryText.setMovementMethod(new ScrollingMovementMethod());
        builder.setView(v).setNeutralButton(R.string.doneButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}