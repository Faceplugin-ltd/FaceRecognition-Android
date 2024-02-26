package com.faceplugin.facerecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent =new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        },MyGlobal.splash_delay_time);

//        Intent intent =new Intent(SplashActivity.this, ChooseActivity.class);
//        startActivity(intent);
//        finish();

    }
}