package com.udpsendtofailed.modmenu.updater;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateDataStorage {
    // This is safe to be public because it's a real class, not a Mixin
    public static final Map<String, DownloadData> DOWNLOAD_CACHE = new ConcurrentHashMap<>();

    public record DownloadData(String url, String filename, String hash) {}
}