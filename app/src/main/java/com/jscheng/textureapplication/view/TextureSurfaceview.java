package com.jscheng.textureapplication.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jscheng.textureapplication.R;
import com.jscheng.textureapplication.render.FliterRender;

import java.util.Random;

public class TextureSurfaceview extends GLSurfaceView implements View.OnClickListener {
    private FliterRender mRender;
    private Random mRandom;
    public TextureSurfaceview(Context context) {
        super(context);
        mRender = new FliterRender(context, FliterRender.FliterEffect.GRAY, R.mipmap.images);
        mRandom = new Random(100);
        setEGLContextClientVersion(2);
        setRenderer(mRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setClickable(true);
        setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Log.w("TextTureSurfaceView", "onClick: " );
        int rint = mRandom.nextInt(4);
        FliterRender.FliterEffect effect;
        switch (rint) {
            case 0:
                effect = FliterRender.FliterEffect.GRAY;
                break;
            case 1:
                effect = FliterRender.FliterEffect.BLUE;
                break;
            case 2:
                effect = FliterRender.FliterEffect.WRAM;
                break;
            default:
                effect = FliterRender.FliterEffect.NONE;
                break;
        }
        setFliter(effect);
    }

    public void setFliter(FliterRender.FliterEffect effect) {
        Toast.makeText(getContext(), effect.toString(), Toast.LENGTH_SHORT).show();
        mRender.setFliter(effect);
        this.requestRender();
    }
}
