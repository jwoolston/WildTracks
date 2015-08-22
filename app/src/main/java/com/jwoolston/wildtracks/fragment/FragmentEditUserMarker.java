package com.jwoolston.wildtracks.fragment;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.jwoolston.wildtracks.MapsActivity;
import com.jwoolston.wildtracks.R;
import com.jwoolston.wildtracks.dialog.DialogActivitiesEdit;
import com.jwoolston.wildtracks.mapping.MapManager;
import com.jwoolston.wildtracks.markers.UserMarker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class FragmentEditUserMarker extends Fragment implements Toolbar.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = FragmentEditUserMarker.class.getSimpleName();

    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;

    private MapManager mMapManager;
    private UserMarker mMarker;

    private Toolbar mToolbar;
    private EditText mMarkerName;
    private EditText mMarkerNotes;

    private Spinner mActivitySpinner;
    private Spinner mTypeSpinner;

    private ArrayAdapter<String> mActivityAdapter;
    private ArrayAdapter<String> mTypeAdapter;

    private ImageView mCreationTimeIcon;
    private ImageView mMarkerLocationIcon;
    private TextView mCreationTimeText;
    private TextView mMarkerLocationText;

    private int mTintColor;
    private DialogActivitiesEdit.ActivitiesPreference mActivityPreference;

    public FragmentEditUserMarker() {

    }

    public void setMapManager(MapManager manager) {
        mMapManager = manager;
    }

    public void setMarker(UserMarker marker) {
        mMarker = marker;
        final MenuItem item = mToolbar.getMenu().findItem(R.id.menu_edit_marker_delete);
        final boolean enabled = mMarker.getId() >= 0;
        item.setEnabled(enabled);
        item.setVisible(enabled);
    }

    public UserMarker getMarker() {
        return mMarker;
    }

    public void updateMarkerPosition() {
        // This is stupidly accurate (approximately 0.2 inches) but it matches google maps
        mMarkerLocationText.setText(String.format("%1.7f, %1.7f", mMarker.getLatitude(), mMarker.getLongitude()));
    }

    public void updateMarkerTime(boolean update) {
        if (update) {
            final long time = System.currentTimeMillis();
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            mMarker.setCreated(time);
            mCreationTimeText.setText(getString(R.string.menu_creation_time_prefix, mDateFormat.format(calendar.getTime()), mTimeFormat.format(calendar.getTime())));

        } else {
            mCreationTimeText.setText(getString(R.string.menu_creation_time_prefix, mDateFormat.format(mMarker.getCreated()), mTimeFormat.format(mMarker.getCreated())));
        }
    }

    public void clearMarkerName() {
        mMarkerName.setText("", TextView.BufferType.EDITABLE);
    }

    public void updateMarkerName() {
        mMarkerName.setText(mMarker.getName(), TextView.BufferType.EDITABLE);
    }

    public void clearMarkerNotes() {
        mMarkerNotes.setText("", TextView.BufferType.EDITABLE);
    }

    public void updateMarkerNotes() {
        mMarkerNotes.setText(mMarker.getNotes(), TextView.BufferType.EDITABLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDateFormat = android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext());
        final boolean is24Hr = android.text.format.DateFormat.is24HourFormat(getActivity().getApplicationContext());
        mTimeFormat = new SimpleDateFormat(is24Hr ? "HH:mm:ss" : "h:mm:ss a", Locale.US);
        final View view = inflater.inflate(R.layout.layout_marker_detail, container, false);
        mTintColor = getResources().getColor(android.R.color.holo_purple);

        ViewCompat.setElevation(view, getResources().getDimensionPixelSize(R.dimen.navigation_elevation));

        mToolbar = (Toolbar) view.findViewById(R.id.detail_view_toolbar);
        mToolbar.setTitle("Edit Marker");
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.inflateMenu(R.menu.menu_edit_marker);
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMarker = null;
                ((MapsActivity) getActivity()).hideMarkerEditWindow();
            }
        });

        mActivitySpinner = (Spinner) view.findViewById(R.id.detail_view_activity_spinner);
        mActivitySpinner.setOnItemSelectedListener(this);
        mTypeSpinner = (Spinner) view.findViewById(R.id.detail_view_type_spinner);
        mTypeSpinner.setOnItemSelectedListener(this);

        mMarkerName = (EditText) view.findViewById(R.id.marker_name_edit_text);
        mMarkerNotes = (EditText) view.findViewById(R.id.marker_notes_edit_text);
        mCreationTimeIcon = (ImageView) view.findViewById(R.id.icon_marker_creation_time);
        mMarkerLocationIcon = (ImageView) view.findViewById(R.id.icon_marker_location);
        mCreationTimeText = (TextView) view.findViewById(R.id.label_marker_creation_time);
        mMarkerLocationText = (TextView) view.findViewById(R.id.label_marker_location);

        applyTintColor();
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            repopulateSpinners();
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_edit_marker_done) {
            mMarkerName.clearFocus();
            mMarkerNotes.clearFocus();
            mMarker.setName(mMarkerName.getText().toString());
            mMarker.setNotes(mMarkerNotes.getText().toString());
            mMapManager.saveMarker(mMarker);
            return true;
        } else if (id == R.id.menu_edit_marker_delete) {
            mMarkerName.clearFocus();
            mMarkerNotes.clearFocus();
            mMapManager.deleteMarker(mMarker);
            return false;
        }
        return false;
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p/>
     * Impelmenters can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == mActivitySpinner.getId()) {
            mMarker.setActivity(position);
            mMarker.setIcon(position);
            updateTypeSpinner(position);
        } else if (parent.getId() == mTypeSpinner.getId()) {
            mMarker.setType(position);
        }
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void repopulateSpinners() {
        mActivityPreference = mMapManager.getActivitiesList();
        mActivityAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mActivityPreference.activities);
        mActivityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mActivitySpinner.setAdapter(mActivityAdapter);
        if (mMarker != null) {
            final int activity = mMarker.getActivity();
            if (activity >= 0) {
                mActivitySpinner.setSelection(activity);
                updateTypeSpinner(activity);
            } else {
                mActivitySpinner.setSelection(0);
            }
        }
    }

    private void updateTypeSpinner(int activity) {
        List<String> types;
        if (activity >= 0) {
            final String activityName = mActivityPreference.activities.get(activity);
            types = mActivityPreference.types.get(activityName);
        } else {
            types = new ArrayList<>();
            types.add(0, "");
        }
        mTypeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, types);
        mTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(mTypeAdapter);
        if (mMarker != null && activity >= 0) {
            final int type = mMarker.getType();
            if (type >= 0) {
                mTypeSpinner.setSelection(type);
            }
        }
    }

    private void applyTintColor() {
        mToolbar.setBackgroundColor(mTintColor);
        mCreationTimeIcon.setColorFilter(mTintColor, PorterDuff.Mode.SRC_ATOP);
        mMarkerLocationIcon.setColorFilter(mTintColor, PorterDuff.Mode.SRC_ATOP);
    }
}
