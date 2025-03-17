package com.example.ilocanospeech_to_texttranslatorapp.fragments;

import android.content.Context;

import android.content.res.AssetManager;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ilocanospeech_to_texttranslatorapp.R;
import com.example.ilocanospeech_to_texttranslatorapp.asr.Recorder;
import com.example.ilocanospeech_to_texttranslatorapp.asr.Whisper;
import com.example.ilocanospeech_to_texttranslatorapp.dbh.DBTranslated;
import com.example.ilocanospeech_to_texttranslatorapp.utils.WaveUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class HomePage extends Fragment {

    // Whisper Model variables
    private static final String TAG = "HomePage";
    private static final String DEFAULT_MODEL_TO_USE = "whisper-tiny.tflite";
    private static final String DEFAULT_WAV_FILE = "MicInput.wav";
    private static final String ENGLISH_ONLY_MODEL_EXTENSION = ".en.tflite";
    private static final String ENGLISH_ONLY_VOCAB_FILE = "filters_vocab_en.bin";
    private static final String MULTILINGUAL_VOCAB_FILE = "filters_vocab_multilingual.bin";
    private static final String[] EXTENSIONS_TO_COPY = {"tflite", "bin", "wav", "pcm"};
    private RelativeLayout mFrame;
    // Speech-to-Text variables
    private ImageView micBut;
    private Recorder mRecord;
    private Whisper mWhisper;

    private File sdcardDataFolder;
    private File selectedWaveFile;
    private File selectedTfliteFile;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Text-to-Text variables
    private EditText editTextIn;
    private TextView engT, iloT;
    private static final String GOOGLE_TRANSLATE_API_URL = "https://translation.googleapis.com/language/translate/v2";

    private final String api = "AIzaSyBAqkkzBG9Be3-804IcD34L3nr0MHWFWn0";

    @SuppressLint("ResourceType")
    // Saved History variables
    private FloatingActionButton transCopy;
    private DBTranslated dbHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);

        // Copy files
        sdcardDataFolder = requireActivity().getExternalFilesDir(null);
        copyAssetsToSdcard(requireContext(), sdcardDataFolder, EXTENSIONS_TO_COPY);

        selectedTfliteFile = new File(sdcardDataFolder, DEFAULT_MODEL_TO_USE);
        selectedWaveFile = new File(sdcardDataFolder, DEFAULT_WAV_FILE);
        // Used model
        initModel(selectedTfliteFile);

        mFrame = view.findViewById(R.id.mic_id);
        // dbhandler class to pass the contents
        dbHandler = new DBTranslated(requireContext());
        // Save History Button
        transCopy = view.findViewById(R.id.transCopy);
        transCopy.setOnClickListener(v -> {
            String englishText = engT.getText().toString();
            String ilocanoText = iloT.getText().toString();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            if (!englishText.equals("Inputted text here.") && !ilocanoText.equals("Translated text here.")) {
                dbHandler.addTranslatedText(englishText, ilocanoText, timestamp);
                Toast.makeText(requireContext(), "Saved to history", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No valid translation to save", Toast.LENGTH_SHORT).show();
            }
        });

        // Microphone Button
        micBut = view.findViewById(R.id.off_record_button);

        micBut.setOnClickListener(v -> {
            if (mRecord != null && mRecord.isInProgress()) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        mRecord = new Recorder(requireContext());
        mRecord.setListener(new Recorder.RecorderListener() {
            @Override
            public void onUpdateReceived(String message) {
                Log.d(TAG, "Recording update: " + message);
            }

            @Override
            public void onDataReceived(float[] samples) {
                if (mWhisper != null) {
                    mWhisper.writeBuffer(samples);
                }
            }
        });

        editTextIn = view.findViewById(R.id.editTextInput);
        engT = view.findViewById(R.id.eng_text);
        iloT = view.findViewById(R.id.ilo_text);

        editTextIn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    engT.setText(s.toString());
                    engT.setTextColor(BLACK);
                    iloT.setTextColor(BLACK);
                    editTextIn.setTextColor(BLACK);
                    asyncTranslateText(s.toString());
                } else {
                    engT.setTextColor(GRAY);
                    iloT.setTextColor(GRAY);
                    editTextIn.setTextColor(GRAY);
                    engT.setText("Inputted text here.");
                    iloT.setText("Translated text here.");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }


    private void initModel(File modelFile) {
        boolean isMultilingualModel = !modelFile.getName().endsWith(ENGLISH_ONLY_MODEL_EXTENSION);
        String vocabFileName = isMultilingualModel ? MULTILINGUAL_VOCAB_FILE : ENGLISH_ONLY_VOCAB_FILE;
        File vocabFile = new File(sdcardDataFolder, vocabFileName);

        mWhisper = new Whisper(requireContext());
        mWhisper.loadModel(modelFile, vocabFile, isMultilingualModel);
        mWhisper.setListener(new Whisper.WhisperListener() {
            @Override
            public void onUpdateReceived(String message) {
                Log.d(TAG, "Whisper update: " + message);
            }

            @Override
            public void onResultReceived(String result) {
                handler.post(() -> editTextIn.setText(result));
            }
        });
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private void startRecording() {
        File waveFile = new File(sdcardDataFolder, WaveUtil.RECORDING_FILE);
        mRecord.setFilePath(waveFile.getAbsolutePath());
        mRecord.start();
        Drawable micBackgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_off_mic_layout);

        if (micBackgroundDrawable instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) micBackgroundDrawable;

            gradientDrawable.setColor(ContextCompat.getColor(requireContext(), R.color.on_mic));

            mFrame.setBackground(gradientDrawable);
            Toast.makeText(requireContext(), "Recording...", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        mRecord.stop();
        if (selectedWaveFile != null) {
            startTranscription(selectedWaveFile.getAbsolutePath());
            Drawable micBackgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_off_mic_layout);

            if (micBackgroundDrawable instanceof GradientDrawable) {
                GradientDrawable gradientDrawable = (GradientDrawable) micBackgroundDrawable;

                gradientDrawable.setColor(ContextCompat.getColor(requireContext(), R.color.off_mic));

                mFrame.setBackground(gradientDrawable);
            }
        }
    }

    private void startTranscription(String waveFilePath) {
        if (mWhisper != null) {
            mWhisper.setFilePath(waveFilePath);
            mWhisper.setAction(Whisper.ACTION_TRANSCRIBE);
            mWhisper.start();
        }
    }

    private void asyncTranslateText(String input) {
        new Thread(() -> {
            try {
                URL url = new URL(GOOGLE_TRANSLATE_API_URL + "?key=" + api);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                String jsonInput = "{ \"q\": \"" + input + "\", \"source\": \"en\", \"target\": \"ilo\", \"format\": \"text\" }";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray translations = jsonResponse.getJSONObject("data").getJSONArray("translations");
                String translatedText = translations.getJSONObject(0).getString("translatedText");

                handler.post(() -> iloT.setText(translatedText));

            } catch (Exception e) {
                handler.post(() -> iloT.setText("Translation error"));
            }
        }).start();
    }

    private static void copyAssetsToSdcard(Context context, File destFolder, String[] extensions) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] assetFiles = assetManager.list("");
            if (assetFiles == null) return;

            for (String assetFileName : assetFiles) {
                for (String extension : extensions) {
                    if (assetFileName.endsWith("." + extension)) {
                        File outFile = new File(destFolder, assetFileName);
                        if (outFile.exists()) break;

                        try (InputStream inputStream = assetManager.open(assetFileName);
                             OutputStream outputStream = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

