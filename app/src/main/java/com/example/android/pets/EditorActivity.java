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
import android.content.ContentResolver;
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
import android.provider.UserDictionary;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetDbHelper;
import com.example.android.pets.data.PetsContract;
import com.example.android.pets.data.PetsContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    Intent intent;
    Uri uri;

    //this boolean variable is to check if the pet is changed (if any field is touched)
    private boolean mPetHasChanged = false;

    //we set up this onTouchListener and hook it up with the fields to report(set the @mPetHasChanged to true)
    //when they are touched
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPetHasChanged = true;
            return false;
        }
    };

    /** EditText field to enter the pet's name */
    private EditText mNameEditText;

    /** EditText field to enter the pet's breed */
    private EditText mBreedEditText;

    /** EditText field to enter the pet's weight */
    private EditText mWeightEditText;

    /** EditText field to enter the pet's gender */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = 0;

    //an object of PetDbHelper
    private PetDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        //here we hook up the fields with the onTouchListener
        mNameEditText.setOnTouchListener(onTouchListener);
        mBreedEditText.setOnTouchListener(onTouchListener);
        mGenderSpinner.setOnTouchListener(onTouchListener);
        mWeightEditText.setOnTouchListener(onTouchListener);

        //create an intent to receive data sent from the ListView items
        intent = getIntent();
        //get the uri sent from the CatalogActivity
        uri = intent.getData();
        //here we check if the EditorActivity is for inserting a new pet or updating one
        //if the intent is null then no intent or data are sent and we are inserting a new pet\
        //if not then we are getting a uri to a specific pet to update its info
        if (uri == null){

            setTitle(R.string.title_add_pet);

            /*
            * This function tell android that it should redraw the menu.
             * By default, once the menu is created, it won't be redrawn every frame
             * (since that would be useless to redraw the same menu over and over again).
             * You should call this function when you changed something in the option menu
             *(added an element, deleted an element or changed a text).
             *  This way android will know that it's time te redraw the menu and your change will appear.
            */
            invalidateOptionsMenu();
        }else {

            setTitle(R.string.title_edit_pet);
            getLoaderManager().initLoader(0, null, this);
        }
        //initializing the dbhelper
        dbHelper = new PetDbHelper(this, PetDbHelper.DATABASE_NAME,
                null, PetDbHelper.DATABASE_VERSION);
        setupSpinner();
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE;; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN;; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN; // Unknown
            }
        });
    }

    //this method gets the values from the edit texts and save them to the db
    public void savePet(){

        Uri resultUri = null;
        long newPetRow = 0;

        //this variable is to hold the value of the weight editText
        String weight = mWeightEditText.getText().toString().trim();
        //this variable holds the converted value of the weight to int
        int petWeight = 0;

        String petName = mNameEditText.getText().toString().trim();
        String petBreed = mBreedEditText.getText().toString().trim();
        //here we check is the weight field is empty or not
        //if not empty we continue saving the pet
        //if empty we put a zero "0" value in the field to avoid NumberFormat error
        if (!weight.isEmpty()){
            petWeight = Integer.parseInt(weight);
        }else {
            weight = "0";
            petWeight = Integer.parseInt(weight);
        }

        int petGender = mGender;

        //if the pet weight field is empty we set it to 0
        if (Integer.valueOf(petWeight) == null){
            petWeight = 0;
        }

        //if the breed field is empty we set it to UNKNOWN
        if (TextUtils.isEmpty(petBreed)){
            petBreed = "UNKNOWN BREED";
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(PetEntry.COLUMN_PET_NAME, petName);
        contentValues.put(PetEntry.COLUMN_PET_BREED, petBreed);
        contentValues.put(PetEntry.COLUMN_PET_GENDER, petGender);
        contentValues.put(PetEntry.COLUMN_PET_WEIGHT, petWeight);

        //here we perform a check on the editor fields so if they are empty
        //we avoid crashing the app by doing nothing in the savePet() method
        if (!TextUtils.isEmpty(petName )){

            if (uri == null){
                resultUri = getContentResolver().insert(PetEntry.CONTENT_URI, contentValues);
                newPetRow = ContentUris.parseId(resultUri);
            }else {
                newPetRow = getContentResolver().update(uri,
                        contentValues, null, null);
            }

            if (newPetRow != -1){
                //showing a toast messege to say we added a new pet successfully
                Toast.makeText(getApplicationContext(),
                        "pet saved, id: " + newPetRow, Toast.LENGTH_SHORT).show();
            }else {
                //showing a toast messege to say that something went wrong
                Toast.makeText(getApplicationContext(),
                        "something went wrong saving the pet", Toast.LENGTH_SHORT).show();
            }
        }

    }

    //this method is used to delete the selected pet
    public void deletePet(){

        int resultRow = getContentResolver().delete(uri, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // execute the insertPet() method
                savePet();
                //this line is to get out of the editor activity
                leaveActivity();

                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // here we`re gonna delete the selected pet
                deletePetDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                discardChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String[] projection = {PetEntry._ID, PetEntry.COLUMN_PET_NAME, PetEntry.COLUMN_PET_BREED
                , PetEntry.COLUMN_PET_GENDER, PetEntry.COLUMN_PET_WEIGHT};

        //here we must pass the current uri (the one we pass from the intent)
        return new CursorLoader(getApplicationContext(), uri,
                projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if(cursor.moveToFirst()){
            int nameColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
            int breedColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED);
            int genderdColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
            int weightColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);
            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            int gender = cursor.getInt(genderdColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);
            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            mWeightEditText.setText(String.valueOf(weight));
            if (gender == PetEntry.GENDER_UNKNOWN){
                mGenderSpinner.setSelection(0);
            }else if (gender == PetEntry.GENDER_MALE){
                mGenderSpinner.setSelection(1);
            }else {
                mGenderSpinner.setSelection(2);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mNameEditText.setText("");
        mBreedEditText.setText("");
        mGenderSpinner.setSelection(0);
        mWeightEditText.setText("");
    }

    //this method is used to modify the options menu in the run time
    //such as adding new options or deleting ones
    //it is called right before the menu is shown
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);
        if (uri == null){
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    //this method is used to empty the fields and exit the activity
    //it`s used after we finish saving a pet or finish editing one
    public void leaveActivity(){

        mNameEditText.setText("");
        mBreedEditText.setText("");
        mGenderSpinner.setSelection(0);
        mWeightEditText.setText("");
        finish();
    }

    //this method is for showing a dialog to make the user confirm leaving the editing
    // or staying in the editing page
    //we only handle the "keep editing" button in this method the
    //"discard" button needs to be handled explicitly in the "up" and "back" methods
    public void discardChangesDialog(DialogInterface.OnClickListener discardButtonInterfaceListener){

        //the builder class helps creating the parts of the dialog such as title, message and buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_discard_title);
        builder.setMessage(R.string.dialog_discard_message);
        builder.setPositiveButton(R.string.dialog_button_keep, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (dialogInterface != null){
                    dialogInterface.dismiss();
                }
            }
        });
        builder.setNegativeButton(R.string.dialog_button_discard, discardButtonInterfaceListener);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //this method shows a dialog to take a confirmation from the user when deleting pets
    public void deletePetDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_delete_pet_title);
        builder.setMessage(R.string.dialog_delete_pet_message);
        builder.setPositiveButton(R.string.dialog_button_delete_pet, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deletePet();
                Toast.makeText(getApplicationContext(), "pet deleted", Toast.LENGTH_SHORT).show();
                finish();
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

    //here we override the onBackPressed to show the dialog when back button is pressed
    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        /// Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that
        // changes should be discarded and then we pass it to the method that shows the dialog
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        discardChangesDialog(discardButtonClickListener);
    }
}