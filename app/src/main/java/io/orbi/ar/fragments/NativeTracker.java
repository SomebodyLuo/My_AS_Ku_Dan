package io.orbi.ar.fragments;

import android.graphics.Bitmap;

/**
 * Created by pc on 2018/1/11.
 */

public class NativeTracker {

    static {
        System.loadLibrary("native-lib");
    }

    //--------------------------
    //Native Methods
    //--------------------------
    public static native void initialiseImageTracker(String key, int width, int height);
    public static native void initialiseArbiTracker(String key, int width, int height);
    public static native void startArbiTracker(boolean startFromImageTrackable);
    public static native void stopArbiTracker();

    public static native boolean addTrackableToImageTracker(
            Bitmap image,
            String name);

    public static native float[] processImageTrackerFrame(
            byte[] image,
            int width,
            int height,
            int channels,
            int padding,
            boolean requiresFlip);

    public static native float[] processArbiTrackerFrame(
            byte[] image,
            float[] gyroOrientation,
            int width,
            int height,
            int channels,
            int padding,
            boolean requiresFlip);

    enum TrackerState {
        IMAGE_DETECTION,
        IMAGE_TRACKING,
        ARBITRACK
    }
}
