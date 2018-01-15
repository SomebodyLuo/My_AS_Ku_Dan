package io.orbi.ar.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;


import io.orbi.ar.CameraSurfaceView;

import io.orbi.ar.R;
import io.orbi.ar.detection.EnviroDetection;
import io.orbi.ar.detection.LocationDetection;
import io.orbi.ar.interfaces.OnModelChangeListener;
import io.orbi.ar.interfaces.OnOrbiCompleteListener;
import io.orbi.ar.render.RendererJPCT;
import io.orbi.ar.utils.BasicTimer;

import static android.content.Context.MODE_PRIVATE;
import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;




public class CameraFragment extends Fragment
{
    private final String TAG = "luoyouren";

    public OnOrbiCompleteListener mListener;//call back to main activity
    public OnModelChangeListener modListener;//when model asset needs changing

    private final float[] mRotationQuaternion = new float[4];
    public RendererJPCT render;

    RectF mSrcRect = new RectF();
    RectF mDstRect = new RectF();
    Matrix mCanvasTransform = new Matrix();

    private GLSurfaceView glSurfaceView;

    private CameraPreview mCameraPreview;
    private VideoPreview mVideoPreview;

    private Size mViewSize = new Size(1280, 720);//1920, 1080    //(1280, 720);
    private Size mPreviewSize = null;

    private SensorWork sensorWork;

    private CameraSurfaceView mSurfaceView;

    private TextView mStatusLabel;

    private ArrayList<Point> trackedCorners = new ArrayList<>(4);

    enum TrackerState {
        IMAGE_DETECTION,
        IMAGE_TRACKING,
        ARBITRACK
    }
    private TrackerState state;
    private TrackerState mTrackerState = TrackerState.IMAGE_DETECTION;

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
    public EnviroDetection enviro = null;
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
        mSurfaceView.setAspectRatio(mViewSize.getWidth(), mViewSize.getHeight());

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
//        target_bttn.setOnTouchListener(new View.OnTouchListener()
//        {
//            public boolean onTouch(View v, MotionEvent event)
//            {
//                if(event.getAction() == MotionEvent.ACTION_UP)
//                {
//                    if(render != null)
//                    {
//                        render.stick2screen(-600, -600);//default it off screen in between states
//                        glSurfaceView.requestRender();//跳真
//                        lss_switch.setChecked(false);//turn off manual light as need touch move events for model.
//                    }
//
//                }
//                if(event.getAction() == MotionEvent.ACTION_DOWN)
//                {
//
//                        // Synchronize with the tracker state to prevent changes to state mid-processing.
//                        synchronized (mTrackerState)
//                        {
//
//                            if (mTrackerState == TrackerState.IMAGE_DETECTION)
//                            {
//
//                                startArbiTracker(false);
//
//                                mTrackerState = TrackerState.ARBITRACK;
//                                target_bttn.setImageResource(R.drawable.camera_icon);
//
//
//
//                            }
//                            else if (mTrackerState == TrackerState.IMAGE_TRACKING)
//                            {
//
//                                startArbiTracker(true);
//
//                                mTrackerState = TrackerState.ARBITRACK;
//                                target_bttn.setImageResource(R.drawable.camera_icon);
//
//
//                                if (render != null)
//                                {
//                                    int xp = v.getWidth() / 2;
//                                    int yp = v.getHeight() / 2;
//                                    //render.updatePosition(new Point(xp, yp), (float) enviro.getAzimuth(), (float) enviro.getZenithAngle(), 5f, mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight());
//                                }
//
//
//                            }
//                            else if (mTrackerState == TrackerState.ARBITRACK)
//                            {
//
//                                stopArbiTracker();
//
//                                mTrackerState = TrackerState.IMAGE_DETECTION;
//                                target_bttn.setImageResource(R.drawable.target);
//
//
//
//                            }
//                        }
//
//                }
//
//                return false;
//            }
//        });


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

        // 初始化摄像头
//        mCameraPreview = new CameraPreview(getContext(), mPreviewSize);

        // 初始化视频文件
        mVideoPreview = new VideoPreview(getContext());


        // 初始化传感器
        sensorWork = new SensorWork(getContext());


