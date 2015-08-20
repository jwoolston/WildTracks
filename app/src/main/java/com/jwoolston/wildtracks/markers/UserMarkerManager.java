package com.jwoolston.wildtracks.markers;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;
import com.jwoolston.wildtracks.data.UserMarkerDatabase;
import com.jwoolston.wildtracks.mapping.MapManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // Mapped by marker Activity to Set<UserMarker>
    private final SparseArray<Set<UserMarker>> mUserMarkerMap;

    // Mapped by marker ID to Activity
    private final Map<Long, Integer> mUserMarkerActivityMap;
    private final ExecutorService mLoaderService;

    private Future<List<UserMarker>> mCurrentFuture;

    public UserMarkerManager(Context context, MapManager manager) {
        mContext = context.getApplicationContext();
        mMapManager = manager;
        mUserMarkerDatabase = new UserMarkerDatabase(mContext);
        mUserMarkerDatabase.open();
        mUserMarkerMap = new SparseArray<>();
        mUserMarkerActivityMap = new HashMap<>();
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
            Log.d(TAG, "Marker map size: " + mUserMarkerMap.size());
            for (int index = 0; index < mUserMarkerMap.size(); ++index) {
                final Set<UserMarker> subset = mUserMarkerMap.valueAt(index);
                Log.d(TAG, "Key at index: " + index + " - " + mUserMarkerMap.keyAt(index));
                Log.d(TAG, "Subset at index: " + index + " - " + subset);
                set.addAll(subset);
            }
        }
        return set;
    }

    public void saveUserMarker(UserMarker marker) {
        // Remove the old self managed marker if it existed
        marker.removeFromMap();
        // Save the marker in the database
        mUserMarkerDatabase.addOrUpdateUserMarker(marker);
        // Check if we need to update our in memory mapping
        if (marker.getId() >= 0) {
            final Integer activity = mUserMarkerActivityMap.get(marker.getId());
            if (activity != null && activity != marker.getActivity()) {
                // This marker already exists in the map, find it and move it
                final Set<UserMarker> original = mUserMarkerMap.get(activity);
                original.remove(marker);
                Set<UserMarker> newSet = mUserMarkerMap.get(marker.getActivity());
                if (newSet == null) newSet = new HashSet<>();
                newSet.add(marker);
                mUserMarkerMap.put(marker.getActivity(), newSet);
                mUserMarkerActivityMap.put(marker.getId(), marker.getActivity());
            } else {
                // This marker does not exist in the map, add it
                Set<UserMarker> set = mUserMarkerMap.get(marker.getActivity());
                if (set == null) set = new HashSet<>();
                set.add(marker);
                mUserMarkerMap.put(marker.getActivity(), set);
                mUserMarkerActivityMap.put(marker.getId(), marker.getActivity());
            }
        }
        // Switch to having the cluster manager manage it
        mMapManager.addUserMarker(marker);
    }

    public void deleteUserMarker(UserMarker marker) {
        // Remove the marker from the database
        mUserMarkerDatabase.deleteUserMarker(marker);
        // Update the memory maps
        final int activity = mUserMarkerActivityMap.get(marker.getId());
        mUserMarkerActivityMap.remove(marker.getId());
        mUserMarkerMap.get(activity).remove(marker);
        mMapManager.deleteUserMarker(marker);
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
            final Map<Long, Integer> idMap = mUserMarkerManager.mUserMarkerActivityMap;
            for (UserMarker marker : markers) {
                Set<UserMarker> set = map.get(marker.getActivity());
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(marker);
                map.put(marker.getActivity(), set);
                idMap.put(marker.getId(), marker.getActivity());
            }
            mUserMarkerManager.mMapManager.addUserMarkers();
            return markers;
        }
    }
}
