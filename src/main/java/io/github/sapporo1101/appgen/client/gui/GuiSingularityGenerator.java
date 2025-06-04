package io.github.sapporo1101.appgen.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import io.github.sapporo1101.appgen.container.ContainerSingularityGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiSingularityGenerator extends AEBaseScreen<ContainerSingularityGenerator> {
    private final ProgressBar pb;

    public GuiSingularityGenerator(ContainerSingularityGenerator menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.pb = new ProgressBar(this.menu, style.getImage("progressBar"), ProgressBar.Direction.VERTICAL);
        widgets.add("progressBar", pb);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.pb.setFullMsg(Component.translatable("gui.appgen.singularity_generator.progress", this.menu.generatableFE));
    }
}