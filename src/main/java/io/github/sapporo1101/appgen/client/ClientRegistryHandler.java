package io.github.sapporo1101.appgen.client;

import appeng.init.client.InitScreens;
import io.github.sapporo1101.appgen.client.gui.GuiFluxCell;
import io.github.sapporo1101.appgen.client.gui.GuiSingularityGenerator;
import io.github.sapporo1101.appgen.container.ContainerFluxCell;
import io.github.sapporo1101.appgen.container.ContainerSingularityGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientRegistryHandler {
    public static final ClientRegistryHandler INSTANCE = new ClientRegistryHandler();

    @SubscribeEvent
    public void registerGui(RegisterMenuScreensEvent event) {
        InitScreens.register(event, ContainerFluxCell.TYPE, GuiFluxCell::new, "/screens/flux_cell.json");
        InitScreens.register(event, ContainerSingularityGenerator.TYPE, GuiSingularityGenerator::new, "/screens/singularity_generator.json");
    }
}
