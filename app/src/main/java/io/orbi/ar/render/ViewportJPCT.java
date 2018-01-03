package io.orbi.ar.render;
import com.threed.jpct.*;

public class ViewportJPCT {
	static Camera mainCam;
	static RendererJPCT.ScreenManager screenMgr;
	static SimpleVector[] spVec={new SimpleVector(),new SimpleVector(),new SimpleVector()};
	static double[] phr={0,0,0};

	static float[] vec9=new float[9],
		fovHalf={0,0,0},
		fovTan={0,0,0},
		fovSin={0,0,0},
		fovCos={0,0,0},
		nearFar={0.1f,200};

	static float maxFov=1, minFov=0.01f, maxPitch, farclip;

	static void gainFocus() { screenMgr=RendererJPCT.screenMgr; }
	static void init() {
		gainFocus();
		maxPitch=MathJPCT.maxPitch;
		farclip=nearFar[1];
		mainCam=RendererJPCT.mainWorld.getCamera();
		mainCam.setFOVLimits(minFov,maxFov);
		RendererJPCT.flatShdWorld.setCameraTo(mainCam);
		mainCam.setClippingPlanes(nearFar[0],farclip);
		setFovX(0.4f); setVec6(1,0,0,0,0,1); }

	static void setFovX(float a) {
		if (a==fovHalf[0]) return;
		double u,t;
		fovHalf[0]=a;
		t=Math.tan(a);
		u=t/screenMgr.aspect;
		fovTan[0]=(float)t;
		fovTan[1]=(float)u;
		fovHalf[1]=(float)Math.atan(u);
		setFovCommon(); }

	static void setFovY(float a) {
		if (a==fovHalf[1]) return;
		double u,t;
		fovHalf[1]=a;
		u=Math.tan(a);
		t=u*screenMgr.aspect;
		fovTan[0]=(float)t;
		fovTan[1]=(float)u;
		fovHalf[0]=(float)Math.atan(t);
		setFovCommon(); }

	private static void setFovCommon() {
		mainCam.setFOV(fovTan[0]*2);
		mainCam.adjustFovToNearPlane();
		fovSin[0]=(float)Math.sin(fovHalf[0]);
		fovSin[1]=(float)Math.sin(fovHalf[1]);
		fovCos[0]=(float)Math.cos(fovHalf[0]);
		fovCos[1]=(float)Math.cos(fovHalf[1]);
		fovTan[2]=(float)Math.sqrt(fovTan[0]*fovTan[0]+fovTan[1]*fovTan[1]);
		fovHalf[2]=(float)Math.atan(fovTan[2]);
		fovSin[2]=(float)Math.sin(fovHalf[2]);
		fovCos[2]=(float)Math.cos(fovHalf[2]);
	}

	private static void writeOrientation() {
		spVec[0].set(vec9[0],vec9[1],vec9[2]);
		spVec[2].set(vec9[6],vec9[7],vec9[8]);
		mainCam.setOrientation(spVec[0],spVec[2]); }

	static void setVec6(float a,float b,float c,float d,float e,float f) {
		vec9[0]=a; vec9[1]=b; vec9[2]=c; vec9[6]=d; vec9[7]=e; vec9[8]=f;
		MathJPCT.vec9phr_d(vec9,phr); MathJPCT.phr2vec9_d(phr,vec9);
		writeOrientation(); }


}