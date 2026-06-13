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
import com.fitplanner.api.GroqApiService;
import com.fitplanner.api.GroqRequest;
import com.fitplanner.api.GroqResponse;
import com.fitplanner.api.Message;
import com.fitplanner.api.RetrofitClient;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DietPlanActivity extends AppCompatActivity {

    private TextView tvBreakfast, tvMidMorning, tvLunch, tvEvening, tvDinner, tvWater;
    private MaterialButton btnGenerate, btnRefresh;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private String dietType = "Vegetarian";
    
    private static final String PREFS_NAME = "DietCache";
    private static final String KEY_CACHED_PLAN = "cached_diet_plan";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_plan);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadUserDietType();
        
        if (!loadFromCache()) {
            setSampleData();
        }

        btnGenerate.setOnClickListener(v -> generateAIDietPlan());
        btnRefresh.setOnClickListener(v -> generateAIDietPlan());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tvBreakfast = findViewById(R.id.tv_breakfast_desc);
        tvMidMorning = findViewById(R.id.tv_mid_morning_desc);
        tvLunch = findViewById(R.id.tv_lunch_desc);
        tvEvening = findViewById(R.id.tv_evening_desc);
        tvDinner = findViewById(R.id.tv_dinner_desc);
        tvWater = findViewById(R.id.tv_water_desc);

        btnGenerate = findViewById(R.id.btn_generate_ai);
        btnRefresh = findViewById(R.id.btn_refresh);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadUserDietType() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            String savedDietType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_DIET_TYPE));
            if (savedDietType != null && !savedDietType.isEmpty()) {
                dietType = savedDietType;
            }
            cursor.close();
        }
    }

    private void generateAIDietPlan() {
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
        String fitnessGoal = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FITNESS_GOAL));
        String dietPreference = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_DIET_TYPE));
        cursor.close();

        // Construct Prompt
        String prompt = "You are a professional fitness nutritionist.\n\n" +
                "Generate a personalized 24-hour meal plan.\n\n" +
                "User Details:\n" +
                "Age: " + age + "\n" +
                "Gender: " + gender + "\n" +
                "Height: " + height + " cm\n" +
                "Current Weight: " + currentWeight + " kg\n" +
                "Target Weight: " + targetWeight + " kg\n" +
                "Activity Level: " + activityLevel + "\n" +
                "Fitness Goal: " + fitnessGoal + "\n" +
                "Diet Preference: " + dietPreference + "\n\n" +
                "Include:\n" +
                "- Breakfast\n" +
                "- Mid-Morning Snack\n" +
                "- Lunch\n" +
                "- Evening Snack\n" +
                "- Dinner\n" +
                "- Daily Water Intake\n\n" +
                "Recommendations should be healthy, practical and affordable.\n" +
                "IMPORTANT: Format your response clearly with headers [BREAKFAST], [MID-MORNING], [LUNCH], [EVENING], [DINNER], [WATER].";

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
                            Toast.makeText(DietPlanActivity.this, "API Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<GroqResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        Toast.makeText(DietPlanActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void parseAndDisplayResponse(String content) {
        String[] sections = content.split("\\[");
        for (String section : sections) {
            if (section.startsWith("BREAKFAST]")) {
                tvBreakfast.setText(section.replace("BREAKFAST]", "").trim());
            } else if (section.startsWith("MID-MORNING]")) {
                tvMidMorning.setText(section.replace("MID-MORNING]", "").trim());
            } else if (section.startsWith("LUNCH]")) {
                tvLunch.setText(section.replace("LUNCH]", "").trim());
            } else if (section.startsWith("EVENING]")) {
                tvEvening.setText(section.replace("EVENING]", "").trim());
            } else if (section.startsWith("DINNER]")) {
                tvDinner.setText(section.replace("DINNER]", "").trim());
            } else if (section.startsWith("WATER]")) {
                tvWater.setText(section.replace("WATER]", "").trim());
            }
        }
        
        // Fallback if formatting was ignored by AI
        if (tvBreakfast.getText().equals("Loading...")) {
             tvBreakfast.setText(content);
             tvMidMorning.setText("Detailed above");
             tvLunch.setText("Detailed above");
             tvEvening.setText("Detailed above");
             tvDinner.setText("Detailed above");
             tvWater.setText("Detailed above");
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGenerate.setEnabled(!isLoading);
        btnRefresh.setEnabled(!isLoading);
    }

    private void saveToCache(String content) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_CACHED_PLAN, content);
        editor.apply();
    }

    private boolean loadFromCache() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedContent = prefs.getString(KEY_CACHED_PLAN, null);
        if (cachedContent != null) {
            parseAndDisplayResponse(cachedContent);
            return true;
        }
        return false;
    }

    private void setSampleData() {
        if (dietType.equalsIgnoreCase("Non Vegetarian")) {
            tvBreakfast.setText("Oatmeal with milk, nuts, and 2 boiled eggs.");
            tvMidMorning.setText("A bowl of Greek yogurt with berries.");
            tvLunch.setText("Grilled chicken breast with brown rice and steamed broccoli.");
            tvEvening.setText("Apple slices with peanut butter.");
            tvDinner.setText("Baked salmon with quinoa and a side salad.");
            tvWater.setText("3.5 Liters (approx. 14 glasses)");
        } else if (dietType.equalsIgnoreCase("Vegan")) {
            tvBreakfast.setText("Chia seed pudding with almond milk and sliced banana.");
            tvMidMorning.setText("A handful of mixed nuts (almonds, walnuts).");
            tvLunch.setText("Tofu stir-fry with mixed vegetables and brown rice.");
            tvEvening.setText("Hummus with carrot sticks.");
            tvDinner.setText("Lentil soup with whole grain bread.");
            tvWater.setText("3.0 Liters (approx. 12 glasses)");
        } else { // Vegetarian
            tvBreakfast.setText("Paneer sandwich with whole wheat bread and a glass of milk.");
            tvMidMorning.setText("Sprouted moong dal salad.");
            tvLunch.setText("Dal tadka, mixed veg sabzi, 2 rotis, and a bowl of curd.");
            tvEvening.setText("Roasted makhana or a cup of green tea.");
            tvDinner.setText("Vegetable khichdi with a side of roasted papad.");
            tvWater.setText("3.2 Liters (approx. 13 glasses)");
        }
    }
}
