from pathlib import Path
import re
b=Path(__file__).resolve().parents[1];d=b/'decoded';C='Lgit/artdeell/mojo/game/renderer/RendererCatalog;'
def replace_method(path,signature,body):
 s=path.read_text();pattern=r'\.method '+re.escape(signature)+r'\n.*?\.end method'
 s,n=re.subn(pattern,'.method '+signature+'\n'+body.strip('\n')+'\n.end method',s,flags=re.S);assert n==1,(path,signature,n);path.write_text(s)
p=d/'smali/git/artdeell/mojo/game/renderer/GameRenderer.smali'
replace_method(p,'public constructor <init>(Ljava/lang/String;)V','''
    .locals 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    new-instance v0, Ljava/util/HashMap;
    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
    iput-object v0, p0, Lgit/artdeell/mojo/game/renderer/GameRenderer;->environment:Ljava/util/Map;
    invoke-static {p1}, '''+C+'''->require(Ljava/lang/String;)Lgit/artdeell/mojo/game/renderer/RenderSpec;
    move-result-object p1
    iput-object p1, p0, Lgit/artdeell/mojo/game/renderer/GameRenderer;->currentRenderer:Lgit/artdeell/mojo/game/renderer/RenderSpec;
    return-void
''')
s=p.read_text();needle='.method public static getKnownRenderer(Ljava/lang/String;)Lgit/artdeell/mojo/game/renderer/RenderSpec;\n    .locals 2';assert s.count(needle)==1
s=s.replace(needle,needle+'''

    invoke-static {p0}, '''+C+'''->find(Ljava/lang/String;)Lgit/artdeell/mojo/game/renderer/RenderSpec;
    move-result-object v0
    if-eqz v0, :mj_original_registry
    return-object v0
    :mj_original_registry
    if-nez p0, :mj_valid_id
    const/4 v0, 0x0
    return-object v0
    :mj_valid_id
''');p.write_text(s)
replace_method(p,'public maybeSetupRenderer()Z','''
    .locals 2
    sget-object v0, Lgit/artdeell/mojo/Tools;->NATIVE_LIB_DIR:Ljava/lang/String;
    iget-object v1, p0, Lgit/artdeell/mojo/game/renderer/GameRenderer;->currentRenderer:Lgit/artdeell/mojo/game/renderer/RenderSpec;
    invoke-interface {v1}, Lgit/artdeell/mojo/game/renderer/RenderSpec;->librarySearchPath()Ljava/lang/String;
    move-result-object v1
    invoke-static {v0, v1}, Lgit/artdeell/mojo/game/renderer/GameRenderer;->setRendererLibraryPath(Ljava/lang/String;Ljava/lang/String;)V
    iget-object v0, p0, Lgit/artdeell/mojo/game/renderer/GameRenderer;->currentRenderer:Lgit/artdeell/mojo/game/renderer/RenderSpec;
    invoke-static {v0}, '''+C+'''->setupOrThrow(Lgit/artdeell/mojo/game/renderer/RenderSpec;)Z
    move-result v0
    return v0
''')
replace_method(p,'public setCurrentRenderer(Lgit/artdeell/mojo/game/renderer/RenderSpec;)V','''
    .locals 1
    iget-object v0, p0, Lgit/artdeell/mojo/game/renderer/GameRenderer;->currentRenderer:Lgit/artdeell/mojo/game/renderer/RenderSpec;
    invoke-static {v0, p1}, '''+C+'''->guardReplacement(Lgit/artdeell/mojo/game/renderer/RenderSpec;Lgit/artdeell/mojo/game/renderer/RenderSpec;)V
    return-void
''')
p=d/'smali/git/artdeell/mojo/game/renderer/RendererCache.smali'
replace_method(p,'public static getCompatibleRenderers(Landroid/content/Context;)Lgit/artdeell/mojo/game/renderer/RendererCache;','''
    .locals 0
    invoke-static {p0}, '''+C+'''->catalog(Landroid/content/Context;)Lgit/artdeell/mojo/game/renderer/RendererCache;
    move-result-object p0
    return-object p0
''')
replace_method(p,'public static releaseRendererCache()V','    .locals 0\n    return-void')
p=d/'smali/git/artdeell/mojo/utils/jre/GameRunner.smali'
replace_method(p,'private static switchRendererIfSupported(ZLgit/artdeell/mojo/game/renderer/RenderSpec;Lgit/artdeell/mojo/game/renderer/GameRenderer;Lgit/artdeell/mojo/instances/Instance;Landroidx/appcompat/app/AppCompatActivity;I)V','''
    .locals 1
    invoke-virtual {p2}, Lgit/artdeell/mojo/game/renderer/GameRenderer;->getCurrentRenderer()Lgit/artdeell/mojo/game/renderer/RenderSpec;
    move-result-object v0
    invoke-static {v0, p1}, '''+C+'''->rejectAutomaticChange(Lgit/artdeell/mojo/game/renderer/RenderSpec;Lgit/artdeell/mojo/game/renderer/RenderSpec;)V
    return-void
''')
s=p.read_text();needle='    move-result-object v14\n\n    .line 197';assert s.count(needle)==1
s=s.replace(needle,'    move-result-object v14\n\n    move-object/from16 v0, p0\n    invoke-static {v0, v14}, '+C+'->validate(Landroid/content/Context;Lgit/artdeell/mojo/game/renderer/RenderSpec;)V\n\n    .line 197');p.write_text(s)
p=d/'smali_classes2/git/artdeell/mojo/multisession/SessionRuntime.smali';s=p.read_text()
a=s.index('.method public static capture(');z=s.index('.end method',a);part=s[a:z]
needle='    .line 25\n    :try_start_c';assert part.count(needle)==1
part=part.replace(needle,'''    invoke-virtual {v0}, Lgit/artdeell/mojo/instances/Instance;->getLaunchRenderer()Ljava/lang/String;
    move-result-object v2
    const-string v3, "mj_session_renderer"
    invoke-virtual {p1, v3, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

'''+needle);s=s[:a]+part+s[z:]
a=s.index('.method public static loadInstance(');z=s.index('.end method',a);part=s[a:z]
needle='    check-cast p0, Lgit/artdeell/mojo/instances/Instance;';assert part.count(needle)==1
part=part.replace(needle,needle+'''

    sget-object v0, Lgit/artdeell/mojo/multisession/SessionRuntime;->launch:Landroid/content/Intent;
    const-string v1, "mj_session_renderer"
    invoke-virtual {v0, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :mj_renderer_snapshot_done
    iput-object v0, p0, Lgit/artdeell/mojo/instances/Instance;->renderer:Ljava/lang/String;
    :mj_renderer_snapshot_done
''');s=s[:a]+part+s[z:];p.write_text(s)
# Explicit ANGLE variants supersede the global switch, which could change the backend silently.
p=d/'smali/git/artdeell/mojo/prefs/screens/LauncherPreferenceVideoFragment.smali';s=p.read_text()
needle='    invoke-virtual {p2, v0}, Landroidx/preference/SwitchPreferenceCompat;->setVisible(Z)V'
# Match specifically the hasAngle boolean read preceding the use_angle switch.
a=re.search(r'    const-string(?:/jumbo)? \w+, "use_angle"',s).start();z=s.index('->setVisible(Z)V',a);start=s.rfind('    invoke-virtual',a,z)
line=s[start:s.index('\n',z)]
regs=re.search(r'\{([^}]+)\}',line)[1].split(', ');flag=regs[1]
s=s[:start]+'    const/4 '+flag+', 0x0\n\n'+s[start:];p.write_text(s)
p=d/'apktool.yml';s=p.read_text();assert 'versionCode: 551' in s;s=s.replace('versionCode: 551','versionCode: 552').replace('afk-9-modified','afk-10-modified');p.write_text(s)
p=d/'smali/git/artdeell/mojo/BuildConfig.smali';s=p.read_text();assert 'VERSION_CODE:I = 0x227' in s;s=s.replace('VERSION_CODE:I = 0x227','VERSION_CODE:I = 0x228').replace('afk-9-modified','afk-10-modified');p.write_text(s)
print('v10 renderer catalog, strict loading, no autoswitch/fallback, and per-session renderer snapshot patched')