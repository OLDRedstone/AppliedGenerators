package io.github.sapporo1101.appgen.datagen;

import io.github.sapporo1101.appgen.AppliedGenerators;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AppliedGenerators.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AGDataGen {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent dataEvent) {
        var pack = dataEvent.getGenerator().getVanillaPack(true);
        var file = dataEvent.getExistingFileHelper();
        var lookup = dataEvent.getLookupProvider();
        var blockTagsProvider = pack.addProvider(c -> new AGBlockTagProvider(c, lookup, file));
        pack.addProvider(p -> new AGLootTableProvider(p, lookup));
        pack.addProvider(c -> new AGItemTagsProvider(c, lookup, blockTagsProvider.contentsGetter(), file));
    }

}
