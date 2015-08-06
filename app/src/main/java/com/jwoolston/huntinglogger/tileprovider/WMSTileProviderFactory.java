package com.jwoolston.huntinglogger.tileprovider;

import android.util.Log;

import com.google.android.gms.maps.model.TileProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class WMSTileProviderFactory {

    private static final String GEOSERVER_FORMAT =
        "http://services.nationalmap.gov/arcgis/rest/services/USGSTopoLarge/MapServer/export" +
            "?service=WMS" +
            "&version=1.1.1" +
            "&request=GetMap" +
            "&layers=yourLayer" +
            "&bbox=%f,%f,%f,%f" +
            "&width=256" +
            "&height=256" +
            //"&srs=EPSG:900913" +
            "&format=png24" +
            "&transparent=true";

    // return a geoserver wms tile layer
    public static TileProvider getTileProvider() {
        return new WMSTileProvider(256, 256) {

            @Override
            public synchronized URL getTileUrl(int x, int y, int zoom) {
                double[] bbox = getBoundingBox(x, y, zoom);
                Log.d("BOUNDING BOX", "BOX: " + Arrays.toString(bbox));
                String s = String.format(Locale.US, GEOSERVER_FORMAT, bbox[MINX],
                    bbox[MINY], bbox[MAXX], bbox[MAXY]);
                URL url = null;
                try {
                    url = new URL(s);
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
                return url;
            }
        };
    }
}
