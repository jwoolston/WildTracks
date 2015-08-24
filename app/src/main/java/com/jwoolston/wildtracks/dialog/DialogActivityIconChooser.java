package com.jwoolston.wildtracks.dialog;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.jwoolston.wildtracks.R;
import com.jwoolston.wildtracks.markers.UserMarkerRenderer;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogActivityIconChooser extends DialogFragment {

    private static final String TAG = DialogActivityIconChooser.class.getSimpleName();

    public static final String ACTION_ACTIVITY_ICON_PICKED = DialogActivityIconChooser.class.getCanonicalName() + ".ACTION_ACTIVITY_ICON_PICKED";
    public static final String EXTRA_ICON_INDEX = DialogActivityIconChooser.class.getCanonicalName() + ".EXTRA_ICON_INDEX";

    private LocalBroadcastManager mLocalBroadcastManager;

    private RecyclerView mRecyclerView;

    private DialogActivitiesEdit.ActivitiesPreference mPreference;

    private String mActivity;

    public DialogActivityIconChooser() {
        // Empty constructor
    }

    public void setActivity(String activity) {
        mActivity = activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_activity_icon_chooser, container, false);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        mPreference = DialogActivitiesEdit.reloadPreference(getActivity());

        mRecyclerView = (RecyclerView) view.findViewById(R.id.item_list_preference_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mRecyclerView.setAdapter(new Adapter(UserMarkerRenderer.LARGE_ICON_MAPPING));

        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.edit_activities_toolbar);
        toolbar.setTitle(R.string.dialog_activity_icon_chooser_title);
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        getDialog().getWindow().setLayout(getActivity().getResources().getDimensionPixelSize(R.dimen.activity_icon_chooser_width), getActivity().getResources().getDimensionPixelSize(R.dimen.activity_icon_chooser_height));
        super.onResume();
    }

    protected void onListItemClicked(Adapter.ViewHolder holder, int position) {
        Log.d(TAG, "Setting icon index for activity " + mActivity + ": " + (position + 1));
        final DialogActivitiesEdit.ActivitiesPreference preference = DialogActivitiesEdit.reloadPreference(getActivity());
        preference.icons.put(mActivity, position + 1);
        DialogActivitiesEdit.persistPreference(getActivity(), preference);
        dismiss();
    }

    protected class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        protected int[] mDataset;

        protected Adapter(int[] dataset) {
            mDataset = new int[dataset.length - 1];
            System.arraycopy(dataset, 1, mDataset, 0, dataset.length - 1);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_activity_icon, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Drawable icon = null;
            try {
                int id = mDataset[position];
                if (id <= 0) id = R.drawable.ic_place_white_24dp;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    icon = getResources().getDrawable(id);
                } else {
                    icon = getActivity().getDrawable(id);
                }
            } finally {
                holder.imgView.setImageDrawable(icon);
                holder.imgView.setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);
            }
        }

        @Override
        public int getItemCount() {
            return mDataset.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // each data item is just a string in this case
            public ImageView imgView;

            public ViewHolder(View v) {
                super(v);
                imgView = (ImageView) v.findViewById(R.id.icon);
                imgView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                // Should only have one view assigned to it so don't bother checking
                DialogActivityIconChooser.this.onListItemClicked(this, getAdapterPosition());
            }
        }
    }
}
