package com.example.spotifydupauvremobile.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private MutableLiveData<Boolean> darkModeEnabled;

    public SettingsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Les paramètres");

        darkModeEnabled = new MutableLiveData<>();
        darkModeEnabled.setValue(false); // Mode sombre désactivé par défaut
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Boolean> isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean enabled) {
        darkModeEnabled.setValue(enabled);
        // Ici, vous pouvez ajouter d'autres logiques en fonction de l'état du mode sombre
    }
}
