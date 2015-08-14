package com.jwoolston.wildtracks.tileprovider;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class URLCacheTileProvider implements TileProvider {

    private final int mTileWidth;
    private final int mTileHeight;

    private String baseUrl;

    public URLCacheTileProvider(int width, int height, String url) {
        mTileWidth = width;
        mTileHeight = height;
        baseUrl = url;
    }

    public URL getTileUrl(int x, int y, int zoom) {
        try {
            return new URL(baseUrl.replace("{z}", "" + zoom).replace("{x}", "" + x)
                .replace("{y}", "" + y));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public final Tile getTile(int x, int y, int zoom) {
        URL url = this.getTileUrl(x, y, zoom);
        if (url == null) {
            return NO_TILE;
        } else {
            Tile var5;
            try {
                var5 = new Tile(mTileWidth, mTileHeight, fetchTile(url.openStream()));
            } catch (IOException var7) {
                var5 = null;
            }

            return var5;
        }
    }

    private static byte[] fetchTile(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        loadStream(is, os);
        return os.toByteArray();
    }

    private static long loadStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        long total = 0L;

        while (true) {
            int count = is.read(buffer);
            if (count == -1) {
                return total;
            }

            os.write(buffer, 0, count);
            total += (long) count;
        }
    }
}
