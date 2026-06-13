package com.fitplanner.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.fitplanner.BuildConfig;
import com.fitplanner.R;
import com.fitplanner.api.RetrofitClient;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvAiModel, tvApiStatus;
    private MaterialButton btnEditProfile, btnClearWeight, btnClearDiet, btnClearWorkout, btnLogout;
    
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tvUserName = findViewById(R.id.tv_user_name_settings);
        tvUserEmail = findViewById(R.id.tv_user_email_settings);
        tvAiModel = findViewById(R.id.tv_ai_model);
        tvApiStatus = findViewById(R.id.tv_api_status);

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnClearWeight = findViewById(R.id.btn_clear_weight_history);
        btnClearDiet = findViewById(R.id.btn_clear_diet_cache);
        btnClearWorkout = findViewById(R.id.btn_clear_workout_cache);
        btnLogout = findViewById(R.id.btn_logout);

        tvAiModel.setText(RetrofitClient.GROQ_MODEL);
        
        // Check API status
        if (BuildConfig.GROQ_API_KEY != null && !BuildConfig.GROQ_API_KEY.isEmpty() 
            && !BuildConfig.GROQ_API_KEY.equals("your_api_key_here")) {
            tvApiStatus.setText(R.string.api_connected);
            tvApiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        } else {
            tvApiStatus.setText(R.string.api_disconnected);
            tvApiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
        }
    }

    private void loadUserData() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        tvUserEmail.setText(email);
        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME));
            tvUserName.setText(name);
            cursor.close();
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UserProfileActivity.class));
        });

        btnClearWeight.setOnClickListener(v -> showConfirmationDialog(
                getString(R.string.confirm_reset_title),
                getString(R.string.confirm_reset_message),
                () -> {
                    dbHelper.deleteAllWeightLogs(userId);
                    Toast.makeText(this, R.string.history_cleared_success, Toast.LENGTH_SHORT).show();
                }
        ));

        btnClearDiet.setOnClickListener(v -> showConfirmationDialog(
                getString(R.string.clear_diet_cache),
                getString(R.string.confirm_clear_diet_message),
                () -> {
                    getSharedPreferences("DietCache", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, R.string.diet_cache_cleared, Toast.LENGTH_SHORT).show();
                }
        ));

        btnClearWorkout.setOnClickListener(v -> showConfirmationDialog(
                getString(R.string.clear_workout_cache),
                getString(R.string.confirm_clear_workout_message),
                () -> {
                    getSharedPreferences("WorkoutCache", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, R.string.workout_cache_cleared, Toast.LENGTH_SHORT).show();
                }
        ));

        btnLogout.setOnClickListener(v -> showConfirmationDialog(
                getString(R.string.confirm_logout_title),
                getString(R.string.confirm_logout_message),
                () -> {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
        ));
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
