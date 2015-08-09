package com.jwoolston.huntinglogger.settings;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jwoolston.huntinglogger.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class DialogActivitiesEdit extends DialogFragment implements View.OnClickListener, Toolbar.OnMenuItemClickListener {

    private static final String TAG = DialogActivitiesEdit.class.getSimpleName();

    private static final String KEY_ACTIVITIES = DialogActivitiesEdit.class.getCanonicalName() + ".KEY_ACTIVITIES";

    private static final Set<String> DEFAULT_ACTIVITIES_SET = new LinkedHashSet<>();
    static {
        DEFAULT_ACTIVITIES_SET.add("Hiking");
        DEFAULT_ACTIVITIES_SET.add("Hunting");
        DEFAULT_ACTIVITIES_SET.add("Fishing");
        DEFAULT_ACTIVITIES_SET.add("Cycling");
    }

    private RecyclerView mRecyclerView;
    private FloatingActionButton mActionButton;

    private List<String> mActivityNames;
    private Map<String, Set<String>> mTypeMap;

    private boolean mShowingActivityDetail = false;
    private String mCurrentActivityDetail = null;

    public DialogActivitiesEdit() {
        super();
    }

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

        reloadPreference();

        mRecyclerView = (RecyclerView) view.findViewById(R.id.item_list_preference_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new Adapter(mActivityNames));

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.edit_activities_toolbar);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mShowingActivityDetail) {
                    dismissAllowingStateLoss();
                } else {
                    mShowingActivityDetail = false;
                    mCurrentActivityDetail = null;
                    mRecyclerView.setAdapter(new Adapter(mActivityNames));
                }
            }
        });
        toolbar.inflateMenu(R.menu.menu_edit_activities);
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        toolbar.setTitle(R.string.dialog_edit_activities_title);

        mActionButton = (FloatingActionButton) view.findViewById(R.id.fab_add_activity);
        mActionButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.fab_add_activity) {
            Toast.makeText(getActivity(), "Creating new activity", Toast.LENGTH_SHORT).show();
            if (!mShowingActivityDetail) {
                addActivity();
            } else {
                addSubType();
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

    protected void addActivity() {

    }

    protected void addSubType() {
        
    }

    protected void reloadPreference() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mActivityNames = new ArrayList<>(preferences.getStringSet(KEY_ACTIVITIES, DEFAULT_ACTIVITIES_SET));
        mTypeMap = new HashMap<>();
        for (String activity : mActivityNames) {
            final String activity_subtypes_key = DialogActivitiesEdit.class.getCanonicalName() + ".ACTIVITIY_SUBTASKS.KEY";
            mTypeMap.put(activity, preferences.getStringSet(activity_subtypes_key, new LinkedHashSet<String>()));
        }
    }

    protected void persistPreference() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_ACTIVITIES, new LinkedHashSet<>(mActivityNames));
        for (String activity : mActivityNames) {
            final String activity_subtypes_key = DialogActivitiesEdit.class.getCanonicalName() + ".ACTIVITIY_SUBTASKS.KEY";
            editor.putStringSet(activity_subtypes_key, mTypeMap.get(activity));
        }
        editor.apply();
    }

    protected void onListItemClicked(Adapter.ViewHolder holder, int position) {
        if (!mShowingActivityDetail) {
            mShowingActivityDetail = true;
            mCurrentActivityDetail = mActivityNames.get(position);
            mRecyclerView.setAdapter(new Adapter(new ArrayList<>(mTypeMap.get(mCurrentActivityDetail))));
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
            // create a new view
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_activity, parent, false);
            // set the view's size, margins, paddings and layout parameters
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

        public void add(int position, String item) {
            mDataset.add(position, item);
            notifyItemInserted(position);
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
            public TextView txtFooter;

            public ViewHolder(View v) {
                super(v);
                v.setClickable(true);
                v.setOnClickListener(this);
                imgView = (ImageView) v.findViewById(R.id.icon);
                txtHeader = (TextView) v.findViewById(R.id.firstLine);
                txtFooter = (TextView) v.findViewById(R.id.secondLine);
            }

            @Override
            public void onClick(View v) {
                // Should only have one view assigned to it so don't bother checking
                DialogActivitiesEdit.this.onListItemClicked(this, getAdapterPosition());
            }
        }
    }
}
