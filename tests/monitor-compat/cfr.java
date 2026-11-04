import java.util.*;
import org.lwjgl.glfw.GLFW;
public final class cfr {
 private final Map<Long, cfj> b = new HashMap<Long,cfj>();
 public cfr(){ for(long h:GLFW.glfwGetMonitors()) b.put(h,new cfj()); GLFW.glfwSetMonitorCallback(null); }
 private void a(long monitor,int event){if(event==0x40001)b.put(monitor,new cfj());}
 public cfj a(long handle){return b.get(Long.valueOf(handle));}
}