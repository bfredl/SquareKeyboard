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
    protected Map<String,State> mStates = new HashMap<String,State>();
    
    State mState, mLastState;
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
    protected Layout getLayout(String name) {
        return mLayouts.get(name);
    }

    protected class State {
        Layout layout;
        String typeState; //FIXME stringly typed
        Key[] sKey = new Key[3+1]; //FIXME HARDCODE
        String postTypeState;
        State() {
        }
    }

    protected State createState(String name) {
        State s = new State();
        mStates.put(name,s);
        return s;
    }

    protected void setState(String newState) {
        if(newState.equals("return")) {
            mState = mLastState;
        } else {
            mLastState = mState;
            mState = mStates.get(newState);
        }
        mView.invalidateAllKeys();
    }

    protected Layout curLayout() {
        return mState.layout;
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

        boolean isCtrlable() {
            return label.matches("[a-zA-Z]");
        }
        
        void onPress() {
            if("ctrl".equals(mState.typeState) && isCtrlable() ) {
                int val = label.toUpperCase().charAt(0);
                mListener.onText(String.valueOf((char)(val - 'A' + 1)));
            } else {
                mListener.onText(getLabel());
            }

            if(mState.postTypeState != null) {
                setState(mState.postTypeState);
            }
        }

        String getLabel() {
            if("shift".equals(mState.typeState)) {
                return label.toUpperCase();
            } else if("ctrl".equals(mState.typeState) && isCtrlable()) {
                return "^" + label.toUpperCase();
            } else {
                return label;
            }
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

    protected class MetaKey extends Key {
        String newState;
        MetaKey(String label, String newState) {
            this.label = label;
            this.newState = newState;
        } 
        void onPress() {
            setState(newState);
        }

    }

    protected class MetaKeyPlaceholder extends Key {
        int id;
        MetaKeyPlaceholder(int id) {
            this.id = id;
        } 
        String getLabel() {
            if(mState.sKey[id] != null) {
                return mState.sKey[id].getLabel();
            } else {
                return mLastState.sKey[id].getLabel();
            }

        }
        void onPress() {
            if(mState.sKey[id] != null) {
                mState.sKey[id].onPress();
            } else {
                // unshift so we don't stack two substates
                mState = mLastState; 
                mLastState.sKey[id].onPress();
            }
        }

    }


    public Key getSpecialKey(String code) {
        code = code.intern();
        Key key = null;
        if( code == "BKSP" ) {
            key = new SpecialKey("<-",new KeyEvent(ACTION_DOWN,KEYCODE_DEL));
        } else if( code == "RET" ) {
            key = new SpecialKey("R",new KeyEvent(ACTION_DOWN,KEYCODE_ENTER));
        } else if( code.matches("S[0-9]+")) {
            key = new MetaKeyPlaceholder(Integer.parseInt(code.substring(1)));
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
        mState = mStates.get("main");
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
        Key k = curLayout().map[r][c];
        if( k == null) 
            return "";
        return k.getLabel();
    }

    public void onKeyPress(int r, int c) {
        Key k = curLayout().map[r][c];
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
        wordChars('_','_');
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
        } else if(sval == "state") {
            parseState();
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
        wordChars('0','9');
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

    void parseState() {
        nextTok();
        if( ttype != TT_WORD)
            fail();
        String name = sval;
        SquareKeyboard.State s = mTarget.createState(name);
        nextTok();
        if( ttype != '{') 
            fail();
        nextTok();
        if( ttype != TT_EOL)
            fail();
        while(true) {
            nextTok();
            if( ttype == TT_EOL) {
                continue;
            } else if( ttype == '}') {
                break;
            } else if( ttype != TT_WORD) {
                fail();
            }
            String cmd = sval.intern();
            nextTok();
            if( cmd == "layout") {
                s.layout = mTarget.getLayout(sval);
            } else if( cmd == "on_type" ) {
                s.typeState = sval; 
            } else if( cmd == "post_type" ) {
                s.postTypeState = sval; 
            } else if(cmd.matches("S[0-9]+")) {
                int id = Integer.parseInt(cmd.substring(1));
                String label = sval;
                nextTok();
                String state = sval;
                s.sKey[id] = mTarget.new MetaKey(label, state);
            }
        }
    }
                         
}

