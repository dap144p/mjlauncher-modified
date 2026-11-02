.class public final Lgit/artdeell/mojo/multisession/SessionV111$Stop;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;
.source "SessionV111.java"
.field private final screen:Landroid/app/Activity;
.field private final slot:I
.method public constructor <init>(Landroid/app/Activity;I)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lgit/artdeell/mojo/multisession/SessionV111$Stop;->screen:Landroid/app/Activity;
    iput p2, p0, Lgit/artdeell/mojo/multisession/SessionV111$Stop;->slot:I
    return-void
.end method
.method public onClick(Landroid/content/DialogInterface;I)V
    .locals 2
    iget-object v0, p0, Lgit/artdeell/mojo/multisession/SessionV111$Stop;->screen:Landroid/app/Activity;
    iget v1, p0, Lgit/artdeell/mojo/multisession/SessionV111$Stop;->slot:I
    invoke-static {v0, v1}, Lgit/artdeell/mojo/multisession/SessionV111;->stop(Landroid/app/Activity;I)V
    return-void
.end method