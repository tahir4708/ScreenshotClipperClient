package com.googleupdaterunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class BackgroundRunner extends BroadcastReceiver {
    private View view;
    private Bitmap screenshot;
    private String fileName = "";

    public BackgroundRunner(View view) {
        this.view = view;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
//        Ringtone ringtone = RingtoneManager.getRingtone(context,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
//        ringtone.play();
//        Toast.makeText(context,"This is my background process" + Calendar.getInstance().getTime(),Toast.LENGTH_SHORT).show();
//
//        ringtone.stop();



    }

    public Object ScreenShotHelper(View view) {
        try {
            if (view != null && view.isLaidOut() && view.getWidth() > 0 && view.getHeight() > 0) {
                view.setDrawingCacheEnabled(true);
                view.buildDrawingCache();
                Bitmap drawingCache = view.getDrawingCache();
                if (drawingCache != null) {
                    screenshot = Bitmap.createBitmap(drawingCache);
                    // Do something with the screenshot
                }
                view.setDrawingCacheEnabled(false);

                if (screenshot != null) {
                    FileOutputStream outputStream = new FileOutputStream(fileName);
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    // Show a toast message with the file path
                    return screenshot;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}