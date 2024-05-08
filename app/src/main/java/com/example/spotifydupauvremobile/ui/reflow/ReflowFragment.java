package com.example.spotifydupauvremobile.ui.reflow;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import androidx.media3.common.Player;

import com.example.spotifydupauvremobile.R;
import com.example.spotifydupauvremobile.databinding.FragmentReflowBinding;
import android.media.MediaRecorder;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
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
import com.example.spotifydupauvremobile.ui.reflow.MusicIce.*;
import com.zeroc.Ice.*;
import android.media.MediaPlayer;




public class ReflowFragment extends Fragment {

    private FragmentReflowBinding binding;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private SpeechClient speechClient;
    MediaPlayer mediaPlayer;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 101;

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioTrack audioTrack;
    private boolean isPlaying = false;

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

    private byte[] recordedAudioData;

    private void startRecording() {
        String outputFile = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.wav";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(outputFile);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Toast.makeText(getContext(), "Enregistrement démarré", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("ReflowFragment", "Erreur de démarrage de l'enregistrement: " + e.getMessage());
            Toast.makeText(getContext(), "Erreur de démarrage de l'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                Toast.makeText(getContext(), "Enregistrement arrêté", Toast.LENGTH_SHORT).show();

                // Lire les bytes du fichier WAV enregistré
                String filePath = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.wav";
                recordedAudioData = readFileToByteArray(filePath);

                // Passer les bytes au convertisseur
                //playRecordedAudio();
                convertSpeechToText(recordedAudioData);
                //jouer("matcher.group(2)", "matcher.group(3)");
            } catch (Exception e) {
                Log.e("ReflowFragment", "Erreur d'arrêt de l'enregistrement: " + e.getMessage());
                //Toast.makeText(getContext(), "Erreur d'arrêt de l'enregistrement", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void playRecordedAudio() {
        try {
            // Chemin de l'audio enregistré
            String outputFile = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.wav";

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

            // Libérer le lecteur de média lorsqu'il a terminé la lecture
            mediaPlayer.setOnCompletionListener(mp -> {
                mediaPlayer.release();
            });
        } catch (IOException e) {
            Log.e("ReflowFragment", "Error playing recorded audio: " + e.getMessage());
        }
    }


    // Méthode pour lire un fichier audio et le convertir en tableau de bytes
    private byte[] readFileToByteArray(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(bytes);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return bytes;
    }

    // Méthode pour convertir les bytes du fichier audio en texte
    private void convertSpeechToText(byte[] audioData) {
        try {
            if (speechClient == null) {
                Log.e("ReflowFragment", "SpeechClient is null. Cannot convert speech to text.");
                return;
            }

            // Créer un objet ByteString à partir des données audio
            ByteString audioBytes = ByteString.copyFrom(audioData);

            // Créer l'objet RecognitionAudio avec les données audio
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();
            System.out.println(audio);

            // Configuration de la reconnaissance vocale (MP3 ça marche)
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                    .setSampleRateHertz(44100)
                    .setLanguageCode("fr-FR")
                    .build();

            // Effectuer la reconnaissance vocale avec l'API SpeechClient
            RecognizeResponse response = speechClient.recognize(config, audio);
            System.out.println(response);

            // Traitement de la réponse
            for (SpeechRecognitionResult result : response.getResultsList()) {
                String transcript = result.getAlternatives(0).getTranscript();
                Log.d("ReflowFragment", "Transcription : " + transcript);
                processTranscript(transcript);
            }
        } catch (Exception e) {
            Log.e("ReflowFragment", "Error converting speech to text: " + e.getMessage());
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
        Communicator communicator = null;
        try {
            communicator = com.zeroc.Ice.Util.initialize();
            String proxyStr = "MusicService:tcp -h 192.168.1.62 -p 10000";

            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(proxyStr);
            if (base == null) {
                Log.e("Error", "Invalid proxy");
                return;
            }

            MusicPrx musicService = MusicPrx.checkedCast(base);
            if (musicService == null) {
                Log.e("Error", "Invalid MusicPrx");
                return;
            }

            musicService.supprimerMusique(musique);
        } catch (com.zeroc.Ice.LocalException e) {
            Log.e("Local Exception", e.getMessage());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }


    private void jouer(String musique, String auteur) {
        Communicator communicator = null;
        try {
            communicator = Util.initialize();
            String proxyStr = "MusicService:tcp -h 192.168.1.62 -p 10000";

            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(proxyStr);
            if (base == null) {
                Log.e("Error", "Invalid proxy");
                return;
            }

            MusicPrx musicService = MusicPrx.checkedCast(base);
            if (musicService == null) {
                Log.e("Error", "Invalid MusicPrx");
                return;
            }

            // Lecture de la musique via Ice
            musicService.play(musique);

            // URL du flux audio à écouter en streaming
            String audioUrl = "http://192.168.1.62:8080/stream.mp3";

            // Obtenez le contexte à partir de l'activité parente
            Context context = getActivity();

            // Vérifiez si le contexte est disponible
            if (context != null) {
                // Création d'un SimpleExoPlayer avec le contexte de l'activité
                SimpleExoPlayer exoPlayer = new SimpleExoPlayer.Builder(context).build();

                // Création de la source de média
                MediaItem mediaItem = MediaItem.fromUri(audioUrl);

                // Préparation du lecteur
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();

                // Ajoutez le lecteur à la vue du fragment
                PlayerView playerView = requireView().findViewById(R.id.playerView);
                playerView.setPlayer(exoPlayer);
            } else {
                Log.e("ReflowFragment", "Context is null. Unable to create ExoPlayer.");
                Toast.makeText(getContext(), "Une erreur s'est produite lors de la lecture du média.", Toast.LENGTH_SHORT).show();
            }
            SpannableString spannableString = new SpannableString("The Last Of Us est entrain d'être jouée !");
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE); // Par exemple, couleur bleue
            spannableString.setSpan(colorSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            Toast.makeText(getContext(), spannableString, Toast.LENGTH_SHORT).show();



        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
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
