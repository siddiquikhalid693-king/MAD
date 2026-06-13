package com.fitplanner.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fitplanner.R;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class UserProfileActivity extends AppCompatActivity {

    private TextInputLayout tilAge, tilHeight, tilCurrentWeight, tilTargetWeight;
    private TextInputEditText etAge, etHeight, etCurrentWeight, etTargetWeight;
    private AutoCompleteTextView actGender, actActivityLevel, actFitnessGoal, actDietType;
    private MaterialButton btnSave;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupDropdowns();
        loadUserProfile();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        tilAge = findViewById(R.id.til_age);
        tilHeight = findViewById(R.id.til_height);
        tilCurrentWeight = findViewById(R.id.til_current_weight);
        tilTargetWeight = findViewById(R.id.til_target_weight);

        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etCurrentWeight = findViewById(R.id.et_current_weight);
        etTargetWeight = findViewById(R.id.et_target_weight);

        actGender = findViewById(R.id.act_gender);
        actActivityLevel = findViewById(R.id.act_activity_level);
        actFitnessGoal = findViewById(R.id.act_fitness_goal);
        actDietType = findViewById(R.id.act_diet_type);

        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void setupDropdowns() {
        String[] genders = getResources().getStringArray(R.array.gender_options);
        actGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders));

        String[] activityLevels = getResources().getStringArray(R.array.activity_level_options);
        actActivityLevel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, activityLevels));

        String[] goals = getResources().getStringArray(R.array.fitness_goal_options);
        actFitnessGoal.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, goals));

        String[] dietTypes = getResources().getStringArray(R.array.diet_type_options);
        actDietType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dietTypes));
    }

    private double existingStartingWeight = 0;

    private void loadUserProfile() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
            
            etAge.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_AGE))));
            actGender.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_GENDER)), false);
            etHeight.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_HEIGHT))));
            etCurrentWeight.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CURRENT_WEIGHT))));
            etTargetWeight.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_TARGET_WEIGHT))));
            actActivityLevel.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ACTIVITY_LEVEL)), false);
            actFitnessGoal.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FITNESS_GOAL)), false);
            actDietType.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_DIET_TYPE)), false);
            
            existingStartingWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_STARTING_WEIGHT));
            
            cursor.close();
        }
    }

    private void saveProfile() {
        if (!validateInput()) return;

        if (userId == -1) {
            String email = sessionManager.getUserEmail();
            userId = dbHelper.getUserIdByEmail(email);
        }

        if (userId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        double currentWeight = Double.parseDouble(etCurrentWeight.getText().toString());
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_AGE, Integer.parseInt(etAge.getText().toString()));
        values.put(DatabaseHelper.COL_USER_GENDER, actGender.getText().toString());
        values.put(DatabaseHelper.COL_USER_HEIGHT, Double.parseDouble(etHeight.getText().toString()));
        values.put(DatabaseHelper.COL_USER_CURRENT_WEIGHT, currentWeight);
        values.put(DatabaseHelper.COL_USER_TARGET_WEIGHT, Double.parseDouble(etTargetWeight.getText().toString()));
        values.put(DatabaseHelper.COL_USER_ACTIVITY_LEVEL, actActivityLevel.getText().toString());
        values.put(DatabaseHelper.COL_USER_FITNESS_GOAL, actFitnessGoal.getText().toString());
        values.put(DatabaseHelper.COL_USER_DIET_TYPE, actDietType.getText().toString());

        // Set starting weight only if it hasn't been set before
        if (existingStartingWeight <= 0) {
            values.put(DatabaseHelper.COL_USER_STARTING_WEIGHT, currentWeight);
        }

        int result = dbHelper.updateUserProfile(userId, values);
        if (result > 0) {
            Toast.makeText(this, R.string.profile_saved_success, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, BMICalculatorActivity.class));
        } else {
            Toast.makeText(this, R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput() {
        boolean isValid = true;
        
        // Reset errors
        tilAge.setError(null);
        tilHeight.setError(null);
        tilCurrentWeight.setError(null);
        tilTargetWeight.setError(null);

        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String currentWeightStr = etCurrentWeight.getText().toString().trim();
        String targetWeightStr = etTargetWeight.getText().toString().trim();

        if (ageStr.isEmpty()) {
            tilAge.setError("Required");
            isValid = false;
        } else {
            int age = Integer.parseInt(ageStr);
            if (age < 10 || age > 100) {
                tilAge.setError("Invalid age (10-100)");
                isValid = false;
            }
        }

        if (heightStr.isEmpty()) {
            tilHeight.setError("Required");
            isValid = false;
        } else {
            double height = Double.parseDouble(heightStr);
            if (height < 50 || height > 250) {
                tilHeight.setError("Invalid height (50-250 cm)");
                isValid = false;
            }
        }

        if (currentWeightStr.isEmpty()) {
            tilCurrentWeight.setError("Required");
            isValid = false;
        } else {
            double weight = Double.parseDouble(currentWeightStr);
            if (weight < 20 || weight > 300) {
                tilCurrentWeight.setError("Invalid weight (20-300 kg)");
                isValid = false;
            }
        }

        if (targetWeightStr.isEmpty()) {
            tilTargetWeight.setError("Required");
            isValid = false;
        } else {
            double weight = Double.parseDouble(targetWeightStr);
            if (weight < 20 || weight > 300) {
                tilTargetWeight.setError("Invalid weight (20-300 kg)");
                isValid = false;
            }
        }

        if (actGender.getText().toString().isEmpty()) {
            actGender.setError("Required");
            isValid = false;
        }

        return isValid;
    }
}
