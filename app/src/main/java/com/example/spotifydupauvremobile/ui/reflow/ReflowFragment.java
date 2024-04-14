package com.example.spotifydupauvremobile.ui.reflow;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spotifydupauvremobile.databinding.FragmentReflowBinding;
import android.media.MediaRecorder;
import android.widget.Toast;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReflowFragment extends Fragment {

    private FragmentReflowBinding binding;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ReflowViewModel reflowViewModel =
                new ViewModelProvider(this).get(ReflowViewModel.class);

        binding = FragmentReflowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textReflow;
        reflowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Button buttonRecord = binding.buttonRecord;
        buttonRecord.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
                buttonRecord.setText("Stop Recording");
            } else {
                stopRecording();
                buttonRecord.setText("Start Recording");
            }
        });

        return root;
    }

    private void startRecording() {
        String outputFile = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.3gp";
        System.out.println(outputFile);
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(outputFile);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("ReflowFragment", "Error starting recording: " + e.getMessage());
            Toast.makeText(getContext(), "Error starting recording", Toast.LENGTH_SHORT).show();

        }
    }


    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            Toast.makeText(getContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
            convertSpeechToText();
        }
    }

    private void convertSpeechToText() {

        // Texte transcrit exemple

        String texteTranscrit = "joue Scrum mium miam de Yoan";
        Pattern pattern = Pattern.compile("(\\w+)\\s(.*?)\\sde\\s(.*)");
        Matcher matcher = pattern.matcher(texteTranscrit);
        if (matcher.find()) {
            String action = matcher.group(1).toLowerCase();
            System.out.println("Action : " + action);

            String musique = matcher.group(2);
            String auteur = matcher.group(3);

            switch (action) {
                case "joue":
                    jouer(musique, auteur);
                    break;
                case "supprime":
                    supprime(musique, auteur);
                    break;
                case "modifie":
                    modifie(musique, auteur);
                    break;
                default:
                    Log.e("ReflowFragment", "Action non reconnue : " + action);
                    break;
            }
        } else {
            Log.e("ReflowFragment", "L'action n'a pas été reconnue.");
        }

    }

    private void modifie(String musique, String auteur) {
        System.out.println("Musique : " + musique);
        System.out.println("Auteur : " + auteur);
    }

    private void supprime(String musique, String auteur) {
        System.out.println("Musique : " + musique);
        System.out.println("Auteur : " + auteur);
    }

    private void jouer(String musique, String auteur) {
        System.out.println("Musique : " + musique);
        System.out.println("Auteur : " + auteur);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
