package com.tomsitter.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.tomsitter.sunshine.data.WeatherContract.LocationEntry;
import com.tomsitter.sunshine.data.WeatherContract.WeatherEntry;

/**
 * Created by tom on 17/08/14.
 */
public class WeatherProvider extends ContentProvider {

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDbHelper mOpenHelper;
    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherEntry.TABLE_NAME + " INNER JOIN " +
                LocationEntry.TABLE_NAME + " ON " +
                        WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_LOC_KEY +
                        " = " + LocationEntry.TABLE_NAME + "." + LocationEntry._ID
        );
    }

    private static final String sLocationSettingSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
    private static final String sLocationSettingWithStartDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? " +
            " AND " + WeatherEntry.COLUMN_DATETEXT + " >= ?";
    private static final String sLocationSettingWithDaySelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? " +
            " AND " + WeatherEntry.COLUMN_DATETEXT + " = ?";

    private Cursor getWeatherByLocationSettings(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherEntry.getStartDateFromUri(uri);

        String selection;
        String[] selectionArgs;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[]{locationSetting, startDate};
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }


    private Cursor getWeatherByLocationSettingWithDate(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String day = WeatherEntry.getDateFromUri(uri);
        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingWithDaySelection,
                new String[]{locationSetting, day},
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return LocationEntry.CONTENT_ITEM_TYPE;
            case LOCATION:
                return LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
    }



    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case WEATHER:
            {
                rowsUpdated = db.update(WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case LOCATION:
            {
                rowsUpdated = db.update(LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }

        if (0 != rowsUpdated)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case WEATHER:
            {
                long _id = db.insert(WeatherEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = WeatherEntry.buildWeatherUri(_id);
                else
                    throw new SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION:
            {
                long _id = db.insert(LocationEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = LocationEntry.buildLocationUri(_id);
                else
                    throw new SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsDeleted;

        switch (sUriMatcher.match(uri)) {
            case WEATHER:
            {
                rowsDeleted = db.delete(WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case LOCATION:
            {
                rowsDeleted = db.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }

        if(null == selection || 0 != rowsDeleted) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingWithDate(uri, projection, sortOrder);
                break;
            }
            case WEATHER_WITH_LOCATION:
            {
                retCursor = getWeatherByLocationSettings(uri, projection, sortOrder);
                break;
            }
            case WEATHER:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //group by
                        null, //having
                        sortOrder
                );
                break;
            }
            case LOCATION_ID:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        WeatherEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;

            }
            case LOCATION:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch(match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WeatherEntry.TABLE_NAME, null, value);
                        if (-1 != _id) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
