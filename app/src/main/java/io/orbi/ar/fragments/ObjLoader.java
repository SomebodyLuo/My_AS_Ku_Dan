package io.orbi.ar.fragments;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

import java.io.IOException;

import io.orbi.ar.R;

/**
 * Created by pc on 2018/1/11.
 */

public class ObjLoader {

    //luoyouren
    private String TAG = "ObjLoader";
    private Resources mRes = null;
    private Activity mActivity = null;

    public ObjLoader(Resources res, Activity activity)
    {
        mRes = res;
        mActivity = activity;
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

        Texture tx = new Texture(mRes.getDrawable(R.drawable.woodtexture));
        Texture tx1 = new Texture(mRes.getDrawable(R.drawable.woodtexturetwo));

        if (!TextureManager.getInstance().containsTexture("Wood Texture.jpg"))
        {
            TextureManager.getInstance().addTexture("Wood Texture.jpg", tx);
        }
        if (!TextureManager.getInstance().containsTexture("Wood Texture 2.JPG"))
        {
            TextureManager.getInstance().addTexture("Wood Texture 2.JPG", tx1);
        }

        Object3D renderObj = null;
        AssetManager mgr = mActivity.getAssets();

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
    //Loader.loadOBJ(mRes.getAssets().open("cube.obj"), null, 20)

    Object3D loadPlant01()
    {
        String n="plant01";
        if (!TextureManager.getInstance().containsTexture("plant01"))
        {
            Texture tx = new Texture(mRes.getDrawable(R.drawable.plant01),false);
            TextureManager.getInstance().addTexture(n,tx);
        }


        try {
            Object3D[] s= Loader.load3DS(mActivity.getAssets().open(n + ".3ds"),1f);
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
        AssetManager mgr = mActivity.getAssets();

        Texture tx = new Texture(mRes.getDrawable(R.drawable.woodone));//128x128
        Texture tx1 = new Texture(mRes.getDrawable(R.drawable.woodtwo));//128x128

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
        AssetManager mgr = mActivity.getAssets();

        Texture tx = new Texture(mRes.getDrawable(R.drawable.plant));//128x128

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
        AssetManager mgr = mActivity.getAssets();

        Texture tx = new Texture(mRes.getDrawable(R.drawable.ironman_mask));//128x128

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
        Texture tx = new Texture(mRes.getDrawable(R.drawable.indoor));//128x128

        if (!TextureManager.getInstance().containsTexture("indoor"))
        {
            TextureManager.getInstance().addTexture("indoor", tx);
        }

        Object3D renderObj = null;
        AssetManager mgr = mActivity.getAssets();

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
        Texture tx = new Texture(mRes.getDrawable(R.drawable.ballskin2));//128x128

        if (!TextureManager.getInstance().containsTexture("skinner"))
        {
            TextureManager.getInstance().addTexture("skinner", tx);
        }

        Object3D renderObj = null;
        AssetManager mgr = mActivity.getAssets();

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

    public Object3D loadHouse()
    {
        Object3D renderObj = null;
        AssetManager mgr = mActivity.getAssets();

        Texture tx = new Texture(mRes.getDrawable(R.drawable.house1));//128x128
        Texture tx1 = new Texture(mRes.getDrawable(R.drawable.house2));//128x128

        if (!TextureManager.getInstance().containsTexture("house1"))
        {
            TextureManager.getInstance().addTexture("house1", tx);
            TextureManager.getInstance().addTexture("house2", tx1);
        }

        try {

            Object3D[] objects = Loader.loadOBJ(mgr.open("house1.obj"),mgr.open("house.mtl"), 1.0f);
            renderObj = Object3D.mergeAll(objects);
            renderObj.setName("house");
            renderObj.setTexture("house1");
            renderObj.setSpecularLighting(false);
            renderObj.setOrigin(new SimpleVector(0, 0, 0));
            renderObj.scale(0.2f);
            renderObj.setOrientation(new SimpleVector(0, 0, 1), new SimpleVector(-10, 10, 0));
            renderObj.strip();
            renderObj.build();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return renderObj;

    }
}
