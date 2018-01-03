package io.orbi.ar.detection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by ibii on 7/18/17.
 */

public class LocationDetection implements LocationListener {

    private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private Criteria criteria;
    private String provider;
    private Context ctx;
    public static String loci = "";

    public LocationDetection(Context context, Activity atx)
    {
        this.ctx = context;

        Log.d("@@APP", "LOCATION");
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(atx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(atx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("Location permissions must be granted to function.");
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, this);
        setMostRecentLocation(locationManager.getLastKnownLocation(provider));

        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null)
        {

            Log.i("@@APP", "Location achieved!");
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            loci = "深圳";

        }
        else
        {
            //default to setting in camerafragment
            latitude = 0;
            longitude = 0;
            loci = "深圳";

            Log.i("@@APP", "No location :(");

        }


    }

    private void setMostRecentLocation(Location lastKnownLocation) {

    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getGeo()
    {
        String loc = "";
        Geocoder gcd = new Geocoder(ctx, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(getLatitude(), getLongitude(), 1);
            if(addresses.size()>0)

            {
                loc = addresses.get(0).getLocality();
            }
            else

            {
                loc = "";
                // do your staff
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return loc;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * android.location.LocationListener#onLocationChanged(android.location.
     * Location)
     */
    @Override
    public void onLocationChanged(Location location)
    {
        double lon = (double) (location.getLongitude());/// * 1E6);
        double lat = (double) (location.getLatitude());// * 1E6);

//      int lontitue = (int) lon;
//      int latitute = (int) lat;
        latitude = lat;
        longitude = lon;


    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.location.LocationListener#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.location.LocationListener#onProviderEnabled(java.lang.String)
     */
    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see android.location.LocationListener#onStatusChanged(java.lang.String,
     * int, android.os.Bundle)
     */
    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub

    }

}
