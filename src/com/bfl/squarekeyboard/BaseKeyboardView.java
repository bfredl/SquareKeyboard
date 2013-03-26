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

public abstract class BaseKeyboardView extends View {
    int mWidth, mHeight;
    boolean mNeedsDraw = true;
    Paint mTextPaint, mBackgroundPaint, mBorderPaint;
    Paint mActivePaint, mLatchedPaint, mAltTextPaint, mSmallTextPaint;
    TextView mPreviewView;
    Drawable mNormalPopupBackground;
    Drawable mSwipedPopupBackground;
    PopupWindow mPreviewWindow;
    final int mPreviewPadW = 4;
    final int mPreviewHeight = 40;
    
    private final String TAG = "SquareKeyboardView";
    public BaseKeyboardView(Context context) {
        super(context);
        construct();
    }

    public BaseKeyboardView(Context context, AttributeSet attrs ) {
        super(context, attrs);
        construct();
    }

    public BaseKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        construct();
    }

    private void construct() {
        LayoutInflater inflater = (LayoutInflater)
            getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        //mTextPaint.setTypeface(Typeface.MONOSPACE);
        mTextPaint.setTextSize(13);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setARGB(255,255,255,255);
        mAltTextPaint = new Paint(mTextPaint);
        mAltTextPaint.setARGB(255,128,128,128);
        mSmallTextPaint = new Paint(mTextPaint);
        mSmallTextPaint.setTextSize(11);


        // xmlify this
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setARGB(255,16,16,16);
        mActivePaint = new Paint();
        mActivePaint.setARGB(255,16,130,200);
        mLatchedPaint = new Paint();
        mLatchedPaint.setARGB(255,70,160,00);
        mBorderPaint = new Paint();
        mBorderPaint.setARGB(255,0,130,180);
        //mBorderPaint.setAntiAlias(true);

        mPreviewWindow = new PopupWindow(getContext());

        mPreviewView = (TextView) inflater.inflate(R.layout.popup, null);
        mPreviewWindow.setContentView(mPreviewView);
        mPreviewWindow.setBackgroundDrawable(null);
        Resources res = getContext().getResources();
        mNormalPopupBackground = res.getDrawable(R.drawable.popupback);
        mSwipedPopupBackground = res.getDrawable(R.drawable.popupback_swiped);
    }

    protected void drawKey(Canvas c, float x0, float x1, float y0, float y1, String label, String altlabel, Paint bkgStyle) {
        c.drawRect(x0,y0,x1,y1,bkgStyle);
        if( altlabel.equals(label) || altlabel.equals("")) {
            Paint paint;
            if(label.length() > 2) {
                paint = mSmallTextPaint;
            } else {
                paint = mTextPaint;
            }
            c.drawText(label,(x0+x1)/2,(y0+y1)/2+5,paint);
        } else {
            c.drawText(label,(3*x0+x1)/4, (4*y1+y0)/5,mTextPaint);
            c.drawText(altlabel,(1*x0+4*x1)/5,(3*y0+1*y1)/4+5,mAltTextPaint);
        }
    }


    public void updateSize() {
        mWidth = getWidth();
        mHeight = getHeight();
        forceDraw();
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateSize();
    }

    void forceDraw() {
        //mNeedsDraw = true;
        invalidate();
    }

    protected void showPreview(String label, int state, int x, int y) {
        mPreviewView.setText(label);
        // not like this!!
        if(state == SquareKeyboard.KEYSTATE_SWIPED) {
            Log.d(TAG, "swiped1!");
            mPreviewView.setBackgroundDrawable(mSwipedPopupBackground);
        } else {
            mPreviewView.setBackgroundDrawable(mNormalPopupBackground);
        }

        int size; 
        if(label.length() >= 4) {
            size = 14;
        } else if(label.length() >= 2) {
            size = 17;
        } else {
            size = 21;
        }
        mPreviewView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mPreviewView.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = mPreviewView.getMeasuredWidth() + mPreviewPadW; 
        showPreviewWindowAt(x-popupWidth/2, y, popupWidth, mPreviewHeight);
    }
        
    private void showPreviewWindowAt(int x, int y, int w, int h) {
         if (mPreviewWindow.isShowing()) {
             // FIXME: text placing wrong if not moving
             // force move as temporary workaround
            mPreviewWindow.update(x+1, y, w, h); 
            mPreviewWindow.update(x, y, w, h);
        } else {
            mPreviewWindow.setWidth(w);
            mPreviewWindow.setHeight(h);
            mPreviewWindow.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
        }
        mPreviewView.setVisibility(VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        String action = null;
        if(a == ev.ACTION_MOVE) {
            //action = "MOVE";
            //FIXME
            onTouchMove(0,ev.getX(),ev.getY());
        } else if(a == ev.ACTION_CANCEL) {
            action = "CANCEL";
            onTouchUp(0,-1f,-1f);
        } else if(a == ev.ACTION_OUTSIDE) {
            action = "OUTSIDE";
        } else {
            int idx =ev.getActionIndex() ; 
            int id = ev.getPointerId(idx);
            float x = ev.getX(idx);
            float y = ev.getY(idx);
            if( a == ev.ACTION_DOWN) {
                action  = "DOWN";
                onTouchDown(id,x,y);
            } else if( a == ev.ACTION_POINTER_DOWN) {
                action  = "PTR_DOWN";
                onTouchDown(id,x,y);
            } else if(a == ev.ACTION_UP) {
                action = "UP";
                onTouchUp(id,x,y);
            } else if( a == ev.ACTION_POINTER_UP) {
                action  = "PTR_UP";
                onTouchUp(id,x,y);
            } else {
                return false;
            }
            if(action != null) {
                Log.d(TAG, "touch " + action + " " + id + " " + x+ " " + y);
            }
        }

        return true;
    }
    // no MyVerySpecialAbstractTouchGestureListenerInterface for you...
    abstract void onTouchDown(int id, float x, float y);
    abstract void onTouchMove(int id, float x, float y);
    abstract void onTouchUp(int id, float x, float y);
}
