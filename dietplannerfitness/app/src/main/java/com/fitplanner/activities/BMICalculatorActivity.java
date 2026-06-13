package com.fitplanner.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fitplanner.R;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class BMICalculatorActivity extends AppCompatActivity {

    private TextInputLayout tilHeight, tilWeight;
    private TextInputEditText etHeight, etWeight;
    private MaterialButton btnCalculate, btnHome;
    private MaterialCardView cardResult;
    private TextView tvBmiValue, tvBmiCategory, tvHealthyWeight, tvWaterIntake;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi_calculator);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        preFillData();

        btnCalculate.setOnClickListener(v -> calculateBMI());
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void initViews() {
        tilHeight = findViewById(R.id.til_height);
        tilWeight = findViewById(R.id.til_weight);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        btnCalculate = findViewById(R.id.btn_calculate);
        btnHome = findViewById(R.id.btn_home);
        cardResult = findViewById(R.id.card_result);
        tvBmiValue = findViewById(R.id.tv_bmi_value);
        tvBmiCategory = findViewById(R.id.tv_bmi_category);
        tvHealthyWeight = findViewById(R.id.tv_healthy_weight);
        tvWaterIntake = findViewById(R.id.tv_water_intake);
    }

    private void preFillData() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            double height = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_HEIGHT));
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CURRENT_WEIGHT));
            
            if (height > 0) etHeight.setText(String.valueOf(height));
            if (weight > 0) etWeight.setText(String.valueOf(weight));
            
            cursor.close();
            
            if (height > 0 && weight > 0) {
                calculateBMI();
            }
        }
    }

    private void calculateBMI() {
        // Reset errors
        tilHeight.setError(null);
        tilWeight.setError(null);

        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (heightStr.isEmpty()) {
            tilHeight.setError("Required");
            return;
        }
        if (weightStr.isEmpty()) {
            tilWeight.setError("Required");
            return;
        }

        try {
            double heightCm = Double.parseDouble(heightStr);
            double weightKg = Double.parseDouble(weightStr);

            if (heightCm < 50 || heightCm > 250) {
                tilHeight.setError("Invalid height (50-250 cm)");
                return;
            }
            if (weightKg < 20 || weightKg > 300) {
                tilWeight.setError("Invalid weight (20-300 kg)");
                return;
            }

            double heightM = heightCm / 100;
            double bmi = weightKg / (heightM * heightM);

            displayResults(bmi, heightM, weightKg);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayResults(double bmi, double heightM, double weightKg) {
        cardResult.setVisibility(View.VISIBLE);
        btnHome.setVisibility(View.VISIBLE);

        tvBmiValue.setText(String.format(Locale.getDefault(), "%.1f", bmi));

        String category;
        if (bmi < 18.5) category = getString(R.string.bmi_underweight);
        else if (bmi < 25) category = getString(R.string.bmi_normal);
        else if (bmi < 30) category = getString(R.string.bmi_overweight);
        else category = getString(R.string.bmi_obese);

        tvBmiCategory.setText(category);

        double minHealthyWeight = 18.5 * (heightM * heightM);
        double maxHealthyWeight = 24.9 * (heightM * heightM);
        tvHealthyWeight.setText(String.format(Locale.getDefault(), "%.1fkg - %.1fkg", minHealthyWeight, maxHealthyWeight));

        // Suggested water intake: 33ml per kg of body weight
        double waterLiters = weightKg * 0.033;
        tvWaterIntake.setText(String.format(Locale.getDefault(), "Daily Water Intake: %.1fL", waterLiters));
    }
}
