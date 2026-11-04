import java.util.*;
import org.lwjgl.glfw.GLFW;
public final class cty {
 private final Map<Long, ctu> a = new HashMap<Long,ctu>();
 public cty(){ for(long h:GLFW.glfwGetMonitors()) a.put(h,new ctu()); GLFW.glfwSetMonitorCallback(null); }
 private void a(long monitor,int event){if(event==0x40001)a.put(monitor,new ctu());}
 public ctu a(long handle){return a.get(Long.valueOf(handle));}
}