package com.jwoolston.huntinglogger.mapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.kml.KmlContainer;
import com.google.maps.android.kml.KmlLayer;
import com.google.maps.android.kml.KmlPlacemark;
import com.google.maps.android.kml.KmlPolygon;
import com.jwoolston.huntinglogger.R;
import com.jwoolston.huntinglogger.dialog.DialogEditPin;
import com.jwoolston.huntinglogger.fragment.FragmentEditUserMarker;
import com.jwoolston.huntinglogger.location.LocationManager;
import com.jwoolston.huntinglogger.settings.DialogActivitiesEdit;
import com.jwoolston.huntinglogger.tileprovider.MapBoxOfflineTileProvider;
import com.jwoolston.huntinglogger.tileprovider.URLCacheTileProvider;
import com.jwoolston.huntinglogger.view.TouchableWrapper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class MapManager implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener, TouchableWrapper.OnUserInteractionCompleteListener {

    private static final String TAG = MapManager.class.getSimpleName();
    private static final String USGS_TOPO_URL = "http://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}";

    public static final String KEY_SELECTED_PROVIDER = MapManager.class.getCanonicalName() + ".KEY_SELECTED_PROVIDER";
    public static final String KEY_PROVIDER_FILE = MapManager.class.getCanonicalName() + ".KEY_PROVIDER_FILE";
    public static final String ACTION_PROVIDER_CHANGED = MapManager.class.getCanonicalName() + ".ACTION_PROVIDER_CHANGED";

    public static final int GOOGLE_TERRAIN = 0;
    public static final int USGS_TOPO_ONLINE = 1;
    public static final int LOCAL_MBTILES_FILE = 2;
    public static final int LOCAL_CACHE_FILE = 3;
    public static final int DEFAULT_PROVIDER = GOOGLE_TERRAIN;

    private final Context mContext;
    private final WrappedMapFragment mMapFragment;
    private final LocalBroadcastManager mLocalBroadcastManager;
    private GoogleMap mMap;

    private Marker mTempMarker;

    private UserLocationCircle mUserLocationCircle;

    private LocationManager mLocationManager;
    private boolean mTrackingLocation;

    private TileOverlay mCurrentMapTiles;

    /**
     * @param lat
     * @param zoom
     *
     * @return
     * @see <a href="https://groups.google.com/d/msg/google-maps-js-api-v3/hDRO4oHVSeM/osOYQYXg2oUJ">Google Groups Explanation</a>
     */
    public static double metersPerPixel(double lat, double zoom) {
        return 156543.03392 * Math.cos(lat * Math.PI / 180) / Math.pow(2, zoom);
    }

    public MapManager(Context context, WrappedMapFragment fragment) {
        mContext = context.getApplicationContext();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mLocalBroadcastManager.registerReceiver(mProviderChangedReceiver, new IntentFilter(ACTION_PROVIDER_CHANGED));
        mLocalBroadcastManager.registerReceiver(mActivitiesUpdatedReceiver, new IntentFilter(DialogActivitiesEdit.ACTION_ACTIVITIES_UPDATED));
        mMapFragment = fragment;
        mMapFragment.setOnUserInteractionCompleteListener(this);
        mTrackingLocation = true;
        initializeMap();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mUserLocationCircle.onCameraUpdate(cameraPosition.zoom);
    }

    @Override
    public void onUpdateMapAfterUserInteraction() {
        mTrackingLocation = false;
    }

    public void onLocationChanged(LatLng location) {
        if (mTrackingLocation) {
            recenterCamera(location, 15);
        }
        mUserLocationCircle.onLocationUpdate(location);
    }

    public void onLocationChanged(Location location) {
        if (mTrackingLocation) {
            recenterCamera(new LatLng(location.getLatitude(), location.getLongitude()), 15);
        }
        mUserLocationCircle.onLocationUpdate(location);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mLocationManager = new LocationManager(mContext, this);
        final LatLng savedLocation = mLocationManager.reloadLastLocation();

        mMap = googleMap;
        mMap.setMyLocationEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        mMap.setOnCameraChangeListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        mUserLocationCircle = new UserLocationCircle(mContext, mMap);
        mUserLocationCircle.onLocationUpdate(savedLocation);
        onLocationChanged(savedLocation);
        recenterCamera(savedLocation, 15);
        selectMapDataProvider();
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "Map clicked at location: " + latLng);
        placeTemporaryPin(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(mTempMarker)) {
            // This is the temp marker
            Log.d(TAG, "Temporary marker clicked!");
        } else {
            // This is a saved marker
            Log.d(TAG, "Saved marker clicked!");
        }
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        showPinDropDialog();
    }

    public void onResume() {
        if (mLocationManager != null) mLocationManager.onResume();
    }

    public void onPause() {
        if (mLocationManager != null) mLocationManager.onPause();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (mLocationManager != null) mLocationManager.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreFromInstanceState(Bundle savedInstanceState) {
        if (mLocationManager != null) mLocationManager.onRestoreFromInstanceState(savedInstanceState);
    }

    public void recenterCamera(LatLng position, float zoom) {
        if (mMap != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
    }

    public void recenterCamera(LatLng position) {
        if (mMap != null) mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
    }

    public KmlLayer loadKML(String file) {
        KmlLayer layer = null;
        // Get a File reference to the MBTiles file.
        final File kml = new File(Environment.getExternalStorageDirectory(), file);
        try {
            final FileInputStream kmlStream = new FileInputStream(kml);
            layer = new KmlLayer(mMap, kmlStream, mContext);
            layer.addLayerToMap();
            moveCameraToKml(layer);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load KML Layer: ", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to load KML Layer: ", e);
        }
        return layer;
    }

    public void moveCameraToKml(KmlLayer kmlLayer) {
        //Retrieve the first container in the KML layer
        KmlContainer container = kmlLayer.getContainers().iterator().next();
        //Retrieve a nested container within the first container
        container = container.getContainers().iterator().next();
        //Retrieve the first placemark in the nested container
        KmlPlacemark placemark = container.getPlacemarks().iterator().next();
        //Retrieve a polygon object in a placemark
        KmlPolygon polygon = (KmlPolygon) placemark.getGeometry();
        //Create LatLngBounds of the outer coordinates of the polygon
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : polygon.getOuterBoundaryCoordinates()) {
            builder.include(latLng);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 1));
    }

    private void initializeMap() {
        mMapFragment.getMapAsync(this);
    }

    private void disableGoogleMapping() {
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
    }

    private void enableUSGSTopoOnline() {
        Log.d(TAG, "Enabling USGS Topographic Online Provider");
        disableGoogleMapping();
        final URLCacheTileProvider mTileProvider = new URLCacheTileProvider(256, 256, USGS_TOPO_URL);
        if (mCurrentMapTiles != null) mCurrentMapTiles.remove();
        mMap.clear();
        mCurrentMapTiles = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileProvider).zIndex(1));
    }

    private void enableLocalMBTiles(String path) {
        Log.d(TAG, "Enabling local MBTiles provider.");
        disableGoogleMapping();
        if (mCurrentMapTiles != null) mCurrentMapTiles.remove();
        mMap.clear();
        // Create new TileOverlayOptions instance.
        final TileOverlayOptions opts = new TileOverlayOptions();

        // Get a File reference to the MBTiles file.
        final File myMBTiles = new File(path);

        // Create an instance of MapBoxOfflineTileProvider.
        final MapBoxOfflineTileProvider provider = new MapBoxOfflineTileProvider(myMBTiles);

        // Set the tile provider on the TileOverlayOptions.
        opts.tileProvider(provider).zIndex(1);

        // Add the tile overlay to the map.
        mCurrentMapTiles = mMap.addTileOverlay(opts);
    }

    private void selectMapDataProvider() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int providerType = preferences.getInt(KEY_SELECTED_PROVIDER, DEFAULT_PROVIDER);
        final String path = preferences.getString(KEY_PROVIDER_FILE, null);
        switch (providerType) {
            case GOOGLE_TERRAIN:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                if (mCurrentMapTiles != null) {
                    mCurrentMapTiles.clearTileCache();
                    mCurrentMapTiles.remove();
                    mCurrentMapTiles = null;
                }
                break;
            case USGS_TOPO_ONLINE:
                enableUSGSTopoOnline();
                break;
            case LOCAL_MBTILES_FILE:
                enableLocalMBTiles(path);
                break;
            case LOCAL_CACHE_FILE:
                disableGoogleMapping();
                break;
        }
    }

    private void showPinDropDialog() {
        final FragmentManager fm = mMapFragment.getActivity().getSupportFragmentManager();
        final DialogEditPin dialog = new DialogEditPin();
        dialog.show(fm, DialogEditPin.class.getCanonicalName());
    }

    private void placeTemporaryPin(LatLng latLng) {
        if (mTempMarker == null) {
            mTempMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title(mContext.getString(R.string.new_marker_title)));
        }
        mTempMarker.setPosition(latLng);
        final FragmentManager fm = mMapFragment.getActivity().getSupportFragmentManager();
        final Fragment existing = fm.findFragmentByTag(FragmentEditUserMarker.class.getCanonicalName());
        if (existing == null) {
            final FragmentEditUserMarker fragment = new FragmentEditUserMarker();
            fragment.setMapManager(this);
            fragment.setMarker(mTempMarker);
            fm.beginTransaction().add(R.id.detail_placeholder, fragment, FragmentEditUserMarker.class.getCanonicalName())
                .setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom).commit();
            fm.executePendingTransactions();
        }
        recenterCamera(latLng);
    }

    public void updateProviderPreferences(int provider, String path) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit()
            .putInt(MapManager.KEY_SELECTED_PROVIDER, provider)
            .putString(MapManager.KEY_PROVIDER_FILE, path)
            .apply();
        notifyAppNewProviderSelected();
    }

    private void notifyAppNewProviderSelected() {
        final Intent intent = new Intent(MapManager.ACTION_PROVIDER_CHANGED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private final BroadcastReceiver mProviderChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            selectMapDataProvider();
        }
    };

    private final BroadcastReceiver mActivitiesUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
}
