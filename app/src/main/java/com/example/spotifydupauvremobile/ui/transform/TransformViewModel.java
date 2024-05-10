package com.example.spotifydupauvremobile.ui.transform;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spotifydupauvremobile.ui.transform.MusicIce.MusicPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import java.util.ArrayList;
import java.util.List;
public class TransformViewModel extends ViewModel {

    private final MutableLiveData<List<String>> mTexts;

    public TransformViewModel() {
        mTexts = new MutableLiveData<>();
        listerMusiquesDisponibles();
    }

    public LiveData<List<String>> getTexts() {
        return mTexts;
    }

    // Méthode pour lister les musiques disponibles
    private void listerMusiquesDisponibles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Communicator communicator = null;
                try {
                    communicator = Util.initialize();
                    String proxyStr = "MusicService:tcp -h 192.168.1.62 -p 10000";

                    com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(proxyStr);
                    if (base == null) {
                        Log.e("Error", "Proxy invalide");
                        return;
                    }

                    MusicPrx musicService = MusicPrx.checkedCast(base);
                    if (musicService == null) {
                        Log.e("Error", "MusicPrx invalide");
                        return;
                    }

                    String[] musiques = musicService.listerMusiques();
                    List<String> musiquesList = new ArrayList<>();
                    for (String musique : musiques) {
                        musiquesList.add(musique);
                    }
                    mTexts.postValue(musiquesList);

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


}