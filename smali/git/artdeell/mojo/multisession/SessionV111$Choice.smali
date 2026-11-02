.class public final Lgit/artdeell/mojo/multisession/SessionV111$Choice;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;
.source "SessionV111.java"

.field private final screen:Lgit/artdeell/mojo/multisession/SessionRouterActivity;
.field private final slot:I

.method public constructor <init>(Lgit/artdeell/mojo/multisession/SessionRouterActivity;I)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lgit/artdeell/mojo/multisession/SessionV111$Choice;->screen:Lgit/artdeell/mojo/multisession/SessionRouterActivity;
    iput p2, p0, Lgit/artdeell/mojo/multisession/SessionV111$Choice;->slot:I
    return-void
.end method

.method public onClick(Landroid/content/DialogInterface;I)V
    .locals 3
    iget-object v0, p0, Lgit/artdeell/mojo/multisession/SessionV111$Choice;->screen:Lgit/artdeell/mojo/multisession/SessionRouterActivity;
    iget v1, p0, Lgit/artdeell/mojo/multisession/SessionV111$Choice;->slot:I
    packed-switch p2, :switch_start
    return-void
    :resume
    :try_resume
    invoke-static {v0, v1}, Lgit/artdeell/mojo/multisession/SessionRuntime;->running(Landroid/content/Context;I)Z
    move-result v2
    if-eqz v2, :not_running
    invoke-static {v0, v1}, Lgit/artdeell/mojo/multisession/SessionRuntime;->openRunning(Landroid/app/Activity;I)V
    return-void
    :not_running
    const-string v1, "Sesi tidak berjalan. Tekan slot untuk memulai baru."
    const/4 v2, 0x0
    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v1
    invoke-virtual {v1}, Landroid/widget/Toast;->show()V
    return-void
    :afk
    invoke-static {v0, v1}, Lgit/artdeell/mojo/multisession/SessionV111;->toggle(Landroid/content/Context;I)V
    return-void
    :stop
    new-instance v2, Landroid/app/AlertDialog$Builder;
    invoke-direct {v2, v0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V
    const-string p1, "Hentikan sesi ini?"
    invoke-virtual {v2, p1}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const-string p1, "Minecraft pada slot ini akan ditutup. Slot lain tidak berubah."
    invoke-virtual {v2, p1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    new-instance p1, Lgit/artdeell/mojo/multisession/SessionV111$Stop;
    invoke-direct {p1, v0, v1}, Lgit/artdeell/mojo/multisession/SessionV111$Stop;-><init>(Landroid/app/Activity;I)V
    const-string v1, "Stop"
    invoke-virtual {v2, v1, p1}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    const-string p1, "Batal"
    const/4 v1, 0x0
    invoke-virtual {v2, p1, v1}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v2}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
    return-void
    :ram
    invoke-virtual {v0, v1}, Lgit/artdeell/mojo/multisession/SessionRouterActivity;->configureSlotRam(I)V
    return-void
    :restart_notice
    const-string v1, "Restart aman: Stop sesi, lalu tekan slot untuk mulai ulang manual."
    const/4 v2, 0x1
    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v1
    invoke-virtual {v1}, Landroid/widget/Toast;->show()V
    return-void
    :switch_start
    .packed-switch 0x0
        :resume
        :afk
        :stop
        :ram
        :restart_notice
    .end packed-switch
.end method