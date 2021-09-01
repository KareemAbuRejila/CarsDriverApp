package com.codeshot.cars;

import androidx.fragment.app.FragmentActivity;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.codeshot.cars.Common.Common;
import com.codeshot.cars.Remote.IGoogleAPI;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripDetails extends FragmentActivity implements OnMapReadyCallback {

    //View
    private TextView tvDistance,tvTime,tvTotalPrice,tvBaseFare,tvDate,tvStartLocation,tvEndLocation;

    private GoogleMap mMap;
    private IGoogleAPI mServices;
    private List<LatLng> polyLineList;
    private PolylineOptions polylineOptions, blackPolyLineOptions;
    private Polyline blackPolyLine,grayPolyLine;

    private Location pickupLocation,mLastLocation;
    private String startLocation,endLocation;
    private String time,distance;
    private double totalPrice;

    private double riderLat,riderLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.tripDetailMap);
        mapFragment.getMapAsync(this);
        mServices=Common.getGoogleAPI();

        tvDistance=findViewById(R.id.tvDistance);
        tvTime=findViewById(R.id.tvTime);
        tvTotalPrice=findViewById(R.id.tvTotalPrice);
        tvBaseFare=findViewById(R.id.tvBaseFare);
        tvDate=findViewById(R.id.tvDate_Time);
        tvStartLocation=findViewById(R.id.tvStartLocation);
        tvEndLocation=findViewById(R.id.tvEndLocation);






    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        settingInformaton();
        getDirections(pickupLocation,mLastLocation);
        mMap.getUiSettings().setAllGesturesEnabled(false);

    }

    private void settingInformaton() {
        if (getIntent()!=null)
        {
            //SetText
            Calendar calendar=Calendar.getInstance();
            String date=String.format("%s, %d/%d",convertToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)),calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.MONTH));
            pickupLocation=getIntent().getParcelableExtra("pickupLocation");
            mLastLocation=getIntent().getParcelableExtra("mLastLocation");
            time=getIntent().getStringExtra("time");
            distance=getIntent().getStringExtra("distance");
            totalPrice=getIntent().getIntExtra("totalPrice",0);

            riderLat=getIntent().getDoubleExtra("riderLat",-1.0);
            riderLng=getIntent().getDoubleExtra("riderLng",-1.0);

            startLocation=getIntent().getStringExtra("startLocation");
            endLocation=getIntent().getStringExtra("endLocation");

            tvDate.setText(date);
            tvTime.setText(time+" Min");
            tvDistance.setText(distance+" KM");
            tvTotalPrice.setText("$"+String.valueOf(totalPrice));
            tvBaseFare.setText("$ "+String.valueOf(Common.base_fare));
            tvStartLocation.setText(startLocation);
            tvEndLocation.setText(endLocation);

        }
    }

    private Object convertToDayOfWeek(int day) {
        switch (day)
        {
            case Calendar.SUNDAY:
                return "SUNDAY";
            case Calendar.MONDAY:
                return "MONDAY";
            case Calendar.THURSDAY:
                return "THURSDAY";
            case Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case Calendar.TUESDAY:
                return "TUESDAY";
            case Calendar.FRIDAY:
                return "FRIDAY";
            case Calendar.SATURDAY:
                return "SATURDAY";
            default:
                return "Not Founded";
        }
    }

    private void getDirections(final Location pickupLocation, Location mLastLocation) {

        String requestAPI=null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin="+pickupLocation.getLatitude()+","+pickupLocation.getLongitude()+"&" +
                    "destination="+mLastLocation.getLatitude()+","+mLastLocation.getLongitude()+"&" +
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
                                    polylineOptions.color(Color.MAGENTA);
                                    polylineOptions.width(5);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.endCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polyLineList);
                                    grayPolyLine=mMap.addPolyline(polylineOptions);


                                    mMap.addMarker(new MarkerOptions()
                                            .position(polyLineList.get(polyLineList.size()-1))
                                            .title("End Trip"));
                                    mMap.addMarker(new MarkerOptions()
                                                    .title("Start Trip")
                                            .position(new LatLng(pickupLocation.getLatitude(),pickupLocation.getLongitude())));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(pickupLocation.getLatitude(),pickupLocation.getLongitude()),12.0f));

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(TripDetails.this, t.getMessage(),Toast.LENGTH_LONG).show();

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

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
