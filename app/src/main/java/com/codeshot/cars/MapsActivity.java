package com.codeshot.cars;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.codeshot.cars.Common.Common;
import com.codeshot.cars.Models.Token;
import com.codeshot.cars.Remote.IGoogleAPI;
import com.codeshot.cars.Starting.LoginActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    //View
    private SwitchCompat location_switch;
    private SupportMapFragment mapFragment;
    private FloatingActionButton btnMapClear;
    private ProgressBar pbMap;

    //Firebase
    private FirebaseAuth mAuth;
    private String currentUserID;
    private DatabaseReference rootRef, driversAvailableRef;
    private GeoFire geoFire;
    //Map
    private GoogleMap mMap;
    //PlayService
    private int My_PERMISSION_REQUEST_CODE=7000;
    private int PLAY_SERVICE_RES_REQUEST=7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private int UPDATE_INTERVAL=5000;
    private int FASTEST_INTERVAL =3000;
    private int DISPLACEMENT=10;

    private Marker marker;
    private Boolean idDriverOnline=false;

    //Car Animation
    private List<LatLng> polyLineList;
    private Marker carMarker;
    private float v;
    private double lat,lng;
    private Handler handler;
    private LatLng startPosition,endPosition,currentPosition,destinationPosition;
    private int index,next;
    private AutocompleteSupportFragment placeAutocompleteFragment;
    private String destination;
    private PolylineOptions polylineOptions, blackPolyLineOptions;
    private Polyline blackPolyLine,grayPolyLine;

    private IGoogleAPI mServices;

    //Presense System to change between online and offline mode
    private DatabaseReference onlineRef,currentUserRef;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapHome);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        initializations();
        location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isOnline) {
                if (isOnline) {
                    pbMap.setVisibility(View.VISIBLE);
                    FirebaseDatabase.getInstance().goOnline();//set connected when switch to on
                    startLocationUpdates();
                    displayLocation();
                    idDriverOnline=true;

                }else {
                    if (mMap.isMyLocationEnabled()){
                        stopLocationUpdates();//set disconnected when switch to off
                        mMap.clear();
                        if (getDrawPathRunnable()!=null&&handler!=null)
                        handler.removeCallbacks(getDrawPathRunnable());
                        Snackbar.make(mapFragment.getView(),"You are Offline",Snackbar.LENGTH_SHORT).show();
                        idDriverOnline=false;
                    }
                    pbMap.setVisibility(View.GONE);

                }
            }
        });
        polyLineList=new ArrayList<>();
        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                if (location_switch.isChecked()){
                    destination=place.getName().toString();
                    destination=destination.replace(" ","+");
                    Log.i("Destination Location : ",destination);
                    destinationPosition=place.getLatLng();
                    getDirections();
                    btnMapClear.show();
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MapsActivity.this,status.getStatus().toString(),Toast.LENGTH_SHORT).show();

            }
        });
        placeAutocompleteFragment.getView().findViewById(R.id.places_autocomplete_clear_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        placeAutocompleteFragment.setText("");
                        v.setVisibility(View.GONE);
                        mMap.clear();
                        displayLocation();
                        btnMapClear.hide();
                    }
                });
        btnMapClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                displayLocation();
                btnMapClear.hide();
            }
        });
        setUpLocation();
        mServices= Common.getGoogleAPI();
        updateTokenToServer();
    }
    private void updateTokenToServer(){
        FirebaseDatabase db= FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference().child(Common.token_tbl);
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(s -> {
            Token token=new Token(s);
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
        });


    }
    private void updateTokenToServer2(){
        FirebaseDatabase db= FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference().child(Common.token_tbl);
        SharedPreferences sharedPreferences = getSharedPreferences("com.codeshot.cars", Context.MODE_PRIVATE);
        String newToken=sharedPreferences.getString("token","");
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
    private void getDirections() {

        currentPosition=new LatLng(Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude());

        String requestAPI=null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&" +
                    "destination="+destinationPosition.latitude+","+destinationPosition.longitude+"&" +
                    "key="+getResources().getString(R.string.google_maps_key);
            Log.i("Direction Request API",requestAPI);
            mServices.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject=new JSONObject(response.body().toString());
                                JSONArray jsonArray=jsonObject.getJSONArray("routes");
                                for (int i=0;i<jsonArray.length();i++){
                                    JSONObject route=jsonArray.getJSONObject(i);
                                    JSONObject poly=route.getJSONObject("overview_polyline");
                                    String polyline=poly.getString("points");
                                    polyLineList=decodePoly(polyline);
                                }
                                //Adjusting bounds
                                LatLngBounds.Builder builder=new LatLngBounds.Builder();
                                for (LatLng latLng:polyLineList){
                                    builder.include(latLng);
                                    LatLngBounds bounds=builder.build();
                                    CameraUpdate cameraUpdate=CameraUpdateFactory.newLatLngBounds(bounds,2);
                                    mMap.animateCamera(cameraUpdate);

                                    polylineOptions=new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);
                                    polylineOptions.width(5);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.endCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polyLineList);
                                    grayPolyLine=mMap.addPolyline(polylineOptions);

                                    blackPolyLineOptions =new PolylineOptions();
                                    blackPolyLineOptions.color(Color.BLACK);
                                    blackPolyLineOptions.width(5);
                                    blackPolyLineOptions.startCap(new SquareCap());
                                    blackPolyLineOptions.endCap(new SquareCap());
                                    blackPolyLineOptions.jointType(JointType.ROUND);
                                    blackPolyLineOptions.addAll(polyLineList);
                                    blackPolyLine=mMap.addPolyline(blackPolyLineOptions);

                                    mMap.addMarker(new MarkerOptions()
                                            .position(polyLineList.get(polyLineList.size()-1))
                                            .title("PickUp Location"));


                                    //Animations
                                    ValueAnimator polyLineAnimator=ValueAnimator.ofInt(0,1000);
                                    polyLineAnimator.setDuration(2000);
                                    polyLineAnimator.setInterpolator(new LinearInterpolator());
                                    polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            List<LatLng> points=grayPolyLine.getPoints();
                                            int percentValue=(int)animation.getAnimatedValue();
                                            int size=points.size();
                                            int newPoint=(int)(size*(percentValue/1000.0f));
                                            List<LatLng> p=points.subList(0,newPoint);
                                            blackPolyLine.setPoints(p);
                                        }
                                    });
                                    polyLineAnimator.start();

