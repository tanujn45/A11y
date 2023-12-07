package com.tanujn45.a11y;

public class CardData {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTextToSpeak() {
        return textToSpeak;
    }

    public void setTextToSpeak(String textToSpeak) {
        this.textToSpeak = textToSpeak;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getNumOfGestures() {
        return numOfGestures;
    }

    public void setNumOfGestures(int numOfGestures) {
        this.numOfGestures = numOfGestures;
    }

    private String name;
    private String textToSpeak;
    private String description;
    private int numOfGestures;

}

/*
########## Name ##########
Name
########## Text To Speak ##########
ttsText
########## Description ##########
description
########## Number of Gestures ##########
nGestures
 */
