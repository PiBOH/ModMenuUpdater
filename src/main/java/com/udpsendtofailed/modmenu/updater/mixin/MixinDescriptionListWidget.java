package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.config.option.BooleanConfigOption;
import com.terraformersmc.modmenu.gui.widget.DescriptionListWidget;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.udpsendtofailed.modmenu.updater.api.DescriptionEntryExtension;
import com.udpsendtofailed.modmenu.updater.api.ModExtension;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Constructor;

@Mixin(DescriptionListWidget.class)
public abstract class MixinDescriptionListWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mod Menu Updater");

    @Shadow private Mod selectedMod;
    @Shadow private TextRenderer textRenderer;
    @Shadow public abstract int getRowWidth();

    @Redirect(
        method = "rebuildUI", 
        at = @At(
            value = "INVOKE", 
            target = "Lcom/terraformersmc/modmenu/config/option/BooleanConfigOption;getValue()Z"
        )
    )
    private boolean interceptUpdateCheck(BooleanConfigOption instance) {
        boolean originalValue = instance.getValue();
        if (!originalValue) return false;

        if (selectedMod instanceof ModExtension ext && ext.isUpdateDownloaded()) {
            int width = getRowWidth() - 5;

            addDescriptionEntry(OrderedText.EMPTY, false);

            for (OrderedText line : textRenderer.wrapLines(
                    Text.translatable("modmenu.update.state.widget.updated").formatted(Formatting.GREEN), width)) {
                addDescriptionEntry(line, true);
            }

            for (OrderedText line : textRenderer.wrapLines(
                    Text.translatable("modmenu.update.state.widget.restartRequired").formatted(Formatting.GRAY), width)) {
                addDescriptionEntry(line, false);
            }

            return false;
        }
        return originalValue;
    }

    @SuppressWarnings("unchecked")
    private void addDescriptionEntry(OrderedText text, boolean isBadge) {
        try {
            DescriptionListWidget self = (DescriptionListWidget) (Object) this;

            Class<?> entryClass = Class.forName(
                    "com.terraformersmc.modmenu.gui.widget.DescriptionListWidget$DescriptionEntry");
            Constructor<?> ctor = entryClass.getDeclaredConstructor(DescriptionListWidget.class, OrderedText.class);
            ctor.setAccessible(true);
            var entry = (EntryListWidget.Entry<?>) ctor.newInstance(self, text);

            if (isBadge && entry instanceof DescriptionEntryExtension ext) {
                ext.setUpdateBadge(true);
            }

            ((DescriptionListWidgetAccessor) self).invokeAddEntry(entry);
        } catch (Exception e) {
            LOGGER.error("Failed to add description entry", e);
        }
    }
}