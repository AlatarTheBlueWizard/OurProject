package com.example.practicewizards;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

public class Main2Activity extends AppCompatActivity {
    private TextureView selfieView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Intent selfieIntent = getIntent();
    }
}
