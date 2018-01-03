package io.orbi.ar.render;
import com.threed.jpct.*;
import java.lang.reflect.Array;
import java.util.Arrays;

// this module contains common methods and some other common things

public class MathJPCT {
	static double pi=Math.PI, deg2rad, rad2deg, pi2, halfpi;
	static float maxPitch=1.569f,pi_f;

	static int[] colorInt={0,0xffffff,0xff0000,0x00ff00,0x0000ff,
		0xffff00,0xff00ff,0x00ffff,0xff6400,0xff0064,
		0x90909c,0x252525,0x202020,0x999999,0x305030,
		0xcc0000,0xeefff8,0x4a4a4c,0xbbbbbb,0x606060,};
	static Texture[] colorTex;
	static String[] colorTexName;

	static void init() {
		int o;
		int[] g=new int[3];
		deg2rad=pi/180;
		rad2deg=180/pi;
		pi2=pi*2;
		halfpi=pi/2;
		pi_f=(float)pi;

		colorTex=new Texture[colorInt.length];
		colorTexName=new String[colorInt.length];
/*		for (o=0;o<colorInt.length;o++) {
			int2rgb(colorInt[o],g);
			colorTex[o]=new Texture(8,8,new RGBColor(g[0],g[1],g[2]));
			colorTexName[o]="color"+Integer.toString(o);
			colorTex[o].setFiltering(false);
			colorTex[o].setMipmap(false);
			RendererJPCT.texMgr.addTexture(colorTexName[o],colorTex[o]);
		}
		RendererJPCT.texMgr.setDummyTexture(colorTex[6]);
*/
	}

	static void vec3mul1_ff(float[] a,float b) { a[0]*=b; a[1]*=b; a[2]*=b; }

	static void vec3mul1_fff(float[] a,float b,float[] c) {
		c[0]=a[0]*b; c[1]=a[1]*b; c[2]=a[2]*b; }

	static void vec3mul3_fff(float[] a,float[] b,float[] c) {
		c[0]=a[0]*b[0]; c[1]=a[1]*b[1]; c[2]=a[2]*b[2]; }

	static void vec3div1_fff(float[] a,float b,float[] c) {
		c[0]=a[0]/b; c[1]=a[1]/b; c[2]=a[2]/b; }

	static void vec3sub_ff(float[] a,float[] b) { a[0]-=b[0]; a[1]-=b[1]; a[2]-=b[2]; }

	static void vec3sub_fff(float[] a,float[] b,float[] c) {
		c[0]=a[0]-b[0]; c[1]=a[1]-b[1]; c[2]=a[2]-b[2]; }

	static void vec3add_ff(float[] a,float[] b) { a[0]+=b[0]; a[1]+=b[1]; a[2]+=b[2]; }

	static void vec3add_fff(float[] a,float[] b,SimpleVector c) {
		c.set(a[0]+b[0],a[1]+b[1],a[2]+b[2]); }

	static void vec3add_fff(float[] a,float[] b,float[] c) {
		c[0]=a[0]+b[0]; c[1]=a[1]+b[1]; c[2]=a[2]+b[2]; }

	static void vec1div3_fff(float a,float[] b,float[] c) {
		c[0]=a/b[0]; c[1]=a/b[1]; c[2]=a/b[2]; }

	static double vec3len_d(float[] a) {
		return Math.sqrt((double)a[0]*a[0]+(double)a[1]*a[1]+(double)a[2]*a[2]); }

	static float vec3len_f(float[] a) {
		return (float)Math.sqrt((double)a[0]*a[0]+(double)a[1]*a[1]+(double)a[2]*a[2]); }

	static float vec3len2_f(float[] a) {
		return (float)((double)a[0]*a[0]+(double)a[1]*a[1]+(double)a[2]*a[2]); }

	static float vec2len_f(float[] a) {
		return (float)Math.sqrt((double)a[0]*a[0]+(double)a[1]*a[1]); }

	static void vec3normalize(float[] a) {
		double g=vec3len_d(a);
		if (g>0) { g=1/g; a[0]*=g; a[1]*=g; a[2]*=g; } }

	static float clamp_f(float a,float b,float c) {
		if (a>c) return c; if (a<b) return b; return a; }

	static double clamp_d(double a,double b,double c) {
		if (a>c) return c; if (a<b) return b; return a; }

	static float dotProduct3_ff(float[] a,float[] b) {
		return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }

	static float cycle_f(float a,float b) { float q=b*2;
		while (a>b) a-=q; while (a<-b) a+=q; return a; }

	static double cycle_d(double a,double b) { double q=b*2;
		while (a>b) a-=q; while (a<-b) a+=q; return a; }

	static byte vec3ph_d(float[] a,double[] b) {
		double m=a[0]*a[0]+a[1]*a[1],n;
		n=m+a[2]*a[2];
		if (n==0) return 0;
		b[0]=Math.asin(a[2]/Math.sqrt(n));
		if (m==0) return 1;
		b[1]=Math.atan2(a[1],a[0]);
		return 2;
	}

	static byte vec3ph_f(float[] a,float[] b) {
		double m=a[0]*a[0]+a[1]*a[1],n;
		n=m+a[2]*a[2];
		if (n==0) return 0;
		b[0]=(float)Math.asin(a[2]/Math.sqrt(n));
		if (m==0) return 1;
		b[1]=(float)Math.atan2(a[1],a[0]);
		return 2;
	}

	static void ph2vec3_ff(float[] a,float[] b) {
		double g=Math.cos(a[0]); b[0]=(float)(g*Math.cos(a[1]));
		b[1]=(float)(g*Math.sin(a[1])); b[2]=(float)Math.sin(a[0]); }

