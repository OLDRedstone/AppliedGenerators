package io.github.sapporo1101.appgen.datagen;

import io.github.sapporo1101.appgen.AppliedGenerators;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = AppliedGenerators.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AGDataGen {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(event.includeServer(), new AGRecipeProvider(out, lookup));
        AGBlockTagProvider blockTags = new AGBlockTagProvider(out, lookup, fileHelper);
        AGItemTagProvider itemTags = new AGItemTagProvider(out, lookup, blockTags.contentsGetter(), fileHelper);

        gen.addProvider(event.includeServer(), blockTags);
        gen.addProvider(event.includeServer(), itemTags);
    }
}
