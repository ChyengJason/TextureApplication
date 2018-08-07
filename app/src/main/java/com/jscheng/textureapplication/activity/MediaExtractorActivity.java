package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jscheng.textureapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaExtractorActivity extends AppCompatActivity implements View.OnClickListener{
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediaextractor);
        findViewById(R.id.seperate_btn).setOnClickListener(this);
        findViewById(R.id.seperate_audio_btn).setOnClickListener(this);
        findViewById(R.id.seperate_media_btn).setOnClickListener(this);
        findViewById(R.id.muxer_btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 2);
            return;
        }

        switch (view.getId()) {
            case R.id.seperate_btn:
                new MediaAsyncTask().execute(0);
                break;
            case R.id.seperate_audio_btn:
                new MediaAsyncTask().execute(1);
                break;
            case R.id.seperate_media_btn:
                new MediaAsyncTask().execute(2);
                break;
            case R.id.muxer_btn:
                new MediaAsyncTask().execute(3);
                break;
            default:
                break;
        }

        findViewById(R.id.seperate_btn).setEnabled(false);
        findViewById(R.id.seperate_audio_btn).setEnabled(false);
        findViewById(R.id.seperate_media_btn).setEnabled(false);
        findViewById(R.id.muxer_btn).setEnabled(false);
    }

    private class MediaAsyncTask extends AsyncTask<Integer, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MediaExtractorActivity.this, "开始转化", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Integer... param) {
            if (param.length < 1){
                return null;
            }
            switch (param[0]) {
                case 0:
                    return seperateAssert();
                case 1:
                    return seperateAudio();
                case 2:
                    return seperateMedia();
                case 3:
                    return muxerMediaAndAudio();
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            findViewById(R.id.seperate_btn).setEnabled(true);
            findViewById(R.id.seperate_audio_btn).setEnabled(true);
            findViewById(R.id.seperate_media_btn).setEnabled(true);
            findViewById(R.id.muxer_btn).setEnabled(true);
            if (s != null) {
                Toast.makeText(MediaExtractorActivity.this, "转化完成 " + s, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MediaExtractorActivity.this, "转化失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String muxerMediaAndAudio() {
        File mediaFile = new File(getSDPath(), "seperate_media.mp4");
        File audioFile = new File(getSDPath(), "seperate_audio.mp4");
        if (!mediaFile.exists() || !audioFile.exists()) {
            return "音视频文件不存在";
        }
        MediaExtractor mMediaExtractor = new MediaExtractor();
        MediaExtractor mAudioExtractor = new MediaExtractor();
        try {
            MediaMuxer mMediaMuxer = new MediaMuxer(getSDPath() + "/media.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMediaExtractor.setDataSource(mediaFile.getAbsolutePath());
            mAudioExtractor.setDataSource(audioFile.getAbsolutePath());

            int mMediaTrack = 0;
            int mAudioTrack = 0;

            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                String mine = mMediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
                if (mine.startsWith("video/")) {
                    mMediaTrack = i;
                    break;
                }
            }
            for (int i = 0; i < mAudioExtractor.getTrackCount(); i++) {
                String mine = mAudioExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
                if (mine.startsWith("audio/")) {
                    mAudioTrack = i;
                    break;
                }
            }
            mMediaExtractor.selectTrack(mMediaTrack);
            mAudioExtractor.selectTrack(mAudioTrack);

            MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(mMediaTrack);
            MediaFormat audioFormat = mAudioExtractor.getTrackFormat(mAudioTrack);

            int mediaMuxerTrack = mMediaMuxer.addTrack(mediaFormat);
            int audioMuxerTrack = mMediaMuxer.addTrack(audioFormat);

            mMediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            int readSize = 0;
            while ((readSize = mMediaExtractor.readSampleData(byteBuffer, 0)) > 0) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                bufferInfo.offset = 0;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                bufferInfo.size = readSize;
                mMediaMuxer.writeSampleData(mediaMuxerTrack, byteBuffer, bufferInfo);
                mMediaExtractor.advance();
            }

            while ((readSize = mAudioExtractor.readSampleData(byteBuffer, 0)) > 0) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.presentationTimeUs = mAudioExtractor.getSampleTime();
                bufferInfo.offset = 0;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                bufferInfo.size = readSize;
                mMediaMuxer.writeSampleData(audioMuxerTrack, byteBuffer, bufferInfo);
                mAudioExtractor.advance();
            }

            mMediaExtractor.unselectTrack(mMediaTrack);
            mAudioExtractor.unselectTrack(mAudioTrack);
            mMediaMuxer.stop();
            mMediaMuxer.release();
            return getSDPath() + "/media.mp4";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(mMediaExtractor != null ) { mMediaExtractor.release(); }
            if(mAudioExtractor != null ) { mAudioExtractor.release(); }
        }
        return null;
    }

    private String seperateMedia() {
        MediaExtractor mMediaExtractor = null;
        MediaMuxer mMediaMuxer = null;
        try {
            int mMediaIndex = -1;
            AssetFileDescriptor fileDescriptor = getAssetFileSource();
            mMediaExtractor = new MediaExtractor();
            mMediaMuxer = new MediaMuxer(getSDPath() + "/seperate_media.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                if (mine.startsWith("video/")) {
                    mMediaIndex = i;
                    break;
                }
            }

            mMediaExtractor.selectTrack(mMediaIndex);

            MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(mMediaIndex);
            int muxerTrackIndex = mMediaMuxer.addTrack(mediaFormat);
            mMediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            int readSize = 0;
            while((readSize = mMediaExtractor.readSampleData(byteBuffer, 0)) > 0) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.size = readSize;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                mMediaMuxer.writeSampleData(muxerTrackIndex, byteBuffer, bufferInfo);
                mMediaExtractor.advance();
            }
            mMediaMuxer.stop();
            mMediaExtractor.unselectTrack(mMediaIndex);

            return "success";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(mMediaExtractor != null ) { mMediaExtractor.release(); }
//            if(mMediaMuxer != null) { mMediaMuxer.release(); }
        }
        return null;
    }

    private String seperateAudio() {
        MediaExtractor mMediaExtractor = null;
        MediaMuxer mMediaMuxer = null;
        try {
            int mAudioIndex = -1;
            AssetFileDescriptor fileDescriptor = getAssetFileSource();
            mMediaExtractor = new MediaExtractor();
            mMediaMuxer = new MediaMuxer(getSDPath() + "/seperate_audio.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            int trackCount = mMediaExtractor.getTrackCount(); // 通道数
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i); // 获取通道方式
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.w("TAG", "changeAssert: " + mime);
                if (mime.startsWith("audio/")) { // 音频
                    mAudioIndex = i;
                }
            }

            mMediaExtractor.selectTrack(mAudioIndex);
            MediaFormat format = mMediaExtractor.getTrackFormat(mAudioIndex);
            int mAudioMuxerIndex = mMediaMuxer.addTrack(format);

            ByteBuffer byteBuffer = ByteBuffer.allocate(100 * 1024);
            int readSize = 0;
            mMediaMuxer.start();
            while ((readSize = mMediaExtractor.readSampleData(byteBuffer, 0)) > 0) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.offset = 0;
                bufferInfo.size = readSize;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME; // mMediaExtractor.getSampleFlags() Failed to stop the muxer
                bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                mMediaMuxer.writeSampleData(mAudioMuxerIndex, byteBuffer, bufferInfo);
                mMediaExtractor.advance();
            }
            mMediaMuxer.stop();
            mMediaExtractor.unselectTrack(mAudioIndex);

            return "success";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(mMediaExtractor != null ) { mMediaExtractor.release(); }
            if(mMediaMuxer != null) { mMediaMuxer.release(); }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private String seperateAssert() {
        MediaExtractor mMediaExtractor = null;
        try {
            // 设置数据源
            AssetFileDescriptor fileDescriptor = getAssetFileSource();
            int mAudioIndex = -1;
            int mVideoIndex = -1;

            if (fileDescriptor == null) {
                return null;
            }
            mMediaExtractor = new MediaExtractor();
            // mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor()) 会初始化失败 Failed to instantiate extractor
            mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            int trackCount = mMediaExtractor.getTrackCount(); // 通道数
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i); // 获取通道方式
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.w("TAG", "changeAssert: " + mime);
                if (mime.startsWith("audio/")) { // 音频
                    mAudioIndex = i;
                } else if (mime.startsWith("video/")) { // 视频
                    mVideoIndex = i;
                }
            }

            mMediaExtractor.selectTrack(mAudioIndex);
            File audioFile = new File(getSDPath(), "screen_recorder_audio");
            if (audioFile.exists()) {
                audioFile.delete();
            }
            FileOutputStream audioStream = new FileOutputStream(audioFile);
            ByteBuffer audioBufferBytes = ByteBuffer.allocate(500  * 1024);
            int readSize = 0;
            while((readSize = mMediaExtractor.readSampleData(audioBufferBytes, 0)) > 0) {
                audioStream.write(audioBufferBytes.array());
                mMediaExtractor.advance(); // 下一帧
            }
            long audioSize = audioStream.getChannel().size();
            audioStream.close();
            mMediaExtractor.unselectTrack(mAudioIndex);

            mMediaExtractor.selectTrack(mVideoIndex);
            File videoFile = new File(getSDPath(), "screen_recorder_video");
            if (videoFile.exists()) {
               videoFile.delete();
            }
            FileOutputStream videoStream = new FileOutputStream(videoFile);
            ByteBuffer vedioBufferBytes = ByteBuffer.allocate(500 * 1024);
            readSize = 0;
            while ((readSize = mMediaExtractor.readSampleData(vedioBufferBytes, 0)) > 0) {
                videoStream.write(vedioBufferBytes.array(), 0, readSize);
                mMediaExtractor.advance();
            }
            long videoSize = videoStream.getChannel().size();
            videoStream.close();
            mMediaExtractor.unselectTrack(mVideoIndex);

            fileDescriptor.close();

            return "audioSize: " + audioSize + " - videoSize: " + videoSize;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(mMediaExtractor != null ) { mMediaExtractor.release(); }
        }
        return null;
    }

    private AssetFileDescriptor getAssetFileSource() throws IOException {
        AssetManager manager = getAssets();
        return manager.openFd("screen_recorder.mp4");
    }

    public static String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }
}