	static void ph2vec3_df(double[] a,float[] b) {
		double g=Math.cos(a[0]); b[0]=(float)(g*Math.cos(a[1]));
		b[1]=(float)(g*Math.sin(a[1])); b[2]=(float)Math.sin(a[0]); }

	static void phr2vec9_d(double[] a,float[] b) {
		double i,j,k,l,m,n,p,q,r,s; i=Math.sin(a[0]); j=Math.cos(a[0]);
		k=Math.sin(a[1]); l=Math.cos(a[1]); m=Math.sin(a[2]);
		n=Math.cos(a[2]); p=k*m; q=k*n; r=l*m; s=l*n;
		b[0]=(float)(l*j); b[1]=(float)(k*j); b[2]=(float)i;
		b[3]=-(float)(i*r+q); b[4]=(float)(s-i*p); b[5]=(float)(j*m);
		b[6]=(float)(p-i*s); b[7]=-(float)(i*q+r); b[8]=(float)(j*n); }

	static void phr2vec9_f(float[] a,float[] b) {
		double i,j,k,l,m,n,p,q,r,s; i=Math.sin(a[0]); j=Math.cos(a[0]);
		k=Math.sin(a[1]); l=Math.cos(a[1]); m=Math.sin(a[2]);
		n=Math.cos(a[2]); p=k*m; q=k*n; r=l*m; s=l*n;
		b[0]=(float)(l*j); b[1]=(float)(k*j); b[2]=(float)i;
		b[3]=-(float)(i*r+q); b[4]=(float)(s-i*p); b[5]=(float)(j*m);
		b[6]=(float)(p-i*s); b[7]=-(float)(i*q+r); b[8]=(float)(j*n); }

	static void vec9phr_d(float[] a,double[] b) {
		double s,c,m=-a[7],n=a[6],x=1-a[2]*a[2];
		b[1]=0; vec3ph_d(a,b); s=Math.sin(b[1]); c=Math.cos(b[1]);
		if (x>0) { n=a[8]/Math.sqrt(x);
			if (Math.abs(s)>0.7) m=(a[6]+a[2]*c*n)/s;
			else if (s!=0) m=(m-s*a[2]*n)/c; }
		b[2]=Math.atan2(m,n); }

// a=source b=rotation vector c=output
	static void wo2lo2D_f(float[] a,float[] b,float[] c) {
		c[0]=a[0]*b[0]+a[1]*b[1]; c[1]=a[1]*b[0]-a[0]*b[1]; }

	static void lo2wo2D_f(float[] a,float[] b,float[] c) {
		c[0]=a[0]*b[0]-a[1]*b[1]; c[1]=a[1]*b[0]+a[0]*b[1]; }

	static void lo2wo_f(float[] a,float[] b,float[] c) {
		c[0]=b[6]*a[2]+b[3]*a[1]+b[0]*a[0]; c[1]=b[7]*a[2]+b[4]*a[1]+b[1]*a[0];
		c[2]=b[8]*a[2]+b[5]*a[1]+b[2]*a[0]; }

	static void wo2lo_f(float[] a,float[] b,float[] c) {
		c[0]=a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; c[1]=a[0]*b[3]+a[1]*b[4]+a[2]*b[5];
		c[2]=a[0]*b[6]+a[1]*b[7]+a[2]*b[8]; }

	static void vec9add(float[] a,float[] b,float[] c) {
		c[0]=b[6]*a[2]+b[3]*a[1]+b[0]*a[0]; c[1]=b[7]*a[2]+b[4]*a[1]+b[1]*a[0];
		c[2]=b[8]*a[2]+b[5]*a[1]+b[2]*a[0]; c[3]=b[6]*a[5]+b[3]*a[4]+b[0]*a[3];
		c[4]=b[7]*a[5]+b[4]*a[4]+b[1]*a[3]; c[5]=b[8]*a[5]+b[5]*a[4]+b[2]*a[3];
		c[6]=b[6]*a[8]+b[3]*a[7]+b[0]*a[6]; c[7]=b[7]*a[8]+b[4]*a[7]+b[1]*a[6];
		c[8]=b[8]*a[8]+b[5]*a[7]+b[2]*a[6]; }

	static String id2fileName(int a,int b) {
		String q=Integer.toString(a);
		q=new String(new char[8-q.length()]).replace("\0","0")+q;
		if (b<0) return q;
		return q+"."+RendererJPCT.fileType[b];
	}

	static byte findChar(char[] a,char b) {
		for (byte h=0;h<a.length;h++) { if (a[h]==b) return h; } return -1; }

	static byte findByte(byte[] a,byte b) {
		for (byte h=0;h<a.length;h++) { if (a[h]==b) return h; } return -1; }

	static byte findByte(byte[] a,byte b,byte c) {
		for (byte h=0;h<c;h++) { if (a[h]==b) return h; } return -1; }

	static short findShort(short[] a,short b) {
		for (short h=0;h<a.length;h++) { if (a[h]==b) return h; } return -1; }

	static int findInt(int[] a,int b) {
		for (int h=0;h<a.length;h++) { if (a[h]==b) return h; } return -1; }

	static int findInt(int[] a,int b,int c) {
		for (int h=0;h<c;h++) { if (a[h]==b) return h; } return -1; }

	static void int2rgb(int a,int[] b) {
		b[0]=a/65536; b[2]=a%65536; b[1]=b[2]/256; b[2]=b[2]%256; }

	static Object arrayDelItem(Object a,int b) {
		int x=Array.getLength(a)-1;
		Object c=Array.newInstance(a.getClass().getComponentType(),x);
		System.arraycopy(a,0,c,0,b);
		System.arraycopy(a,b+1,c,b,x-b);
		return c;
	}

}