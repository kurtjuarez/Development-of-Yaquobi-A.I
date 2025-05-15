package com.example.ilocanospeech_to_texttranslatorapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ilocanospeech_to_texttranslatorapp.R;


// The Fragment of AboutUsPage
public class AboutUsPage extends Fragment {

    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Gets the fragment
        view = inflater.inflate(R.layout.fragment_about_us_page, container, false);

        return view;
    }
}