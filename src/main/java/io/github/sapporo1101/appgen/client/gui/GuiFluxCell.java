package io.github.sapporo1101.appgen.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import io.github.sapporo1101.appgen.container.ContainerFluxCell;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiFluxCell extends AEBaseScreen<ContainerFluxCell> {
    public GuiFluxCell(ContainerFluxCell menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
