package com.jscheng.textureapplication.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jscheng.textureapplication.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class ImageActivity extends AppCompatActivity {
    private GLSurfaceView mSurfaceView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new GLSurfaceView(this);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(new ImageRender(this, R.mipmap.images));
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setContentView(mSurfaceView);
    }

    private static class ImageRender implements GLSurfaceView.Renderer {
        private static final String TAG = ImageRender.class.getSimpleName();

        private Bitmap mBitmap;
        private Context mContext;
        private int mProgram;
        private int mVertexShader;
        private int mFragmentShader;
        private boolean isInited;
        private int mVertexPosition;
        private int mTexturePosition;
        private int mTexture;

        private int mTextureId;
        private FloatBuffer mVertexPositionBuffer;
        private FloatBuffer mTexturePositionBuffer;
        private int COORD_COUNT = 5;
        // 纹理坐标
        private float[] vertexPositions = new float[] {
                -1f, 1f,
                1f, 1f,
                1f, -1f,
                -1f, -1f,
                -1f, 1f,
        };
        // 图像坐标 与 纹理坐标一一对应
        private float[] texturePositions = new float[] {
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
        };

        public ImageRender(Context context, int resouceId) {
            this.mContext = context;
            this.mBitmap = BitmapFactory.decodeResource(context.getResources(), resouceId);
            this.isInited = false;
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            int[] status = new int[1];
            mVertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(mVertexShader, VertexSource);
            GLES20.glCompileShader(mVertexShader);
            GLES20.glGetShaderiv(mVertexShader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == GLES20.GL_FALSE) {
                Log.e(TAG, "glCompileShader mVertextShader: " + GLES20.glGetShaderInfoLog(mVertexShader));
                GLES20.glDeleteShader(mVertexShader);
                return;
            }

            mFragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(mFragmentShader, FragmentSrouce);
            GLES20.glCompileShader(mFragmentShader);
            GLES20.glGetShaderiv(mFragmentShader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == GLES20.GL_FALSE) {
                Log.e(TAG, "glCompileShader mFragmentShader: " + GLES20.glGetShaderInfoLog(mFragmentShader));
                GLES20.glDeleteShader(mFragmentShader);
                return;
            }

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, mVertexShader);
            GLES20.glAttachShader(mProgram, mFragmentShader);
            GLES20.glLinkProgram(mProgram);
            GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == GLES20.GL_FALSE) {
                Log.e(TAG, "glLinkProgram " + GLES20.glGetProgramInfoLog(mProgram));
                GLES20.glDeleteProgram(mProgram);
                return;
            }

            mVertexPosition = GLES20.glGetAttribLocation(mProgram,"av_Position");
            mTexturePosition = GLES20.glGetAttribLocation(mProgram,"as_Position");
            mTexture = GLES20.glGetUniformLocation(mProgram, "u_texture");

            mVertexPositionBuffer = ByteBuffer.allocateDirect(vertexPositions.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexPositions);
            mVertexPositionBuffer.position(0);
            mTexturePositionBuffer = ByteBuffer.allocateDirect(texturePositions.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(texturePositions);
            mTexturePositionBuffer.position(0);

            int[] textures = new int[1];
            // params: 生成纹理数量 存储纹理数组 偏移量
            GLES20.glGenTextures(1, textures, 0);
            // 绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            // GL_TEXTURE_MIN_FILTER 在图像绘制时小于贴图的原始尺寸时采用
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            // GL_TEXTURE_MAG_FILTER 在图像绘制时大于贴图的原始尺寸时采用
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            // GL_TEXTURE_WRAP_S S方向(水平方向)的贴图模式 GL_CLAMP_TO_EDGE直接截断 GL_REPEAT 重复
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            // GL_TEXTURE_WRAP_T  t 方向(垂直方向)的贴图模式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            mTextureId = textures[0];
            mBitmap.recycle();
            isInited = true;
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            GLES20.glViewport(0, 0 , width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            if (!isInited) {
                return;
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);

            GLES20.glEnableVertexAttribArray(mVertexPosition);
            // size 指的是每次取几个点
            GLES20.glVertexAttribPointer(mVertexPosition, 2, GLES20.GL_FLOAT, false, 8, mVertexPositionBuffer);

            GLES20.glEnableVertexAttribArray(mTexturePosition);
            GLES20.glVertexAttribPointer(mTexturePosition, 2, GLES20.GL_FLOAT, false, 8, mTexturePositionBuffer);

            // 激活
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            GLES20.glUniform1i(mTexture, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, COORD_COUNT);
        }

        private static String VertexSource = "attribute vec4 av_Position;" +
                "attribute vec2 as_Position;" +
                "varying vec2 vs_Position;" +
                "void main() {" +
                "    vs_Position = as_Position;" +
                "    gl_Position = av_Position;" +
                "}";

        private static String FragmentSrouce = "precision mediump float; " +
                "uniform sampler2D u_texture;" +
                "varying vec2 vs_Position;" +
                "void main() {" +
                "    gl_FragColor = texture2D(u_texture, vs_Position); " +
                "}";
    }
}
