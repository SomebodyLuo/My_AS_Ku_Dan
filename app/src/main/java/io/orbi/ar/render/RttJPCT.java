package io.orbi.ar.render;
import com.threed.jpct.*;

// rtt means Render-To-Texture, it's for shadow or other effects

public class RttJPCT {
	static TextureManager texMgr;
	static FrameBuffer fb;
	static RendererJPCT.ScreenManager screenMgr;
	static World clearAlphaWorld=new World();
	static int[] rttTexID={};
	static short[][] rttTexSize={};

	static void gainFocus() {
		fb=RendererJPCT.fb;
		screenMgr=RendererJPCT.screenMgr;
		renewRtt();
	}

	static void init() {
		int v,p; short[] z;
		gainFocus();
		texMgr=RendererJPCT.texMgr;
		for (v=0;v<rttTexID.length;v++) {
			p=rttTexID[v];
			RendererJPCT.texName[p]=MathJPCT.id2fileName(p,0);
			z=rttTexSize[v];
			RendererJPCT.tex[p]=new Texture(z[0],z[1]);
			RendererJPCT.tex[p].setMipmap(false);
			texMgr.addTexture(RendererJPCT.texName[p],RendererJPCT.tex[p]);
		}
		initClearAlpha();
	}

	static void renewRtt() {}

	static void clearAlpha() {
		clearAlphaWorld.renderScene(fb);
		clearAlphaWorld.draw(fb);
	}
	static void initClearAlpha() {
		World w=clearAlphaWorld;
		float[] p=new float[]{0,-9,2,-9,9,2,9,9,2};
		Object3D j=new Object3D(p,null,new int[]{0,1,2},-1);
		j.build(); j.strip(); w.addObject(j);
		w.getCamera().setFOV(0);
		String[] n=RendererJPCT.glslCmn,v=RendererJPCT.glslV,f=RendererJPCT.glslF;
		String g=n[2]+n[3]+f[4]+"=vec4(0,0,0,0);}";
		j.setShader(new GLSLShader(n[2]+v[0]+n[3]+v[1]+v[4]+"}",g));
	}


}