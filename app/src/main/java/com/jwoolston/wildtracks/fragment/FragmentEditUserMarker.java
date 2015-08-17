package com.jwoolston.wildtracks.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
    private NavigationView mNavigationView;
    private EditText mMarkerName;

    private Menu mMenu;
    private MenuItem mCreationTime;
    private MenuItem mMarkerLocation;

    private int mTintColor;

    public FragmentEditUserMarker() {

    }

    public void setMapManager(MapManager manager) {
        mMapManager = manager;
    }

    public void setMarker(UserMarker marker) {
        mMarker = marker;
    }

    public void updateMarkerPosition() {
        // This is stupidly accurate (approximately 0.2 inches) but it matches google maps
        mMarkerLocation.setTitle(String.format("%1.7f, %1.7f", mMarker.getLatitude(), mMarker.getLongitude()));
    }

    public void updateMarkerTime(boolean update) {
        if (update) {
            final long time = System.currentTimeMillis();
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            mMarker.setCreated(time);
            mCreationTime.setTitle(getString(R.string.menu_creation_time_prefix, mDateFormat.format(calendar.getTime()), mTimeFormat.format(calendar.getTime())));

        } else {
            mCreationTime.setTitle(getString(R.string.menu_creation_time_prefix, mDateFormat.format(mMarker.getCreated()), mTimeFormat.format(mMarker.getCreated())));
        }
    }

    public void clearMarkerName() {
        mMarkerName.setText("", TextView.BufferType.EDITABLE);
    }

    public void updateMarkerName() {
        mMarkerName.setText(mMarker.getName(), TextView.BufferType.EDITABLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDateFormat = android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext());
        final boolean is24Hr = android.text.format.DateFormat.is24HourFormat(getActivity().getApplicationContext());
        mTimeFormat = new SimpleDateFormat(is24Hr ? "HH:mm:ss" : "h:mm:ss a", Locale.US);
        final View view = inflater.inflate(R.layout.layout_marker_detail, container, false);
        mTintColor = getResources().getColor(android.R.color.holo_purple);
        mToolbar = (Toolbar) view.findViewById(R.id.detail_view_toolbar);

        mToolbar.setTitle("Edit Marker");
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.inflateMenu(R.menu.menu_edit_marker);
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MapsActivity) getActivity()).hideMarkerEditWindow();
            }
        });

        mNavigationView = (NavigationView) view.findViewById(R.id.detail_view_navigation_view);
        mMarkerName = (EditText) mNavigationView.findViewById(R.id.marker_name_edit_text);
        mMenu = mNavigationView.getMenu();
        mCreationTime = mMenu.findItem(R.id.menu_creation_time);
        mMarkerLocation = mMenu.findItem(R.id.menu_coordinates);

        applyTintColor();
        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_edit_marker_done) {
            mMarkerName.clearFocus();
            mMarker.setName(mMarkerName.getText().toString());
            mMapManager.saveCurrentMarker();
        }
        return false;
    }

    private void applyTintColor() {
        mToolbar.setBackgroundColor(mTintColor);

        ColorStateList textStateList = new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
            },
            new int[]{
                mTintColor,
                mTintColor
            }
        );

        mNavigationView.setItemIconTintList(textStateList);
    }
}
