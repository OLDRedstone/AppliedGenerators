package io.github.sapporo1101.appgen.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.glodblock.github.extendedae.client.button.ActionEPPButton;
import com.glodblock.github.extendedae.client.button.EPPIcon;
import com.glodblock.github.extendedae.client.gui.subgui.OutputSideConfig;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.CEAEGenericPacket;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.menu.FluxCellMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FluxCellScreen extends AEBaseScreen<FluxCellMenu> {

    public FluxCellScreen(FluxCellMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        ActionEPPButton outputSideBtn = new ActionEPPButton(b -> this.openOutputConfig(), EPPIcon.OUTPUT_SIDES);
        outputSideBtn.setMessage(Component.translatable("gui.extendedae.set_output_sides.open"));
        this.addToLeftToolbar(outputSideBtn);
    }

    private void openOutputConfig() {
        if (this.getMenu().getHost() != null) {
            switchToScreen(new OutputSideConfig<>(
                    this,
                    new ItemStack(AGSingletons.FLUX_CELL),
                    this.getMenu().getHost(),
                    this.getMenu().getOutputSides(),
                    (side, value) -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("set_side", side.getName(), value)))
            );
        }
    }
}
