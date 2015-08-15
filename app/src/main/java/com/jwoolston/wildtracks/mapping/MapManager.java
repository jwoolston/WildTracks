package com.jwoolston.wildtracks.mapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

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
import com.jwoolston.wildtracks.MapsActivity;
import com.jwoolston.wildtracks.R;
import com.jwoolston.wildtracks.fragment.FragmentEditUserMarker;
import com.jwoolston.wildtracks.location.LocationManager;
import com.jwoolston.wildtracks.markers.UserMarker;
import com.jwoolston.wildtracks.markers.UserMarkerManager;
import com.jwoolston.wildtracks.settings.DialogActivitiesEdit;
import com.jwoolston.wildtracks.tileprovider.MapBoxOfflineTileProvider;
import com.jwoolston.wildtracks.tileprovider.URLCacheTileProvider;
import com.jwoolston.wildtracks.view.TouchableWrapper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class MapManager implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener, TouchableWrapper.OnUserInteractionCompleteListener, View.OnClickListener {

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

    private UserMarker mTempMarker;

    private UserLocationCircle mUserLocationCircle;

    private LocationManager mLocationManager;
    private UserMarkerManager mUserMarkerManager;

    private boolean mTrackingLocation;

    private TileOverlay mCurrentMapTiles;

    private FragmentEditUserMarker mFragmentEditUserMarker;

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
        mUserMarkerManager = new UserMarkerManager(mContext, this);
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
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        selectMapDataProvider();
        mUserLocationCircle.onLocationUpdate(savedLocation);
        onLocationChanged(savedLocation);
        recenterCamera(savedLocation, 15);
        mMapFragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mMapFragment.getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mUserMarkerManager.reloadUserMarkers(savedLocation, 15, new Point(2 * mMapFragment.getView().getWidth(), 2 * mMapFragment.getView().getHeight()));
            }
        });
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.fab_user_location) {
            mTrackingLocation = true;
            recenterCamera(mUserLocationCircle.getLocation());
        } else if (id == R.id.fab_select_layers) {
            // Show layers
        }
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

    }

    public void onMarkerEditWindowClosed() {
        mMap.setPadding(0, 0, 0, 0);
        if (mTempMarker != null) {
            mTempMarker.removeFromMap();
            mTempMarker = null;
        }
    }

    public void saveCurrentMarker() {
        //TODO: Make this a snackbar
        Toast.makeText(mContext, "Saving marker.", Toast.LENGTH_SHORT).show();
        mUserMarkerManager.addUserMarker(mTempMarker);
        mTempMarker = null;
        ((MapsActivity) mMapFragment.getActivity()).hideMarkerEditWindow();
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

    public void updateProviderPreferences(int provider, String path) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit()
            .putInt(MapManager.KEY_SELECTED_PROVIDER, provider)
            .putString(MapManager.KEY_PROVIDER_FILE, path)
            .apply();
        notifyAppNewProviderSelected();
    }

    public FragmentEditUserMarker getEditUserMarkerFragment() {
        if (mFragmentEditUserMarker == null) {
            mFragmentEditUserMarker = new FragmentEditUserMarker();
        }
        return mFragmentEditUserMarker;
    }

    public void recenterCamera(LatLng position, float zoom) {
        if (mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
    }

    public void recenterCamera(LatLng position) {
        if (mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLng(position));
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
        mUserLocationCircle = new UserLocationCircle(mContext, mMap);
    }

    private void placeTemporaryPin(LatLng latLng) {
        if (mTempMarker == null) {
            final MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title(mContext.getString(R.string.new_marker_title));
            mTempMarker = new UserMarker(options);
            mTempMarker.addToMap(mMap);
            mFragmentEditUserMarker.clearMarkerName();
        }
        mTempMarker.setPosition(latLng);
        mFragmentEditUserMarker.setMapManager(this);
        mFragmentEditUserMarker.setMarker(mTempMarker);
        mFragmentEditUserMarker.updateMarkerPosition();
        mFragmentEditUserMarker.updateMarkerTime();
        showMarkerEditWindow();
        recenterCamera(latLng);
    }

    private void showMarkerEditWindow() {
        mMap.setPadding(mMapFragment.getActivity().getResources().getDimensionPixelSize(R.dimen.detail_view_width), 0, 0, 0);
        recenterCamera(mTempMarker.getPosition());
        ((MapsActivity) mMapFragment.getActivity()).showMarkerEditWindow();
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
