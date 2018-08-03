package com.jscheng.textureapplication.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.jscheng.textureapplication.R;

public class CustomView extends View {
    private Paint mPaint;
    private Bitmap mBitmap;
    public CustomView(Context context) {
        super(context);
        init();
    }

    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmap = BitmapFactory.decodeResource(getContext().getResources(), R.mipmap.images);
    }

    //onLayout layout()种调用changed表示是否更改了位置，余下四个参是相对于父view的位置
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpec = MeasureSpec.getMode(widthMeasureSpec);
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpec = MeasureSpec.getMode(heightMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect srcRect = new Rect(0, 0, mBitmap.getHeight(), mBitmap.getWidth());
        Rect destRect = getBitmapRect(mBitmap);
        canvas.drawBitmap(mBitmap, srcRect, destRect, mPaint);
    }

    private Rect getBitmapRect(Bitmap bimap) {
        int bimapHeight = bimap.getHeight();
        int bimapWidth = bimap.getWidth();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        float bimapRatio = (float) bimapWidth / (float) bimapHeight;
        float screenRatio = (float) viewWidth / (float) viewHeight;
        int factWidth;
        int factHeight;
        int x1, y1, x2, y2;
        if (bimapRatio > screenRatio) {
            factWidth = bimapWidth;
            factHeight = (int)(factWidth / bimapRatio);
            x1 = 0;
            y1 = (viewHeight - factHeight) / 2;
        } else if (bimapRatio < screenRatio) {
            factHeight = bimapHeight;
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
