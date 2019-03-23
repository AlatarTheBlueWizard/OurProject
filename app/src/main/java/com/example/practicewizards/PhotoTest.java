package com.example.practicewizards;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DrawableUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PhotoTest extends AppCompatActivity {
    private static final String TAG = "MergeActivity";
    private int selfieResSize = 1;
    private static final int SELFIE_SIZE_THRESHOLD = 4;
    // Keep track of selfie file name
    private String selfieFileName;
    private ImageView selfieTestView;
    private ImageView groupTestView;
    private String msg;

    //paramaters for layout
    private android.widget.RelativeLayout.LayoutParams layoutParams;

    // Boolean representing whether scaleUp or scaleDown button is visible
    private boolean isInvisible; // Put state into bool var to speed performance

    // Bitmap for the selfie photo
    private Bitmap selfieBitmap;

    // Thread Handling
    private HandlerThread mergeBackgroundThread;
    private Handler       mergeBackgroundHandler;

    // File saving for our selfieBitmap
    private String mergedSelfieFileName;
    private File mergedSelfieFileFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startBackgroundThread();
        setContentView(R.layout.activity_photo_test);
        // On resume will be called after this to start the background thread

        Gson gson = new Gson();
        Intent intent = getIntent();
        String bitmapsJson = intent.getStringExtra("BitmapArray");
        String groupFileName = intent.getStringExtra("GroupFileName");
        selfieFileName = intent.getStringExtra("SelfieFileName");

        Type listType = new TypeToken<ArrayList<Bitmap>>(){}.getType();
        final List<Bitmap> bitmaps = new Gson().fromJson(bitmapsJson, listType);

        groupTestView = (ImageView)findViewById(R.id.groupTestView);
        selfieTestView = (ImageView)findViewById(R.id.selfieTestView);

        // Group photo is first because it was taken first
        Bitmap group  = bitmaps.get(0);
        selfieBitmap = bitmaps.get(1);

        // Some math here to preserve aspect ratio
        // Just comments for example.
        // A preserved aspect ratio image is such that given
        //      oldWidth / oldHeight == newWidth / newHeight
        // Some Algebra gives us an equivalent equation
        //      oldWidth / newWidth  == oldHeight / newHeight
        // And if that's the truth than given a scale factor (lets say s) the ratios will be
        // preserved: oldWidth / 2 == oldHeight / 2 in ratio. We are downsizing the image to half

        Log.d(TAG, "Selfie width: " + selfieBitmap.getWidth());
        Log.d(TAG, "Selfie height: " + selfieBitmap.getHeight());
        Log.d(TAG, "Group Path: " + groupFileName);
        Log.d(TAG, "Selfie Path: " + selfieFileName);
        try {
            createBitmapFolder();
            createPhotoFileName();
        } catch (IOException e) {
            Log.e(TAG, "Failed to create Photo File Name");
            e.printStackTrace();
        }
        mergeBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fOut = null;
                try {
                    fOut = new FileOutputStream(mergedSelfieFileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                bitmapOverlayToCenter(bitmaps.get(0), bitmaps.get(1));
                Bitmap mergedSelfieBitmap = createGrayScale(selfieBitmap);
                mergedSelfieBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                try {
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mergeBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                // Use picasso to scale down and maintain aspect ratio
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Picasso.with(getApplicationContext())
                                .load(new File(mergedSelfieFileName))
                                .resizeDimen(R.dimen.size1, R.dimen.size1)
                                .onlyScaleDown()
                                .into(selfieTestView);
                    }
                });
            }
        });



        //groupTestView.setImageBitmap(group);
