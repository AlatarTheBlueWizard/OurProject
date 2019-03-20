package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
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

    //blend function using paint
    //May need to create new drawables for colors (errors)
    private Bitmap getARGBImage() {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap red = BitmapFactory.decodeResource(getResources(), R.drawable.red, opt);
        Bitmap green = BitmapFactory.decodeResource(getResources(), R.drawable.green, opt);
        Bitmap blue = BitmapFactory.decodeResource(getResources(), R.drawable.blue, opt);
        Bitmap alphaGray = BitmapFactory.decodeResource(getResources(), R.drawable.alpha, opt);

        int width = red.getWidth();
        int height = red.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.eraseColor(Color.BLACK);

        Paint redP = new Paint();
        redP.setShader(new BitmapShader(red, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        redP.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY));
        redP.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        Paint greenP = new Paint();
        greenP.setShader(new BitmapShader(green, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        greenP.setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY));
        greenP.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        Paint blueP = new Paint();
        blueP.setShader(new BitmapShader(blue, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        blueP.setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY));
        blueP.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        Canvas c = new Canvas(result);
        c.drawRect(0, 0, width, height, redP);
        c.drawRect(0, 0, width, height, greenP);
        c.drawRect(0, 0, width, height, blueP);

        Bitmap alpha = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] alphaPix = new int[width * height];
        alphaGray.getPixels(alphaPix, 0, width, 0, 0, width, height);

        int count = width * height;
        for (int i = 0; i < count; ++i) {
            alphaPix[i] = alphaPix[i] << 8;
        }
        alpha.setPixels(alphaPix, 0, width, 0, 0, width, height);

        Paint alphaP = new Paint();
        alphaP.setAntiAlias(true);
        alphaP.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        c.drawBitmap(alpha, 0, 0, alphaP);

        red.recycle();
        green.recycle();
        blue.recycle();
        alphaGray.recycle();
        alpha.recycle();

        return result;
    }
}
