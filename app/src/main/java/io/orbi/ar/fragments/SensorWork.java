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

public class SensorWork /*implements SensorEventListener*/ {

    private Context mContext = null;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Handler myHandler = null;

    private final float[] mRotationQuaternion = new float[4];

    public SensorWork(Context context, Handler handler)
    {
        mContext = context;
        myHandler = handler;
    }

    //-----------------------------------------
    //Setup the rotation sensor for receiving
    //data on the device orientation status.
    //----------------------------------------
    public void setupRotationSensor(RendererJPCT render)
    {
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        ImprovedOrientationSensor2Provider orientationProvider = new ImprovedOrientationSensor2Provider(mSensorManager);
        render.setOrientationProvider(orientationProvider);
        orientationProvider.start();

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mSensor, 30000);
    }
    //-----------------------------
    //Sensor events
    //----------------------------
//    @Override
//    public void onSensorChanged(SensorEvent event)
//    {
//
//        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
//        {
//            // Get the current device rotation.
//            float temp1[] = new float[16];
//            float temp2[] = new float[16];
//
//            SensorManager.getRotationMatrixFromVector(temp1, event.values);
//
//            // Remap the device rotation to the arbitracker coordinate system.
//            SensorManager.remapCoordinateSystem(temp1, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_MINUS_X, temp2);
//
//            // Convert the rotation matrix into a quaternion.
//            double w = Math.sqrt(1.0 + temp2[0] + temp2[5] + temp2[10]) / 2.0;
//            double w4 = (4.0 * w);
//            double x = (temp2[9] - temp2[6]) / w4;
//            double y = (temp2[2] - temp2[8]) / w4;
//            double z = (temp2[4] - temp2[1]) / w4;
//
//            mRotationQuaternion[0] = (float) w;
//            mRotationQuaternion[1] = (float) x;
//            mRotationQuaternion[2] = (float) y;
//            mRotationQuaternion[3] = (float) z;
//
//            Message msg = Message.obtain();
//            msg.what = 10;
//            msg.obj = mRotationQuaternion;
//            myHandler.sendMessage(msg);
//        }
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//    }

    //Stops the rotation sensor.
    public void teardownRotationSensor()
    {
        mSensorManager.unregisterListener(this);
        mSensorManager = null;
        mSensor = null;
    }
}
