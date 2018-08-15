package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.jscheng.textureapplication.R;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AacTrackActivity extends AppCompatActivity {
    public static String[] MICROPHONE = {Manifest.permission.RECORD_AUDIO};
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private MyAudioTrack myAudioTrack;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac_track);
        myAudioTrack = new MyAudioTrack();
        findViewById(R.id.acc_playing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkRecordPermission();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        myAudioTrack.start();
                    }
                }).start();
            }
        });
    }

    private class MyAudioTrack {
        private AudioTrack mPlayer;
        private MediaCodec mDecoder;
        private MediaExtractor mExtractor;

        public void start() {
            try {
                mExtractor = new MediaExtractor();
                mExtractor.setDataSource(getSDPath() + "/acc_encode.mp4");
                MediaFormat mFormat = null;
                int samplerate = 0;
                int changelConfig = 0;
                int selectTrack = 0;
                String mine = MediaFormat.MIMETYPE_AUDIO_AAC;
                for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                    mFormat = mExtractor.getTrackFormat(i);
                    mine = mFormat.getString(MediaFormat.KEY_MIME);
                    if (mine.startsWith("audio/")) {
                        selectTrack = i;
                        samplerate = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        changelConfig = mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        break;
                    }
                }
                mExtractor.selectTrack(selectTrack);

                int minBufferSize = AudioTrack.getMinBufferSize(samplerate, changelConfig, AudioFormat.ENCODING_PCM_16BIT);
                mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, samplerate, changelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
                mPlayer.play();

                mDecoder = MediaCodec.createDecoderByType(mine);
                mDecoder.configure(mFormat, null, null, 0);
                mDecoder.start();
                decodeAndPlay();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AacTrackActivity.this, "文件不存在",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private void decodeAndPlay() {
            boolean isFinish = false;
            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            while (!isFinish) {
                int inputIdex = mDecoder.dequeueInputBuffer(10000);
                if (inputIdex < 0) {
                    isFinish = true;
                }
                ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIdex);
                inputBuffer.clear();
                int samplesize = mExtractor.readSampleData(inputBuffer, 0);
                if (samplesize > 0) {
                    mDecoder.queueInputBuffer(inputIdex, 0, samplesize, 0, 0);
                    mExtractor.advance();
                } else {
                    isFinish = true;
                }
                int outputIndex = mDecoder.dequeueOutputBuffer(decodeBufferInfo, 10000);
                ByteBuffer outputBuffer;
                byte[] chunkPCM;
                while (outputIndex >= 0) {//每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
                    outputBuffer = mDecoder.getOutputBuffer(outputIndex);//拿到用于存放PCM数据的Buffer
                    chunkPCM = new byte[decodeBufferInfo.size];//BufferInfo内定义了此数据块的大小
                    outputBuffer.get(chunkPCM);//将Buffer内的数据取出到字节数组中
                    outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
                    mPlayer.write(chunkPCM, 0, decodeBufferInfo.size);
                    mDecoder.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
                    outputIndex = mDecoder.dequeueOutputBuffer(decodeBufferInfo, 10000);//再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
                }
            }
            release();
        }

        private void release() {
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
                mDecoder = null;
            }

            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }

            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AacTrackActivity.this, "播放完成", Toast.LENGTH_SHORT).show();
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
}
