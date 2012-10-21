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
    SquareKeyboard mKeyboard;
    int mRows, mCols;
    int mRowHeight, mColWidth;
    int mLineThickness = 1;

    int mWidth, mHeight;
    Canvas mCanvas;
    Bitmap mBuffer;
    Paint mTextPaint, mBackgroundPaint, mBorderPaint, mActivePaint;
    boolean mNeedsDraw = true;

    float mStartX, mStartY;
    int mActiveI = -1, mActiveJ = -1;

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
        mRows = 1;
        mCols = 1;
        mRowHeight = 30;

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTypeface(Typeface.MONOSPACE);
        mTextPaint.setTextSize(9);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setARGB(255,255,255,255);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setARGB(255,16,16,16);
        mActivePaint = new Paint();
        mActivePaint.setARGB(255,16,128,65);
        mBorderPaint = new Paint();
        mBorderPaint.setARGB(255,32,128,32);
    }

    void setKeyboard(SquareKeyboard keyboard) {
        mKeyboard = keyboard;
        keyboard.setView(this);
        mRows = keyboard.getRows();
        mCols = keyboard.getCols();
        // size of keyboard might have changed
        mHeight = 0; // force resize
        requestLayout(); 
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
            mColWidth = w/ mCols;
            mNeedsDraw = true;
            invalidate();
            Log.d(TAG, "updateSize " + w + " " + h);
        }
    }

    public void invalidateAllKeys() {
        mNeedsDraw = true;
        invalidate();
    }

    private int xToJ(float x) {
        if( x <= 1 || x >= mWidth -1 ) return -1;
        return (int) (x / mColWidth);
    }

    private int yToI(float y) {
        if( y <= 1 || y >= mHeight -1 ) return -1;
        return (int) (y / mRowHeight);
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
        //c.drawRect(0, 0, w, h, mBackgroundPaint);
        drawGrid();
        for(int i = 0; i < mRows; i++) {
           for(int j = 0; j < mCols; j++) {
               drawKey(i,j);
           }
        }
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

    private void drawKey(int i, int j) {
        // intervals inclusive [x0, x1]
        int y0 = i*mRowHeight+mLineThickness;
        int y1 = y0 + mRowHeight-mLineThickness ;
        int x0 = j*mColWidth+mLineThickness;
        int x1 = x0 + mColWidth-mLineThickness ;
        x1 = Math.min(x1,mWidth-mLineThickness);
        Paint p;
        if(mActiveI == i && mActiveJ == j) {
            p = mActivePaint;
        } else {
            p = mBackgroundPaint;
        }
            
        mCanvas.drawRect(x0,y0,x1,y1,p);
        mCanvas.drawText(mKeyboard.getKeyLabel(i,j),(x0+x1)/2,(y0+y1)/2,mTextPaint);
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
            onTouchUp(ev.getX(),ev.getY());
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

    void onMovePos(float x, float y) {
        int j = xToJ(x), i = yToI(y);
        if(j == -1) i = -1;
        if(mActiveI != i || mActiveJ != j)  {
            int oldI = mActiveI;
            int oldJ = mActiveJ;
            mActiveI = i; mActiveJ = j;
            if(oldI >= 0) {
                drawKey(oldI,oldJ);
            }
            if(i >= 0) {
                drawKey(i,j);
            }
            invalidate();
        }
    }

    void onTouchUp(float x, float y) {
        int j = xToJ(x), i = yToI(y);
        if( i < 0 || j < 0) return;
        mActiveI = mActiveJ = -1;
        mKeyboard.onKeyPress(i,j);
        drawKey(i,j);
        invalidate();
    }


}

