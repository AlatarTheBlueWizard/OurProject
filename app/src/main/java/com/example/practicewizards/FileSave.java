package com.example.practicewizards;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.net.URI;

import static android.content.ContentValues.TAG;

public class FileSave {
    private URI imageUri;

    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
    File photo;

    try {
        //place to store the picture that was taken
        photo = this.createTemporaryFile("picture", ".jpg");
        photo.delete();
    } catch(Exception e) {
        Log.v(TAG, "Can't create file to take the picture");
        Toast.makeText(activity, "Please check SD card! Image shot is impossible!", 10000);
    }

    imageUri = Uri.fromFile(photo);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

    //start camera intent
    activity.startActivityForResult(this, intent, MenuShootImage);

    private File createTemporaryFile(String part, String ext) throws Exception {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath()+"/.temp/");
        if(!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return File.createTempFile(part, ext, tempDir);
    }
}
