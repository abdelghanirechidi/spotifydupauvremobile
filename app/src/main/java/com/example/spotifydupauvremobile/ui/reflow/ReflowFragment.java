package com.example.spotifydupauvremobile.ui.reflow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spotifydupauvremobile.R;
import com.example.spotifydupauvremobile.databinding.FragmentReflowBinding;
import android.media.MediaRecorder;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import android.media.MediaRecorder;



public class ReflowFragment extends Fragment {

    private FragmentReflowBinding binding;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private SpeechClient speechClient;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 101;

    // Méthode pour vérifier si la permission est accordée
    private boolean checkRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    // Méthode pour demander la permission
    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
    }

    // Surcharge pour gérer la réponse de l'utilisateur à la demande de permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, démarrer l'enregistrement
                startRecording();
            } else {
                // Permission refusée, afficher un message à l'utilisateur ou prendre d'autres mesures
                Toast.makeText(getContext(), "Permission d'enregistrement audio refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
            if (checkRecordAudioPermission()) {
                if (!isRecording) {
                    startRecording();
                    buttonRecord.setText("Arrêter");
                } else {
                    stopRecording();
                    buttonRecord.setText("Enregister");
                }
            } else {
                // Demander la permission à l'utilisateur
                requestRecordAudioPermission();
            }
        });

        // Initialize SpeechClient here
        try {
            // Load the service account key file
            InputStream credentialsStream = getResources().openRawResource(R.raw.credentials);

            // Create credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);

            // Create SpeechClientSettings using the credentials
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            // Create the SpeechClient using the settings
            speechClient = SpeechClient.create(settings);
        } catch (IOException e) {
            Log.e("ReflowFragment", "Error initializing SpeechClient: " + e.getMessage());
        }




        return root;
    }

    private void startRecording() {
        String outputFile = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.flac";
        Log.d("OutputFile", outputFile);
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS); // Output format for AAC
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // AAC audio encoder
        mediaRecorder.setAudioSamplingRate(44100); // Sample rate 44100 Hz
        mediaRecorder.setAudioChannels(1); // Mono channel
        mediaRecorder.setAudioEncodingBitRate(192000); // Bit rate 192 kbps
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
            Toast.makeText(getContext(), "L'enregistrement est arrêté", Toast.LENGTH_SHORT).show();
            //playRecordedAudio();
            convertSpeechToText();
        }
    }

    private void playRecordedAudio() {
        try {
            // Chemin de l'audio enregistré
            String outputFile = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.flac";
            // Créer un lecteur de média
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(outputFile);
            mediaPlayer.prepare();
            // Commencer la lecture
            mediaPlayer.start();
            // Gérer les exceptions
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("ReflowFragment", "Error playing recorded audio: " + what);
                return false;
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                // Libérer le lecteur de média lorsqu'il a terminé la lecture
                mediaPlayer.release();
            });
        } catch (IOException e) {
            Log.e("ReflowFragment", "Error playing recorded audio: " + e.getMessage());
        }
    }


    private byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];

        fis.read(data);
        fis.close();
        return data;
    }

    private void convertSpeechToText() {
        try {
            if (speechClient == null) {
                Log.e("ReflowFragment", "SpeechClient is null. Cannot convert speech to text.");
                return;
            }

            String outputFile = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.flac";

            // Read the content of the FLAC audio file
            byte[] audioBytes = readFile(outputFile);
            Log.e("bytes", Arrays.toString(audioBytes));

            // Convert bytes to ByteString
            ByteString audioBytesStr = ByteString.copyFrom(audioBytes);

            // Create recognition configuration
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                            .setSampleRateHertz(44100) // Make sure to set the correct sample rate for your FLAC file
                            .setLanguageCode("fr-FR")
                            .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytesStr) // Use the ByteString created from audioBytes
                    .build();

            // Perform speech recognition
            RecognizeResponse response = speechClient.recognize(config, audio);
            System.out.println(response.getResultsList());
            for (SpeechRecognitionResult result : response.getResultsList()) {
                String transcript = result.getAlternatives(0).getTranscript();
                System.out.println(transcript);
                processTranscript(transcript);
            }
        } catch (IOException e) {
            Log.e("ReflowFragment", "Error reading FLAC audio file: " + e.getMessage());
        }
    }


    private void processTranscript(String transcript) {
        Pattern pattern = Pattern.compile("(\\w+)\\s(.*?)\\sde\\s(.*)");
        Matcher matcher = pattern.matcher(transcript);
        if (matcher.find()) {
            String action = matcher.group(1).toLowerCase();
            switch (action) {
                case "joue":
                    jouer(matcher.group(2), matcher.group(3));
                    break;
                case "supprime":
                    supprime(matcher.group(2), matcher.group(3));
                    break;
                case "modifie":
                    modifie(matcher.group(2), matcher.group(3));
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
        Log.d("ReflowFragment", "Modifie : Musique - " + musique + ", Auteur - " + auteur);
    }

    private void supprime(String musique, String auteur) {
        Log.d("ReflowFragment", "Supprime : Musique - " + musique + ", Auteur - " + auteur);
    }

    private void jouer(String musique, String auteur) {
        Log.d("ReflowFragment", "Joue : Musique - " + musique + ", Auteur - " + auteur);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechClient != null) {
            try {
                speechClient.close();
            } catch (Exception e) {
                Log.e("ReflowFragment", "Erreur lors de la fermeture du SpeechClient: " + e.getMessage());
            }
        }
        binding = null;
    }
}
