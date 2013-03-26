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
    int mRowHeight;
     float mColWidth;
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
        mRowHeight = 35;

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
        setMeasuredDimension(width, mRowHeight*mRows+2*mLineThickness);
    }


    private int xToJ(float x) {
        int j =  (int) (x / mColWidth);
        if( j < 0 || j >= mCols ) return -1;
        return j;
    }

    private int yToI(float y) {
        int i =  (int) (y / mRowHeight);
        if( y < 1 || i < 0 || i >= mRows ) return -1;
        return i;
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
    public void onDraw(Canvas c) {
        mColWidth = (float)(mWidth-mLineThickness)/ mCols;
        c.drawRect(0, 0, mWidth, mHeight, mBorderPaint);
        //drawGrid(c);
        for(int i = 0; i < mRows; i++) {
           for(int j = 0; j < mCols; j++) {
               drawKey(c,i,j);
           }
        }
    }

    private void drawKey(Canvas c, int i, int j) {
        // intervals inclusive [x0, x1]
        String label = mKeyboard.getKeyLabel(i,j,0);
        String altlabel = mKeyboard.getKeyLabel(i,j,SquareKeyboard.SWIPE_DISPLAY);
        float x0 = j*mColWidth+mLineThickness;
        int j2=j+1;
        if( label.equals(" ")) {
            while(j2<mCols && mKeyboard.getKeyLabel(i,j2,0).equals(" ")) 
                j2++;
        }
        float x1 = j2*mColWidth;//-mLineThickness ;
        //x1 = Math.min(x1,(float)mWidth-mLineThickness);
        float y0 = i*mRowHeight+mLineThickness;
        float y1 = (i+1)*mRowHeight;//-mLineThickness ;
        //y1 = Math.min(y1,(float)mHeight-mLineThickness);
        Paint p;
        if(mActiveI == i && mActiveJ == j) {
            p = mActivePaint;
        } else if(mKeyboard.getKeyState(i,j,0) == SquareKeyboard.KEYSTATE_DEADMETA) {
            p = mLatchedPaint;
        } else {
            p = mBackgroundPaint;
        }

        drawKey(c,x0,x1,y0,y1,label,altlabel,p);
    }

    private void updatePreview() {
        if(mActiveI == -1 )  {
            mPreviewWindow.dismiss();
            return;
        }
        String label = mKeyboard.getKeyLabel(mActiveI,mActiveJ,mActiveDir);
        int state = mKeyboard.getKeyState(mActiveI,mActiveJ,mActiveDir);
        int x = (int) (mColWidth*mActiveJ+mColWidth/2);
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
                // FIXME maybe. Redrawing entire keyboard is probably a non-issue.
                //drawKey(oldI,oldJ);
                invalidate();
            }
            if(i >= 0) {
                //drawKey(i,j);
                invalidate();
            }
            //invalidate();
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

