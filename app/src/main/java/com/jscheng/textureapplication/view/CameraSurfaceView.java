package com.jscheng.textureapplication.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.jscheng.textureapplication.render.CameraRender;
import com.jscheng.textureapplication.render.FliterRender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static android.content.Context.CAMERA_SERVICE;

public class CameraSurfaceView extends GLSurfaceView implements CameraRender.CameraRenderListener, View.OnClickListener{
    private static final String TAG = CameraSurfaceView.class.getSimpleName();
    private Context mContext;
    private CameraRender mRender;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private HandlerThread mCameraHandlerThread;
    private Handler mCameraHandler;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private Random mRandom;

    public CameraSurfaceView(Context context) {
        super(context);
        mContext = context;
        mRender = new CameraRender(context, this);
        mRandom = new Random(100);
        setEGLContextClientVersion(2);
        setRenderer(mRender);
        setClickable(true);
        setOnClickListener(this);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRender.getSurfaceTexture();
    }

    private void createCameraThread() {
        mCameraHandlerThread = new HandlerThread("cameraThread");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());
    }

    @Override
    public void onRequestRender() {
        this.requestRender();
    }

    @Override
    public void onCreate() {
        createCameraThread();
        setUpCamera(getWidth(), getHeight());
        openCamera();
    }

    @Override
    public void onChange() {

    }

    @Override
    public void onDraw() {

    }

    private void setUpCamera(int width, int height) {
        CameraManager manager = (CameraManager) mContext.getSystemService(CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = id;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        Log.d(TAG, "openCamera");
        CameraManager manager = (CameraManager) mContext.getSystemService(CAMERA_SERVICE);
        try {
            manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e(TAG, "cameraDeveice onDisconnected" );
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e(TAG, "cameraDeveice onError");
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    };

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    public void startPreview() {
        //设置SurfaceTexture的默认尺寸
        SurfaceTexture mSurfaceTexture = getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //根据mSurfaceTexture创建Surface
        Surface previewSurface = new Surface(mSurfaceTexture);
        try {
            //创建preview捕获请求
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //将此请求输出目标设为我们创建的Surface对象，这个Surface对象也必须添加给createCaptureSession才行
            mCaptureRequestBuilder.addTarget(previewSurface);
            //创建捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        //创建捕获请求
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        //设置重复捕获数据的请求，之后surface绑定的SurfaceTexture中就会一直有数据到达，然后就会回调SurfaceTexture.OnFrameAvailableListener接口
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

    public synchronized void setFliter(FliterRender.FliterEffect effect) {
        Toast.makeText(getContext(), effect.toString(), Toast.LENGTH_SHORT).show();
        mRender.setFliter(effect);
    }
}
