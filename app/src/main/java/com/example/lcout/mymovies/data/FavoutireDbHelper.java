package com.example.lcout.mymovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by lcout on 19/03/2018.
 */

public class FavoutireDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "FavouriteMovies.db";
    public static final int VERSION = 1;

    FavoutireDbHelper(Context mContext){
        super(mContext,DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_TRABLE = "CREATE TABLE " + FavouriteContract.FavouriteEntry.TABLE_NAME + " (" +
                FavouriteContract.FavouriteEntry._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FavouriteContract.FavouriteEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                FavouriteContract.FavouriteEntry.COLUMN_MOVIE_ID    + " INTEGER NOT NULL);";
        db.execSQL(CREATE_TRABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FavouriteContract.FavouriteEntry.TABLE_NAME);
        onCreate(db);
    }
}
