package io.orbi.ar.render;
import com.threed.jpct.*;
import com.threed.jpct.util.*;

public class G3dJPCT {
	static World mainWorld,flatShdWorld;
	static TextureManager texMgr;
	static FrameBuffer fb;
	static ShadowHelper shdHelp;
	static RendererJPCT.ScreenManager screenMgr;
	static SimpleVector[] spVec={new SimpleVector(),new SimpleVector(),new SimpleVector()};
	static float[] sunVec;

	static int[] objTypeID={3,8,10,12,14};
	static float[] objHeight={0.3f,0.1f,0.15f,0.22f,0.2f};
	static float[] objRadius={0.4f,0.1f,0.3f,0.3f,0.3f};//last float allows more radius to shadow to fit into

	// an visual Object is like a tree, containing multiple visual parts which are linked together
	static short visObjQty;
	static VisualObj[] visObjs=new VisualObj[32];

	// this stores every unique mesh, and each mesh can be used on multiple visual objects
	// mesh has no bone animation
	static MeshType[] meshTypes=new MeshType[64];
	static int[] meshLoader={0,1,2,3,4,5,6};

	// mesh ID, alphaMesh actually means transparent mesh
	static int[] alphaMesh={};

	// whether use "add mode" transparency
	static byte[] alphaMeshAddMode={};

	// opacity range from 0 to 250
	static char[] meshAlpha={};

	//////////////////////////////////////////
	// self-lit mesh ID
	static int[] litMesh={};

	// self-lit intensity
	static char[][] litMeshRGB={{255,255,255},};

	// ID of source mesh, ID in "meshTypes"
	// a ModelPart is from a MeshType, or just a bone
	static int[] modelMeshType={0,1,2,-1,-1,-1,-1,3,-1,4, -1,5,-1,6,-1};

	// models with specular lighting
	static int[] specularModel={0,2,9,11};

	//////////////////////////////////////////
	// models with shadow model (which should cast shadow on ground)
	static int[] modelWithShadow={0,1,2,7,9,11,13};

	// shadow model ID (shadow models are simpler, -1 means use self)
	static int[] shadowModelType={-1,-1,-1,-1,-1,-1,-1};

	//////////////////////////////////////////
	// models as parent, ID in "modelMeshType" array
	static int[] parentModel={3,4,5,6,8,10,12,14};

	// children linkage for each parentModel, ID in the "linkedModelType"
	static int[][] childrenLink={{0,1,8,9},{3},{4,5,6,7},{2},{10},{11},{12},{13}};

	///////////////////////////////////////////
	// ID in "modelMeshType"
	static int[] linkedModelType={1,2,4,5,0,0,0,0,6,0, 7,9,11,13};

	// which link has pos offset
	static int[] linkWithPos={0,2,9,13};
	static float[][] linkPos={{0,0,-0.3f},{0.1f,0,0},{0,0,0.1f},{0,0,-0.2f}};

	// which link has angle offset
	static int[] linkWithPhr={2,4,5,6,7,8,3,9,11,12};
	static float[][] linkPhr={{-1.57f,0,0},{0,0.785f,0.2f},{0,2.36f,0.2f},{0,-0.785f,0.2f},{0,-2.36f,0.2f},
		{0,0,0},{0,0,0},{-1.57f,0,0},{3.1416f,0,0},{3.1416f,0,0}};

	// which linked model need scaling, usually not necessary
	static int[] linkWithScale={9,10,11,12,13};
	static float[] linkScale={1.6f,0.00158f,0.005f,0.008f,0.06f};

	// which link has animation
	static int[] linkWithAni={8,3,11,12};
	static short[] linkAni={0,1,2,3};

	static void gainFocus() { fb=RendererJPCT.fb; screenMgr=RendererJPCT.screenMgr; }

	//----------------------
	//init method
	//------------------------
	static void init()
	{
		gainFocus(); texMgr=RendererJPCT.texMgr;

		mainWorld = RendererJPCT.mainWorld;

		flatShdWorld = RendererJPCT.flatShdWorld;
	}

	static void initShdHelper(ShadowHelper a) {
		shdHelp=a; for (int i=0;i<visObjQty;i++) visObjs[i].add2shdHelp(); }