//        selfieTestView.setImageBitmap(createGreenGrayBitmap(selfieBitmap));


        // Set scaleDown button to invisible by default, can't scale down from size1. No size0.
        findViewById(R.id.scaleDown).setVisibility(Button.INVISIBLE);
        // One button is invisible
        isInvisible = true;

        //Long-Click-Listener for the selfieTestView drag and drop
        selfieTestView.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v) {
                ClipData.Item item = new ClipData.Item((CharSequence)v.getTag());
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};

                ClipData dragData = new ClipData(v.getTag().toString(), mimeTypes, item);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(selfieTestView);

                v.startDrag(dragData, myShadow, null, 0);
                return true;
            }
        });

        //On-Drag listener for selfieTestView (Allows dragging and dropping of the photo)
        selfieTestView.setOnDragListener(new View.OnDragListener(){
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        layoutParams = (RelativeLayout.LayoutParams)v.getLayoutParams();
                        Log.d(msg, "Action is DragEvent.ACTION_DRAG_STARTED");
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        Log.d(msg, "Action is DragEvent.ACTION_DRAG_ENTERED");
                        int x_cord = (int) event.getX();
                        int y_cord = (int) event.getY();
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        Log.d(msg, "Action is DragEvent.ACTION_DRAG_EXITED");
                        x_cord = (int) event.getX();
                        y_cord = (int) event.getY();
                        layoutParams.leftMargin = x_cord;
                        layoutParams.topMargin = y_cord;
                        v.setLayoutParams(layoutParams);
                        break;
                        // During drag event
                    case DragEvent.ACTION_DRAG_LOCATION:
                        Log.d(msg, "Action is DragEvent.ACTION_DRAG_LOCATION");
                        x_cord = (int) event.getX();
                        y_cord = (int) event.getY();
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        Log.d(msg, "Action is DragEvent.ACTiON_DRAG_ENDED");
                        break;
                    case DragEvent.ACTION_DROP:
                        Log.d(msg, "ACTION_DROP event");
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        //checks if user used the motion to touch the image
        selfieTestView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ClipData data = ClipData.newPlainText("","");
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(selfieTestView);

                    selfieTestView.startDrag(data, shadowBuilder, selfieTestView, 0);
                    selfieTestView.setVisibility(View.VISIBLE);
                    return true;
                } else {
                    return false;
                }
            }
        });
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
                    .load(new File(mergedSelfieFileName))
                    .resizeDimen(getCurrentDimension(), getCurrentDimension())
                    .onlyScaleDown()
                    .into(selfieTestView);
        }
        // Else increment up and set button to invisible so user doesn't press it again
        else {
            // Use picasso to scale down and maintain aspect ratio
            Picasso.with(this)
                    .load(new File(mergedSelfieFileName))
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
                    .load(new File(mergedSelfieFileName))
                    .resizeDimen(getCurrentDimension(), getCurrentDimension())
                    .onlyScaleDown()
                    .into(selfieTestView);
        }
        // Else decrement down and set button to invisible so user doesn't press it again
        else {
            // Use picasso to scale down and maintain aspect ratio
            Picasso.with(this)
                    .load(new File(mergedSelfieFileName))
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
        //Bitmaps Red, Green, Blue, and AlphaGray
        Bitmap red = createRedGrayBitmap(selfieBitmap);
        Bitmap green = createGreenGrayBitmap(selfieBitmap);
        Bitmap blue = createBlueGrayBitmap(selfieBitmap);
        Bitmap alphaGray = createGrayScale(selfieBitmap);



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


    /**
     * Creates a red grayscale selfie bitmap. Uses the createGrayScale method after the red
     * channeled version of the selfie bitmap is created. We need a gray scaled versions of the red
     * channel of the selfie photo.
     */
    public Bitmap createRedGrayBitmap(Bitmap source) {
        // Create bitmap to be returned from its width and height and its configuration
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                source.getConfig());

        // Create ARGB variables
        // Disclude G and B, we want Red
        int alpha;
        int red;


        /*
        Loop through all the pixels and retrieve its pixel amount
         */
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                // Retrieve pixel amount
                int pixel = source.getPixel(x, y);

                // Retrieves the color channels for alpha and red of the retrieved
                // pixel integer in ARGB form
                alpha = Color.alpha(pixel); // A
                red = Color.red(pixel);     // R

                // sets a pixel (x,y) on output bitmap to ARGB
                bitmap.setPixel(x, y, Color.argb(alpha, red, 0, 0));
            }
        }
        // Return the gray scaled version of the red bitmap
        return createGrayScale(bitmap);
    }

    /**
     * Creates a blue grayscale selfie bitmap. Uses the createGrayScale method after the blue
     * channeled version of the selfie bitmap is created. We need a gray scaled versions of the blue
     * channel of the selfie photo.
     */
    public Bitmap createBlueGrayBitmap(Bitmap source) {
        // Create bitmap to be returned from its width and height and its configuration
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                source.getConfig());

        // Create ARGB variables
        // Disclude G and B, we want Red
        int alpha;
        int blue;


        /*
        Loop through all the pixels and retrieve its pixel amount
         */
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                // Retrieve pixel amount
                int pixel = source.getPixel(x, y);

                // Retrieves the color channels for alpha and red of the retrieved
                // pixel integer in ARGB form
                alpha = Color.alpha(pixel); // A
                blue = Color.red(pixel);     // R

                // sets a pixel (x,y) on output bitmap to ARGB
                bitmap.setPixel(x, y, Color.argb(alpha, 0, 0, blue));
            }
        }
        // Return the gray scaled version of the red bitmap
        return createGrayScale(bitmap);
    }

    /**
     * Creates an alpha grayscale selfie bitmap. Uses the createGrayScale method after the alpha
     * channeled version of the selfie bitmap is created. We need a gray scaled versions of the alpha
     * channel of the selfie photo.
     */
    public Bitmap createAlphaGrayBitmap(Bitmap source) {
        // Create bitmap to be returned from its width and height and its configuration
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                source.getConfig());

        // Create ARGB variables
        // Disclude G and B, we want Red
        int alpha;


        /*
        Loop through all the pixels and retrieve its pixel amount
         */
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                // Retrieve pixel amount
                int pixel = source.getPixel(x, y);

                // Retrieves the color channels for alpha and red of the retrieved
                // pixel integer in ARGB form
                alpha = Color.alpha(pixel); // A

                // sets a pixel (x,y) on output bitmap to ARGB
                bitmap.setPixel(x, y, Color.argb(alpha, 0, 0, 0));
            }
        }
        // Return the gray scaled version of the red bitmap
        return createGrayScale(bitmap);
    }

    /**
     * Creates a green grayscale selfie bitmap. Uses the createGrayScale method after the red
     * channeled version of the selfie bitmap is created. We need a gray scaled versions of the red
     * channel of the selfie photo.
     */
    public Bitmap createGreenGrayBitmap(Bitmap source) {
        // Create bitmap to be returned from its width and height and its configuration
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                source.getConfig());

        // Create ARGB variables
        // Disclude G and B, we want Red
        int alpha;
        int green;


        /*
        Loop through all the pixels and retrieve its pixel amount
         */
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                // Retrieve pixel amount
                int pixel = source.getPixel(x, y);

                // Retrieves the color channels for alpha and red of the retrieved
                // pixel integer in ARGB form
                alpha = Color.alpha(pixel); // A
                green = Color.green(pixel);     // R

                // sets a pixel (x,y) on output bitmap to ARGB
                bitmap.setPixel(x, y, Color.argb(alpha, 0, green, 0));
            }
        }
        // Return the gray scaled version of the red bitmap
        return createGrayScale(bitmap);
    }


    /**
     * Creates a grayscaled image based on the input source bitmap.
     * Uses ARGB implementation of Android's pixel storing. A = Alpha, R = Red, B = Blue, G = Green
     * Src: https://xjaphx.wordpress.com/2011/06/21/image-processing-grayscale-image-on-the-fly/
     */
    public Bitmap createGrayScale(Bitmap source) {
        Log.d(TAG, "Called");
        // constant factors for our algorithm. These are the correct percentages for a
        // valid grayscale
        final double PERCENT_RED   = 0.299; // 30%
        final double PERCENT_BLUE  = 0.587; // 59%
        final double PERCENT_GREEN = 0.114; // 11%

        // Create bitmap to be returned from its width and height and its configuration
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                source.getConfig());

        // Create ARGB variables
        int alpha;
        int red;
        int blue;
        int green;

        // Integer representing the current pixel ARGB (actually in hex, AARRGGBB)
        // Example. Pure red is #FFFF0000. Green is #FF00FF00. Blue is #FF0000FF with always
        // #FF for the alpha because we want completely opaque color, not transparent as int
        // #00.
        int pixel; // One given pixel of the source bitmap

        /*
         * Loop through all the pixels of the source bitmap and scale their pixels to grayscale
         * Loops through all X (width) and Y (height) pixels up to the actual width and height
         * Do pre-increment for speed
         */
        for (int x = 0; x < source.getWidth(); ++x)
            for (int y = 0; y < source.getHeight(); ++y) {
                // Get current pixel
                pixel = source.getPixel(x, y); // retrieves the (x, y) pixel from the bitmap

                // Retrieves the color channels for alpha, red, green, and blue of the retrieved
                // pixel integer in ARGB form
                alpha = Color.alpha(pixel); // A
                red = Color.red(pixel);     // R
                blue = Color.blue(pixel);   // B
                green = Color.green(pixel); // G

                // Ramp up conversion to single value
                red = blue = green = (int)(PERCENT_RED * red +
                                            PERCENT_BLUE * blue +
                                              PERCENT_GREEN + green);

                // sets a pixel (x,y) on output bitmap to ARGB
                bitmap.setPixel(x, y, Color.argb(alpha, red, green, blue));

            }
        return bitmap;
    }

    /**
     * When app is resumed, start background thread again, setup cameras again, connect to the
     * camera. If the view is not available, set the view
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Start thread
        startBackgroundThread();
    }

    /**
     * When user navigates away, close the camera and stop the background thread
     */
    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * When activity is safely killed
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * When activity is killed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Safely quits and joins any started threads and sets variables back to null
     */
    private void stopBackgroundThread() {
        //Avoid errors on stopping thread by quitting safely
        mergeBackgroundThread.quitSafely();
        try {
            //Join threads
            mergeBackgroundThread.join();
            //Set Background handler and Handler thread to null
            mergeBackgroundThread = null;
            mergeBackgroundHandler= null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Group Background Handler Thread failed to join after quitting safely");
            e.printStackTrace();
        }
    }

    /**
     * initializes declared background handler thread and sets a name for it
     * starts the thread and initializes the handler using the same thread
     */
    private void startBackgroundThread() {
        // Make sure its not already running
        if (mergeBackgroundThread == null) {
            mergeBackgroundThread = new HandlerThread("MergeThread");
            mergeBackgroundThread.start();
            mergeBackgroundHandler = new Handler(mergeBackgroundThread.getLooper());
        }
    }

    /**
     * Contains photoFolder creation method and file name of the photo taken
     * @return selfiePhotoFileName will be returned
     */
    private void createBitmapFolder() {
        //gets external storage from public directory path (DIRECTORY_PICTURES)
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //if imageFile directory doesn't exist
        if (!imageFile.mkdirs())
            Log.e(TAG, "Directory not created");

        // Create folder from the abstract pathname created above (imageFile)
        mergedSelfieFileFolder = new File(imageFile, "CameraImages");

        //if photo folder doesn't exist
        if(!mergedSelfieFileFolder.exists()) {
            mergedSelfieFileFolder.mkdirs(); // Make sub-directory under parent
        }
    }

    /**
     * Method creates name of photo file, adds additional date format and timestamp.
     * if photo folder does not exist, notifies of its non existence, also creates a temp
     * file that is prepended with ".jpg" and gets the path from the photo file.
     * @throws IOException if working with file fails
     */
    private String createPhotoFileName()throws IOException {
        //adds a date format for the timestamp of the photo taken
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "PHOTO_" + timeStamp + "_";
        try {
            //if photo folder does not exist...
            if (!mergedSelfieFileFolder.exists()) {
                throw new NullPointerException("Photo Folder does not exist");
            }
        }
        catch (NullPointerException folderError) {
            //will notify of folders non-existence
            Log.e(TAG, "Folder non-existent");
            folderError.printStackTrace();
        }

        //creates temporary photo file with ".jpg" suffix which is then prepended with
        //existing photo folder.
        // Don't create to temporary files if selfiePhotoFileName already exists
        if (mergedSelfieFileName == null) {
            Log.i(TAG, "File Name doesn't exist. Create it.");
            File photoFile = File.createTempFile(prepend, ".jpg", mergedSelfieFileFolder);
            mergedSelfieFileName = photoFile.getAbsolutePath();
            Log.i(TAG, mergedSelfieFileName);
        }
        //return photo filename
        return mergedSelfieFileName;
    }

    public Bitmap bitmapOverlayToCenter(Bitmap bitmap1, Bitmap overlayBitmap) {
        // Get group bitmap dimensions
        int bitmap1Width = bitmap1.getWidth();
        int bitmap1Height = bitmap1.getHeight();
        // Divide by 3 to make overlayBitmap smaller
        int bitmap2Width = overlayBitmap.getWidth() / 3;
        int bitmap2Height = overlayBitmap.getHeight() / 3;

        // Determine position to draw overlayBitmap
        float marginLeft = (float) (bitmap1Width * 0.5 - bitmap2Width * 0.5);
        float marginTop = (float) (bitmap1Height * 0.5 - bitmap2Height * 0.5);

        // Create final bitmap from group bitmap
        Bitmap finalBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, bitmap1.getConfig());
        // Create canvas for drawing
        Canvas canvas = new Canvas(finalBitmap);
        // Draw both bitmaps on top
        canvas.drawBitmap(bitmap1, new Matrix(), null);
        canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null);
        return finalBitmap;
    }
}