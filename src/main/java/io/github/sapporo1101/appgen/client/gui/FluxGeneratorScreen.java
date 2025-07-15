package io.github.sapporo1101.appgen.client.gui;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.core.definitions.AEItems;
import com.glodblock.github.extendedae.client.button.ActionEPPButton;
import com.glodblock.github.extendedae.client.button.EPPIcon;
import com.glodblock.github.extendedae.client.gui.subgui.OutputSideConfig;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.CEAEGenericPacket;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.menu.FluxGeneratorMenu;
import io.github.sapporo1101.appgen.util.CommaSeparator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.api.AAESettings;
import net.pedroksl.advanced_ae.client.gui.widgets.AAEServerSettingToggleButton;

public class FluxGeneratorScreen extends UpgradeableScreen<FluxGeneratorMenu> {
    private final ServerSettingToggleButton<RedstoneMode> redstoneMode;
    private final AAEServerSettingToggleButton<YesNo> meExportBtn;
    private final ActionEPPButton outputSideBtn;

    public FluxGeneratorScreen(FluxGeneratorMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.meExportBtn = new AAEServerSettingToggleButton<>(AAESettings.ME_EXPORT, YesNo.NO);
        this.addToLeftToolbar(meExportBtn);
        this.outputSideBtn = new ActionEPPButton(b -> this.openOutputConfig(), EPPIcon.OUTPUT_SIDES);
        this.outputSideBtn.setMessage(Component.translatable("gui.extendedae.set_output_sides.open"));
        this.addToLeftToolbar(this.outputSideBtn);
        this.redstoneMode = new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        addToLeftToolbar(this.redstoneMode);
    }

    private void openOutputConfig() {
        if (this.getMenu().getHost() != null) {
            switchToScreen(new OutputSideConfig<>(
                    this,
                    new ItemStack(AGSingletons.SINGULARITY_GENERATOR_1K),
                    this.getMenu().getHost(),
                    this.getMenu().getOutputSides(),
                    (side, value) -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("set_side", side.getName(), value)))
            );
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.setTextContent("active_info", Component.translatable("gui.appgen.flux_generator.active_info", this.menu.isOn));
        this.setTextContent("generating_info", Component.translatable("gui.appgen.flux_generator.generating_info", CommaSeparator.FORMATTER.format(this.menu.lastGeneratePerTick)));
        this.redstoneMode.set(this.getMenu().getRedStoneMode());
        this.redstoneMode.setVisibility(this.getMenu().hasUpgrade(AEItems.REDSTONE_CARD));
        this.meExportBtn.set(this.getMenu().getMeExport());
        this.outputSideBtn.setVisibility(this.meExportBtn.getCurrentValue() == YesNo.NO);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}