	static void draw()
	{
		if (sunVec[2]<0)
		{
			flatShdWorld.renderScene(fb);
			flatShdWorld.draw(fb);
		}
		mainWorld.renderScene(fb);
		mainWorld.draw(fb);
		}

	static void update()
	{
		VisualObj j;
		for (short i=0;i<visObjQty;i++) {
			j=visObjs[i];
			if (j.hidden) continue;
			j.pos[3]=MathJPCT.vec3len_f(j.pos);
			j.modelPart.update(); j.shd.update();
		}
	}

	static void showFlatShadow(boolean a) {
		for (int i=0;i<visObjQty;i++) {
			if (visObjs[i].hidden) continue;
			visObjs[i].shd.shdFlat.setVisibility(a); }
	}

	static int addObj(int a)
	{
		if ( a >= objTypeID.length) return -1;
		visObjs[visObjQty]=new VisualObj(a);
		visObjQty++;
		return visObjQty-1;
	}

	static void setObjVisible(int a,boolean b) { if (a<visObjQty) visObjs[a].show(b); }

	static void setObjPHR(int a,double[] b) { if (a<visObjQty) visObjs[a].setPHR(b); }

	static void setObjPos(int a,float[] b,double[] c) {
		if (a>=visObjQty) return;
		if (b!=null) visObjs[a].setPos(b);
		if (c!=null) visObjs[a].setPHR(c); }

	static void setObjScale(int a,float b) { if (a<visObjQty) visObjs[a].setScale(b); }

	static class VisualObj { boolean hidden=true, updatePos, updatePhr;
		int modelType; float[] pos=new float[5], vec9=new float[9];
		double[] phr=new double[3]; float initAGL,radius;
		ModelPart modelPart; ShadowJPCT.FlatShadow shd;

		public VisualObj(int a) {
			modelType=objTypeID[a]; initAGL=objHeight[a]; radius=objRadius[a];
			MathJPCT.phr2vec9_d(phr,vec9);
			shd=new ShadowJPCT.FlatShadow(this);
			modelPart=new ModelPart(modelType,null,-1,pos,vec9,this); }

		void setPos(float[] a) { pos[0]=a[0]; pos[1]=a[1]; pos[2]=a[2]; }
		void setPHR(double[] a) {
			System.arraycopy(a,0,phr,0,3); MathJPCT.phr2vec9_d(phr,vec9); }
		void setScale(float a) {}
		void add2shdHelp() { modelPart.add2shdHelp(); }
		void show(boolean a) {
			if (hidden!=a) return; hidden=!hidden;
			if (hidden) { modelPart.hide(); shd.shdFlat.setVisibility(false); }
			else { modelPart.show(); if (sunVec[2]<0) shd.shdFlat.setVisibility(true); } }
	}

	// mesh coordinate system: left up forward
	// RendererJPCT package coordinate system: forward left up
	static class MeshType {
		boolean hasNormal=true, hasUV;
		int meshID;
		Object3D obj3d;
		Mesh mesh;
		public MeshType(int a) {
			int e;
			char[] w;
			meshID=a;
			obj3d=AssetJPCT.loadMesh(a);
			if (obj3d==null) return;
			mesh=obj3d.getMesh();
			mesh.compress();
			mesh.setLocked(true);
			e=MathJPCT.findInt(litMesh,a);
			if (e>=0) {
				w=litMeshRGB[e];
				obj3d.setAdditionalColor(w[0],w[1],w[2]);
			}
			e=MathJPCT.findInt(alphaMesh,a);
			if (e>=0) {
				obj3d.setTransparency(meshAlpha[e]);
				obj3d.setTransparencyMode(alphaMeshAddMode[e]);
			}
		}
	}

