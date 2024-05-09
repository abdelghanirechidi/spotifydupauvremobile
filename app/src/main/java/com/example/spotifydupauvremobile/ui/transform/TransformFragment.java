package com.example.spotifydupauvremobile.ui.transform;

import static kotlin.io.ByteStreamsKt.readBytes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotifydupauvremobile.R;
import com.example.spotifydupauvremobile.databinding.FragmentTransformBinding;
import com.example.spotifydupauvremobile.databinding.ItemTransformBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import com.example.spotifydupauvremobile.ui.reflow.MusicIce.MusicPrx;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.LocalException;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.SocketException;
import com.zeroc.Ice.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
public class TransformFragment extends Fragment {

    private static final int REQUEST_PICK_AUDIO_FILE = 1;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private FragmentTransformBinding binding;

    Communicator communicator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TransformViewModel transformViewModel = new ViewModelProvider(this).get(TransformViewModel.class);
        binding = FragmentTransformBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        communicator = Util.initialize();
        RecyclerView recyclerView = binding.recyclerviewTransform;
        recyclerView.setAdapter(new TransformAdapter());
        transformViewModel.getTexts().observe(getViewLifecycleOwner(), strings -> {
            ListAdapter adapter = (ListAdapter) recyclerView.getAdapter();
            if (adapter != null) {
                adapter.submitList(strings);
            }
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }


        binding.fabAddItem.setOnClickListener(v -> openFilePicker());

        return root;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_PICK_AUDIO_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_AUDIO_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    byte[] audioBytes = readBytesFromUri(uri);
                    String fileName = getFileNameFromUri(uri);
                    String title;
                    String author;
                    fileName = fileName.replace(".mp3", "");
                    int underscoreIndex = fileName.indexOf('_');
                    if (underscoreIndex != -1) {
                        title = fileName.substring(0, underscoreIndex);
                        author = fileName.substring(underscoreIndex + 1);
                    } else {
                        title = fileName;
                        author = "Inconnu";
                    }
                    //playAudio(audioBytes);
                    envoyerFichierAudioEnChunks(uri,title,author);

                    Intent intent = requireActivity().getIntent();
                    requireActivity().finish();
                    requireActivity().startActivity(intent);
                    //envoyerChunkAuServeur(title, author, audioBytes);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Erreur lors de la lecture du fichier audio", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    // Fonction pour envoyer un fichier audio en chunks au serveur
    public void envoyerFichierAudioEnChunks(Uri uri, String titre, String auteur) {
        try {
            byte[] audioBytes = readBytesFromUri(uri);
            int chunkSize = 8192; // Taille du chunk en octets
            int offset = 0;
            while (offset < audioBytes.length) {
                int length = Math.min(chunkSize, audioBytes.length - offset);
                byte[] chunk = Arrays.copyOfRange(audioBytes, offset, offset + length);
                envoyerChunkAuServeur(titre, auteur, chunk);
                offset += length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void envoyerChunkAuServeur(String titre, String auteur, byte[] chunk) {

        try {

            String proxyStr = "MusicService:tcp -h 192.168.1.62 -p 10000";

            ObjectPrx base = communicator.stringToProxy(proxyStr);
            if (base == null) {
                Log.e("Error", "Invalid proxy");
                return;
            }

            MusicPrx musicService = MusicPrx.checkedCast(base);
            if (musicService == null) {
                Log.e("Error", "Invalid MusicPrx");
                return;
            }

            String chunkBase64 = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                chunkBase64 = Base64.getEncoder().encodeToString(chunk);
            }
            if (chunkBase64 == null) {
                Log.e("Error", "chunkBase64 is null");
                return;
            }
            {
                CompletableFuture<Void> future = musicService.envoyerMusiqueAsync(titre, auteur, chunkBase64);
                future.exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            }

        } catch (LocalException e) {
            Log.e("Local Exception", e.getMessage());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }



    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri != null) {
            Cursor cursor = null;
            try {
                ContentResolver contentResolver = requireContext().getContentResolver();
                cursor = contentResolver.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return fileName;
    }


    private void playAudio(byte[] audioBytes) {
        try {
            // Créer un fichier temporaire pour stocker les données audio
            File tempAudioFile = File.createTempFile("temp_audio", ".mp3", requireActivity().getFilesDir());

            // Écrire les données audio dans le fichier temporaire
            FileOutputStream fos = new FileOutputStream(tempAudioFile);
            fos.write(audioBytes);
            fos.close();

            // Lire le fichier audio temporaire avec MediaPlayer
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempAudioFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Nettoyer le fichier temporaire après la lecture
            mediaPlayer.setOnCompletionListener(mp -> {
                tempAudioFile.delete();
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }



    private byte[] readBytesFromUri(@NonNull Uri uri) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ContentResolver contentResolver = requireActivity().getContentResolver();

        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return outputStream.toByteArray();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class TransformAdapter extends ListAdapter<String, TransformViewHolder> {

        private final List<Integer> drawables = Arrays.asList(
                R.drawable.avatar_1,
                R.drawable.avatar_2,
                R.drawable.avatar_3,
                R.drawable.avatar_4,
                R.drawable.avatar_5,
                R.drawable.avatar_6,
                R.drawable.avatar_7,
                R.drawable.avatar_8,
                R.drawable.avatar_9,
                R.drawable.avatar_10,
                R.drawable.avatar_11,
                R.drawable.avatar_12,
                R.drawable.avatar_13,
                R.drawable.avatar_14,
                R.drawable.avatar_15,
                R.drawable.avatar_16);

        protected TransformAdapter() {
            super(new DiffUtil.ItemCallback<String>() {
                @Override
                public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @NonNull
        @Override
        public TransformViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemTransformBinding binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new TransformViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull TransformViewHolder holder, @SuppressLint("RecyclerView") int position) {
            holder.textView.setText(getItem(position));
            /*
            holder.imageView.setImageDrawable(
                    ResourcesCompat.getDrawable(holder.imageView.getResources(),
                            drawables.get(position),
                            null));*/

            //System.out.println(getItem(position));
            loadImageFromApi(getItem(position), holder.imageView);

            holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String itemText = getItem(position);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Communicator communicator = null;
                            try {
                                communicator = com.zeroc.Ice.Util.initialize();
                                if (communicator != null) {
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

                                    musicService.supprimerMusique(itemText);

                                    Intent intent = requireActivity().getIntent();
                                    requireActivity().finish();
                                    requireActivity().startActivity(intent);

                                }
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
                    }).start();
                }
            });

        }

        private void loadImageFromApi(String item, ImageView imageView) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    // Appeler la méthode de recherche d'image de l'API Google Custom Search
                    return searchImageApi(item, "AIzaSyCfE_SiSiOcxnvE3NSGNNGCtxdMeBY7KxQ", "343fdfbc420124731");
                }

                @Override
                protected void onPostExecute(String imageUrl) {
                    // Charger et afficher l'image dans l'ImageView avec Picasso
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get()
                                .load(imageUrl)
                                .resize(300, 300)
                                .centerCrop()
                                .into(imageView);
                    } else {
                        // Si aucune image n'est trouvée, utilisez une image de placeholder
                        imageView.setImageDrawable(ResourcesCompat.getDrawable(imageView.getResources(), R.drawable.placeholder_image, null));
                    }
                }
            }.execute();
        }

        public String searchImageApi(String musicTitle, String apiKey, String searchEngineId) {
            try {
                // Construire l'URL de requête
                musicTitle = musicTitle + " cover";
                String apiUrl = "https://www.googleapis.com/customsearch/v1" +
                        "?key=" + apiKey +
                        "&cx=" + searchEngineId +
                        "&q=" + musicTitle +
                        "&searchType=image" +
                        "&num=1";

                System.out.println(apiUrl);

                // Ouvrir la connexion
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Lire la réponse
                Scanner scanner = new Scanner(conn.getInputStream());
                StringBuilder responseBuilder = new StringBuilder();
                while (scanner.hasNextLine()) {
                    responseBuilder.append(scanner.nextLine());
                }
                scanner.close();

                // Analyser la réponse JSON
                JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
                JSONArray items = jsonResponse.getJSONArray("items");
                if (items.length() > 0) {
                    JSONObject firstItem = items.getJSONObject(0);
                    return firstItem.getString("link");
                } else {
                    Log.e("ImageSearcher", "Aucune image trouvée pour : " + musicTitle);
                    return null;
                }
            } catch (IOException | JSONException e) {
                Log.e("ImageSearcher", "Erreur lors de la recherche d'image : " + e.getMessage());
                return null;
            }
        }
    }

    private static class TransformViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView textView;
        private final ImageButton buttonDelete;

        public TransformViewHolder(ItemTransformBinding binding) {
            super(binding.getRoot());
            imageView = binding.imageViewItemTransform;
            textView = binding.textViewItemTransform;
            buttonDelete = binding.buttonDelete;

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setAdjustViewBounds(true);

        }
    }
}