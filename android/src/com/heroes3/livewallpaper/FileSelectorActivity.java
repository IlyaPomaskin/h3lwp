package com.heroes3.livewallpaper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.EXTRA_LOCAL_ONLY;

public class FileSelectorActivity extends AppCompatActivity {
    private static final int PICK_FILE_RESULT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
            .setType("application/octet-stream")
            .addCategory(CATEGORY_OPENABLE)
            .putExtra(EXTRA_LOCAL_ONLY, true);

        startActivityForResult(
            Intent.createChooser(intent, "Select H3sprite.lod"),
            PICK_FILE_RESULT_CODE
        );
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
        sendFilePath(filePath);
    }

    protected void sendFilePath(String filePath) {
        Intent intent = new Intent(LiveWallpaper.INTENT_NAME);
        intent.putExtra(LiveWallpaper.INTENT_EXTRA_NAME, filePath);
        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_RESULT_CODE && resultCode == RESULT_OK) {
            handleLodFileSelect(data);
            finish();
        }
    }
}
