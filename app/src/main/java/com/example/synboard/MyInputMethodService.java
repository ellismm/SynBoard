package com.example.synboard;

import android.content.Context;
import android.inputmethodservice.*;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.inputmethod.InputConnection;
import android.os.Vibrator;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    private KeyboardView kv;
    private Keyboard keyboard, keyboard2, numbers_layout;

    private boolean caps = false;
    boolean two = false;


    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_layout, null);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboard2 = new Keyboard(this, R.xml.keys_layout2);
        numbers_layout = new Keyboard(this, R.xml.keys_layout_numbers);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        return kv;
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
        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibe.vibrate(10);
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

                    if(two) {
//                        keyboard = new Keyboard(this, R.xml.keys_layout);
                        System.out.println("I am here: ");
                        kv.setKeyboard(numbers_layout);
                        two = false;
                    }
                    else {
//                        keyboard = new Keyboard(this, R.xml.keys_layout2);
                        System.out.println("I am here too: ");
                        kv.setKeyboard(keyboard2);
                        two = true;
                    }

                    kv.setOnKeyboardActionListener(this);
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    kv.invalidateAllKeys();
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

