package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DrawableUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
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
    // Keep track of selfie file name
    private String selfieFileName;
    private ImageView selfieTestView;
    private ImageView groupTestView;
    // Boolean representing whether scaleUp or scaleDown button is visible
    private boolean isInvisible; // Put state into bool var to speed performance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_test);

        Gson gson = new Gson();
        Intent intent = getIntent();
        String bitmapsJson = intent.getStringExtra("BitmapArray");
        String groupFileName = intent.getStringExtra("GroupFileName");
        selfieFileName = intent.getStringExtra("SelfieFileName");

        Type listType = new TypeToken<ArrayList<Bitmap>>(){}.getType();
        List<Bitmap> bitmaps = new Gson().fromJson(bitmapsJson, listType);

        groupTestView = (ImageView)findViewById(R.id.groupTestView);
        selfieTestView = (ImageView)findViewById(R.id.selfieTestView);

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
                .resizeDimen(R.dimen.size1, R.dimen.size1)
                .onlyScaleDown()
                .into(selfieTestView);


        groupTestView.setImageBitmap(getARGBImage());
        //selfieTestView.setImageBitmap(resizedSelfie);

        // Set scaleDown button to invisible by default, can't scale down from size1. No size0.
        findViewById(R.id.scaleDown).setVisibility(Button.INVISIBLE);
        // One button is invisible
        isInvisible = true;
    }

    /**
     * Returns a resource representing the current dp the image should be scaled to
     * @return
     */
    private int getCurrentDimension() {
        // Switch for speed the selfieResSize
        switch (selfieResSize) {
            case 1:
                return R.dimen.size1;
            case 2:
                return R.dimen.size2;
            case 3:
                return R.dimen.size3;
            case 4:
                return R.dimen.size4;
        }
        return 0; // error
    }

    /**
     * Scales the selfie image up to next size dimension found in R.dimen. If limit is reached,
     * button is set to invisible.
     * @param view
     */
    public void scaleUp(View view) {
        // See if scaleDown button is invisible. If so, set it to be visible
        if (isInvisible) {
            // We're in scaleUp mode so only scaleDown button should be visible
            findViewById(R.id.scaleDown).setVisibility(Button.VISIBLE);
            // We set button to visible
            isInvisible = false;
        }

        // If size + 1 would not equal the max selfie size, increment up
        if (++selfieResSize < SELFIE_SIZE_THRESHOLD) {
            // Use picasso to scale down and maintain aspect ratio
            Picasso.with(this)
                    .load(new File(selfieFileName))
                    .resizeDimen(getCurrentDimension(), getCurrentDimension())
                    .onlyScaleDown()
                    .into(selfieTestView);
        }
        // Else increment up and set button to invisible so user doesn't press it again
        else {
            // Use picasso to scale down and maintain aspect ratio
            Picasso.with(this)
                    .load(new File(selfieFileName))
                    .resizeDimen(getCurrentDimension(), getCurrentDimension())
                    .onlyScaleDown()
                    .into(selfieTestView);

            Button scaleUpButton = findViewById(R.id.scaleUp);
            // Set to invisible
            scaleUpButton.setVisibility(Button.INVISIBLE);
            // A button is invisible, set it to true
            isInvisible = true;
        }
    }

    /**
     * Scales the selfie image down to the previous size dimension found in R.dimen. If limit is reached,
     * button is set to invisible.
     * @param view
     */
    public void scaleDown(View view) {
        // See if scaleDown button is invisible. If so, set it to be visible
        if (isInvisible) {
            // We're in scaleDown mode so only scaleUp button should be visible
            // The user can scale up after at least one scaleDown
            findViewById(R.id.scaleUp).setVisibility(Button.VISIBLE);
            // We set button to visible
            isInvisible = false;
        }

        // If size - 1 would not equal 1, decrement down
        if (--selfieResSize > 1) {
            // Use picasso to scale down and maintain aspect ratio
            Picasso.with(this)
                    .load(new File(selfieFileName))
                    .resizeDimen(getCurrentDimension(), getCurrentDimension())
                    .onlyScaleDown()
                    .into(selfieTestView);
        }
        // Else decrement down and set button to invisible so user doesn't press it again
        else {
            // Use picasso to scale down and maintain aspect ratio
            Picasso.with(this)
                    .load(new File(selfieFileName))
                    .resizeDimen(getCurrentDimension(), getCurrentDimension())
                    .onlyScaleDown()
                    .into(selfieTestView);

            Button scaleUpButton = findViewById(R.id.scaleDown);
            // Set to invisible
            scaleUpButton.setVisibility(Button.INVISIBLE);
            // A button is invisible, set it to true
            isInvisible = true;
        }
    }

    //blend function using paint
    //May need to create new drawables for colors (errors)
    private Bitmap getARGBImage() {
        // Add selfiebitmap to drawable

        // Create black image
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;

        // Get resources returns the drawable folder attached to this context (activity)
        // but we don't have the selfie photo in the drawable folder... so there is nothing
        // much useful there. It returns null (I tried it). And from my research there is no
        // way to code the image into the drawable folder. That must be done by hand but of course
        // we know we can't do that ;) haha. So... we need to somehow perform this same functionality
        // with the photos we do have.

        // Problem is these decode functions below...
        // Should create red version of selfie photo
        Bitmap red = BitmapFactory.decodeResource(getResources(), R.color.red, opt);
        // Should create blue version of selfie photo
        Bitmap green = BitmapFactory.decodeResource(getResources(), R.color.green, opt);
        // Should create green version of selfie photo
        Bitmap blue = BitmapFactory.decodeResource(getResources(), R.color.blue, opt);
        // Should create gray version of selfie photo
        Bitmap alphaGray = BitmapFactory.decodeResource(getResources(), R.color.alpha, opt);

        int width = red.getWidth();   // failed because red is null
        int height = red.getHeight(); // same

        // ARGB stands for Alpha Red Green Blue configuration
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.eraseColor(Color.BLACK);
        // Done!

        // Get regular image back
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
        // Done!

        // Create alpha photo, fully opaque, and make the background transparent
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

        // Return created bitmap
        return result;
    }
}
