package com.jwoolston.wildtracks.util;

import android.graphics.Point;

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
     * @param start
     * @param bearing {@code double} The range bearing in degrees.
     * @param range   {@code double} The range to the destination, in km.
     *
     * @return
     */
    public static LatLng calculateTerminalLocationHaversine(LatLng start, double bearing, double range) {
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

    /**
     * Vincenty Direct Solution of Geodesics on the Ellipsoid (c) Chris Veness
     * 2005-2012
     * <p/>
     * from: Vincenty direct formula - T Vincenty, "Direct and Inverse Solutions
     * of Geodesics on the Ellipsoid with application of nested equations", Survey
     * Review, vol XXII no 176, 1975 http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
     * Calculates destination point and final bearing given given start point,
     * bearing & distance, using Vincenty inverse formula for ellipsoids
     *
     * @param start   start point position
     * @param bearing initial bearing in decimal degrees
     * @param range   distance along bearing in km
     *
     * @returns an array of the desination point coordinates and the final bearing
     */
    public static LatLng calculateTerminalLocationVincenty(LatLng start, double bearing, double range) {
        double a = 6378137, b = 6356752.3142, f = 1 / 298.257223563; // WGS-84
        // ellipsiod
        double s = range * 1000;
        double alpha1 = angleToRadians(bearing);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);

        double tanU1 = (1 - f) * Math.tan(angleToRadians(start.latitude));
        double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1)), sinU1 = tanU1 * cosU1;
        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1 - sinAlpha * sinAlpha;
        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double sinSigma = 0, cosSigma = 0, deltaSigma = 0, cos2SigmaM = 0;
        double sigma = s / (b * A), sigmaP = 2 * Math.PI;

        while (Math.abs(sigma - sigmaP) > 1e-12) {
            cos2SigmaM = Math.cos(2 * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            deltaSigma = B
                * sinSigma
                * (cos2SigmaM + B
                / 4
                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6
                * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma)
                * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
            sigmaP = sigma;
            sigma = s / (b * A) + deltaSigma;
        }

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double lat2 = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1, (1 - f) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
        double lambda = Math.atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);
        double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
        double L = lambda - (1 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        double lon2 = (angleToRadians(start.longitude) + L + 3 * Math.PI) % (2 * Math.PI) - Math.PI; // normalise
        // to
        // -180...+180

        //double revAz = Math.atan2(sinAlpha, -tmp); // final bearing, if required
        return new LatLng(angleToDegrees(lat2), angleToDegrees(lon2));
    }

    public static LatLngBoundingBox calculateBoundingBox(LatLng center, double zoom, Point windowSize) {
        final LatLng[] array = new LatLng[4];
        final double mpp = metersPerPixel(center.latitude, zoom);
        // We use one viewport dimen from center to double our search
        final double rangeNS = (windowSize.y * mpp) / 1000.0;
        final double rangeEW = (windowSize.x * mpp) / 1000.0;
        final LatLng north = calculateTerminalLocationVincenty(center, 0, rangeNS);
        final LatLng east = calculateTerminalLocationVincenty(center, 90, rangeEW);
        final LatLng south = calculateTerminalLocationVincenty(center, 180, rangeNS);
        final LatLng west = calculateTerminalLocationVincenty(center, 270, rangeEW);

        final LatLng nw = new LatLng(north.latitude, west.longitude);
        final LatLng ne = new LatLng(north.latitude, east.longitude);
        final LatLng sw = new LatLng(south.latitude, west.longitude);
        final LatLng se = new LatLng(south.latitude, east.longitude);

        array[0] = nw;
        array[1] = ne;
        array[2] = sw;
        array[3] = se;
        return new LatLngBoundingBox(array);
    }

    public static double angleToRadians(double angle) {
        return (angle * Math.PI / 180);
    }

    public static double angleToDegrees(double angle) {
        return (angle * 180 / Math.PI);
    }
}