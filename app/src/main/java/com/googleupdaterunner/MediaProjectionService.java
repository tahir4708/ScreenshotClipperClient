package com.googleupdaterunner;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MediaProjectionService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
