package test;
import java.nio.file.*;
import org.lwjgl.openal.AL10;
import org.lwjgl.glfw.GLFW;
public class Game {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static void state(Path p,String v)throws Exception{Files.write(p,v.getBytes("UTF-8"));Thread.sleep(160);}
 public static void main(String[] args)throws Exception{
  Path p=Paths.get(args[0]);
  state(p,"0");
  check(GLFW.glfwInit(),"glfw init");
  check(GLFW.glfwGetPrimaryMonitor()==7,"primary monitor retained");
  check(GLFW.glfwGetVideoMode(7)==null,"NULL vidmode retained for diagnosis");
  check(GLFW.glfwCreateWindow(10,10,"",0,0)==12,"window untouched");
  AL10.alSourcef(7,0x100A,.73f);AL10.alSourcePlay(7);
  check(Math.abs(AL10.sources.get(7)-.73)<.01,"foreground source volume");
  GLFW.glfwSwapBuffers(1);
  check(GLFW.swaps==1,"foreground swap");
  state(p,"1");
  AL10.alListener3f(1,1,1,1);
  check(AL10.sources.get(7)==0f,"old playing source muted on sound update");
  AL10.alSourcef(8,0x100A,.32f);AL10.alSourcePlay(8);
  check(AL10.sources.get(8)==0f,"new background source muted");
  GLFW.glfwSwapBuffers(1);
  check(GLFW.swaps==1,"background no swap");
  state(p,"0"); AL10.alListener3f(1,1,1,1);
  check(Math.abs(AL10.sources.get(7)-.73)<.01,"old source restored");
  check(Math.abs(AL10.sources.get(8)-.32)<.01,"new source restored");
  GLFW.glfwSwapBuffers(1);
  check(GLFW.swaps==2,"foreground restores presentation");
  System.out.println("PASS AFK source mute/restore & GLFW pass-through diagnostic");
 }
}