package io.github.sapporo1101.appgen.api.caps;

import appeng.api.behaviors.GenericInternalInventory;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public interface IGenericInternalInvHost {

    default GenericInternalInventory getGenericInv(@Nullable Direction ignoredSide) {
        return this.getGenericInv();
    }

    GenericInternalInventory getGenericInv();
}
