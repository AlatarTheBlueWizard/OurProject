package com.example.practicewizards;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Toast;

public class Main2Activity extends AppCompatActivity {
    private TextureView selfieView;
    private TextureView.SurfaceTextureListener selfieTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(),"It's Available!", Toast.LENGTH_LONG);
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
        setContentView(R.layout.activity_main2);

        Intent selfieIntent = getIntent();
        selfieView = (TextureView) findViewById(R.id.selfieView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (selfieView.isAvailable()) {

        } else {
            selfieView.setSurfaceTextureListener(selfieTextListener);
        }
    }
}
