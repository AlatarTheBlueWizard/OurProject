package com.example.practicewizards;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SelfieAcitivity extends AppCompatActivity {
    private static final String TAG = "SelfieAcitivty.java";
    private static int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private TextureView selfieView;
    private TextureView.SurfaceTextureListener selfieTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface Texture Available");
            setUpCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    //CAMERA DEVICE FOR SELFIE VIEW
    private CameraDevice selfieCameraDevice;
    private CameraDevice.StateCallback selfieCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "Set opened camera");
            selfieCameraDevice = camera;
            try {
                startPic();
            } catch (CameraAccessException e) {
                Log.e(TAG, "Error: unable to access camera");
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            selfieCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            selfieCameraDevice = null;
        }
    };

    //FUNCTIONS MEMBER VARIABLES
    // State members
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    // Hold the state
    private int captureState = STATE_PREVIEW;

    // Boolean representing whether picture has been taken or not
    private boolean picTaken = false;
    // Bitmap of image
    private Bitmap bitmap;
    // Button to take selfie photo
    private Button selfieTakeImageButton;
    // Folder for Selfies
    private File selfiePhotoFolder;
    // File name for selfie picture
    private String selfiePhotoFileName;
    // Boolean for whether or not front facing camera can focus
    private boolean canFocus;
    // Boolean representing whether or not the camera is ready or not
    private boolean isReady = false;
    // Boolean representing whether or not the photo saving has started
    private boolean saveStarted = false; // start out not starting a save

    private HandlerThread selfieBackgroundHandlerThread;
    private Handler selfieBackgroundHandler;
    private String selfieCameraDeviceId; // for setup of the camera
    private CaptureRequest.Builder selfieCaptureRequestBuilder;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private final ImageReader.OnImageAvailableListener selfieOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "Post last image to be saved");
                    // Call our runnable to save photo to storage
                    // Post to the handler the latest image reader
                    selfieBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            };

    // Nested runnable class
    private class ImageSaver implements Runnable {
        private final Image image;

        public ImageSaver(Image image) {
            this.image = image;
        }
        @Override
        public void run() {
            Log.i(TAG, "Running ImageSaver on last image");
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            // Remaining bytes
            byte[] bytes = new byte[byteBuffer.remaining()];
            // Call get to retrieve all the bytes representing the image data
            byteBuffer.get(bytes);
            // Now put bytes into file
            FileOutputStream fileOutputStream = null;
            try {
                // createPhotoFolder() should have already been called
                Log.i(TAG, "Write the photo to the photo filename");
                if (!selfiePhotoFolder.exists())
                    Log.e(TAG, "Called create photo folder, it still doesn't exist" +
                            selfiePhotoFolder.mkdirs());
                fileOutputStream = new FileOutputStream(selfiePhotoFileName); // open file
                fileOutputStream.write(bytes); // Write the bytes to the file
                Log.d(TAG, "File Name: " + selfiePhotoFileName);

                // Set picTaken to true, picture and file saving have been successful
                picTaken = true;
                Log.i(TAG, "File saved");

                // Update the UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // We're done saving, set next button visible
                        findViewById(R.id.to_editor).setVisibility(View.VISIBLE);
                        // Hide texture view but keep space dimensions
                        findViewById(R.id.selfieView).setVisibility(View.INVISIBLE);
                        // Allow user to click next
                        // Find the view and set its visibility on
                        final ImageView imageView = findViewById(R.id.selfieImageDisplayView);
                        // Show user image
                        imageView.setVisibility(View.VISIBLE);
                        // Use picasso to pull the file image and center it inside of the image view
                        Picasso.with(getApplicationContext())
                                .load(new File(selfiePhotoFileName))
                                .into(imageView);

                        Log.d(TAG, "Set Display View to visible");

                        imageView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Now hide from user
                                imageView.setVisibility(View.INVISIBLE);
                                // And reopen live stream
                                findViewById(R.id.selfieView).setVisibility(View.VISIBLE);
                                // Make sure bitmap is not null
                                if (bitmap != null) {
                                    // Free up resources
                                    bitmap.recycle();
                                    bitmap = null; // Help GC
                                }
                            }
                        }, 3000);

                        //groupTakeImageButton.setText(R.string.retake);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException nullPtr) {
                Log.e(TAG, "Null something");
                nullPtr.printStackTrace();
            }
            finally {
                // Close image
                Log.i(TAG, "Close the output stream");
                image.close();
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private CameraCaptureSession previewCaptureSession;
    private CameraCaptureSession.CaptureCallback previewCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult captureResult) {
                    Log.d(TAG, "Processing results");
                    switch (captureState) {
                        case STATE_PREVIEW:
                            Log.d(TAG, "STATE_PREVIEW");
                            // Do nothing
                            break;
                        case STATE_WAIT_LOCK:
                            Log.d(TAG, "STATE_WAIT_LOCK");
                            // Set state back to preview to avoid taking tons of pics
                            captureState = STATE_PREVIEW;

                            if (canFocus) {
                                // Integer for Auto Focus State
                                Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                                // SUPPORT new and old devices
                                if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                        afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                    Log.i(TAG, "Start Still Capture Call");
                                    // we've started our saving process
                                    saveStarted = true;
                                    startStillCapture();
                                    Log.i(TAG, "AF Locked");
                                }
                            }
                            else {
                                // we've started our saving process
                                saveStarted = true;
                                startStillCapture();
                            }
                            break;
                    }
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    process(result); // Start Still Capture
                    Log.i(TAG, "Results Processed");

                    /* A BETTER WAY
                    // Stop streaming the camera. Hold the state
                    try {
                        Log.d(TAG, "Stop Repeating");
                        session.stopRepeating(); // Stop repeating requests
                        Log.d(TAG, "Close the Camera");
                        closeCamera(); // Close camera
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error in closing camera");
                        e.printStackTrace();
                    }
                    */
                }
            };

    // Image size
    private Size imageSize;
    // Image reader
    private ImageReader imageReader;

    /**
     * Creates views, also Logs the file location from public path.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);
        getWindow().getDecorView().setBackgroundColor(Color.argb(255, 0, 100, 100));

        Intent selfieIntent = getIntent();
        // Set view
        selfieView = (TextureView) findViewById(R.id.selfieView);

        // Get our photo folder ready
        createPhotoFolder();

        // Try to create a unique photo file
        try {
            Log.d(TAG, "call createPhotoFileName() in stillCapCallback");
            createPhotoFileName();
        } catch (IOException e) {
            Log.e(TAG, "Error in calling createPhotoFileName()");
            e.printStackTrace();
        }

        // Log
        Log.i(TAG, "Files Location" + selfiePhotoFolder.getAbsolutePath());


        selfieTakeImageButton = findViewById(R.id.btn_takeSelfie);
        selfieTakeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Post onto the queue
                selfieBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // First set next button to invisible, not ready yet
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.to_editor).setVisibility(View.INVISIBLE);
                            }
                        });

                        // Then Call lock focus to begin taking our picture!
                        lockFocus();
                    }
                });
            }
        }); // End of onClickListener initialization
    }

    /**
     * Starts next activity to merge the two pictures
     * @param view reference to views state
     */
    public void startMergeActivity(View view) {
        // Don't start next activity if the user hasn't taken a picture
        // and saved the image
        // make sure we have a saved image. Double check also the bitmap
        if (picTaken) {
            // First set take pic button to invisible
            findViewById(R.id.btn_takeSelfie).setVisibility(View.INVISIBLE);
            // Post to decode the file
            selfieBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Decode the file and then try to open its exifinterface data
                    bitmap = BitmapFactory.decodeFile(selfiePhotoFileName);
                }
            });
            // Then post to rotate the image as necessary and start next activity
            selfieBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Make sure orientation of bitmap is correct
                    ExifInterface exifInterface = null;
                    try {
                        exifInterface = new ExifInterface(selfiePhotoFileName);
                        // See if the returned getAttribute string tag equals "6"
                        // Left Landscape
                        if (exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).equals("6")) {
                            Matrix matrix = new Matrix();
                            // Add 90 to create portrait bitmap
                            // plan to rotate bitmap 90 degrees to portrait mode
                            matrix.postRotate(90);
                            // Create a new bitmap from the desired bitmap (member variable)
                            // With no offset in x and y and its original width/height
                            // But with a different matrix rotation
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), matrix, true);
                        }
                        // Right Landscape
                        else if (exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
                                .equals("8")) {
                            Matrix matrix = new Matrix();
                            // Subtract 90 to create portrait bitmap
                            // plan to rotate bitmap 90 degrees to portrait mode
                            matrix.postRotate(-90);
                            // Create a new bitmap from the desired bitmap (member variable)
                            // With no offset in x and y and its original width/height
                            // But with a different matrix rotation
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), matrix, true);
                        }
                        Log.d(TAG, "ExifInterface " + exifInterface
                                .getAttribute(ExifInterface.TAG_ORIENTATION));
                    } catch (IOException e) {
                        Log.e(TAG, "File Access Error");
                        e.printStackTrace();
                    }
                    // Run on UI, go to next activity
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Merge intent starting");
                            // Get intent that created this activity to retrieve bitmap and group filename
                            Intent intent = getIntent();
                            String bitmapJson = intent.getStringExtra("Bitmap");
                            String groupFileName = intent.getStringExtra("GroupFileName");


                            Gson gson = new Gson();
                            Bitmap groupBitmap = gson.fromJson(bitmapJson, Bitmap.class);

                            // Create list of bitmaps to be passed
                            List<Bitmap> bitmaps = new ArrayList<>();
                            bitmaps.add(0, groupBitmap); // Index 0
                            bitmaps.add(1, bitmap);      // Index 1

                            String bitmapsJson = gson.toJson(bitmaps);

                            Intent mergeIntent = new Intent(getApplicationContext(), PhotoTest.class);
                            mergeIntent.putExtra("BitmapArray", bitmapsJson);    // Add bitmaps
                            mergeIntent.putExtra("GroupFileName", groupFileName); // Add group photo
                            mergeIntent.putExtra("SelfieFileName", selfiePhotoFileName); // Add selfie photo
                            startActivity(mergeIntent);
                        }
                    });
                }
            });
        }
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
      
        // See if view is available
        if(selfieView.isAvailable()) {
            // Set up and connect
            setUpCamera(selfieView.getWidth(), selfieView.getHeight());
            connectCamera();
        }
        // Else view not available, set it
        else {
            // Call set on groupView
            selfieView.setSurfaceTextureListener(selfieTextListener);
        }
    }

    /**
     * Prompt user to allow camera permissions if he/she has previously declined to do so
     * @param requestCode What request are we using
     * @param permissions Have permissions been granted
     * @param grantResults What permissions have been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // IF permission code is the camera permission code
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            // If the first result given which will be the camera permission has not been granted
            // Make a toast notifying user
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Application won't run without camera services", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * When user navigates away, close the camera and stop the background thread
     */
    @Override
    protected void onPause() {
        closeCamera();
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
     * Creates a Camera Manager Object and iterates through its CameraIdList to determine which camera ID is needed to connect the camera
     * @param width
     * Uses the the width of the texture view to determine image size width
     * @param height
     * Uses the the height of the texture view to determine image size height
     */
    private void setUpCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d(TAG, "Setup Camera");
        try {
            for(String cameraId : cameraManager.getCameraIdList()) {
                //create an object to store each camera ID's camera characteristics
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                // See if the Lens facing of found camera is front facing. If it is, we want it!
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    //Set the image size to be the width and height of the texture view
                    imageSize = new Size(selfieView.getWidth(), selfieView.getHeight());
                    // image reader with group view's width, height, and maxImages is 2 because
                    // "discarding all-but-the-newest Image requires temporarily acquiring two
                    // Image at once." (https://developer.android.com/reference/android/
                    //                              media/ImageReader#acquireLatestImage())
                    imageReader = ImageReader.newInstance(selfieView.getWidth(), selfieView.getHeight(),
                            ImageFormat.JPEG, 2);
                    //Set image reader's available listener
                    imageReader.setOnImageAvailableListener(selfieOnImageAvailableListener,
                            selfieBackgroundHandler);
                    //set the Camera Device ID to the selected camera
                    selfieCameraDeviceId = cameraId; //create a parameter for this

                    // See if Auto Focus works on this camera
                    int[] afAvailableModes = cameraCharacteristics.
                            get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
                    if (afAvailableModes[0] == CameraMetadata.CONTROL_AE_MODE_OFF) {
                        Log.i(TAG, "No Focus on Camera");
                        canFocus = false;
                    }
                    return;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to iterate through camera ID list from cameraManager");
            e.printStackTrace();
        }
    }

    /**
     * Uses a camera ID, a camera device, and a background handler to connect and open the camera
     */
    private void connectCamera() {
        //create a camera manager object and retrieve its service context
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d(TAG, "Connect Camera");
        //check if android sdk version supports Camera 2 API
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //check if user granted permission to access camera
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                try {
                    //open the camera
                    cameraManager.openCamera(selfieCameraDeviceId, selfieCameraDeviceStateCallback, selfieBackgroundHandler);
                    Log.i(TAG, "Camera Opened");
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Camera failed to open");
                    e.printStackTrace();
                }
            } else {
                //if permission not yet granted, ask user for permission
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "App requires access to camera", Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
            }
        } else {
            try {
                //if not within SDK range, try opening anyway
                cameraManager.openCamera(selfieCameraDeviceId, selfieCameraDeviceStateCallback, selfieBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera failed to open");
                e.printStackTrace();
                return; // Don't let us get to setting isReady to true
            }
        }
        // After setup camera and connect camera have successfully been completed, we're ready
        // to take the picture!
        isReady = true;
    }

    /**
     * Sets up the surfaceTexture, sets its buffer size, creates a previewSurface
     * to add as target for our selfieCaptureRequestBuilder. The builder comes from
     * a createCaptureRequest() on the selfieCameraDevice using the TEMPLATE_STILL_CAPTURE
     * for a single picture.
     * Then, we createCaptureSession() from the previewSurface, imageReader surface, and
     * create a new CameraCaptureSession.StateCallBack() anonymous object.
     * When onConfigured() is called on the CaptureSession, the previewCaptureSession member
     * variable will be set. Then we will continue to setRepeatingRequests for image capturing on
     * builder and background handler.
     * @throws CameraAccessException
     */
    private void startPic() throws CameraAccessException {
        Log.d(TAG, "Start Pic");
        // Set textures
        SurfaceTexture surfaceTexture = selfieView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(selfieView.getWidth(), selfieView.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        // Set CaptureRequestBuilder from camera createCaptureRequest() method and
        // add the builder to target the preview surface
        // Create the capture session
        try {
            selfieCaptureRequestBuilder = selfieCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            selfieCaptureRequestBuilder.addTarget(previewSurface);
            selfieCameraDevice.createCaptureSession(Arrays.asList(previewSurface,
                    imageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                /**
                 * Create a capture session live streaming the CaptureRequestBuilder
                 * @param session
                 */
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG, "Configure Capture Session");
                    previewCaptureSession = session; // Set member
                    // Try to set repeating requests
                    try {
                        session.setRepeatingRequest(selfieCaptureRequestBuilder.build(),
                                null, selfieBackgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error in accessing cameras");
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Unable to setup camera preview",
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    Log.i(TAG, "Close Capture Session and Image Reader");
                    if (imageReader != null) {
                        imageReader.close();
                    }
                }

            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Locks the focus on the live streaming camera device for setup to take the actual picture.
     * Picture will be taken immediately after.
     */
    private void lockFocus() {
        Log.d(TAG, "Lock Focus()");
        // Set our CaptureRequestBuilder to lock the focus
        selfieCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        // Lock the capture state
        // STATE_WAIT_LOCK is final int = 1
        // Capture State holds the current state of the camera preview (whether its locked or not)
        captureState = STATE_WAIT_LOCK;
        // Try to capture the image
        try {
            Log.d(TAG, "Capture Picture");
            // Put it on the background thread
            previewCaptureSession.capture(selfieCaptureRequestBuilder.build(), previewCaptureCallback,
                    selfieBackgroundHandler);
        }
        // Catch any accessing exceptions
        catch (CameraAccessException camAccessExcept) {
            Log.e(TAG, "Error Accessing Camera for capture()");
            camAccessExcept.printStackTrace(); // Print stack
        }
    }

    /**
     * Closes the selfieCameraDevice
     */
    private void closeCamera() {
        if(selfieCameraDevice != null) {
            selfieCameraDevice.close();
            selfieCameraDevice = null;
        }
    }

    /**
     * initializes declared background handler thread and sets a name for it
     * starts the thread and initializes the handler using the same thread
     */
    private void startBackgroundThread() {
        selfieBackgroundHandlerThread = new HandlerThread("GroupCameraThread");
        selfieBackgroundHandlerThread.start();
        selfieBackgroundHandler = new Handler(selfieBackgroundHandlerThread.getLooper());
    }

    /**
     * Safely quits and joins any started threads and sets variables back to null
     */
    private void stopBackgroundThread() {
        //Avoid errors on stopping thread by quitting safely
        selfieBackgroundHandlerThread.quitSafely();
        try {
            //Join threads
            selfieBackgroundHandlerThread.join();
            //Set Background handler and Handler thread to null
            selfieBackgroundHandlerThread = null;
            selfieBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Group Background Handler Thread failed to join after quitting safely");
            e.printStackTrace();
        }
    }

    /**
     * Creates a still capture session for a single picture to be taken in portrait mode (90֯ ).
     * Create a stillCaptureCallback with the needed call to createPhotoFileName() when capture
     * has started.
     */
    private void startStillCapture() {
        Log.d(TAG, "Start Still Capture()");
        // Try to create a CaptureCall back
        try {
            // Use the still capture template for our capture request builder
            // Add target to be the imageReader's surface
            // Set the orientation to be portrait
            selfieCaptureRequestBuilder =
                    selfieCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            selfieCaptureRequestBuilder.addTarget(imageReader.getSurface());

            Log.v(TAG, "Still capture at orientation: " + ORIENTATIONS.valueAt(3));
            selfieCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    ORIENTATIONS.valueAt(3));
            Log.d(TAG, "Target Set");

            // Create stillCaptureCallback
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        // When capture has started, call createPhotoFileName()
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                                     @NonNull CaptureRequest request,
                                                     long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                        }
                    };
            Log.d(TAG, "Begin Capture");
            // Call capture! Give it the builder and the stillCaptureCallback
            previewCaptureSession.capture(selfieCaptureRequestBuilder.build(), stillCaptureCallback,
                    null); // Already on the background thread, give thread null
        }
        catch (CameraAccessException camAccessExcept) {
            Log.e(TAG, "Error accessing camera");
            camAccessExcept.printStackTrace();
        }
    }

    /**
     * Contains photoFolder creation method and file name of the photo taken
     * @return selfiePhotoFileName will be returned
     */
    private void createPhotoFolder() {
        //gets external storage from public directory path (DIRECTORY_PICTURES)
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //if imageFile directory doesn't exist
        if (!imageFile.mkdirs())
            Log.e(TAG, "Directory not created");

        // Create folder from the abstract pathname created above (imageFile)
        selfiePhotoFolder = new File(imageFile, "CameraImages");

        //if photo folder doesn't exist
        if(!selfiePhotoFolder.exists()) {
            selfiePhotoFolder.mkdirs(); // Make sub-directory under parent
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
            if (!selfiePhotoFolder.exists()) {
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
        if (selfiePhotoFileName == null) {
            Log.i(TAG, "File Name doesn't exist. Create it.");
            File photoFile = File.createTempFile(prepend, ".jpg", selfiePhotoFolder);
            selfiePhotoFileName = photoFile.getAbsolutePath();
            Log.i(TAG, selfiePhotoFileName);
        }
        //return photo filename
        return selfiePhotoFileName;
    }
}
