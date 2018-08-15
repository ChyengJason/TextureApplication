package com.jscheng.textureapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jscheng.textureapplication.activity.AacRecordActivity;
import com.jscheng.textureapplication.activity.AacTrackActivity;
import com.jscheng.textureapplication.activity.CameraActivity;
import com.jscheng.textureapplication.activity.CameraPreviewActivity;
import com.jscheng.textureapplication.activity.CustomViewActivity;
import com.jscheng.textureapplication.activity.FliterActivity;
import com.jscheng.textureapplication.activity.ImageActivity;
import com.jscheng.textureapplication.activity.ImageSurfaceActivity;
import com.jscheng.textureapplication.activity.ImageViewActivity;
import com.jscheng.textureapplication.activity.MediaExtractorActivity;
import com.jscheng.textureapplication.activity.RecordingActivity;
import com.jscheng.textureapplication.activity.TriangleActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        findViewById(R.id.image_button).setOnClickListener(this);
        findViewById(R.id.fliter_button).setOnClickListener(this);
        findViewById(R.id.camera_button).setOnClickListener(this);
        findViewById(R.id.image_view_button).setOnClickListener(this);
        findViewById(R.id.image_surfaceview_button).setOnClickListener(this);
        findViewById(R.id.image_custom_button).setOnClickListener(this);
        findViewById(R.id.recording_button).setOnClickListener(this);
        findViewById(R.id.camera_preview_button).setOnClickListener(this);
        findViewById(R.id.media_extracotr_btn).setOnClickListener(this);
        findViewById(R.id.triangle_button).setOnClickListener(this);
        findViewById(R.id.aac_button).setOnClickListener(this);
        findViewById(R.id.aac_play_button).setOnClickListener(this);
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
            case R.id.camera_preview_button:
                startActivity(new Intent(this, CameraPreviewActivity.class));
                break;
            case R.id.media_extracotr_btn:
                startActivity(new Intent(this, MediaExtractorActivity.class));
                break;
            case R.id.triangle_button:
                startActivity(new Intent(this, TriangleActivity.class));
                break;
            case R.id.aac_button:
                startActivity(new Intent(this, AacRecordActivity.class));
                break;
            case R.id.aac_play_button:
                startActivity(new Intent(this, AacTrackActivity.class));
            default:
                break;
        }
    }
}
