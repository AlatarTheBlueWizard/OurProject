package com.example.practicewizards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST=1888;
    ImageView myImage;
    ImageView myImage2;
    Button button;
    Button button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myImage = (ImageView)findViewById(R.id.myImage);
        myImage2 = (ImageView)findViewById(R.id.myImage2);

        button = (Button)findViewById(R.id.button);
        button2 = (Button)findViewById(R.id.button2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
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