//                                    carMarker=mMap.addMarker(new MarkerOptions().position(currentPosition)
//                                    .flat(true)
//                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
//
//                                    handler=new Handler();
//                                    index=-1;
//                                    next=1;
//                                    handler.postDelayed(getDrawPathRunnable(),3000);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(MapsActivity.this, t.getMessage(),Toast.LENGTH_LONG).show();

                        }
                    });
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p=new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);

        }

        return poly;
    }
    private Runnable getDrawPathRunnable(){
        Runnable drawPathRunnable=new Runnable() {
            @Override
            public void run() {
                if (index<polyLineList.size()-1){
                    index++;
                    next=index+1;
                }
                if (index<polyLineList.size()-1){
                    startPosition=polyLineList.get(index);
                    endPosition=polyLineList.get(next);
                }
                final ValueAnimator valueAnimator=ValueAnimator.ofFloat(0,1);
                valueAnimator.setDuration(3000);
                valueAnimator.setInterpolator(new LinearInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        v=valueAnimator.getAnimatedFraction();
                        lng=v*endPosition.longitude+(1-v)*startPosition.longitude;
                        lat=v*endPosition.latitude+(1-v)*startPosition.latitude;
                        LatLng newPos=new LatLng(lat,lng);
                        carMarker.setPosition(newPos);
                        carMarker.setAnchor(0.05f,0.05f);
                        carMarker.setRotation(getBearing(startPosition,newPos));
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(newPos)
                                        .zoom(15.5f)
                                        .build()
                        ));
                    }
                });
                valueAnimator.start();
                handler.postDelayed(this,3000);

            }
        };
        return drawPathRunnable;
    }
    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat=Math.abs(startPosition.latitude-endPosition.latitude);
        double lng=Math.abs(startPosition.longitude-endPosition.longitude);

        if (startPosition.latitude<endPosition.latitude&&startPosition.longitude<endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat)));
        else if (startPosition.latitude>=endPosition.latitude&&startPosition.longitude<endPosition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+90);
        else if (startPosition.latitude>=endPosition.latitude&&startPosition.longitude>=endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat))+180);
        else if (startPosition.latitude<endPosition.latitude&&startPosition.longitude>=endPosition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        return -1;
    }

    private void stopLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                    &&ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                return;
            }
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
            geoFire.removeLocation(currentUserID, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if (error==null)
                        btnMapClear.hide();
                    else
                        Toast.makeText(MapsActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();

                }
            });


        }
    }
    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                &&ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            return;
        }
        Common.mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation!=null){
            if (location_switch.isChecked()){
                final double latitude=Common.mLastLocation.getLatitude();
                final double longitude=Common.mLastLocation.getLongitude();

                //updateToFireBase
                geoFire.setLocation(currentUserID, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        //Add Marker
                        if (mMap.isMyLocationEnabled()) mMap.setMyLocationEnabled(false);//Remove already marker

//                        marker=mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(latitude,longitude))
//                                .title("You"));
                        mMap.setMyLocationEnabled(true);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));
                        //Draw animation rotate marker
//                        rotateMarker(marker,-360,mMap);
                        Snackbar.make(mapFragment.getView(),"You are Online",Snackbar.LENGTH_SHORT).show();
                        pbMap.setVisibility(View.GONE);

                    }
                });
            }
        }

    }
    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                &&ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            //Request runtime permission
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            },My_PERMISSION_REQUEST_CODE);
        }else {
            if (checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked()){
                    displayLocation();
                }
            }
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
    private void rotateMarker(final Marker marker, final int i, GoogleMap mMap) {
        final Handler handler=new Handler();
        long start= SystemClock.uptimeMillis();
        final float startRotation=marker.getRotation();
        final long duration=1500;

        final Interpolator interpolator=new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed=SystemClock.uptimeMillis();
                float t=interpolator.getInterpolation((float)elapsed/duration);
                float rot=t*i*(1-t)*startRotation;
                marker.setRotation(-rot>180?rot/2:rot);
                if (t<1.0){
                    handler.postDelayed(this,16);
                }
            }
        });
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
    private void sendToLoginActivity() {
        Intent intent =new Intent(MapsActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            if (checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked()){
                    displayLocation();
                }
            }
        }
    }
    private void initializations() {
        location_switch=findViewById(R.id.location_switch);
        btnMapClear =findViewById(R.id.btnMapClear);
        pbMap=findViewById(R.id.pbMap);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.googleDirectionKey));
        }
        placeAutocompleteFragment= (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteFragment);
        placeAutocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.LAT_LNG ,Place.Field.NAME));
        rootRef=FirebaseDatabase.getInstance().getReference();
        driversAvailableRef = rootRef.child(Common.driversAvailable_tbl);
        mAuth= FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null)
            currentUserID=mAuth.getCurrentUser().getUid();
        geoFire=new GeoFire(driversAvailableRef);

        //Presense System
        onlineRef=rootRef.getRef().child(".info/connected");
        currentUserRef=FirebaseDatabase.getInstance().getReference(Common.driversAvailable_tbl).child(currentUserID);
        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //remove value from Driver tbl when driver disconnected
                currentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Change status of Driver",databaseError.getMessage());
            }
        });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);


    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }
    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,connectionResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation=location;
        displayLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Common.OnScreen=true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Common.OnScreen=false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Common.OnScreen=false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Common.OnScreen=true;
    }
}
