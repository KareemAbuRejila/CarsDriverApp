package com.codeshot.cars.Helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.codeshot.cars.R;

public class NotificationsHelper extends ContextWrapper {

    private static final String CARS_CHANNEL_ID="com.codeshot.carscustomerapp";
    private static final String CARS_CHANNEL_NAME="Cars App";

    private NotificationManager notificationManager;

    public NotificationsHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            createChannels();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {

        NotificationChannel carsChannel=new NotificationChannel(CARS_CHANNEL_ID
        ,CARS_CHANNEL_NAME
        , NotificationManager.IMPORTANCE_DEFAULT);
        carsChannel.enableLights(true);
        carsChannel.enableVibration(true);
        carsChannel.setLightColor(getColor(R.color.colorPrimaryDark));
        carsChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(carsChannel);
    }

    public NotificationManager getManager() {

        if (notificationManager==null)
            notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getCarsNotification(String title, String content, PendingIntent contentIntent, Uri soundUri)
    {
     return new Notification.Builder(getApplicationContext(),CARS_CHANNEL_ID)
             .setContentText(content)
             .setContentTitle(title)
             .setSound(soundUri)
             .setContentIntent(contentIntent)
             .setSmallIcon(R.drawable.cars_logo_3);
    }
}
