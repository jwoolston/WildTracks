package com.jwoolston.huntinglogger;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.jwoolston.huntinglogger.dialog.DialogDataSourceSelector;
import com.jwoolston.huntinglogger.dialog.DialogLegalNotices;
import com.jwoolston.huntinglogger.mapping.MapManager;

public class MapsActivity extends AppCompatActivity {

    private static final String TAG = "MapsActivity";

    private MapManager mMapManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Setup the map
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_map_provider) {
            showMapProviderChooser();
            return true;
        } else if (id == R.id.menu_legal) {
            showLegalNoticesDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap(GoogleMap)} once when {@link #mMapManager} is not null.
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
            final SupportMapFragment map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
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
    private void setUpMap(SupportMapFragment map) {
        mMapManager = new MapManager(getApplicationContext(), map);

        //if (mLastLocation != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));

        findViewById(R.id.map).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //At this point the layout is complete and the
                //dimensions of myView and any child views are known.
                //loadKML();
            }
        });
    }

    private void showMapProviderChooser() {
        final FragmentManager fm = getSupportFragmentManager();
        final DialogDataSourceSelector dialog = new DialogDataSourceSelector();
        dialog.show(fm, DialogDataSourceSelector.class.getCanonicalName());
    }

    private void showLegalNoticesDialog() {
        final FragmentManager fm = getSupportFragmentManager();
        final DialogLegalNotices dialog = new DialogLegalNotices();
        dialog.show(fm, DialogLegalNotices.class.getCanonicalName());
    }
}
