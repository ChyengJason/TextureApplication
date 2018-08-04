package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.jscheng.textureapplication.R;

public class CameraPreviewActivity extends AppCompatActivity implements View.OnClickListener {
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static String[] CAMERA = {Manifest.permission.CAMERA};
    private Button mSurfaceviewBtn;
    private Button mTextureViewBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        mSurfaceviewBtn = findViewById(R.id.surfaceview_btn);
        mTextureViewBtn = findViewById(R.id.textureview_btn);
        initView();
    }

    private void initView() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, CAMERA, 1);
            return;
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 2);
            return;
        }
        mSurfaceviewBtn.setOnClickListener(this);
        mTextureViewBtn.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 || requestCode == 2) {
            initView();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.surfaceview_btn:
                startActivity(new Intent(this, SurfaceviewPreviewActivity.class));
                break;
            case R.id.textureview_btn:
                startActivity(new Intent(this, TextureviewPreviewActivity.class));
                break;
        }
    }
}
