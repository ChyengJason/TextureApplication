package com.jscheng.textureapplication.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.jscheng.textureapplication.R;

public class ImageViewActivity extends AppCompatActivity {
    private ImageView mImageView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_imageview);
        mImageView = findViewById(R.id.image_view);
        mImageView.setImageResource(R.mipmap.images);
    }
}
