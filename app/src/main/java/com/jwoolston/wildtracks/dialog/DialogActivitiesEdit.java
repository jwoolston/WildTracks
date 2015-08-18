package com.jwoolston.wildtracks.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private static final String DEFAULT_ACTIVITIES_SET = "Hiking,Hunting,Fishing,Cycling";
    private static final String DELIMETER = ",";

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
        final String[] activities_array = preferences.getString(KEY_ACTIVITIES, DEFAULT_ACTIVITIES_SET).split(DELIMETER);
        final List<String> activities = new ArrayList<>();
        final Map<String, List<String>> types = new HashMap<>();
        final ActivitiesPreference preference = new ActivitiesPreference(activities, types);
        for (String activity : activities_array) {
            if (!activity.isEmpty()) activities.add(activity);
            final String activity_subtypes_key = KEY_MARKERTYPES + "." + activity;
            final String pref = preferences.getString(activity_subtypes_key, null);
            Log.d(TAG, "Activity: " + activity);
            Log.d(TAG, "Marker Types Preference: " + pref);
            final List<String> list = new ArrayList<>();
            list.add("");
            if (pref != null) {
                final String[] subtypes_array = pref.split(DELIMETER);
                Collections.addAll(list, subtypes_array);
                Log.d(TAG, "Preference List: " + list);
            }
            types.put(activity, list);
        }
        activities.add(0, "");
        types.put(activities.get(0), new ArrayList<String>());
        Log.d(TAG, "Types Keyset: " + preference.types.keySet());
        return preference;
    }

    public DialogActivitiesEdit() {
        super();
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
        mRecyclerView.setAdapter(new Adapter(mPreference.activities));

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
                    mRecyclerView.setAdapter(new Adapter(mPreference.activities));
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
            persistPreference();
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

    protected void persistPreference() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor editor = preferences.edit();
        final StringBuilder builder_activities = new StringBuilder();
        StringBuilder builder_subtypes;
        for (String activity : mPreference.activities) {
            builder_activities.append(activity).append(DELIMETER);
            final String activity_subtypes_key = KEY_MARKERTYPES + "." + activity;
            final List<String> list = mPreference.types.get(activity);
            builder_subtypes = new StringBuilder();
            for (String s : list) {
                builder_subtypes.append(s).append(DELIMETER);
            }
            editor.putString(activity_subtypes_key, builder_subtypes.toString());
        }
        editor.putString(KEY_ACTIVITIES, builder_activities.toString());
        editor.apply();
        final Intent intent = new Intent(ACTION_ACTIVITIES_UPDATED);
        mLocalBroadcastManager.sendBroadcast(intent);
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

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // each data item is just a string in this case
            public ImageView imgView;
            public TextView txtHeader;

            public ViewHolder(View v) {
                super(v);
                v.setClickable(true);
                v.setOnClickListener(this);
                imgView = (ImageView) v.findViewById(R.id.icon);
                txtHeader = (TextView) v.findViewById(R.id.firstLine);
            }

            @Override
            public void onClick(View v) {
                // Should only have one view assigned to it so don't bother checking
                DialogActivitiesEdit.this.onListItemClicked(this, getAdapterPosition());
            }
        }
    }

    public static class ActivitiesPreference {

        public final List<String> activities;
        public final Map<String, List<String>> types;

        public ActivitiesPreference(List<String> activities, Map<String, List<String>> types) {
            this.activities = activities;
            this.types = types;
        }
    }
}
