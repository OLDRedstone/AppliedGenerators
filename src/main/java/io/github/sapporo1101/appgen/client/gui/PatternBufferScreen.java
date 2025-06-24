package io.github.sapporo1101.appgen.client.gui;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import io.github.sapporo1101.appgen.menu.PatternBufferMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PatternBufferScreen extends UpgradeableScreen<PatternBufferMenu> {
    public PatternBufferScreen(PatternBufferMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
