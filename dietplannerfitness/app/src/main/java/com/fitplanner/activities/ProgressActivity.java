package com.fitplanner.activities;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fitplanner.R;
import com.fitplanner.database.DatabaseHelper;
import com.fitplanner.models.WeightLog;
import com.fitplanner.utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProgressActivity extends AppCompatActivity {

    private static final String TAG = "ProgressActivity";
    private TextView tvCurrentWeight, tvTargetWeight, tvWeightDiff, tvCompletionPercent;
    private TextView tvStartingWeight, tvWeightChangeValue, tvChangeLabel;
    private TextInputEditText etNewWeight;
    private MaterialButton btnSaveWeight, btnResetHistory;
    private RecyclerView rvHistory;
    private LineChart lineChart;
    private MaterialCardView cardChart;
    
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private WeightHistoryAdapter adapter;
    private List<WeightLog> weightLogs;
    private int userId = -1;
    private double targetWeight = 0;
    private double startingWeight = 0; // Fixed from profile
    private double currentWeight = 0;  // Latest log or profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        weightLogs = new ArrayList<>();

        initViews();
        loadUserData();
        loadWeightHistory();

        btnSaveWeight.setOnClickListener(v -> saveWeightEntry());
        btnResetHistory.setOnClickListener(v -> showResetConfirmation());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_progress);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tvCurrentWeight = findViewById(R.id.tv_current_weight);
        tvTargetWeight = findViewById(R.id.tv_target_weight);
        tvWeightDiff = findViewById(R.id.tv_weight_diff);
        tvCompletionPercent = findViewById(R.id.tv_completion_percent);
        tvStartingWeight = findViewById(R.id.tv_starting_weight);
        tvWeightChangeValue = findViewById(R.id.tv_weight_change_value);
        tvChangeLabel = findViewById(R.id.tv_change_label);
        
        etNewWeight = findViewById(R.id.et_new_weight);
        btnSaveWeight = findViewById(R.id.btn_save_weight);
        btnResetHistory = findViewById(R.id.btn_reset_history);
        lineChart = findViewById(R.id.line_chart);
        cardChart = findViewById(R.id.card_chart);
        
        rvHistory = findViewById(R.id.rv_weight_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeightHistoryAdapter(weightLogs);
        rvHistory.setAdapter(adapter);

        setupChart();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.setNoDataText("Add at least 2 weight entries to view progress graph.");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(0.1f);
        
        lineChart.getAxisRight().setEnabled(false);
    }

    private void loadUserData() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        Cursor cursor = dbHelper.getUserByEmail(email);
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
            targetWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_TARGET_WEIGHT));
            startingWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_STARTING_WEIGHT));
            
            // Default current weight to profile value if no logs exist
            currentWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CURRENT_WEIGHT));

            tvTargetWeight.setText(String.format(Locale.getDefault(), "%.1f kg", targetWeight));
            tvStartingWeight.setText(String.format(Locale.getDefault(), "%.1f kg", startingWeight));
            
            cursor.close();
        }
    }

    private void loadWeightHistory() {
        if (userId == -1) return;
        
        weightLogs.clear();
        Cursor cursor = dbHelper.getWeightHistory(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ID));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_WEIGHT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_DATE));
                weightLogs.add(new WeightLog(id, weight, date));
            }
            cursor.close();
            
            if (!weightLogs.isEmpty()) {
                currentWeight = weightLogs.get(0).getWeight(); // Latest entry from logs
            }
            
            Log.d(TAG, "Starting Weight (Profile): " + startingWeight);
            Log.d(TAG, "Current Weight (Latest): " + currentWeight);
            Log.d(TAG, "Target Weight (Profile): " + targetWeight);
            Log.d(TAG, "WeightLogs loaded: " + weightLogs.size());
            
            tvCurrentWeight.setText(String.format(Locale.getDefault(), "%.1f kg", currentWeight));
            
            calculatePercentage();
            
            if (!weightLogs.isEmpty()) {
                cardChart.setVisibility(View.VISIBLE);
                if (weightLogs.size() < 2) {
                    lineChart.clear();
                    lineChart.setNoDataText("Add at least 2 weight entries to view progress graph.");
                } else {
                    updateChart();
                }
            } else {
                cardChart.setVisibility(View.GONE);
                Log.d(TAG, "No weight logs found for user: " + userId);
            }
            
            adapter.notifyDataSetChanged();
        }
    }

    private void updateChart() {
        if (weightLogs.size() < 2) return;

        List<Entry> entries = new ArrayList<>();
        final List<String> dates = new ArrayList<>();
        
        // Oldest to newest for plotting
        List<WeightLog> chartData = new ArrayList<>(weightLogs);
        Collections.reverse(chartData);

        float minWeight = (float) targetWeight;
        float maxWeight = (float) targetWeight;

        for (int i = 0; i < chartData.size(); i++) {
            WeightLog log = chartData.get(i);
            float weight = (float) log.getWeight();
            entries.add(new Entry(i, weight));
            dates.add(log.getDate());
            
            if (weight < minWeight) minWeight = weight;
            if (weight > maxWeight) maxWeight = weight;
        }

        // Configure X-Axis with dates
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dates.size()) {
                    try {
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                        Date date = originalFormat.parse(dates.get(index));
                        return date != null ? displayFormat.format(date) : dates.get(index);
                    } catch (ParseException e) {
                        return dates.get(index);
                    }
                }
                return "";
            }
        });

        // Resolve Primary Color
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        LineDataSet dataSet = new LineDataSet(entries, "Weight History");
        dataSet.setColor(colorPrimary);
        dataSet.setCircleColor(colorPrimary);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(colorPrimary);
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        // Highlight latest point
        dataSet.setHighLightColor(Color.RED);
        dataSet.setHighlightEnabled(true);

        // Add Target Weight Limit Line
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        
        LimitLine llTarget = new LimitLine((float) targetWeight, "Target");
        llTarget.setLineColor(Color.RED);
        llTarget.setLineWidth(2f);
        llTarget.enableDashedLine(10f, 10f, 0f);
        llTarget.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        llTarget.setTextSize(10f);
        leftAxis.addLimitLine(llTarget);

        // Adjust Axis Range
        leftAxis.setAxisMinimum(minWeight - 3f);
        leftAxis.setAxisMaximum(maxWeight + 3f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    private void calculatePercentage() {
        if (userId == -1) return;
        
        double change = currentWeight - startingWeight;
        double diff = Math.abs(currentWeight - targetWeight);
        
        // Display difference to target
        tvWeightDiff.setText(String.format(Locale.getDefault(), "%.1f kg to go", diff));

        // Display change from start
        if (change <= 0) {
            tvChangeLabel.setText(R.string.weight_lost);
            tvWeightChangeValue.setText(String.format(Locale.getDefault(), "%.1f kg", Math.abs(change)));
        } else {
            tvChangeLabel.setText(R.string.weight_gained);
            tvWeightChangeValue.setText(String.format(Locale.getDefault(), "%.1f kg", change));
        }

        double percent = 0;
        double totalChangeRequired = startingWeight - targetWeight;
        
        if (totalChangeRequired > 0) { // Weight Loss Goal
            percent = ((startingWeight - currentWeight) / totalChangeRequired) * 100;
        } else if (totalChangeRequired < 0) { // Weight Gain Goal
            percent = ((currentWeight - startingWeight) / Math.abs(totalChangeRequired)) * 100;
        } else {
            percent = 100;
        }

        // Clamp between 0 and 100
        percent = Math.max(0, Math.min(100, percent));

        tvCompletionPercent.setText(String.format(Locale.getDefault(), "%.0f%%", percent));
    }

    private void saveWeightEntry() {
        if (etNewWeight.getText() == null) return;
        
        String weightStr = etNewWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            etNewWeight.setError("Enter weight");
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            long result = dbHelper.insertWeightLog(userId, weight, currentDate);
            if (result != -1) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COL_USER_CURRENT_WEIGHT, weight);
                dbHelper.updateUserProfile(userId, values);
                
                Toast.makeText(this, R.string.weight_entry_success, Toast.LENGTH_SHORT).show();
                etNewWeight.setText("");
                loadUserData();
                loadWeightHistory();
            }
        } catch (NumberFormatException e) {
            etNewWeight.setError("Invalid weight format");
        }
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_reset_title)
                .setMessage(R.string.confirm_reset_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> resetWeightHistory())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void resetWeightHistory() {
        if (userId == -1) return;

        dbHelper.deleteAllWeightLogs(userId);
        Toast.makeText(this, R.string.history_cleared_success, Toast.LENGTH_SHORT).show();
        
        loadWeightHistory();
        // Also update summary since last weight is gone
        tvCompletionPercent.setText("0%");
        tvWeightDiff.setText("-- kg to go");
    }

    private class WeightHistoryAdapter extends RecyclerView.Adapter<WeightHistoryAdapter.ViewHolder> {
        private List<WeightLog> logs;

        public WeightHistoryAdapter(List<WeightLog> logs) {
            this.logs = logs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WeightLog log = logs.get(position);
            holder.text1.setText(String.format(Locale.getDefault(), "%.1f kg", log.getWeight()));
            holder.text2.setText(log.getDate());
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
