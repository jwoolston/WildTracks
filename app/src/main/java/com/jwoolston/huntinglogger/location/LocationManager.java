package com.jwoolston.huntinglogger.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.jwoolston.huntinglogger.mapping.MapManager;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class LocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final String KEY_LAST_LATITUDE = LocationManager.class.getCanonicalName() + ".KEY_LAST_LATITUDE";
    public static final String KEY_LAST_LONGITUDE = LocationManager.class.getCanonicalName() + ".KEY_LAST_LONGITUDE";

    private final Context mContext;
    private final MapManager mMapManager;

    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    public LocationManager(Context context, MapManager manager) {
        mContext = context.getApplicationContext();
        mMapManager = manager;
        reloadLastLocation();
        buildGoogleApiClient();
    }

    public void onResume() {
        startLocationUpdates();
    }

    public void onPause() {
        stopLocationUpdates();
        if (mLastLocation != null) updateLastKnownLocationPreference();
    }

    public LatLng reloadLastLocation() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return new LatLng(preferences.getFloat(KEY_LAST_LATITUDE, 0.0f), preferences.getFloat(KEY_LAST_LONGITUDE, 0.0f));
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mMapManager.onLocationChanged(location);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mMapManager.onLocationChanged(mLastLocation);
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
        mGoogleApiClient.connect();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startLocationUpdates();
    }


    private void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
    private void updateLastKnownLocationPreference() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit()
            .putFloat(KEY_LAST_LATITUDE, (float) mLastLocation.getLatitude())
            .putFloat(KEY_LAST_LONGITUDE, (float) mLastLocation.getLongitude())
            .apply();
    }
}
