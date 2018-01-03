package io.orbi.ar.render;
import com.threed.jpct.*;
import com.threed.jpct.util.*;

public class ShadowJPCT { static FrameBuffer fb;
	static TextureManager texMgr;
	static World mainWorld,flatShdWorld;
	static GLSLShader rttShader,flatShader;
	static SimpleVector[] spVec={new SimpleVector(),new SimpleVector(),new SimpleVector()};
	static SimpleVector spVecUpFlat=new SimpleVector(0,0,1),spVecForwFlat=new SimpleVector(), spVecUpCam=new SimpleVector(),spVecForwCam=new SimpleVector();
	static ShadowHelper shdHelp;
	static Projector proj=new Projector();
	static short texQty;
	static float[] sunVec,sunPH,flatUniform=new float[4],projPos=new float[3];
	static String[] glslSrc={"uniform vec4 u;",};

	static void gainFocus() { fb=RendererJPCT.fb; }

	static void init() { gainFocus(); texMgr=RendererJPCT.texMgr;
		mainWorld=RendererJPCT.mainWorld; flatShdWorld=RendererJPCT.flatShdWorld;
		initShader(); }

	static void setAmbientLight()
	{
		float[] m=EnvironmentJPCT.ambientRGB_f;
		flatUniform[0]=1-(m[0]+m[1]+m[2])/3;
		if (shdHelp!=null)
		{
			shdHelp.setAmbientLight(new RGBColor((int)(m[0]*255),(int)(m[1]*255),(int)(m[2]*255)));
		}
	}

	static void initShdHelper() { float[] m=EnvironmentJPCT.ambientRGB_f;
		proj.setClippingPlanes(100,1000); proj.setFOVLimits(0.0001f,1);
		shdHelp=new ShadowHelper(fb,proj,512,0.0001f); shdHelp.setAutoAdjustFov(true);
		shdHelp.setAmbientLight(new RGBColor((int)(m[0]*255),(int)(m[1]*255),(int)(m[2]*255)));
		shdHelp.setBorder(2); shdHelp.setFilterSize(1); shdHelp.setShadowMode(2);
		shdHelp.setCullingMode(true);
		G3dJPCT.initShdHelper(shdHelp);
	}

	static void initShader()
	{
		String v,f;
		float[] e=EnvironmentJPCT.ambientRGB_f;
		String[] x = RendererJPCT.glslCmn,y = RendererJPCT.glslV, z = RendererJPCT.glslF,w=glslSrc;

		rttShader=new GLSLShader(x[2]+y[0]+x[3]+y[1]+y[4]+"}",x[2]+x[3]+z[4]+"=vec4(0,0,0,1);}");

		v =x[0]+w[0]+x[4]+y[0]+y[2]+x[3]+y[3]+";"+y[5]+y[4]+"p.z*=u[3]/u[1];p.x*=u[3];"+ "if (p.y!=0.0) { p.x*=1.0+0.1*(1.0-u[1]); p.y=0.0; }"+y[1]+"p;}";
		f =x[0]+w[0]+x[4]+z[2]+x[3]+z[4]+"="+z[5]+";"+z[4]+".w*=u[0]*max(0.0,1.0-tc.y*u[2]*(1.0-u[1]));}";

		flatShader=new GLSLShader(v,f); flatShader.setUniform("u",flatUniform);
		flatShdWorld.setGlobalShader(flatShader);
		flatUniform[0]=1-(e[0]+e[1]+e[2])/3; flatUniform[2]=1.5f; flatUniform[3]=0.1f;
	}

	static void updateShdHelp()
	{
		if (shdHelp==null) initShdHelper();
		if (sunVec[2]>=0) return;

		proj.setFOV(0.0015f); proj.setYFOV(0.0015f);
		shdHelp.updateShadowMap(fb,mainWorld);
	}

	static void updSunShdVec()
	{
		flatUniform[1]=-sunVec[2];
		spVecForwFlat.set((float)Math.cos(sunPH[1]),(float)Math.sin(sunPH[1]),0);
		spVecForwCam.set(sunVec[0],sunVec[1],sunVec[2]);
		spVecUpCam.set(sunVec[6],sunVec[7],sunVec[8]);
		proj.setOrientation(spVecForwCam,spVecUpCam);
		if (G3dJPCT.visObjQty>0)
		{
			MathJPCT.vec3mul1_fff(sunVec,-500,projPos);
			MathJPCT.vec3add_fff(projPos,G3dJPCT.visObjs[0].pos,spVec[1]);
			proj.setPosition(spVec[1]);
		}
	}

	static class FlatShadow
	{
		World world;
		Camera cam;
		G3dJPCT.VisualObj visObj;
		Texture tex;
		Object3D shdFlat;
		float camDist,objRadius;
		float[] camOfs=new float[3];

		public FlatShadow(G3dJPCT.VisualObj object3d)
		{
			visObj = object3d;
			int[] n;
			float[] p,t;
			String s;
			float obRad = object3d.radius;//radius
			camDist=obRad*1000;
			world=new World();
			world.setGlobalShader(rttShader);
			cam=world.getCamera();
			cam.setFOVLimits(0.0001f,1);
			cam.setFOV(0.002f);
			cam.setClippingPlanes(1,1e6f);
			tex = new Texture(512,512);
			tex.setMipmap(false);
			tex.setClamping(true);
			s="shd"+Integer.toString(texQty); 
			texQty++; 
			texMgr.addTexture(s,tex);
			obRad *= 10;
			p=new float[]{obRad,0,obRad,obRad,0,-obRad,-obRad,0,-obRad,-obRad,0,obRad,obRad,1,obRad,obRad,0,-obRad,-obRad,0,-obRad,-obRad,0,obRad,obRad,0,obRad,obRad,0,-obRad,-obRad,0,-obRad,-obRad,1,obRad};
			t=new float[]{0,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1};
			shdFlat=new Object3D(p,t,new int[]{0,1,2,2,3,0,4,5,6,6,7,4,8,9,11,11,9,10},texMgr.getTextureID(s));
			shdFlat.getMesh().compress(); shdFlat.build(); shdFlat.strip();
			//todo: ---
			flatShdWorld.addObject(shdFlat);
			//flatShdWorld.removeAll();
			//
			shdFlat.setTransparency(255);//0-255
			shdFlat.setVisibility(false); spVec[0].set(0,0,0);
			shdFlat.setCenter(spVec[0]); shdFlat.setRotationPivot(spVec[0]);
		}
		void updCastVec()
		{
			shdFlat.setOrientation(spVecForwFlat,spVecUpFlat);
			cam.setOrientation(spVecForwCam,spVecUpCam);
			MathJPCT.vec3mul1_fff(sunVec,-camDist,camOfs);
		}
		void drawRtt()
		{
			fb.setRenderTarget(tex);
			RttJPCT.clearAlpha();
			world.renderScene(fb);
			world.draw(fb);
		}
		void update()
		{
			if (sunVec[2]>=0) return;

			float[] j=new float[3],k=visObj.pos;
			float h;
			h =- visObj.initAGL/sunVec[2];
			spVec[1].set(k[0]+sunVec[0]*h,k[1]+sunVec[1]*h,k[2]-visObj.initAGL);
			shdFlat.setOrigin(spVec[1]);
			updCastVec();
			MathJPCT.vec3add_fff(k,camOfs,spVec[1]); cam.setPosition(spVec[1]); drawRtt();
		}
		//added to rmeove the shadow element
		public void removeShadow()
		{

		}
	}
	public static void resetAll()
	{
		if(flatShdWorld != null)
		{
			flatShdWorld.removeAll();
		}
	}

}