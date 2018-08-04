package com.jscheng.textureapplication.activity;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jscheng.textureapplication.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SurfaceviewPreviewActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private static final int STATE_FOCUS = 1;
    private static final int STATE_PREVIEW =2;
    private Button mCaptureBtn;
    private Button mChangeBtn;
    private SurfaceView mSurfaceView;
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private HandlerThread mCameraThead;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private CameraStateCallback mCameraStateCallback;
    private CameraCaptureCallback mCaptureCallback;
    private CaptureRequest.Builder mCaptureBuilder;
    private CameraCaptureSession mCaptureSession;
    private boolean isFrontCamera;
    private int mState;
    private Size mPreviewSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_preview);
        mCaptureBtn = findViewById(R.id.surfaceview_capture);
        mChangeBtn = findViewById(R.id.surfaceview_change);
        mSurfaceView = findViewById(R.id.surfaceview);
        mCaptureBtn.setOnClickListener(this);
        mChangeBtn.setOnClickListener(this);
        isFrontCamera = false;
        initSurfaceView();
    }

    private void initSurfaceView() {
        mSurfaceView.getHolder().addCallback(this);
    }

    private void initCamera() {
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mCameraThead = new HandlerThread("cameraThread");
        mCameraThead.start();
        mCameraHandler = new Handler(mCameraThead.getLooper());
        mCameraStateCallback = new CameraStateCallback();
        mCameraDevice = null;
        mState = STATE_PREVIEW;
        mCaptureCallback = new CameraCaptureCallback();

        openCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            getCameraId(isFrontCamera);
            mSurfaceView.getHolder().setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            if (mCameraId != null) {
                mCameraManager.openCamera(mCameraId, mCameraStateCallback, mCameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void getCameraId(boolean isFront) throws CameraAccessException {

        for (String cameraId : mCameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
            boolean isFrontCamera = isFront && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
            boolean isBackCamera = !isFront && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK;
            if (isFrontCamera || isBackCamera){
                mCameraId = cameraId;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceHolder.class), mSurfaceView.getWidth(), mSurfaceView.getHeight());
            }
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private class CameraStateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    }

    private void startPreview() {
        try {
            Surface mSurface = mSurfaceView.getHolder().getSurface();
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureBuilder.addTarget(mSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        mCaptureSession = cameraCaptureSession;
                        // 自动对焦
                        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        CaptureRequest captureRequest = mCaptureBuilder.build();
                        mCaptureSession.setRepeatingRequest(captureRequest, mCaptureCallback, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mCameraHandler);
            mState = STATE_PREVIEW;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.surfaceview_change:
                switchCamera();
                break;
            case R.id.surfaceview_capture:
                takePicture();
                break;
            default:
                break;
        }
    }

    private class CameraCaptureCallback extends CameraCaptureSession.CaptureCallback {

        private void process(CaptureResult result) {
            if (mState == STATE_FOCUS) {
                capturePic();
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            process(result);
        }
    }

    private void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        try {
            // 修改状态
            mState = STATE_FOCUS;
            // 相机对焦
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void capturePic() {
        try {
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mSurfaceView.getHolder().getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            CaptureRequest captureRequest = captureBuilder.build();
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(SurfaceviewPreviewActivity.this, "拍照！", Toast.LENGTH_SHORT).show();
                    unLockFocus();
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unLockFocus() {
        try {
            mState = STATE_PREVIEW;
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
            mCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, mCameraHandler);
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void switchCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        isFrontCamera = !isFrontCamera;
        openCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

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
}
