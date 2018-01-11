package io.orbi.ar.render;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

import android.content.Context;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Point;
import android.opengl.GLSurfaceView;

import io.orbi.ar.orientationProvider.OrientationProvider;
import io.orbi.ar.representation.MatrixF4x4;


public class RendererJPCT implements GLSurfaceView.Renderer {

//	public static class Point { public double x,y; }

    static Context context;
    static GLSurfaceView GLview;
    static GL10 gl;
    static FrameBuffer fb;
    static TextureManager texMgr = TextureManager.getInstance();
    static World mainWorld = new World(), flatShdWorld = new World();
    static ScreenManager screenMgr;
    static String[] fileType = {"png", "jpg", "tga"};
    static int texQty = 64, objPosMode;
    static String[] texName = new String[texQty];
    static Texture[] tex = new Texture[texQty];
    static String folderTex;
    static float userScale;
    static OrientationProvider orientProvider;
    static Object3D soccerball, md2sample, spacecraft1, plant01;
    static MatrixF4x4 mat4 = new MatrixF4x4();
    static float[] vec9tmp1 = new float[9], vec9tmp2 = new float[9],
            refPos = new float[3], pos2ref = new float[2];
    static String[]
            glslCmn = {"precision highp float;", "precision mediump float;",
            "precision lowp float;", "void main(){", "varying vec2 tc;"},
            glslV = {"uniform mat4 modelViewProjectionMatrix;attribute vec4 position;",
                    "gl_Position=modelViewProjectionMatrix*", "attribute vec2 texture0;",
                    "tc=texture0.xy", "position;", "vec4 p=", "tc.x=u[i];tc.y=u[i+1];"},
            glslF = {"uniform vec4 fc;uniform float fd;", "uniform vec4 c0;",
                    "uniform sampler2D textureUnit0;", "uniform vec2 nf;",
                    "gl_FragColor", "texture2D(textureUnit0,tc)", "=mix(fc,"};

    public RendererJPCT(Context a) {
        context = a;
        AssetJPCT.context = a;
        AssetJPCT.texMgr = texMgr;
        initAll();
    }

    @Override
    public void onSurfaceCreated(GL10 g, EGLConfig c) {
    }

    @Override
    public void onSurfaceChanged(GL10 g, int w, int h) {
        boolean needInit = (fb == null);
        if (!needInit) fb.dispose();
        fb = new FrameBuffer(w, h);
        gl = g;
        screenMgr = new ScreenManager();
        if (needInit) ViewportJPCT.init();
        gainFocus();
    }

    @Override
    public void onDrawFrame(GL10 g) {
        if (screenMgr == null) return;
        stickObj();
        gl.glDisable(GL10.GL_DEPTH_TEST);
        G3dJPCT.update();
        fb.removeRenderTarget();
        gl.glEnable(GL10.GL_DEPTH_TEST);
        ShadowJPCT.updateShdHelp();
//			fb.clear(0x80808080);
        fb.clear();
        G3dJPCT.draw();
//			ShadowJPCT.shdHelp.blit(fb);
        fb.display();
    }

    class ScreenManager {
        int width, height, halfWidth, halfHeight;
        double aspect;

        public ScreenManager() {
            width = fb.getWidth();
            height = fb.getHeight();
            aspect = ((double) width) / height;
            halfWidth = width / 2;
            halfHeight = height / 2;
        }
    }

    static void loadTex(int a, String b, Texture c, boolean d) {
        if (texName[a] != null) return;
        texName[a] = b;
        tex[a] = c;
        tex[a].compress();
        tex[a].setTextureCompression(d);
        texMgr.addTexture(texName[a], tex[a]);
    }

    static void unloadTex(int a) {
        if (tex[a] != null) texMgr.unloadTexture(fb, tex[a]);
    }

    void gainFocus() {
        RttJPCT.gainFocus();
        G3dJPCT.gainFocus();
        EnvironmentJPCT.gainFocus();
        ViewportJPCT.gainFocus();
        ShadowJPCT.gainFocus();
    }

    void initAll() {
        initConfig();
        MathJPCT.init();
        RttJPCT.init();
        G3dJPCT.init();
        EnvironmentJPCT.init();
        ShadowJPCT.init();
    }

    void initConfig() {
        Config.glIgnoreNearPlane = false;
        Config.glTrilinear = true;
        Config.glTransparencyMul = 0.004f;
        Config.glTransparencyOffset = 0;
        Config.specPow = 90;
        Logger.setLogLevel(0);
    }

    public void init(Object3D a) {
        if (a.getName() == "md2sample") {
            md2sample = a;
            G3dJPCT.addObj(2);
        } else if (a.getName() == "spacecraft1") {
            spacecraft1 = a;
            G3dJPCT.addObj(3);
        } else if (a.getName() == "plant01") {
            plant01 = a;
            G3dJPCT.addObj(4);
        } else {
            soccerball = a;
            G3dJPCT.addObj(1);
        }
        G3dJPCT.setObjVisible(0, true);
    }

    public void setFovX(float a) {
        if (screenMgr != null) ViewportJPCT.setFovX(a / 2);
    }

    public void setElevation(float a) {
    }

    public void setSunMode(boolean a) {
        EnvironmentJPCT.enableSun(a);
    }

    public void setSunLum(float a) {
        EnvironmentJPCT.setSunRGB(a / 255, a / 255, a / 255);
    }

    public void setAmbientLum(float a) {
        EnvironmentJPCT.setAmbientRGB(a / 255, a / 255, a / 255);
    }

    public void setOrientationProvider(OrientationProvider a) {
        orientProvider = a;
    }

    public void setVisible(boolean a) {
        G3dJPCT.visObjs[0].show(a);
    }

