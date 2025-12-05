package com.udpsendtofailed.modmenu.updater.mixin;

import com.terraformersmc.modmenu.config.option.BooleanConfigOption;
import com.terraformersmc.modmenu.gui.widget.DescriptionListWidget;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.udpsendtofailed.modmenu.updater.api.DescriptionEntryExtension;
import com.udpsendtofailed.modmenu.updater.api.ModExtension;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Mixin(DescriptionListWidget.class)
public abstract class MixinDescriptionListWidget {
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
            
            // 1. Add Spacer (Empty Line)
            addSpacerEntry();

            // 2. Header with Badge (Green)
            // Passing true here triggers setUpdateBadge(true)
            addReflectionEntry(Text.translatable("modmenu.update.state.widget.updated").formatted(Formatting.GREEN), width, true); 
            
            // 3. Body (Gray)
            addReflectionEntry(Text.translatable("modmenu.update.state.widget.restartRequired").formatted(Formatting.GRAY), width, false);

            return false;
        }
        return originalValue;
    }

    // Helper to add an empty line explicitly
    private void addSpacerEntry() {
        try {
            Class<?> entryClass = Class.forName("com.terraformersmc.modmenu.gui.widget.DescriptionListWidget$DescriptionEntry");
            Constructor<?> constructor = entryClass.getDeclaredConstructor(DescriptionListWidget.class, OrderedText.class);
            constructor.setAccessible(true);
            // Create entry with OrderedText.EMPTY
            Object entryInstance = constructor.newInstance((DescriptionListWidget)(Object)this, OrderedText.EMPTY);
            
            injectEntry(entryInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addReflectionEntry(Text text, int width, boolean isBadge) {
        for (OrderedText line : textRenderer.wrapLines(text, width)) {
            try {
                Class<?> entryClass = Class.forName("com.terraformersmc.modmenu.gui.widget.DescriptionListWidget$DescriptionEntry");
                Constructor<?> constructor = entryClass.getDeclaredConstructor(DescriptionListWidget.class, OrderedText.class);
                constructor.setAccessible(true);
                Object entryInstance = constructor.newInstance((DescriptionListWidget)(Object)this, line);

                if (isBadge && entryInstance instanceof DescriptionEntryExtension ext) {
                    ext.setUpdateBadge(true); // This sets updateTextEntry = true
                }

                injectEntry(entryInstance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void injectEntry(Object entryInstance) {
        try {
            Method addEntryMethod = null;
            Class<?> currentClass = DescriptionListWidget.class;
            
            // Search for addEntry method
            while (currentClass != null && addEntryMethod == null) {
                for (Method m : currentClass.getDeclaredMethods()) {
                    if (m.isSynthetic() || m.isBridge()) continue;
                    if (m.getParameterCount() == 1) {
                        Class<?> paramType = m.getParameterTypes()[0];
                        Class<?> returnType = m.getReturnType();
                        // Match method: takes our Entry class, returns int
                        if ((paramType.isInstance(entryInstance) || paramType.isAssignableFrom(entryInstance.getClass())) 
                            && (returnType == int.class || returnType == Integer.class)) {
                            addEntryMethod = m;
                            break;
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            if (addEntryMethod != null) {
                addEntryMethod.setAccessible(true);
                addEntryMethod.invoke(this, entryInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}