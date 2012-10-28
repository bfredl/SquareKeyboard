package com.bfl.squarekeyboard;
import android.view.KeyEvent;
import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import static android.view.KeyEvent.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import android.util.Log;
import java.util.ArrayList;
public class SquareKeyboard {
    private final String TAG = "SquareKeyboard";

    protected int mRows, mCols;

    SquareKeyboardView mView = null;
    ActionListener mListener;


    protected Map<String,Key[][]> mLayouts = new HashMap<String,Key[][]>();
    protected Map<String,State> mStates = new HashMap<String,State>();

    State mState;
    List<Layout> mDeadLayout;
    int mActiveDeadKey = -1;

    final int SHIFT_CAPS = 1;
    final int SHIFT_CTRL = 2;
    protected class Layout {
        Key[][] map;
        int shiftstate;
        Layout(Key[][] map, int shiftstate) {
            this.map = map;
            this.shiftstate = shiftstate;
        }
        public Key getKey(int i, int j) {
            if(map[i][j] != null) {
                return map[i][j].getShifted(shiftstate);
            }
            return null;
        }
    }

    protected Key[][] createLayout(String name) {
        Key[][] l = new Key[mRows][mCols];
        mLayouts.put(name,l);
        return l;
    }
    protected Layout getLayout(String name, int shiftstate) {
        return new Layout(mLayouts.get(name),shiftstate);
    }
    protected static Key getKeyFromLayoutList(List<Layout> layouts, int i, int j) {
        for(Layout l: layouts) {
            Key k = l.getKey(i,j);
            if(k != null) 
                return k;
        }
        return null;
    }

    protected class State {
        List<Layout> layout;
        MetaKey[] sKey = new MetaKey[3+1]; //FIXME HARDCODE
    }

    protected State createState(String name) {
        State s = new State();
        mStates.put(name,s);
        return s;
    }

    protected void setState(String newState) {
        mState = mStates.get(newState);
        mActiveDeadKey = -1;
        mDeadLayout = null;
        if(mView != null) 
            mView.invalidateAllKeys();
    }

    static protected abstract class Key {
        String label;
        abstract void onPress();

        String getLabel() {
            return label;
        }

        Key getShifted(int shiftstate) {
            return this;
        }
    }

    protected class TypeKey extends Key {
        String text;
        TypeKey(String text) {
            this(text,text);
        }

        TypeKey(String label, String text) {
            this.label = label;
            this.text = text;
        }

        boolean isCtrlable() {
            return label.matches("[a-zA-Z]");
        }

        Key getShifted(int shiftstate) {
            if(shiftstate == SHIFT_CAPS) {
                return new TypeKey(text.toUpperCase());
            } else if(shiftstate == SHIFT_CTRL && isCtrlable()) {
                char val = text.toUpperCase().charAt(0);
                return new TypeKey("^" + val,
                        String.valueOf((char)(val - 'A' + 1)));
            }
            return this;
        }

