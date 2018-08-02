package com.jscheng.textureapplication.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.jscheng.textureapplication.view.ImageSurfaceview;

public class ImageActivity extends AppCompatActivity {

    private ImageSurfaceview mSurfaceView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new ImageSurfaceview(this);
        setContentView(mSurfaceView);
    }
}
