package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST=1888;
    ImageView myImage;
    private TextureView groupView;
    private TextureView.SurfaceTextureListener groupTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(), "It's available!", Toast.LENGTH_LONG).show();
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groupView = (TextureView)findViewById(R.id.groupView);
    }

    public void startSelfieActivity(View view) {
        Intent selfieIntent = new Intent(this, Main2Activity.class);
        startActivity(selfieIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(groupView.isAvailable()) {

        } else {
            groupView.setSurfaceTextureListener(groupTextListener);
        }
    }

    public void takePicture() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureName = getPictureName();
        File imagefile = new File(pictureName);
        Uri pictureUri = Uri.fromFile(imagefile);
        i.putExtra(MediaStore.EXTRA_OUTPUT,pictureUri);
        startActivityForResult(i, CAMERA_REQUEST);
    }

    public void takePicture2() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, 0);
    }

    private String getPictureName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        return "Plane place image" + timestamp + ".jpg";
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

}
