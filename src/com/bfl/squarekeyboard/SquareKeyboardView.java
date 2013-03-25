package com.bfl.squarekeyboard;
import android.view.View;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.content.res.Resources;

public class SquareKeyboardView extends BaseKeyboardView {

    private final String TAG = "SquareKeyboardView";
    SquareKeyboard mKeyboard;
    int mRows, mCols;
    int mRowHeight, mColWidth;
    int mLineThickness = 1;


    float mStartX, mStartY;
    int mActiveI = -1, mActiveJ = -1;

    int mActiveDir = 0;
    int sweepTreshold = 50;



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


    private int xToJ(float x) {
        if( x <= 1 || x >= mWidth -1 ) return -1;
        return (int) (x / mColWidth);
    }

    private int yToI(float y) {
        if( y <= 1 || y >= mHeight -1 ) return -1;
        return (int) (y / mRowHeight);
    }

    private int calcAngle(float dx, float dy) {
        if(dx*dx+dy*dy < sweepTreshold*sweepTreshold) 
            return 0;
        double angl = Math.atan2(dy,dx) / (Math.PI);
        if(angl > -0.25 && angl < 0.25 || angl > 0.75 || angl < -0.75) 
            return SquareKeyboard.SWIPE_LR;
        else
            return SquareKeyboard.SWIPE_UD;
    }

    @Override
    protected void reDraw() {
        Canvas c = mCanvas;
        mColWidth = mWidth/ mCols;
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
        } else if(mKeyboard.getKeyState(i,j,0) == SquareKeyboard.KEYSTATE_DEADMETA) {
            p = mLatchedPaint;
        } else {
            p = mBackgroundPaint;
        }

        mCanvas.drawRect(x0,y0,x1,y1,p);
        String label = mKeyboard.getKeyLabel(i,j,0);
        String altlabel = mKeyboard.getKeyLabel(i,j,SquareKeyboard.SWIPE_DISPLAY);
        if( altlabel.equals(label) || altlabel.equals("")) {
            Paint paint;
            if(label.length() > 2) {
                paint = mSmallTextPaint;
            } else {
                paint = mTextPaint;
            }
            mCanvas.drawText(label,(x0+x1)/2,(y0+y1)/2+5,paint);
        } else {
            mCanvas.drawText(label,(3*x0+x1)/4, (4*y1+y0)/5,mTextPaint);
            mCanvas.drawText(altlabel,(1*x0+4*x1)/5,(3*y0+1*y1)/4+5,mAltTextPaint);
        }
    }

    private void updatePreview() {
        if(mActiveI == -1 )  {
            mPreviewWindow.dismiss();
            return;
        }
        String label = mKeyboard.getKeyLabel(mActiveI,mActiveJ,mActiveDir);
        int state = mKeyboard.getKeyState(mActiveI,mActiveJ,mActiveDir);
        int x = mColWidth*mActiveJ+mColWidth/2;
        int y = mRowHeight*(mActiveI-2)-mRowHeight/2;
        showPreview(label,state,x,y);
    }


    @Override
    void onTouchDown(int id, float x, float y) {
        mStartX = x;
        mStartY = y;
        onTouchMove(id, x, y);
    }


    @Override
    void onTouchMove(int id,float x, float y) {
        int j = xToJ(x), i = yToI(y), dir = calcAngle(x-mStartX,y-mStartY);
        if(j == -1) i = -1;
        if(mActiveI != i || mActiveJ != j || mActiveDir != dir)  {
            int oldI = mActiveI;
            int oldJ = mActiveJ;
            mActiveI = i; mActiveJ = j;
            mActiveDir = dir;
            updatePreview();
            if(oldI >= 0) {
                drawKey(oldI,oldJ);
            }
            if(i >= 0) {
                drawKey(i,j);
            }
            invalidate();
        }
    }

    @Override
    void onTouchUp(int id,float x, float y) {
        int j = xToJ(x), i = yToI(y), dir = calcAngle(x-mStartX,y-mStartY);
        if( i < 0 || j < 0) return;
        // FIXME: multitouch still a bit buggy, can commit unpreviewed key
        mKeyboard.onKeyPress(i,j,dir);
        onTouchMove(id,-1f,-1f);
    }


}

