package com.example.ilocanospeech_to_texttranslatorapp.fragments;

import android.content.Context;
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
    private String api = "";
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

        // Set up translation model (English to Ilocano)
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.TAGALOG)
                .build();

        translator = Translation.getClient(options);

        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    // Enable input translation after model download
                    editTextIn.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            engT.setText(s.toString()); // Set input text as English text
                            translateText(s.toString());
                        }

                        @Override
                        public void afterTextChanged(Editable s) {}
                    });
                })
                .addOnFailureListener(e -> iloT.setText("Error downloading translation model"));

        return view;
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

    // Translate input text
    private void translateText(String input) {
        if (input.isEmpty()) {
            iloT.setText("");
            engT.setTextColor(getResources().getColor(R.color.black)); // Ensure default color
            iloT.setTextColor(getResources().getColor(R.color.black));
            editTextIn.setTextColor(getResources().getColor(R.color.black));
            return;
        }
        engT.setTextColor(getResources().getColor(R.color.black)); // Change font color to black
        translator.translate(input)
                .addOnSuccessListener(translatedText -> {
                    iloT.setText(translatedText);
                    iloT.setTextColor(getResources().getColor(R.color.black)); // Change font color to black
                })
                .addOnFailureListener(e -> iloT.setText("Translation error"));
        translator.translate(input)
                .addOnSuccessListener(translatedText -> iloT.setText(translatedText)) // Set Ilocano text
                .addOnFailureListener(e -> iloT.setText("Translation error"));
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
