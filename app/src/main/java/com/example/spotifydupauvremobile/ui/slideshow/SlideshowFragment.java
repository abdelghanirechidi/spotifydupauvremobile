package com.example.spotifydupauvremobile.ui.slideshow;

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
import com.google.android.material.snackbar.Snackbar;

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
            String modifiedTitle = editTextTitle.getText().toString();
            String modifiedArtist = editTextArtist.getText().toString();
            String modifiedPath = editTextAlbum.getText().toString();

            // Appeler la méthode pour modifier la musique avec les nouvelles valeurs
            modifyMusic(modifiedTitle, modifiedArtist, modifiedPath);
        });

        return root;
    }

    // Méthode pour modifier la musique
    private void modifyMusic(String title, String artist, String path) {
        // Ici, vous devriez implémenter la logique pour modifier la musique
        // Pour l'instant, nous affichons simplement les nouvelles valeurs dans la console
        Log.d("SlideshowFragment", "Nouveau titre : " + title);
        Log.d("SlideshowFragment", "Nouvel artiste : " + artist);
        Log.d("SlideshowFragment", "Nouveau chemin : " + path);

        Toast.makeText(getContext(), "Musique modifiée avec succès",  Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
