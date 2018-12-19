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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class PortraitBlurService extends IntentService {
    private static int FOREGROUND_ID=1337;
    String image_path;
    public PortraitBlurService() {
        super("PortraitBlurService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        image_path=(String)intent.getExtras().get("path");
        Dbhandler dbhandler=new Dbhandler(this,null,null,1);
        Notification notification;
        String CHANNEL_ID = "com.anondev.gaurav.camerablur.PortraitBlurService";
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
        int ar[]=DeeplabProcessor.GetBlurredImage(bitmap);
        bitmap=getBitmap(ar,bitmap,7,4);


        FileOutputStream out = null;

        if(bitmap!=null)
        try {
            entry e=new entry();
            File dst = File.createTempFile(
                    "Blurred",  /* prefix */
                    ".jpg",         /* suffix */
                    getExternalFilesDir("Pictures")      /* directory */
            );
            out = new FileOutputStream(dst);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            e.setPath(dst.getPath());
            dst = File.createTempFile(
                    "SegMap",  /* prefix */
                    ".mp",         /* suffix */
                    getExternalFilesDir("Map")      /* directory */
            );
            out = new FileOutputStream(dst);
            ByteBuffer byteBuffer = ByteBuffer.allocate(ar.length * 4);
            byteBuffer.asIntBuffer().put(ar);
            out.write(byteBuffer.array());
            e.setMap(dst.getPath());
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
    public Bitmap getBitmap(int mOutputs[],Bitmap bitmap,int bRad,int eRad){
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Bitmap blur=ImageUtils.RenderBlur(PortraitBlurService.this,bitmap,bRad);
        Bitmap softBlur=ImageUtils.RenderBlur(PortraitBlurService.this, bitmap,eRad);

        int imgMatrixEroded[][]=new int[w][h];
        int imgMatrixDilated[][]=new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                imgMatrixEroded[x][y]=imgMatrixDilated[x][y]=mOutputs[y * w + x];
            }
        }
        imgMatrixDilated=ImageUtils.dilate(imgMatrixDilated,1);
        imgMatrixEroded=ImageUtils.erode(imgMatrixEroded,2);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                output.setPixel(x, y,imgMatrixDilated[x][y]==1 ?  (imgMatrixEroded[x][y]==1?bitmap.getPixel(x,y):softBlur.getPixel(x,y)):blur.getPixel(x,y));
            }
        }
        return output;
    }


}
