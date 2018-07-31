package com.jscheng.textureapplication.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraRender extends TextureAbstractRender {
    private static final String TAG = CameraRender.class.getSimpleName();
    private Context mContext;
    private FliterRender.FliterEffect mFliterEffect;
    private int mTexture;
    private SurfaceTexture mSurfaceTexture;
    private CameraRenderListener mCameraRenderListener;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;

    private int avPosition;
    private int afPosition;
    private int vTexture;
    private int vChangeColor;
    private int vType;

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

    private static final int COORDS_PER_VERTEX = 3; // 每一次取点的时候取几个点
    private final int vertexCount = vertexData.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每一次取的总的点大小

    public interface CameraRenderListener {
        void onRequestRender();
        void onCreate();
        void onChange();
        void onDraw();
    }

    public CameraRender(Context context, CameraRenderListener listener) {
        super(context);
        this.mContext = context;
        this.mCameraRenderListener = listener;
        this.mFliterEffect = FliterRender.FliterEffect.GRAY;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    protected void onCreate() {

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

        mTexture = loadExternelTexture();

        avPosition = GLES20.glGetAttribLocation(mProgram, "av_Position");
        afPosition = GLES20.glGetAttribLocation(mProgram, "af_Position");
        vTexture = GLES20.glGetUniformLocation(mProgram, "v_texture");
        vType = GLES20.glGetUniformLocation(mProgram, "v_Type");
        vChangeColor = GLES20.glGetUniformLocation(mProgram, "v_ChangeColor");

        mSurfaceTexture = new SurfaceTexture(mTexture);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mCameraRenderListener.onRequestRender();
            }
        });

        mCameraRenderListener.onCreate();
    }

    @Override
    protected void onChanged() {
        mCameraRenderListener.onChange();
    }

    @Override
    protected void onDraw() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
        }

        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);

        //设置顶点位置值
        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        //设置纹理位置值
        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glUniform1i(vTexture, 0);
        GLES20.glUniform3fv(vChangeColor, 1, getFliterColor(), 0);
        GLES20.glUniform1i(vType, getFliterType());
        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);

        mCameraRenderListener.onDraw();
    }
    @Override
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_TextureCoord; " +
                "void main() { " +
                "    v_TextureCoord = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        String source = "#extension GL_OES_EGL_image_external : require\n " +
                "precision mediump float; " +
                "uniform int v_Type;" +
                "uniform vec3 v_ChangeColor; " +
                "varying vec2 v_TextureCoord; " +
                "uniform samplerExternalOES v_texture; " +
                "void modifyColor(vec4 color) { " +
                "    color.r = max(min(color.r,1.0), 0.0); " +
                "    color.g = max(min(color.g,1.0), 0.0); " +
                "    color.b = max(min(color.b,1.0), 0.0); " +
                "    color.a = max(min(color.a,1.0), 0.0); " +
                "} " +
                "void main() { " +
                "    vec4 nColor = texture2D(v_texture, v_TextureCoord); " +
                "    if (v_Type == 0 ) { " +
                "       float c = nColor.r * v_ChangeColor.r + nColor.g * v_ChangeColor.g + nColor.b * v_ChangeColor.b;" +
                "       gl_FragColor = vec4(c, c, c, nColor.a);" +
                "    } else {" +
                "       vec4 deltaColor = nColor + vec4(v_ChangeColor , 0.0);" +
                "       modifyColor(deltaColor); " +
                "       gl_FragColor = deltaColor;" +
//                "      gl_FragColor = texture2D(v_texture, v_TextureCoord);" +
                "    } " +
                "}";
        return source;
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

    public synchronized void setFliter(FliterRender.FliterEffect fliter) {
        this.mFliterEffect = fliter;
    }

    public enum FliterEffect {
        GRAY,
        WRAM,
        BLUE,
        NONE,
    };
}
