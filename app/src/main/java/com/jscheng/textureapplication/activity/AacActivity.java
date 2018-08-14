package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.jscheng.textureapplication.R;

public class AacActivity extends AppCompatActivity{
    private final static String TAG = AacActivity.class.getSimpleName();
    public static String[] MICROPHONE = {Manifest.permission.RECORD_AUDIO};
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Button mRecordBtn;
    private Button mPlayBtn;
    private ExecutorService mExecutor;
    private MyAudioRecorder mAudioRecorder;
    private MyAudioTrack mAudioTrack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac);

        checkRecordPermission();

        mExecutor = new ThreadPoolExecutor(1, 5, 2000L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(1024));
        mRecordBtn = findViewById(R.id.aac_recording_btn);
        mPlayBtn = findViewById(R.id.aac_playing_btn);
        mAudioRecorder = new MyAudioRecorder(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new MyAudioTrack();
        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    checkRecordPermission();
                    mRecordBtn.setTag(true);
                    mRecordBtn.setText("正在录制");
                    startRecord();
                } else if (action == MotionEvent.ACTION_UP) {
                    mRecordBtn.setTag(false);
                    mRecordBtn.setText("录音");
                    stopRecord();
                }
                return false;
            }
        });

        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        checkRecordPermission();
                        mAudioTrack.start();
                    }
                });
            }
        });
    }

    private void startRecord() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mAudioRecorder.start();
            }
        });
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
        private int audioSource;
        private int samplerRate;
        private int channelCofig;
        private int audioFormat;
        private MyAccEncode mEncoder;

        public MyAudioRecorder(int audioSource, int samplerRate, int channelCofig, int audioFormat) {
                this.audioSource = audioSource;
                this.samplerRate = samplerRate;
                this.channelCofig = channelCofig;
                this.audioFormat = audioFormat;
                this.mEncoder = new MyAccEncode();
        }

        public void start() {
            try {
                OutputStream mOutFile = new FileOutputStream(getSDPath() + "/acc_encode.mp4");
                int minBufferSize = getMinBufferSize();
                mRecorder = new AudioRecord(audioSource, samplerRate, channelCofig, audioFormat, minBufferSize);
                mEncoder.start(samplerRate, 2, 96000, minBufferSize * 2);
                byte[] bufferbytes = new byte[minBufferSize];
                if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "initRecord: mAudioRecord init failed");
                }
                mRecorder.startRecording();
                int len = 0;
                while ((len = mRecorder.read(bufferbytes, 0, minBufferSize)) > 0) {
                     mOutFile.write(mEncoder.encode(bufferbytes));
                }
                mOutFile.flush();
                mOutFile.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void stop() {
            if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
            if (mEncoder != null) {
                mEncoder.stop();
            }
        }

        private int getMinBufferSize() {
            return AudioRecord.getMinBufferSize(samplerRate, channelCofig, audioFormat);
        }
    }

    private class MyAccEncode {
        private MediaCodec mMediaEncoder;
        private MediaCodec.BufferInfo mBufferInfo;
        private ByteArrayOutputStream mOutputStream;

        public MyAccEncode() {

        }

        // samplerRate: 44100, 22050, 16000, 11025
        // keyBitRate: 64000, 96000, 128000
        public void start(int samplerRate, int channelConfig, int keyBitRate, int bufferSize) {
            try {
                mOutputStream = new ByteArrayOutputStream();
                mBufferInfo = new MediaCodec.BufferInfo();
                mMediaEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, samplerRate, channelConfig);
                format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                //比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
                format.setInteger(MediaFormat.KEY_BIT_RATE, keyBitRate);
                //传入的数据大小
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
                mMediaEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mMediaEncoder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public synchronized byte[] encode(byte[] data) throws IOException {
            Log.e(TAG, "encode: " + data.length);
            if (mMediaEncoder == null) {
                return null;
            }
            //-1代表一直等待，0代表不等待。此处单位为微秒
            int inputBufferIndex = mMediaEncoder.dequeueInputBuffer(100000);
            if (inputBufferIndex >=0 ) {
                ByteBuffer inputBuffer = mMediaEncoder.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(data);
                inputBuffer.limit(data.length);
                mMediaEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, System.nanoTime(), 0);
            }

            int outputBufferIndex = mMediaEncoder.dequeueOutputBuffer(mBufferInfo, 0);
            while (outputBufferIndex >= 0) {
                int outBitsSize = mBufferInfo.size;
                int outPacketSize = outBitsSize + 7; // ADTS头部是7个字节
                ByteBuffer outputBuffer = mMediaEncoder.getOutputBuffer(outputBufferIndex);
                outputBuffer.position(mBufferInfo.offset);
                outputBuffer.limit(mBufferInfo.offset + outBitsSize);

                byte[] outData = new byte[outPacketSize];
                addADTStoPacket(outData, outPacketSize);

                outputBuffer.get(outData, 7, outBitsSize);
                outputBuffer.position(mBufferInfo.offset);
                mOutputStream.write(outData);
                mMediaEncoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mMediaEncoder.dequeueOutputBuffer(mBufferInfo, 0);
            }
            byte[]  result = mOutputStream.toByteArray();
            mOutputStream.flush();
            mOutputStream.reset();
            return  result;
        }

        public synchronized void stop() {
            try {
                if (mMediaEncoder != null) {
                    mMediaEncoder.stop();
                    mMediaEncoder.release();
                }
                if (mOutputStream != null) {
                    mOutputStream.flush();
                    mOutputStream.close();
                }
                AacActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AacActivity.this, "AAC录制完毕", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private class MyAudioTrack {
        private MediaExtractor mMediaExtractor;
        private MediaCodec mDecoder;
        private int mSamplerRate;
        private int mChannelConfig;
        private String mMine;
        public void start() {
            try {
                mMediaExtractor = new MediaExtractor();
                mMediaExtractor.setDataSource(getSDPath() + "/acc_encode.mp4");
                int mAudioTrack = 0;
                for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                    String mine = mMediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
                    if (mine.startsWith("audio/")) {
                        mAudioTrack = i;
                        break;
                    }
                }
                MediaFormat format =  mMediaExtractor.getTrackFormat(mAudioTrack);
                mMediaExtractor.selectTrack(mAudioTrack);
                mMine = format.getString(MediaFormat.KEY_MIME);
                mSamplerRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                mChannelConfig = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                long duration = format.getLong(MediaFormat.KEY_DURATION);
                Log.e(TAG,"length:" + duration/1000000);

                mDecoder = MediaCodec.createDecoderByType(mMine);
                mDecoder.configure(format, null, null, 0);
                mDecoder.start();

                decodeAndPlay();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AacActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private void decodeAndPlay() {
            int minBufferSize = AudioTrack.getMinBufferSize(mSamplerRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSamplerRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mAudioTrack.play();
            while (true) {
                int inputIndex = mDecoder.dequeueInputBuffer(1000000);
                if (inputIndex >=0 ) {
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);
                    // mediaExtractor读取一帧数据
                    int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, mMediaExtractor.getSampleTime(), 0);
                        mMediaExtractor.advance();
                    }
                } else {
                    break;
                }

                int outIndex = mDecoder.dequeueOutputBuffer(bufferInfo, 100000);
                if (outIndex >= 0) {
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat format = mDecoder.getOutputFormat();
                        Log.d(TAG, "New format " + format);
                        mAudioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                    } else {
                        ByteBuffer outBuffer = mDecoder.getOutputBuffer(outIndex);
                        byte[] bytes = new byte[bufferInfo.size];
                        outBuffer.get(bytes);
                        outBuffer.clear();
                        mAudioTrack.write(bytes, bufferInfo.offset, bufferInfo.offset + bufferInfo.size);
                        mDecoder.releaseOutputBuffer(outIndex, false);
                    }
                } else {
                    break;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 1) {
                    break;
                }
            }
            mDecoder.stop();
            mDecoder.release();
            mMediaExtractor.release();
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    public String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }
}
