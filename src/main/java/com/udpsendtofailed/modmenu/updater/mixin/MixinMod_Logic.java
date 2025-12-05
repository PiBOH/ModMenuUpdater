package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.util.mod.Mod;
import com.udpsendtofailed.modmenu.updater.api.ModExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mod.class)
public interface MixinMod_Logic {

    /**
     * Intercepts the default 'hasUpdate' method in the Mod interface.
     * If the update is downloaded, we force it to return false so the badge disappears.
     */
    @Inject(method = "hasUpdate", at = @At("HEAD"), cancellable = true)
    default void hideUpdateIfDownloaded(CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof ModExtension ext && ext.isUpdateDownloaded()) {
            cir.setReturnValue(false);
        }
    }
}