package com.example.android.pets.data;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by ali on 9/23/2018.
 */

public class PetsContract {

    //constant for the content authority
    public static final String CONTENT_AUTHORITY = "com.example.android.pets";
    //constant for the basic part of the uri we`re gonna use (schema + authority)
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    //constant for the path table name, which is the name of the table to get data from
    public static final String PATH_PETS = "pets";


    //we create an inner class for each table
    //this one is for the "Pets" table
    public static final class PetEntry implements BaseColumns {

        //constants for the pets table and its columns` names
        public static final String TABLE_NAME = "Pets";
        //the id column is automatically created and handled
        //so, this is how we define it in the contract class
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PET_NAME = "name";
        public static final String COLUMN_PET_BREED = "breed";
        public static final String COLUMN_PET_GENDER = "gender";
        public static final String COLUMN_PET_WEIGHT = "weight";

        //constants for the gender column
        public static final int GENDER_MALE = 1;
        public static final int GENDER_FEMALE = 2;
        public static final int GENDER_UNKNOWN = 0;

        //constant to the full uri of the pets table
        //the withAppendPath() method is used to append a string to a uri
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PETS);
    }
}
