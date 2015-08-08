package com.jwoolston.huntinglogger.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jwoolston.huntinglogger.R;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogEditPin extends DialogFragment {

    public DialogEditPin() {
        // Empty constructor required for DialogFragment
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, R.style.Theme_AppCompat);
        final View view = inflater.inflate(R.layout.layout_dialog_drop_pin, container);
        getDialog().setTitle(R.string.dialog_drop_pin_title);
        return view;
    }
}
