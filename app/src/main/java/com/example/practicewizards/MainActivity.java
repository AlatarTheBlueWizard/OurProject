package com.example.practicewizards;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import android.support.annotation.NonNull;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GroupPhoto.java";
    private File mPhotoFolder;
    private String mPhotoFileName;
    private static final int CAMERA_REQUEST=1888;
    ImageView myImage;
    private static int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    // State members
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    // Hold the state
    private int captureState = STATE_PREVIEW;

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
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
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
                fileOutputStream = new FileOutputStream(mPhotoFileName); // open file
                fileOutputStream.write(bytes); // Write the bytes to the file
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                // Close image
                Log.e(TAG, "WAHOO");
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
                        case STATE_PREVIEW: {
                            // Do nothing
                            break;
                        }
                        case STATE_WAIT_LOCK: {
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
                            }
                            break;
                        }
                    }
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    process(result);
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

    private TextureView mTextureView;
    private Button mTakeImageButton;
    private boolean mIsTaken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createPhotoFolder();
        try {
            createPhotoFileName();
        } catch (IOException e) {
            e.printStackTrace();
        }

        groupView = (TextureView)findViewById(R.id.groupView);

        mTextureView = (TextureView) findViewById(R.id.groupView);
        mTakeImageButton = (Button) findViewById(R.id.btn_takeGroup);
        mTakeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call lock focus to begin taking our picture!
                lockFocus();
            }
        });
    }

    public void startSelfieActivity(View view) {
        Intent selfieIntent = new Intent(this, Main2Activity.class);
        startActivity(selfieIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if(groupView.isAvailable()) {
            setUpCamera(groupView.getWidth(), groupView.getHeight());
            connectCamera();
        } else {
            groupView.setSurfaceTextureListener(groupTextListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Application won't run without camera services", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setUpCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                imageSize = new Size(groupView.getWidth(), groupView.getHeight());
                // image reader with group view's width, height, and maxImages is just 1
                imageReader = ImageReader.newInstance(groupView.getWidth(), groupView.getHeight(),
                        ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener,
                        groupBackgroundHandler);
                groupCameraDeviceId = cameraId; //create a parameter for this
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                try {
                    cameraManager.openCamera(groupCameraDeviceId, groupCameraDeviceStateCallback, groupBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "App requires access to camera", Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
            }
        } else {
            try {
                cameraManager.openCamera(groupCameraDeviceId, groupCameraDeviceStateCallback, groupBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void startPic() throws CameraAccessException {
        SurfaceTexture surfaceTexture = groupView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(groupView.getWidth(), groupView.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        groupCaptureRequestBuilder = groupCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        groupCaptureRequestBuilder.addTarget(previewSurface);

        groupCameraDevice.createCaptureSession(Arrays.asList(previewSurface,
                                                              imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        // Set up member
                        previewCaptureSession = session;
                        try {
                            previewCaptureSession.setRepeatingRequest(
                                    groupCaptureRequestBuilder.build(), null, null
                            );
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {

                    }
                }, null);
    }

    private void lockFocus() {
        groupCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        // Lock the capture state
        captureState = STATE_WAIT_LOCK;
        try {
            previewCaptureSession.capture(groupCaptureRequestBuilder.build(), previewCaptureCallback,
                    groupBackgroundHandler);
        }
        // Rename later
        catch (CameraAccessException error) {
            error.printStackTrace();
        }
    }

    private void closeCamera() {
        if(groupCameraDevice != null) {
            groupCameraDevice.close(); // end camera process
            groupCameraDevice = null; // set it to null
        }
    }

    private void startBackgroundThread() {
        groupBackgroundHandlerThread = new HandlerThread("GroupCameraThread");
        groupBackgroundHandlerThread.start();
        groupBackgroundHandler = new Handler(groupBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        groupBackgroundHandlerThread.quitSafely();
        try {
            groupBackgroundHandlerThread.join();
            groupBackgroundHandlerThread = null;
            groupBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    /**
     * Creates a still capture
     */
    private void startStillCapture() {
        try {
            // Use the still capture template for our capture request
            groupCaptureRequestBuilder =
                    groupCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            groupCaptureRequestBuilder.addTarget(imageReader.getSurface());
            groupCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                                     @NonNull CaptureRequest request,
                                                     long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            try {
                                createPhotoFileName();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
            previewCaptureSession.capture(groupCaptureRequestBuilder.build(), stillCaptureCallback,
                    null); // Already on the background thread, give thread null
        }
        catch (CameraAccessException camAccessExcept) {
            Log.e(TAG, "Error accessing camera");
            camAccessExcept.printStackTrace();
        }
    }

    public void takePicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder =
                    groupCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, CAMERA_REQUEST);
    }

    public void takePicture2() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, 0);
    }

    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_OK) {
            if(requestCode == CAMERA_REQUEST) {
                Bitmap b = (Bitmap)data.getExtras().get("data");
                myImage.setImageBitmap(b);
            }
        }
    }

    private void createPhotoFolder() {
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mPhotoFolder = new File(imageFile, "CameraImages");
        if(!mPhotoFolder.exists()) {
            mPhotoFolder.mkdirs();
        }
    }

    private File createPhotoFileName()throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "PHOTO_" + timeStamp + "_";
        File photoFile = File.createTempFile(prepend, ".jpg", mPhotoFolder);
        mPhotoFileName = photoFile.getAbsolutePath();
        return photoFile;
    }
}
