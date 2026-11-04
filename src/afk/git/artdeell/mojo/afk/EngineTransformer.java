package git.artdeell.mojo.afk;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Changes call sites, including native LWJGL entry points, without replacing LWJGL. */
public final class EngineTransformer implements ClassFileTransformer {
    private static final String BRIDGE = "git/artdeell/mojo/afk/Bridge";
    public byte[] transform(ClassLoader loader, String name, Class<?> type,
                            ProtectionDomain domain, byte[] input) {
        if (name == null || name.startsWith("java/") || name.startsWith("jdk/") ||
            name.startsWith("sun/") || name.startsWith("org/lwjgl/") ||
            name.startsWith("git/artdeell/mojo/afk/")) return null;
        // Latin-1 preserves ASCII constant-pool names, without parsing unrelated classes.
        String constants;
        try { constants = new String(input, "ISO-8859-1"); } catch (Exception e) { return null; }
        if (!constants.contains("org/lwjgl/") && !constants.contains("gameRenderer")) return null;
        try {
            ClassNode cls = new ClassNode();
            new ClassReader(input).accept(cls, 0);
            List<MethodNode> added = new ArrayList<MethodNode>();
            boolean changed = false;
            boolean mainRenderGate = false;
            for (MethodNode method : cls.methods) {
                for (AbstractInsnNode n : method.instructions.toArray()) {
                    // Minecraft vanilla and intermediary namespaces retain this profiler marker.
                    if (n instanceof LdcInsnNode && "gameRenderer".equals(((LdcInsnNode)n).cst)) {
                        AbstractInsnNode jump = previous(n);
                        if (jump != null && jump.getOpcode() == Opcodes.ALOAD) jump = previous(jump);
                        if (jump instanceof JumpInsnNode && jump.getOpcode() == Opcodes.IFNE) {
                            AbstractInsnNode field = previous(jump);
                            if (field instanceof FieldInsnNode && field.getOpcode() == Opcodes.GETFIELD &&
                                "Z".equals(((FieldInsnNode)field).desc) && ((FieldInsnNode)field).owner.equals(name)) {
                                method.instructions.insert(field, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    BRIDGE, "suppressRender", "(Z)Z", false));
                                changed = mainRenderGate = true;
                            }
                        }
                    }
                    if (!(n instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)n;
                    if (call.getOpcode() != Opcodes.INVOKESTATIC) continue;
                    if (call.owner.equals("org/lwjgl/glfw/GLFW") &&
                        call.name.equals("glfwInit") && call.desc.equals("()Z")) {
                        InsnList probe = new InsnList();
                        probe.add(new InsnNode(Opcodes.DUP));
                        probe.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "observeGlfwInit", "(Z)V", false));
                        method.instructions.insert(call, probe); changed = true;
                    } else if (call.owner.equals("org/lwjgl/glfw/GLFW") &&
                               call.name.equals("glfwGetPrimaryMonitor") && call.desc.equals("()J")) {
                        InsnList probe = new InsnList();
                        probe.add(new InsnNode(Opcodes.DUP2));
                        probe.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "observeMonitor", "(J)V", false));
                        method.instructions.insert(call, probe); changed = true;
                    } else if (call.owner.equals("org/lwjgl/glfw/GLFW") &&
                               call.name.equals("glfwGetVideoMode") && call.desc.equals("(J)Lorg/lwjgl/glfw/GLFWVidMode;")) {
                        InsnList probe = new InsnList();
                        probe.add(new InsnNode(Opcodes.DUP));
                        probe.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "observeVideoMode", "(Ljava/lang/Object;)V", false));
                        method.instructions.insert(call, probe); changed = true;
                    } else if (call.owner.equals("org/lwjgl/glfw/GLFW") &&
                               call.name.equals("glfwCreateWindow") && call.desc.startsWith("(II") && call.desc.endsWith(")J")) {
                        InsnList probe = new InsnList();
                        probe.add(new InsnNode(Opcodes.DUP2));
                        probe.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "observeWindow", "(J)V", false));
                        method.instructions.insert(call, probe); changed = true;
                    } else if (call.owner.equals("org/lwjgl/glfw/GLFW") &&
                        call.name.equals("glfwSwapBuffers") && call.desc.equals("(J)V")) {
                        method.instructions.insertBefore(call, new LdcInsnNode(Type.getObjectType(call.owner)));
                        call.owner = BRIDGE; call.name = "swapBuffers"; call.desc = "(JLjava/lang/Class;)V";
                        changed = true;
                    } else if (isAL(call.owner) && call.name.equals("alSourcef") && call.desc.equals("(IIF)V")) {
                        method.instructions.insertBefore(call, new LdcInsnNode(Type.getObjectType(call.owner)));
                        call.owner = BRIDGE; call.name = "sourceGain"; call.desc = "(IIFLjava/lang/Class;)V";
                        changed = true;
                    } else if (isAL(call.owner) && call.name.equals("alSourcePlay") && call.desc.equals("(I)V")) {
                        method.instructions.insertBefore(call, new LdcInsnNode(Type.getObjectType(call.owner)));
                        call.owner = BRIDGE; call.name = "sourcePlay"; call.desc = "(ILjava/lang/Class;)V";
                        changed = true;
                    } else if (isAL(call.owner) && call.name.equals("alListenerf") && call.desc.equals("(IF)V")) {
                        method.instructions.insertBefore(call, new LdcInsnNode(Type.getObjectType(call.owner)));
                        call.owner = BRIDGE; call.name = "listenerGain"; call.desc = "(IFLjava/lang/Class;)V";
                        changed = true;
                    } else if (isAL(call.owner) && (call.name.equals("alListener3f") || call.name.equals("alListenerfv"))) {
                        InsnList pulse = new InsnList();
                        pulse.add(new LdcInsnNode(Type.getObjectType(call.owner)));
                        pulse.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "audioPulse", "(Ljava/lang/Class;)V", false));
                        method.instructions.insertBefore(call, pulse); changed = true;
                    } else if (drawCall(call)) {
                        String wrapper = "mj$afk$draw$" + added.size();
                        // Synthetic wrappers preserve argument evaluation and existing stack maps.
                        added.add(wrapper(wrapper, call));
                        call.owner = name; call.name = wrapper;
                        call.itf = (cls.access & Opcodes.ACC_INTERFACE) != 0;
                        changed = true;
                    }
                }
            }
            // Vanilla 1.13/1.14: monitor manager lookup uses a map populated by
            // glfwGetMonitors(). Some Android GLFW builds report a primary monitor
            // that is not in this cached list. Recover only if exact class shape
            // and getter signatures match. Never change the native pointer.
            String monitorReturn = "cfr".equals(name) ? "cfj" : "cty".equals(name) ? "ctu" : null;
            if (monitorReturn != null && hasMonitorEnumerateAndCallback(cls)) {
                for (MethodNode method : cls.methods) {
                    if (!"a".equals(method.name) || !("(J)L" + monitorReturn + ";").equals(method.desc)) continue;
                    boolean readsMap = false;
                    for (AbstractInsnNode n : method.instructions.toArray()) {
                        if (n instanceof MethodInsnNode &&
                            ((MethodInsnNode)n).name.equals("get") &&
                            ((MethodInsnNode)n).owner.equals("java/util/Map")) readsMap = true;
                    }
                    if (!readsMap) continue;
                    for (AbstractInsnNode n : method.instructions.toArray()) {
                        if (n.getOpcode() != Opcodes.ARETURN) continue;
                        InsnList call = new InsnList();
                        // Stack already contains the original lookup result.
                        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        call.add(new VarInsnNode(Opcodes.LLOAD, 1));
                        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "recoverMonitor",
                            "(Ljava/lang/Object;Ljava/lang/Object;J)Ljava/lang/Object;", false));
                        call.add(new TypeInsnNode(Opcodes.CHECKCAST, monitorReturn));
                        method.instructions.insertBefore(n, call);
                        changed = true;
                    }
                }
            }
            if (!changed) return null;
            cls.methods.addAll(added);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cls.accept(writer);
            if (mainRenderGate) System.err.println("[MJ AFK] Main render gate installed: " + name);
            return writer.toByteArray();
        } catch (Throwable failure) {
            System.err.println("[MJ AFK] Unchanged class " + name + ": " + failure);
            return null;
        }
    }
    private static boolean hasMonitorEnumerateAndCallback(ClassNode cls) {
        boolean enumerate = false, callback = false;
        for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode i = (MethodInsnNode)n;
            if (!"org/lwjgl/glfw/GLFW".equals(i.owner)) continue;
            if ("glfwGetMonitors".equals(i.name)) enumerate = true;
            if ("glfwSetMonitorCallback".equals(i.name)) callback = true;
        }
        return enumerate && callback;
    }
    private static AbstractInsnNode previous(AbstractInsnNode node) {
        node = node.getPrevious();
        while (node != null && node.getOpcode() < 0) node = node.getPrevious();
        return node;
    }
    private static boolean isAL(String owner) {
        return owner.equals("org/lwjgl/openal/AL10") || owner.equals("org/lwjgl/openal/AL10C");
    }
    private static boolean drawCall(MethodInsnNode call) {
        if (!call.owner.startsWith("org/lwjgl/opengl/") || !call.desc.endsWith(")V")) return false;
        String n = call.name.startsWith("n") ? call.name.substring(1) : call.name;
        return n.matches("gl(?:Draw(?:Arrays|Elements|RangeElements|MeshTasks|TransformFeedback).*|MultiDraw.*|Blit.*|Clear(?:Buffer[fiuv]+|NamedFramebuffer[fiuv]+)?(?:ARB|EXT)?)");
    }
    private static MethodNode wrapper(String name, MethodInsnNode original) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                                      name, original.desc, null, null);
        m.visitCode();
        Label active = new Label();
        m.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "background", "()Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, active);
        m.visitInsn(Opcodes.RETURN);
        m.visitLabel(active);
        m.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        int slot = 0;
        for (Type argument : Type.getArgumentTypes(original.desc)) {
            m.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), slot); slot += argument.getSize();
        }
        m.visitMethodInsn(original.getOpcode(), original.owner, original.name, original.desc, original.itf);
        m.visitInsn(Opcodes.RETURN); m.visitMaxs(0, 0); m.visitEnd();
        return m;
    }
}