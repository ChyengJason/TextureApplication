package com.jscheng.textureapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jscheng.textureapplication.activity.CameraActivity;
import com.jscheng.textureapplication.activity.CustomViewActivity;
import com.jscheng.textureapplication.activity.FliterActivity;
import com.jscheng.textureapplication.activity.ImageActivity;
import com.jscheng.textureapplication.activity.ImageSurfaceActivity;
import com.jscheng.textureapplication.activity.ImageViewActivity;
import com.jscheng.textureapplication.activity.RecordingActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button mImageBtn;
    private Button mFliterBtn;
    private Button mCameraBtn;
    private Button mImageViewBtn;
    private Button mImageSurfaceBtn;
    private Button mCustomViewBtn;
    private Button mRecordingBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mImageBtn = findViewById(R.id.image_button);
        mFliterBtn = findViewById(R.id.fliter_button);
        mCameraBtn = findViewById(R.id.camera_button);
        mImageViewBtn = findViewById(R.id.image_view_button);
        mImageSurfaceBtn = findViewById(R.id.image_surfaceview_button);
        mCustomViewBtn = findViewById(R.id.image_custom_button);
        mRecordingBtn = findViewById(R.id.recording_button);
        mImageBtn.setOnClickListener(this);
        mFliterBtn.setOnClickListener(this);
        mCameraBtn.setOnClickListener(this);
        mImageViewBtn.setOnClickListener(this);
        mImageSurfaceBtn.setOnClickListener(this);
        mCustomViewBtn.setOnClickListener(this);
        mRecordingBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image_button:
                startActivity(new Intent(this, ImageActivity.class));
                break;
            case R.id.fliter_button:
                startActivity(new Intent(this, FliterActivity.class));
                break;
            case R.id.camera_button:
                startActivity(new Intent(this, CameraActivity.class));
                break;
            case R.id.image_view_button:
                startActivity(new Intent(this, ImageViewActivity.class));
                break;
            case R.id.image_surfaceview_button:
                startActivity(new Intent(this, ImageSurfaceActivity.class));
                break;
            case R.id.image_custom_button:
                startActivity(new Intent(this, CustomViewActivity.class));
                break;
            case R.id.recording_button:
                startActivity(new Intent(this, RecordingActivity.class));
                break;
            default:
                break;
        }
    }
}
