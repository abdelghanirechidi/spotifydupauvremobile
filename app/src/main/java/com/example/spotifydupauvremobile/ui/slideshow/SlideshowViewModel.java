package com.example.spotifydupauvremobile.ui.slideshow;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SlideshowViewModel extends ViewModel {

    private final MutableLiveData<String> title;
    private final MutableLiveData<String> artist;
    private final MutableLiveData<String> path;

    public SlideshowViewModel() {
        title = new MutableLiveData<>();
        artist = new MutableLiveData<>();
        path = new MutableLiveData<>();

    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getArtist() {
        return artist;
    }

    public LiveData<String> getPath() {
        return path;
    }
}

