package com.jscheng.textureapplication.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jscheng.textureapplication.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ImageSurfaceActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private ExecutorService mThread;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new SurfaceView(this);
        setContentView(mSurfaceView);
        initSurfaceView();
    }

    private void initSurfaceView() {
        // 创建一个只有一个线程的线程池，其实用Thread也可以
        mThread = new ThreadPoolExecutor(1, 1, 2000L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());
        // 添加SurfaceHolder.callback，在回调中可以绘制
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // 执行绘制
        mThread.execute(new DrawRunnable());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (!mThread.isShutdown()) {
            mThread.shutdown();
        }
    }

    private class DrawRunnable implements Runnable {
        @Override
        public void run() {
            Bitmap bimap = BitmapFactory.decodeResource(ImageSurfaceActivity.this.getResources(), R.mipmap.images);
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            Canvas canvas = surfaceHolder.lockCanvas(); // 获取画布
            Paint paint = new Paint();
            Rect srcRect = new Rect(0, 0, bimap.getHeight(), bimap.getWidth());
            Rect destRect = getBitmapRect(bimap);
            canvas.drawBitmap(bimap, srcRect, destRect, paint);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    /**
     *     图片的尺寸和屏幕的尺寸不一样，需要把图片调整居中
     **/
    private Rect getBitmapRect(Bitmap bimap) {
        int bimapHeight = bimap.getHeight();
        int bimapWidth = bimap.getWidth();
        int viewWidth = mSurfaceView.getWidth();
        int viewHeight = mSurfaceView.getHeight();
        float bimapRatio = (float) bimapWidth / (float) bimapHeight; // 宽高比
        float screenRatio = (float) viewWidth / (float) viewHeight;
        int factWidth;
        int factHeight;
        int x1, y1, x2, y2;
        if (bimapRatio > screenRatio) {
            factWidth = viewWidth;
            factHeight = (int)(factWidth / bimapRatio);
            x1 = 0;
            y1 = (viewHeight - factHeight) / 2;
        } else if (bimapRatio < screenRatio) {
            factHeight = viewHeight;
            factWidth = (int)(factHeight * bimapRatio);
            x1 = (viewWidth - factWidth) / 2;
            y1 = 0;
        } else {
            factWidth = bimapWidth;
            factHeight = bimapHeight;
            x1 = 0;
            y1 = 0;
        }
        x2 = x1 + factWidth;
        y2 = y1 + factHeight;
        return new Rect(x1, y1, x2, y2);
    }
}
