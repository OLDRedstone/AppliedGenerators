package io.github.sapporo1101.appgen.datagen;

import io.github.sapporo1101.appgen.common.AGRegistryHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class AGLootTableProvider extends LootTableProvider {

    public AGLootTableProvider(PackOutput p, CompletableFuture<HolderLookup.Provider> provider) {
        super(p, Collections.emptySet(), Collections.singletonList(new SubProviderEntry(SubProvider::new, LootContextParamSets.BLOCK)), provider);
    }

    public static class SubProvider extends BlockLootSubProvider {

        protected SubProvider(HolderLookup.Provider provider) {
            super(Collections.emptySet(), FeatureFlagSet.of(), provider);
        }

        @Override
        protected void generate() {
            for (var block : AGRegistryHandler.INSTANCE.getBlocks()) {
                add(block, createSingleItemTable(block));
            }
        }

        @Override
        public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> bi) {
            this.generate();
            for (var e : this.map.entrySet()) {
                bi.accept(e.getKey(), e.getValue());
            }
        }

        protected final Holder<Enchantment> getEnchantment(ResourceKey<Enchantment> key) {
            return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

    }
}
