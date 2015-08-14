package com.jwoolston.wildtracks.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.jwoolston.wildtracks.R;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogLegalNotices extends DialogFragment implements DialogInterface.OnClickListener {

    public DialogLegalNotices() {
        // Empty constructor required for DialogFragment
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle("Legal Notices");
        builder.setItems(R.array.dialog_legal_notices_items, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 0:
                showGoogleMapsAPIDialog();
                break;
            case 1:
                showUSGSDialog();
                break;
            case 2:
                showNoNonsenseFilePickerDialog();
                break;
        }
    }

    private void showGoogleMapsAPIDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle(R.string.dialog_legal_notices_google_maps_title);
        builder.setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getActivity()));
        builder.show();
    }

    private void showUSGSDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle(R.string.dialog_legal_notices_usgs_title);
        builder.setMessage(R.string.dialog_legal_notices_usgs_text);
        builder.show();
    }

    private void showNoNonsenseFilePickerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle(R.string.dialog_legal_notices_nononsense_title);
        builder.setMessage(getResources().getString(R.string.dialog_legal_notices_nononsense_text)
            + '\n' + '\n'
            + getResources().getString(R.string.nononsense_license_lesser)
            + '\n' + '\n'
            + getResources().getString(R.string.nononsense_license));
        builder.show();
    }
}
