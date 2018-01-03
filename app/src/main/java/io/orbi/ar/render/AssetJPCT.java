package io.orbi.ar.render;
import com.threed.jpct.*;
import android.content.Context;

// this module loads assets from android resources

public class AssetJPCT {

	static Context context;
	static TextureManager texMgr;

	static Object3D loadMesh(int a) {
		switch (a) {
			case 0: return createMesh0(); case 1: return createMesh1();
			case 2: return createMesh2(); case 3: return loadBall();
			case 4: return loadMD2(); case 5: return loadSpacecraft1();
			case 6: return loadPlant01();
			default: return null; }
	}

	private static Object3D createMesh0() {
		float[] p,n;
		int[] i=new int[]{0,1,2,2,3,0,4,5,6,6,7,4};
		n=new float[]{0,1,0,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0};
		p=new float[]{-0.02f,0,0.03f,-0.04f,0,0.25f,0.04f,0,0.25f,0.02f,0,0.03f,
			0.02f,0,0.03f,0.04f,0,0.25f,-0.04f,0,0.25f,-0.02f,0,0.03f};
		return new Object3D(p,n,null,i,-1); }

	private static Object3D createMesh1() {
		float[] p,n;
		int[] i=new int[]{0,1,2,2,3,0};
		n=new float[]{0,1,0,0,1,0,0,1,0,0,1,0};
		p=new float[]{-0.2f,0,-0.2f,-0.2f,0,0.2f,0.2f,0,0.2f,0.2f,0,-0.2f};
		return new Object3D(p,n,null,i,-1); }

	private static Object3D createMesh2() {
		Object3D j=Primitives.getSphere(0.1f);
//		j.setTexture(MathJPCT.colorTexName[13]);
		return j; }

	private static Object3D loadBall() { return RendererJPCT.soccerball; }
	private static Object3D loadMD2() { return RendererJPCT.md2sample; }
	private static Object3D loadSpacecraft1() { return RendererJPCT.spacecraft1; }
	private static Object3D loadPlant01() { return RendererJPCT.plant01; }

}