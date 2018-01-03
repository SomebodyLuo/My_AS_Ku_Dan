package io.orbi.ar.render;
import com.threed.jpct.*;

// this module manages lights, fog, etc...

public class EnvironmentJPCT {
	static FrameBuffer fb;
	static RendererJPCT.ScreenManager screenMgr;
	static World mainWorld;

	// in case there are other Worlds on screen, there may be other suns and moons
	static Light[] sun=new Light[1];
	static Light[] moon=new Light[1];

	static int lightQty;
	static MyLight[] lights=new MyLight[8];

	static SimpleVector[] spVec={new SimpleVector(),new SimpleVector(),new SimpleVector()};

	// 1e8f is like infinite
	static float sunDist=1e8f;

	// RGB color stored as float values
	static float[] ambientRGB_f={0,0,0},sunRGB_f={1,1,1};

	// PHR is pitch-heading-roll
	// roll is not needed for light, but needed for calcuation of vector9 (like matrix)
	static float[] sunPHR={-0.6f,0.7f,0};
	static float[] sunVec=new float[9], sunPos={0,0,0};

	static class MyLight { boolean far,enable=true; Light lightMain;
		float[] color_f=new float[3],phr=new float[3],vec=new float[9],pos=new float[3];
		public MyLight(boolean a) { far=a; lightMain=new Light(mainWorld);
			if (a) { lightMain.setAttenuation(-1); lightMain.setDiscardDistance(-1);
				System.arraycopy(sunPHR,0,phr,0,3); System.arraycopy(sunVec,0,vec,0,9); setPos(); }
			lightMain.setIntensity(0,0,0);
		}
		void setPH(float p,float h) { if (!far) return;
			phr[0]=p; phr[1]=h; MathJPCT.phr2vec9_f(phr,vec); setPos(); }
		void setPos() { if (!far) return;
			MathJPCT.vec3mul1_fff(vec,-sunDist,pos); spVec[1].set(pos[0],pos[1],pos[2]);
			lightMain.setPosition(spVec[1]); }
		void setColor(float a,float b,float c) { color_f[0]=a; color_f[1]=b; color_f[2]=c;
			lightMain.setIntensity(a*255,b*255,c*255); }
		
	}

	static void gainFocus() { fb=RendererJPCT.fb; screenMgr=RendererJPCT.screenMgr; }

	static void init() {
		gainFocus();
		mainWorld=RendererJPCT.mainWorld;
		ShadowJPCT.sunVec=sunVec; ShadowJPCT.sunPH=sunPHR; G3dJPCT.sunVec=sunVec;
		setAmbientRGB(0.2f,0.2f,0.2f); initSun();
	}

	static void initSun() {
		sun[0]=new Light(mainWorld);
		sun[0].setAttenuation(-1);
		sun[0].setDiscardDistance(-1);
		setSunRGB(1,1,1); setSunPHR(); }

	static void enableSun(boolean a) {}
//	static void enableSun(boolean a) { if (a) sun[0].enable(); else sun[0].disable(); }

	private static void togSun() {
		if (sunPHR[0]<0) { G3dJPCT.showFlatShadow(true); sun[0].enable(); }
		else { G3dJPCT.showFlatShadow(false); sun[0].disable(); }
	}

	private static void setSunPos() {
		MathJPCT.vec3mul1_fff(sunVec,-sunDist,sunPos);
		spVec[1].set(sunPos[0],sunPos[1],sunPos[2]);
		sun[0].setPosition(spVec[1]);
		ShadowJPCT.updSunShdVec(); }

	private static void setSunPHR() { MathJPCT.phr2vec9_f(sunPHR,sunVec); setSunPos(); }

	static void setSunPH(float a,float b) {
		float p=sunPHR[0]; sunPHR[0]=a; sunPHR[1]=b; setSunPHR();
		if (p*a<=0) togSun(); }

	static void setSunVec(float a,float b,float c) {
		sunPHR[1]=0; sunVec[0]=a; sunVec[1]=b; sunVec[2]=c;
		MathJPCT.vec3ph_f(sunVec,sunPHR); setSunPHR(); }

	static void setSunRGB(float a,float b,float c) {
		float[] i=sunRGB_f; i[0]=a; i[1]=b; i[2]=c;
		sun[0].setIntensity(i[0]*255,i[1]*255,i[2]*255); }

	static void setAmbientRGB(float a,float b,float c) {
		float[] m=ambientRGB_f; m[0]=a; m[1]=b; m[2]=c;
		int x=(int)(m[0]*255),y=(int)(m[1]*255),z=(int)(m[2]*255);
		mainWorld.setAmbientLight(x,y,z); ShadowJPCT.setAmbientLight(); }

	static int addLight(boolean a) { if (lightQty>7) return -1;
		lights[lightQty]=new MyLight(a); lightQty++; return lightQty-1; }

	static void setLightColor(int a,float b,float c,float d) {
		if (a<lightQty) lights[a].setColor(b,c,d); }

	static void setLightPH(int a,float p,float h) { if ((a>=lightQty)||(!lights[a].far)) return;
		lights[a].setPH(p,h); }

}