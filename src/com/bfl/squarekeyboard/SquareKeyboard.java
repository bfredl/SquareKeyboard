package com.bfl.squarekeyboard;
import android.view.KeyEvent;
public class SquareKeyboard {
    private final String TAG = "GestureKeyboard";

    protected int mRows, mCols;

    SquareKeyboardView mView = null;
    ActionListener mListener;
    Key[][] mLayout;


    static private abstract class Key {
        String label;
        abstract void onPress();
    }

    private class TypeKey extends Key {
        String text;

        TypeKey(String label, String text) {
            this.label = label;
            this.text = text;
        }
        
        void onPress() {
            mListener.onText(text);
        }

    }

    public SquareKeyboard(ActionListener listener) {
        mListener = listener;
        mRows = 4;
        mCols = 10;
    }

    public void setView(SquareKeyboardView view) {
        mView = view;
    }



    public interface ActionListener {
        void onKey(char ch);
        void onText(CharSequence text);
        void onSpecialKey(int keyCode, KeyEvent event);
    }

    public int getRows() {
        return mRows;
    }

    public int getCols() {
        return mCols;
    }

    public String getKeyLabel(int x, int y) {
        return String.valueOf((char)(32+x*mCols+y));
    }

    public void onKeyPress(int x, int y) {
        mListener.onText(String.valueOf((char)(32+y*mCols+x)));
    }


}
