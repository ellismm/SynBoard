package com.example.synboard;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class WordTree {

    private static LetterNode head = new LetterNode('a');
    private static LetterNode current;
    private static HashMap<String, Integer> suggestScores = new HashMap<>();
    private Context context;
    private static int SUGGEST_NUM = 10;
    private static LetterNode lastWord = head;
    /**
     * TODO: increase the number suggestions given.
     */


    /**
     * Constructor that constructs the dictionary in a tree like structure
     * @param context
     * @throws IOException
     */
    public WordTree(Context context) throws IOException {
        this.context = context;
        if(head.nextLetters.size() == 0) {
            readSynFile();
            readNonSynFile2();
        }
    }

    public void output(ArrayList<String> arr) {


        int n = arr.size();

        for(int i = 0; i < n; i++) {
            System.out.print(arr.get(i) + " ");
        }
    }

    /**
     * Determines if a desired string is word
     * @param word
     * @return
     */
    public Boolean isWord(String word) {
        if(word == null)
            return false;
        current = head;
        int n = word.length();

        for(int i = 0; i < n; i++) {
            char let = word.charAt(i);

            LetterNode temp = current.findPointer(let);
            if(temp != null)
                current = temp;
            else {
                return false;
            }
        }

        if(current.isWord) {
            return true;
        }

        return false;
    }

    /**
     * A method to add a point to a word
     * @param word
     * @return
     */
    public void addExtraPoint(String word) {
        if(word == null)
            return;
        current = head;
        LetterNode parent = current;
        int n = word.length();

        for(int i = 0; i < n; i++) {
            char let = word.charAt(i);

            LetterNode temp = current.findPointer(let);
            if(temp != null) {
                current.totalScore++;
                parent = current;
                temp.addScore();
                current = temp;
            }
            else {
                break;
            }
        }

        if(current.isWord) {
            current.addScore();
            parent.totalScore++;
        }
    }


    /**
     * Reads the text file with all the words with synonyms
     * @throws IOException
     */
    public void readSynFile() throws IOException {
        try {
            InputStream iS = context.getAssets().open("synonyms.txt");
            BufferedReader buf = new BufferedReader(new InputStreamReader(iS));
            String line;
            while((line = buf.readLine()) != null) {
                ArrayList<String> temp = new ArrayList<>();
                String word = line;
                line = buf.readLine();
                String[] temp2 = line.split(", ");
                int index = 0;
                for(String s : temp2) {
                    temp.add(s);
                    if(++index == 31)
                        break;

                }
                wordConstructor(word, temp);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * reads the text file of all the words with synonyms
     */
    private void readNonSynFile2() {
        try {
            InputStream iS = context.getAssets().open("noSynWords.txt");
            BufferedReader buf = new BufferedReader(new InputStreamReader(iS));
            String word;
            while((word = buf.readLine()) != null) {
                wordConstructor(word, null);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the dictionary in a tree like structure
     * where every node represents a letter, and a path represents a word
     * @param word
     * @param syns
     */
    private static void wordConstructor(String word, ArrayList<String> syns) {

        LetterNode c = head;
        int n = word.length();
        LetterNode temp;

        for(int i = 0; i < n; i++) {
            char let = word.charAt(i);

            temp = c.findPointer(let);
            if(temp != null)
                c = temp;
            else {
                temp = new LetterNode(let);
                if(c != head)
                    temp.parent = c;
                temp.letter = let;
                c.addPointer(temp);
                c = temp;
            }
            int number_of_syns = 0;
            if(i == n - 1) {
                c.isWord = true;
                if(syns != null) {

                    for(String s: syns) {
                        temp = addSynonymPointer(s);
                        if(number_of_syns == 32)
                            break;
                        if(temp != null) {
                            number_of_syns++;
                            c.addSyn(temp);
                        }
                    }
                }
            }
        }

    }

    /**
     * Finds the pointer of the synonym.
     * If the synonym word is not constructed then the word gets add;
     * @param word
     * @return
     */
    private static LetterNode addSynonymPointer(String word) {
        LetterNode temp = getWordPointer(word);
        if(word == null || word == "")
            return  null;
        if(temp != null)
            return temp;

        wordConstructor(word, null);
        temp = getWordPointer(word);
        if(temp != null)
            return temp;

        return  temp;

    }

    /**
     * A method to calculate suggestions of words
     * given a prefix string
     * @param pref
     * @return
     */
    public ArrayList<String> suggestions(String pref) {

        current = head;
        int n = pref.length();

        for(int i = 0; i < n; i++) {
            char let = pref.charAt(i);

            LetterNode temp = current.findPointer(let);
            if(temp != null) {
//                if(temp.score < 25) {
//                    current.totalScore++;
//                    temp.addScore();
//                }
                current = temp;
            }
            else {
                return null;
            }
        }


        ArrayList<String> wordSuggestions = new ArrayList<>();

        suggestScores.clear();
        followPaths(pref, 3, wordSuggestions, current);
        return wordSuggestions;

    }


    /**
     * This method follows  the greatest @param pathsLeft paths to find suggestions of words
     * @param word
     * @param pathsLeft
     * @param result
     * @param tempPoint
     */
    private static void followPaths(String word,
                                    int pathsLeft, ArrayList<String> result, LetterNode tempPoint) {

        tempPoint.sortPoints();
        ArrayList<LetterNode> nextLetters = tempPoint.getPoints();
        int x = pathsLeft;

        for(LetterNode p: nextLetters) {
            String tempWord = null;

            tempWord = followPathsUntilWord(word + p.letter, p);
            if(tempWord != null) {
//                System.out.print(tempWord + " ");
                LetterNode wordValidator = getWordPointer(tempWord);
                suggestScores.put(tempWord, wordValidator.score);
                if(!result.contains(tempWord)) {
                    if(result.size() < SUGGEST_NUM) {
                        int index = result.size();
                        for (int i = 0; i < result.size(); i++) {
                            int s = suggestScores.get(result.get(i));
                            if(wordValidator.score > s){
                                index = i;
                                break;
                            }
//                            System.out.println("the word " + tempWord + " is get input into results at index " + i + " the scores " + s + " : " + wordValidator.score + " " + result.get(i));

                        }
                        result.add(index, tempWord);
                    }
                    else {
                        if(suggestScores.get(result.get(SUGGEST_NUM - 1)) < p.score) {
                            int index = SUGGEST_NUM;
                            for (int i = SUGGEST_NUM - 2; i >= 0; i--) {
                                int s = suggestScores.get(result.get(i));
                                if(p.score > s){
                                    index = i;
                                }
                                else {
                                    break;
                                }
                            }
                            result.add(index, tempWord);
                            result.remove(SUGGEST_NUM);
                        }
                    }

                }
            }

            followPaths(word + p.letter, pathsLeft - 1, result, p);

            if(tempWord != null)
                x--;
            if(x == 0)
                break;
        }
    }


    /**
     * This will follow one path until a word is hit or there is no words for this path
     * @param pref
     * @param tempPoint
     * @return
     */
    private static String followPathsUntilWord(String pref,
                                               LetterNode tempPoint) {
        if(tempPoint.isWord)
            return pref;



        tempPoint.sortPoints();
        ArrayList<LetterNode> nextLetters = tempPoint.getPoints();
        String word = pref;

        for(LetterNode p: nextLetters) {
            word += p.letter;
            if(p.isWord) {
                // System.out.println("hi: " + word);
                return word;
            }
            else {
                // System.out.println("not a word: " + word);
                word = followPathsUntilWord(word, p);
            }

            if(word != null)
                return word;
            word = pref;

        }

        return null;
    }

    /**
     * returns the synonyms for a given word
     * null if it doesn't exist
     * @param word
     * @return
     */
    public static ArrayList<String> getSyns(String word) {
        LetterNode c = getWordPointer(word);
        if(c == null)
            return null;
        ArrayList<LetterNode> synPointers = c.getSyns();
        if(synPointers == null)
            return null;
        if(synPointers.size() > 32)
            System.out.println("from get syns: the result size: " + synPointers.size() + " :the word: " + word);
        ArrayList<String> result = new ArrayList<>();
        String s = "";
        for(LetterNode p: synPointers) {
            s = followToHead(p);
            if(s != null)
                result.add(s);
        }

        if(result.size() > 0) {
            return result;
        }
        return  null;

    }

    /**
     * A method to return a prediction of the next words
     * based on what the user has typed in before
     * @param word
     * @return
     */
    public static ArrayList<String> getNextWords(String word) {
        current = head;
        current = getWordPointer(word);
        if(current == null)
            return null;
        ArrayList<LetterNode> nextWords = current.getWords();
        if(nextWords == null || nextWords.size() == 0)
            return null;
        ArrayList<String> result = new ArrayList<>();
        String s = "";
        for(LetterNode p: nextWords) {
            s = followToHead(p);
            if(s != null)
                result.add(s);
        }

        if(result.size() > 0) {
            return result;
        }
        return  null;
    }

    /**
     * A method to add a prediction word based off of previous
     * user input
     * @param word
     */
    public static void addPredictionWord(String word) {
        System.out.println("this should be updating from the very beginning");
        if(word == null || word == "")
            return;
        if((".?:;!/@").contains(String.valueOf(word.charAt(word.length() - 1))))
            word = word.substring(0, word.length() - 1);
        System.out.println("the word to add for predicitons is :" + word + ":");
        LetterNode temp = getWordPointer(word);
        if(temp == null)
            return ;

        if(lastWord == null) {
            lastWord = temp;
            return;
        }
        if(temp != lastWord)
            lastWord.addWord(temp);
        lastWord = temp;
    }



    /**
     * A method to reset the lastword to the head once the user types a period
     */
    public void resetLastWord() {
        System.out.println("reseting the lastword pointer");
        this.lastWord = head;
    }

    /**
     * this follows pointer up to the head of the tree and constructs a word
     * @param p
     * @return
     */
    private static String followToHead(LetterNode p) {

        if(p.parent == null) {
            return null;
        }
        String s = "";
        while(p != null) {
            s = p.letter + s;
            p = p.parent;
        }
        if(s == "")
            return null;
        return s;
    }


    /**
     * follows a prefix to determine the node for the last letter of the given word
     * @param word
     * @return
     */
    private static LetterNode getWordPointer(String word) {


        if(word == "" || word == null)
            return null;
        LetterNode c = head;
        int n = word.length();
        for(int i = 0; i < n; i++) {
            char let = word.charAt(i);

            LetterNode temp = c.findPointer(let);
            if(temp != null) {
                c = temp;
            }
            else {
//                System.out.println("this prefix is not in the tree");
                return null;
            }
        }
        return c;
    }


    /**
     * Calculates if there any spelling errors in a given word
     * return a list of words as spelling suggestions
     * null if there are no suggestions
     * @param word
     * @return
     */
    public ArrayList<String> spellChecker(String word) {
        suggestScores.clear();
        ArrayList<String> result = new ArrayList<>();
        swapAdjacentLetters(word, result);
        addOneLetter("", word, result, head);
        if(word.length() > 1)
            removeOneLetter(String.valueOf(word.charAt(0)), word.substring(1), result);
        replaceOneLetter("", word, result, head);

//        for(String s : result)
//            System.out.println("the score for " + s + " is: " + suggestScores.get(s));

        return result;
    }

    /**
     * Determines if there are any spelling errors of a given word by adding letters
     * @param startPref
     * @param endPref
     * @param result
     * @param tempPoint
     * @return
     */
    private ArrayList<String> addOneLetter(String startPref, String endPref, ArrayList<String> result, LetterNode tempPoint) {
        ArrayList<LetterNode> nextLetters = tempPoint.getPoints();
        if(nextLetters == null)
            return result;
        for(LetterNode p: nextLetters) {
            String word = startPref + p.letter + endPref;
            LetterNode wordValidator = getWordPointer(word);
            if(wordValidator != null && wordValidator.isWord) {
                suggestScores.put(word, wordValidator.score);
                if(!result.contains(word)) {
                    if(result.size() < SUGGEST_NUM) {
                        int index = result.size();
                        for (int i = 0; i < result.size(); i++) {
                            int s = suggestScores.get(result.get(i));
                            if(wordValidator.score > s){
                                index = i;
                                break;
                            }
                        }
                        result.add(index, word);
                    }
                    else {
                        if(suggestScores.get(result.get(SUGGEST_NUM -1)) < wordValidator.score) {
                            int index = SUGGEST_NUM -1;
                            for (int i = SUGGEST_NUM - 1; i >= 0; i--) {
                                int s = suggestScores.get(result.get(i));
                                if(wordValidator.score > s){
                                    index = i;
                                }
                                else {
                                    break;
                                }
                            }
                            result.add(index, word);
                            result.remove(SUGGEST_NUM);
                        }
                    }
                }
            }
        }

        if(!endPref.equals("")) {
            startPref += endPref.charAt(0);
            endPref = endPref.substring(1);
            LetterNode x = getWordPointer(startPref);
            if(x != null) {
                return addOneLetter(startPref, endPref, result, x);
            }
        }
        return result;
    }

    /**
     * Determines if there any spelling errors by removing letters
     * @param startPref
     * @param endPref
     * @param result
     * @return
     */
    private ArrayList<String> removeOneLetter(String startPref, String endPref, ArrayList<String> result) {
        if(startPref.equals("") || startPref == null || endPref == null)
            return result;
        String word = startPref.substring(0, startPref.length() - 1) + endPref;
        LetterNode wordValidator = getWordPointer(word);

        if(wordValidator != null && wordValidator.isWord) {
            suggestScores.put(word, wordValidator.score);
            if(!result.contains(word)) {
                if(result.size() < SUGGEST_NUM) {
                    int index = result.size();
                    for (int i = 0; i < result.size(); i++) {
                        int s = suggestScores.get(result.get(i));
                        if(wordValidator.score > s){
                            index = i;
                            break;
                        }
                    }
                    result.add(index, word);
                }
                else {
                    if(suggestScores.get(result.get(SUGGEST_NUM - 1)) < wordValidator.score) {
                        int index = SUGGEST_NUM = 1;
                        for (int i = SUGGEST_NUM - 2; i >= 0; i--) {
                            int s = suggestScores.get(result.get(i));
                            if(wordValidator.score > s){
                                index = i;
                            }
                            else {
                                break;
                            }
                        }
                        result.add(index, word);
                        result.remove(SUGGEST_NUM);
                    }
                }
            }
        }

        if(!endPref.equals("")) {
            startPref += endPref.charAt(0);
            endPref = endPref.substring(1);
            return removeOneLetter(startPref, endPref, result);
        }
        return result;
    }

    /**
     * Determines if there are any spelling errors replacing letters
     * @param startPref
     * @param endPref
     * @param result
     * @param tempPoint
     * @return
     */
    private ArrayList<String> replaceOneLetter(String startPref, String endPref, ArrayList<String> result, LetterNode tempPoint) {
        ArrayList<LetterNode> nextLetters = tempPoint.getPoints();
        if(endPref.equals("") || startPref == null || endPref == null)
            return result;

        for(LetterNode p: nextLetters) {
            String word = startPref + p.letter + endPref.substring(1);
            LetterNode wordValidator = getWordPointer(word);
            if(wordValidator != null && wordValidator.isWord) {
                suggestScores.put(word, wordValidator.score);
                if(!result.contains(word)) {
                    if(result.size() < SUGGEST_NUM) {
                        int index = result.size();
                        for (int i = 0; i < result.size(); i++) {
                            int s = suggestScores.get(result.get(i));
                            if(wordValidator.score > s){
                                index = i;
                                break;
                            }
                        }
                        result.add(index, word);
                    }
                    else {
                        if(suggestScores.get(result.get(SUGGEST_NUM - 1)) < wordValidator.score) {
                            int index = SUGGEST_NUM - 1;
                            for (int i = SUGGEST_NUM - 2; i >= 0; i--) {
                                int s = suggestScores.get(result.get(i));
                                if(wordValidator.score > s){
                                    index = i;
                                }
                                else {
                                    break;
                                }
                            }
                            result.add(index, word);
                            result.remove(SUGGEST_NUM);
                        }
                    }
                }
            }
        }

        startPref += endPref.charAt(0);
        endPref = endPref.substring(1);
        LetterNode x = getWordPointer(startPref);
        if(x != null)
            return replaceOneLetter(startPref, endPref, result, x);
        return result;
    }


    /**
     * Method to spellcheck a word by swaping adjacent letters
     * @param word
     * @param result
     * @return
     */
    private static ArrayList<String> swapAdjacentLetters(String word, ArrayList<String> result) {
        if(word == null || word == "")
            return result;
        int n = word.length();
        if(n < 2)
            return result;
        LetterNode tempNode;
        if(n == 2) {
            word = String.valueOf(word.charAt(1)) + String.valueOf(word.charAt(0));
            tempNode = getWordPointer(word);
            if(tempNode != null && tempNode.isWord) {
                result.add(word);
                suggestScores.put(word, tempNode.score);
            }
            System.out.println("here: " + word);
            return result;
        }
        String temp = String.valueOf(word.charAt(1)) + String.valueOf(word.charAt(0)) + word.substring(2);
        tempNode = getWordPointer(temp);
        if(tempNode != null && tempNode.isWord) {
            result.add(temp);
            suggestScores.put(temp, tempNode.score);
        }
        System.out.println("here: " + temp);
        for(int i = 1; i < n - 1; i++) {
            if(i >= n - 1)
                temp = word.substring(0, i) + word.charAt(i + 1) + word.charAt(i);
            else
                temp = word.substring(0, i) + word.charAt(i + 1) + word.charAt(i) + word.substring(i + 2);
            tempNode = getWordPointer(temp);
            if(tempNode != null && tempNode.isWord) {
                result.add(temp);
                suggestScores.put(temp, tempNode.score);
            }
            System.out.println("here: " + temp);
        }

        return result;
    }

    /**
     * This method will refactor the dictionary to make sure that the score for a node
     * doesn't get to high
     */
    public void refactorDictionary() {
        refactorDictionaryB(this.head);
    }

    private void refactorDictionaryB(LetterNode point) {
        ArrayList<LetterNode> nextLetters = point.getPoints();
        for(LetterNode p : nextLetters) {
            p.refactorScore();
        }

    }

}
