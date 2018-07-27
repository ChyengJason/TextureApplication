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

public class FliterRender extends TextureAbstractRender {
    private static final String TAG = FliterRender.class.getSimpleName();
    private float vertexData[] = {   // in counterclockwise order:
            -1f, -1f, 0.0f, // bottom left
            1f, -1f, 0.0f, // bottom right
            -1f, 1f, 0.0f, // top left
            1f, 1f, 0.0f,  // top right
    };

    // 纹理坐标对应顶点坐标与之映射
    private float textureData[] = {   // in counterclockwise order:
            0f, 1f, 0.0f, // bottom left
            1f, 1f, 0.0f, // bottom right
            0f, 0f, 0.0f, // top left
            1f, 0f, 0.0f,  // top right
    };

    private Bitmap mBitmap;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private FliterEffect mFliterEffect;
    private int avPosition;
    private int afPosition;
    private int vMatrix;
    private int vChangeColor;
    private int vType;
    private int mTexture;
    private float[] mProjectMatrix;
    private float[] mViewMatrix;
    private float[] mResultMatrix;
    private static final int COORDS_PER_VERTEX = 3; // 每一次取点的时候取几个点
    private final int vertexCount = vertexData.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每一次取的总的点大小

    public FliterRender(Context context, FliterEffect mFliterEffect, int resourceId) {
        super(context);
        this.mFliterEffect = mFliterEffect;
        this.mBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        assert (mBitmap == null);
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

        createTexture();

        avPosition = GLES20.glGetAttribLocation(mProgram, "avPosition");
        afPosition = GLES20.glGetAttribLocation(mProgram, "afPosition");
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        vChangeColor = GLES20.glGetUniformLocation(mProgram, "vChangeColor");
        vType = GLES20.glGetUniformLocation(mProgram, "vType");
    }

    @Override
    protected void onChanged() {
        int w=mBitmap.getWidth();
        int h=mBitmap.getHeight();
        float sWH=w/(float)h;
        float sWidthHeight=width/(float)height;
        if(width>height){
            if(sWH>sWidthHeight){
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight*sWH,sWidthHeight*sWH, -1,1, 3, 7);
            }else{
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight/sWH,sWidthHeight/sWH, -1,1, 3, 7);
            }
        }else{
            if(sWH>sWidthHeight){
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
        //GLES20.glEnableVertexAttribArray(vChangeColor);

        //设置顶点位置值
        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        //设置纹理位置值
        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureBuffer);

        GLES20.glUniformMatrix4fv(vMatrix,1,false, mResultMatrix,0);

        GLES20.glUniform3fv(vChangeColor, 1, getFliterColor(), 0);

        GLES20.glUniform1i(vType, getFliterType());

        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);
    }

    private float[] getFliterColor() {
        switch (mFliterEffect) {
            case GRAY:
                return new float[]{0.299f, 0.587f, 0.114f};
            case WRAM:
                return new float[]{0.1f, 0.1f, 0.0f};
            case BLUE:
                return new float[]{0.006f, 0.004f, 0.002f};
            default:
                return new float[]{0.0f, 0.0f, 0.0f};
        }
    }

    private int getFliterType() {
        switch (mFliterEffect) {
            case GRAY:
                return 0;
            case WRAM:
                return 1;
            case BLUE:
                return 2;
            default:
                return 3;
        }
    }

    @Override
    protected String getVertexSource() {
        final String source =
                "attribute vec4 avPosition; " +
                        "attribute vec2 afPosition; " +
                        "varying vec2 aCoordinate; " +
                        "uniform mat4 vMatrix; " +
                        "void main() { " +
                        "    aCoordinate = afPosition; " +
                        "    gl_Position = vMatrix * avPosition; " +
                        "}";
        return source;
    }

    /**
     * 黑白滤镜
     * @return
     */
    @Override
    protected String getFragmentSource() {
        final String source =
                "precision mediump float; " +
                        "uniform int vType;" +
                        "uniform sampler2D vTexture; " +
                        "uniform vec3 vChangeColor; " +
                        "varying vec2 aCoordinate; " +
                        "void modifyColor(vec4 color) { " +
                        "    color.r = max(min(color.r,1.0), 0.0); " +
                        "    color.g = max(min(color.g,1.0), 0.0); " +
                        "    color.b = max(min(color.b,1.0), 0.0); " +
                        "    color.a = max(min(color.a,1.0), 0.0); " +
                        "} " +
                        "void main() {\n" +
                        "    vec4 nColor = texture2D(vTexture, aCoordinate); " +
                        "    if (vType == 0 ) { " +
                        "       vec4 nColor = texture2D(vTexture, aCoordinate); " +
                        "       float c = nColor.r * vChangeColor.r + nColor.g * vChangeColor.g + nColor.b * vChangeColor.b;" +
                        "       gl_FragColor = vec4(c, c, c, nColor.a);" +
                        "    } else { " +
                        "       vec4 deltaColor=nColor + vec4(vChangeColor , 0.0);" +
                        "       modifyColor(deltaColor); " +
                        "       gl_FragColor=deltaColor;" +
                        "    } " +
                        "} ";
        return source;
    }

    private void createTexture() {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        if (textureIds[0] == 0) {
            return;
        }
        mTexture = textureIds[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
    }

    public void setFliter(FliterEffect fliter) {
        this.mFliterEffect = fliter;
    }

    public enum FliterEffect {
        GRAY,
        WRAM,
        BLUE,
        NONE,
    };
}
