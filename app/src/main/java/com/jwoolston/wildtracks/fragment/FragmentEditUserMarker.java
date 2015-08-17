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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.jwoolston.wildtracks.MapsActivity;
import com.jwoolston.wildtracks.R;
import com.jwoolston.wildtracks.mapping.MapManager;
import com.jwoolston.wildtracks.markers.UserMarker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class FragmentEditUserMarker extends Fragment implements Toolbar.OnMenuItemClickListener {

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

    private ImageView mCreationTimeIcon;
    private ImageView mMarkerLocationIcon;
    private TextView mCreationTimeText;
    private TextView mMarkerLocationText;

    private int mTintColor;

    public FragmentEditUserMarker() {

    }

    public void setMapManager(MapManager manager) {
        mMapManager = manager;
    }

    public void setMarker(UserMarker marker) {
        mMarker = marker;
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
        mTypeSpinner = (Spinner) view.findViewById(R.id.detail_view_type_spinner);

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
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_edit_marker_done) {
            mMarkerName.clearFocus();
            mMarkerNotes.clearFocus();
            mMarker.setName(mMarkerName.getText().toString());
            mMapManager.saveCurrentMarker();
        }
        return false;
    }

    private void applyTintColor() {
        mToolbar.setBackgroundColor(mTintColor);
        mCreationTimeIcon.setColorFilter(mTintColor, PorterDuff.Mode.SRC_ATOP);
        mMarkerLocationIcon.setColorFilter(mTintColor, PorterDuff.Mode.SRC_ATOP);
    }
}
