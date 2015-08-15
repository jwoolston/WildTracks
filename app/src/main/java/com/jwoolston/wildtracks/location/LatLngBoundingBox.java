package com.jwoolston.wildtracks.location;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class LatLngBoundingBox {

    final LatLng[] mCorners;

    public LatLngBoundingBox(LatLng[] corners) {
        mCorners = corners;
    }

    public double getMinimumLatitude() {
        return Math.min(Math.min(mCorners[0].latitude, mCorners[1].latitude), Math.min(mCorners[2].latitude, mCorners[3].latitude));
    }

    public double getMaximumLatitude() {
        return Math.max(Math.max(mCorners[0].latitude, mCorners[1].latitude), Math.max(mCorners[2].latitude, mCorners[3].latitude));
    }

    public double getMinimumLongitude() {
        return Math.min(Math.min(mCorners[0].longitude, mCorners[1].longitude), Math.min(mCorners[2].longitude, mCorners[3].longitude));
    }

    public double getMaximumLongitude() {
        return Math.max(Math.max(mCorners[0].longitude, mCorners[1].longitude), Math.max(mCorners[2].longitude, mCorners[3].longitude));
    }

    @Override
    public String toString() {
        return "LatLngBoundingBox{" +
            "mCorners=" + Arrays.toString(mCorners) +
            '}';
    }
}
