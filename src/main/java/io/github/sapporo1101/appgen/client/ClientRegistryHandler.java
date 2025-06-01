package io.github.sapporo1101.appgen.client;

import appeng.init.client.InitScreens;
import io.github.sapporo1101.appgen.client.gui.GuiFluxCell;
import io.github.sapporo1101.appgen.container.ContainerFluxCell;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientRegistryHandler {
    public static final ClientRegistryHandler INSTANCE = new ClientRegistryHandler();

    @SubscribeEvent
    public void registerGui(RegisterMenuScreensEvent event) {
        InitScreens.register(event, ContainerFluxCell.TYPE, GuiFluxCell::new, "/screens/flux_cell.json");
    }
}
