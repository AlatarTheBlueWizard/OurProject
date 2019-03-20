package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

//        ImageView groupTestView = (ImageView)findViewById(R.id.groupTestView);
//        ImageView selfieTestView = (ImageView)findViewById(R.id.selfieTestView);
          ImageView selfieTestView = (ImageView)findViewById(R.id.resultView);

//        Bitmap group = bitmaps.get(0);
//        Bitmap selfie = bitmaps.get(1);
          Bitmap result = bitmapOverlayToCenter(bitmaps.get(0), bitmaps.get(1));
//
//        groupTestView.setImageBitmap(group);
//        selfieTestView.setImageBitmap(selfie);
          selfieTestView.setImageBitmap(result);
    }

    private Bitmap combineImageIntoOneFlexWidth(List<Bitmap> bitmap) {
        int w = 0, h = 0;
        for (int i = 0; i < bitmap.size(); i++) {
            if (i < bitmap.size() - 1) {
                h = bitmap.get(i).getHeight() > bitmap.get(i + 1).getHeight() ? bitmap.get(i).getHeight() : bitmap.get(i + 1).getHeight();
            }
            w += bitmap.get(i).getWidth();
        }

        Bitmap temp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(temp);
        int top = 0;
        for (int i = 0; i < bitmap.size(); i++) {
            Log.e("HTML", "Combine: " + i + "/" + bitmap.size() + 1);

            top = (i == 0 ? 0 : top + bitmap.get(i).getWidth());
            //attributes 1:bitmap,2:width that starts drawing,3:height that starts drawing
            canvas.drawBitmap(bitmap.get(i), top, 0f, null);
        }
        return temp;
    }

    public Bitmap bitmapOverlayToCenter(Bitmap bitmap1, Bitmap overlayBitmap) {
        int bitmap1Width = bitmap1.getWidth();
        int bitmap1Height = bitmap1.getHeight();
        int bitmap2Width = overlayBitmap.getWidth() / 3;
        int bitmap2Height = overlayBitmap.getHeight() / 3;

        float marginLeft = (float) (bitmap1Width * 0.5 - bitmap2Width * 0.5);
        float marginTop = (float) (bitmap1Height * 0.5 - bitmap2Height * 0.5);

        Bitmap finalBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, bitmap1.getConfig());
        Canvas canvas = new Canvas(finalBitmap);
        canvas.drawBitmap(bitmap1, new Matrix(), null);
        canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null);
        return finalBitmap;
    }
}
