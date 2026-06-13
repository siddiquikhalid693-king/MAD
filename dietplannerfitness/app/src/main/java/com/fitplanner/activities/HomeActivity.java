package com.fitplanner.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.fitplanner.R;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvCurrentWeight, tvTargetWeight, tvBmi, tvGoal, tvDietType;
    private MaterialCardView cardDiet, cardWorkout, cardProgress, cardSettings;
    private MaterialButton btnEditProfile;
    
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvCurrentWeight = findViewById(R.id.tv_current_weight);
        tvTargetWeight = findViewById(R.id.tv_target_weight);
        tvBmi = findViewById(R.id.tv_bmi);
        tvGoal = findViewById(R.id.tv_goal);
        tvDietType = findViewById(R.id.tv_diet_type);

        btnEditProfile = findViewById(R.id.btn_edit_profile);

        cardDiet = findViewById(R.id.card_diet_plan);
        cardWorkout = findViewById(R.id.card_workout_plan);
        cardProgress = findViewById(R.id.card_progress);
        cardSettings = findViewById(R.id.card_settings);
    }

    private void loadUserData() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME));
            double currentWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CURRENT_WEIGHT));
            double targetWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_TARGET_WEIGHT));
            double height = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_HEIGHT));
            String goal = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FITNESS_GOAL));
            String diet = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_DIET_TYPE));

            tvUserName.setText(name);
            tvCurrentWeight.setText(String.format(Locale.getDefault(), "%.1f kg", currentWeight));
            tvTargetWeight.setText(String.format(Locale.getDefault(), "%.1f kg", targetWeight));
            
            if (goal != null && !goal.isEmpty()) {
                tvGoal.setText(String.format("Goal: %s", goal));
            } else {
                tvGoal.setText("Goal: Not set");
            }

            if (diet != null && !diet.isEmpty()) {
                tvDietType.setText(String.format("Diet: %s", diet));
            } else {
                tvDietType.setText("Diet: Not set");
            }

            if (height > 0 && currentWeight > 0) {
                double heightM = height / 100;
                double bmi = currentWeight / (heightM * heightM);
                tvBmi.setText(String.format(Locale.getDefault(), "%.1f", bmi));
            } else {
                tvBmi.setText("--");
            }

            cursor.close();
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));

        cardDiet.setOnClickListener(v -> startActivity(new Intent(this, DietPlanActivity.class)));
        cardWorkout.setOnClickListener(v -> startActivity(new Intent(this, WorkoutPlanActivity.class)));
        cardProgress.setOnClickListener(v -> startActivity(new Intent(this, ProgressActivity.class)));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData(); // Refresh data when coming back from other activities
    }
}
