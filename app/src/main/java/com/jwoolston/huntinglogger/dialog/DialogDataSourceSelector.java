package com.jwoolston.huntinglogger.dialog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

import com.jwoolston.huntinglogger.R;
import com.jwoolston.huntinglogger.file.ActivityFilePicker;
import com.jwoolston.huntinglogger.mapping.MapManager;
import com.nononsenseapps.filepicker.FilePickerActivity;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogDataSourceSelector extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String TAG = DialogDataSourceSelector.class.getSimpleName();

    private static final int FILE_CODE = 10;

    private LocalBroadcastManager mLocalBroadcastManager;

    private int mSelectedProvider;

    public DialogDataSourceSelector() {
        // Empty constructor required for DialogFragment
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final int checked = preferences.getInt(MapManager.KEY_SELECTED_PROVIDER, MapManager.DEFAULT_PROVIDER);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Map Source");
        builder.setSingleChoiceItems(R.array.dialog_data_provider_chooser_items, checked, this);
        return builder.create();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                updateProviderPreferences(uri.getPath());
                notifyAppNewProviderSelected();
                dismiss();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mSelectedProvider = which;
        if (which == MapManager.LOCAL_MBTILES_FILE || which == MapManager.LOCAL_CACHE_FILE) {
            // We need to ask the user to select a file
            final Intent i = new Intent(getActivity(), ActivityFilePicker.class);

            // Set these depending on your use case. These are the defaults.
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
            i.putExtra(ActivityFilePicker.EXTRA_FILTER_EXTENSION, (which == MapManager.LOCAL_MBTILES_FILE) ? ".mbtiles" : ".mapcache");

            // Configure initial directory by specifying a String.
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

            startActivityForResult(i, FILE_CODE);
        } else {
            updateProviderPreferences(null);
            notifyAppNewProviderSelected();
            dialog.dismiss();
        }
    }

    private void updateProviderPreferences(String path) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit()
            .putInt(MapManager.KEY_SELECTED_PROVIDER, mSelectedProvider)
            .putString(MapManager.KEY_PROVIDER_FILE, path)
            .apply();
    }

    private void notifyAppNewProviderSelected() {
        final Intent intent = new Intent(MapManager.ACTION_PROVIDER_CHANGED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }
}
