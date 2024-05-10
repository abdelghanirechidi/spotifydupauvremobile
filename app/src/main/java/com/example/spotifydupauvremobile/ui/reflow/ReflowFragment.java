package com.example.spotifydupauvremobile.ui.reflow;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spotifydupauvremobile.R;
import com.example.spotifydupauvremobile.databinding.FragmentReflowBinding;
import com.example.spotifydupauvremobile.ui.reflow.MusicIce.MusicPrx;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


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
                    buttonRecord.setBackgroundResource(R.drawable.mic_on);
                } else {
                    stopRecording();
                    buttonRecord.setText("Enregister");
                    buttonRecord.setBackgroundResource(R.drawable.mic_off);
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

                // Lire les bytes du fichier WAV enregistré
                String filePath = getActivity().getExternalCacheDir().getAbsolutePath() + "/recording.wav";
                recordedAudioData = readFileToByteArray(filePath);

                convertSpeechToText(recordedAudioData);

            } catch (Exception e) {
                Log.e("ReflowFragment", "Erreur d'arrêt de l'enregistrement: " + e.getMessage());
            }
        }
    }


    private void playRecordedAudio() {
        try {
            int audioResourceId = getResources().getIdentifier("erreur", "raw", getActivity().getPackageName());
            MediaPlayer mediaPlayer = new MediaPlayer();

            AssetFileDescriptor afd = getResources().openRawResourceFd(audioResourceId);
            if (afd != null) {
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();

                // Définir la vitesse de lecture sur la valeur par défaut (1.0f)
                PlaybackParams params = new PlaybackParams();
                params.setSpeed(1.0f);
                mediaPlayer.setPlaybackParams(params);

                mediaPlayer.setOnPreparedListener(mp -> {
                    mediaPlayer.start();
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e("ReflowFragment", "Error playing recorded audio: " + what);
                    return false;
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    mediaPlayer.release();
                });

                mediaPlayer.prepareAsync(); // Préparation asynchrone
            } else {
                Log.e("ReflowFragment", "Error opening raw resource for MediaPlayer");
            }
        } catch (Exception e) {
            Log.e("ReflowFragment", "Error playing recorded audio: " + e.getMessage());
            mediaPlayer.release();
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

    public void processTranscript(String transcript) {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"transcript\": \"" + transcript + "\"}");

        Request request = new Request.Builder()
                .url("http://192.168.1.62:5000/analyze")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                client.dispatcher().executorService().shutdown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse1 = new JSONObject(responseBody);
                    String responseStatus = jsonResponse1.getString("response");
                    System.out.println(responseStatus);
                    if(responseStatus.equals("Action not detected")){
                        playRecordedAudio();
                    } else {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String a = jsonResponse.toString();
                            String[] parts = a.split("\"response\":\"");
                            String action = "";
                            String titre = "";
                            String artiste = "";
                            if (parts.length > 1) {
                                String responseMessage = parts[1];
                                // Extraire l'action
                                int actionStartIndex = responseMessage.indexOf("action: ");
                                if (actionStartIndex != -1) {
                                    int actionEndIndex = responseMessage.indexOf(", Music:");
                                    if (actionEndIndex != -1) {
                                        action = responseMessage.substring(actionStartIndex + "action: ".length(), actionEndIndex).trim();
                                        System.out.println("Action: " + action);
                                    }
                                }
                                // Extraire le titre et l'artiste
                                int musicStartIndex = responseMessage.indexOf(", Music: ('");
                                if (musicStartIndex != -1) {
                                    int musicEndIndex = responseMessage.indexOf("')\"}");
                                    if (musicEndIndex != -1) {
                                        String musicDetails = responseMessage.substring(musicStartIndex + ", Music: ('".length(), musicEndIndex);
                                        String[] musicParts = musicDetails.split("', '");
                                        if (musicParts.length == 2) {
                                            titre = musicParts[0];
                                            artiste = musicParts[1];
                                        }
                                    }
                                }
                            }

                            if ("jouer".equals(action)) {
                                if(!titre.equals("")){


                                    jouer(titre, artiste);
                                    View view = getView();
                                    if (view != null) {
                                        Snackbar.make(view, titre + " a été lancée !",  Snackbar.ANIMATION_MODE_FADE).show();
                                    }

                                }
                                else{
                                    requireActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "Veuillez préciser le nom de la musique", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }
                            } else if ("supprimer".equals(action)) {
                                if(!titre.equals("")) {
                                    supprime(titre, artiste);

                                    View view = getView();
                                    if (view != null) {
                                        Snackbar.make(view, titre + " a été supprimée !",  Snackbar.ANIMATION_MODE_FADE).show();
                                    }
                                }
                                else {
                                    requireActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "Veuillez préciser le nom de la musique", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                client.dispatcher().executorService().shutdown();
            }
        });
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
            musicService.stop();
            musicService.play(musique);

            // URL du flux audio à écouter en streaming
            String audioUrl = "http://192.168.1.62:8080/stream.mp3";

            // Obtenez le contexte à partir de l'activité parente
            Context context = getActivity();

            // Vérifiez si le contexte est disponible
            if (context != null) {
                // Exécutez le code d'accès au lecteur sur le thread principal
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Création d'un SimpleExoPlayer avec le contexte de l'activité
                            SimpleExoPlayer exoPlayer = new SimpleExoPlayer.Builder(context).build();

                            // Création de la source de média
                            MediaItem mediaItem = MediaItem.fromUri(audioUrl);

                            // Préparation du lecteur
                            exoPlayer.setMediaItem(mediaItem);
                            exoPlayer.prepare();
                            exoPlayer.setPlayWhenReady(true);
                        } catch (Exception e) {
                            Log.e("ReflowFragment", "Error creating ExoPlayer: " + e.getMessage());
                            Toast.makeText(getContext(), "An error occurred while playing media.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                Log.e("ReflowFragment", "Context is null. Unable to create ExoPlayer.");
                Toast.makeText(getContext(), "An error occurred while playing media.", Toast.LENGTH_SHORT).show();
            }

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
