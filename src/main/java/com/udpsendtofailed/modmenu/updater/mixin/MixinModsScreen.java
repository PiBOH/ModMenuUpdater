package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.DescriptionListWidget;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.udpsendtofailed.modmenu.updater.ModUpdaterService;
import com.udpsendtofailed.modmenu.updater.api.ModExtension;
import com.udpsendtofailed.modmenu.updater.api.UpdateInfoExtension;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModsScreen.class)
public abstract class MixinModsScreen extends Screen {
    @Shadow private ModListEntry selected;
    @Shadow private int paneWidth;
    @Shadow private int rightPaneX;
    @Shadow private TextFieldWidget searchBox;
    @Shadow private ClickableWidget filtersButton;
    @Shadow private int searchBoxX; 
    @Shadow private int filtersX; 
    @Shadow private int searchRowWidth;
    @Shadow private DescriptionListWidget descriptionListWidget;
    @Shadow private ClickableWidget configureButton;

    @Unique private ModUpdaterService updater;
    @Unique private ClickableWidget updateButton;
    @Unique private ClickableWidget updateAllButton;

    protected MixinModsScreen(Text title) { super(title); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initService(Screen previous, CallbackInfo ci) {
        this.updater = new ModUpdaterService((ModsScreen)(Object)this);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void recalculateLayoutExact(CallbackInfo ci) {
        boolean updateChecksEnabled = ModMenuConfig.UPDATE_CHECKER.getValue();

        // --- 1. Manage Buttons ---
        
        // Update All Button
        if (updateChecksEnabled) {
            if (this.updateAllButton == null) {
                this.updateAllButton = ButtonWidget.builder(Text.translatable("modmenu.update.button.updateAll"), b -> updater.performUpdateAll())
                    .size(0, 20)
                    .build();
                this.updateAllButton.setTooltip(Tooltip.of(Text.translatable("modmenu.update.tooltip.all")));
            }
            // Always add it to the screen if enabled
            this.addDrawableChild(this.updateAllButton);
        } else {
            // If disabled, clear the reference so it's recreated fresh if re-enabled
            this.updateAllButton = null;
        }

        // Update Button (Single)
        if (updateChecksEnabled) {
            if (this.updateButton == null) {
                this.updateButton = ButtonWidget.builder(Text.translatable("modmenu.update.button.update"), b -> {
                    if (selected != null) {
                        updater.performUpdate(selected.getMod());
                        this.descriptionListWidget.updateSelectedMod(selected.getMod());
                    }
                })
                .size(0, 20)
                .build();
                this.updateButton.visible = false;
                this.updateButton.setTooltip(Tooltip.of(Text.translatable("modmenu.update.tooltip.single")));
            }
            // Always add it to the screen if enabled
            this.addDrawableChild(this.updateButton);
        } else {
            this.updateButton = null;
        }

        // --- 2. PRECISE LAYOUT CALCULATION ---
        // Only run layout math if we are actually rendering the buttons
        
        Text updateAllText = Text.translatable("modmenu.update.button.updateAll");
        int updateAllButtonWidth = updateChecksEnabled ? this.textRenderer.getWidth(updateAllText) + 20 : 0;
        int updateAllGap = updateChecksEnabled ? 2 : 0;
        int filtersButtonSize = (ModMenuConfig.CONFIG_MODE.getValue() ? 0 : 22);

        int searchWidthMax = this.paneWidth - 32 - filtersButtonSize - updateAllButtonWidth - updateAllGap;
        int searchBoxWidth = ModMenuConfig.CONFIG_MODE.getValue() ? Math.min(200, searchWidthMax) : searchWidthMax;
        int totalRowWidth = searchBoxWidth + filtersButtonSize + updateAllGap + updateAllButtonWidth;
        
        this.searchBoxX = this.paneWidth / 2 - totalRowWidth / 2;
        this.searchRowWidth = this.searchBoxX + totalRowWidth + 22;

        if (this.searchBox != null) {
            this.searchBox.setX(this.searchBoxX);
            this.searchBox.setWidth(searchBoxWidth);
        }

        int currentX = this.searchBoxX + searchBoxWidth + 2;

        if (!ModMenuConfig.CONFIG_MODE.getValue() && this.filtersButton != null) {
            this.filtersButton.setX(currentX);
            this.filtersX = currentX; 
            currentX += 20 + updateAllGap;
        }

        if (this.updateAllButton != null && updateChecksEnabled) {
            this.updateAllButton.setX(currentX);
            this.updateAllButton.setY(22);
            this.updateAllButton.setWidth(updateAllButtonWidth);
        }

        if (this.updateButton != null && updateChecksEnabled) {
            int urlButtonWidths = this.paneWidth / 2 - 2;
            int cappedButtonWidth = Math.min(urlButtonWidths, 200);
            int issuesButtonX = this.rightPaneX + urlButtonWidths + 4 + (urlButtonWidths / 2) - (cappedButtonWidth / 2);
            int issuesRightAlign = issuesButtonX + cappedButtonWidth;
            int updateButtonWidth = this.textRenderer.getWidth(Text.translatable("modmenu.update.state.updating")) + 10;
            
            this.updateButton.setWidth(updateButtonWidth);
            this.updateButton.setY(48);

            boolean configVisible = this.configureButton != null && this.configureButton.visible;
            if (configVisible) {
                this.updateButton.setX(issuesRightAlign - 20 - updateButtonWidth - 2);
            } else {
                this.updateButton.setX(issuesRightAlign - updateButtonWidth);
            }
            
            // Re-apply configure button pos
            if (this.configureButton != null) {
                this.configureButton.setX(issuesRightAlign - 20);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/gui/widget/ModListWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void updateButtonState(CallbackInfo ci) {
        if (updateAllButton != null) {
            boolean anyUpdates = ModMenu.MODS.values().stream().anyMatch(m ->
                m instanceof ModExtension ext
                && m.hasUpdate()
                && !ext.isUpdateDownloaded()
                && !ext.isDownloadingUpdate()
                && m.getUpdateInfo() instanceof UpdateInfoExtension uie
                && uie.getDownloadUrl() != null
            );
            updateAllButton.active = anyUpdates;
        }

        if (selected != null) {
            Mod mod = selected.getMod();

            if (!(mod instanceof ModExtension ext)) {
                if (updateButton != null) updateButton.visible = false;
                return;
            }

            boolean hasUrl =
                mod.getUpdateInfo() instanceof UpdateInfoExtension uie
                && uie.getDownloadUrl() != null;
            
            if (updateButton != null) {
                updateButton.visible = hasUrl || ext.isUpdateDownloaded();
                
                int urlButtonWidths = this.paneWidth / 2 - 2;
                int cappedButtonWidth = Math.min(urlButtonWidths, 200);
                int issuesButtonX = this.rightPaneX + urlButtonWidths + 4 + (urlButtonWidths / 2) - (cappedButtonWidth / 2);
                int issuesRightAlign = issuesButtonX + cappedButtonWidth;
                boolean configVisible = this.configureButton != null && this.configureButton.visible;
                
                if (configVisible) {
                    updateButton.setX(issuesRightAlign - 20 - updateButton.getWidth() - 2);
                } else {
                    updateButton.setX(issuesRightAlign - updateButton.getWidth());
                }

                if (ext.isDownloadingUpdate()) {
                    updateButton.setMessage(Text.translatable("modmenu.update.state.updating"));
                    updateButton.active = false;
                } else if (ext.isUpdateDownloaded()) {
                    if (updateButton.active || !updateButton.getMessage().equals(Text.translatable("modmenu.update.state.updated"))) {
                        updateButton.setMessage(Text.translatable("modmenu.update.state.updated"));
                        updateButton.active = false;
                        this.descriptionListWidget.updateSelectedMod(mod);
                    }
                } else {
                    updateButton.setMessage(Text.translatable("modmenu.update.button.update"));
                    updateButton.active = mod.hasUpdate();
                }
            }
        } else {
            if (updateButton != null) updateButton.visible = false;
        }
    }
}