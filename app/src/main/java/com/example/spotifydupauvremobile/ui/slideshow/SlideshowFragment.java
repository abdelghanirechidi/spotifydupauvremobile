package com.example.spotifydupauvremobile.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.spotifydupauvremobile.databinding.FragmentSlideshowBinding;
import com.example.spotifydupauvremobile.ui.reflow.MusicIce.MusicPrx;
import com.google.android.material.snackbar.Snackbar;
import com.zeroc.Ice.Communicator;

public class SlideshowFragment extends Fragment {

    private SlideshowViewModel slideshowViewModel;
    private FragmentSlideshowBinding binding;
    private EditText editTextTitle;
    private EditText editTextArtist;
    private EditText editTextAlbum;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        slideshowViewModel = new ViewModelProvider(this).get(SlideshowViewModel.class);
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialisation des EditText
        editTextTitle = binding.editTextTitle;
        editTextArtist = binding.editTextArtist;
        editTextAlbum = binding.editTextAlbum;

        // Observer pour mettre à jour les champs du formulaire avec les données de la musique
        slideshowViewModel.getTitle().observe(getViewLifecycleOwner(), title -> {
            editTextTitle.setText(title);
        });

        slideshowViewModel.getArtist().observe(getViewLifecycleOwner(), artist -> {
            editTextArtist.setText(artist);
        });

        slideshowViewModel.getPath().observe(getViewLifecycleOwner(), path -> {
            editTextAlbum.setText(path);
        });

        // Récupération du bouton "Modifier" depuis la liaison
        Button buttonModify = binding.buttonModify;

        // Ajout d'un écouteur de clic sur le bouton "Modifier"
        buttonModify.setOnClickListener(v -> {
            // Récupérer les nouvelles valeurs des champs de texte
            String titre = editTextTitle.getText().toString();
            String nouveautitre = editTextArtist.getText().toString();
            String nouvelartiste = editTextAlbum.getText().toString();


            // Appeler la méthode pour modifier la musique avec les nouvelles valeurs
            modifyMusic(titre, nouveautitre, nouvelartiste);
        });

        return root;
    }

    // Méthode pour modifier la musique
    private void modifyMusic(String titre, String nouveautitre, String nouvelartiste) {
        Log.d("SlideshowFragment", "Titre : " + titre);
        Log.d("SlideshowFragment", "Nouveau titre : " + nouveautitre);
        Log.d("SlideshowFragment", "Nouvel artiste : " + nouvelartiste);
        String nouveauFichierAudio = nouveautitre + ".mp3";

        Toast.makeText(getContext(), titre + " a été modifiée avec succès",  Toast.LENGTH_SHORT).show();

        new Thread(() -> {
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

                musicService.modifierMusique(titre, nouveautitre, nouvelartiste, nouveauFichierAudio);
            } catch (com.zeroc.Ice.LocalException e) {
                Log.e("Local Exception", e.getMessage());
            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            } finally {
                if (communicator != null) {
                    communicator.destroy();
                }
            }
        }).start();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
