package com.bfl.squarekeyboard;
import android.view.View;
import android.view.View;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.util.Log;

public class SquareKeyboardView extends View {

    private final String TAG = "SquareKeyboardView";
    int mRows, mCols;
    int mRowHeight;
    int mLineThickness = 1;

    int mWidth, mHeight;
    Canvas mCanvas;
    Bitmap mBuffer;
    Paint mPaint, mBackgroundPaint, mBorderPaint;
    boolean mNeedsDraw = true;


    public SquareKeyboardView(Context context) {
        super(context);
        construct();
    }

    public SquareKeyboardView(Context context, AttributeSet attrs ) {
        super(context, attrs);
        construct();
    }

    public SquareKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        construct();
    }
    
    private void construct() {
        mRows = 5;
        mCols = 10;
        mRowHeight = 30;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        //mPaint.setTextSize(keyTextSize);
        //mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setARGB(255,32,32,33);
        mBorderPaint = new Paint();
        mBorderPaint.setARGB(255,32,128,32);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec); // should be fill parent
        setMeasuredDimension(width, mRowHeight*mRows+mLineThickness);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateSize(false);
    }

    public void updateSize(boolean force) {
        int w = getWidth();
        int h = getHeight();
        if (force || w != mWidth || h != mHeight) {
            mWidth = w;
            mHeight = h;
            mNeedsDraw = true;
            invalidate();
            Log.d(TAG, "updateSize " + w + " " + h);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mNeedsDraw) {
            drawBuffer();
            mNeedsDraw = false;
        }
        canvas.drawBitmap(mBuffer, 0, 0, null);
    }

    private void drawBuffer() {
        int w = mWidth, h = mHeight;
        if (mBuffer == null||mBuffer.getWidth() != w || mBuffer.getHeight() != h) { // || resized
            mBuffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBuffer);
        }
        Canvas c = mCanvas;
        c.drawRect(0, 0, w, h, mBackgroundPaint);
        drawGrid();
    }

    private void drawGrid() {
        float w1 = (float)mWidth/mCols;
        float h1 = mRowHeight;
        for(int i = 0; i < mRows+1; i++) {
            mCanvas.drawLine(0,i*h1,mWidth,i*h1,mBorderPaint);
        }
        for(int i = 0; i < mCols; i++) {
            mCanvas.drawLine(i*w1,0,i*w1,mHeight,mBorderPaint);
        }
        mCanvas.drawLine(mWidth-1,0,mWidth-1,mHeight,mBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        String action;
        if( a == ev.ACTION_DOWN) {
            action  = "DOWN";
            mStartX = ev.getX();
            mStartY = ev.getY();
            onMovePos(ev.getX(),ev.getY());
        } else if(a == ev.ACTION_MOVE) {
            action = "MOVE";
            onMovePos(ev.getX(),ev.getY());
        } else if(a == ev.ACTION_UP) {
            action = "UP";
        } else if(a == ev.ACTION_CANCEL) {
            action = "CANCEL";
        } else if(a == ev.ACTION_OUTSIDE) {
            action = "OUTSIDE";
        } else {
            return false;
        }

        Log.d(TAG, "touch " + action + " " + ev.getX() + " " + ev.getY());
        return true;
    }


    public interface ActionListener {
        void onKey(char ch);
        void onText(CharSequence text);
        void onSpecialKey(int keyCode, KeyEvent event);
    }
}
