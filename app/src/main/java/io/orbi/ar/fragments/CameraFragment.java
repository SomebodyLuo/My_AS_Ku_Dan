package io.orbi.ar.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


import io.orbi.ar.CameraSurfaceView;

import io.orbi.ar.R;
import io.orbi.ar.detection.EnviroDetection;
import io.orbi.ar.detection.LocationDetection;
import io.orbi.ar.interfaces.OnModelChangeListener;
import io.orbi.ar.interfaces.OnOrbiCompleteListener;
import io.orbi.ar.orientationProvider.ImprovedOrientationSensor2Provider;
import io.orbi.ar.render.RendererJPCT;
import io.orbi.ar.utils.BasicTimer;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;


public class CameraFragment extends Fragment implements SensorEventListener
{
    private final String TAG = "luoyouren";

    public OnOrbiCompleteListener mListener;//call back to main activity
    public OnModelChangeListener modListener;//when model asset needs changing

    static {
        System.loadLibrary("native-lib");
    }

    private final float[] mRotationQuaternion = new float[4];
    public RendererJPCT render;

    RectF mSrcRect = new RectF();
    RectF mDstRect = new RectF();
    Matrix mCanvasTransform = new Matrix();

    TrackerState mTrackerState = TrackerState.IMAGE_DETECTION;

    private GLSurfaceView glSurfaceView;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private Size mCameraPreviewSize = new Size(1280, 720);//1920, 1080
    //(1280, 720);

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraSurfaceView mSurfaceView;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private TextView mStatusLabel;

