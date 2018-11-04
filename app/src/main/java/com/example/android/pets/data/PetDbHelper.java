package com.example.android.pets.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.pets.data.PetsContract.PetEntry;
/**
 * Created by ali on 9/23/2018.
 */

public class PetDbHelper extends SQLiteOpenHelper {

    //constant for the db name
    public static final String DATABASE_NAME = "shelter.db";
    //constant for the db version
    public static int DATABASE_VERSION = 1;
    //constant for the create command used to create tables
    private final String DB_CREATE_TABLE_COMMAND = "CREATE TABLE " +
            PetEntry.TABLE_NAME + "(" + PetEntry._ID + " INTEGER PRIMARY KEY" +
            "," + PetEntry.COLUMN_PET_NAME + " TEXT" + "," + PetEntry.COLUMN_PET_BREED +
            " TEXT" + "," + PetEntry.COLUMN_PET_GENDER + " INTEGER" +
            "," + PetEntry.COLUMN_PET_WEIGHT + " INTEGER" + ")";
    //constant for the drop table command used to delete tables
    private final String DB_DROP_TABLE_COMMAND = "DROP TABLE IF EXISTS " + PetEntry.TABLE_NAME;

    public PetDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    //for when the database is first created
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(DB_CREATE_TABLE_COMMAND);
    }

    //for when the database schema is changed
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {

        db.execSQL(DB_DROP_TABLE_COMMAND);
        onCreate(db);
    }
}
