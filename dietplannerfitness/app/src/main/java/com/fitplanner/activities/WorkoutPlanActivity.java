package com.fitplanner.activities;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.fitplanner.BuildConfig;
import com.fitplanner.R;
import com.fitplanner.api.GroqRequest;
import com.fitplanner.api.GroqResponse;
import com.fitplanner.api.Message;
import com.fitplanner.api.RetrofitClient;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutPlanActivity extends AppCompatActivity {

    private TextView tvMon, tvTue, tvWed, tvThu, tvFri, tvSat, tvSun;
    private MaterialButton btnGenerate, btnRefresh;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    
    private String fitnessGoal = "Maintain Fitness";
    private String workoutLocation = "Home Workout"; // Default
    private String experienceLevel = "Beginner"; // Default

    private static final String PREFS_NAME = "WorkoutCache";
    private static final String KEY_CACHED_WORKOUT = "cached_workout_plan";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_plan);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadUserPreferences();
        
        if (!loadFromCache()) {
            setSampleWorkoutData();
        }

        btnGenerate.setOnClickListener(v -> generateAIWorkoutPlan());
        btnRefresh.setOnClickListener(v -> generateAIWorkoutPlan());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_workout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tvMon = findViewById(R.id.tv_mon_workout);
        tvTue = findViewById(R.id.tv_tue_workout);
        tvWed = findViewById(R.id.tv_wed_workout);
        tvThu = findViewById(R.id.tv_thu_workout);
        tvFri = findViewById(R.id.tv_fri_workout);
        tvSat = findViewById(R.id.tv_sat_workout);
        tvSun = findViewById(R.id.tv_sun_workout);

        btnGenerate = findViewById(R.id.btn_generate_workout);
        btnRefresh = findViewById(R.id.btn_refresh_workout);
        progressBar = findViewById(R.id.progress_bar_workout);
    }

    private void loadUserPreferences() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            fitnessGoal = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FITNESS_GOAL));
            // In a real app, these would be in the user profile table/UI
            // For now using fitnessGoal and defaults
            cursor.close();
        }
    }

    private void generateAIWorkoutPlan() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gather User Details
        int age = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_AGE));
        String gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_GENDER));
        double height = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_HEIGHT));
        double currentWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CURRENT_WEIGHT));
        double targetWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_TARGET_WEIGHT));
        String activityLevel = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ACTIVITY_LEVEL));
        String goal = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FITNESS_GOAL));

        double heightM = height / 100;
        double bmi = (heightM > 0) ? currentWeight / (heightM * heightM) : 0;
        cursor.close();

        // Construct Prompt
        String prompt = "You are a certified fitness trainer.\n\n" +
                "Generate a personalized 7-day workout plan.\n\n" +
                "User Details:\n" +
                "Age: " + age + "\n" +
                "Gender: " + gender + "\n" +
                "Height: " + height + " cm\n" +
                "Current Weight: " + currentWeight + " kg\n" +
                "Target Weight: " + targetWeight + " kg\n" +
                "BMI: " + String.format(Locale.getDefault(), "%.1f", bmi) + "\n" +
                "Activity Level: " + activityLevel + "\n" +
                "Fitness Goal: " + goal + "\n" +
                "Workout Location: " + workoutLocation + "\n" +
                "Experience Level: " + experienceLevel + "\n\n" +
                "Include:\n" +
                "[MONDAY]\n" +
                "[TUESDAY]\n" +
                "[WEDNESDAY]\n" +
                "[THURSDAY]\n" +
                "[FRIDAY]\n" +
                "[SATURDAY]\n" +
                "[SUNDAY]\n\n" +
                "For each day provide: Exercises, Sets, Repetitions, Duration, Warm-up and Rest recommendations.\n" +
                "IMPORTANT: Format your response clearly with headers [MONDAY], [TUESDAY], etc.";

        showLoading(true);

        // Prepare Request
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));
        GroqRequest request = new GroqRequest(RetrofitClient.GROQ_MODEL, messages, 0.7);

        // API Call
        RetrofitClient.getGroqApiService().getChatCompletion("Bearer " + BuildConfig.GROQ_API_KEY, request)
                .enqueue(new Callback<GroqResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GroqResponse> call, @NonNull Response<GroqResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            String aiContent = response.body().getChoices().get(0).getMessage().getContent();
                            parseAndDisplayResponse(aiContent);
                            saveToCache(aiContent);
                        } else {
                            Toast.makeText(WorkoutPlanActivity.this, "API Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<GroqResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        Toast.makeText(WorkoutPlanActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void parseAndDisplayResponse(String content) {
        String[] sections = content.split("\\[");
        for (String section : sections) {
            if (section.startsWith("MONDAY]")) {
                tvMon.setText(section.replace("MONDAY]", "").trim());
            } else if (section.startsWith("TUESDAY]")) {
                tvTue.setText(section.replace("TUESDAY]", "").trim());
            } else if (section.startsWith("WEDNESDAY]")) {
                tvWed.setText(section.replace("WEDNESDAY]", "").trim());
            } else if (section.startsWith("THURSDAY]")) {
                tvThu.setText(section.replace("THURSDAY]", "").trim());
            } else if (section.startsWith("FRIDAY]")) {
                tvFri.setText(section.replace("FRIDAY]", "").trim());
            } else if (section.startsWith("SATURDAY]")) {
                tvSat.setText(section.replace("SATURDAY]", "").trim());
            } else if (section.startsWith("SUNDAY]")) {
                tvSun.setText(section.replace("SUNDAY]", "").trim());
            }
        }
        
        // Fallback if formatting was ignored
        if (tvMon.getText().toString().equals("Loading...")) {
            tvMon.setText(content);
            tvTue.setText("Detailed above");
            tvWed.setText("Detailed above");
            tvThu.setText("Detailed above");
            tvFri.setText("Detailed above");
            tvSat.setText("Detailed above");
            tvSun.setText("Detailed above");
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGenerate.setEnabled(!isLoading);
        btnRefresh.setEnabled(!isLoading);
    }

    private void saveToCache(String content) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_CACHED_WORKOUT, content);
        editor.apply();
    }

    private boolean loadFromCache() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedContent = prefs.getString(KEY_CACHED_WORKOUT, null);
        if (cachedContent != null) {
            parseAndDisplayResponse(cachedContent);
            return true;
        }
        return false;
    }

    private void setSampleWorkoutData() {
        if (fitnessGoal.equalsIgnoreCase("Weight Loss")) {
            tvMon.setText("Cardio: 30 min Running, 15 min Burpees");
            tvTue.setText("HIIT: 20 min Circuit, 10 min Jumping Jacks");
            tvWed.setText("Active Rest: 45 min Brisk Walking");
            tvThu.setText("Cardio: 30 min Cycling, 15 min Mountain Climbers");
            tvFri.setText("Full Body: Light Weights, High Reps (3 sets of 15)");
            tvSat.setText("Yoga/Stretching: 40 min session");
            tvSun.setText("Rest Day");
        } else if (fitnessGoal.equalsIgnoreCase("Muscle Gain") || fitnessGoal.equalsIgnoreCase("Weight Gain")) {
            tvMon.setText("Chest & Triceps: Bench Press, Dips (4 sets of 8-12)");
            tvTue.setText("Back & Biceps: Pull-ups, Rows, Curls (4 sets of 8-12)");
            tvWed.setText("Rest Day");
            tvThu.setText("Legs: Squats, Deadlifts, Lunges (4 sets of 10)");
            tvFri.setText("Shoulders & Abs: Overhead Press, Planks (4 sets of 12)");
            tvSat.setText("Full Body Compound Movements (3 sets of 8)");
            tvSun.setText("Rest Day");
        } else { // Maintain Fitness
            tvMon.setText("Full Body: Moderate Weights (3 sets of 12)");
            tvTue.setText("Cardio: 20 min Jogging");
            tvWed.setText("Pilates or Bodyweight Training");
            tvThu.setText("Upper Body: Pushups, Rows (3 sets of 12)");
            tvFri.setText("Lower Body: Squats, Glute Bridges (3 sets of 12)");
            tvSat.setText("Outdoor Activity: Hiking or Swimming");
            tvSun.setText("Rest Day");
        }
    }
}
