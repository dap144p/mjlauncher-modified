package git.artdeell.mojo.afk;
import java.lang.instrument.Instrumentation;
public final class Agent {
    public static void premain(String state, Instrumentation instrumentation) {
        try {
            Bridge.initialize(state);
            instrumentation.addTransformer(new EngineTransformer());
            System.err.println("[MJ AFK] Built-in engine active; no mod loader required.");
        } catch (Throwable failure) {
            System.err.println("[MJ AFK] Engine unavailable: " + failure);
        }
    }
}