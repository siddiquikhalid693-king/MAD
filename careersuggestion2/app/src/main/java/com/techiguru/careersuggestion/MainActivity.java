package com.techiguru.careersuggestion;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button bt_suggestion;
    TextView tv_result;
    EditText et_Interest;

    String groq_key = ""; // API Key removed for security


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bt_suggestion=findViewById(R.id.bt_suggest);
        tv_result=findViewById(R.id.tv_result);
        et_Interest=findViewById(R.id.et_interest);

        bt_suggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suggestCareer();
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void suggestCareer() {
        String interest = et_Interest.getText().toString();

        try {

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();

            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content",
                    "Suggest 3 careers for " + interest);

            messages.put(msg);
            body.put("messages", messages);

            JsonObjectRequest request =
                    new JsonObjectRequest(
                            Request.Method.POST,
                            "https://api.groq.com/openai/v1/chat/completions",
                            body,

                            response -> {
                                try {
                                    String result =
                                            response.getJSONArray("choices")
                                                    .getJSONObject(0)
                                                    .getJSONObject("message")
                                                    .getString("content");

                                    tv_result.setText(result);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            },

                            error -> tv_result.setText("Error")
                    ) {

                        @Override
                        public Map<String, String> getHeaders()
                                throws AuthFailureError {

                            Map<String, String> headers =
                                    new HashMap<>();

                            headers.put("Authorization",
                                    "Bearer " + groq_key);

                            headers.put("Content-Type",
                                    "application/json");

                            return headers;
                        }
                    };

            RequestQueue queue =
                    Volley.newRequestQueue(this);

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}