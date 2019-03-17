package com.example.practicewizards;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Main Activity class
 * initializes variables for use in various methods throughout main.
 * Uses TextureView to create a new SurfaceTextureListener.
 * Methods include actions such as setting up the camera, calls connectCamera()
 * Methods also handled are those such as the size changing, if it is destroyed,
 * or if it has updated.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GroupPhoto.java";
    private File groupPhotoFolder;
    private String groupPhotoFileName;
    private static final int CAMERA_REQUEST=1888;
    ImageView myImage;
    private static int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    // State members
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    // Hold the state
    private int captureState = STATE_PREVIEW;

    // Boolean representing whether picture has been taken or not
    boolean picTaken = false;
    // Bitmap of image
    private Bitmap bitmap;

    private TextureView groupView;
    private TextureView.SurfaceTextureListener groupTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
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

    //CAMERA DEVICE FOR GROUP VIEW
    private CameraDevice groupCameraDevice;
    private CameraDevice.StateCallback groupCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            groupCameraDevice = camera;
            try {
                startPic();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            groupCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            groupCameraDevice = null;
        }
    };

    //FUNCTIONS MEMBER VARIABLES
    private HandlerThread groupBackgroundHandlerThread;
    private Handler groupBackgroundHandler;
    private String groupCameraDeviceId; // for setup of the camera
    private Size mPhotoSize;
    private Size mPreviewSize;
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener groupOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // Call our runnable to save photo to storage
                    // Post to the handler the latest image reader
                    groupBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
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
                if (!groupPhotoFolder.exists())
                    Log.e(TAG, "Called create photo folder, it still doesn't exist" +
                            groupPhotoFolder.mkdirs());
                fileOutputStream = new FileOutputStream(createPhotoFileName()); // open file
                Toast.makeText(getApplicationContext(), "File Output Stream Created",
                        Toast.LENGTH_SHORT).show();
                fileOutputStream.write(bytes); // Write the bytes to the file
                Log.d(TAG, "File Name: " + groupPhotoFileName);

                // Set picTaken to true, picture and file saving have been successful
                picTaken = true;
                // Save the image to outer class
                bitmap = BitmapFactory.decodeFile(groupPhotoFileName);
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
                    switch (captureState) {
                        case STATE_PREVIEW:
                            // Do nothing
                            break;
                        case STATE_WAIT_LOCK:
                            // Set state back to preview to avoid taking tons of pics
                            captureState = STATE_PREVIEW;

                            // Integer for Auto Focus State
                            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                            // SUPPORT new and old devices
                            if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                Toast.makeText(getApplicationContext(), "AF LOCKED!",
                                        Toast.LENGTH_SHORT).show();
                                startStillCapture();
                                Log.i(TAG, "AF Locked");
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

                    // Stop streaming the camera. Hold the state
                    try {
                        session.stopRepeating(); // Stop repeating requests
                        closeCamera(); // Close camera
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error in closing camera");
                        e.printStackTrace();
                    }
                }
            };
    private CaptureRequest.Builder groupCaptureRequestBuilder;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    // Image size
    private Size imageSize;
    // Image reader
    private ImageReader imageReader;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }


    // Button to take group photo
    private Button groupTakeImageButton;

    /**
     * Creates views, also Logs the file location from public path.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set views
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(Color.argb(255, 0, 100, 100));

        // Get our photo folder ready
        createPhotoFolder();

        // Log
        Log.i(TAG, "Files Location" + groupPhotoFolder.getAbsolutePath());

        // Set the groupView
        groupView = (TextureView)findViewById(R.id.groupView);

        groupTakeImageButton = (Button) findViewById(R.id.btn_takeGroup);
        groupTakeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the picture has not been taken, take it!
                if (!picTaken) {
                    // Call lock focus to begin taking our picture!
                    lockFocus();
                    groupTakeImageButton.setText(R.string.retake);
                }
                // else when clicked after picture has been taken
                // Reset the view and set up cameras again and change the text
                // Set pic taken back to false
                else {
                    // Delete last saved image
                    if (picTaken && groupPhotoFileName != null) {
                        Log.i(TAG, "Delete old photo");
                        File fDelete = new File(groupPhotoFileName);
                        // Make sure it exists
                        if (fDelete.exists()) {
                            // Delete the file and set the string filename to null
                            // No more pic taken
                            fDelete.delete();
                            groupPhotoFileName = null;
                            picTaken = false;
                        }
                    }
                    // Pause momentarily and then resume again.
                    onPause();
                    onResume();
                    // Reset text
                    groupTakeImageButton.setText(R.string.take_group);
                    // Reset bitmap, help Garbage Collector free up the buffer faster
                    bitmap = null;
                }
            }
        }); // End of onClickListener initialization
    }

    /**
     * Starts next activity to take selfie pic
     * @param view reference to views state
     */
    public void startSelfieActivity(View view) {
        // Don't start next activity if the user hasn't taken a picture
        // and saved the image
        // make sure we have a saved image. Double check also the bitmap
        if (picTaken && bitmap != null) {
            Log.i(TAG, "Selfie intent starting");
            Intent selfieIntent = new Intent(this, Main2Activity.class);
            startActivity(selfieIntent);
        }
        // Else image is null, make toast
        else {
            Toast.makeText(getApplicationContext(), R.string.error_pic_not_taken,
                    Toast.LENGTH_SHORT).show();
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
        if(groupView.isAvailable()) {
            // Set up and connect
            setUpCamera(groupView.getWidth(), groupView.getHeight());
            connectCamera();
        }
        // Else view not available, set it
        else {
            // Call set on groupView
            groupView.setSurfaceTextureListener(groupTextListener);
        }
    }

    /**
     * Prompt user to allow camera permissions if he/she has previously declined to do so
     * @param requestCode What request are we using
     * @param permissions Have permissions been granted
     * @param grantResults What permissions have been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Call parent
        // IF permission code is the camera permission code
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            // If the first result given which will be the camera permission has not been granted
            // Make a toast notifying user
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Application won't run without camera services",
                        Toast.LENGTH_LONG).show();
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
        //create a camera manager object and retrieve its service context
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            //iterate through the camera manager object's camera ID list
            for(String cameraId : cameraManager.getCameraIdList()){
                //create an object to store each camera ID's camera characteristics
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                //Skip the first (front facing) camera
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //Set the image size to be the width and height of the texture view
                imageSize = new Size(groupView.getWidth(), groupView.getHeight());
                // image reader with group view's width, height, and maxImages is just 1
                imageReader = ImageReader.newInstance(groupView.getWidth(), groupView.getHeight(),
                        ImageFormat.JPEG, 1);
                //Set image reader's available listener
                imageReader.setOnImageAvailableListener(groupOnImageAvailableListener,
                        groupBackgroundHandler);
                //set the Camera Device ID to the selected camera
                groupCameraDeviceId = cameraId;
                return;
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

        //check if android sdk version supports Camera 2 API
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //check if user granted permission to access camera
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                try {
                    //open the camera
                    cameraManager.openCamera(groupCameraDeviceId, groupCameraDeviceStateCallback, groupBackgroundHandler);
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
                cameraManager.openCamera(groupCameraDeviceId, groupCameraDeviceStateCallback, groupBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera failed to open");
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets up the surfaceTexture, sets its buffer size, creates a previewSurface
     * to add as target for our groupCaptureRequestBuilder. The builder comes from
     * a createCaptureRequest() on the groupCameraDevice using the TEMPLATE_STILL_CAPTURE
     * for a single picture.
     * Then, we createCaptureSession() from the previewSurface, imageReader surface, and
     * create a new CameraCaptureSession.StateCallBack() anonymous object.
     * When onConfigured() is called on the CaptureSession, the previewCaptureSession member
     * variable will be set. Then we will continue to setRepeatingRequests for image capturing on
     * builder and background handler.
     * @throws CameraAccessException
     */
    private void startPic() throws CameraAccessException {
        // Set textures
        SurfaceTexture surfaceTexture = groupView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(groupView.getWidth(), groupView.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        // Set CaptureRequestBuilder from camera createCaptureRequest() method and
        // add the builder to target the preview surface
        // Create the capture session
        groupCaptureRequestBuilder = groupCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        groupCaptureRequestBuilder.addTarget(previewSurface);
        groupCameraDevice.createCaptureSession(Arrays.asList(previewSurface,
                                                              imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {


                    /**
                     * Create a capture session live streaming the CaptureRequestBuilder
                     * @param session
                     */
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        // Set up member
                        previewCaptureSession = session;
                        try {
                            previewCaptureSession.setRepeatingRequest(
                                    groupCaptureRequestBuilder.build(), null,
                                    groupBackgroundHandler); // Used to be null!, 3rd parameter
                        } catch (CameraAccessException e) {
                            Log.e(TAG, "Error in accessing cameras");
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {

                    }

                    @Override
                    public void onClosed(CameraCaptureSession session) {
                        Log.i(TAG, "Close Capture Session and Image Reader");
                        if (imageReader != null) {
                            imageReader.close();
                        }
                    }
                }, null);
    }

    /**
     * Locks the focus on the live streaming camera device for setup to take the actual picture.
     * Picture will be taken immediately after.
     */
    private void lockFocus() {
        // Set our CaptureRequestBuilder to lock the focus
        groupCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        // Lock the capture state
        // STATE_WAIT_LOCK is final int = 1
        // Capture State holds the current state of the camera preview (whether its locked or not)
        captureState = STATE_WAIT_LOCK;
        // Try to capture the image
        try {
            // Put it on the background thread
            previewCaptureSession.capture(groupCaptureRequestBuilder.build(), previewCaptureCallback,
                    groupBackgroundHandler);
        }
        // Catch any accessing exceptions
        catch (CameraAccessException camAccessExcept) {
            Log.e(TAG, "Error Accessing Camera for capture()");
            camAccessExcept.printStackTrace(); // Print stack
        }
    }

    /**
     * Closes the groupCameraDevice
     */
    private void closeCamera() {
        // Check if not null pointer
        if(groupCameraDevice != null) {
            // Close camera
            groupCameraDevice.close(); // end camera process
            groupCameraDevice = null; // set it to null
        }
    }

    /**
     * initializes declared background handler thread and sets a name for it
     * starts the thread and initializes the handler using the same thread
     */
    private void startBackgroundThread() {
        //Initialize Background Handler Thread
        groupBackgroundHandlerThread = new HandlerThread("GroupCameraThread");
        //Start Thread
        groupBackgroundHandlerThread.start();
        //Initialize Background Handler using the Background Handler Thread in its Constructor
        groupBackgroundHandler = new Handler(groupBackgroundHandlerThread.getLooper());
    }

    /**
     * Safely quits and joins any started threads and sets variables back to null
     */
    private void stopBackgroundThread() {
        //Avoid errors on stopping thread by quitting safely
        groupBackgroundHandlerThread.quitSafely();
        try {
            //Join threads
            groupBackgroundHandlerThread.join();

            //Set Background handler and Handler thread to null
            groupBackgroundHandlerThread = null;
            groupBackgroundHandler = null;
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
        // Try to create a CaptureCall back
        try {
            // Use the still capture template for our capture request builder
            // Add target to be the imageReader's surface
            // Set the orientation to be portrait
            groupCaptureRequestBuilder =
                    groupCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            groupCaptureRequestBuilder.addTarget(imageReader.getSurface());
            groupCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            // Create stillCaptureCallback
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        // When capture has started, call createPhotoFileName()
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                                     @NonNull CaptureRequest request,
                                                     long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            // Try to create a unique photo file
                            try {
                                createPhotoFileName();
                            } catch (IOException e) {
                                Log.e(TAG, "Error in calling createPhotoFileName()");
                                e.printStackTrace();
                            }
                        }
                    };
            // Call capture! Give it the builder and the stillCaptureCallback
            previewCaptureSession.capture(groupCaptureRequestBuilder.build(), stillCaptureCallback,
                    null); // Already on the background thread, give thread null
        }
        catch (CameraAccessException camAccessExcept) {
            Log.e(TAG, "Error accessing camera");
            camAccessExcept.printStackTrace();
        }
    }

    /**
     * Contains photoFolder creation method and file name of the photo taken
     * @return groupPhotoFileName will be returned
     */

    //Creates toast notifying photo folder creation
    private void createPhotoFolder() {
        Toast.makeText(getApplicationContext(), "Create Photo Folder called", Toast.LENGTH_SHORT)
                .show();
        //gets external storage from public directory path (DIRECTORY_PICTURES)
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //if imageFile directory doesn't exist
        if (!imageFile.mkdirs())
            Log.e(TAG, "Directory not created");

        Toast.makeText(getApplicationContext(), "External file storage: " +
                imageFile.getName(), Toast.LENGTH_SHORT)
                .show();

        // Create folder from the abstract pathname created above (imageFile)
        groupPhotoFolder = new File(imageFile, "CameraImages");
        Toast.makeText(getApplicationContext(), "Photo folder created: " +
                groupPhotoFolder.getName(), Toast.LENGTH_SHORT)
                .show();

        //if photo folder doesn't exist
        if(!groupPhotoFolder.exists()) {

            //toast notifying of directory creation
            Toast.makeText(getApplicationContext(), "Mkdir" + groupPhotoFolder.mkdirs(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    //Method creates name of photo file, adds additional date format and timestamp.
    //if photo folder does not exist, notifies of its non existence, also creates a temp
    //file that is prepended with ".jpg" and gets the path from the photo file.
    private String createPhotoFileName()throws IOException {
        //adds a date format for the timestamp of the photo taken
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "PHOTO_" + timeStamp + "_";
        try {
            //if photo folder does not exist...
            if (!groupPhotoFolder.exists()) {
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
        // Don't create to temporary files if groupPhotoFileName already exists
        if (groupPhotoFileName == null) {
            Log.i(TAG, "File Name doesn't exist. Create it.");
            File photoFile = File.createTempFile(prepend, ".jpg", groupPhotoFolder);
            groupPhotoFileName = photoFile.getAbsolutePath();
            Log.i(TAG, groupPhotoFileName);
        }
        //return photo filename
        return groupPhotoFileName;
    }
}
