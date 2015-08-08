package com.jwoolston.huntinglogger.mapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
import com.jwoolston.huntinglogger.tileprovider.MapBoxOfflineTileProvider;
import com.jwoolston.huntinglogger.tileprovider.URLCacheTileProvider;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class MapManager implements GoogleMap.OnMyLocationChangeListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = MapManager.class.getSimpleName();
    private static final String USGS_TOPO_URL = "http://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}";

    public static final String KEY_LAST_LATITUDE = MapManager.class.getCanonicalName() + ".KEY_LAST_LATITUDE";
    public static final String KEY_LAST_LONGITUDE = MapManager.class.getCanonicalName() + ".KEY_LAST_LONGITUDE";
    public static final String KEY_SELECTED_PROVIDER = MapManager.class.getCanonicalName() + ".KEY_SELECTED_PROVIDER";
    public static final String KEY_PROVIDER_FILE = MapManager.class.getCanonicalName() + ".KEY_PROVIDER_FILE";
    public static final String ACTION_PROVIDER_CHANGED = MapManager.class.getCanonicalName() + ".ACTION_PROVIDER_CHANGED";

    public static final int GOOGLE_TERRAIN = 0;
    public static final int USGS_TOPO_ONLINE = 1;
    public static final int LOCAL_MBTILES_FILE = 2;
    public static final int LOCAL_CACHE_FILE = 3;
    public static final int DEFAULT_PROVIDER = GOOGLE_TERRAIN;

    private final Context mContext;
    private final SupportMapFragment mMapFragment;
    private final LocalBroadcastManager mLocalBroadcastManager;
    private GoogleMap mMap;

    private Marker mTempMarker;

    private Location mLastLocation;

    private TileOverlay mCurrentMapTiles;

    public MapManager(Context context, SupportMapFragment fragment) {
        mContext = context.getApplicationContext();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mLocalBroadcastManager.registerReceiver(mProviderChangedReceiver, new IntentFilter(ACTION_PROVIDER_CHANGED));
        mMapFragment = fragment;
        initializeMap();
    }

    private void initializeMap() {
        mMapFragment.getMapAsync(this);
    }

    public void onPause() {
        if (mLastLocation != null) updateLastKnownLocationPreference();
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (mLastLocation == null) {
            recenterCamera(new LatLng(location.getLatitude(), location.getLongitude()), 15);
        }
        mLastLocation = location;
    }

    public void recenterCamera(LatLng position, float zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMyLocationChangeListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        selectMapDataProvider();

        reloadLastLocation();
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

    private final BroadcastReceiver mProviderChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            selectMapDataProvider();
        }
    };


    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "Map clicked at location: " + latLng);
        placeTemporaryPin(latLng);
    }

    private void placeTemporaryPin(LatLng latLng) {
        if (mTempMarker == null) {
            mTempMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title(mContext.getString(R.string.new_marker_title)));
        } else {
            mTempMarker.setPosition(latLng);
        }
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

    public void updateProviderPreferences(int provider, String path) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit()
            .putInt(MapManager.KEY_SELECTED_PROVIDER, provider)
            .putString(MapManager.KEY_PROVIDER_FILE, path)
            .apply();
        notifyAppNewProviderSelected();
    }

    private void updateLastKnownLocationPreference() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit()
            .putFloat(MapManager.KEY_LAST_LATITUDE, (float) mLastLocation.getLatitude())
            .putFloat(MapManager.KEY_LAST_LONGITUDE, (float) mLastLocation.getLongitude())
            .apply();
    }

    private void notifyAppNewProviderSelected() {
        final Intent intent = new Intent(MapManager.ACTION_PROVIDER_CHANGED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void reloadLastLocation() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final LatLng position = new LatLng(preferences.getFloat(KEY_LAST_LATITUDE, 0.0f), preferences.getFloat(KEY_LAST_LONGITUDE, 0.0f));
        recenterCamera(position, 15);
    }
}
