package io.github.sapporo1101.appgen.client;

import appeng.init.client.InitScreens;
import io.github.sapporo1101.appgen.client.gui.FluxCellScreen;
import io.github.sapporo1101.appgen.client.gui.SingularityGeneratorScreen;
import io.github.sapporo1101.appgen.menu.FluxCellMenu;
import io.github.sapporo1101.appgen.menu.SingularityGeneratorMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientRegistryHandler {
    public static final ClientRegistryHandler INSTANCE = new ClientRegistryHandler();

    @SubscribeEvent
    public void registerGui(RegisterMenuScreensEvent event) {
        InitScreens.register(event, FluxCellMenu.TYPE, FluxCellScreen::new, "/screens/flux_cell.json");
        InitScreens.register(event, SingularityGeneratorMenu.TYPE, SingularityGeneratorScreen::new, "/screens/singularity_generator.json");
    }
}
