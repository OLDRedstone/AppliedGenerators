package io.github.sapporo1101.appgen.util;

import io.github.sapporo1101.appgen.AppliedGenerators;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class AGTags {
    public static final TagKey<Item> GENERATOR = TagKey.create(Registries.ITEM, AppliedGenerators.id("generator"));
}
