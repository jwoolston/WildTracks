package com.jwoolston.wildtracks.markers;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.jwoolston.wildtracks.data.UserMarkerDatabase;
import com.jwoolston.wildtracks.mapping.MapManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserMarkerManager {

    private static final String TAG = UserMarkerManager.class.getSimpleName();

    private final Context mContext;
    private final MapManager mMapManager;
    private final UserMarkerDatabase mUserMarkerDatabase;

    private final Map<Marker, UserMarker> mUserMarkerMap;
    private final ExecutorService mLoaderService;

    private Future<List<UserMarker>> mCurrentFuture;

    public UserMarkerManager(Context context, MapManager manager) {
        mContext = context.getApplicationContext();
        mMapManager = manager;
        mUserMarkerDatabase = new UserMarkerDatabase(mContext);
        mUserMarkerDatabase.open();
        mUserMarkerMap = new HashMap<>();
        mLoaderService = Executors.newSingleThreadExecutor();
    }

    public void addUserMarker(UserMarker marker) {
        mUserMarkerDatabase.addUserMarker(marker);
    }

    public void reloadUserMarkers(LatLng location, float zoom, Point window) {
        Log.d(TAG, "Reloading user markers around location: " + location + " Window: " + window);
        mCurrentFuture = mLoaderService.submit(new LoaderTask(mUserMarkerDatabase, location, zoom, window));
    }

    public void updateVisibleMarkers(LatLng location, float zoom, Point window) {

    }

    private static class LoaderTask implements Callable<List<UserMarker>> {

        private static final String TAG = LoaderTask.class.getSimpleName();

        private final UserMarkerDatabase mUserMarkerDatabase;
        private final LatLng mLocation;
        private final float mZoom;
        private final Point mWindow;

        private LoaderTask(UserMarkerDatabase database, LatLng location, float zoom, Point window) {
            mUserMarkerDatabase = database;
            mLocation = location;
            mZoom = zoom;
            mWindow = window;
        }

        @Override
        public List<UserMarker> call() throws Exception {
            final List<UserMarker> markers = mUserMarkerDatabase.getMarkersAroundLocation(mLocation, mZoom, mWindow);
            Log.d(TAG, "Markers: " + markers);
            return markers;
        }
    }
}
