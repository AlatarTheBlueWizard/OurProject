package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PhotoTest extends AppCompatActivity {
    private static final String TAG = "MergeActivity";
    private int selfieResSize = 1;
    private static final int SELFIE_SIZE_THRESHOLD = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_test);

        Gson gson = new Gson();
        Intent intent = getIntent();
        String bitmapsJson = intent.getStringExtra("BitmapArray");
        String groupFileName = intent.getStringExtra("GroupFileName");
        String selfieFileName = intent.getStringExtra("SelfieFileName");

        Type listType = new TypeToken<ArrayList<Bitmap>>(){}.getType();
        List<Bitmap> bitmaps = new Gson().fromJson(bitmapsJson, listType);

        ImageView groupTestView = (ImageView)findViewById(R.id.groupTestView);
        ImageView selfieTestView = (ImageView)findViewById(R.id.selfieTestView);

        // Group photo is first because it was taken first
        Bitmap group  = bitmaps.get(0);
        Bitmap selfie = bitmaps.get(1);

        // Some math here to preserve aspect ratio
        // Just comments for example.
        // A preserved aspect ratio image is such that given
        //      oldWidth / oldHeight == newWidth / newHeight
        // Some Algebra gives us an equivalent equation
        //      oldWidth / newWidth  == oldHeight / newHeight
        // And if that's the truth than given a scale factor (lets say s) the ratios will be
        // preserved: oldWidth / 2 == oldHeight / 2 in ratio. We are downsizing the image to half

        Log.d(TAG, "Selfie width: " + selfie.getWidth());
        Log.d(TAG, "Selfie height: " + selfie.getHeight());
        Log.d(TAG, "Group Path: " + groupFileName);
        Log.d(TAG, "Selfie Path: " + selfieFileName);

        // Use picasso to scale down and maintain aspect ratio
        Picasso.with(this)
                .load(new File(selfieFileName))
                .resizeDimen(R.dimen.size4, R.dimen.size4)
                .onlyScaleDown()
                .into(selfieTestView);


        // Try resizing the selfie
        // Give filter true, do bilinear filtering to make image quality as retained as possible
        // Give the dstWidth and dstHeight the scaling math
        Bitmap resizedSelfie = Bitmap.createScaledBitmap(selfie,
                selfie.getWidth() / 4,
                selfie.getHeight() / 2,
                true);
        //groupTestView.setImageBitmap(group);
        //selfieTestView.setImageBitmap(resizedSelfie);
    }

    /**
     * Scales the selfie image up to next size dimension found in R.dimen. If limit is reached,
     * button is set to invisible.
     * @param view
     */
    public void scaleUp(View view) {
        // If the size is still less than the max selfie size, increment up
        if (selfieResSize < SELFIE_SIZE_THRESHOLD) {
            selfieResSize++;
        }
        // Else
    }
}
