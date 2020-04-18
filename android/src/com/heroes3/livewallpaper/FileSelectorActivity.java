package com.heroes3.livewallpaper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.EXTRA_LOCAL_ONLY;

public class FileSelectorActivity extends Activity {
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

    protected String getFilePath(Intent data) {
        Uri uri = data.getData();
        if (uri == null) {
            return "";
        }

        String uriPath = uri.getPath();
        if (uriPath == null) {
            return "";
        }

        return uriPath.substring(uriPath.indexOf(":") + 1);
    }

    protected void sendFilePath(String filePath) {
        Intent intent = new Intent(LiveWallpaper.INTENT_NAME);
        intent.putExtra(LiveWallpaper.INTENT_EXTRA_NAME, filePath);
        sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_RESULT_CODE && resultCode == RESULT_OK) {
            sendFilePath(getFilePath(data));
            finish();
        }
    }
}
