package org.lwjgl.glfw;
public class GLFW {
 public static int swaps;
 public static boolean glfwInit(){return true;}
 public static long glfwGetPrimaryMonitor(){return 7;}
 public static GLFWVidMode glfwGetVideoMode(long m){return null;}
 public static long glfwCreateWindow(int w,int h,java.lang.CharSequence t,long m,long s){return 12;}
 public static void glfwSwapBuffers(long w){swaps++;}
}
