package com.example.synboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class LetterNode {

    char letter;
    int score = 0;
    int totalScore = 0;
    Boolean isWord = false;
    LetterNode parent;
    private ArrayList<LetterNode> syns = new ArrayList<>();
    public ArrayList<LetterNode> nextLetters = new ArrayList<>();
    private ArrayList<LetterNode> nextWords = new ArrayList<>();

    /**
     * Constructor to set the value of the node
     * @param l
     */
    public LetterNode(char l) {
        this.letter = l;
    }

    /**
     * Method to link other nodes to current node
     * @param let
     */
    public void addPointer(LetterNode let) {
        this.nextLetters.add(let);
    }

    /**
     * adds a word to nextwords
     * @param word
     */
    public void addWord(LetterNode word) {
        Collections.sort(this.nextWords, new LetterNode.CustomComparator());
        if(nextWords.size() < 20 & !nextWords.contains(word))
            this.nextWords.add(word);
        else
            if(!nextWords.contains(word))
                this.nextWords.add(19, word);
    }

    /**
     * Method to find a desired letter
     * @param l
     * @return
     */
    public LetterNode findPointer(char l) {

        for (LetterNode p : this.nextLetters)
            if (p.letter == l)
                return p;

        return null;
    }

    /**
     * returns next words array
     * @return
     */
    public ArrayList<LetterNode> getWords() {
        return this.nextWords;
    }

    /**
     * return the synonyms of the current path
     * @return
     */
    public ArrayList<LetterNode> getSyns() {
        return this.syns;
    }

    /**
     * Set the synonyms of the current node
     * @param syn
     */
    public void addSyn(LetterNode syn) {
        this.syns.add(syn);
    }

    /**
     * returns all the nodes linked to the current node
     * @return
     */
    public ArrayList<LetterNode> getPoints() {
        return this.nextLetters;
    }

    /**
     * Adds one to the score because this node was used
     */
    public void addScore() {
        if(this.score < 25)
            this.score++;
    }


    /**
     * method to sort the nodes that the current node is linked to
     * by in descending order of their score
     */
    public void sortPoints() {
        Collections.sort(this.nextLetters, new LetterNode.CustomComparator());
    }

    /**
     * a custom comparator method to help the method sortPoints()
     */
    private class CustomComparator implements Comparator<LetterNode> {
        @Override
        public int compare(LetterNode o1, LetterNode o2) {
            return o2.score - o1.score;
        }
    }

    /**
     * To make sure that the most used words is
     * what the user is currently most recently using
     * refactor the score the nodes linked to the current node
     * by making sure the score doesn't get to high.
     */
    public void refactorScore() {
        sortPoints();
        for (LetterNode p : nextLetters) {
            int left = totalScore - 50;
            if (left <= 0)
                break;
            int s = p.score;
            if (s <= 0)
                break;
            p.score--;
            totalScore--;
        }
        int left = totalScore - 50;
        if (left > 0)
            refactorScore();

    }
}
