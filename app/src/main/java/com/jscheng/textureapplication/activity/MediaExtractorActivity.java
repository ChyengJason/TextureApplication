package com.jscheng.textureapplication.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
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
    private static String TAG = MediaExtractorActivity.class.getSimpleName();
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediaextractor);
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
                case 1:
                    return seperateMedia("audio.mp4", true);
                case 2:
                    return seperateMedia("video.mp4", false);
                case 3:
                    return muxerMediaAndAudio("video.mp4","audio.mp4", "result.mp4");
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            findViewById(R.id.seperate_audio_btn).setEnabled(true);
            findViewById(R.id.seperate_media_btn).setEnabled(true);
            findViewById(R.id.muxer_btn).setEnabled(true);
            if (s != null) {
                Toast.makeText(MediaExtractorActivity.this, "转化完成 " + s, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MediaExtractorActivity.this, "转化失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String muxerMediaAndAudio(String mediaFileName, String audioFileName, String resultName) {
        File mediaFile = new File(getSDPath(), mediaFileName);
        File audioFile = new File(getSDPath(), audioFileName);
        if (!mediaFile.exists() || !audioFile.exists()) {
            return "音视频文件不存在";
        }
        MediaMuxer mMediaMuxer = null;
        MediaExtractor mMediaExtractor = new MediaExtractor();
        MediaExtractor mAudioExtractor = new MediaExtractor();
        try {
            mMediaMuxer = new MediaMuxer(getSDPath() + "/" + resultName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
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

            MediaCodec.BufferInfo mediaBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            int mediaMuxerTrack = mMediaMuxer.addTrack(mediaFormat);
            int audioMuxerTrack = mMediaMuxer.addTrack(audioFormat);

            mMediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            int readSize = 0;

            int mMediaFramerate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            while ((readSize = mMediaExtractor.readSampleData(byteBuffer, 0)) > 0) {
                mediaBufferInfo.presentationTimeUs += 1000 * 1000 / mMediaFramerate;
                mediaBufferInfo.offset = 0;
                mediaBufferInfo.flags = mMediaExtractor.getSampleFlags();
                mediaBufferInfo.size = readSize;
                mMediaMuxer.writeSampleData(mediaMuxerTrack, byteBuffer, mediaBufferInfo);
                mMediaExtractor.advance();
            }

            while ((readSize = mAudioExtractor.readSampleData(byteBuffer, 0)) > 0) {
                audioBufferInfo.presentationTimeUs = mAudioExtractor.getSampleTime();
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = mAudioExtractor.getSampleFlags();
                audioBufferInfo.size = readSize;
                mMediaMuxer.writeSampleData(audioMuxerTrack, byteBuffer, audioBufferInfo);
                mAudioExtractor.advance();
            }
            mMediaMuxer.stop();
            return getSDPath() + resultName;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(mMediaExtractor != null ) {
                mMediaExtractor.release();
                mMediaExtractor = null;
            }
            if(mAudioExtractor != null ) {
                mAudioExtractor.release();
                mAudioExtractor = null;
            }
            if(mMediaMuxer != null) {
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        }
        return null;
    }

    private String seperateMedia(String fileName, boolean isAudio) {
        String type = isAudio ? "audio/" : "video/";
        MediaExtractor mMediaExtractor = new MediaExtractor();;
        MediaMuxer mMediaMuxer = null;
        try {
            mMediaMuxer = new MediaMuxer(getSDPath() + "/" + fileName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            // 获取assert中的资源文件
            AssetFileDescriptor fileDescriptor = getAssetFileSource();
            // 设置资源文件
            mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            int mMediaIndex = 0;
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                //获取码流的详细格式/配置信息
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                // 查找音频："audio/" 或者视频："video/"的轨道
                if (mine.startsWith(type)) {
                    mMediaIndex = i;
                    break;
                }
            }
            // 选择感兴趣的轨道
            mMediaExtractor.selectTrack(mMediaIndex);
            // 获取通道格式，可以自己新建，但是有坑
            MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(mMediaIndex);
            int muxerTrackIndex = mMediaMuxer.addTrack(mediaFormat);
            // 当采集视频的使用，需要获取帧率，音频轨道没有这个参数
            int framerate = 0;
            if (mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                framerate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            }
            mMediaMuxer.start();

            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            int readSize = 0;
            // writeSampleData需要BufferInfo参数
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while((readSize = mMediaExtractor.readSampleData(byteBuffer, 0)) > 0) {
                bufferInfo.size = readSize;
                bufferInfo.flags = mMediaExtractor.getSampleFlags(); //设置为关键帧等
                bufferInfo.offset = 0;
                if (framerate != 0) { // 时间戳，音频和视频的处理方式不一样
                    bufferInfo.presentationTimeUs += 1000 * 1000 / framerate;
                } else {
                    bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                }
                mMediaMuxer.writeSampleData(muxerTrackIndex, byteBuffer, bufferInfo);
                Log.d("getSampleTime", "seperateMedia: " + mMediaExtractor.getSampleTime() );
                mMediaExtractor.advance(); //下一帧
            }
            mMediaMuxer.stop();
            return "success";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 释放资源
            if(mMediaExtractor != null ) {
                mMediaExtractor.release();
                mMediaExtractor = null;
            }
            if(mMediaMuxer != null) {
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
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
