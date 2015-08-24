package com.jwoolston.wildtracks.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jwoolston.wildtracks.R;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogEditText extends DialogFragment implements TextView.OnEditorActionListener, View.OnClickListener {

    private static final String TAG = DialogEditText.class.getSimpleName();

    private ImageView mImageView;
    private EditText mEditText;
    private EditTextListener mNameDialogListener;
    private boolean isActivity;

    public DialogEditText() {
        // Empty constructor required for DialogFragment
        setStyle(STYLE_NORMAL, R.style.EditText_Light_Dialog_FixedSize);
    }

    public void setEditTextListener(EditTextListener listener) {
        mNameDialogListener = listener;
    }

    public void setIsActivity(boolean isActivity) {
        this.isActivity = isActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_edit_text, container);

        mImageView = (ImageView) view.findViewById(R.id.icon);
        mEditText = (EditText) view.findViewById(R.id.text_field);
        mEditText.setOnEditorActionListener(this);
        mEditText.setHint(isActivity ? "i.e. Hiking" : "i.e. Trailhead");

        if (isActivity) {
            mImageView.setOnClickListener(this);
        }

        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.edit_text_toolbar);
        toolbar.setTitle("Create New " + (isActivity ? "Activity" : "Marker Type"));
        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text to activity
            mNameDialogListener.onFinishEditDialog(mEditText.getText().toString());
            dismiss();
            return true;
        }
        return false;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.icon) {
            Toast.makeText(getActivity(), "Image view clicked.", Toast.LENGTH_SHORT).show();
        }
    }

    public interface EditTextListener {
        void onFinishEditDialog(String input);
    }
}
