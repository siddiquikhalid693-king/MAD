package com.techiguru.jsonparsing;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ListView lv_data ;
    ArrayList<String> nameList ;
    ArrayAdapter<String> ad ;
    String apiUrl = "https://jsonplaceholder.typicode.com/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        lv_data = findViewById(R.id.lv_data);
        nameList = new ArrayList<>();
        ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,nameList);
        lv_data.setAdapter(ad);
        fetchdata();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchdata() {

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                apiUrl,
                null,

                response -> {

                    try {

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            
                            String id = obj.getString("id");
                            String name = obj.getString("name");
                            String email = obj.getString("email");

                            // Get the nested address object and extract city
                            JSONObject address = obj.getJSONObject("address");
                            String city = address.getString("city");

                            nameList.add(
                                    "ID : " + id +
                                            "\nName : " + name +
                                            "\nEmail : " + email +
                                            "\nCity : " + city
                            );
                        }

                        ad.notifyDataSetChanged();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                },

                error -> Toast.makeText(
                        MainActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT
                ).show()
        );

        queue.add(request);
    }
}