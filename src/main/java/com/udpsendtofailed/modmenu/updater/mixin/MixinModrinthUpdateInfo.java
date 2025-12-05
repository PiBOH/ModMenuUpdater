package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.util.mod.ModrinthUpdateInfo;
import com.udpsendtofailed.modmenu.updater.api.UpdateInfoExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModrinthUpdateInfo.class)
public class MixinModrinthUpdateInfo implements UpdateInfoExtension {
    @Unique private String downloadUrl;
    @Unique private String fileName;
    @Unique private String fileHash;

    @Override
    public void setDownloadInfo(String url, String filename, String hash) {
        this.downloadUrl = url;
        this.fileName = filename;
        this.fileHash = hash;
    }

    @Override
    public String getDownloadUrl() { return this.downloadUrl; }
    
    @Override
    public String getFileName() { return this.fileName; }
    
    @Override
    public @Nullable String getFileHash() { return this.fileHash; }
}