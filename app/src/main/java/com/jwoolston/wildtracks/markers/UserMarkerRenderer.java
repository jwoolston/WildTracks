package com.jwoolston.wildtracks.markers;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserMarkerRenderer extends DefaultClusterRenderer<UserMarker> {

    private final Context mContext;

    public UserMarkerRenderer(Context context, GoogleMap map, ClusterManager<UserMarker> clusterManager) {
        super(context.getApplicationContext(), map, clusterManager);
        mContext = context.getApplicationContext();
    }

    @Override
    protected void onBeforeClusterItemRendered(UserMarker marker, MarkerOptions markerOptions) {
        // Draw a single user marker.
        markerOptions.title(marker.getName());
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<UserMarker> cluster, MarkerOptions markerOptions) {
        // Draw multiple people.
        // Note: this method runs on the UI thread. Don't spend too much time in here.
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        // Always render clusters.
        return cluster.getSize() > 1;
    }
}