    private ArrayList<Point> trackedCorners = new ArrayList<>(4);
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private TrackerState state;

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback()
    {

        @Override
        public void surfaceCreated(SurfaceHolder holder)
        {

            // Prevent camera device setup if a background thread is not available.
            if (mBackgroundHandler == null) {
                return;
            }

            // Setup the camera only when the Surface has been created to ensure a valid output
            // surface exists when the CameraCaptureSession is created.
            setupCameraDevice();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    // luoyouren: 重要！
    private ImageReader.OnImageAvailableListener mImageAvailListener = new ImageReader.OnImageAvailableListener() {


        Bitmap cameraFrame = Bitmap.createBitmap(mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight(), Bitmap.Config.ALPHA_8);
        Rect cameraFrameRect = new Rect();
        byte[] cameraFrameData = new byte[mCameraPreviewSize.getWidth() * mCameraPreviewSize.getHeight()];

        int[] argb8888 = new int[mCameraPreviewSize.getWidth() * mCameraPreviewSize.getHeight()];
        Bitmap colourFrame = Bitmap.createBitmap(mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight(), Bitmap.Config.ARGB_8888);

        @Override
        public void onImageAvailable(ImageReader reader) {

            // Synchronize with the tracker state to prevent changes to state mid-processing.
            if(reader != null)
            {

                synchronized (mTrackerState)
                {

                    Image currentCameraImage = reader.acquireLatestImage();

                    // Return if no new camera image is available.
                    if (currentCameraImage == null) {
                        return;
                    }

                    int width = currentCameraImage.getWidth();
                    int height = currentCameraImage.getHeight();

                    // luoyouren: just Y channel
                    // Get the buffer holding the luma data from the YUV-format image.
                    ByteBuffer buffer = currentCameraImage.getPlanes()[0].getBuffer();

                    // Push the luma data into a byte array.
                    buffer.get(cameraFrameData);

                    // Update the cameraFrame bitmap with the new image data.
                    buffer.rewind();
                    cameraFrame.copyPixelsFromBuffer(buffer);

                    // Process tracking based on the new camera frame data.
                    mTrackerState = processTracking(cameraFrameData, width, height, mTrackerState, trackedCorners);
                    // Render the new frame and tracking results to screen.
                    //------------------------------------------------------------------------------
                    //oli for PFL addition

                    buffer.rewind();

                    //first step get the YUV-format data
                    Image.Plane Y = currentCameraImage.getPlanes()[0];
                    Image.Plane U = currentCameraImage.getPlanes()[1];
                    Image.Plane V = currentCameraImage.getPlanes()[2];

                    int Yb = Y.getBuffer().remaining();
                    int Ub = U.getBuffer().remaining();
                    int Vb = V.getBuffer().remaining();

                    byte[] data = new byte[Yb + Ub + Vb + 2];

                    Y.getBuffer().get(data, 0, Yb);
                    U.getBuffer().get(data, Yb + 1, Ub);
                    V.getBuffer().get(data, Yb + Ub + 2, Vb);

                    //  now we can convert data to rgb

                    //data should store all the info we need


                    //we can do the next stage in two ways
                    //method one
                    // working but a bit slow?
            /*    if (rs==null) rs = RenderScript.create(getContext());
                Allocation alloc = renderScriptNV21ToRGBA888(rs,width,height,data);
                alloc.copyTo(colourFrame);
          */

                    //method 2
                    decodeYUV(argb8888, data, width, height);
                    colourFrame.setPixels(argb8888, 0, width, 0, 0, width, height);

                    //----------------------------------
                    //Render Camera Frame to Screen
                    //----------------------------------
                    renderFrameToScreen(colourFrame, cameraFrameRect, mTrackerState, trackedCorners);

                    //------------------------------------------------------------------------------
                    //end oli for PFL addition
                    // Clean up frame data.
                    buffer.clear();
                    currentCameraImage.close();
                }
            }
        }
    };
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            Log.i("CameraDevice", "CameraDevice Opened.");

            mCameraDevice = cameraDevice;

            // Get the KudanCV API key from the Android Manifest.
            String apiKey = getAPIKey();

            // Initialise the native tracking objects.
            initialiseImageTracker(apiKey, mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight());
            initialiseArbiTracker(apiKey, mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight());

            //-------------------------------------------------------
            // Add the image trackable to the native image tracker.
            //-------------------------------------------------------
//            addTrackable(R.mipmap.ani1, "ANI1");
//            addTrackable(R.mipmap.ani2, "ANI2");
//            addTrackable(R.mipmap.kien, "Kien");
            addTrackable(R.mipmap.sample, "sample");
//            addTrackable(R.mipmap.desktop, "desktop");
//            addTrackable(R.mipmap.roof, "roof");
            addTrackable(R.mipmap.roof2, "roof2");
            addTrackable(R.mipmap.roof3, "roof3");
            addTrackable(R.mipmap.roof4, "roof4");
            //-------


            // Create the camera preview.
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            Log.i("CameraDevice", "CameraDevice Disconnected.");

            // Release the Semaphore to allow the CameraDevice to be closed.
            mCameraOpenCloseLock.release();

            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {

            Log.e("CameraDevice", "CameraDevice Error.");

            // Release the Semaphore to allow the CameraDevice to be closed.
            mCameraOpenCloseLock.release();

            cameraDevice.close();
            mCameraDevice = null;

            // Stop the activity.
            Activity activity = getActivity();

            if (null != activity) {
                activity.finish();
            }
        }
    };

    private RenderScript rs = null;

    //------
    //UI and States
    //------
    private Switch debug_switch;
    private Switch lss_switch;
    private Switch preset_switch;
    private boolean debug = false;
    private boolean lss = false;
    private boolean angle_preset = false;
    private double preset_azimuth = 90;
    private double preset_zenith = 90;

    private View vw;
    //--------------------
    //location and enviro
    //---------------------
    private double lat = 0;
    private double lng = 0;
    private EnviroDetection enviro = null;
    private LocationDetection appLocationManager = null;
    private double fixedAzimuth = 0;
    private double fixedZenith = 90;
    private TextView output;

    // 显示时间
    private TextView info;

    //-----------
    //Chameleon
    //----------
    private int lastImageR=0;
    private int lastImageG=0;
    private int lastImageB=0;

    private int light1;
    private int light2;

    //-------------
    //Target Image
    //--------------
    Bitmap target_image = null;

    //----------
    //Buttons
    //----------
    ImageView target_bttn;

    //-----
    //Pinch
    //------
    private float mScaleFactor = 1.0f;
    private ScaleGestureDetector mScaleDetector;
    private boolean pinching = false;

    //----------------------
    //Present storage
    //---------------------
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    //------
    //Pause all render updates
    //---------------
    private boolean frozen = false;

    //--------------
    //Point smoothing
    //-----------------
    private Point oldPoint = null;

    //----------
    //Model index
    //--------------
    private int model_index = 0;


    //-----------------------------
    //Camera Fragment SUPER
    //-----------------------------
    public CameraFragment()
    {

        super();

        // Pre-allocate point objects to store tracked corner data.
        for (int i = 0; i < 4; i++)
        {
            trackedCorners.add(new Point());
        }
    }
    //-----------------------------
    //Camera Fragment Instance
    //-----------------------------
    public static CameraFragment newInstance()
    {
        return new CameraFragment();
    }

    //------------------------------
    //Lifecycle Methods
    //------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        this.model_index = getArguments().getInt("model_index");//the model index

        Log.i("@@INDEX",":"+model_index);
        return inflater.inflate(R.layout.camera_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)
    {
        //camera
        mSurfaceView = (CameraSurfaceView) view.findViewById(R.id.surface_view);
        mSurfaceView.setAspectRatio(mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight());

        mStatusLabel = (TextView) view.findViewById(R.id.status_label);
        mStatusLabel.setText("Setting Up.");

        //-----------------
        //Preset storage
        //------------------
        settings = getActivity().getSharedPreferences("ORBI_DB", MODE_PRIVATE);


        //--------
        //pinch
        //--------
        mScaleDetector = new ScaleGestureDetector(getActivity().getApplicationContext(), new ScaleListener());

        //---------
        //Text HUD
        //----------
        output = (TextView) view.findViewById(R.id.output);

        //---------
        //Text HUD
        //----------
        info = (TextView) view.findViewById(R.id.info);

        //------------------------------
        //Tracking target image
        //--------------------------------
        //target_image = BitmapFactory.decodeResource(getResources(), R.drawable.target);

        //-------------
        //Buttons
        //-------------
        target_bttn = (ImageView) view.findViewById(R.id.target_bttn);
        target_bttn.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    if(render != null)
                    {
                        render.stick2screen(-600, -600);//default it off screen in between states
                        glSurfaceView.requestRender();//跳真
                        lss_switch.setChecked(false);//turn off manual light as need touch move events for model.
                    }

                }
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {

                        // Synchronize with the tracker state to prevent changes to state mid-processing.
                        synchronized (mTrackerState)
                        {

                            if (mTrackerState == TrackerState.IMAGE_DETECTION)
                            {

                                startArbiTracker(false);

                                mTrackerState = TrackerState.ARBITRACK;
                                target_bttn.setImageResource(R.drawable.camera_icon);



                            }
                            else if (mTrackerState == TrackerState.IMAGE_TRACKING)
                            {

                                startArbiTracker(true);

                                mTrackerState = TrackerState.ARBITRACK;
                                target_bttn.setImageResource(R.drawable.camera_icon);


                                if (render != null)
                                {
                                    int xp = v.getWidth() / 2;
                                    int yp = v.getHeight() / 2;
                                    //render.updatePosition(new Point(xp, yp), (float) enviro.getAzimuth(), (float) enviro.getZenithAngle(), 5f, mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight());
                                }


                            }
                            else if (mTrackerState == TrackerState.ARBITRACK)
                            {

                                stopArbiTracker();

                                mTrackerState = TrackerState.IMAGE_DETECTION;
                                target_bttn.setImageResource(R.drawable.target);



                            }
                        }

                }

                return false;
            }
        });


        //----------------
        //Debug switch
        //----------------
        debug_switch = (Switch) view.findViewById(R.id.debug);
        debug_switch.setChecked(false);
        debug_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {

                if(isChecked)
                {

                    debug = true;
                    sendToast("Debug On");


                }
                else
                {

                    debug = false;
                    sendToast("Debug Off");
                }

            }
        });
        //---------------------------
        //Light Source Setting LSS
        //---------------------------
        lss_switch = (Switch) view.findViewById(R.id.fixed);
        lss_switch.setChecked(false);
        lss_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {

                if(isChecked)
                {
                    lss = true;
                    sendToast("LSS On");
                }
                else
                {
                    lss = false;
                    sendToast("LSS Off");

                    //light Detection from location
                    getlocLight();
                }

            }
        });
        //----------------
        //preset_switch
        //----------------
        preset_switch = (Switch) view.findViewById(R.id.preset);
        preset_switch.setChecked(false);
        preset_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {

                if(isChecked)
                {
                    preset_azimuth = Double.longBitsToDouble(settings.getLong("pre_azi", Double.doubleToLongBits(0)));
                    preset_zenith = Double.longBitsToDouble(settings.getLong("pre_zen", Double.doubleToLongBits(90)));
                    angle_preset = true;
                    sendToast("Preset On");
                }
                else
                {
                    angle_preset = false;
                    sendToast("Preset Off");
                }

            }
        });

        //-----------------
        //Models
        //-----------------
        Spinner spinner = (Spinner) view.findViewById(R.id.model_spinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(getActivity(), R.array.model_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String item = parent.getItemAtPosition(position).toString();

                //swap models
                if(render != null && !item.equals("DEFAULT"))
                {
                    Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();

                    int w_model = 1;

                    if(item.equals("PLANT"))
                    {
                        w_model = 1;
                    }
                    if(item.equals("BALL"))
                    {
                        w_model = 2;
                        render.replaceModel(loadObj());
                    }
                    if(item.equals("ROBOT"))
                    {
                        w_model = 3;
                    }

                    //mImageReader = null;
                    //render = null;

                    //Send back to main activity to remove fragment and recreate with diff model load
//                    modListener.onModelChanged(w_model);

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });


        //-----------------------
        //Screen touch events
        //----------------------
        view.setOnTouchListener(new View.OnTouchListener()
        {

            public boolean onTouch(View v, MotionEvent event)
            {
                //pinch
                mScaleDetector.onTouchEvent(event);

                //-----------
                if(event.getAction() == MotionEvent.ACTION_MOVE)
                {

                    if(!lss && render != null && !pinching)
                    {
                        float xp = event.getX() / v.getWidth();
                        float yp = event.getY() / v.getHeight();

                        render.stick2plane(xp, 1 - yp);//quick fix on y to stop reversal

                    }
                    else if (lss && render != null && !pinching)
                    {
                        float x = event.getX();
                        float y = event.getY();

                        float view_width = view.getWidth();
                        float azi_increments = 360 / view_width;
                        float azi_count = x * azi_increments;

                        fixedAzimuth = azi_count;

                        float view_height = view.getHeight();
                        float zenith_increments = 180 / view_height;
                        float zenith_count = y * zenith_increments;

                        fixedZenith = zenith_count;

                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                output.setText("AZI: "+fixedAzimuth+" | ZEN: "+fixedZenith +" | "+LocationDetection.loci);
                            }
                        });

                    }

                }
                //-------------------------------
                //If preset save to database
                //-------------------------------
                if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    if(angle_preset && lss && !pinching)
                    {
                        editor = settings.edit();
                        editor.putLong("pre_azi", Double.doubleToRawLongBits(fixedAzimuth));
                        editor.putLong("pre_zen", Double.doubleToRawLongBits(fixedZenith));
                        editor.commit();

                        Log.i("@@PRESET",""+fixedAzimuth+":"+fixedZenith);
                    }
                }

                return true;
            }
        });

        setScene(view);
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 5.0f));
            Log.i("@@SCALE",":"+mScaleFactor);


            if(!pinching)
            {
                pinching = true;
                sendToast("Scale Setting");

                BasicTimer.TaskHandle handle = BasicTimer.setTimeout(new Runnable() {
                    public void run()
                    {
                        Log.i("@@TIMER", "Executed after 3000 ms!");
                        pinching = false;
                        sendToast("Scale Set");
                    }
                }, 2500);
            }


            //invalidate();
            return true;
        }
    }

    //----------------------------------
    //Init render and gl
    //----------------------------------
    private void setScene(View v)
    {
        //Get the light angle
        getlocLight();

        if(render != null)
        {
            render = null;
        }
        if(glSurfaceView !=  null)
        {
            glSurfaceView = null;
        }

        //create the render
        render = new RendererJPCT(getActivity().getApplicationContext());

        render.setSunMode(false);//makes sure sun is on corrected side for enviro

        //Real Sun Projection
        light1 = render.addSun(); // you can add 8 lights at most, return -1 if error
        //Chameleon color
        light2 = render.addSun(); // you can add 8 lights at most, return -1 if error

        //which model to load and settings
        if(model_index == 0 || model_index == 1)
        {
            Log.i("@@INDEX",":Plant");
            sendToast("Plant Loaded");
            render.init(loadIronman());
            render.setLightColor(light1, 255, 255, 255);
            render.setSunLum(210);//set ambient init light
        }
        else if(model_index == 2)
        {
            Log.i("@@INDEX",":Ball");
            sendToast("Ball Loaded");
            render.init(loadPlant01());
            render.setLightColor(light1, 60, 60, 60);
            render.setSunLum(180);//set ambient init light
        }

        //sendToast("Model loaded");
        //gl surface view
        glSurfaceView = (GLSurfaceView) v.findViewById(R.id.glsurfaceview);

        // luoyouren: 设置透明
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setRenderer(render);
        glSurfaceView.setRenderMode(RENDERMODE_WHEN_DIRTY);

        //------------------------
        //Start the Render
        //------------------------
        if (glSurfaceView != null)
        {
            glSurfaceView.requestRender();
        }

        //send back to main activity fragment is loaded and setup
        mListener.onComplete();

    }
    //--------------------
    //light Detection from location
    //------------------
    private void getlocLight()
    {
        //setup location provider
        appLocationManager = new LocationDetection(this.getActivity().getApplicationContext(),this.getActivity());
        lat = appLocationManager.getLatitude();
        lng = appLocationManager.getLongitude();

        //default to shenzhen
        if(lat == 0 && lng == 0)
        {
            lat = 33.5946571;
            lng = 130.359499;

            //japan
            //latitude = 33.5946571;
            //longitude = 130.359499;
        }

        Log.i("@@LOC"," "+lat+":"+lng);

        enviro = new EnviroDetection(lat,lng);


    }

    @Override
    public void onPause()
    {

        teardownCamera();
        teardownBackgroundThread();
        teardownRotationSensor();

        super.onPause();
    }

    @Override
    public void onResume()
    {

        super.onResume();

        setupBackgroundThread();
        setupRotationSensor();

        if (mSurfaceView.getHolder().getSurface().isValid())
        {
            setupCameraDevice();
        }
        else
        {
            mSurfaceView.getHolder().addCallback(mSurfaceCallback);
        }
    }

    //-------------------------
    //API Key
    //-------------------------
    private String getAPIKey()
    {

        String appPackageName = getActivity().getPackageName();

        try
        {
            ApplicationInfo app = getActivity().getPackageManager().getApplicationInfo(appPackageName, PackageManager.GET_META_DATA);

            Bundle bundle = app.metaData;

            String apiKeyID = "eu.kudan.ar.API_KEY";

            if (bundle == null) {
                throw new RuntimeException("No manifest meta-data tags exist.\n\nMake sure the AndroidManifest.xml file contains a <meta-data\n\tandroid:name=\"" + apiKeyID + "\"\n\tandroid:value=\"${YOUR_API_KEY}\"></meta-data>\n");
            }

            String apiKey = bundle.getString(apiKeyID);

            if (apiKey == null) {
                throw new RuntimeException("Could not get API Key from Android Manifest meta-data.\n\nMake sure the AndroidManifest.xml file contains a <meta-data\n\tandroid:name=\"" + apiKeyID + "\"\n\tandroid:value=\"${YOUR_API_KEY}\"></meta-data>\n");
            }

            if (apiKey.isEmpty()) {
                throw new RuntimeException("Your API Key from Android Manifest meta-data appears to be empty.\n\nMake sure the AndroidManifest.xml file contains a <meta-data\n\tandroid:name=\"" + apiKeyID + "\"\n\tandroid:value=\"${YOUR_API_KEY}\"></meta-data>\n");
            }

            return apiKey;

        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Cannot find Package with name \"" + appPackageName + "\". Cannot load API key.");
        }
    }

    //endregion

    //region Setup and Teardown Methods

    //--------------------------------
    //Sets up a new background thread
    // and it's Handler.
    //--------------------------------
    private void setupBackgroundThread()
    {

        mBackgroundThread = new HandlerThread("BackgroundCameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    //--------------------------------------------------
    //Sets up a new CameraDevice check camera permissions
    //----------------------------------------------------
    private void setupCameraDevice()
    {

        // Check for camera permissions.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            throw new RuntimeException("Camera permissions must be granted to function.");
        }

        //setup camera manager
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);


        try {
            String[] cameras = manager.getCameraIdList();

            // Find back-facing camera.
            for (String camera : cameras) {

                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(camera);

                // Reject all cameras but the back-facing camera.
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != LENS_FACING_BACK) {
                    continue;
                }

                try {
                    if (!mCameraOpenCloseLock.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException(("Camera lock cannot be acquired during opening."));
                    }

                    // Open camera. Events are sent to the mStateCallback listener and handled on the background thread.
                    manager.openCamera(camera, mStateCallback, mBackgroundHandler);

                    // Open one camera only.
                    return;

                } catch (InterruptedException e) {
                    throw new RuntimeException("Camera open/close semaphore cannot be acquired");
                }
            }

        } catch (CameraAccessException e) {
            throw new RuntimeException("Cannot access camera.");
        }
    }

    //--------------------------------------------------------------
    // Creates a new CameraCaptureSession for the camera preview.
    //-------------------------------------------------------------
    private void createCameraPreviewSession()
    {

        try
        {

            // Create an ImageReader instance that buffers two camera images so there is always room for most recent camera frame.
            // mImageReader = ImageReader.newInstance(mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            mImageReader = ImageReader.newInstance(mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            // Handle all new camera frames received on the background thread.
            // mImageAvailListener非常关键！
            mImageReader.setOnImageAvailableListener(mImageAvailListener, mBackgroundHandler);

            // Set up a CaptureRequest.Builder with the output Surface of the ImageReader.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // 将mImageReader加入到Preview Request队列中
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // Create the camera preview CameraCaptureSession.
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            // The camera is already closed
                            if (mCameraDevice == null)
                            {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;

                            try
                            {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);//CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE

                                // Finally, start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);

                                // Release the Semaphore to allow the CameraDevice to be closed.
                                mCameraOpenCloseLock.release();

                            } catch (CameraAccessException e) {
                                throw new RuntimeException("Cannot access camera during CameraCaptureSession setup.");
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            throw new RuntimeException("Camera capture session configuration failed.");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            throw new RuntimeException("Cannot access camera during CameraCaptureSession setup.");
        }
    }

    //-------------------------------------------------------
    //Tears down and closes the camera device and session.
    //------------------------------------------------------
    private void teardownCamera() {

        try {
            // Prevent the teardown from occuring at the same time as setup.
            mCameraOpenCloseLock.acquire();

            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    //---------------------------------------
    // Stops the background thread and handler.
    //-----------------------------------------
    private void teardownBackgroundThread() {

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //-----------------------------------------
    //Setup the rotation sensor for receiving
    //data on the device orientation status.
    //----------------------------------------
    private void setupRotationSensor()
    {
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        ImprovedOrientationSensor2Provider orientationProvider = new ImprovedOrientationSensor2Provider(mSensorManager);
        render.setOrientationProvider(orientationProvider);
        orientationProvider.start();

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mSensor, 30000);
    }
    //-----------------------------
    //Sensor events
    //----------------------------
    @Override
    public void onSensorChanged(SensorEvent event)
    {

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (enviro != null)
                    {
                        int gazi = (int) Math.round(enviro.getAzimuth());
                        int zazi = (int) Math.round(enviro.getZenithAngle());

                        output.setText("AZI: " + gazi + " | ZEN: " + zazi + " | " + LocationDetection.loci);
                    }
                }
            });


            // Get the current device rotation.
            float temp1[] = new float[16];
            float temp2[] = new float[16];

            SensorManager.getRotationMatrixFromVector(temp1, event.values);

            // Remap the device rotation to the arbitracker coordinate system.
            SensorManager.remapCoordinateSystem(temp1, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_MINUS_X, temp2);

            // Convert the rotation matrix into a quaternion.
            double w = Math.sqrt(1.0 + temp2[0] + temp2[5] + temp2[10]) / 2.0;
            double w4 = (4.0 * w);
            double x = (temp2[9] - temp2[6]) / w4;
            double y = (temp2[2] - temp2[8]) / w4;
            double z = (temp2[4] - temp2[1]) / w4;

            mRotationQuaternion[0] = (float) w;
            mRotationQuaternion[1] = (float) x;
            mRotationQuaternion[2] = (float) y;
            mRotationQuaternion[3] = (float) z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

     //Stops the rotation sensor.
    private void teardownRotationSensor()
    {
        mSensorManager.unregisterListener(this);
        mSensorManager = null;
        mSensor = null;
    }

    //---------------------------------------------
    //Processes tracking on a camera frame's data
    //---------------------------------------------
    private TrackerState processTracking(byte[] data, int width, int height, TrackerState currentState, ArrayList<Point> projectedTrackingCorners) {

        float[] trackedData = null;
        TrackerState newState = currentState;

        // Perform image detection and tracking.
        if (currentState != TrackerState.ARBITRACK)
        {
            oldSize=0;
            // Native call to the image tracking and detection object.
            trackedData = processImageTrackerFrame(data, width, height, 1, 0, false);

            if (trackedData != null)
            {
                newState = TrackerState.IMAGE_TRACKING;
            }
            else
            {
                newState = TrackerState.IMAGE_DETECTION;

            }
        }

        //perform markerless tracking.
        else if (currentState == TrackerState.ARBITRACK)
        {
            // Inverse the device rotation quaternion to counteract it's rotation in the tracker.
            float w = mRotationQuaternion[0];
            float x = mRotationQuaternion[1];
            float y = mRotationQuaternion[2];
            float z = mRotationQuaternion[3];

            float norm = w * w + x * x + y * y + z * z;

            if (norm > 0.0)
            {
                float invNorm = 1.0f / norm;
                x *= -invNorm;
                y *= -invNorm;
                z *= -invNorm;
                w *= invNorm;
            }

            mRotationQuaternion[0] = w;
            mRotationQuaternion[1] = x;
            mRotationQuaternion[2] = y;
            mRotationQuaternion[3] = z;

            // Native call to the markerless tracking object.  channels / number of channels in the image (mono, colour, colour+alpha)
            trackedData = processArbiTrackerFrame(data, mRotationQuaternion, width, height, 1, 0, false);

            Log.i(TAG, "processArbiTrackerFrame over");
        }
        if (trackedData != null)
        {

            // Set the supplied point ArrayList values to the returned projected tracking coordinates.
            projectedTrackingCorners.get(0).set(Math.round(trackedData[2]), Math.round(trackedData[3]));
            projectedTrackingCorners.get(1).set(Math.round(trackedData[4]), Math.round(trackedData[5]));
            projectedTrackingCorners.get(2).set(Math.round(trackedData[6]), Math.round(trackedData[7]));
            projectedTrackingCorners.get(3).set(Math.round(trackedData[8]), Math.round(trackedData[9]));

            for (int i = 0; i < 4; i++)
            {
                Log.i(TAG, "projectedTrackingCorners.get(i).x =  " + projectedTrackingCorners.get(i).x + "; y = " + projectedTrackingCorners.get(i).y);
            }

            /*
            //FIND MAX OF DISTANCE BETWEEN POINTS
            float max =0;
            for (int i=0; i<4; i++)
            {
                Point pt1 = projectedTrackingCorners.get(i);
                Point pt2= projectedTrackingCorners.get((i+1) %4);
                double d = Math.sqrt((pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y));
                if (d>max) max = (float)d;
            }
            //if (oldSize==0) oldSize = max*10f;
            if (oldSize==0) oldSize = max*10f;
            float scale = max/oldSize;
            */

            float max =0;
            for (int i=0; i<4; i++)
            {
                Point pt1 = projectedTrackingCorners.get(i);
                Point pt2= projectedTrackingCorners.get((i+1) %4);
                double d = Math.sqrt((pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y));
                if (d>max) max = (float)d;
            }
            float prescale = max / 2;
            float scale = prescale * mScaleFactor;
            //float scale = max / 2;


            //--------------------------
            //Render Updates
            //--------------------------
            if (render!=null && !frozen)
            {

                if (trackedData!=null)
                {
                    Point stable_point = smoother(trackedData[0], trackedData[1]);

                    //render.updatePosition(new Point(Math.round(trackedData[0]), Math.round(trackedData[1])), 0, scale, width, height);
                    //Log.i("@@POINT_MOD",":"+Math.round(trackedData[0])+":"+Math.round(trackedData[1]));
                    if(!lss && !angle_preset)
                    {
                        render.updateReference(stable_point, (float) enviro.getAzimuth(), (float) enviro.getZenithAngle(), scale, width, height);
                        //render.updatePosition(new Point(Math.round(trackedData[0]), Math.round(trackedData[1])), (float) enviro.getAzimuth(), (float) enviro.getZenithAngle(), scale, width, height);
                        render.setLightAngle(light1,(float) enviro.getAzimuth(), (float) enviro.getZenithAngle());
                        render.setLightAngle(light2,(float) enviro.getAzimuth(), 0);//chameleon

                    }
                    else if(lss)
                    {
                        render.updateReference(stable_point, (float) fixedAzimuth,(float) fixedZenith, scale, width, height);
                        render.setLightAngle(light1,(float) fixedAzimuth,(float) fixedZenith);
                        render.setLightAngle(light2,(float) fixedAzimuth, 0);//Chameleon

                    }
                    else if(!lss && angle_preset)
                    {
                        render.updateReference(stable_point, (float) preset_azimuth, (float) preset_zenith, scale, width, height);
                        render.setLightAngle(light1,(float) preset_azimuth,(float) preset_zenith);
                        render.setLightAngle(light2,(float) preset_azimuth, 0);//Chameleon
                    }

                }

                /*
                //retrieve sensor quarternion as retrieved by kudan and pass it to our jpoct renderer
                float[] matrix=new float[16];
                SensorManager.getRotationMatrixFromVector(matrix, mRotationQuaternion);
                render.setRotationMatrix(matrix);*/
            }
            glSurfaceView.requestRender();//跳真

        }
        return newState;
    }

    float oldSize=0;

    /*
    public void updatePosition(Point pt1, float angleRad, float sizeMarker, double width, double height) {

        if (pt1 != null) {
            Point center = smoothPoint(pt1);
            //move root to tracked location
            newPos = smoothSimpleVector(getWorldPointFrom(center.x, center.y, width, height));
        }
       if (sizeMarker>0) {
            float ratioForScale = (float) smoothSizing(sizeMarker);
            newSize = factor * ratioForScale;
        }
        newPosSun=new SimpleVector(newPos.x,newPos.y+100,newPos.z+100);

    }

     */

    private Point smoother(float px,float py)
    {
        // Point stable_point = smoother(trackedData[0],trackedData[1]);
        float smoothFactor = 0.3f;//smaller the number the less stablility but faster response to grid tracking updates.
        if(oldPoint != null)
        {
            float ptx = oldPoint.x * smoothFactor + (1f - smoothFactor) * px;
            float pty = oldPoint.y * smoothFactor + (1f - smoothFactor) * py;

            oldPoint = new Point(Math.round(ptx), Math.round(pty));
        }
        else
        {
            oldPoint = new Point(Math.round(px), Math.round(py));
        }

        return oldPoint;
    }

    //----------------------------------
    //Render Camera Frame to Screen
    //----------------------------------
    private void renderFrameToScreen(Bitmap cameraFrame, Rect cameraFrameRect, TrackerState currentState, ArrayList<Point> primitiveCorners)
    {
        state = currentState;
        final int buttonColor;
        final String buttonText;
        final String primitiveLabel;
        final String statusLabel;
        final Drawing.DrawingPrimitive primitive;

        if (currentState == TrackerState.IMAGE_DETECTION)
        {
            primitiveLabel = "";
            statusLabel = "Image Recognition";
            primitive = Drawing.DrawingPrimitive.DRAWING_NOTHING;

        } else if (currentState == TrackerState.IMAGE_TRACKING)
        {

            primitiveLabel = "ANI";
            statusLabel = "Image Tracking";
            primitive = Drawing.DrawingPrimitive.DRAWING_RECTANGLE;


        }
        else if (currentState == TrackerState.ARBITRACK)
        {
            primitiveLabel = "Arbitrack";
            statusLabel = "Plane Tracking";

            if (target_image != null)
            {
                primitive = Drawing.DrawingPrimitive.DRAWING_IMAGE;
            }
            else
            {
                primitive = Drawing.DrawingPrimitive.DRAWING_GRID;

            }


        } else {

            buttonColor = Color.TRANSPARENT;
            buttonText = "";
            primitiveLabel = "";
            statusLabel = "";
            primitive = Drawing.DrawingPrimitive.DRAWING_NOTHING;


        }

        // Update Android GUI.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mStatusLabel.setText(statusLabel);
                //mStatusLabel.setTextColor(buttonColor);
            }
        });

        // luoyouren: 显示帧率
        updateFrameRates();
        //---------------------

        mSrcRect.set(0, 0, mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight());
        mDstRect.set(0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight());
        mCanvasTransform.setRectToRect(mSrcRect, mDstRect, Matrix.ScaleToFit.END);

        Canvas canvas = mSurfaceView.getHolder().getSurface().lockCanvas(mSurfaceView.getClipBounds());

        // luoyouren: 将Camera Frame刷新给SurfaceView
        Drawing.drawBackground(
                canvas,
                cameraFrame
        );

        //------------------------------
        // if debug Draw the tracking primitive.
        //------------------------------
        // luoyouren:
        if(debug)
        {
            Drawing.drawPrimitive(
                    canvas,
                    mCanvasTransform,
                    primitive,
                    primitiveCorners.get(0),
                    primitiveCorners.get(1),
                    primitiveCorners.get(2),
                    primitiveCorners.get(3),
                    primitiveLabel,
                    target_image,
                    debug
            );
        }
        mSurfaceView.getHolder().getSurface().unlockCanvasAndPost(canvas);
    }


    private float	mRealTimeFrame; //实时帧率
    private float	mAverageFrame; //平均帧率
    private float	mAllFrame;
    private int		mFrameCount;
    private long	mLastTime = 0;
    String framerates;

    private void updateFrameRates()
    {

        long time = System.currentTimeMillis();
        if(mLastTime == 0){

            this.mRealTimeFrame = 0;
            this.mAverageFrame = 0;

        }else{

            this.mRealTimeFrame = 1000f /(time - mLastTime);
            this.mAllFrame += mRealTimeFrame;
            this.mFrameCount ++;
            this.mAverageFrame = mAllFrame/mFrameCount;
            if(mFrameCount > 400 ){
                mFrameCount = 0;
                mAllFrame = 0;
            }

        }
        mLastTime = time;
//        if(mFrameCount % 100 == 0){
//
//            framerates = "" + (int)(mAverageFrame*100)/100f;
//            Log.d("frame","frame = " + framerates);
//        }

        framerates = "" + (int)(mRealTimeFrame*100)/100f;
        Log.d("frame","frame = " + framerates);

        // Update Android GUI.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                info.setText(framerates);
                info.setTextColor(Color.RED);
            }
        });
    }



    //----------------------------
    //Add Trackable Resource
    //----------------------------
    public void addTrackable(int resourceID, String name)
    {
        Bitmap image = BitmapFactory.decodeResource(getResources(), resourceID);
        boolean success = addTrackableToImageTracker(image, name);

        if (!success)
        {
            throw new RuntimeException("Trackable could not be added to image tracker.");
        }
    }

    //--------------------------
    //Native Methods
    //--------------------------
    private native void initialiseImageTracker(String key, int width, int height);
    private native void initialiseArbiTracker(String key, int width, int height);
    private native void startArbiTracker(boolean startFromImageTrackable);
    private native void stopArbiTracker();

    private native boolean addTrackableToImageTracker(
            Bitmap image,
            String name);

    private native float[] processImageTrackerFrame(
            byte[] image,
            int width,
            int height,
            int channels,
            int padding,
            boolean requiresFlip);

    private native float[] processArbiTrackerFrame(
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

    //-----------------------------
    //decode Y, U, and V values on the YUV 420 & Split RGBA
    //-------------------------------------

    public void decodeYUV(int[] out, byte[] fg, int width, int height) throws NullPointerException, IllegalArgumentException {
        int sz = width * height;
        if (out == null) throw new NullPointerException("buffer out is null");
        if (out.length < sz)
            throw new IllegalArgumentException("buffer out size " + out.length + " < minimum " + sz);
        if (fg == null) throw new NullPointerException("buffer 'fg' is null");
        if (fg.length < sz)
            throw new IllegalArgumentException("buffer fg size " + fg.length + " < minimum " + sz * 3 / 2);
        int i, j;
        int Y, Cr = 0, Cb = 0;
        long totalR=0;
        long totalG=0;
        long totalB=0;
        for (j = 0; j < height; j++) {
            int pixPtr = j * width;
            final int jDiv2 = j >> 1;
            for (i = 0; i < width; i++) {
                Y = fg[pixPtr];
                if (Y < 0) Y += 255;
                if ((i & 0x1) != 1) {
                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                    Cb = fg[cOff];
                    if (Cb < 0) Cb += 127;
                    else Cb -= 128;
                    Cr = fg[cOff + 1];
                    if (Cr < 0) Cr += 127;
                    else Cr -= 128;
                }
                int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                if (R < 0) R = 0;
                else if (R > 255) R = 255;
                int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                if (G < 0) G = 0;
                else if (G > 255) G = 255;
                int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                if (B < 0) B = 0;
                else if (B > 255) B = 255;
                totalR+=R;
                totalG+=G;
                totalB+=B;
                out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
            }
        }
        //Oli note the RGB above was not matching the r g b expected colour as we retrieve a aRGB format
        //so the switch below between blue and red components
        lastImageR=(int)(totalB/(width*height));
        lastImageG=(int)(totalG/(width*height));
        lastImageB=(int)(totalR/(width*height));

        //Log.i("@@RGB"," R:"+lastImageR);

        if(render != null)
        {
            render.setLightColor(light2,lastImageR,lastImageG,lastImageB);
        }

    }




        // using existing renderscript method, seems a bit slower?
    public Allocation renderScriptNV21ToRGBA888(RenderScript rs, int width, int height, byte[] nv21) {

        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        return out;
    }

    protected Object3D loadTest()
    {
        Object3D myObject = Primitives.getCube(5);
        RGBColor cubeColour = new RGBColor(222, 120, 40);
        myObject.setAdditionalColor(cubeColour);
        myObject.setName("testCube");
        myObject.build();
        return myObject;
    }
        /*
    protected Object3D loadTable()
    {
        //Wood Texture 2.JPG
        //Wood Texture.jpg

        Texture tx = new Texture(getResources().getDrawable(R.drawable.woodtexture));
        Texture tx1 = new Texture(getResources().getDrawable(R.drawable.woodtexturetwo));

        if (!TextureManager.getInstance().containsTexture("Wood Texture.jpg"))
        {
            TextureManager.getInstance().addTexture("Wood Texture.jpg", tx);
        }
        if (!TextureManager.getInstance().containsTexture("Wood Texture 2.JPG"))
        {
            TextureManager.getInstance().addTexture("Wood Texture 2.JPG", tx1);
        }

        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        try {

            Object3D[] objects = Loader.load3DS(mgr.open("coffeetable.3ds"), 5.0f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("table");
            renderObj.setTexture("Wood Texture.jpg");
            renderObj.setTexture("Wood Texture 2.JPG");
            renderObj.setSpecularLighting(false);
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(5);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }
    */
    //Loader.loadOBJ(getResources().getAssets().open("cube.obj"), null, 20)

    Object3D loadPlant01()
    {
        String n="plant01";
        if (!TextureManager.getInstance().containsTexture("plant01"))
        {
            Texture tx = new Texture(getResources().getDrawable(R.drawable.plant01),false);
            TextureManager.getInstance().addTexture(n,tx);
        }


        try {
            Object3D[] s=Loader.load3DS(getActivity().getAssets().open(n+".3ds"),1f);
            Object3D j=Object3D.mergeAll(s);
            j.setTexture(n);
            j.setName(n);
            return j;
        }
        catch (Exception x) { return null; }
    }



    protected Object3D loadTable()
    {

        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        Texture tx = new Texture(getResources().getDrawable(R.drawable.woodone));//128x128
        Texture tx1 = new Texture(getResources().getDrawable(R.drawable.woodtwo));//128x128

        if (!TextureManager.getInstance().containsTexture("Wood_1"))
        {
            TextureManager.getInstance().addTexture("Wood_1", tx);
            TextureManager.getInstance().addTexture("Wood_2", tx1);
        }

        try {

            Object3D[] objects = Loader.loadOBJ(mgr.open("coffeetable.obj"),mgr.open("coffeetable.mtl"), 9f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("table");
            renderObj.setTexture("Wood_2");
            renderObj.setSpecularLighting(false);
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(9);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }

    protected Object3D loadPlant()
    {

        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        Texture tx = new Texture(getResources().getDrawable(R.drawable.plant));//128x128

        if (!TextureManager.getInstance().containsTexture("plant"))
        {
            TextureManager.getInstance().addTexture("plant", tx);
        }

        try {

            Object3D[] objects = Loader.loadOBJ(mgr.open("plant.obj"),null, 4f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("table");
            renderObj.setTexture("plant");
            renderObj.setSpecularLighting(false);
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(4);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }

    protected Object3D loadIronman()
    {

        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        Texture tx = new Texture(getResources().getDrawable(R.drawable.ironman_mask));//128x128

        if (!TextureManager.getInstance().containsTexture("ironman_mask"))
        {
            TextureManager.getInstance().addTexture("ironman_mask", tx);
        }

        try {

            Object3D[] objects = Loader.loadOBJ(mgr.open("ironman_mask.obj"),mgr.open("ironman_mask.mtl"), 6f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("ironman_mask");
            renderObj.setTexture("ironman_mask");
            renderObj.setSpecularLighting(false);
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.setOrientation(new SimpleVector(0, 1, 0), new SimpleVector(0, 90, 0));
            renderObj.scale(1.0f);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }

    //-------------------------
    //Load plant, from blender .3ds export Z Forward Y Up.
    //--------------------------------
    protected Object3D loadPlant2()
    {
        Texture tx = new Texture(getResources().getDrawable(R.drawable.indoor));//128x128

        if (!TextureManager.getInstance().containsTexture("indoor"))
        {
            TextureManager.getInstance().addTexture("indoor", tx);
        }

        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        try {

            Object3D[] objects = Loader.load3DS(mgr.open("myplant.3ds"), 6.0f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("ball");
            renderObj.setSpecularLighting(false);
            renderObj.setTexture("indoor");
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(6);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }



    protected Object3D loadObj()
    {
        Texture tx = new Texture(getResources().getDrawable(R.drawable.ballskin2));//128x128

        if (!TextureManager.getInstance().containsTexture("skinner"))
        {
            TextureManager.getInstance().addTexture("skinner", tx);
        }

        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        try {

            Object3D[] objects = Loader.load3DS(mgr.open("soccerball.3ds"), 1.0f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("ball");
            renderObj.setSpecularLighting(false);
            renderObj.setTexture("skinner");
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(1);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }

    protected Object3D loadHouse()
    {
        Object3D renderObj = null;
        AssetManager mgr = getActivity().getAssets();

        Texture tx = new Texture(getResources().getDrawable(R.drawable.house1));//128x128
        Texture tx1 = new Texture(getResources().getDrawable(R.drawable.house2));//128x128

        if (!TextureManager.getInstance().containsTexture("house1"))
        {
            TextureManager.getInstance().addTexture("house1", tx);
            TextureManager.getInstance().addTexture("house2", tx1);
        }

        try {

            Object3D[] objects = Loader.loadOBJ(mgr.open("house.obj"),mgr.open("house.mtl"), 1.0f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("house");
            renderObj.setTexture("house1");
            renderObj.setSpecularLighting(false);
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(0.5f);
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }

    private void sendToast(String str)
    {
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), str, Toast.LENGTH_LONG);
        toast.show();
    }

    //-----------------------
    //On Fragment completed
    //-----------------------
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        try
        {
            this.mListener = (OnOrbiCompleteListener)getActivity();
            this.modListener = (OnModelChangeListener)getActivity();
        }
        catch (final ClassCastException e)
        {
            throw new ClassCastException(getActivity().toString() + " must implement OnOrbiCompleteListener and OnModelChanged");
        }
    }

    //-----------------------
    //Getters,setters
    //-----------------------
    public String getTestString()
    {
        String st = "hello";
        return st;
    }


}
