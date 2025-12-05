package com.udpsendtofailed.modmenu.updater.api;

import org.jetbrains.annotations.Nullable;

public interface UpdateInfoExtension {
    void setDownloadInfo(String url, String filename, String hash);
    String getDownloadUrl();
    String getFileName();
    @Nullable String getFileHash();
}