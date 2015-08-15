package com.jwoolston.wildtracks.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.jwoolston.wildtracks.markers.UserMarker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserMarkerDatabase {

    private static final String TAG = UserMarkerDatabase.class.getSimpleName();

    // Database fields
    private SQLiteDatabase mDatabase;
    private final Helper mHelper;

    private String[] ALL_COLUMNS = {Helper.COLUMN_ID, Helper.COLUMN_NAME, Helper.COLUMN_LATITUDE, Helper.COLUMN_LONGITUDE, Helper.COLUMN_CREATED,
        Helper.COLUMN_ACTIVITY, Helper.COLUMN_TYPE, Helper.COLUMN_ICON, Helper.COLUMN_NOTES};


    public UserMarkerDatabase(Context context) {
        mHelper = new Helper(context);
    }

    public void open() throws SQLException {
        mDatabase = mHelper.getWritableDatabase();
    }

    public void close() {
        mHelper.close();
    }

    public long addUserMarker(UserMarker marker) {
        final ContentValues values = new ContentValues();
        values.put(Helper.COLUMN_NAME, marker.getName());
        values.put(Helper.COLUMN_LATITUDE, marker.getLatitude());
        values.put(Helper.COLUMN_LONGITUDE, marker.getLongitude());
        values.put(Helper.COLUMN_CREATED, marker.getCreated());
        values.put(Helper.COLUMN_ACTIVITY, marker.getActivity());
        values.put(Helper.COLUMN_TYPE, marker.getType());
        values.put(Helper.COLUMN_ICON, marker.getIcon());
        values.put(Helper.COLUMN_NOTES, marker.getNotes());
        Log.d(TAG, "Saving with content values: " + values);
        return mDatabase.insertOrThrow(Helper.TABLE_MARKERS, null, values);
    }

    public void deleteComment(UserMarker marker) {
        long id = marker.getId();
        mDatabase.delete(Helper.TABLE_MARKERS, Helper.COLUMN_ID + " = " + id, null);
        System.out.println("User marker deleted with id: " + id);
    }

    public @NonNull List<UserMarker> getAllMarkers() {
        final List<UserMarker> markers = new ArrayList<>();

        final Cursor cursor = mDatabase.query(Helper.TABLE_MARKERS, ALL_COLUMNS, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final UserMarker marker = cursorToMarker(cursor);
            markers.add(marker);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return markers;
    }

    private UserMarker cursorToMarker(@NonNull Cursor cursor) {
        final int id = cursor.getInt(cursor.getColumnIndexOrThrow(Helper.COLUMN_ID));
        final String name = cursor.getString(cursor.getColumnIndexOrThrow(Helper.COLUMN_NAME));
        final double latititude = cursor.getDouble(cursor.getColumnIndexOrThrow(Helper.COLUMN_LATITUDE));
        final double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Helper.COLUMN_LONGITUDE));
        final long created = cursor.getLong(cursor.getColumnIndexOrThrow(Helper.COLUMN_CREATED));
        final int activity = cursor.getInt(cursor.getColumnIndexOrThrow(Helper.COLUMN_ACTIVITY));
        final int type = cursor.getInt(cursor.getColumnIndexOrThrow(Helper.COLUMN_TYPE));
        final int icon = cursor.getInt(cursor.getColumnIndexOrThrow(Helper.COLUMN_ICON));
        final String notes = cursor.getString(cursor.getColumnIndexOrThrow(Helper.COLUMN_NOTES));
        return new UserMarker(id, name, new LatLng(latititude, longitude), created, activity, type, icon, notes);
    }

    /**
     * @author Jared Woolston (jwoolston@idealcorp.com)
     */
    private static class Helper extends SQLiteOpenHelper {

        private static final String TAG = UserMarkerDatabase.class.getSimpleName();

        static final String TABLE_MARKERS = "markers";
        static final String COLUMN_ID = "_id";
        static final String COLUMN_NAME = "name";
        static final String COLUMN_LATITUDE = "latitude";
        static final String COLUMN_LONGITUDE = "longitude";
        static final String COLUMN_CREATED = "created_on";
        static final String COLUMN_ACTIVITY = "activity";
        static final String COLUMN_TYPE = "type";
        static final String COLUMN_ICON = "icon";
        static final String COLUMN_NOTES = "notes";

        private static final String DATABASE_NAME = "user_markers.db";
        private static final int DATABASE_VERSION = 1;

        private static final String DATABASE_CREATE = "create table "
            + TABLE_MARKERS + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_NAME + " text not null, "
            + COLUMN_LATITUDE + " real not null, "
            + COLUMN_LONGITUDE + " real not null, "
            + COLUMN_CREATED + " integer not null, "
            + COLUMN_ACTIVITY + " integer not null, "
            + COLUMN_TYPE + " integer not null, "
            + COLUMN_ICON + " integer not null, "
            + COLUMN_NOTES + " text not null);";

        private Helper(Context context) {
            super(context, (new File(Environment.getExternalStorageDirectory(), DATABASE_NAME)).getAbsolutePath(), null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading mDatabase from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKERS);
            onCreate(db);
        }
    }
}
