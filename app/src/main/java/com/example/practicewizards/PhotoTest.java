package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
