package com.codeshot.cars.Common;


import android.location.Location;

import com.codeshot.cars.Models.User;
import com.codeshot.cars.Remote.FCMClient;
import com.codeshot.cars.Remote.IFCMService;
import com.codeshot.cars.Remote.IGoogleAPI;
import com.codeshot.cars.Remote.RetrofitClient;


public class Common {

    private static final String baseURL="https://maps.googleapis.com";
    private static final String fcmURL="https://fcm.googleapis.com/";
    public static Location mLastLocation=null;

    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

    public static final String driversAvailable_tbl="DriversAvailable";
    public static final String drivers_tbl="Drivers";
    public static final String riders_tbl="Riders";
    public static final String pickUpRequest_tbl="PickUpRequests";
    public static final String token_tbl="Tokens";
    public static User currentUser;
    //Price Values
    public static double base_fare=2.55;
    public static double time_rate=0.35;
    public static double distance_rate=1.75;
    public static double carsFee=0.0;
    public static double othersFee=0.0;

    public static double getPrice(double km,int min){
        return (base_fare+(time_rate*min)+(distance_rate*km)-carsFee+othersFee);
    }



    public static IFCMService getFCMService() {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }

    public static Boolean OnScreen=false;
}