    public boolean objectVisible() {
        return (!G3dJPCT.visObjs[0].hidden);
    }

    public void setUserScaleFactor(float a) {
        userScale = a;
    }

    public int addSun() {
        return EnvironmentJPCT.addLight(true);
    }

    public void setLightColor(int a, float b, float c, float d) {
        EnvironmentJPCT.setLightColor(a, b / 255, c / 255, d / 255);
    }

    public void setLightAngle(int a, float b, float c) {
        EnvironmentJPCT.setLightPH(a, -(float) (c * MathJPCT.deg2rad), (float) ((180 - b) * MathJPCT.deg2rad));
    }

    public void updatePosition(Point a, float b, float c, float d, double e, double f) {
        if ((screenMgr == null) || (orientProvider == null)) return;
        float[] m, n = new float[3], v = ViewportJPCT.fovTan;
        float s;
        orientProvider.getRotationMatrix(mat4);
        m = mat4.matrix;
//		m=new float[]{0,0,-1,0,0,0,0,0,1,0,0};
//		m=new float[]{0.707f,0,-0.707f,0,0,0,0,0,0.707f,0,0.707f};
        ViewportJPCT.setVec6(-m[6], m[2], -m[10], m[4], -m[0], m[8]);
        s = (float) (0.1 / (v[0] * d * (1 + userScale / 2) / e));
        n[1] = (float) Math.atan((1 - 2f * a.x / e) * v[0]);
        n[0] = (float) Math.atan((1 - 2f * a.y / f) * v[1]);
        MathJPCT.phr2vec9_f(n, vec9tmp1);
        MathJPCT.vec9add(vec9tmp1, ViewportJPCT.vec9, vec9tmp2);
        MathJPCT.vec3mul1_fff(vec9tmp2, s, n);
        G3dJPCT.setObjPos(0, n, null);
        objPosMode = 0;
        EnvironmentJPCT.setSunPH(-(float) (c * MathJPCT.deg2rad), (float) ((180 - b) * MathJPCT.deg2rad));
    }

    public void updateReference(Point a, float b, float c, float d, double e, double f) {
        if ((screenMgr == null) || (orientProvider == null)) return;
        float[] m, n = new float[3], v = ViewportJPCT.fovTan;
        float s;
        orientProvider.getRotationMatrix(mat4);
        m = mat4.matrix;
//		m=new float[]{0,0,-1,0,0,0,0,0,1,0,0};
//		m=new float[]{0.707f,0,-0.707f,0,0,0,0,0,0.707f,0,0.707f};
        ViewportJPCT.setVec6(-m[6], m[2], -m[10], m[4], -m[0], m[8]);
        s = (float) (0.1 / (v[0] * d * (1 + userScale / 2) / e));
        n[1] = (float) Math.atan((1 - 2f * a.x / e) * v[0]);
        n[0] = (float) Math.atan((1 - 2f * a.y / f) * v[1]);
        MathJPCT.phr2vec9_f(n, vec9tmp1);
        MathJPCT.vec9add(vec9tmp1, ViewportJPCT.vec9, vec9tmp2);
        MathJPCT.vec3mul1_fff(vec9tmp2, s, refPos);
        EnvironmentJPCT.setSunPH(-(float) (c * MathJPCT.deg2rad), (float) ((180 - b) * MathJPCT.deg2rad));
    }

    private void stickObj() {
        if (objPosMode == 0) return;
        if (objPosMode == 1) stickObjMode1();
        else stickObjMode2();
    }

    private void stickObjMode1() {
        float[] u = refPos, n = vec9tmp2, v = ViewportJPCT.fovTan;
        n[1] = (float) Math.atan(pos2ref[0] * v[0]);
        n[0] = (float) Math.atan(pos2ref[1] * v[1]);
        MathJPCT.phr2vec9_f(n, vec9tmp1);
        MathJPCT.vec9add(vec9tmp1, ViewportJPCT.vec9, n);
        if (n[2] >= 0) return;
        G3dJPCT.setObjPos(0, new float[]{u[2] * n[0] / n[2], u[2] * n[1] / n[2], u[2]}, null);
    }

    private void stickObjMode2() {
        float[] u = refPos, v = pos2ref;
        G3dJPCT.setObjPos(0, new float[]{u[0] + v[0], u[1] + v[1], u[2]}, null);
    }

    public void stick2screen(float a, float b) {
        objPosMode = 1;
        pos2ref[0] = 1 - 2 * a;
        pos2ref[1] = 2 * b - 1;
    }

    public void stick2plane(float a, float b) {
        float[] n = vec9tmp2, v = ViewportJPCT.fovTan;
        n[1] = (float) Math.atan((1 - 2 * a) * v[0]);
        n[0] = (float) Math.atan((2 * b - 1) * v[1]);
        MathJPCT.phr2vec9_f(n, vec9tmp1);
        MathJPCT.vec9add(vec9tmp1, ViewportJPCT.vec9, n);
        if (n[2] >= 0)
            return;
        objPosMode = 2;

        pos2ref[0] = refPos[2] * n[0] / n[2] - refPos[0];
        pos2ref[1] = refPos[2] * n[1] / n[2] - refPos[1];
    }

    public void replaceModel(Object3D a) {
        G3dJPCT.resetAll();

        if (a.getName() == "md2sample") {
            md2sample = a;
            G3dJPCT.addObj(2);
        } else if (a.getName() == "spacecraft1") {
            spacecraft1 = a;
            G3dJPCT.addObj(3);
        } else if (a.getName() == "plant01") {
            plant01 = a;
            G3dJPCT.addObj(4);
        } else {
            soccerball = a;
            G3dJPCT.addObj(1);
        }

        G3dJPCT.setObjVisible(0, true);

    }

}