        // 初始化 OpenGL/JPCT 相关
        setScene(view);



    }

    public VideoPrepareListener videoPrepareListener = new VideoPrepareListener() {
        @Override
        public void onVideoPrepare(Size size) {
            mPreviewSize = size;

            // 初始化Tracker
            InitAPI initAPI = new InitAPI(getContext(), mPreviewSize);
            initAPI.Initialise();


            //-------------------------------------------------------
            // Add the image trackable to the native image tracker.
            //-------------------------------------------------------
//            addTrackable(R.mipmap.ani1, "ANI1");
//            addTrackable(R.mipmap.ani2, "ANI2");
//            addTrackable(R.mipmap.kien, "Kien");
            addTrackable(R.mipmap.sample, "sample");
//            addTrackable(R.mipmap.desktop, "desktop");
//            addTrackable(R.mipmap.roof, "roof");
//            addTrackable(R.mipmap.roof2, "roof2");
//            addTrackable(R.mipmap.roof3, "roof3");
//            addTrackable(R.mipmap.roof4, "roof4");

            addTrackable(R.mipmap.roof10, "roof10");
            addTrackable(R.mipmap.roof11, "roof11");
            addTrackable(R.mipmap.roof12, "roof12");
            addTrackable(R.mipmap.roof13, "roof13");
            addTrackable(R.mipmap.roof14, "roof14");
            addTrackable(R.mipmap.roof15, "roof15");
            addTrackable(R.mipmap.roof16, "roof16");
            addTrackable(R.mipmap.roof17, "roof17");
            //-------------------------------------------------------
        }
    };


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

    private ObjLoader objLoader = null;
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

        objLoader = new ObjLoader(getResources(), getActivity());

        //which model to load and settings
        if(model_index == 0 || model_index == 1)
        {
            Log.i("@@INDEX",":Plant");
            sendToast("Plant Loaded");
            render.init(objLoader.loadHouse());
            render.setLightColor(light1, 255, 255, 255);
            render.setSunLum(210);//set ambient init light
        }
        else if(model_index == 2)
        {
            Log.i("@@INDEX",":Ball");
            sendToast("Ball Loaded");
            render.init(objLoader.loadPlant01());
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

        if (enviro != null)
        {
            int gazi = (int) Math.round(enviro.getAzimuth());
            int zazi = (int) Math.round(enviro.getZenithAngle());

            output.setText("AZI: " + gazi + " | ZEN: " + zazi + " | " + LocationDetection.loci);
        }


    }

    @Override
    public void onPause()
    {

//        mCameraPreview.teardownCamera();
//        mCameraPreview.teardownBackgroundThread();

        mVideoPreview.teardownBackgroundThread();

        sensorWork.teardownRotationSensor();

        super.onPause();
    }

    @Override
    public void onResume()
    {

        super.onResume();

//        mCameraPreview.setupBackgroundThread();
        mVideoPreview.setupBackgroundThread();

        if (mSurfaceView.getHolder().getSurface().isValid())
        {
//            mCameraPreview.setImageAvailListener(mImageAvailListener);
//            mCameraPreview.initCamera2();

            mVideoPreview.InitMediaPlayer(mViewSize, mImageAvailListener, videoPrepareListener);
        }
        else
        {
            mSurfaceView.getHolder().addCallback(mSurfaceCallback);
        }

        sensorWork.setupRotationSensor(render);
    }

    private static final int FRAMERATE = 10;
    private static final int DOWNLOAD_FAILED = 1;

    private final MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler{

        //对Activity的弱引用
        private final WeakReference<CameraFragment> fragmentWeakReference;

        public MyHandler(CameraFragment fragment){
            fragmentWeakReference = new WeakReference<CameraFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            final CameraFragment fragment = fragmentWeakReference.get();
            if(fragment == null){
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {
                case DOWNLOAD_FAILED:


                    break;
                case FRAMERATE:

                    fragment.info.setText((String)msg.obj);
                    fragment.info.setTextColor(Color.RED);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
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
            trackedData = NativeTracker.processImageTrackerFrame(data, width, height, 1, 0, false);

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
            trackedData = NativeTracker.processArbiTrackerFrame(data, mRotationQuaternion, width, height, 1, 0, false);

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
        // Update Android GUI.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                info.setText(FrameRate.updateFrameRates());
                info.setTextColor(Color.RED);
            }
        });
        //---------------------

        mSrcRect.set(0, 0, mPreviewSize.getWidth(), mPreviewSize.getHeight());
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

    //----------------------------
    //Add Trackable Resource
    //----------------------------
    public void addTrackable(int resourceID, String name)
    {
        Bitmap image = BitmapFactory.decodeResource(getResources(), resourceID);
        boolean success = NativeTracker.addTrackableToImageTracker(image, name);

        if (!success)
        {
            throw new RuntimeException("Trackable could not be added to image tracker.");
        }
    }


    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback()
    {

        @Override
        public void surfaceCreated(SurfaceHolder holder)
        {

            // Setup the camera only when the Surface has been created to ensure a valid output
            // surface exists when the CameraCaptureSession is created.
//            mCameraPreview.setImageAvailListener(mImageAvailListener);
//            mCameraPreview.initCamera2();
            mVideoPreview.InitMediaPlayer(mViewSize, mImageAvailListener, videoPrepareListener);
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


        Rect cameraFrameRect = new Rect();

        Bitmap cameraFrame = null;
        byte[] cameraFrameData = null;

        int[] argb8888 = null;
        Bitmap colourFrame = null;

        @Override
        public void onImageAvailable(ImageReader reader) {

//            if (null == cameraFrame) {
//                cameraFrame = Bitmap.createBitmap(mPreviewSize.getWidth(), mPreviewSize.getHeight(), Bitmap.Config.ALPHA_8);
//            }
//
//            if (null == cameraFrameData) {
//                cameraFrameData = new byte[mPreviewSize.getWidth() * mPreviewSize.getHeight()];
//            }
//
//            if (null == argb8888) {
//                argb8888 = new int[mPreviewSize.getWidth() * mPreviewSize.getHeight()];
//            }
//
//            if (null == colourFrame) {
//                colourFrame = Bitmap.createBitmap(mPreviewSize.getWidth(), mPreviewSize.getHeight(), Bitmap.Config.ARGB_8888);
//            }

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

                    final Image.Plane[] planes = currentCameraImage.getPlanes();
                    Log.i(TAG, "image format = " + currentCameraImage.getFormat());
                    Log.i(TAG, "image.width = " + currentCameraImage.getWidth());
                    Log.i(TAG, "image.height = " + currentCameraImage.getHeight());
                    Log.i(TAG, "planes = " + planes.length);

                    Log.i(TAG, "planes[0].getPixelStride = " + planes[0].getPixelStride());
                    Log.i(TAG, "planes[0].getRowStride = " + planes[0].getRowStride());

                    Log.i(TAG, "planes[1].getPixelStride = " + planes[1].getPixelStride());
                    Log.i(TAG, "planes[1].getRowStride = " + planes[1].getRowStride());

                    Log.i(TAG, "planes[2].getPixelStride = " + planes[2].getPixelStride());
                    Log.i(TAG, "planes[2].getRowStride = " + planes[2].getRowStride());

                    //first step get the YUV-format data
                    Image.Plane Y = planes[0];
                    Image.Plane U = planes[1];
                    Image.Plane V = planes[2];

                    int Yb = Y.getBuffer().remaining();
                    int Ub = U.getBuffer().remaining();
                    int Vb = V.getBuffer().remaining();

                    byte[] data = new byte[Yb * 2];

                    Y.getBuffer().get(data, 0, Yb);
                    U.getBuffer().get(data, Yb + 1, Ub);
                    V.getBuffer().get(data, Yb * 3 / 2 + 1, Vb);

                    int width = planes[0].getRowStride();
                    int height = (int)Math.ceil(Yb / width);    // 对一个数进行上取整。让最终的Bitmap能够全部存储下所有的YUV byte
                    Log.i(TAG, "operating width = " + width + "; height = " + height);
                    if (null == argb8888) {
                        argb8888 = new int[width * height];
                    }

                    if (null == colourFrame) {
                        colourFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    }

                    int YDataSize = mPreviewSize.getWidth() * mPreviewSize.getHeight();
                    if (null == cameraFrameData) {
                        cameraFrameData = new byte[width * height];
                    }
                    Y.getBuffer().rewind();
                    Y.getBuffer().get(cameraFrameData, 0, YDataSize);

                    // Process tracking based on the new camera frame data.
                    mTrackerState = processTracking(cameraFrameData, mPreviewSize.getWidth(), mPreviewSize.getHeight(), mTrackerState, trackedCorners);


                    //method 2
                    decodeYUVSemiPlanar(argb8888, data, width, height);

                    colourFrame.setPixels(argb8888, 0, width, 0, 0, width, height);

                    //----------------------------------
                    //Render Camera Frame to Screen
                    //----------------------------------
                    renderFrameToScreen(colourFrame, cameraFrameRect, mTrackerState, trackedCorners);

                    //------------------------------------------------------------------------------
                    //end oli for PFL addition
                    // Clean up frame data.
                    currentCameraImage.close();
                }
            }
        }
    };



    //-----------------------------
    //decode Y, U, and V values on the YUV 420 & Split RGBA
    //-------------------------------------

    public void decodeYUVSemiPlanar(int[] out, byte[] fg, int width, int height) throws NullPointerException, IllegalArgumentException {
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
