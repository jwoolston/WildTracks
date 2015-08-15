package com.jwoolston.wildtracks.util;

import android.graphics.Point;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.jwoolston.wildtracks.location.LatLngBoundingBox;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class MapUtils {

    private static final String TAG = MapUtils.class.getSimpleName();

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

    /**
     *
     * @param start
     * @param bearing {@code double} The range bearing in degrees.
     * @param range {@code double} The range to the destination, in km.
     * @return
     */
    public static LatLng calculateTerminalLocation(LatLng start, double bearing, double range) {
        final double distance = range / 6371;
        final double bearingRad = angleToRadians(bearing);

        final double lat1 = angleToRadians(start.latitude);
        final double lon1 = angleToRadians(start.longitude);

        final double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance) + Math.cos(lat1) * Math.sin(distance) * Math.cos(bearingRad));
        final double lon2 = lon1 + Math.atan2(Math.sin(bearingRad) * Math.sin(distance) * Math.cos(lat1),
            Math.cos(distance) - Math.sin(lat1) * Math.sin(lat2));
        if (Double.isNaN(lat2) || Double.isNaN(lon2)) return null;
        return new LatLng(angleToDegrees(lat2), angleToDegrees(lon2));
    }

    public static LatLngBoundingBox calculateBoundingBox(LatLng center, double zoom, Point windowSize) {
        final LatLng[] array = new LatLng[4];
        final double mpp = metersPerPixel(center.latitude, zoom);
        Log.d(TAG, "Pixels per meter: " + mpp);
        final double rangeNS = 0.5 * (windowSize.y / mpp) / 1000.0;
        final double rangeEW = 0.5 * (windowSize.x / mpp) / 1000.0;
        Log.d(TAG, "Ranges: " + rangeNS + "/" + rangeEW);
        final LatLng north = calculateTerminalLocation(center, 0, rangeNS);
        final LatLng east = calculateTerminalLocation(center, 90, rangeEW);
        final LatLng south = calculateTerminalLocation(center, 180, rangeNS);
        final LatLng west = calculateTerminalLocation(center, 270, rangeEW);

        final LatLng nw = new LatLng(north.latitude, west.longitude);
        final LatLng ne = new LatLng(north.latitude, east.longitude);
        final LatLng sw = new LatLng(south.latitude, west.longitude);
        final LatLng se = new LatLng(south.latitude, east.longitude);

        array[0] = nw; array[1] = ne; array[2] = sw; array[3] = se;
        return new LatLngBoundingBox(array);
    }

    public static double angleToRadians(double angle) {
        return (angle * Math.PI / 180);
    }

    public static double angleToDegrees(double angle) {
        return (angle * 180 / Math.PI);
    }
}