package com.example.ilocanospeech_to_texttranslatorapp.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ilocanospeech_to_texttranslatorapp.R;
import com.example.ilocanospeech_to_texttranslatorapp.asr.Recorder;
import com.example.ilocanospeech_to_texttranslatorapp.asr.Whisper;
import com.example.ilocanospeech_to_texttranslatorapp.utils.WaveUtil;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.cloud.translate.Translate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class HomePage extends Fragment {

    // TensorFlow Whisper Model variables
    private static final String TAG = "HomePage";
    private static final String DEFAULT_MODEL_TO_USE = "whisper-tiny.tflite";
    private static final String ENGLISH_ONLY_MODEL_EXTENSION = ".en.tflite";
    private static final String ENGLISH_ONLY_VOCAB_FILE = "filters_vocab_en.bin";
    private static final String MULTILINGUAL_VOCAB_FILE = "filters_vocab_multilingual.bin";
    private static final String[] EXTENSIONS_TO_COPY = {"tflite", "bin", "wav", "pcm"};
    // Speech-to-Text variables
    private ImageView micBut;
    private Recorder mRecord = null;
    private Whisper mWhisper = null;

    // Model and file type selector
    private File sdcardDataFolder = null;
    private File selectedWaveFile = null;
    private File selectedTfliteFile = null;

    //
    private long startTime = 0;
    private final boolean loopTesting = false;
    private final SharedResource transcriptionSync = new SharedResource();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Text-to-Text variables
    private EditText editTextIn;
    private TextView engT, iloT;
    private static final String GOOGLE_TRANSLATE_API_URL = "https://translation.googleapis.com/language/translate/v2";
    private String api = "AIzaSyBAqkkzBG9Be3-804IcD34L3nr0MHWFWn0";
    private Translator translator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_home_page, container, false);
        // Microphone Button
        micBut = view.findViewById(R.id.off_record_button);
        micBut.setOnClickListener(v -> {
            if(mRecord != null && mRecord.isInProgress()) {
                Log.d(TAG, "Recording is in progress... stopping...");
                stopRecording();
            } else {
                Log.d(TAG, "Start recording...");
                startRecording();
            }
        });

        // Audio Record Functionality
        mRecord = new Recorder(requireContext());
        mRecord.setListener(new Recorder.RecorderListener() {
            @Override
            public void onUpdateReceived(String message) {
                Log.d(TAG, "Update is received, Message: " + message);
            }

            @Override
            public void onDataReceived(float[] samples) {
                mWhisper.writeBuffer(samples);
            }
        });

        // Initialize UI components
        editTextIn = view.findViewById(R.id.editTextInput);
        engT = view.findViewById(R.id.eng_text);
        iloT = view.findViewById(R.id.ilo_text);


        // Set up text change listener to trigger translation
        editTextIn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    editTextIn.setTextColor(Color.BLACK);
                    engT.setText(s.toString());
                    engT.setTextColor(Color.BLACK);
                    iloT.setTextColor(Color.BLACK);
                    asyncTranslateText(s.toString()); // Call API-based translation
                } else {
                    // Reset to placeholder texts when input is cleared
                    editTextIn.setTextColor(Color.GRAY);
                    engT.setText("Inputted text here.");
                    iloT.setText("Translated text here.");
                    engT.setTextColor(Color.GRAY);
                    iloT.setTextColor(Color.GRAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        return view;
    }


    private void asyncTranslateText(String input) {
        if (input.isEmpty()) {
            iloT.setText("");
            return;
        }

        // Execute translation in a background thread
        new TranslateTask().execute(input);
    }

    private class TranslateTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... texts) {
            String inputText = texts[0];
            try {
                URL url = new URL(GOOGLE_TRANSLATE_API_URL + "?key=" + api);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                // JSON payload for translation request
                String jsonInputString = "{ \"q\": \"" + inputText + "\", \"source\": \"en\", \"target\": \"ilo\", \"format\": \"text\" }";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read the response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray translations = jsonResponse.getJSONObject("data").getJSONArray("translations");
                return translations.getJSONObject(0).getString("translatedText");

            } catch (Exception e) {
                e.printStackTrace();
                return "Translation error";
            }
        }

        @Override
        protected void onPostExecute(String translatedText) {
            iloT.setText(translatedText);
        }

    }


    // Recording calls
    private void startRecording() {

        File waveFile= new File(sdcardDataFolder, WaveUtil.RECORDING_FILE);
        mRecord.setFilePath(waveFile.getAbsolutePath());
        mRecord.start();
    }

    private void stopRecording() {
        mRecord.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (translator != null) {
            translator.close(); // Clean up translator instance
        }
    }

    static class SharedResource {
        // Synchronized method for Thread 1 to wait for a signal with a timeout
        public synchronized boolean waitForSignalWithTimeout(long timeoutMillis) {
            long startTime = System.currentTimeMillis();

            try {
                wait(timeoutMillis);  // Wait for the given timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Restore interrupt status
                return false;  // Thread interruption as timeout
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            // Check if wait returned due to notify or timeout
            if (elapsedTime < timeoutMillis) {
                return true;  // Returned due to notify
            } else {
                return false;  // Returned due to timeout
            }
        }

        // Synchronized method for Thread 2 to send a signal
        public synchronized void sendSignal() {
            notify();  // Notifies the waiting thread
        }
    }

}