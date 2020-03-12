package com.example.narendra.eatfoodshipper.Helper;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import com.example.narendra.eatfoodshipper.R;

public class NotificationHelper extends ContextWrapper {
    private static final String CHANNEL_ID="com.example.narendra.eatfoodshipper.Helper";
    private static final String CHANNEL_NAME="Eat It";
    private NotificationManager manager;
    public NotificationHelper(Context base) {
        super(base);
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O)
            createChannel();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel=new NotificationChannel(CHANNEL_ID,CHANNEL_NAME,NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableLights(false);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(channel);
    }

    public NotificationManager getManager() {
        if(manager==null)
            manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        return manager;
    }
    @TargetApi(Build.VERSION_CODES.O)
    public android.app.Notification.Builder getChannelNotification(String title, String body, PendingIntent contentIntent,
                                                                   Uri soundUri)
    {
        return new android.app.Notification.Builder(getApplicationContext(),CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_local_shipping_black_24dp)
                .setSound(soundUri)
                .setAutoCancel(false);
    }
    @TargetApi(Build.VERSION_CODES.O)
    public android.app.Notification.Builder getChannelNotification(String title, String body,
                                                                   Uri soundUri)
    {
        return new android.app.Notification.Builder(getApplicationContext(),CHANNEL_ID)

                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setAutoCancel(false);
    }
}

