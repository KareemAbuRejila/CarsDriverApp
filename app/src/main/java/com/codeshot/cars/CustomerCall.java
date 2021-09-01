package com.codeshot.cars;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.codeshot.cars.Common.Common;
import com.codeshot.cars.Models.DataMessage;
import com.codeshot.cars.Models.FCMResponse;
import com.codeshot.cars.Models.Token;
import com.codeshot.cars.Remote.IFCMService;
import com.codeshot.cars.Remote.IGoogleAPI;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerCall extends AppCompatActivity {
    private TextView tvTime,tvAddress,tvDistance;
    private Button btnAcceptReq,btnCancelReq;
    private MediaPlayer mediaPlayer;
    private IGoogleAPI mServices;
    private String customerToken,requestId,customerId;
    private IFCMService ifcmService;

    private double lat,lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_call);
        mServices=Common.getGoogleAPI();

        //InitView
        tvTime=findViewById(R.id.tvTimeCall);
        tvAddress=findViewById(R.id.tvAddressCall);
        tvDistance=findViewById(R.id.tvDistanceCall);
        btnAcceptReq=findViewById(R.id.btnAcceptReq);
        btnCancelReq=findViewById(R.id.btnCancelReq);
        mediaPlayer=MediaPlayer.create(this,R.raw.noti_rang);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        btnCancelReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(customerToken)){
                    cancelBooking();
                }
            }
        });
        btnAcceptReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent driverTrackingIntent=new Intent(CustomerCall.this, DriverTracking.class);
                //SEnd Customer Location to new activity
                driverTrackingIntent.putExtra("lat",lat);
                driverTrackingIntent.putExtra("lng",lng);
                driverTrackingIntent.putExtra("customer", customerToken);

                startActivity(driverTrackingIntent);
                finish();
            }
        });

        if (getIntent()!=null){
            lat=getIntent().getDoubleExtra("lat",-1.0);
            lng=getIntent().getDoubleExtra("lng",-1.0);
            customerToken =getIntent().getStringExtra("customerToken");
            requestId=getIntent().getStringExtra("requestId");
            customerId=getIntent().getStringExtra("customerId");

            //getDirections from Map
            getDirections(lat,lng);

            FirebaseDatabase.getInstance().getReference().child("Requests").child(customerId)
                    .child(requestId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    FirebaseDatabase.getInstance().getReference().child("Riders")
                            .child(dataSnapshot.child("from").getValue().toString())
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    ImageView imageView=findViewById(R.id.circleImageView);
                                    TextView customerName=findViewById(R.id.tvCustomerNAme);
                                    customerName.setText(Objects.requireNonNull(dataSnapshot.child("userName").getValue()).toString());
                                    Picasso.get().load(Objects.requireNonNull(dataSnapshot.child("imageUrl").getValue()).toString()).into(imageView);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    private void cancelBooking() {
        Token token=new Token(customerToken);
//        Notification notification=new Notification("Cancel","Driver has cancelled your request\n you can book another driver");
//        Sender sender=new Sender(token.getToken(),notification);

        Map<String,String> content=new HashMap<>();
        content.put("title","Cancel");
        content.put("message","Driver has cancelled your request\\n you can book another driver");
        DataMessage dataMessage=new DataMessage(token.getToken(),content);
        ifcmService=Common.getFCMService();
        ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                Toast.makeText(CustomerCall.this,"Cancelled",Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.e("ERROR NOTICE",t.getMessage());
            }
        });

    }

    private void getDirections(double lat, double lng) {
        String requestAPI=null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin="+Common.mLastLocation.getLatitude()+","+Common.mLastLocation.getLongitude()+"&" +
                    "destination="+lat+","+lng+"&" +
                    "key="+getResources().getString(R.string.google_maps_key);
            Log.i("Direction Request API",requestAPI);
            mServices.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject=new JSONObject(response.body().toString());
                                JSONArray routes=jsonObject.getJSONArray("routes");
                                //after get routes , i need to get first element of routes
                                JSONObject object=routes.getJSONObject(0);
                                //after get first element , i need to get array with name 'legs'
                                JSONArray legs=object.getJSONArray("legs");
                                //and get first element of legs array
                                JSONObject legsObject=legs.getJSONObject(0);
                                //Now, getDistance
                                 JSONObject distance=legsObject.getJSONObject("distance");
                                 tvDistance.setText(distance.getString("text"));
                                 //getTime
                                JSONObject time=legsObject.getJSONObject("duration");
                                tvTime.setText(time.getString("text"));
                                //getAddress
                                String address=legsObject.getString("end_address");
                                tvAddress.setText(address);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(CustomerCall.this, t.getMessage(),Toast.LENGTH_LONG).show();
                            Log.e("Get Dirction error",t.getLocalizedMessage());

                        }
                    });
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();
    }
}
