package io.github.sapporo1101.appgen.xmod.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import io.github.sapporo1101.appgen.AppliedGenerators;
import net.minecraft.network.chat.Component;

public class AGRecipeCategory extends EmiRecipeCategory {
    private final Component name;

    public AGRecipeCategory(String id, EmiRenderable icon, Component name) {
        super(AppliedGenerators.id(id), icon);
        this.name = name;
    }

    @Override
    public Component getName() {
        return this.name;
    }
}
