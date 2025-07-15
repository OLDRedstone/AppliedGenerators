package io.github.sapporo1101.appgen.recipe;

import appeng.api.stacks.GenericStack;
import com.glodblock.github.glodium.recipe.stack.IngredientStack;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.pedroksl.advanced_ae.AdvancedAE;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class GenesisSynthesizerRecipeBuilder {
    private final List<IngredientStack.Item> inputs = new ArrayList<>();
    private IngredientStack.Fluid fluid = null;
    private final int energy;
    private final GenericStack output;

    public GenesisSynthesizerRecipeBuilder(@NotNull GenericStack output, int energy) {
        this.output = output;
        this.energy = energy;
    }

    public static GenesisSynthesizerRecipeBuilder synthesize(ItemStack stack, int energy) {
        return new GenesisSynthesizerRecipeBuilder(Objects.requireNonNull(GenericStack.fromItemStack(stack)), energy);
    }

    public static GenesisSynthesizerRecipeBuilder synthesize(ItemLike stack, int energy) {
        return synthesize(new ItemStack(stack), energy);
    }

    public static GenesisSynthesizerRecipeBuilder synthesize(ItemLike stack, int count, int energy) {
        return synthesize(new ItemStack(stack, count), energy);
    }

    public static GenesisSynthesizerRecipeBuilder synthesize(FluidStack stack, int energy) {
        return new GenesisSynthesizerRecipeBuilder(Objects.requireNonNull(GenericStack.fromFluidStack(stack)), energy);
    }

    public static GenesisSynthesizerRecipeBuilder synthesize(Fluid stack, int energy) {
        return synthesize(new FluidStack(stack, 1000), energy);
    }

    public static GenesisSynthesizerRecipeBuilder synthesize(Fluid stack, int count, int energy) {
        return synthesize(new FluidStack(stack, count), energy);
    }

    public GenesisSynthesizerRecipeBuilder fluid(FluidStack fluid) {
        this.fluid = IngredientStack.of(fluid);
        return this;
    }

    public GenesisSynthesizerRecipeBuilder fluid(Fluid fluid, int amount) {
        this.fluid = IngredientStack.of(new FluidStack(fluid, amount));
        return this;
    }

    public GenesisSynthesizerRecipeBuilder fluid(TagKey<Fluid> tag, int amount) {
        this.fluid = IngredientStack.of(FluidIngredient.tag(tag), amount);
        return this;
    }

    public GenesisSynthesizerRecipeBuilder input(ItemStack item) {
        this.inputs.add(IngredientStack.of(item));
        return this;
    }

    public GenesisSynthesizerRecipeBuilder input(ItemLike item) {
        this.inputs.add(IngredientStack.of(new ItemStack(item)));
        return this;
    }

    public GenesisSynthesizerRecipeBuilder input(ItemLike item, int count) {
        this.inputs.add(IngredientStack.of(new ItemStack(item, count)));
        return this;
    }

    public GenesisSynthesizerRecipeBuilder input(TagKey<Item> tag) {
        this.inputs.add(IngredientStack.of(Ingredient.of(tag), 1));
        return this;
    }

    public GenesisSynthesizerRecipeBuilder input(TagKey<Item> tag, int count) {
        this.inputs.add(IngredientStack.of(Ingredient.of(tag), count));
        return this;
    }

    public void save(RecipeOutput consumer, ResourceLocation id) {
        var recipe = new GenesisSynthesizerRecipe(this.output, this.inputs, this.fluid, this.energy);
        consumer.accept(id, recipe, null);
    }

    public void save(RecipeOutput consumer, String id) {
        this.save(consumer, AdvancedAE.makeId(id));
    }
}
