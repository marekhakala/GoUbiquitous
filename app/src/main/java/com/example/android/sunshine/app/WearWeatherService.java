package com.example.android.sunshine.app;

import android.util.Log;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public class WearWeatherService extends WearableListenerService {
    private static final String PATH = "/weather";
    private static final String TAG = WearWeatherService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                Log.d(TAG, "PATH: " + path);

                if (path.equals(PATH))
                    SunshineSyncAdapter.syncImmediately(this);
            }
        }
    }
}