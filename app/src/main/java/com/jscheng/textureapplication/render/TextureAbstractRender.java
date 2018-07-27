package com.jscheng.textureapplication.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class TextureAbstractRender implements GLSurfaceView.Renderer {
    private static final String TAG = TextureAbstractRender.class.getSimpleName();
    protected Context mContext;
    protected int width;
    protected int height;
    protected int mVertexShader;
    protected int mFragmentShader;
    protected int mProgram;

    public TextureAbstractRender(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        createProgram();
        onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        this.height = height;
        this.width = width;
        onChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glUseProgram(mProgram);
        onDraw();
    }

    private int loadShader(int shaderType, String shaderSource) {
        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);
        int status[] = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "loadShader: compiler error");
            Log.e(TAG, "loadShader: " + GLES20.glGetShaderInfoLog(shader) );
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void createProgram() {
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexSource());
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentSource());
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, mVertexShader);
        GLES20.glAttachShader(mProgram, mFragmentShader);
        GLES20.glLinkProgram(mProgram);
        int [] status = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "createProgam: link error");
            Log.e(TAG, "createProgam: " + GLES20.glGetProgramInfoLog(mProgram));
            GLES20.glDeleteProgram(mProgram);
            return;
        }
    }

    protected abstract String getVertexSource();

    protected abstract String getFragmentSource();

    protected abstract void onChanged();

    protected abstract void onDraw();

    protected abstract void onCreate();
}
