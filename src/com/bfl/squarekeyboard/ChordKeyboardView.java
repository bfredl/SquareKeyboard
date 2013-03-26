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

public class ChordKeyboardView extends BaseKeyboardView {

    private final String TAG = "SquareKeyboardView";
    SquareKeyboard mKeyboard;
    int mRows, mCols;
    int mRowHeight, mColWidth;
    int mLineThickness = 1;

    int mMidPaneX, mRightPaneX;
    private final int BOXES =2;


    public ChordKeyboardView(Context context) {
        super(context);
        construct();
    }

    public ChordKeyboardView(Context context, AttributeSet attrs ) {
        super(context, attrs);
        construct();
    }

    public ChordKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        construct();
    }
    
    private void construct() {
        mRows = 2;
        mCols = 5;
        mRowHeight = 40;
        mColWidth = 40;
    }

    void setKeyboard(SquareKeyboard keyboard) {
        mKeyboard = keyboard;
        mRows = 2;
        mCols = 5;
        // size of keyboard might have changed
        requestLayout(); 
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec); // should be fill parent
        setMeasuredDimension(width, mRowHeight*mRows+mLineThickness);
    }


    @Override
    public void updateSize() {
        super.updateSize();
        mMidPaneX = mCols*mColWidth;
        mRightPaneX = mWidth - mMidPaneX;
    }

    private int indexToId(int box, int col, int row) {
        return mRows*(mCols*box+col)+row; // C order: key[box][[col][row]
    }

    private int posToId(float x,float y) {
        if( x <= 1 || x >= mWidth -1 ) return -1;
        if( y <= 1 || y >= mWidth -1 ) return -1;
        int row = (int) y/ mRowHeight;
        int box;
        if( x < mMidPaneX) {
            box = 0;
        } else if(x >= mRightPaneX) {
            box = 1;
            x -= mRightPaneX;
        } else {
            // return midPaneId(x,y);
            return -2;
        }
        int col = (int) (x / mColWidth);
        return indexToId(box,col,row);
    }


    @Override
    public void onDraw(Canvas c) {
        mColWidth = mWidth/ mCols;
        c.drawRect(0, 0, mWidth, mHeight, mBorderPaint);
        //drawGrid(c);
        for(int box =0; box < BOXES; box++) {
            for(int i = 0; i < mRows; i++) {
               for(int j = 0; j < mCols; j++) {
                   drawKey(c,box,i,j);
               }
            }
        }
    }

    private void drawKey(Canvas c, int box, int i, int j) {
        // intervals inclusive [x0, x1]
        String label = String.valueOf(indexToId(box,i,j));
        String altlabel = "";
        int boxX = (box == 1) ? mRightPaneX : 0;
        float x0 = boxX+j*mColWidth+mLineThickness;
        float x1 = boxX+(j+1)*mColWidth;
        float y0 = i*mRowHeight+mLineThickness;
        float y1 = (i+1)*mRowHeight;//-mLineThickness ;
        x1 = Math.min(x1,mWidth-mLineThickness);
        Paint p;
            p = mBackgroundPaint;

        drawKey(c,x0,x1,y0,y1,label,altlabel,p);
    }


    @Override
    void onTouchDown(int ptrId, float x, float y) {
        onTouchMove(ptrId, x, y);
    }


    @Override
    void onTouchMove(int ptrId,float x, float y) {
        int id = posToId(x,y);
    }

    @Override
    void onTouchUp(int ptrId,float x, float y) {
        onTouchMove(ptrId,-1f,-1f);
    }


}

