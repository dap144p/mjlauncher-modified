#!/usr/bin/env python3
"""Build-time diagnostic mode to isolate window startup crashes from session/AFK hooks."""
from pathlib import Path
import sys

upstream = Path(sys.argv[1])
chooser = upstream / "app_mojo/src/main/java/git/artdeell/mojo/sessions/SessionChooserActivity.java"
controller = upstream / "app_mojo/src/main/java/git/artdeell/mojo/game/afk/AfkController.java"

def replace_one(text, old, new, label):
    if text.count(old) != 1:
        raise RuntimeError(f"{label}: expected one matching location, found {text.count(old)}")
    return text.replace(old, new, 1)

src = chooser.read_text()
src = replace_one(src,
    'String[] labels = new String[SessionManager.MAX_SESSIONS];',
    'String[] labels = new String[SessionManager.MAX_SESSIONS + 1];\n        labels[0]="Compatibility test (original game process; AFK OFF)";',
    "chooser options")
src = replace_one(src,
    'for(int i=0;i<labels.length;i++) {\n            int slot=i+1;',
    'for(int i=1;i<labels.length;i++) {\n            int slot=i;',
    "chooser slot indices")
src = replace_one(src,
    '                int slot=which+1;\n                if(SessionManager.isRunning(this,slot)) {',
    '                if(which == 0) { launchCompatibility(); return; }\n                int slot=which;\n                if(SessionManager.isRunning(this,slot)) {',
    "chooser selection")
src = replace_one(src,
    '    private void launch(int slot) {',
    '''    /** Uses the original single-game Android process to isolate native window startup. */
    private void launchCompatibility() {
        if(launched) return;
        launched=true;
        Intent source=getIntent();
        Intent start=new Intent(this,GameActivity.class);
        start.putExtra(GameActivity.INTENT_LAUNCH_VERSION,
                source.getStringExtra(GameActivity.INTENT_LAUNCH_VERSION));
        java.io.Serializable cp=source.getSerializableExtra(GameActivity.INTENT_LAUNCH_CLASSPATH);
        if(cp != null) start.putExtra(GameActivity.INTENT_LAUNCH_CLASSPATH, cp);
        String id=source.getStringExtra(SessionManager.EXTRA_INSTANCE_ID);
        if(id == null) id=LauncherPreferences.DEFAULT_PREF.getString(
                LauncherPreferences.PREF_KEY_CURRENT_INSTANCE, "");
        start.putExtra(SessionManager.EXTRA_INSTANCE_ID,id);
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(start);
        finish();
    }

    private void launch(int slot) {''',
    "compat launch")
chooser.write_text(src)

afk = controller.read_text()
afk = replace_one(afk,
    '    public static void prepare(Context context, int slot) {\n',
    '''    public static void prepare(Context context, int slot) {
        // The classic Android game process is always free of instrumentation.
        // This baseline is essential to distinguish window/GLFW failures from
        // multi-session or AFK Java-agent regressions on older Minecraft clients.
        if(slot == 0) {
            enabled = false;
            agentJar = null;
            stateDirectory = null;
            Log.i(TAG, "Compatibility mode: built-in AFK agent disabled");
            return;
        }
''', "compat AFK bypass")
controller.write_text(afk)
print("Applied v13 startup compatibility isolation mode")
