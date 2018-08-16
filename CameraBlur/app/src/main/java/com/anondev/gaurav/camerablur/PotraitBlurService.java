package com.anondev.gaurav.camerablur;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class PotraitBlurService extends IntentService {
    private static int FOREGROUND_ID=1337;
    String image_path;
    public PotraitBlurService() {
        super("PotraitBlurService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        image_path=(String)intent.getExtras().get("path");
        Dbhandler dbhandler=new Dbhandler(this,null,null,1);
        Notification notification;
        String CHANNEL_ID = "com.anondev.gaurav.camerablur.PotraitBlurService";
        String CHANNEL_NAME = "Channel One";
        NotificationChannel notificationChannel = null;
        NotificationManager notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);
            notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                    .setContentTitle("Processing Image")
                    .setContentText("Processing..")
                    .setSmallIcon(android.R.drawable.stat_notify_sync).
                            setTicker("Processing I..").build();
        }else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Processing Image")
                    .setContentText("Processing..")
                    .setSmallIcon(android.R.drawable.stat_notify_sync).
                            setTicker("Processing I..").build();
        }

        startForeground(FOREGROUND_ID,notification);
        if(!DeeplabProcessor.isInitialized())
            DeeplabProcessor.initialize(getBaseContext());
        Bitmap bitmap=ImageUtils.decodeBitmapFromFile(image_path,DeeplabProcessor.INPUT_SIZE,DeeplabProcessor.INPUT_SIZE);
        if(bitmap==null)
            Log.e("PotraitBlur","Null Bitmap");
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        float resizeRatio = (float) DeeplabProcessor.INPUT_SIZE / Math.max(bitmap.getWidth(), bitmap.getHeight());
        int rw = Math.round(w * resizeRatio);
        int rh = Math.round(h * resizeRatio);

        bitmap = ImageUtils.tfResizeBilinear(bitmap, rw, rh);
        bitmap=DeeplabProcessor.GetBlurredImage(bitmap);


        FileOutputStream out = null;

        if(bitmap!=null)
        try {
            File dst = File.createTempFile(
                    "Blurred",  /* prefix */
                    ".jpg",         /* suffix */
                    getExternalFilesDir("Pictures")      /* directory */
            );
            out = new FileOutputStream(dst);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            entry e=new entry();
            e.setPath(dst.getPath());
            dbhandler.addRow(e);
            Intent localIntent = new Intent("PotraitFinished");
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else Log.e("PotraitBlur","Null Bitmap");
        stopForeground(true);


    }


}
