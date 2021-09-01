package com.codeshot.cars;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import com.codeshot.cars.Common.Common;
import com.codeshot.cars.Helper.DirectionsJSONParser;
import com.codeshot.cars.Models.DataMessage;
import com.codeshot.cars.Models.FCMResponse;
import com.codeshot.cars.Models.Token;
import com.codeshot.cars.Models.User;
import com.codeshot.cars.Remote.IFCMService;
import com.codeshot.cars.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTracking extends FragmentActivity implements OnMapReadyCallback,
GoogleApiClient.OnConnectionFailedListener,
GoogleApiClient.ConnectionCallbacks,
        LocationListener {
    //View
    private Button btnStartTrip;

    private GoogleMap mMap;

    private double riderLat, riderLng;
    private String customer;
    private Circle riderMarker;
    private Marker driverMarker;

    private Polyline directions;
    IGoogleAPI mServices;
    //PlayService
    private int PLAY_SERVICE_RES_REQUEST=7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private int UPDATE_INTERVAL=5000;
    private int FASTEST_INTERVAL =3000;
    private int DISPLACEMENT=10;

    private GeoFire geoFire;
    private IFCMService ifcmService;

    private Location pickupLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapDriving);
        mapFragment.getMapAsync(this);

        if (getIntent()!=null){
            riderLat=getIntent().getDoubleExtra("lat",-1.0);
            riderLng =getIntent().getDoubleExtra("lng",-1.0);
            customer=getIntent().getStringExtra("customer");
        }
        mServices=Common.getGoogleAPI();
        ifcmService=Common.getFCMService();
        setUpLocation();
        btnStartTrip=findViewById(R.id.btnStratTrip);

        btnStartTrip.setEnabled(true);

        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnStartTrip.getText().equals("Start Trip"))
                {
                    pickupLocation=Common.mLastLocation;
                    startTrip();
                    btnStartTrip.setText("Drop Off Here");

                }
                else if ((btnStartTrip.getText().equals("Drop Off Here")))
                {
                    catculateCashFee(pickupLocation,Common.mLastLocation);
                    btnStartTrip.setText("Start Trip");
                    finish();
                }
            }
        });
    }
    private void startTrip() {
        Token token=new Token(customer);
//        Notification notification=new Notification("Cancel","Driver has cancelled your request\n you can book another driver");
//        Sender sender=new Sender(token.getToken(),notification);

        Map<String,String> content=new HashMap<>();
        content.put("title","start trip");
        content.put("message","Driver has start your trip");
        DataMessage dataMessage=new DataMessage(token.getToken(),content);
        ifcmService=Common.getFCMService();
        ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                Toast.makeText(DriverTracking.this,"started",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.e("ERROR TRIP",t.getMessage());
            }
        });

    }

    private void dropHereRequest() {
        Token token=new Token(customer);
//        Notification notification=new Notification("Cancel","Driver has cancelled your request\n you can book another driver");
//        Sender sender=new Sender(token.getToken(),notification);

        Map<String,String> content=new HashMap<>();
        content.put("title","drop here");
        content.put("message","Driver was Drop your Trip\\n Please Rate this Drive");
        DataMessage dataMessage=new DataMessage(token.getToken(),content);
        ifcmService=Common.getFCMService();
        ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                Toast.makeText(DriverTracking.this,"Drop Here",Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.e("ERROR DROP",t.getMessage());
            }
        });

    }


    private void catculateCashFee(final Location pickupLocation, final Location mLastLocation) {

        //This function use same code with getDirections
        String requestAPI=null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin="+pickupLocation.getLatitude()+","+pickupLocation.getLongitude()+"&" +
                    "destination="+mLastLocation.getLatitude()+","+mLastLocation.getLongitude()+"&" +
                    "key="+getResources().getString(R.string.google_maps_key);
            Log.i("CalcultingRequestAPI",requestAPI);
            mServices.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            JSONObject jsonObject = null;
                            JSONArray routes =null;
                            try {
                                //Extract JSON
                                jsonObject = new JSONObject(response.body().toString());
                                routes = jsonObject.getJSONArray("routes");

                                JSONObject object=routes.getJSONObject(0);
                                JSONArray legs=object.getJSONArray("legs");

                                JSONObject legsObject=legs.getJSONObject(0);

                                //Get distance
                                JSONObject distanceObject=legsObject.getJSONObject("distance");
                                String distance=distanceObject.getString("text");
                                //Use regex to extract double from string
                                //this reges will remove all text not is digital
                                Double distanceValue=Double.parseDouble(distance.replaceAll("[^0-9\\\\.]",""));

                                //Get Time
                                JSONObject timeObject=legsObject.getJSONObject("duration");
                                String time=timeObject.getString("text");
                                int timeValue=Integer.parseInt(time.replaceAll("\\D+",""));

                                //Funcation To Calucate Total Price ot this Trip
                                //Create new Activity to show Trip details
                                Intent detailsMapIntent=new Intent(DriverTracking.this,TripDetails.class);
                                detailsMapIntent.putExtra("pickupLocation",pickupLocation);
                                detailsMapIntent.putExtra("mLastLocation",mLastLocation);
                                String startLocation,endLocation;
                                if (legsObject.has("start_address")) {
                                    startLocation=legsObject.getString("start_address");
                                }else {
                                    JSONObject startAddressObject=legsObject.getJSONObject("start_location");
                                    startLocation=startAddressObject.getString("lat")+","+startAddressObject.getString("lng");
                                }
                                if (legsObject.has("end_address")) {
                                    endLocation=legsObject.getString("end_address");
                                }else {
                                    JSONObject endAddressObject=legsObject.getJSONObject("end_location");
                                    endLocation=endAddressObject.getString("lat")+","+endAddressObject.getString("lng");
                                }
                                detailsMapIntent.putExtra("startLocation",startLocation);
                                detailsMapIntent.putExtra("endLocation",endLocation);

                                detailsMapIntent.putExtra("time",String.valueOf(timeValue));
                                detailsMapIntent.putExtra("distance",String.valueOf(distanceValue));
                                int totalPrice=(int)Common.getPrice(distanceValue,timeValue);
                                detailsMapIntent.putExtra("totalPrice",totalPrice);

                                detailsMapIntent.putExtra("riderLat",riderLat);
                                detailsMapIntent.putExtra("riderLng",riderLng);
                                dropHereRequest();
                                startActivity(detailsMapIntent);
                                
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this, t.getMessage(),Toast.LENGTH_LONG).show();

                        }
                    });


        }catch (Exception e)
        {
            e.printStackTrace();
        }


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        riderMarker=mMap.addCircle(new CircleOptions()
        .center(new LatLng(riderLat,riderLng))
        .radius(50) //50 => radias is 50m
        .strokeColor(Color.BLUE)
        .fillColor(0x220000FF)
        .strokeWidth(5.0f));

        //create Geo fencing with radius is 50m
        geoFire=new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driversAvailable_tbl));
        GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(riderLat,riderLng),0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendArrivedNotification(customer);
                btnStartTrip.setEnabled(true);
                Toast.makeText(DriverTracking.this,"onKeyEntered "+location.toString(),Toast.LENGTH_LONG).show();
            }
            @Override
            public void onKeyExited(String key) {
                Log.i("onKeyExited","Key = "+key);
                Toast.makeText(DriverTracking.this,"onKeyExited "+key.toString(),Toast.LENGTH_LONG).show();
            }
            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.i("onKeyMoved","Location : "+location.latitude+" "+location.longitude);
                Toast.makeText(DriverTracking.this,"onKeyMoved "+location.toString(),Toast.LENGTH_LONG).show();

            }
            @Override
            public void onGeoQueryReady() {
                Log.i("onGeoQueryReady","on geo query ready");
                Toast.makeText(DriverTracking.this,"onGeoQueryReady ",Toast.LENGTH_LONG).show();

            }
            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("ERROR ONGeoQuery",error.getMessage());
                Toast.makeText(DriverTracking.this,"onGeoQueryError "+error.getMessage(),Toast.LENGTH_LONG).show();


            }
        });
    }

    private void sendArrivedNotification(String customer) {
        Token token=new Token(customer);
//        Notification notification=new Notification("Arrived",String.format("The Driver it's Arrived at your location"));
//        Sender sender=new Sender(token.getToken(),notification);
        Map<String,String> content=new HashMap<>();
        content.put("title","Arrived");
        content.put("message",String.format("The Driver it's Arrived at your location ",Common.currentUser.getUserName()));
        DataMessage dataMessage=new DataMessage(token.getToken(),content);
        ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success==1) {
                    Toast.makeText(DriverTracking.this,"Successful "+response.message(),Toast.LENGTH_LONG).show();
                }
                    if (response.body().success!=1){
                    Toast.makeText(DriverTracking.this,"Failed "+response.errorBody(),Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.e("ERROR SEND Arrived",t.getMessage());
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
        FirebaseDatabase.getInstance().getReference(Common.drivers_tbl)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Common.currentUser=dataSnapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("GoogleAPI Client",connectionResult.getErrorMessage());
    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation=location;
        displayLocation();


    }
    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                &&ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            return;
        }
        Common.mLastLocation= LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation!=null){
                final double latitude=Common.mLastLocation.getLatitude();
                final double longitude=Common.mLastLocation.getLongitude();

                if (driverMarker!=null)
                    driverMarker.remove();
                driverMarker=mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude))
                .title("YOU")
                .icon(BitmapDescriptorFactory.defaultMarker()));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),17.0f));

                if (directions!=null)
                    directions.remove();
                getDirections();
        }

    }

    private void getDirections() {

        LatLng currentPosition=new LatLng(Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude());

        String requestAPI=null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&" +
                    "destination="+riderLat+","+riderLng+"&" +
                    "key="+getResources().getString(R.string.google_maps_key);
            Log.i("DirectionTracking API",requestAPI);
            mServices.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                new ParserTask().execute(response.body().toString());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this, t.getMessage(),Toast.LENGTH_LONG).show();

                        }
                    });
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                    &&ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);

        }
    }

    private void setUpLocation() {
            if (checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
    }
    private void createLocationRequest() {
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }
    private void buildGoogleApiClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    private boolean checkPlayServices() {
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode!= ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICE_RES_REQUEST).show();
            }else {
                Toast.makeText(this,"This Device is not supported",Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }


    private class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {

        ProgressDialog progressDialog=new ProgressDialog(DriverTracking.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Please waiting....");
            progressDialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String,String>>> routes=null;
            try {
                jsonObject=new JSONObject(strings[0]);
                DirectionsJSONParser parser=new DirectionsJSONParser();
                routes=parser.parse(jsonObject);
            }catch (JSONException e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            progressDialog.dismiss();
            ArrayList points=null;
            PolylineOptions polylineOptions=null;
            for (int i=0;i<lists.size();i++){
                points=new ArrayList();
                polylineOptions=new PolylineOptions();
                List<HashMap<String,String>> path=lists.get(i);
                for (int j=2;j<path.size();j++){
                    HashMap<String,String> point=path.get(j);
                    double lat=Double.parseDouble(point.get("lat"));
                    double lng=Double.parseDouble(point.get("lng"));
                    LatLng position=new LatLng(lat,lng);
                    points.add(position);
                }
                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.RED);
                polylineOptions.geodesic(true);
            }
            if (polylineOptions!=null)
            directions=mMap.addPolyline(polylineOptions);
            else Toast.makeText(DriverTracking.this,"PolyLineOptions is Null",Toast.LENGTH_LONG).show();
        }
    }
    private void cancelBooking() {
        Token token=new Token(customer);
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
                Toast.makeText(DriverTracking.this,"Cancelled",Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.e("ERROR NOTICE",t.getMessage());
            }
        });

    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel")
                .setMessage("Are you sure to cancel this trip ?!!!!")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelBooking();
                        DriverTracking.super.onBackPressed();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                return;
            }
        }).show();

    }
}
