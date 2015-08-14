package com.jwoolston.wildtracks.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class UserMarkerDatabase {

    private final Helper mHelper;

    public UserMarkerDatabase(Context context) {

        mHelper = new Helper(context);
    }


    /**
     * @author Jared Woolston (jwoolston@idealcorp.com)
     */
    private static class Helper extends SQLiteOpenHelper {

        private static final String TAG = UserMarkerDatabase.class.getSimpleName();

        static final String TABLE_MARKERS = "markers";
        static final String COLUMN_ID = "_id";
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
            + COLUMN_LATITUDE + " real not null, "
            + COLUMN_LONGITUDE + " real not null, "
            + COLUMN_CREATED + " integer not null, "
            + COLUMN_ACTIVITY + " integer not null, "
            + COLUMN_TYPE + " integer not null, "
            + COLUMN_ICON + " integer not null, "
            + COLUMN_NOTES + " text not null);";

        private Helper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKERS);
            onCreate(db);
        }
    }
}
