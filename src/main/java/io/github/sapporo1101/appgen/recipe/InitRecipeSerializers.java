package io.github.sapporo1101.appgen.recipe;

import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class InitRecipeSerializers {

    private InitRecipeSerializers() {
    }

    public static void register(Registry<RecipeSerializer<?>> registry) {
        Registry.register(registry, GenesisSynthesizerRecipe.ID, GenesisSynthesizerRecipeSerializer.INSTANCE);
    }
}