	static class ModelPart {
		ModelPart parent, rootModel;
		ModelPart[] children;
		VisualObj visObj;
		boolean isBone, hasShadow, hasModel, hidden=true, detach, inheritPos, inheritPhr;
		MeshType meshType;
		ModelAni modelAni;
		float[] initPos, initPhr, vec9lo, vec9wo, posWo, posLo, phrLo, posWo2parent;
		int modelType, aniID=-1, linkID;
		Object3D obj3d, shadowObj3d;
		float initScale=1;
		public ModelPart(int a, ModelPart b, int c, float[] d, float[] e,VisualObj f) {
			byte n;
			float[] p,v;
			int s,x;
			int[] h=null;
			modelType=a;
			parent=b;
			linkID=c;
			visObj=f;
			rootModel=(b==null)?this:b.rootModel;
			if (d!=null) {
				posWo=d;
				inheritPos=true;
			}
			else {
				initPos=linkPos[MathJPCT.findInt(linkWithPos,linkID)];
				posWo=new float[3];
				posWo2parent=new float[3];
				posLo=new float[3];
				System.arraycopy(initPos,0,posLo,0,3);
			}
			if (e!=null) {
				vec9wo=e;
				inheritPhr=true;
			}
			else {
				initPhr=linkPhr[MathJPCT.findInt(linkWithPhr,linkID)];
				vec9wo=new float[9];
				vec9lo=new float[9];
				MathJPCT.phr2vec9_f(initPhr,vec9lo);
			}
			s=modelMeshType[modelType];
			isBone=(s<0);
			x=MathJPCT.findInt(parentModel,modelType);
			if (x>=0) h=childrenLink[x];
			x=MathJPCT.findInt(linkWithAni,linkID);
			if (x>=0) aniID=linkAni[x];
			if (!isBone)
			{
				if (meshTypes[s]==null) meshTypes[s]=new MeshType(s);
				meshType=meshTypes[s];
				obj3d=new Object3D(meshType.obj3d,true);
				obj3d.setSpecularLighting((MathJPCT.findInt(specularModel,a)>=0));
				obj3d.setLighting((meshType.hasNormal)?0:1);
				obj3d.build();
				obj3d.strip();
				spVec[1].set(0,0,0);
				obj3d.setCenter(spVec[1]);
				obj3d.setRotationPivot(spVec[1]);

				mainWorld.addObject(obj3d);
				//todo:------------------
				//mainWorld.removeAll();
				//-----------------------
//				shdHelp.addCaster(obj3d);
//				shdHelp.addReceiver(obj3d);
				obj3d.setVisibility(false);
				x=MathJPCT.findInt(linkWithScale,linkID);
				if (x>=0) {
					initScale=linkScale[x];
					obj3d.scale(initScale);
				}
			}
			x=MathJPCT.findInt(modelWithShadow,modelType);
			if (x>=0) {
				x=shadowModelType[x];
				if (x<0) {
					if (!isBone) shadowObj3d=new Object3D(obj3d,true);
				}
				else {
					s=modelMeshType[x];
					if (meshTypes[s]==null) meshTypes[s]=new MeshType(s);
					shadowObj3d=new Object3D(meshTypes[s].obj3d,true);
				}
			}
			if (shadowObj3d!=null) {
				shadowObj3d.build();
				shadowObj3d.strip();
				spVec[2].set(0,0,0);
				shadowObj3d.setCenter(spVec[2]);
				shadowObj3d.setRotationPivot(spVec[2]);
				shadowObj3d.setVisibility(false);
				hasShadow=true;
				visObj.shd.world.addObject(shadowObj3d);
			}
			hasModel=(hasShadow||(!isBone));
			if (h!=null) {
				children=new ModelPart[h.length];
				for (n=0; n<h.length; n++) {
					x=h[n];
					p=(MathJPCT.findInt(linkWithPos,x)<0)?posWo:null;
					v=(MathJPCT.findInt(linkWithPhr,x)<0)?vec9wo:null;
					children[n]=new ModelPart(linkedModelType[x],this,x,p,v,visObj);
				}
			}
			if (aniID>=0) initAni();
		}
		void initAni() { switch(aniID) {
				case 0: modelAni=new FanTurnAni(this); break;
				case 1: modelAni=new RotorHubAni(this); break;
				case 2: modelAni=new MD2AniSample(this); break;
				case 3: modelAni=new Spacecraft1Ani(this); break;
				default: break; } }
		void writePos() {
			spVec[1].set(posWo[0],posWo[1],posWo[2]);
			if (!isBone) obj3d.setOrigin(spVec[1]);
			if (hasShadow) shadowObj3d.setOrigin(spVec[1]);
		}
		void writePhr() {
			spVec[0].set(vec9wo[0],vec9wo[1],vec9wo[2]);
			spVec[2].set(vec9wo[6],vec9wo[7],vec9wo[8]);
			if (!isBone) obj3d.setOrientation(spVec[0],spVec[2]);
			if (hasShadow) shadowObj3d.setOrientation(spVec[0],spVec[2]);
		}
		void hide() {
			if (hidden) return;
			hidden=true;
			if (!isBone) obj3d.setVisibility(false);
			if (hasShadow) shadowObj3d.setVisibility(false);
			if (children!=null) {
				for (short n=0;n<children.length;n++) children[n].hide();
			}
		}
		void show() {
			if ((!hidden)||detach) return;
			hidden=false;
			if (!isBone) obj3d.setVisibility(true);
			if (hasShadow) shadowObj3d.setVisibility(true);
			if (children!=null) {
				for (byte n=0;n<children.length;n++) children[n].show();
			}
		}
		void detach() { if (detach) return; detach=true; hide(); }
		void attach() { if (!detach) return; detach=false; show(); }
		void add2shdHelp() {
			if (!isBone) { shdHelp.addCaster(obj3d); shdHelp.addReceiver(obj3d); }
			if (children!=null) { for (byte n=0;n<children.length;n++) children[n].add2shdHelp(); } }
		void update() {
			if (hidden) return;
			if (aniID>=0) modelAni.update();
			if (!inheritPos) {
				MathJPCT.lo2wo_f(posLo, parent.vec9wo, posWo2parent);
				MathJPCT.vec3add_fff(parent.posWo, posWo2parent, posWo); }
			if (!inheritPhr) MathJPCT.vec9add(vec9lo, parent.vec9wo, vec9wo);
			if (children!=null) {
				for (short n=0;n<children.length;n++) children[n].update(); }
			if (hasModel) { writePos(); writePhr(); }
		}
	}

