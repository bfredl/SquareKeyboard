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

public class SquareKeyboardView extends View {

    private final String TAG = "SquareKeyboardView";
    SquareKeyboard mKeyboard;
    int mRows, mCols;
    int mRowHeight, mColWidth;
    int mLineThickness = 1;

    int mWidth, mHeight;
    Canvas mCanvas;
    Bitmap mBuffer;
    Paint mTextPaint, mBackgroundPaint, mBorderPaint;
    Paint mActivePaint, mLatchedPaint, mAltTextPaint, mSmallTextPaint;
    boolean mNeedsDraw = true;

    float mStartX, mStartY;
    int mActiveI = -1, mActiveJ = -1;

    int mActiveDir = 0;
    int sweepTreshold = 50;


    TextView mPreviewView;
    Drawable mNormalPopupBackground;
    Drawable mSwipedPopupBackground;
    PopupWindow mPreviewWindow;
    final int mPreviewPadW = 4;
    final int mPreviewHeight = 40;

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

        LayoutInflater inflater = (LayoutInflater)
            getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRows = 1;
        mCols = 1;
        mRowHeight = 30;

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTypeface(Typeface.MONOSPACE);
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

        mPreviewWindow = new PopupWindow(getContext());

        mPreviewView = (TextView) inflater.inflate(R.layout.popup, null);
        mPreviewWindow.setContentView(mPreviewView);
        mPreviewWindow.setBackgroundDrawable(null);
        Resources res = getContext().getResources();
        mNormalPopupBackground = res.getDrawable(R.drawable.popupback);
        mSwipedPopupBackground = res.getDrawable(R.drawable.popupback_swiped);
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
        mPreviewView.setText(label);
        int state = mKeyboard.getKeyState(mActiveI,mActiveJ,mActiveDir);
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
        int x = mColWidth*mActiveJ-(popupWidth-mColWidth)/2;
        int y = mRowHeight*(mActiveI-2)-mRowHeight/2;
        showPreviewAt(x, y, popupWidth, mPreviewHeight);
        mPreviewView.setVisibility(VISIBLE);
    }

    private void showPreviewAt(int x, int y, int w, int h) {
         if (mPreviewWindow.isShowing()) {
             // FIXME: text placing wrong if not moving
             // force move as temporary workaround
            mPreviewWindow.update(x+1, y, w, h); 
            mPreviewWindow.update(x, y, w, h);
        } else {
            mPreviewWindow.setWidth(w);
            mPreviewWindow.setHeight(h);
            mPreviewWindow.showAtLocation(this, Gravity.NO_GRAVITY,
                    x, y);
        }
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

        //Log.d(TAG, "touch " + action + " " + ev.getX() + " " + ev.getY());
        return true;
    }

    void onMovePos(float x, float y) {
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

    void onTouchUp(float x, float y) {
        int j = xToJ(x), i = yToI(y), dir = calcAngle(x-mStartX,y-mStartY);
        if( i < 0 || j < 0) return;
        // FIXME: multitouch still a bit buggy, can commit unpreviewed key
        mKeyboard.onKeyPress(i,j,dir);
        onMovePos(-1,-1);
    }


}

