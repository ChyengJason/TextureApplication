package com.jscheng.textureapplication.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ImageRender extends TextureAbstractRender {
    private static final String TAG = TextureAbstractRender.class.getSimpleName();
    private int mResourceId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private int avPosition;
    private int afPosition;
    private int vMatrix;
    private int sTexture;
    private float[] mProjectMatrix;
    private float[] mViewMatrix;
    private float[] mResultMatrix;
    private static final int COORDS_PER_VERTEX = 3; // 每一次取点的时候取几个点
    private final int vertexCount = vertexData.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每一次取的总的点大小
    private int mTexture;

    static float vertexData[] = {   // in counterclockwise order:
            -1f, -1f, 0.0f, // bottom left
            1f, -1f, 0.0f, // bottom right
            -1f, 1f, 0.0f, // top left
            1f, 1f, 0.0f,  // top right
    };

    // 纹理坐标对应顶点坐标与之映射
    static float textureData[] = {   // in counterclockwise order:
            0f, 1f, 0.0f, // bottom left
            1f, 1f, 0.0f, // bottom right
            0f, 0f, 0.0f, // top left
            1f, 0f, 0.0f,  // top right
    };

    public ImageRender(Context context, int resourceId) {
        super(context);
        this.mResourceId = resourceId;
    }

    @Override
    protected void onCreate() {
        mProjectMatrix = new float[16];
        mViewMatrix = new float[16];
        mResultMatrix = new float[16];

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);

        mTexture = loadTexture(mResourceId);
        avPosition = GLES20.glGetAttribLocation(mProgram, "av_Position");
        afPosition = GLES20.glGetAttribLocation(mProgram, "af_Position");
        vMatrix = GLES20.glGetUniformLocation(mProgram, "v_matrix");
        sTexture = GLES20.glGetUniformLocation(mProgram, "sTexture");
    }

    @Override
    protected void onChanged() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mResourceId);
        assert (bitmap == null);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float sWH = w / (float)h;
        float sWidthHeight = width / (float)height;
        if(width > height){
            if(sWH > sWidthHeight){
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight*sWH,sWidthHeight*sWH, -1,1, 3, 7);
            }else{
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight/sWH,sWidthHeight/sWH, -1,1, 3, 7);
            }
        }else{
            if(sWH > sWidthHeight){
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1/sWidthHeight*sWH, 1/sWidthHeight*sWH,3, 7);
            }else{
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -sWH/sWidthHeight, sWH/sWidthHeight,3, 7);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mResultMatrix,0,mProjectMatrix,0,mViewMatrix,0);
    }

    @Override
    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);
        GLES20.glEnableVertexAttribArray(vMatrix);
        //设置顶点位置值
        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        //设置纹理位置值
        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureBuffer);

        GLES20.glUniformMatrix4fv(vMatrix,1,false, mResultMatrix,0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glUniform1i(sTexture, 0);
        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);
    }

    @Override
    protected String getVertexSource() {
        final String source =
                "attribute vec4 av_Position; " +
                        "attribute vec2 af_Position; " +
                        "varying vec2 v_texPo; " +
                        "uniform mat4 v_matrix; " +
                        "void main() { " +
                        "    v_texPo = af_Position; " +
                        "    gl_Position = v_matrix * av_Position; " +
                        "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        final String source =
                "precision mediump float; " +
                        "varying vec2 v_texPo; " +
                        "uniform sampler2D sTexture; " +
                        "void main() { " +
                        "   gl_FragColor = texture2D(sTexture, v_texPo); " +
                        "} ";
        return source;
    }
}
