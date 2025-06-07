package io.github.sapporo1101.appgen.client.gui;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import com.glodblock.github.extendedae.client.button.ActionEPPButton;
import com.glodblock.github.extendedae.client.button.EPPIcon;
import com.glodblock.github.extendedae.client.gui.subgui.OutputSideConfig;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.CEAEGenericPacket;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.container.ContainerSingularityGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GuiSingularityGenerator extends UpgradeableScreen<ContainerSingularityGenerator> {
    private final ProgressBar pb;
    private final SettingToggleButton<YesNo> autoExportBtn;
    private final ActionEPPButton outputSideBtn;

    public GuiSingularityGenerator(ContainerSingularityGenerator menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.pb = new ProgressBar(this.menu, style.getImage("progressBar"), ProgressBar.Direction.VERTICAL);
        widgets.add("progressBar", pb);
        this.autoExportBtn = new ServerSettingToggleButton<>(Settings.AUTO_EXPORT, YesNo.NO);
        this.addToLeftToolbar(autoExportBtn);
        this.outputSideBtn = new ActionEPPButton(b -> this.openOutputConfig(), EPPIcon.OUTPUT_SIDES);
        this.outputSideBtn.setMessage(Component.translatable("gui.extendedae.set_output_sides.open"));
        this.addToLeftToolbar(this.outputSideBtn);
    }

    private void openOutputConfig() {
        if (this.getMenu().getHost() != null) {
            switchToScreen(new OutputSideConfig<>(
                    this,
                    new ItemStack(AGSingletons.SINGULARITY_GENERATOR),
                    this.getMenu().getHost(),
                    this.getMenu().getOutputSides(),
                    (side, value) -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("set_side", side.getName(), value)))
            );
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.pb.setFullMsg(Component.translatable("gui.appgen.singularity_generator.progress", this.menu.generatableFE, this.menu.getHost().getFEPerSingularity()));
        this.autoExportBtn.set(getMenu().getAutoExport());
        this.outputSideBtn.setVisibility(this.autoExportBtn.getCurrentValue() == YesNo.YES);
    }
}