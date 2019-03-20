package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PhotoTest extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_test);

        Gson gson = new Gson();
        Intent intent = getIntent();
        String bitmapsJson = intent.getStringExtra("BitmapArray");

        Type listType = new TypeToken<ArrayList<Bitmap>>(){}.getType();
        List<Bitmap> bitmaps = new Gson().fromJson(bitmapsJson, listType);

        ImageView groupTestView = (ImageView)findViewById(R.id.groupTestView);
        ImageView selfieTestView = (ImageView)findViewById(R.id.selfieTestView);

        Bitmap group = bitmaps.get(0);
        Bitmap selfie = bitmaps.get(1);

        groupTestView.setImageBitmap(group);
        selfieTestView.setImageBitmap(selfie);
    }
}
