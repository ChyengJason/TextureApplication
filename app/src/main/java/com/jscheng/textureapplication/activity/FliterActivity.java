package com.jscheng.textureapplication.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.jscheng.textureapplication.view.TextureSurfaceview;

public class FliterActivity extends AppCompatActivity {

    private TextureSurfaceview mSurfaceView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new TextureSurfaceview(this);
        setContentView(mSurfaceView);
    }
}
