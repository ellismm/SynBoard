package com.example.synboard;

import android.inputmethodservice.*;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.inputmethod.InputConnection;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    private KeyboardView keyboardView;
    private Keyboard keyboard;

    private boolean caps = false;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_layout, null);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        return keyboardView;
    }

    public MyInputMethodService() {

    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    @Override
    public void onKey(int i, int[] ints) {
        InputConnection inputConnection = getCurrentInputConnection();
        if(inputConnection != null) {
            switch(i) {
                case Keyboard.KEYCODE_DELETE:
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if(TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 0);
                    }
                    else {
                        inputConnection.commitText("",1);
                    }
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    keyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    break;
                default:
                    char code = (char) i;
                    if(Character.isLetter(code) && caps)
                        code = Character.toUpperCase(code);
                    inputConnection.commitText(String.valueOf(code), 1);
            }

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

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public  void swipeUp() {

    }


}

