package com.bfl.squarekeyboard;
import android.view.View;
import android.content.Context;
import android.util.AttributeSet;

public class SquareKeyboardView extends View {

    public SquareKeyboardView(Context context) {
        super(context);
    }

    public SquareKeyboardView(Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    public SquareKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(10, 100);
    }
}