        void onPress() {
            mListener.onText(text);
            if(mDeadLayout != null) {
                mActiveDeadKey = -1;
                mDeadLayout = null;
                mView.invalidateAllKeys();
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

    abstract protected static class MetaKey extends Key{
        abstract void onSecondPress();
    }

    protected class DeadKey extends MetaKey {
        List<Layout> deadLayout;
        DeadKey(String label, List<Layout> deadLayout) {
            this.label = label;
            this.deadLayout = deadLayout;
        } 
        void onPress() {
            mDeadLayout = deadLayout;
            mView.invalidateAllKeys();
        }
        void onSecondPress() {
            mDeadLayout = null;
            mView.invalidateAllKeys();
        }

    }

    protected class StateKey extends MetaKey {
        String newState;
        boolean dead;
        StateKey(String label, String newState, boolean dead) {
            this.label = label;
            this.newState = newState;
            this.dead = dead;
        } 
        void onPress() {
            if(dead) {
                mDeadLayout = mStates.get(newState).layout;
                mView.invalidateAllKeys();
            } else {
                setState(newState);
            }
        }
        void onSecondPress() {
            setState(newState);
        }
    }

    protected class MetaKeyPlaceholder extends Key {
        int id;
        MetaKeyPlaceholder(int id) {
            this.id = id;
        } 
        String getLabel() {
            return mState.sKey[id].getLabel();

        }
        void onPress() {
            if(mActiveDeadKey != id) {
                mActiveDeadKey = id;
                mState.sKey[id].onPress();
            } else {
                mActiveDeadKey = -1;
                mState.sKey[id].onSecondPress();
            }

        }

    }

    public Key getSpecialKey(String code) {
        code = code.intern();
        Key key = null;
        if( code == "BKSP" ) {
            key = new SpecialKey("\u21a4",new KeyEvent(ACTION_DOWN,KEYCODE_DEL));
        } else if( code == "RET" ) {
            key = new SpecialKey("\u21B5",new KeyEvent(ACTION_DOWN,KEYCODE_ENTER));
        } else if( code == "LEFT" ) {
            key = new SpecialKey("\u2190",new KeyEvent(ACTION_DOWN, 	KEYCODE_DPAD_LEFT));
        } else if( code == "RIGHT" ) {
            key = new SpecialKey("\u2192",new KeyEvent(ACTION_DOWN, 	KEYCODE_DPAD_RIGHT));
        } else if( code == "UP" ) {
            key = new SpecialKey("\u2191",new KeyEvent(ACTION_DOWN, 	KEYCODE_DPAD_UP));
        } else if( code == "DOWN" ) {
            key = new SpecialKey("\u2193",new KeyEvent(ACTION_DOWN, 	KEYCODE_DPAD_DOWN));
        } else if( code == "TAB" ) {
            key = new SpecialKey("\u21B9",new KeyEvent(ACTION_DOWN, 	KEYCODE_TAB));
        } else if( code == "ESC" ) {
            key = new SpecialKey("Esc",new KeyEvent(ACTION_DOWN, 	KEYCODE_ESCAPE));
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
            fr = new MapFileReader(new FileReader(filename));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        fr.parseFile();
        setState("main");
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

    public Key getKey(int i, int j) {
        List<Layout> l;
        if(mDeadLayout != null) {
            l = mDeadLayout;
        } else {
            l = mState.layout;
        }
        return getKeyFromLayoutList(l, i, j);
    }

    public String getKeyLabel(int r, int c) {
        Key k = getKey(r,c);
        if( k == null) 
            return "";
        return k.getLabel();
    }

    public void onKeyPress(int r, int c) {
        Key k = getKey(r,c);
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

    private class MapFileReader extends StreamTokenizer {
        private static final String TAG = "MapFileReader";

        MapFileReader(Reader r) {
            super(r);
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
            setSize(rows,cols);
        }


        void parseLayout() {
            nextTok();
            if( ttype != TT_WORD)
                fail();
            String name = sval;
            Key[][] l = createLayout(name);
            nextTok();
            if( ttype != '{') 
                fail();
            nextTok();
            if( ttype != TT_EOL)
                fail();

            ordinaryChars('0','9');
            wordChars('0','9');
            nextTok(); // we lie ahead
            for(int r = 0; r < mRows; r++) {
                for(int c = 0; c < mCols; c++) {
                    Key key = null;
                    if( ttype == TT_EOL) {
                        break; 
                    } else if( ttype == TT_EOF) {
                        fail(); 
                    } else if(ttype == TT_WORD) {
                        if(sval.length() >= 2) {
                            key = getSpecialKey(sval);
                        } else {
                            key = new TypeKey(sval);
                        }
                    } else if(ttype == '"') {
                        key = new TypeKey(sval);
                    } else {
                        String txt = String.valueOf((char)ttype);
                        key = new TypeKey(txt);
                    }
                    l[r][c] = key;
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
            Log.d(TAG, "state " + name);
            State s = createState(name);
            nextTok();
            if( ttype != '{') 
                fail();
            nextTok();
            if( ttype != TT_EOL)
                fail();
            nextTok();
            while(true) {
                if( ttype == TT_EOL) {
                    nextTok();
                    continue;
                } else if( ttype == '}') {
                    break;
                } else if( ttype != TT_WORD) {
                    fail();
                }
                String cmd = sval.intern();
                nextTok();
                if( cmd == "layout") {
                    s.layout = parseLayoutList();
                } else if(cmd.matches("S[0-9]+")) {
                    int id = Integer.parseInt(cmd.substring(1));
                    String label = sval;
                    nextTok();
                    String type = sval.intern();
                    nextTok();
                    MetaKey key = null;
                    if(type == "dead") {
                        List<Layout> l = parseLayoutList();
                        key = new DeadKey(label,l);
                    } else if(type == "state") {
                        String state = sval;
                        nextTok();
                        key = new StateKey(label,state,false);
                    } else if(type == "deadlatch") {
                        String state = sval;
                        nextTok();
                        key = new StateKey(label,state,true);
                    }
                    s.sKey[id] =key;

                }
            }
        }

        List<Layout> parseLayoutList() {
            List<Layout> l = new ArrayList<Layout>();
            while(true) {
                if( ttype != TT_WORD) 
                    break;
                int shiftstate = 0;
                if(sval.equals("shift")) {
                    shiftstate = SHIFT_CAPS; 
                } else if(sval.equals("ctrl")) { 
                    shiftstate = SHIFT_CTRL; 
                } 

                if(shiftstate != 0) {
                    nextTok();
                    if( ttype != ':') 
                        fail();
                    nextTok();
                }
                if( ttype != TT_WORD) 
                    fail();
                Log.d(TAG, "layout " + sval);
                l.add(getLayout(sval,shiftstate));
                nextTok();
            }
            return l;
        }

    }

}
