package com.heroes3.livewallpaper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.EXTRA_LOCAL_ONLY;

public class SettingsActivity extends AppCompatActivity {

    private static final int PICK_FILE_RESULT_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings, new SettingsFragment())
            .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = getSharedPreferences("wallpaper_preferences", Context.MODE_PRIVATE);

        String updateFrequency = prefs.getString("update_frequency", null);
        System.out.println("FREQ: " + updateFrequency);
    }

    protected void handleLodFileSelect(Intent data) {
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }

        String uriPath = uri.getPath();
        if (uriPath == null) {
            return;
        }

        boolean isCorrectFile = uriPath.toLowerCase().endsWith("h3sprite.lod");
        if (!isCorrectFile) {
            Toast
                .makeText(
                    this,
                    "Incorrect file selected",
                    Toast.LENGTH_LONG
                )
                .show();
            return;
        }

        String filePath = uriPath.substring(uriPath.indexOf(":") + 1);
        System.out.println("SELECTED FILE: " + filePath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        System.out.println("ON ACTIVITY RESULT: " + requestCode + " : " + resultCode);
        if (/*requestCode == PICK_FILE_RESULT_CODE && */resultCode == RESULT_OK) {
            handleLodFileSelect(data);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference selectLodItem = findPreference("select_lod");
            if (selectLodItem != null) {
                selectLodItem.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                            .setType("application/octet-stream")
                            .addCategory(CATEGORY_OPENABLE)
                            .putExtra(EXTRA_LOCAL_ONLY, true);

                        startActivityForResult(
                            Intent.createChooser(intent, "Select file"),
                            PICK_FILE_RESULT_CODE
                        );
                        return true;
                    }
                });
            }
        }
    }
}