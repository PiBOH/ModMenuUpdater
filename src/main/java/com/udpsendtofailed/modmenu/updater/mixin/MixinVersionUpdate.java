package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.api.UpdateInfo;
import com.udpsendtofailed.modmenu.updater.UpdateDataStorage; // IMPORT NEW CLASS
import com.udpsendtofailed.modmenu.updater.api.UpdateInfoExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.terraformersmc.modmenu.util.UpdateCheckerUtil$VersionUpdate")
public class MixinVersionUpdate {

    @Shadow String versionId;

    @Inject(method = "asUpdateInfo", at = @At("RETURN"))
    private void injectUrl(CallbackInfoReturnable<UpdateInfo> cir) {
        UpdateInfo info = cir.getReturnValue();
        if (info instanceof UpdateInfoExtension ext) {
            
            // USE THE NEW STORAGE CLASS
            var data = UpdateDataStorage.DOWNLOAD_CACHE.get(this.versionId);
            
            if (data != null) {
                ext.setDownloadInfo(data.url(), data.filename(), data.hash());
            }
        }
    }
}