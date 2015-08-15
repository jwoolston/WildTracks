package com.jwoolston.wildtracks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.SupportMapFragment;
import com.jwoolston.wildtracks.animation.SlideInTransformer;
import com.jwoolston.wildtracks.dialog.DialogLegalNotices;
import com.jwoolston.wildtracks.file.ActivityFilePicker;
import com.jwoolston.wildtracks.mapping.MapManager;
import com.jwoolston.wildtracks.mapping.WrappedMapFragment;
import com.jwoolston.wildtracks.settings.DialogActivitiesEdit;
import com.jwoolston.wildtracks.settings.SettingsActivity;
import com.jwoolston.wildtracks.view.NonSwipeableViewPager;
import com.nononsenseapps.filepicker.FilePickerActivity;

import io.fabric.sdk.android.Fabric;

public class MapsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {

    private static final String TAG = "MapsActivity";

    public static final int HIDE_ALL_WINDOWS = 0;
    public static final int EDIT_USER_MARKER_WINDOW = 1;

    private MapManager mMapManager;
    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;
    private FloatingActionButton mMyLocationButton;
    private FloatingActionButton mLayersButton;
    private NonSwipeableViewPager mPager;
    private PageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_maps);

        mDrawer = (DrawerLayout) findViewById(R.id.main_drawer);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mMyLocationButton = (FloatingActionButton) findViewById(R.id.fab_user_location);
        mLayersButton = (FloatingActionButton) findViewById(R.id.fab_select_layers);

        // Set up the ViewPager
        final ViewPager.PageTransformer transformer = new SlideInTransformer();
        mAdapter = new PageAdapter(getSupportFragmentManager());
        mPager = (NonSwipeableViewPager) findViewById(R.id.detail_pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(0);
        mPager.setPageTransformer(true, transformer);
        mPager.setScrollDurationFactor(5.0);
        mPager.addOnPageChangeListener(this);

        if (savedInstanceState != null) {
            if (mMapManager != null) mMapManager.onRestoreFromInstanceState(savedInstanceState);
            mMyLocationButton.setOnClickListener(mMapManager);
            mLayersButton.setOnClickListener(mMapManager);
        }
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Setup the map
        setUpMapIfNeeded();
        if (mMapManager != null) mMapManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMapManager != null) mMapManager.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 1) {
            mPager.setCurrentItem(0, true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMapManager != null) mMapManager.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MapManager.LOCAL_MBTILES_FILE) {
                final Uri uri = data.getData();
                mMapManager.updateProviderPreferences(MapManager.LOCAL_MBTILES_FILE, uri.getPath());
            } else if (requestCode == MapManager.LOCAL_CACHE_FILE) {
                final Uri uri = data.getData();
                mMapManager.updateProviderPreferences(MapManager.LOCAL_CACHE_FILE, uri.getPath());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        mDrawer.closeDrawers();
        final int id = item.getItemId();
        if (id == R.id.mapping_source_google_terrain) {
            mMapManager.updateProviderPreferences(MapManager.GOOGLE_TERRAIN, null);
            return true;
        } else if (id == R.id.mapping_source_usgs_topo) {
            mMapManager.updateProviderPreferences(MapManager.USGS_TOPO_ONLINE, null);
            return true;
        } else if (id == R.id.mapping_source_local_mbtiles) {
            // We need to ask the user to select a file
            final Intent i = new Intent(this, ActivityFilePicker.class);

            // Set these depending on your use case. These are the defaults.
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
            i.putExtra(ActivityFilePicker.EXTRA_FILTER_EXTENSION, ".mbtiles");

            // Configure initial directory by specifying a String.
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

            startActivityForResult(i, MapManager.LOCAL_MBTILES_FILE);
            return true;
        } else if (id == R.id.menu_activities) {
            showActivitiesEditDialog();
            return true;
        } else if (id == R.id.menu_settings) {
            showSettings();
            return true;
        } else if (id == R.id.menu_legal) {
            showLegalNoticesDialog();
            return true;
        }
        return false;
    }

    public void showMarkerEditWindow() {
        mPager.setCurrentItem(EDIT_USER_MARKER_WINDOW);
    }

    public void hideMarkerEditWindow() {
        mPager.setCurrentItem(HIDE_ALL_WINDOWS);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap(WrappedMapFragment)} once when {@link #mMapManager} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMapManager == null) {
            // Try to obtain the map from the SupportMapFragment.
            final WrappedMapFragment map = ((WrappedMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap(map);
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMapManager} is not null.
     */
    private void setUpMap(WrappedMapFragment map) {
        mMapManager = new MapManager(getApplicationContext(), map);
        mMyLocationButton.setOnClickListener(mMapManager);
        mLayersButton.setOnClickListener(mMapManager);
    }

    private void showActivitiesEditDialog() {
        final FragmentManager fm = getSupportFragmentManager();
        final DialogActivitiesEdit dialog = new DialogActivitiesEdit();
        dialog.show(fm, DialogActivitiesEdit.class.getCanonicalName());
    }

    private void showSettings() {
        final Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showLegalNoticesDialog() {
        final FragmentManager fm = getSupportFragmentManager();
        final DialogLegalNotices dialog = new DialogLegalNotices();
        dialog.show(fm, DialogLegalNotices.class.getCanonicalName());
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Do nothing
    }

    @Override
    public void onPageSelected(int position) {
        if (position == HIDE_ALL_WINDOWS) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPager.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            mMapManager.onMarkerEditWindowClosed();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // Do nothing
    }

    private final class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case EDIT_USER_MARKER_WINDOW:
                    return mMapManager.getEditUserMarkerFragment();
                default:
                    return new PlaceholderFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

    }

    public static class PlaceholderFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_placeholder, container, false);
        }
    }
}
