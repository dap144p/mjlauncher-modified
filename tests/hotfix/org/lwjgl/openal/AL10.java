package org.lwjgl.openal;
public class AL10 {
 public static java.util.Map<Integer,Float> sources=new java.util.concurrent.ConcurrentHashMap<>();
 public static float listener=1f;
 public static int plays;
 public static void alSourcef(int s,int p,float v) { if(p==0x100A) sources.put(s,v); }
 public static void alSourcePlay(int s) { plays++; }
 public static void alListenerf(int p,float v) { if(p==0x100A)listener=v; }
 public static void alListener3f(int p,float x,float y,float z) {}
}