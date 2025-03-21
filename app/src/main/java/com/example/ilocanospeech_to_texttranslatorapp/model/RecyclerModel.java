package com.example.ilocanospeech_to_texttranslatorapp.model;

public class RecyclerModel {

    private String timestamp;
    private String english_text;
    private String ilocano_text;

    public RecyclerModel(String timestamp, String english_text, String ilocano_text){
        this.timestamp = timestamp;
        this.english_text = english_text;
        this.ilocano_text = ilocano_text;
    }
    public String getTimestamp(){
        return timestamp;
    }
    public String getEnglish_text(){
        return english_text;
    }
    public String getIlocano_text(){
        return ilocano_text;
    }
}
