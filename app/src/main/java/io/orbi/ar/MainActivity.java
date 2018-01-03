package io.orbi.ar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;

import io.orbi.ar.fragments.CameraFragment;
import io.orbi.ar.interfaces.OnModelChangeListener;
import io.orbi.ar.interfaces.OnOrbiCompleteListener;
import io.orbi.ar.utils.BasicTimer;

/**
 * Main activity that checks and requests camera permissions from the user before starting the
 * KudanCV demo.
 */
public class MainActivity extends Activity implements OnOrbiCompleteListener,OnModelChangeListener
{
    private Typeface typeface;
    private CameraFragment arScene;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Make the activity fullscreen.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //layout
        setContentView(R.layout.activity_main);

        //Custom Fonts
        typeface = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/Roboto_Light.ttf");

        //-----------------------------
        //If no permissions set,saved
        //-----------------------------
        if (null == savedInstanceState)
        {
            permissionsRequest();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        overrideFonts(this,getWindow().getDecorView());
    }

    //-------------------------------
    //Check Permissions & if set
    //-------------------------------
    public void permissionsRequest()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.READ_PHONE_STATE,Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    1);
        }
        else
        {
            setupFragment();
        }
    }

    //--------------------------------------
    //On Permission acceptance if not set
    //---------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                {

                    setupFragment();
                }
                else
                {

                    throw new RuntimeException("Camera permissions must be granted to function.");
                }
            }
        }
    }


    //--------------------------
    //Setup the intial Scene
    //--------------------------
    private void setupFragment()
    {
        Bundle bundle = new Bundle();
        bundle.putInt("model_index", 0);
        arScene = CameraFragment.newInstance();
        arScene.setArguments(bundle);

        getFragmentManager().beginTransaction().replace(R.id.camera_container, arScene).commit();

    }

    //---------------------------------------------------
    //Arscene Has loaded, can setup all orbi features
    //---------------------------------------------------
    public void onComplete()
    {
        Log.i("@@APP"," Loaded");
        // After the fragment completes, it calls this callback.
        // setup the rest of your layout now
        //mMapFragment.getMap().
        String st = arScene.getTestString();

        //arScene.load3DResource()
    }

    //-----------------------
    //On model asset changes
    //------------------------
    public void onModelChanged(final int mod)
    {
        getFragmentManager().beginTransaction().remove(arScene).commit();

        BasicTimer.TaskHandle handle = BasicTimer.setTimeout(new Runnable()
        {
            public void run()
            {
                Bundle bundle = new Bundle();
                bundle.putInt("model_index", mod);
                arScene = CameraFragment.newInstance();
                arScene.setArguments(bundle);
                getFragmentManager().beginTransaction().replace(R.id.camera_container, arScene).commit();

            }
        }, 3000);

    }


    //----------------------------------
    //Font override for whole project
    //-----------------------------------
    private void overrideFonts(final Context context, final View v)
    {
        try
        {
            if (v instanceof ViewGroup)
            {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++)
                {
                    View child = vg.getChildAt(i);
                    overrideFonts(context, child);
                }
            } else if (v instanceof TextView) {
                ((TextView) v).setTypeface(typeface);
            } else if (v instanceof EditText ) {
                ((EditText) v).setTypeface(typeface);
            } else if (v instanceof Button) {
                ((Button) v).setTypeface(typeface);
            }
            else if (v instanceof Switch)
            {
                ((Switch) v).setTypeface(typeface);
            }
            else if (v instanceof Spinner)
            {
                ((Switch) v).setTypeface(typeface);
            }
            else if (v instanceof SpinnerAdapter)
            {
                ((Switch) v).setTypeface(typeface);
            }
            else if (v instanceof CheckedTextView)
            {
                ((Switch) v).setTypeface(typeface);
            }
            //CheckedTextView
        }
        catch (Exception e)
        {
        }
    }
    /*
    JPCT REMOVE AND ADD MODEL
    getCurrentScene().removeChild(myCurrentObj);
ObjParser parser = new ObjParser(this, 'mydir\myfile.obj');
parser.parse();
getCurrentScene().addChild(parser.getParsedObject());
     */
}
