package io.github.sapporo1101.appgen.datagen;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.recipe.GenesisSynthesizerRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AGRecipeProvider extends RecipeProvider {
    public AGRecipeProvider(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        super(out, lookup);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AGSingletons.PATTERN_BUFFER)
                .pattern("IGI")
                .pattern("APF")
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', AEBlocks.QUARTZ_GLASS)
                .define('A', AEItems.ANNIHILATION_CORE)
                .define('P', AEItems.BLANK_PATTERN)
                .define('F', AEItems.FORMATION_CORE)
                .unlockedBy("has_blank_pattern", has(AEItems.BLANK_PATTERN))
                .save(recipeOutput, AppliedGenerators.id("pattern_buffer"));

        GenesisSynthesizerRecipeBuilder.synthesize(AGSingletons.EMBER_BUDDING_DAMAGED, 1, 1000000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED)
                .input(AGSingletons.EMBER_BLOCK)
                .input(AGSingletons.EMBER_CRYSTAL)
                .fluid(Fluids.LAVA, 1000)
                .save(recipeOutput, AppliedGenerators.id("ember_budding_damaged"));
    }
}
