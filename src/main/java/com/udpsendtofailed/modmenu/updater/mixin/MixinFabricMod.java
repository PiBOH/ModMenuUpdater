package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.util.mod.fabric.FabricMod;
import com.udpsendtofailed.modmenu.updater.api.ModExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FabricMod.class)
public class MixinFabricMod implements ModExtension {
    @Shadow protected boolean childHasUpdate;
    
    @Unique private boolean isDownloadingUpdate = false;
    @Unique private boolean isUpdateDownloaded = false;

    @Override
    public void setDownloadingUpdate(boolean downloading) {
        this.isDownloadingUpdate = downloading;
    }

    @Override
    public boolean isDownloadingUpdate() {
        return this.isDownloadingUpdate;
    }

    @Override
    public void setUpdateDownloaded(boolean downloaded) {
        this.isUpdateDownloaded = downloaded;
    }

    @Override
    public boolean isUpdateDownloaded() {
        return this.isUpdateDownloaded;
    }

    @Override
    public void resetChildHasUpdate() {
        this.childHasUpdate = false;
    }
}