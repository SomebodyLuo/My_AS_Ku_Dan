package io.orbi.ar.detection;

import android.util.Log;

import java.util.GregorianCalendar;

import io.orbi.ar.calc.AzimuthZenithAngle;
import io.orbi.ar.calc.Grena3;


/**
 * Created by ibii on 7/19/17.
 */

public class EnviroDetection
{
    public AzimuthZenithAngle sunPosition = null;

    public EnviroDetection(double lat, double lon)
    {
        GregorianCalendar cal = new GregorianCalendar();
        sunPosition =  Grena3.calculateSolarPosition(cal,lat,lon,70,1000,20);
        Log.e("@@APP", "Found Sun position  as " + sunPosition.getAzimuth() + ", " + sunPosition.getZenithAngle());

    }

    public double getAzimuth()
    {
        return sunPosition.getAzimuth();
    }
    public double getZenithAngle()
    {

        double zenith = sunPosition.getZenithAngle();
        //change angles into degrees
        zenith = 90.0f -zenith;

        return zenith;
    }
}
