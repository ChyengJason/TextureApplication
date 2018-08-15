package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import com.jscheng.textureapplication.R;

public class AacRecordActivity extends AppCompatActivity{
    private final static String TAG = AacRecordActivity.class.getSimpleName();
    public static String[] MICROPHONE = {Manifest.permission.RECORD_AUDIO};
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Button mRecordBtn;
    private MyAudioRecorder mAudioRecorder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac);
        mRecordBtn = findViewById(R.id.aac_recording_btn);
        mAudioRecorder = new MyAudioRecorder();
        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    checkRecordPermission();
                    mRecordBtn.setText("正在录制");
                    startRecord();
                } else if (action == MotionEvent.ACTION_UP) {
                    mRecordBtn.setText("录音");
                    stopRecord();
                }
                return false;
            }
        });
    }

    private void startRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecorder.start();
            }
        }).start();
    }

    private void stopRecord() {
        mAudioRecorder.stop();
    }

    private void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, MICROPHONE, 1);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 1);
            return;
        }
    }

    private class MyAudioRecorder {
        private AudioRecord mRecorder;
        private MediaCodec mEncoder;
        private OutputStream mFileStream;
        private boolean isRecording;

        public void start() {
            try {
                isRecording = true;
                int minBufferSize = AudioRecord.getMinBufferSize(44100, 1, AudioFormat.ENCODING_PCM_16BIT);
                mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100,  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
                if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "initRecord: mAudioRecord init failed");
                    isRecording = false;
                    return;
                }
                mRecorder.startRecording();

                mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
                format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                //比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
                format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
                //传入的数据大小
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
                mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mEncoder.start();

                mFileStream = new FileOutputStream(getSDPath() + "/acc_encode.mp4");

                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                byte[] bytesBuffer = new byte[minBufferSize];
                int len = 0;
                while (isRecording && (len = mRecorder.read(bytesBuffer, 0, minBufferSize)) > 0) {
                    int inputBufferIndex = mEncoder.dequeueInputBuffer(100000);
                    if (inputBufferIndex >=0 ) {
                        ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(bytesBuffer);
                        inputBuffer.limit(len);
                        mEncoder.queueInputBuffer(inputBufferIndex, 0, len, System.nanoTime(), 0);
                    } else {
                        isRecording = false;
                    }

                    int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        int outBitsSize = mBufferInfo.size;
                        int outPacketSize = outBitsSize + 7; // ADTS头部是7个字节
                        ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
                        outputBuffer.position(mBufferInfo.offset);
                        outputBuffer.limit(mBufferInfo.offset + outBitsSize);

                        byte[] outData = new byte[outPacketSize];
                        addADTStoPacket(outData, outPacketSize);

                        outputBuffer.get(outData, 7, outBitsSize);
                        outputBuffer.position(mBufferInfo.offset);
                        mFileStream.write(outData);
                        mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                    }
                }
                release();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void stop() {
            isRecording = false;
        }

        private void release() throws IOException {
            if (mFileStream != null) {
                mFileStream.flush();
                mFileStream.close();
            }
            if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
            if (mEncoder != null) {
                mEncoder.stop();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AacRecordActivity.this, "录制完毕", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44100
        int chanCfg = 2;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
