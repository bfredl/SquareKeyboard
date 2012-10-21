package com.bfl.squarekeyboard;
import android.view.KeyEvent;
import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import static android.view.KeyEvent.*;
import java.util.Map;
import java.util.HashMap;
public class SquareKeyboard {
    private final String TAG = "SquareKeyboard";

    protected int mRows, mCols;

    SquareKeyboardView mView = null;
    ActionListener mListener;
    

    protected Map<String,Layout> mLayouts = new HashMap<String,Layout>();
    Layout mCurLayout;
    boolean mShiftState = false;
    protected class Layout {
        Key[][] map;
        Layout() {
            map = new Key[mRows][mCols];
        }
        public void setKey(int r, int c, Key k) {
            map[r][c] = k;
        }
    }

    protected Layout createLayout(String name) {
        Layout l = new Layout();
        mLayouts.put(name,l);
        return l;
    }


    static protected abstract class Key {
        String label;
        abstract void onPress();

        String getLabel() {
            return label;
        }
    }

    protected class TypeKey extends Key {
        TypeKey(String text) {
            this.label = text;
        }
        
        void onPress() {
            mListener.onText(getLabel());
        }

        String getLabel() {
            return doShift(label);
        }

    }
    protected class SpecialKey extends Key {
        KeyEvent ev;

        SpecialKey(String label, KeyEvent ev) {
            this.label = label;
            this.ev = KeyEvent.changeFlags(ev, ev.getFlags() | KeyEvent.FLAG_SOFT_KEYBOARD);
        }
        
        void onPress() {
            mListener.onSpecialKey(ev.getKeyCode(), ev);
        }

    }

    protected class ShiftKey extends Key {
        ShiftKey() {
            this.label = "sh";
        } 
        void onPress() {
            mShiftState = ! mShiftState;
            mView.invalidateAllKeys();
        }

    }

    protected class SetLayoutKey extends Key {
        String layout;
        SetLayoutKey(String label, String layout) {
            this.label = label;
            this.layout = layout;
        } 
        void onPress() {
            mCurLayout = mLayouts.get(layout);
            mView.invalidateAllKeys();
        }

    }


    protected String doShift(String s) {
        if(mShiftState) {
            return s.toUpperCase();
        } else {
            return s;
        }
    }

    public Key getSpecialKey(String code) {
        code = code.intern();
        Key key = null;
        if( code == "BKSP" ) {
            key = new SpecialKey("<-",new KeyEvent(ACTION_DOWN,KEYCODE_DEL));
        } else if( code == "RET" ) {
            key = new SpecialKey("R",new KeyEvent(ACTION_DOWN,KEYCODE_ENTER));
        } else if( code == "SHFT" ) {
            key = new ShiftKey();
        } else if( code == "SYM" ) {
            key = new SetLayoutKey("sym","sym");
        } else if( code == "TXT" ) {
            key = new SetLayoutKey("txt","main");
        } else {
            throw new RuntimeException("INvalid code: " + code);
        }

        return key;
    }



    public SquareKeyboard(ActionListener listener) {
        mListener = listener;
        setSize(1,1);
    }


    public void loadFile(String filename) {
        MapFileReader fr;
        try {
            fr = new MapFileReader(new FileReader(filename),this);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        fr.parseFile();
        mCurLayout = mLayouts.get("main");
    }

    public void setView(SquareKeyboardView view) {
        mView = view;
    }


    public int getRows() {
        return mRows;
    }

    public int getCols() {
        return mCols;
    }

    public String getKeyLabel(int r, int c) {
        Key k = mCurLayout.map[r][c];
        if( k == null) 
            return "";
        return k.getLabel();
    }

    public void onKeyPress(int r, int c) {
        Key k = mCurLayout.map[r][c];
        if( k != null) 
            k.onPress();
    }

    public interface ActionListener {
        void onKey(char ch);
        void onText(CharSequence text);
        void onSpecialKey(int keyCode, KeyEvent event);
    }

    void setSize(int rows, int cols) {
        mRows = rows;
        mCols = cols;
    }

}

class MapFileReader extends StreamTokenizer {
    private static final String TAG = "MapFileReader";
    private SquareKeyboard mTarget;

    MapFileReader(Reader r, SquareKeyboard target) {
        super(r);
        mTarget = target;
        //resetSyntax();
        eolIsSignificant(true);
        whitespaceChars(' ', ' ');
        whitespaceChars('\t', '\t');
        wordChars('a','z');
        wordChars('A','Z');
        ordinaryChar('.');
        ordinaryChar('-');
        ordinaryChar('/'); //meh
        ordinaryChar('\\'); //meh
        quoteChar('"');
        commentChar('#');
    }

    void fail() {
        throw new RuntimeException("Parse error on line " + lineno());
    }

    void nextTok() {
        try {
            nextToken();
        } catch(IOException e ) {
            throw new RuntimeException(e);
        }
    }

    void parseFile() {
        while(true) {
            nextTok();
            if( ttype == TT_EOL) {
                continue;
            } else if(ttype == TT_EOF) {
                break;
            } else if(parseLine()) {
                continue;
            } else {
                fail();
            }

        }
    }

    // reuses
    boolean parseLine() {
        if(ttype != TT_WORD) {
            return false;
        }
        sval = sval.intern();
        if(sval == "size") {
            parseSize();
        } else if(sval == "layout") {
            parseLayout();
        } else {
            fail();
        }
        return true;
    }


    void parseSize() {
        parseNumbers();
        nextTok();
        if( ttype != TT_NUMBER) 
            fail();
        int rows = (int) nval;
        nextTok();
        if( ttype != TT_NUMBER) 
            fail();
        int cols = (int) nval;
        mTarget.setSize(rows,cols);
    }


    void parseLayout() {
        nextTok();
        if( ttype != TT_WORD)
            fail();
        String name = sval;
        SquareKeyboard.Layout l = mTarget.createLayout(name);
        nextTok();
        if( ttype != '{') 
            fail();
        nextTok();
        if( ttype != TT_EOL)
            fail();
        int rows = mTarget.getRows(), cols = mTarget.getCols();
        ordinaryChars('0','9');
        nextTok(); // we lie ahead
        for(int r = 0; r < rows; r++) {
            for(int c = 0; c < cols; c++) {
                SquareKeyboard.Key key = null;
                if( ttype == TT_EOL) {
                    break; 
                } else if( ttype == TT_EOF) {
                    fail(); 
                } else if(ttype == TT_WORD) {
                    if(sval.length() >= 2) {
                        key = mTarget.getSpecialKey(sval);
                    } else {
                        key = mTarget.new TypeKey(sval);
                    }
                } else if(ttype == '"') {
                    key = mTarget.new TypeKey(sval);
                } else {
                    String txt = String.valueOf((char)ttype);
                    key = mTarget.new TypeKey(txt);
                }
                l.setKey(r,c,key);
                nextTok(); 
            }
            if( ttype != TT_EOL) 
                fail();
            nextTok();
        }
        if( ttype != '}') 
            fail();
    }
                         
}

