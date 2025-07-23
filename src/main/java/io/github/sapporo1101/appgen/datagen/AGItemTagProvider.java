package io.github.sapporo1101.appgen.datagen;

import appeng.datagen.providers.tags.ConventionTags;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.util.AGTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class AGItemTagProvider extends ItemTagsProvider {

    public AGItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper fileHelper) {
        super(output, lookupProvider, blockTags, AppliedGenerators.MODID, fileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(AGTags.SINGULARITY_GENERATORS)
                .add(AGSingletons.SINGULARITY_GENERATOR_1K.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_4K.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_16K.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_64K.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_256K.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_1M.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_4M.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_16M.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_64M.asItem())
                .add(AGSingletons.SINGULARITY_GENERATOR_256M.asItem());
        tag(AGTags.FLUX_GENERATORS)
                .add(AGSingletons.FLUX_GENERATOR_1K.asItem())
                .add(AGSingletons.FLUX_GENERATOR_4K.asItem())
                .add(AGSingletons.FLUX_GENERATOR_16K.asItem())
                .add(AGSingletons.FLUX_GENERATOR_64K.asItem())
                .add(AGSingletons.FLUX_GENERATOR_256K.asItem())
                .add(AGSingletons.FLUX_GENERATOR_1M.asItem())
                .add(AGSingletons.FLUX_GENERATOR_4M.asItem())
                .add(AGSingletons.FLUX_GENERATOR_16M.asItem())
                .add(AGSingletons.FLUX_GENERATOR_64M.asItem())
                .add(AGSingletons.FLUX_GENERATOR_256M.asItem());
        tag(AGTags.GENERATORS)
                .addTag(AGTags.SINGULARITY_GENERATORS)
                .addTag(AGTags.FLUX_GENERATORS);
        tag(AGTags.EMBER_DUST)
                .add(AGSingletons.EMBER_DUST);
        tag(AGTags.COPPER_DUST)
                .add(AGSingletons.COPPER_DUST);
        tag(AGTags.GOLD_DUST)
                .add(AGSingletons.GOLD_DUST);
        tag(AGTags.NETHERITE_DUST)
                .add(AGSingletons.NETHERITE_DUST);
        tag(Tags.Items.DUSTS)
                .addTag(AGTags.EMBER_DUST)
                .addTag(AGTags.COPPER_DUST)
                .addTag(AGTags.GOLD_DUST)
                .addTag(AGTags.NETHERITE_DUST);
        tag(AGTags.EMBER_BLOCK)
                .add(AGSingletons.EMBER_BLOCK.asItem());
        tag(Tags.Items.STORAGE_BLOCKS)
                .addTag(AGTags.EMBER_BLOCK);
        tag(AGTags.EMBER_CRYSTAL)
                .add(AGSingletons.EMBER_CRYSTAL);
        tag(Tags.Items.GEMS)
                .addTag(AGTags.EMBER_CRYSTAL);
        tag(ConventionTags.BUDDING_BLOCKS)
                .add(AGSingletons.BUDDING_EMBER_DAMAGED.asItem())
                .add(AGSingletons.BUDDING_EMBER_CHIPPED.asItem())
                .add(AGSingletons.BUDDING_EMBER_FLAWED.asItem())
                .add(AGSingletons.BUDDING_EMBER_FLAWLESS.asItem());
        tag(ConventionTags.BUDS)
                .add(AGSingletons.EMBER_BUD_SMALL.asItem())
                .add(AGSingletons.EMBER_BUD_MEDIUM.asItem())
                .add(AGSingletons.EMBER_BUD_LARGE.asItem());
        tag(ConventionTags.CLUSTERS)
                .add(AGSingletons.EMBER_CLUSTER.asItem());
    }
}
