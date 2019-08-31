package com.example.synboard;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.*;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.VibrationEffect;
import android.support.annotation.RequiresApi;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.os.Vibrator;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.TextInfo;
import android.util.Log;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.sql.SQLOutput;
import java.util.*;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener,
                SpellCheckerSession.SpellCheckerSessionListener {
    private KeyboardView kv;
    private Keyboard keyboard, keyboard2, numbers_layout;
    private InputMethodManager mInputMethodManager;
    private CandidateView mCV;
    private CompletionInfo[] mCompletions;
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private boolean mCapsLock;

    private Keyboard mQwertyKeyboard;
    private Keyboard mCurrKeyboard;
    private KeyboardView mInputView;

    private SpellCheckerSession mScs;

    private String mWordSeparators;

    private long mMetaState;


    private StringBuilder mComposing = new StringBuilder();

    private List<String> mSuggestions;

    private boolean caps = false;
//    int two = 1;


    @Override
    public void onCreate() {
        super.onCreate();;
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        final TextServicesManager tsm = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        mScs = tsm.newSpellCheckerSession(null, null, this, true);
    }
    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_layout, null);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboard2 = new Keyboard(this, R.xml.keys_layout2);
        numbers_layout = new Keyboard(this, R.xml.keys_layout_numbers);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        setCandidatesViewShown(true);
        return kv;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        mComposing.setLength(0);
        updateCandidates();

        if(!restarting) {
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        switch(attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                mCurrKeyboard = keyboard2;
                break;

            case InputType.TYPE_CLASS_PHONE:
                mCurrKeyboard = keyboard2;
                break;

            case InputType.TYPE_CLASS_TEXT:
                mCurrKeyboard = keyboard;
                mPredictionOn = true;

                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;

                if(variation == InputType.TYPE_TEXT_VARIATION_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    mPredictionOn = false;
                }

                if(variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == InputType.TYPE_TEXT_VARIATION_URI ||
                        variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    mPredictionOn = false;
                }

                if((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                break;

            default:
                mCurrKeyboard = keyboard;
                updateShiftKeyState(attribute);
        }
    }

    @Override
    public View onCreateCandidatesView() {
        mCV = new CandidateView(this);
        mCV.setService(this);
        return mCV;
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        kv.setKeyboard(keyboard);
        kv.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        pickDefaultCandidates();
//        kv.setSubtypeOnSpaceKey(subtype);
    }

    public MyInputMethodService() {

    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    public boolean isWordSeparator(int code) {
        String separators = mWordSeparators;
        return separators.contains(String.valueOf((char) code));

    }

    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void sendKey(int keyCode) {
        switch(keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if(keyCode >= '0' & keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                }
                else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    @Override
    public void onKey(int i, int[] ints) {
        Log.d("test", "KECODE: " + i);
        InputConnection inputConnection = getCurrentInputConnection();
        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibe.vibrate(10);
        if(inputConnection != null) {
//            if (isWordSeparator(i)) {
//                if (mComposing.length() > 0) {
//                    commitTyped(getCurrentInputConnection());
//                }
//
//                sendKey(i);
//            }
            if(mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            switch(i) {

                case Keyboard.KEYCODE_DELETE:
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if(TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 1);
                    }
                    else {
                        inputConnection.commitText("",1);
                    }
                    updateCandidates();
                    break;

                case Keyboard.KEYCODE_MODE_CHANGE:
                    kv.setKeyboard(keyboard);
                    kv.setOnKeyboardActionListener(this);
                    break;

                case Keyboard.KEYCODE_CANCEL:
                    kv.setKeyboard(keyboard2);
                    kv.setOnKeyboardActionListener(this);
                    break;

                case Keyboard.KEYCODE_ALT:
                    kv.setKeyboard(numbers_layout);
                    kv.setOnKeyboardActionListener(this);
                    break;

                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    kv.invalidateAllKeys();
//                    handleShift();
                    break;

                case Keyboard.KEYCODE_DONE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    break;

                default:
                    char code = (char) i;
                    if(Character.isLetter(code) && caps)
                        code = Character.toUpperCase(code);
                    inputConnection.commitText(String.valueOf(code), 1);
                    updateCandidates();
                    break;
            }

        }
    }


    private void pickDefaultCandidates() {
        pickSuggestionManually(0);
    }
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCV != null) {
                mCV.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {

            if (mPredictionOn && mSuggestions != null && index >= 0) {
                mComposing.replace(0, mComposing.length(), mSuggestions.get(index));
            }
            commitTyped(getCurrentInputConnection());

        }
    }

    private void updateShiftKeyState(EditorInfo attr) {
        if(attr != null && keyboard == kv.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if(ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            kv.setShifted(mCapsLock || caps != 0);
        }
    }

    private void commitTyped(InputConnection inputConnection) {
        if(mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateCandidates() {
        if(!mCompletionOn) {
            if(mComposing.length() > 0 ) {
                ArrayList<String> list = new ArrayList<String>();
                Log.d("softkeyboard", "Requesting: " + mComposing.toString());
                mScs.getSentenceSuggestions(new TextInfo[] {new TextInfo(mComposing.toString())}, 5);
                setSuggestions(list, true, true);

            }
            else {
                setSuggestions(null, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions, boolean typedWordValid) {
        if(suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);

        }
        else if(isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        mSuggestions = suggestions;
        if(mCV != null) {
            Log.d("myInputMethodService", "correct this function is called");
            mCV.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if(mCompletionOn) {
            mCompletions = completions;
            if(completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for(int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if(ci != null) {
                    stringList.add(ci.getText().toString());
                }

            }

            setSuggestions(stringList, true, true);
        }
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {

        System.out.println("Im in the onGetSuggestions function");
        final StringBuilder sb = new StringBuilder();

        for(int i = 0; i < results.length; ++i) {
            final int len = results[i].getSuggestionsCount();
            sb.append('\n');

            for(int j = 0; j < len; ++j) {
                sb.append("," + results[i].getSuggestionAt(j));
            }

            sb.append(" (" + len + ")");
        }
        Log.d("softkeyboard", "SUGGESTIONS: " + sb.toString());
//        setSuggestions(sb, true, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        System.out.println("Im in the onGetSentenceSuggestions function");

        Log.d("SoftKeyboard", "OnGetSentenceSuggestions");
        final List<String> sb = new ArrayList<>();
        for(int i = 0; i < results.length; ++i) {
            final SentenceSuggestionsInfo ssi = results[i];
            for(int j = 0; j < ssi.getSuggestionsCount(); ++j) {
                dumpSuggestionsInfoInternal(sb, ssi.getSuggestionsInfoAt(j), ssi.getOffsetAt(j), ssi.getLengthAt(j));
            }
        }

        Log.d("MyinputMethodService", "Suggestions" + sb.toString());
        setSuggestions(sb, true, true);
    }

    private void dumpSuggestionsInfoInternal(final List<String> sb, final SuggestionsInfo si, final int length, final int offset) {
        final int len = si.getSuggestionsCount();

        for(int i = 0; i < len; ++i) {
            sb.add(si.getSuggestionAt(i));
        }
    }

    private void handleShift() {
        if(kv == null) {
            return;
        }

        Keyboard cKeyboard = kv.getKeyboard();
        if(keyboard == cKeyboard) {
            kv.setShifted(mCapsLock || !kv.isShifted());
        }
    }


    @Override
    public  void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {
        Log.d("SoftKeyboard", "Swipe Left");
        if(mCompletionOn || mPredictionOn) {
            pickDefaultCandidates();
        }
    }

    @Override
    public void swipeDown() {

    }

    @Override
    public  void swipeUp() {

    }

//    @Override
//    public void onComputeInsets(InputMethodService.Insets outInsets) {
//        super.onComputeInsets(outInsets);
//        if (!isFullscreenMode()) {
//            outInsets.contentTopInsets = outInsets.visibleTopInsets;
//        }
//    }
}

