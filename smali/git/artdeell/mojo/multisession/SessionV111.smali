.class public final Lgit/artdeell/mojo/multisession/SessionV111;
.super Ljava/lang/Object;
.source "SessionV111.java"

.method public static markCurrent(Landroid/content/Context;)V
    .locals 6
    :try_start
    invoke-static {}, Lgit/artdeell/mojo/multisession/SessionRuntime;->index()I
    move-result v0
    if-ltz v0, :end
    const/16 v1, 0x14
    if-ge v0, v1, :end
    new-instance v1, Ljava/io/File;
    invoke-virtual {p0}, Landroid/content/Context;->getFilesDir()Ljava/io/File;
    move-result-object v2
    const-string v3, "native-afk"
    invoke-direct {v1, v2, v3}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    invoke-virtual {v1}, Ljava/io/File;->mkdirs()Z
    new-instance v2, Ljava/io/File;
    new-instance v3, Ljava/lang/StringBuilder;
    const-string v4, "process-"
    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-static {}, Landroid/os/Process;->myPid()I
    move-result v4
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v4, ".slot"
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v3
    invoke-direct {v2, v1, v3}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v2}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0
    const-string v2, "UTF-8"
    invoke-virtual {v0, v2}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B
    move-result-object v0
    invoke-virtual {v1, v0}, Ljava/io/FileOutputStream;->write([B)V
    invoke-virtual {v1}, Ljava/io/FileOutputStream;->close()V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch_all
    :end
    return-void
    :catch_all
    move-exception v0
    const-string v1, "MJV111"
    const-string v2, "Cannot map AFK process to session"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    return-void
.end method

.method private static flagFile(Landroid/content/Context;I)Ljava/io/File;
    .locals 4
    new-instance v0, Ljava/io/File;
    invoke-virtual {p0}, Landroid/content/Context;->getFilesDir()Ljava/io/File;
    move-result-object v1
    const-string v2, "native-afk"
    invoke-direct {v0, v1, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    invoke-virtual {v0}, Ljava/io/File;->mkdirs()Z
    new-instance v1, Ljava/io/File;
    new-instance v2, Ljava/lang/StringBuilder;
    const-string v3, "slot-"
    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v3, ".enabled"
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v2
    invoke-direct {v1, v0, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    return-object v1
.end method

.method public static enabled(Landroid/content/Context;I)Z
    .locals 2
    :try_start
    invoke-static {p0, p1}, Lgit/artdeell/mojo/multisession/SessionV111;->flagFile(Landroid/content/Context;I)Ljava/io/File;
    move-result-object v0
    invoke-virtual {v0}, Ljava/io/File;->isFile()Z
    move-result v1
    if-eqz v1, :default
    new-instance v1, Ljava/io/FileInputStream;
    invoke-direct {v1, v0}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    invoke-virtual {v1}, Ljava/io/FileInputStream;->read()I
    move-result v0
    invoke-virtual {v1}, Ljava/io/FileInputStream;->close()V
    const/16 v1, 0x30
    if-ne v0, v1, :default
    const/4 v0, 0x0
    return v0
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :default
    :default
    const/4 v0, 0x1
    return v0
.end method

.method public static toggle(Landroid/content/Context;I)V
    .locals 4
    :try_start
    invoke-static {p0, p1}, Lgit/artdeell/mojo/multisession/SessionV111;->flagFile(Landroid/content/Context;I)Ljava/io/File;
    move-result-object v0
    invoke-static {p0, p1}, Lgit/artdeell/mojo/multisession/SessionV111;->enabled(Landroid/content/Context;I)Z
    move-result v1
    new-instance v2, Ljava/io/FileOutputStream;
    invoke-direct {v2, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    if-eqz v1, :write_on
    const/16 v3, 0x30
    goto :write
    :write_on
    const/16 v3, 0x31
    :write
    invoke-virtual {v2, v3}, Ljava/io/FileOutputStream;->write(I)V
    invoke-virtual {v2}, Ljava/io/FileOutputStream;->close()V
    if-eqz v1, :toast_on
    const-string v0, "AFK sesi: OFF (render/audio normal)"
    goto :toast
    :toast_on
    const-string v0, "AFK sesi: ON saat background"
    :toast
    const/4 v2, 0x0
    invoke-static {p0, v0, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch_all
    return-void
    :catch_all
    move-exception v0
    const-string v1, "MJV111"
    const-string v2, "AFK toggle failed"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    return-void
.end method

.method public static show(Lgit/artdeell/mojo/multisession/SessionRouterActivity;I)V
    .locals 6
    new-instance v0, Landroid/app/AlertDialog$Builder;
    invoke-direct {v0, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V
    new-instance v1, Ljava/lang/StringBuilder;
    const-string v2, "Kontrol Sesi "
    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    add-int/lit8 v2, p1, 0x1
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const/4 v1, 0x5
    new-array v1, v1, [Ljava/lang/String;
    const/4 v2, 0x0
    const-string v3, "Resume (tanpa restart JVM)"
    aput-object v3, v1, v2
    const/4 v2, 0x1
    invoke-static {p0, p1}, Lgit/artdeell/mojo/multisession/SessionV111;->enabled(Landroid/content/Context;I)Z
    move-result v4
    if-eqz v4, :set_on
    const-string v3, "Matikan AFK khusus sesi ini"
    goto :set_afk
    :set_on
    const-string v3, "Aktifkan AFK khusus sesi ini"
    :set_afk
    aput-object v3, v1, v2
    const/4 v2, 0x2
    const-string v3, "Stop sesi (konfirmasi)"
    aput-object v3, v1, v2
    const/4 v2, 0x3
    const-string v3, "Atur heap RAM"
    aput-object v3, v1, v2
    const/4 v2, 0x4
    const-string v3, "Restart: Stop, lalu tap sesi lagi"
    aput-object v3, v1, v2
    new-instance v2, Lgit/artdeell/mojo/multisession/SessionV111$Choice;
    invoke-direct {v2, p0, p1}, Lgit/artdeell/mojo/multisession/SessionV111$Choice;-><init>(Lgit/artdeell/mojo/multisession/SessionRouterActivity;I)V
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setItems([Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
    return-void
.end method

.method public static stop(Landroid/app/Activity;I)V
    .locals 4
    :try_start
    new-instance v0, Landroid/content/Intent;
    invoke-static {p1}, Lgit/artdeell/mojo/multisession/SessionRuntime;->serviceClass(I)Ljava/lang/Class;
    move-result-object v1
    invoke-direct {v0, p0, v1}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    const-string v1, "kill"
    const/4 v2, 0x1
    invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;
    invoke-virtual {p0, v0}, Landroid/app/Activity;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;
    const-string v0, "Permintaan Stop dikirim ke sesi ini saja"
    const/4 v1, 0x0
    invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch_all
    return-void
    :catch_all
    move-exception v0
    const-string v1, "MJV111"
    const-string v2, "Stop failed"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    return-void
.end method