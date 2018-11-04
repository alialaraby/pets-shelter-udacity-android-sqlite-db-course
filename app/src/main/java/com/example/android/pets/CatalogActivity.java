/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.pets.data.PetDbHelper;
import com.example.android.pets.data.PetsContract;
import com.example.android.pets.data.PetsContract.PetEntry;

/**
 * Displays list of pets that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    //setting up an id for our loader
    private static final int PET_LOADER = 0;

    PetDbHelper mDbHelper;
    ListView petListView;
    PetCursorAdapter petCursorAdapter;
    String[] projection = {PetEntry._ID, PetEntry.COLUMN_PET_NAME, PetEntry.COLUMN_PET_BREED};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        //initialize the listview
        petListView = (ListView)findViewById(R.id.pets_list_view);
        //create a view to hold the empty view layout in case the list is empty
        View view = findViewById(R.id.empty_view);
        //set an empty view to the activity in case the listview is empty
        petListView.setEmptyView(view);
        // To access our database, we instantiate our subclass of SQLiteOpenHelper
        // and pass the context, which is the current activity.
        mDbHelper = new PetDbHelper(this, PetDbHelper.DATABASE_NAME, null, PetDbHelper.DATABASE_VERSION);

        //initialising the cursorAdapter
        petCursorAdapter = new PetCursorAdapter(this, null);
        //setting the adapter to the listView
        petListView.setAdapter(petCursorAdapter);

        //initializing the CursorLoader
        getLoaderManager().initLoader(PET_LOADER, null, this);
        //displayDatabaseInfo();

        //setting up the actions for clicking each item in the ListView
        petListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
                //prepare an intent to access the editor activity
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                //extracting the uri of each pet(row) to send with the intent
                Uri uri = ContentUris.withAppendedId(PetEntry.CONTENT_URI, id);
                //this method is used to send a uri with the intent
                intent.setData(uri);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                // open the editor activity
                insertDummyData();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // here we delete all the pets
                showDeleteAllPetsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Temporary helper method to display information in the onscreen TextView about the state of
     * the pets database.
     */
    private void displayDatabaseInfo() {

        /*
        * String[] projection = {PetEntry._ID, PetEntry.COLUMN_PET_NAME, PetEntry.COLUMN_PET_BREED
            , PetEntry.COLUMN_PET_GENDER, PetEntry.COLUMN_PET_WEIGHT};
        *
        * */

        // Perform this raw SQL query "SELECT * FROM pets"
        // to get a Cursor that contains all rows from the pets table.
        //Cursor cursor = db.query(PetEntry.TABLE_NAME, projection, null, null, null, null, null);


        //Perform the query method of the content resolver using the matcher
        Cursor cursor = getContentResolver().query(PetEntry.CONTENT_URI, projection,
                null, null, null);

        //creating an cursorAdapter instance and attaching the cursor to the cursorAdapter
        petCursorAdapter = new PetCursorAdapter(this, cursor);
        //setting the cursorAdapter into the listView
        petListView.setAdapter(petCursorAdapter);

    }


    public long insertDummyData(){

        //creates a contentvalues instance to store the values we need to insert
        ContentValues contentValues = new ContentValues();
        contentValues.put(PetEntry.COLUMN_PET_NAME, "Totto");
        contentValues.put(PetEntry.COLUMN_PET_BREED, "Terrier");
        contentValues.put(PetEntry.COLUMN_PET_GENDER, PetEntry.GENDER_MALE);
        contentValues.put(PetEntry.COLUMN_PET_WEIGHT, 7);

        Uri resultUri = getContentResolver().insert(PetEntry.CONTENT_URI, contentValues);

        if (ContentUris.parseId(resultUri) != -1){
            return ContentUris.parseId(resultUri);
        }else return -1;
    }

    //is called whenever a cursorloader needs to be created
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //return a new cursorloader that will handle creating a new cursor
        // with the data being queried
        return new CursorLoader(getApplicationContext(), PetEntry.CONTENT_URI, projection,
                null, null, null);
    }

    //is called when the created cursorloader is finished loading
    //it takes the returned cursor and pass it to the UI thread
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        //load the returned cursor to the adapter to display its data on the listView
        petCursorAdapter.swapCursor(cursor);
    }

    //is called to reset the cursor whenever the data is changed to clear the old data
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        //pass a null cursor to the adapter
        petCursorAdapter.swapCursor(null);
    }

    //this method shows a dialog to take a confirmation from the user when deleting pets
    public void showDeleteAllPetsDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_delete_all_title);
        builder.setMessage(R.string.dialog_delete_all_message);
        builder.setPositiveButton(R.string.dialog_button_delete_all, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteAllPets();
            }
        });
        builder.setNegativeButton(R.string.dialog_button_ignore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (dialogInterface != null){
                    dialogInterface.dismiss();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //this method is used to delete all pets in the pets table
    public void deleteAllPets(){

        int deleteResult = getContentResolver().delete(PetEntry.CONTENT_URI, null, null);

        Toast.makeText(getApplicationContext(), "all pets are deleted", Toast.LENGTH_SHORT).show();
    }
}
