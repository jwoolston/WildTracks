package com.jwoolston.huntinglogger.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jwoolston.huntinglogger.R;

import java.util.Arrays;
import java.util.List;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class ItemListPreference extends DialogPreference implements View.OnClickListener, RecyclerView.OnItemTouchListener {

    private static final String TAG = ItemListPreference.class.getSimpleName();

    private static final String[] DEFAULT_ARRAY = new String[] {
        "Hiking", "Hunting", "Fishing", "Cycling"
    };

    private static final Gson GSON = new Gson();
    private static final String DEFAULT_VALUE = GSON.toJson(DEFAULT_ARRAY);

    private RecyclerView mRecyclerView;
    private FloatingActionButton mActionButton;

    private String mPackedString;
    private List<String> mStringList;

    public ItemListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_item_list);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(context.getResources().getDrawable(R.mipmap.ic_launcher));

        Log.d(TAG, DEFAULT_VALUE);
        mStringList = Arrays.asList(GSON.fromJson(DEFAULT_VALUE, String[].class));
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();
        mRecyclerView = (RecyclerView) view.findViewById(R.id.item_list_preference_list);
        mRecyclerView.addOnItemTouchListener(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(new Adapter(mStringList));

        mActionButton = (FloatingActionButton) view.findViewById(R.id.item_list_preference_add);
        mActionButton.setOnClickListener(this);
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistString(mPackedString);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        Log.d(TAG, "onSetInitialValue() called.");
        if (restorePersistedValue) {
            // Restore existing state
            mPackedString = getPersistedString(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            mPackedString = (String) defaultValue;
            persistString(mPackedString);
        }
        mStringList = Arrays.asList(GSON.fromJson(mPackedString, String[].class));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(getContext(), "Creating new activity", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    protected class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        protected List<String> mDataset;

        protected Adapter(List<String> dataset) {
            mDataset = dataset;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_list_preference, parent, false);
            // set the view's size, margins, paddings and layout parameters
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

        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public ImageView imgView;
            public TextView txtHeader;
            public TextView txtFooter;

            public ViewHolder(View v) {
                super(v);
                imgView = (ImageView) v.findViewById(R.id.icon);
                txtHeader = (TextView) v.findViewById(R.id.firstLine);
                txtFooter = (TextView) v.findViewById(R.id.secondLine);
            }
        }
    }
}
