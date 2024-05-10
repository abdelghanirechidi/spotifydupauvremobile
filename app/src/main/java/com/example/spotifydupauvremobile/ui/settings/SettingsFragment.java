package com.example.spotifydupauvremobile.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spotifydupauvremobile.R;
import com.example.spotifydupauvremobile.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSettings;
        settingsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Récupérer le bouton du layout
        Button buttonDarkMode = binding.getRoot().findViewById(R.id.button_dark_mode);
        // Définir un écouteur de clic pour le bouton
        buttonDarkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Basculez l'état du mode sombre
                boolean darkModeEnabled = settingsViewModel.isDarkModeEnabled().getValue();
                settingsViewModel.setDarkModeEnabled(!darkModeEnabled);
                // Mettre à jour le texte du bouton en fonction de l'état actuel du mode sombre
                buttonDarkMode.setText(darkModeEnabled ? "Activer le mode sombre" : "Désactiver le mode sombre");
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
