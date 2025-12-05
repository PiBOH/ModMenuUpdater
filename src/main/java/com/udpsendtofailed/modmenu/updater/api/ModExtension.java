package com.udpsendtofailed.modmenu.updater.api;

public interface ModExtension {
    void setDownloadingUpdate(boolean downloading);
    boolean isDownloadingUpdate();
    void setUpdateDownloaded(boolean downloaded);
    boolean isUpdateDownloaded();
    void resetChildHasUpdate();
}