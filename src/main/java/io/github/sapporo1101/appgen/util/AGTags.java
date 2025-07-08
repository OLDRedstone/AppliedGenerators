package io.github.sapporo1101.appgen.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class AGTags {

    public static final TagKey<Item> GOLD_DUST = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts/gold"));
}
