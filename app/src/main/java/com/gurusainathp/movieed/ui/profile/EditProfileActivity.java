package com.gurusainathp.movieed.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.gurusainathp.movieed.R;

public class EditProfileActivity extends AppCompatActivity {

    public static final String PREF_NAME = "user_profile_prefs";
    public static final String KEY_USER_NAME = "user_display_name";

    private TextInputEditText etDisplayName;

    public static void launch(Context context) {
        Intent intent = new Intent(context, EditProfileActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etDisplayName = findViewById(R.id.et_display_name);
        Button btnSave = findViewById(R.id.btn_save_profile);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String currentName = prefs.getString(KEY_USER_NAME, "Alex");
        if (etDisplayName != null) {
            etDisplayName.setText(currentName);
        }

        btnSave.setOnClickListener(v -> {
            String input = etDisplayName != null && etDisplayName.getText() != null ?
                    etDisplayName.getText().toString().trim() : "Alex";
            String pascalCaseName = toPascalCase(input);

            prefs.edit().putString(KEY_USER_NAME, pascalCaseName).apply();
            finish();
        });
    }

    public static String toPascalCase(String input) {
        if (input == null || input.trim().isEmpty()) return "Alex";
        String[] words = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
