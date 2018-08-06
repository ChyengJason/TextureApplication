package com.jscheng.textureapplication.activity;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.jscheng.textureapplication.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TextureviewPreviewActivity extends AppCompatActivity {
    private TextureView mTextureView;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private String mCameraId;
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;
    private CameraDevice.StateCallback mDeviceCallback;
    private CaptureRequest.Builder mCapterBuider;
    private CameraCaptureSession mCapterSeesion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_preview);
        mTextureView = findViewById(R.id.preview_textureview);
        initTextureView();
    }

    private void initTextureView() {
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                initCamera(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                stopPreview();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }

    @SuppressLint("MissingPermission")
    private void initCamera(int width, int height) {
        try {
            mCameraThread = new HandlerThread("texturePreviewThread");
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
            CameraManager cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                }
            }
            mDeviceCallback = new CameraDevice.StateCallback() {
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
            };
            cameraManager.openCamera(mCameraId, mDeviceCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        if (mCameraDevice != null) {
            mTextureView.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            try {
                mCapterBuider = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Surface surface = new Surface(mTextureView.getSurfaceTexture());
                mCapterBuider.addTarget(surface);
                mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        try {
                            mCapterSeesion = cameraCaptureSession;
                            mCapterSeesion.setRepeatingRequest(mCapterBuider.build(), null, mCameraHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        stopPreview();
                    }
                }, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopPreview() {
        if (mCapterSeesion != null) {
            mCapterSeesion.close();
            mCapterSeesion = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
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
