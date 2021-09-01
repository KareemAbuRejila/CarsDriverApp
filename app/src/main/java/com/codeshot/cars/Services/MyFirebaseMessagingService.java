package com.codeshot.cars.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.codeshot.cars.Common.Common;
import com.codeshot.cars.CustomerCall;
import com.codeshot.cars.Helper.NotificationsHelper;
import com.codeshot.cars.Models.Token;
import com.codeshot.cars.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData()!=null){

            Map<String,String> data=remoteMessage.getData();
            String title=data.get("title");
//            String message=data.get("message");
            String lat=data.get("lat");
            String lng=data.get("lng");
            String requestId=data.get("requestId");
            String customerId=data.get("customerId");
            //Becouse i will send the Firebase message with contain lat and lng from Rider App
            //So i need convert message to LatLng
            if (Common.OnScreen)
            startCallActivity(title,lat,lng,requestId,customerId);
            else {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
                    showRequestNotificationAPI26(title,lat,lng,requestId,customerId);
                else
                    showRequestNotification(title,lat,lng,requestId,customerId);
            }


        }



    }

    private void startCallActivity( String customerToken, String lat, String lng, String requestId,String customerId) {
//        LatLng customerLocation=new Gson().fromJson(message,LatLng.class);
        Intent msgCallIntent=new Intent(getBaseContext(), CustomerCall.class);
        msgCallIntent.putExtra("lat",Double.valueOf(lat));
        msgCallIntent.putExtra("lng",Double.valueOf(lng));
        msgCallIntent.putExtra("requestId",requestId);
        msgCallIntent.putExtra("customerId",customerId);
        String notificationTitle=customerToken;
        msgCallIntent.putExtra("customerToken",notificationTitle);
        msgCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(msgCallIntent);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showRequestNotificationAPI26(String title, String lat, String lng, String requestId,String customerId) {
//        LatLng customerLocation=new Gson().fromJson(message,LatLng.class);
        Intent msgCallIntent=new Intent(getBaseContext(), CustomerCall.class);
        msgCallIntent.putExtra("lat",Double.valueOf(lat));
        msgCallIntent.putExtra("lng",Double.valueOf(lng));
        msgCallIntent.putExtra("requestId",requestId);
        msgCallIntent.putExtra("customerId",customerId);
        String notificationTitle=title;
        msgCallIntent.putExtra("customerToken",notificationTitle);

        PendingIntent contentIntent=PendingIntent.getActivity(getBaseContext(),
                0,msgCallIntent,PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationsHelper notificationsHelper=new NotificationsHelper(getBaseContext());
        Notification.Builder builder=notificationsHelper.getCarsNotification("New Request",
                lat+","+lng,
                contentIntent,
                defaultSound);
        notificationsHelper.getManager().notify(1,builder.build());
    }

    private void showRequestNotification(String title, String lat, String lng, String requestId,String customerId) {
//        LatLng customerLocation=new Gson().fromJson(message,LatLng.class);
        Intent msgCallIntent=new Intent(getBaseContext(), CustomerCall.class);
        msgCallIntent.putExtra("lat",Double.valueOf(lat));
        msgCallIntent.putExtra("lng",Double.valueOf(lng));
        msgCallIntent.putExtra("requestId",requestId);
        msgCallIntent.putExtra("customerId",customerId);
        String notificationTitle=title;
        msgCallIntent.putExtra("customerToken",notificationTitle);

        //This code only work for android api 25 and below
        //from android api 26 of higher i need create notification channel
        PendingIntent contentIntent=PendingIntent.getActivity(getBaseContext(),
                0,msgCallIntent,PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(getBaseContext());
        builder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("New Request")
                .setContentText(lat+","+lng)
                .setContentIntent(contentIntent);
        NotificationManager notificationManager=(NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1,builder.build());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(s1 -> {
            updateTokenToServer(s);
        });

        Log.d("NEW_TOKEN",s);
        SharedPreferences sharedPreferences = getSharedPreferences("com.codeshot.cars", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("token",s).apply();

    }
    private void updateTokenToServer(String newToken){
        FirebaseDatabase db= FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference().child(Common.token_tbl);

        Token token=new Token(newToken);
        if (FirebaseAuth.getInstance().getCurrentUser()!=null)//if already login, must update Token
        {
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .setValue(token).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i("DEVICE TOKEN","is updated to server");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("ERROR TOKEN",e.getMessage());
                }
            });
        }

    }

}
