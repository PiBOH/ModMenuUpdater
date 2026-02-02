package com.udpsendtofailed.modmenu.updater;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.udpsendtofailed.modmenu.updater.api.ModExtension;
import com.udpsendtofailed.modmenu.updater.api.UpdateInfoExtension;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModUpdaterService {
    public static final Logger LOGGER = LoggerFactory.getLogger("Mod Menu Updater");
    private final ModsScreen screen;
    private final Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    public ModUpdaterService(ModsScreen screen) {
        this.screen = screen;
    }

    public void performUpdateAll() {
        List<Mod> toUpdate = ModMenu.MODS.values().stream()
                .filter(mod -> {
                    if (mod.getUpdateInfo() instanceof UpdateInfoExtension info && info.getDownloadUrl() != null) {
                        return mod.hasUpdate() && !((ModExtension) mod).isDownloadingUpdate()
                                && !((ModExtension) mod).isUpdateDownloaded();
                    }
                    return false;
                })
                .toList();

        if (toUpdate.isEmpty())
            return;

        toastSuccess(
                Text.translatable("modmenu.update.toast.all.started.title"),
                Text.translatable("modmenu.update.toast.all.started.description", toUpdate.size()));

        for (Mod mod : toUpdate) {
            performUpdate(mod);
        }
    }

    public void performUpdate(Mod mod) {
        if (!(mod.getUpdateInfo() instanceof UpdateInfoExtension info) || info.getDownloadUrl() == null)
            return;

        ModExtension ext = (ModExtension) mod;
        if (ext.isDownloadingUpdate() || ext.isUpdateDownloaded())
            return;

        Optional<Path> oldFile = findModJar(mod);
        if (oldFile.isEmpty()) {
            LOGGER.warn("Could not find JAR for mod '{}'. Skipping.", mod.getId());
            return;
        }

        ext.setDownloadingUpdate(true);

        CompletableFuture.runAsync(() -> {
            try {
                Path tempFile = downloadFile(info.getDownloadUrl());

                if (info.getFileHash() != null) {
                    String downloadedHash = Files.asByteSource(tempFile.toFile()).hash(Hashing.sha512()).toString();
                    if (!info.getFileHash().equalsIgnoreCase(downloadedHash)) {
                        throw new IOException("File hash mismatch");
                    }
                }

                Path newFilePath = modsDir.resolve(info.getFileName());
                java.nio.file.Files.move(tempFile, newFilePath, StandardCopyOption.REPLACE_EXISTING);

                CleanupManager.scheduleForCleanup(oldFile.get());

                toastSuccess(
                        Text.translatable("modmenu.update.toast.single.success.title"),
                        Text.translatable("modmenu.update.toast.single.success.description", mod.getName()));

                ext.setUpdateDownloaded(true);

                String parentId = mod.getParent();
                if (parentId != null) {
                    Mod parentMod = ModMenu.MODS.get(parentId);
                    if (parentMod instanceof ModExtension parentExt) {
                        parentExt.resetChildHasUpdate();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Update failed for " + mod.getName(), e);
                toastError(
                        Text.translatable("modmenu.update.toast.error.title"),
                        Text.literal(mod.getName() + ": " + e.getMessage()));
            } finally {
                ext.setDownloadingUpdate(false);
            }
        }, EXECUTOR);
    }

    private Path downloadFile(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build();
        Path tempFile = java.nio.file.Files.createTempFile("modmenu-updater", ".jar.tmp");
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode());
        try (InputStream is = response.body()) {
            java.nio.file.Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private Optional<Path> findModJar(Mod mod) {
        if (mod instanceof com.terraformersmc.modmenu.util.mod.fabric.FabricMod fabricMod) {
            var container = fabricMod.getContainer();
            if (container.getOrigin().getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH) {
                return container.getOrigin().getPaths().stream()
                        .filter(p -> p.toString().toLowerCase().endsWith(".jar") && java.nio.file.Files.exists(p))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private void toastSuccess(Text title, Text description) {
        MinecraftClient.getInstance().execute(() -> SystemToast.add(MinecraftClient.getInstance().getToastManager(),
                SystemToast.Type.PERIODIC_NOTIFICATION, title, description));
    }

    private void toastError(Text title, Text description) {
        MinecraftClient.getInstance().execute(() -> SystemToast.add(MinecraftClient.getInstance().getToastManager(),
                SystemToast.Type.PACK_COPY_FAILURE, title, description));
    }
}