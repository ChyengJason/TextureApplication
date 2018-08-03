package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.jscheng.textureapplication.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RecordingActivity extends AppCompatActivity {
    public static String[] MICROPHONE = {Manifest.permission.RECORD_AUDIO};
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private final static int AudioSource = MediaRecorder.AudioSource.DEFAULT;
    private final static int AudioRate = 44100;
    private final static int AudioInChannel = AudioFormat.CHANNEL_IN_MONO;
    private final static int AudioOutChannel = AudioFormat.CHANNEL_OUT_MONO;
    private final static int AudioFormater = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private Button mRecordBtn;
    private Button mPlayBtn;
    private boolean isRecording;
    private boolean isPlaying;
    private ExecutorService mExecutor;
    private ThreadFactory mThreadFactory;
    private int recordBufferMinSize;
    private int playBufferMinSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        mRecordBtn = findViewById(R.id.recording_btn);
        mPlayBtn = findViewById(R.id.playing_btn);
        mThreadFactory = new NameThreadFactory();
        mExecutor = new ThreadPoolExecutor(1, 1, 2000L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(1024), mThreadFactory);
        initRecord();
        initTrack();
        initView();
    }

    private void initView() {
        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mRecordBtn.setTag(true);
                    mRecordBtn.setText("正在录制");
                    startRecord();
                } else if (action == MotionEvent.ACTION_UP) {
                    mRecordBtn.setTag(false);
                    mRecordBtn.setText("开始录音");
                    stopRecord();
                }
                return false;
            }
        });
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPcm();
            }
        });
    }

    private void initRecord() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, MICROPHONE, 1);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 1);
            return;
        }

        isRecording = false;
        recordBufferMinSize = AudioRecord.getMinBufferSize(AudioRate, AudioInChannel, AudioFormater);
        mAudioRecord = new AudioRecord(AudioSource, AudioRate, AudioInChannel, AudioFormater, recordBufferMinSize);
        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("mAudioRecord", "initRecord: mAudioRecord init failed");
        }
    }

    private void initTrack() {
        isPlaying = false;
        playBufferMinSize = AudioTrack.getMinBufferSize(AudioRate, AudioOutChannel, AudioFormater);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioRate, AudioOutChannel, AudioFormater, playBufferMinSize, AudioTrack.MODE_STREAM);
    }

    private void stopRecord() {
        if (isRecording) {
            isRecording = false;
            mAudioRecord.stop();
        }
    }

    private void startRecord() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, MICROPHONE, 2);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 2);
            return;
        }
        isRecording = true;
        mExecutor.execute(new RecordRunnable());
    }

    private void playPcm() {
        isPlaying = true;
        mExecutor.execute(new PlayerRunnable());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            initRecord();
        } else if(requestCode == 2 && (boolean) (mRecordBtn.getTag()) == true){
            startRecord();
        }
    }

    private class RecordRunnable implements Runnable {
        private OutputStream mOutputStream;
        private byte[] bufferbytes;
        private File mFile;

        public RecordRunnable() {
            try {
                mFile = getRecordFile();
                bufferbytes = new byte[recordBufferMinSize];
                mOutputStream = new FileOutputStream(mFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                mAudioRecord.startRecording();
                while(isRecording) {
                    int readSize = mAudioRecord.read(bufferbytes, 0, recordBufferMinSize);
                    if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                        mOutputStream.write(bufferbytes, 0, readSize);
                    }
                }
                mOutputStream.close();
                View contentView = RecordingActivity.this.getWindow().getDecorView().findViewById(android.R.id.content);
                contentView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RecordingActivity.this, "录制完成: " + mFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private class PlayerRunnable implements Runnable {
        private InputStream mInputStream;
        private File mFile;
        private byte[] bufferbytes;

        public PlayerRunnable() {
            try {
                mFile = getPlayerFile();
                bufferbytes = new byte[playBufferMinSize];
                mInputStream = new FileInputStream(mFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            View contentView = RecordingActivity.this.getWindow().getDecorView().findViewById(android.R.id.content);
            if (mInputStream == null) {
                contentView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RecordingActivity.this, "音频不存在", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            try {
                while (mInputStream.available() > 0) {
                    int readSize = mInputStream.read(bufferbytes);
                    if (readSize== -1 || readSize == AudioTrack.ERROR_BAD_VALUE || readSize == AudioTrack.ERROR_INVALID_OPERATION) {
                        continue;
                    }
                    mAudioTrack.write(bufferbytes, 0, playBufferMinSize);
                }
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAudioTrack.stop();
            isPlaying = false;
        }
    }

    private class NameThreadFactory implements ThreadFactory {
        private int count = 0;

        public NameThreadFactory() {
        }

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, "MyThread - " + count++);
        }
    }

    @Override
    protected void onDestroy() {
        mAudioTrack.release();
        mAudioRecord.release();
        super.onDestroy();
    }

    public static String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    public File getRecordFile() {
        File dir = new File(getSDPath());
        dir.mkdirs();
        File mFile = new File(dir, "record-file");
        if (mFile.exists()) {
            mFile.delete();
        }
        return mFile;
    }

    public File getPlayerFile() {
        File dir = new File(getSDPath());
        File mFile = new File(dir, "record-file");
        if (mFile.exists()) {
            return mFile;
        }
        return null;
    }
}
