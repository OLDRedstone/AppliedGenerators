package io.github.sapporo1101.appgen.datagen;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.datagen.providers.tags.ConventionTags;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipeBuilder;
import appeng.recipes.transform.TransformCircumstance;
import appeng.recipes.transform.TransformRecipeBuilder;
import com.glodblock.github.appflux.common.AFSingletons;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.recipe.GenesisSynthesizerRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AGRecipeProvider extends RecipeProvider {
    public AGRecipeProvider(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        super(out, lookup);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {

        // Crafting Table Recipes

        // Pattern Buffer
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
                .save(recipeOutput, AppliedGenerators.id("crafting/pattern_buffer"));
        // Ember Block
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AGSingletons.EMBER_BLOCK)
                .pattern("EE")
                .pattern("EE")
                .define('E', AGSingletons.EMBER_CRYSTAL)
                .unlockedBy("has_ember_crystal", has(AGSingletons.EMBER_CRYSTAL))
                .save(recipeOutput, AppliedGenerators.id("crafting/ember_block"));

        // Inscriber Recipes

        // Origination Circuit
        InscriberRecipeBuilder.inscribe(AGSingletons.EMBER_CRYSTAL, AGSingletons.ORIGINATION_PRINT, 1)
                .setTop(Ingredient.of(AGSingletons.ORIGINATION_PRESS))
                .setMode(InscriberProcessType.INSCRIBE)
                .save(recipeOutput, AppliedGenerators.id("inscriber/printed_origination_processor"));
        // Origination Processor
        InscriberRecipeBuilder.inscribe(ConventionTags.REDSTONE, AGSingletons.ORIGINATION_PROCESSOR, 1)
                .setTop(Ingredient.of(AGSingletons.ORIGINATION_PRINT))
                .setBottom(Ingredient.of(AEItems.SILICON_PRINT))
                .setMode(InscriberProcessType.PRESS)
                .save(recipeOutput, AppliedGenerators.id("inscriber/origination_processor"));
        // Inscriber Origination Press (Duplicate)
        InscriberRecipeBuilder.inscribe(Items.IRON_BLOCK, AGSingletons.ORIGINATION_PRESS, 1)
                .setTop(Ingredient.of(AGSingletons.ORIGINATION_PRESS))
                .setMode(InscriberProcessType.INSCRIBE)
                .save(recipeOutput, AppliedGenerators.id("inscriber/origination_press_duplicate"));

        // Genesis Synthesizer Recipes

        // Inscriber Origination Press
        GenesisSynthesizerRecipeBuilder.synthesize(AGSingletons.ORIGINATION_PRESS, 1, 1000000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED, 4)
                .input(AEItems.CALCULATION_PROCESSOR_PRESS)
                .input(Items.MAGMA_CREAM, 4)
                .input(AGSingletons.EMBER_CRYSTAL, 4)
                .fluid(Fluids.LAVA, 4000)
                .save(recipeOutput, AppliedGenerators.id("synthesizer/origination_press"));
        // Damaged Ember Budding
        GenesisSynthesizerRecipeBuilder.synthesize(AGSingletons.EMBER_BUDDING_DAMAGED, 1, 100000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED, 2)
                .input(AGSingletons.EMBER_BLOCK)
                .input(AGSingletons.EMBER_CRYSTAL)
                .fluid(Fluids.LAVA, 1000)
                .save(recipeOutput, AppliedGenerators.id("synthesizer/ember_budding_damaged"));
        // Chipped Ember Budding
        GenesisSynthesizerRecipeBuilder.synthesize(AGSingletons.EMBER_BUDDING_CHIPPED, 1, 100000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED, 2)
                .input(AGSingletons.EMBER_BUDDING_DAMAGED)
                .input(AGSingletons.EMBER_CRYSTAL)
                .fluid(Fluids.LAVA, 1000)
                .save(recipeOutput, AppliedGenerators.id("synthesizer/ember_budding_chipped"));
        // Flawed Ember Budding
        GenesisSynthesizerRecipeBuilder.synthesize(AGSingletons.EMBER_BUDDING_FLAWED, 1, 100000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED, 2)
                .input(AGSingletons.EMBER_BUDDING_CHIPPED)
                .input(AGSingletons.EMBER_CRYSTAL)
                .fluid(Fluids.LAVA, 1000)
                .save(recipeOutput, AppliedGenerators.id("synthesizer/ember_budding_flawed"));
        // Flawless Ember Budding
        GenesisSynthesizerRecipeBuilder.synthesize(AGSingletons.EMBER_BUDDING_FLAWLESS, 1, 1000000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED, 100)
                .input(AGSingletons.EMBER_BUDDING_FLAWED)
                .input(AGSingletons.EMBER_CRYSTAL)
                .fluid(Fluids.LAVA, 10000)
                .save(recipeOutput, AppliedGenerators.id("synthesizer/ember_budding_flawless"));
        // Insulating Resin
        GenesisSynthesizerRecipeBuilder.synthesize(AFSingletons.INSULATING_RESIN, 1, 10000)
                .input(AGSingletons.EMBER_CRYSTAL_CHARGED, 1)
                .input(AEItems.SKY_DUST)
                .input(AEItems.SILICON)
                .input(Items.SLIME_BALL)
                .fluid(Fluids.WATER, 1000)
                .save(recipeOutput, AppliedGenerators.id("synthesizer/insulating_resin"));

        // World Transformation Recipes

        // Ember Crystal (Duplicate)
        TransformRecipeBuilder.transform(
                recipeOutput,
                AppliedGenerators.id("transform/ember_crystal_duplicate"),
                AGSingletons.EMBER_CRYSTAL, 1,
                TransformCircumstance.fluid(FluidTags.LAVA),
                Ingredient.of(AGSingletons.EMBER_DUST),
                Ingredient.of(AEItems.FLUIX_CRYSTAL)
        );
        // Ember Crystal
        TransformRecipeBuilder.transform(
                recipeOutput,
                AppliedGenerators.id("transform/ember_crystal"),
                AGSingletons.EMBER_CRYSTAL, 2,
                TransformCircumstance.fluid(FluidTags.LAVA),
                Ingredient.of(AEItems.FLUIX_CRYSTAL),
                Ingredient.of(Items.GUNPOWDER),
                Ingredient.of(Items.BLAZE_POWDER)
        );
    }
}
