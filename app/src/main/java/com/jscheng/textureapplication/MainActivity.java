package com.jscheng.textureapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button mImageBtn;
    private Button mFliterBtn;
    private Button mCameraBtn;

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
        mImageBtn.setClickable(true);
        mImageBtn.setOnClickListener(this);
        mFliterBtn.setClickable(true);
        mFliterBtn.setOnClickListener(this);
        mCameraBtn.setClickable(true);
        mCameraBtn.setOnClickListener(this);
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
            default:
                break;
        }
    }
}
