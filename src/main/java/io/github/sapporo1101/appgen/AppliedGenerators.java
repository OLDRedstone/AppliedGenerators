package io.github.sapporo1101.appgen;

import appeng.init.InitCapabilityProviders;
import io.github.sapporo1101.appgen.client.ClientRegistryHandler;
import io.github.sapporo1101.appgen.common.AGRegistryHandler;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.recipe.InitRecipeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AppliedGenerators.MODID)
public class AppliedGenerators {
    public static final String MODID = "appgen";
    public static final Logger LOGGER = LogManager.getLogger();
    public static AppliedGenerators INSTANCE;

    public AppliedGenerators(IEventBus bus, ModContainer container) {
        assert INSTANCE == null;
        INSTANCE = this;
        if (!container.getModId().equals(MODID)) {
            throw new IllegalArgumentException("Invalid ID: " + MODID);
        }
        bus.addListener((RegisterEvent e) -> {
            if (e.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
                AGRegistryHandler.INSTANCE.registerTab(e.getRegistry(Registries.CREATIVE_MODE_TAB));
            } else if (e.getRegistryKey().equals(Registries.BLOCK)) {
                AGSingletons.init(AGRegistryHandler.INSTANCE);
                AGRegistryHandler.INSTANCE.runRegister();
            }
        });
        if (FMLEnvironment.dist.isClient()) {
            bus.register(ClientRegistryHandler.INSTANCE);
        }

        bus.addListener(this::commonSetup);
        bus.addListener(InitCapabilityProviders::register);
        bus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey() == Registries.RECIPE_TYPE) {
                InitRecipeTypes.init(event.getRegistry(Registries.RECIPE_TYPE));
            }
        });

        bus.register(AGRegistryHandler.INSTANCE);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        AGRegistryHandler.INSTANCE.onInit();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    public static ResourceLocation id(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }
}
