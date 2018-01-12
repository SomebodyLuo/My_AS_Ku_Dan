package io.orbi.ar.fragments;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;

import io.orbi.ar.detection.LocationDetection;
import io.orbi.ar.orientationProvider.ImprovedOrientationSensor2Provider;
import io.orbi.ar.render.RendererJPCT;

/**
 * Created by pc on 2018/1/11.
 */

public class SensorWork {

    private Context mContext = null;
    private SensorManager mSensorManager;

    ImprovedOrientationSensor2Provider orientationProvider;

    public SensorWork(Context context)
    {
        mContext = context;
    }

    //-----------------------------------------
    //Setup the rotation sensor for receiving
    //data on the device orientation status.
    //----------------------------------------
    public void setupRotationSensor(RendererJPCT render)
    {
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        orientationProvider = new ImprovedOrientationSensor2Provider(mSensorManager);
        render.setOrientationProvider(orientationProvider);
        orientationProvider.start();

    }


    //Stops the rotation sensor.
    public void teardownRotationSensor()
    {
        orientationProvider.stop();
    }
}
