package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.jscheng.textureapplication.R;

public class Mp4CollectActivity extends AppCompatActivity{
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static String[] CAMERA = {Manifest.permission.CAMERA};
    private static final String TAG = Mp4CollectActivity.class.getSimpleName();
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private Button mCollectBtn;
    private TextureView mDisplayView;
    private ImageView mImageView;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private Size mPreviewSize;
    private ImageReader mImageReader;
    private MediaCodec mVideoCodec;
    private boolean isRecording;
    private Surface mVideoCodecSurface;
    private FileOutputStream mMp4FileStream;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4_collect);
        mCollectBtn = findViewById(R.id.collect_mp4);
        mDisplayView = findViewById(R.id.collect_texture);
        mImageView = findViewById(R.id.collect_iamge);
        initView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 || requestCode == 2) {
            initView();
        }
    }

    private void initView() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, CAMERA, 1);
            return;
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 2);
            return;
        }
        initImageReader(1080, 1920);
        initCodec(1920, 1080);
        mDisplayView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                initCameraHandler();
                initCamera(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });

        mCollectBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    stopRecord();
                }
                return false;
            }
        });

        mCollectBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //startRecord();
                return false;
            }
        });

        mCollectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
    }

    private void initCameraHandler() {
        mCameraThread = new HandlerThread("cameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private void initImageReader(int width, int height) {
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG,1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { //可以在这里处理拍照得到的临时照片 例如，写入本地
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.e(TAG, "onImageAvailable: " + Thread.currentThread().getName());
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes); //由缓冲区存入字节数组
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageView.setImageBitmap(bitmap);
                        }
                    });
                    image.close();
                }
            }
        }, mCameraHandler);
    }

    @SuppressLint("MissingPermission")
    private void initCamera(int width, int height) {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = id;
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    break;
                }
            }
//            initImageReader(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            initCodec(1280, 720);
            if (!mCameraId.isEmpty()) {
                cameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        mCameraDevice = cameraDevice;
                        initCaptureSession();
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
                }, mCameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initCaptureSession() {
        try {
            mDisplayView.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface textureSurface = new Surface(mDisplayView.getSurfaceTexture());
            mCameraDevice.createCaptureSession(Arrays.asList(textureSurface, mImageReader.getSurface(), mVideoCodecSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    startPreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {}
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        if (mCameraDevice == null) {
            return;
        }
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface textureSurface = new Surface(mDisplayView.getSurfaceTexture());
            captureBuilder.addTarget(textureSurface);
            assert (mVideoCodecSurface == null || mVideoCodecSurface.isValid());
            captureBuilder.addTarget(mVideoCodecSurface);
            // 自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            CaptureRequest captureRequest = captureBuilder.build();
            mCaptureSession.setRepeatingRequest(captureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (mCameraDevice == null) {
            return;
        }
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //拍照
            CaptureRequest mCaptureRequest = captureBuilder.build();
            mCaptureSession.capture(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initCodec(int width, int height) {
        try {
            isRecording = false;
            mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            // 1280x960, 1280x720, 640x480, 352x288, 320x240, 176x144
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);// 比特率 码率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 设置关键帧间隔的秒数
            mVideoCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) { }
                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
                    if (isRecording && mMp4FileStream != null && i >= 0) {
                        ByteBuffer outPutBuffer = mediaCodec.getOutputBuffer(i);
                        byte[] outData = new byte[bufferInfo.size];
                        outPutBuffer.get(outData);
                        try {
                            mMp4FileStream.write(outData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mediaCodec.releaseOutputBuffer(i, false);
                    } else {
                        stopCodec();
                    }
                }
                @Override
                public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                    if (isRecording) {
                        isRecording = false;
                        stopCodec();
                    }
                }
                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) { }
            });

            mVideoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoCodecSurface = mVideoCodec.createInputSurface();
            assert (mVideoCodecSurface == null || mVideoCodecSurface.isValid());
            mVideoCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCodec(){
        if (!isRecording) {
            return;
        }
        try {
            if (mVideoCodec != null) {
                mVideoCodec.stop();
                mVideoCodec.reset();
            }
            if (mMp4FileStream != null) {
                mMp4FileStream.flush();
                mMp4FileStream.close();
                mMp4FileStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "record_mp4 完成", Toast.LENGTH_SHORT).show();
    }

    private void startRecord() {
        try {
            isRecording = true;
            mVideoCodec.start();
            mMp4FileStream = new FileOutputStream(getSDPath() + "/record_mp4.h264");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void stopRecord() {
        isRecording = false;
    }

    public String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoCodec != null) {
            mVideoCodec.release();
            mVideoCodec = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
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
