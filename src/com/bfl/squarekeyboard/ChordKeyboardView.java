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
import java.util.*;

public class ChordKeyboardView extends BaseKeyboardView {

    private final String TAG = "SquareKeyboardView";
    SquareKeyboard mKeyboard;
    int mRows, mCols;
    int mRowHeight, mColWidth;
    int mLineThickness = 1;

    int mMidPaneX, mRightPaneX;
    private final int BOXES =2;

    // move this to ChordKeyboard when implemented 
    SquareKeyboard.ActionListener mListener;

    
    Map<Integer,Sequence> mTouchSequences = new HashMap<Integer,Sequence>();

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
        mRowHeight = 50;
        mColWidth = 50;
    }

    void setListener_Temporary(SquareKeyboard.ActionListener l) {
        this.mListener = l;
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
        mMidPaneX = mCols*mColWidth+mLineThickness;
        mRightPaneX = mWidth - mMidPaneX;
    }

    private int indexToId(int box, int i, int j) {
        return mRows*(mCols*box+j)+i; // C order: key[box][[col][row]
    }

    private int posToId(float x,float y) {
        if( x <= 1 || x >= mWidth -1 ) return -1;
        if( y <= 1 || y >= mWidth -1 ) return -1;
        int i = (int) y/ mRowHeight;
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
        int j = (int) (x / mColWidth);
        return indexToId(box,i,j);
    }


    @Override
    public void onDraw(Canvas c) {
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
        String label = String.valueOf(keyName(indexToId(box,i,j)));
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
        int key = posToId(x,y);
        mTouchSequences.put(ptrId, new Sequence(key));
        onTouchMove(ptrId, x, y);
    }


    @Override
    void onTouchMove(int ptrId,float x, float y) {
        int key = posToId(x,y);
        Sequence seq = mTouchSequences.get(ptrId);
        if(seq == null) 
            return;
        seq.curKey = key;
        invalidate();
        showPreview(describeChord(),0,mWidth/2,mHeight/2);
    }

    @Override
    void onTouchUp(int ptrId,float x, float y) {
        int id = posToId(x,y);
        Sequence seq = mTouchSequences.get(ptrId);
        if(seq == null) 
            return;
        mListener.onText("#"+describeChord()+"\n");
        mTouchSequences.remove(ptrId);
        showPreview(describeChord(),0,mWidth/2,mHeight/2);
    }


    private static class Sequence {
        int startKey;
        int curKey;
        boolean dead;
        Sequence(int k) {
            curKey = startKey = k;
        }
    }

    String describeChord() {
        SortedSet<Integer> starts = new TreeSet<Integer>();
        SortedSet<Integer> hovers = new TreeSet<Integer>();
        for(Sequence s : mTouchSequences.values()) {
            if(s.startKey >= 0) {
                starts.add(s.startKey);
            }
            if(s.curKey >= 0) {
                hovers.add(s.curKey);
            }
        }
        hovers.removeAll(starts);
        StringBuilder desc = new StringBuilder();
        for(int s: starts) {
            desc.append(keyName(s));
        }
        if(!hovers.isEmpty()) {
            desc.append("/");
            for(int h: hovers) {
                desc.append(keyName(h));
            }
        }
        return desc.toString();
    }

    char keyName(int keyId) {
        return (char)('a' + keyId);
    }



}

