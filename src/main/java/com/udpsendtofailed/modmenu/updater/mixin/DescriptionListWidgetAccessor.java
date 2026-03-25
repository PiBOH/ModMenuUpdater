package com.udpsendtofailed.modmenu.updater.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSelectionList.class)
public interface DescriptionListWidgetAccessor {
    @Invoker("addEntry")
    int invokeAddEntry(AbstractSelectionList.Entry<?> entry);
}
