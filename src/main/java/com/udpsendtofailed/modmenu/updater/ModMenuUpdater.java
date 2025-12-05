package com.udpsendtofailed.modmenu.updater;

import net.fabricmc.api.ClientModInitializer;

public class ModMenuUpdater implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CleanupManager.performCleanup();
    }
}