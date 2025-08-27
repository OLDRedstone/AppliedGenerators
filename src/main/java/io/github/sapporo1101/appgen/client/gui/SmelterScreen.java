package io.github.sapporo1101.appgen.client.gui;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import io.github.sapporo1101.appgen.menu.SmelterMenu;
import io.github.sapporo1101.appgen.util.CommaSeparator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SmelterScreen extends UpgradeableScreen<SmelterMenu> {

    private final ProgressBar pb;

    public SmelterScreen(SmelterMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.pb = new ProgressBar(this.menu, style.getImage("progressBar"), ProgressBar.Direction.VERTICAL);
        widgets.add("progressBar", pb);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.pb.setFullMsg(Component.translatable(
                "gui.appgen.smelter.progress",
                CommaSeparator.FORMATTER.format(this.menu.progress),
                CommaSeparator.FORMATTER.format(this.menu.maxProgress)
        ));
    }
}
