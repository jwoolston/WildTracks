package com.jwoolston.wildtracks.markers;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;
import com.jwoolston.wildtracks.data.UserMarkerDatabase;
import com.jwoolston.wildtracks.mapping.MapManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private final SparseArray<Set<UserMarker>> mUserMarkerMap;
    private final ExecutorService mLoaderService;

    private Future<List<UserMarker>> mCurrentFuture;

    public UserMarkerManager(Context context, MapManager manager) {
        mContext = context.getApplicationContext();
        mMapManager = manager;
        mUserMarkerDatabase = new UserMarkerDatabase(mContext);
        mUserMarkerDatabase.open();
        mUserMarkerMap = new SparseArray<>();
        mLoaderService = Executors.newSingleThreadExecutor();
    }

    public Set<UserMarker> getUserMarkersForActivity(int i) {
        Set<UserMarker> set;
        if (i >= 0) {
            set = mUserMarkerMap.get(i);
            if (set == null) {
                set = new HashSet<>();
                mUserMarkerMap.put(i, set);
            }
        } else {
            set = new HashSet<>();
            for (int index = 0; index < mUserMarkerMap.size(); ++index){
                set.addAll(mUserMarkerMap.valueAt(index));
            }
        }
        return set;
    }

    public void saveUserMarker(UserMarker marker) {
        // Save the marker in the database
        mUserMarkerDatabase.addUserMarker(marker);
        // Switch to having the cluster manager manage it
        mMapManager.addUserMarker(marker);
        // Remove the old self managed marker
        marker.removeFromMap();
    }

    public void reloadUserMarkers(LatLng location, float zoom, Point window) {
        Log.d(TAG, "Reloading user markers around location: " + location + " Window: " + window);
        mCurrentFuture = mLoaderService.submit(new LoaderTask(this, location, zoom, window));
    }

    public void updateVisibleMarkers(LatLng location, float zoom, Point window) {

    }

    private static class LoaderTask implements Callable<List<UserMarker>> {

        private static final String TAG = LoaderTask.class.getSimpleName();

        private final UserMarkerManager mUserMarkerManager;
        private final LatLng mLocation;
        private final float mZoom;
        private final Point mWindow;

        private LoaderTask(UserMarkerManager manager, LatLng location, float zoom, Point window) {
            mUserMarkerManager = manager;
            mLocation = location;
            mZoom = zoom;
            mWindow = window;
        }

        @Override
        public List<UserMarker> call() throws Exception {
            final List<UserMarker> markers = mUserMarkerManager.mUserMarkerDatabase.getMarkersAroundLocation(mLocation, mZoom, mWindow);
            Log.d(TAG, "Markers: " + markers);
            final SparseArray<Set<UserMarker>> map = mUserMarkerManager.mUserMarkerMap;
            for (UserMarker marker : markers) {
                Set<UserMarker> set = map.get(marker.getActivity());
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(marker);
                map.put(marker.getActivity(), set);
            }
            mUserMarkerManager.mMapManager.addUserMarkers();
            return markers;
        }
    }
}
