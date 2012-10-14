package com.bfl.squarekeyboard;
import android.view.View;
import android.view.KeyEvent;
import android.content.Context;
import android.util.AttributeSet;

public class SquareKeyboardView extends View {

    int mRows, mCols;
    int mRowHeight;

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

    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(10, mRowHeight*mRows);
    }

   /*@Override
   public void onDraw(Canvas c) {

   }*/

    public interface ActionListener {
        void onKey(char ch);
        void onText(CharSequence text);
        void onSpecialKey(int keyCode, KeyEvent event);
    }
}
