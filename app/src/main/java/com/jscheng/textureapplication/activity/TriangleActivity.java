package com.jscheng.textureapplication.activity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TriangleActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(new TriangleRender());
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setContentView(mGLSurfaceView);
    }

    private static class TriangleRender implements GLSurfaceView.Renderer {
        private final static String TAG = TriangleRender.class.getSimpleName();
        private int width, height;
        private int mProgram;
        private int mVertexShader;
        private int mFragmentShader;
        private int mAvPosition;
        private int mAfColor;
        private FloatBuffer mVertextPostionBuffer;
        private FloatBuffer mFragmentColorBuffer;
        private boolean isInited;

        private float[] mVertextPosition = {
                -0.5f, 0.5f, 0f,
                0.5f, 0.5f, 0f,
                0f, -0.5f, 0f
        };

        private float[] mFragmentColor = {
                0.0f, 1.0f, 0.0f, 1.0f ,
                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f
        };

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            isInited = false;
            mVertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(mVertexShader, VertexSource);
            GLES20.glCompileShader(mVertexShader);
            int[] status = new int[1];
            GLES20.glGetShaderiv(mVertexShader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == GLES20.GL_FALSE) {
                String errorLog = GLES20.glGetShaderInfoLog(mVertexShader);
                Log.e(TAG, "compileVertexShader: " + errorLog);
                GLES20.glDeleteShader(mVertexShader);
                return;
            }

            mFragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(mFragmentShader, FragmentSource);
            GLES20.glCompileShader(mFragmentShader);
            GLES20.glGetShaderiv(mFragmentShader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == GLES20.GL_FALSE) {
                String errorLog = GLES20.glGetShaderInfoLog(mFragmentShader);
                Log.e(TAG, "compileFragmentShader: " + errorLog);
                GLES20.glDeleteShader(mFragmentShader);
                return;
            }

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, mVertexShader);
            GLES20.glAttachShader(mProgram, mFragmentShader);
            GLES20.glLinkProgram(mProgram);
            GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == GLES20.GL_FALSE){
                String errorLog = GLES20.glGetProgramInfoLog(mProgram);
                Log.e(TAG, "onSurfaceCreated: " + errorLog);
                GLES20.glDeleteProgram(mProgram);
                return;
            }

            mAvPosition = GLES20.glGetAttribLocation(mProgram, "av_Position");
            mAfColor = GLES20.glGetAttribLocation(mProgram, "a_color");
            mVertextPostionBuffer = ByteBuffer.allocateDirect(mVertextPosition.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mVertextPostionBuffer.put(mVertextPosition);
            mVertextPostionBuffer.position(0);
            mFragmentColorBuffer = ByteBuffer.allocateDirect(mFragmentColor.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mFragmentColorBuffer.put(mFragmentColor);
            mFragmentColorBuffer.position(0);
            isInited = true;
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            this.width = width;
            this.height = height;
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            if (!isInited) {
                return;
            }
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mAvPosition);
            GLES20.glEnableVertexAttribArray(mAfColor);
            GLES20.glVertexAttribPointer(mAvPosition, 3, GLES20.GL_FLOAT, false, 12, mVertextPostionBuffer);
            GLES20.glVertexAttribPointer(mAfColor, 3, GLES20.GL_FLOAT, false, 16, mFragmentColorBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 3);
        }

        private final String VertexSource = "attribute vec4 av_Position;" +
                "attribute vec4 a_color;" +
                "varying vec4 v_color;" +
                "void main() {" +
                "    v_color = a_color;" +
                "    gl_Position = av_Position;" +
                "}";

        private final String FragmentSource = "precision mediump float; " +
                "varying vec4 v_color;" +
                "void main() {" +
                "    gl_FragColor = v_color;" +
                "}";
    }

}
