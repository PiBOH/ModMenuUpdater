package com.udpsendtofailed.modmenu.updater.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntryListWidget.class)
public interface DescriptionListWidgetAccessor {
    @Invoker("addEntry")
    int invokeAddEntry(EntryListWidget.Entry<?> entry);
}
