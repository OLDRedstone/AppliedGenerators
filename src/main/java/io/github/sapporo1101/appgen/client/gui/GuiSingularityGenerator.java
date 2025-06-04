package io.github.sapporo1101.appgen.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import io.github.sapporo1101.appgen.container.ContainerSingularityGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiSingularityGenerator extends AEBaseScreen<ContainerSingularityGenerator> {
    public GuiSingularityGenerator(ContainerSingularityGenerator menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}