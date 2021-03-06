package com.jwoolston.wildtracks.dialog;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jwoolston.wildtracks.R;
import com.jwoolston.wildtracks.markers.UserMarkerRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogActivitiesEdit extends DialogFragment implements View.OnClickListener, Toolbar.OnMenuItemClickListener, DialogEditText.EditTextListener {

    private static final String TAG = DialogActivitiesEdit.class.getSimpleName();

    private static final String KEY_ACTIVITIES = DialogActivitiesEdit.class.getCanonicalName() + ".KEY_ACTIVITIES";
    private static final String KEY_MARKERTYPES = DialogActivitiesEdit.class.getCanonicalName() + ".ACTIVITIY_MARKERTYPES.KEY";

    private static final String DEFAULT_ACTIVITIES_SET = "Hiking:1,Running:2,Cycling:3,Geocaching:4";
    private static final String DELIMETER = ",";
    private static final String ICON_DELIMETER = ":";

    public static final String ACTION_ACTIVITIES_UPDATED = DialogActivitiesEdit.class.getCanonicalName() + ".ACTION_ACTIVITIES_UPDATED";

    private LocalBroadcastManager mLocalBroadcastManager;

    private RecyclerView mRecyclerView;
    private FloatingActionButton mActionButton;

    private List<String> mTempList;
    private ActivitiesPreference mPreference;

    private boolean mShowingActivityDetail = false;
    private String mCurrentActivityDetail = null;

    public static ActivitiesPreference reloadPreference(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "Loading activities preference: " + preferences.getString(KEY_ACTIVITIES, DEFAULT_ACTIVITIES_SET));
        final String[] activities_array = preferences.getString(KEY_ACTIVITIES, DEFAULT_ACTIVITIES_SET).split(DELIMETER);
        final List<String> activities = new ArrayList<>();
        final Map<String, Integer> icons = new HashMap<>();
        final Map<String, List<String>> types = new HashMap<>();
        final ActivitiesPreference preference = new ActivitiesPreference(activities, icons, types);
        String[] splits;
        for (String activity : activities_array) {
            if (!activity.isEmpty()) {
                splits = activity.split(ICON_DELIMETER);
                activities.add(splits[0]);
                icons.put(splits[0], Integer.valueOf(splits[1]));
                final String activity_subtypes_key = KEY_MARKERTYPES + "." + activity;
                final String pref = preferences.getString(activity_subtypes_key, null);
                final List<String> list = new ArrayList<>();
                list.add("");
                if (pref != null) {
                    final String[] subtypes_array = pref.split(DELIMETER);
                    Collections.addAll(list, subtypes_array);
                }
                types.put(splits[0], list);
            }
        }
        activities.add(0, "");
        types.put(activities.get(0), new ArrayList<String>());
        return preference;
    }

    public static void persistPreference(Context context, ActivitiesPreference preference) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = preferences.edit();
        final StringBuilder builder_activities = new StringBuilder();
        StringBuilder builder_subtypes;
        for (String activity : preference.activities) {
            if (activity == null || activity.isEmpty()) continue;
            builder_activities.append(activity).append(ICON_DELIMETER);
            builder_activities.append(preference.icons.get(activity)).append(DELIMETER);
            final String activity_subtypes_key = KEY_MARKERTYPES + "." + activity;
            final List<String> list = preference.types.get(activity);
            builder_subtypes = new StringBuilder();
            for (String s : list) {
                builder_subtypes.append(s).append(DELIMETER);
            }
            editor.putString(activity_subtypes_key, builder_subtypes.toString());
        }
        Log.d(TAG, "Saving activities preference: " + builder_activities.toString());
        editor.putString(KEY_ACTIVITIES, builder_activities.toString());
        editor.apply();
        final Intent intent = new Intent(ACTION_ACTIVITIES_UPDATED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private final BroadcastReceiver mActivitiesUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received broadcast of activity preference update.");
            mPreference = DialogActivitiesEdit.reloadPreference(context);
            // Sure we could update the existing adapter, but this is simpler and its not that heavy
            mRecyclerView.setAdapter(new Adapter(mPreference.activities.subList(1, mPreference.activities.size())));
        }
    };

    public DialogActivitiesEdit() {
        super();
    }

    @Override
    public void onResume() {
        super.onResume();
        mLocalBroadcastManager.registerReceiver(mActivitiesUpdatedReceiver, new IntentFilter(ACTION_ACTIVITIES_UPDATED));
    }

    @Override
    public void onPause() {
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(mActivitiesUpdatedReceiver);
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
        final View view = inflater.inflate(R.layout.dialog_edit_activities, container, false);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        mPreference = reloadPreference(getActivity());

        mRecyclerView = (RecyclerView) view.findViewById(R.id.item_list_preference_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new Adapter(mPreference.activities.subList(1, mPreference.activities.size())));

        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.edit_activities_toolbar);
        toolbar.setTitle(R.string.dialog_edit_activities_title);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.menu_edit_activities);
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mShowingActivityDetail) {
                    dismissAllowingStateLoss();
                } else {
                    toolbar.setTitle(R.string.dialog_edit_activities_title);
                    mPreference.types.get(mCurrentActivityDetail).clear();
                    mPreference.types.get(mCurrentActivityDetail).addAll(mTempList);
                    mTempList = null;
                    mShowingActivityDetail = false;
                    mCurrentActivityDetail = null;
                    mRecyclerView.setAdapter(new Adapter(mPreference.activities.subList(1, mPreference.activities.size())));
                }
            }
        });

        mActionButton = (FloatingActionButton) view.findViewById(R.id.fab_add_activity);
        mActionButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.fab_add_activity) {
            if (!mShowingActivityDetail) {
                addActivity();
            } else {
                addMarkerType();
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_edit_activities_done) {
            persistPreference(getActivity(), mPreference);
            dismiss();
        }
        return false;
    }

    @Override
    public void onFinishEditDialog(String input) {
        if (!mShowingActivityDetail) {
            ((Adapter) mRecyclerView.getAdapter()).add(input);
            mPreference.types.put(input, new ArrayList<String>());
        } else {
            ((Adapter) mRecyclerView.getAdapter()).add(input);
            mPreference.types.get(mCurrentActivityDetail).add(input);
        }
    }

    protected void addActivity() {
        final FragmentManager fm = getActivity().getSupportFragmentManager();
        final DialogEditText dialog = new DialogEditText();
        dialog.setIsActivity(true);
        dialog.setEditTextListener(this);
        dialog.show(fm, DialogEditText.class.getCanonicalName());
    }

    protected void addMarkerType() {
        final FragmentManager fm = getActivity().getSupportFragmentManager();
        final DialogEditText dialog = new DialogEditText();
        dialog.setIsActivity(false);
        dialog.setEditTextListener(this);
        dialog.show(fm, DialogEditText.class.getCanonicalName());
    }

    protected void onListItemClicked(Adapter.ViewHolder holder, int position) {
        if (!mShowingActivityDetail) {
            mShowingActivityDetail = true;
            mCurrentActivityDetail = mPreference.activities.get(position);
            mTempList = new ArrayList<>(mPreference.types.get(mCurrentActivityDetail));
            mRecyclerView.setAdapter(new Adapter(mTempList));
            getDialog().setTitle(mCurrentActivityDetail);
        } else {
            Toast.makeText(getActivity(), "List Detail Item Clicked", Toast.LENGTH_SHORT).show();
        }
    }

    protected boolean onListItemLongClicked(Adapter.ViewHolder holder, int position) {
        final DialogActivityIconChooser chooser = new DialogActivityIconChooser();
        chooser.setActivity(mPreference.activities.get(position + 1));
        chooser.show(getActivity().getSupportFragmentManager(), DialogActivityIconChooser.class.getCanonicalName());
        return true;
    }

    protected class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        protected List<String> mDataset;

        protected Adapter(List<String> dataset) {
            mDataset = dataset;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_activity, parent, false);
            view.setOnClickListener(DialogActivitiesEdit.this);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.txtHeader.setText(mDataset.get(position));
            Drawable icon = null;
            try {
                final String key = !mShowingActivityDetail ? mDataset.get(position) : mCurrentActivityDetail;
                int id = UserMarkerRenderer.ICON_MAPPING[mPreference.icons.get(key)];
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
            return mDataset.size();
        }

        public List<String> getDataset() {
            return mDataset;
        }

        public void add(String item) {
            mDataset.add(item);
            notifyItemInserted(mDataset.size());
        }

        public void remove(String item) {
            int position = mDataset.indexOf(item);
            mDataset.remove(position);
            notifyItemRemoved(position);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
            // each data item is just a string in this case
            public ImageView imgView;
            public TextView txtHeader;

            public ViewHolder(View v) {
                super(v);
                v.setClickable(true);
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
                imgView = (ImageView) v.findViewById(R.id.icon);
                txtHeader = (TextView) v.findViewById(R.id.firstLine);
            }

            @Override
            public void onClick(View v) {
                // Should only have one view assigned to it so don't bother checking
                DialogActivitiesEdit.this.onListItemClicked(this, getAdapterPosition());
            }

            @Override
            public boolean onLongClick(View v) {
                return DialogActivitiesEdit.this.onListItemLongClicked(this, getAdapterPosition());
            }
        }
    }

    public static class ActivitiesPreference {

        public final List<String> activities;
        public final Map<String, Integer> icons;
        public final Map<String, List<String>> types;

        public ActivitiesPreference(List<String> activities, Map<String, Integer> icons, Map<String, List<String>> types) {
            this.activities = activities;
            this.icons = icons;
            this.types = types;
        }
    }
}
