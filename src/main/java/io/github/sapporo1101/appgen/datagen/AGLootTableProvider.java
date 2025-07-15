package io.github.sapporo1101.appgen.datagen;

import io.github.sapporo1101.appgen.common.AGRegistryHandler;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.common.blocks.interfaces.ISpecialDrop;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class AGLootTableProvider extends LootTableProvider {

    public AGLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, Collections.emptySet(), Collections.singletonList(new LootTableProvider.SubProviderEntry(AGSubProvider::new, LootContextParamSets.BLOCK)), provider);
    }


    public static class AGSubProvider extends BlockLootSubProvider {


        protected AGSubProvider(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, provider);
        }

        @Override
        protected void generate() {
            for (var block : AGRegistryHandler.INSTANCE.getBlocks()) {
                if (!(block instanceof ISpecialDrop)) {
                    add(block, createSingleItemTable(block));
                }
            }
            add(AGSingletons.EMBER_BUD_SMALL, createSingleItemTableWithSilkTouch(AGSingletons.EMBER_BUD_SMALL, AGSingletons.EMBER_DUST));
            add(AGSingletons.EMBER_BUD_MEDIUM, createSingleItemTableWithSilkTouch(AGSingletons.EMBER_BUD_MEDIUM, AGSingletons.EMBER_DUST));
            add(AGSingletons.EMBER_BUD_LARGE, createSingleItemTableWithSilkTouch(AGSingletons.EMBER_BUD_LARGE, AGSingletons.EMBER_DUST));
            add(AGSingletons.EMBER_CLUSTER, createSilkTouchDispatchTable(AGSingletons.EMBER_CLUSTER,
                    LootItem.lootTableItem(AGSingletons.EMBER_CRYSTAL)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                            .apply(ApplyBonusCount.addUniformBonusCount(getEnchantment(Enchantments.FORTUNE)))
                            .apply(ApplyExplosionDecay.explosionDecay()))
            );
            add(AGSingletons.EMBER_BUDDING_DAMAGED, createSingleItemTableWithSilkTouch(AGSingletons.EMBER_BUDDING_DAMAGED, AGSingletons.EMBER_BLOCK));
            add(AGSingletons.EMBER_BUDDING_CHIPPED, createSingleItemTableWithSilkTouch(AGSingletons.EMBER_BUDDING_CHIPPED, AGSingletons.EMBER_BUDDING_DAMAGED));
            add(AGSingletons.EMBER_BUDDING_FLAWED, createSingleItemTableWithSilkTouch(AGSingletons.EMBER_BUDDING_FLAWED, AGSingletons.EMBER_BUDDING_CHIPPED));
            add(AGSingletons.EMBER_BUDDING_FLAWLESS, createSingleItemTable(AGSingletons.EMBER_BUDDING_FLAWED));
        }

        @Override
        public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
            generate();
            map.forEach(writer);
        }

        @SuppressWarnings("SameParameterValue")
        protected final Holder<Enchantment> getEnchantment(ResourceKey<Enchantment> key) {
            return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }
    }
}