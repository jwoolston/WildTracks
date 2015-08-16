package com.jwoolston.wildtracks.markers;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UnhandledMarkerClusterManager<T extends ClusterItem> extends ClusterManager<T> {

    private GoogleMap.OnMarkerClickListener mUnhandledMarkerClickListener;

    public UnhandledMarkerClusterManager(Context context, GoogleMap map) {
        super(context, map);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final boolean consumed = super.onMarkerClick(marker);
        return consumed || (mUnhandledMarkerClickListener != null && mUnhandledMarkerClickListener.onMarkerClick(marker));
    }

    public void setUnhandledMarkerClickListener(GoogleMap.OnMarkerClickListener listener) {
        mUnhandledMarkerClickListener = listener;
    }
}
