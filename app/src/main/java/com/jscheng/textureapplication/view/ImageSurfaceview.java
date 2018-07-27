package com.jscheng.textureapplication.view;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.jscheng.textureapplication.R;
import com.jscheng.textureapplication.render.ImageRender;

public class ImageSurfaceview extends GLSurfaceView{
    private GLSurfaceView.Renderer mRender;
    public ImageSurfaceview(Context context) {
        super(context);
        mRender = new ImageRender(context, R.mipmap.images);
        setEGLContextClientVersion(2);
        setRenderer(mRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
}
