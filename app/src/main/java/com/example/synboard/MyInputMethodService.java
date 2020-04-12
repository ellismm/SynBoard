package com.example.synboard;

import android.content.Context;
import android.inputmethodservice.*;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.os.Vibrator;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.util.Log;
import android.view.textservice.TextServicesManager;

import java.io.IOException;
import java.security.Key;
import java.util.*;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    /**
     * TODO: Indexoutofbounds error sometimes if trying to get synonyms and the candidate view is
     * pressed more than one time
     * TODO: Implement synonyms so that you don't have to highlight the word an all you have to
     * do is move the cursor inside the desired word
     * TODO: Implement the functionality for autocorrect after hitting the space bar  and what is
     * currently composed is not a word.
     * TODO: Implement multithereading to make the loading faster
     * TODO: Determine what is causing some layouts to move as the user is typing on synboard
     * TODO: Determine why the candidate view doesn't work properly work on groupme app
     */
    private KeyboardView kv;
    private Keyboard mainKeyboard, symKeyboard, numKeyboard, emojiKeyboard;
    private InputMethodManager mInputMethodManager;
    private CandidateView mCV;
    private CompletionInfo[] mCompletions;
    private WordTree wordTree;
    private boolean mPredictionOn;
    private boolean mCompletionOn;

    private boolean spacePress = false; // is space button pressed
    private boolean backSpacePress = false; // is backspace pressed
    private boolean autoCompleted = false; // determine if the last word was autopredicted
    private String autoCompletedWord = ""; // the auto completed word
    private boolean predictedWords = true; //

    private String mWordSeparators;


    private int cursorStart = 0;
    private int cursorEnd = 0;
    private boolean lookForSyns = false;
    private String synWord = "";


    private int caps = 0;
    private Key shift_key_but;


    private StringBuilder composer = new StringBuilder();

    private List<String> mSuggestions;

    DownloadSyns theFile;

    @Override
    public void onCreate() {
        super.onCreate();
        theFile = new DownloadSyns(this);
        try {
            wordTree = new WordTree(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        final TextServicesManager tsm = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
    }
    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_layout, null);
        mainKeyboard = new Keyboard(this, R.xml.keys_layout2);
        symKeyboard = new Keyboard(this, R.xml.symbols_layout2);
        numKeyboard = new Keyboard(this, R.xml.numbers_layout2);
        emojiKeyboard = new Keyboard(this, R.xml.emojis_layout);
        kv.setKeyboard(numKeyboard);
        kv.setOnKeyboardActionListener(this);
        setCandidatesViewShown(false);
        return kv;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        composer.setLength(0);
//        updateSuggestionsCandidates();

        if(!restarting) {
            caps = 0;
        }

        mPredictionOn = false;
        mCompletionOn = true;
        mCompletions = null;

        switch(attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                kv.setKeyboard(numKeyboard);
                kv.setOnKeyboardActionListener(this);

//            case InputType.TYPE_CLASS_DATETIME:
//                break;
//
//            case InputType.TYPE_CLASS_PHONE:
//                );
//                mPredictionOn = false;
//                break;

            case InputType.TYPE_CLASS_TEXT:
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
                int bend = 0;
        }
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {

        if(((oldSelStart > newSelStart && newSelStart > 0) ) && newSelStart > 0) {
            InputConnection inputConnection = getCurrentInputConnection();

//            System.out.println("this.lookForSyns: " + this.lookForSyns);
            this.lookForSyns = true;
//            System.out.println("I'm in onUpdateSelection");
//            System.out.println("oldSelStart: " + oldSelStart);
//            System.out.println("oldSelEnd: " + oldSelEnd);
//            System.out.println("newSelStart: " + newSelStart);
//            System.out.println("newSelEnd: " + newSelEnd);
//            System.out.println("candidatesStart: " + candidatesStart);
//            System.out.println("candidatesEnd: " + candidatesEnd);
//
//            System.out.println("I'm exiting onUpdateSelection");
            String prefix = inputConnection.getTextBeforeCursor(1, 0).toString();
            String postfix = inputConnection.getTextAfterCursor(1, 0).toString();
//            if(postfix.length() > 0) {
//                if (postfix.charAt(0) == ' ')
//                    this.lookForSyns = false;
//            }

            this.cursorStart = 0;
            this.cursorEnd = 0;
            if (prefix.length() > 0) {
                int i = 2;
                while (prefix.charAt(0) != ' ') {
                    prefix = inputConnection.getTextBeforeCursor(i, 0).toString();
                    this.cursorStart++;
                    i++;
                    if (i == 30) {
                        if(oldSelStart <= 0)
                            this.lookForSyns = false;
                        break;
                    }
                }
                if (prefix.charAt(0) == ' ')
                    prefix = prefix.substring(1);
            }
            if (postfix.length() > 0) {
                int i = 2;
                while (postfix.charAt(postfix.length() - 1) != ' ' && this.lookForSyns) {
                    postfix = inputConnection.getTextAfterCursor(i, 0).toString();
                    this.cursorEnd++;
                    i++;
                    if (i == 30) {
                        this.lookForSyns = false;
                    }
                }


                if (postfix.charAt(postfix.length() - 1) == ' ')
                    postfix = postfix.substring(0, postfix.length() - 1);
            }
            this.synWord = prefix + postfix;
            if (lookForSyns)
                updateSynonymsCandidates(newSelStart);
        }
        else {
//            if(mCV != null)
//                mCV.clear();
//            this.autoCompleted = false;
            this.lookForSyns = false;

//            System.out.println("never made it to updating synonyms!!");
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
        kv.setKeyboard(mainKeyboard);
        kv.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        caps = 0;
        handleShift();
        updatePredictionWords();
        System.out.println("this should start up when the keyboard comes up to view...");
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

            char code;
            switch(i) {
                // If the delete button is pressed
                case Keyboard.KEYCODE_DELETE:
                    handleDelete();
                    backSpacePress = true;
                    break;


                //if the space button is pressed
                // I don't know why the key code is set to D
                case KeyEvent.KEYCODE_D:
                    autoCompleted = false;
                    code = (char) i;
                    checkContractionWords();
                    handleSpace();
                    inputConnection.commitText(String.valueOf(code), 1);
                    backSpacePress = false;
                    break;

                // Switching to the main keyboard
                case Keyboard.KEYCODE_MODE_CHANGE:
                    kv.setKeyboard(mainKeyboard);
                    kv.setOnKeyboardActionListener(this);
                    mPredictionOn = true;
                    break;

                    // Switching to the second layout
                case Keyboard.KEYCODE_CANCEL:
                    kv.setKeyboard(symKeyboard);
                    kv.setOnKeyboardActionListener(this);
                    mPredictionOn = false;
                    break;

                // Switching the emoji keyboard
                case KeyEvent.KEYCODE_DVR:
                    kv.setKeyboard(emojiKeyboard);
                    kv.setOnKeyboardActionListener(this);
                    mPredictionOn = false;
                    autoCompleted = false;
                    break;

                    // switching to the numbers layout
                case Keyboard.KEYCODE_ALT:
                    wordTree.refactorDictionary();
                    kv.setKeyboard(numKeyboard);
                    kv.setOnKeyboardActionListener(this);
                    mPredictionOn = false;
                    break;

                    // if the shift key is pressed
                case Keyboard.KEYCODE_SHIFT:
                    handleShift();
                    break;

                    // If the done key is pressed
                case Keyboard.KEYCODE_DONE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    break;

                default:
                    code = (char) i;

                    if(spacePress && !backSpacePress) {
                        composer.setLength(0);
                        spacePress = false;
                    }
                    backSpacePress = false;
                    autoCompleted = false;


//                    if(Character.isLetter(code) || code == '.' )
//                        composer.append(code);
                    if(Character.isLetter(code) && caps != 0) {
//                        System.out.println("to upper case");
                        code = Character.toUpperCase(code);
                    }
                    if(Character.isDigit(code))
                        composer.append(code);
                    if(Character.isLetter(code) || code == '.' )
                        composer.append(code);
//                    if(code == '.')
//                        wordTree.resetLastWord();
                    inputConnection.commitText(String.valueOf(code), 1);
                    if(caps == 1) {
                        caps = 2;
                        handleShift();
                    }
                    updateSuggestionsCandidates();
                    break;
            }

        }
    }


    /**
     * A Method to handle the pressing of the space bar
     * if the last Character was a period then we need to Shift the keyboard to uppercase.
     *
      */
    private void handleSpace() {
        InputConnection inputConnection = getCurrentInputConnection();

        int n = composer.length();
        if(n == 0)
            return;
        //TODO: There mus be a better way  to handle the case of an i
        char last = composer.charAt(n-1);
        // this is automatically capitalize i
        if(last == 'i' && n == 1) {
            handleDelete();
            inputConnection.commitText(String.valueOf('I'), 1);
            return;
        }
        Log.d("space bar: ", "I'm Handling the space bar2: " + composer.toString() + (composer.toString() == "i"));
        // this is to automatically press the shift key
        if(last == '.') {

            if(caps == 2){
                caps = 1;
                handleShift();
            }
            else {
                caps = 0;
                handleShift();
            }
        }
        if(!checkCollection(composer.toString().toLowerCase())
                && !(".?:;!/@").contains(String.valueOf(composer.charAt(composer.length() - 1))))
            pickDefaultCandidates();
        addExtraPoint(composer.toString());
        wordTree.addPredictionWord(composer.toString().toLowerCase());
        if(!checkCollection(composer.toString().toLowerCase())
                && !(".?:;!/@").contains(String.valueOf(composer.charAt(composer.length() - 1))))
            wordTree.resetLastWord();
        updatePredictionWords();
        composer.append(" ");
//        composer.setLength(0);
        spacePress = true;

    }

    /**
     * A Method to handle the pressing of the backspace button
     * if there are no characters to delete then update the shift status to uppercase
     */
    private void handleDelete() {
        InputConnection inputConnection = getCurrentInputConnection();
        if(autoCompleted && !predictedWords) {
//            System.out.println("deleting..");
            inputConnection.deleteSurroundingText(composer.length(), 0);
            composer.replace(0, composer.length(), autoCompletedWord);
            commitTyped(inputConnection);
        }
        else {
            // delete the last character in the composer
            autoCompletedWord = "";
            if (composer.length() > 0) {
                composer.deleteCharAt(composer.length() - 1);
            }

            // Determine what is currently in the text view.
            CharSequence selectedText = inputConnection.getTextBeforeCursor(1, 0);

            if (TextUtils.isEmpty(selectedText)) {
                inputConnection.commitText("", 1);
                if (caps != 2) {
                    caps = 0;
                    handleShift();
                }

            }
            //TODO: The shift update happen if the last character just got deleted.
            else {
//            if(selectedText.length() == 1) {
//                if(caps != 2) {
//                    caps = 0;
//                    handleShift();
//                }
//            }
                inputConnection.deleteSurroundingText(1, 0);
            }
            updateSuggestionsCandidates();
        }

        autoCompleted = false;
    }

    /**
     * A method to handle when the shift key is pressed
     * if caps == 0 then that means the current state of keys is lowercase
     * if caps == 1 then the current state of the keys is uppercase until one key is pressed
     * if caps == 2 then the current state of the keys is uppercase until shift key is pressed
     */
    private void handleShift() {

        // if not in the first keyboard layout then there is no reason to handle the shift state
        if(kv == null) {
            return;
        }

        caps += 1; // Changes the status of the key to the next status
        caps %= 3; // make sure the caps is not greater than 2
//        System.out.println("caps: " + caps);

        // if the key status is 1 or 2 then shift the key to uppercase
        // if the key status is 0 then shift the keys to lowercase
        kv.setShifted(caps != 0);
//        System.out.println("the key for .get(2): " + mainKeyboard.getShiftKeyIndex());
        if(caps == 0) {
            mainKeyboard.getKeys().get(30).icon = ContextCompat.getDrawable(this , R.drawable.shift_lower);
//            kv.setBackgroundResource(R.drawable.round_key);
//            keyboard.getKeys().get(30).label = "CAPS";
        }

        else {
            mainKeyboard.getKeys().get(30).icon = ContextCompat.getDrawable(this, R.drawable.shift_upper);
//            kv.setBackgroundResource(R.drawable.shift_key);
//            keyboard.getKeys().get(30).label = "SPAC";
        }
    }


    /**
     * tail method for pickSuggestionManually
     */
    private void pickDefaultCandidates() {
        int i = mCV.getSuggestionIndex();
        autoCompleted = true;
        if(i >= 0)
            pickSuggestionManually(i);
    }

    /**
     * this method handles choosing a desired word from
     * the candidate view
     * @param index
     */
    public void pickSuggestionManually(int index) {
        InputConnection inputConnection = getCurrentInputConnection();
        System.out.println(inputConnection.getSelectedText(0));

        if(mSuggestions != null)
            addExtraPoint(mSuggestions.get(index));
        if(!lookForSyns) {

            if (mCompletionOn && mCompletions != null && index >= 0
                    && index < mCompletions.length) {
                CompletionInfo ci = mCompletions[index];
                getCurrentInputConnection().commitCompletion(ci);
                if (mCV != null) {
                    mCV.clear();
                }
            } else if (composer.length() > 0) {
                String word = mSuggestions.get(index);
                int len = composer.length();
                if((".?:;!/@").contains(String.valueOf(composer.charAt(composer.length() - 1)))) {
                    word += mSuggestions.get(index) + composer.charAt(composer.length() - 1);
                    wordTree.resetLastWord();
                }
                if(composer.toString().equals(composer.toString().substring(0, 1).toUpperCase() + composer.substring(1)) && !predictedWords) {
                    word = word.substring(0, 1).toUpperCase() + word.substring(1);
                }
                if(autoCompleted)
                    autoCompletedWord = composer.toString();
                composer.replace(0, composer.length(), word);
//                addExtraPoint(mSuggestions.get(index));
                if(!predictedWords) {
                    getCurrentInputConnection().deleteSurroundingText(len, 0);
                }
                commitTyped(getCurrentInputConnection());
                updatePredictionWords();
            }
        }
        else {
            if (mCompletionOn && mCompletions != null && index >= 0
                    && index < mCompletions.length) {
                CompletionInfo ci = mCompletions[index];
                getCurrentInputConnection().commitCompletion(ci);
                if (mCV != null) {
                    mCV.clear();
                }
            } else if (synWord.length() > 0 && mSuggestions.size() > 0) {
                String word = mSuggestions.get(index);
                int len = composer.length();
                if((".?:;!/@").contains(String.valueOf(synWord.charAt(synWord.length() - 1))))
                    word += synWord.charAt(synWord.length() - 1);
                if(synWord.equals(synWord.substring(0, 1).toUpperCase() + synWord.substring(1)))
                    word = word.substring(0, 1).toUpperCase() + word.substring(1);
                composer.replace(0, len, word);
                getCurrentInputConnection().deleteSurroundingText(cursorStart, cursorEnd);
                commitTyped(getCurrentInputConnection());
//                inputConnection.setSelection(cursorStart - mSuggestions.get(index).length() - 1,cursorStart);
//                getCurrentInputConnection().commitText(selectedText, len);
            }

        }
    }

    /**
     * this method will replace a certain string with a desired string
     * composer was replaced from the caller
     * @param inputConnection
     */
    private void commitTyped(InputConnection inputConnection) {
        if(lookForSyns) {
            inputConnection.commitText(composer, 0);
//            composer.setLength(0);
            updateSynonymsCandidates(0);
        }
        else if(composer.length() > 0) {
            if(predictedWords) {
                inputConnection.commitText(composer + " ", composer.length() + 1);
                updatePredictionWords();
            }

            else {

                inputConnection.commitText(composer, composer.length());
                //            composer.setLength(0);
                updateSuggestionsCandidates();

            }
        }
    }

    /**
     * This method updates the candidate view with
     * suggestions of synonyms for the already composed word.
     * If there is no synonyms for the desired word, then
     * direct to updateSuggestionCandidates
     * @param newSelStart
     */
    private void updateSynonymsCandidates(int newSelStart) {
        predictedWords = false;
        InputConnection inputConnection = getCurrentInputConnection();
        System.out.println(inputConnection.getSelectedText(0));
//        String selectedText = null;
//        if(inputConnection.getSelectedText(0) != null) {
//            selectedText = inputConnection.getSelectedText(0).toString();
//
//        }

        if(mCompletionOn && lookForSyns) {
            setCandidatesViewShown(true);
            if(synWord.length() > 0 ) {
                ArrayList<String> list = null;

                boolean check = checkCollection(this.synWord);
                if(check) {
                    composer.replace(0, composer.length(), this.synWord);
//                    String temp = composer.toString();
                    if(wordTree.isWord(this.synWord.toLowerCase())) {
                        setCandidatesViewShown(true);
                        ArrayList<String> theSyns = wordTree.getSyns(this.synWord.toLowerCase());
                        list = theSyns;

                    }
//                    if(list != null && list.size() > 32)
//                        System.out.println("the syns in update candidates: " + list + " : " + this.synWord);
                    if(list == null)
                        setSuggestions(null, false, false);
                    else
                        setSuggestions(list, true, true);
                }
                else {
                    updateSpellingCandidates();
                }

            }

            else {
                setSuggestions(null, false, false);
            }
        }
        else {
            setSuggestions(null, false, false);
        }
    }


    /**
     * This is a method to update the candidate view
     * with suggestions of words based on already composed word.
     * If there is no suggestions then determine if there is any
     * spelling errors
     */
    private void updateSuggestionsCandidates() {
        predictedWords = false;
        if(mCompletionOn) {
            if(composer.length() > 0 ) {
                ArrayList<String> list = new ArrayList<>();

                list = wordTree.suggestions(composer.toString());
                if(list == null) {
                    updateSpellingCandidates();
                }
                else {
                    System.out.println("the size of the suggestions list is " + list.size() + " : " + list);
                    setCandidatesViewShown(true);
                    setSuggestions(list, true, true);
                }


            }
            else {
                setSuggestions(null, false, false);
            }
        }
    }


    /**
     * This method update the candidate view with
     * spell checking errors
     * if there is no spelling error suggestions
     * then the candidate view is turned off
     */
    private void updateSpellingCandidates() {
        predictedWords = false;

        if(this.lookForSyns) {
            if (synWord.length() > 0) {
                ArrayList<String> list = new ArrayList<>();

                list = wordTree.spellChecker(this.synWord.toLowerCase());
                if (list == null)
                    setCandidatesViewShown(false);
                else {
                    composer.replace(0, composer.length(), this.synWord);
                    setCandidatesViewShown(true);
                }

                setSuggestions(list, true, true);

            }

            else {
                setSuggestions(null, false, false);
            }
        }
        else {
            if (composer.length() > 0) {
                ArrayList<String> list = wordTree.spellChecker(composer.toString().toLowerCase());
                if (list == null)
                    setCandidatesViewShown(false);
                else
                    setCandidatesViewShown(true);

                setSuggestions(list, true, true);

            }

            else {
                setSuggestions(null, false, false);
            }
        }
    }

    /**
     * This method updates the candidate view with
     * prediction words based off of the user previous inputs
     * If there is no predictions for the desired word, then
     * direct to I don't know yet, maybe nothing
     */
    private void updatePredictionWords() {
        predictedWords = false;
        InputConnection inputConnection = getCurrentInputConnection();
//        System.out.println(inputConnection.getSelectedText(0));

        if(mCompletionOn) {
            setCandidatesViewShown(true);
            if(composer.length() > 0 ) {
                ArrayList<String> list = null;

                boolean check = checkCollection(this.composer.toString().toLowerCase());
                if((".?:;!/@").contains(String.valueOf(composer.charAt(composer.length() - 1))))
                    check = checkCollection(this.composer.substring(0, composer.length() - 1).toLowerCase());

                if(check) {
//                    String temp = composer.toString();
//                    if(wordTree.isWord(this.synWord.toLowerCase())) {
//
//                    }
                    ArrayList<String> predictionWords;
                    setCandidatesViewShown(true);
                    if((".?:;!/@").contains(String.valueOf(composer.charAt(composer.length() - 1))))
                        predictionWords = wordTree.getNextWords(this.composer.substring(0, composer.length() - 1).toLowerCase());
                    else
                        predictionWords = wordTree.getNextWords(this.composer.toString().toLowerCase());
                    list = predictionWords;

                    System.out.println("looking to predict some words: " + list + " : " + this.composer + ":");
                    if(list == null)
                        setSuggestions(null, false, false);
                    else {
                        predictedWords = true;
                        setSuggestions(list, true, true);
                    }
                }
                else {
                    setSuggestions(null, false, false);
//                    updateSpellingCandidates();
                }

            }

            else {
                setSuggestions(null, false, false);
            }
        }
        else {
            setSuggestions(null, false, false);
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

            mCV.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    /**
     * Since any word along the path of the word will have equal score
     * This method will insure that the most frequently used words have the highest score
     * by adding to the score only if the word is what the user meant to type or
     * if they pick it from the candidate view
     */
    public void addExtraPoint(String word) {
        System.out.println("the word that gets a extra point is: " + word);
        wordTree.addExtraPoint(word);
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

    /**
     * adds apostrophes to contractions words
     */
    public void checkContractionWords() {
        InputConnection inputConnection = getCurrentInputConnection();
//        System.out.println("size of contract: " + composer.length());
        // delete the last character in the composer
        if(composer.length() > 0) {
            String contract = composer.toString();
            if(theFile.contractionWords.containsKey(contract)) {
                String result = theFile.contractionWords.get(contract);
                int n = contract.length();
                CharSequence selectText = inputConnection.getTextBeforeCursor(n, 0);
                String first = Character.toString(selectText.charAt(0));
                n = result.length();
                result = first + result.substring(1, n);


                n = contract.length();


                // Determine what is currently in the text view.

                inputConnection.deleteSurroundingText(n, 0);
//                System.out.println("size of contract: " + selectedText);
                inputConnection.commitText(result, 1);

            }
        }



//        if(TextUtils.isEmpty(selectedText)) {
//            inputConnection.commitText("",1);
//            if(caps != 2) {
//                caps = 0;
//                handleShift();
//            }
//
//        }


    }

    @Override
    public void onExtractedCursorMovement(int dx, int dy) {
//        System.out.println("the cursor has moved" );
//        System.out.println("dx: " + dx);
//        System.out.println("dy: " + dy);
    }

    private boolean checkCollection(String word) {
//        String temp;
//        if(composer.length() > 0) {
//            temp = composer.toString();
//            if(theFile.theSyns.get(temp) != null) {
//
//                return true;
//            }
//        }

        if(wordTree.isWord(word.toLowerCase()))
            return true;
        return false;
    }


    @Override
    public  void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {
//        Log.d("SoftKeyboard", "Swipe Left");
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