	interface ModelAni { void update(); }

	static class RotorHubAni implements ModelAni {
		ModelPart modelPart; double rotatePhase;
		public RotorHubAni(ModelPart a) { modelPart=a; }
		public void update() {
			rotatePhase=MathJPCT.cycle_d(rotatePhase-0.03,MathJPCT.pi);
			float[] v=modelPart.vec9lo; v[1]=(float)Math.sin(rotatePhase);
			v[0]=(float)Math.cos(rotatePhase); v[3]=-v[1]; v[4]=v[0]; } }

	static class FanTurnAni implements ModelAni {
		ModelPart modelPart; float yaw; boolean yawLeft;
		public FanTurnAni(ModelPart a) { modelPart=a; }
		public void update() {
			if (yawLeft) { yaw+=0.005f; if (yaw>1) yawLeft=false; }
			else { yaw-=0.005f; if (yaw<-1) yawLeft=true; }
			float[] v=modelPart.vec9lo; v[1]=(float)Math.sin(yaw);
			v[0]=(float)Math.cos(yaw); v[3]=-v[1]; v[4]=v[0]; } }

	static class MD2AniSample implements ModelAni {
		ModelPart modelPart; float ani;
		public MD2AniSample(ModelPart a) { modelPart=a; }
		public void update() { ani+=0.0005f; if (ani>=1) ani=0;
			modelPart.obj3d.animate(ani);
			if (modelPart.hasShadow) modelPart.shadowObj3d.animate(ani); } }

	static class Spacecraft1Ani implements ModelAni {
		ModelPart modelPart; float ani=0.63f;
		public Spacecraft1Ani(ModelPart a) { modelPart=a; }
		public void update() { ani+=0.0003f; if (ani>=0.83f) ani=0.63f;
			modelPart.obj3d.animate(ani);
			if (modelPart.hasShadow) modelPart.shadowObj3d.animate(ani); } }

	//---------------------
	//Reset the model and shadow
	//---------------------
	public static void resetAll()
	{
		if(mainWorld!= null)
		{
			mainWorld.removeAll();
			ShadowJPCT.resetAll();
		}

	}
	static int addNewObj(int a)
	{
		visObjs=new VisualObj[32];

		if ( a >= objTypeID.length) return -1;
		visObjs[visObjQty]=new VisualObj(a);
		visObjQty++;
		return visObjQty-1;
	}

}