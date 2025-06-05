package io.github.sapporo1101.appgen.util;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.helpers.externalstorage.GenericStackInv;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class CustomStackInv extends GenericStackInv {

    private final CustomIOFilter ioFilter;

    public CustomStackInv(Set<AEKey> allowedKeys, CustomIOFilter ioFilter, @Nullable Runnable listener, GenericStackInv.Mode mode, int size) {
        super(listener, mode, size);
        this.setCapacity(AEKeyType.items(), 0);
        this.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) this.setCapacity(ExternalTypes.GAS, 0);
        if (ExternalTypes.MANA != null) this.setCapacity(ExternalTypes.MANA, 0);
        if (ExternalTypes.FLUX != null) this.setCapacity(ExternalTypes.FLUX, 0);
        if (ExternalTypes.SOURCE != null) this.setCapacity(ExternalTypes.SOURCE, 0);
        this.setFilter((slot, key) -> allowedKeys.contains(key));
        this.ioFilter = ioFilter;
    }

    @Override
    public long insert(int slot, AEKey what, long amount, Actionable mode) {
        if (this.ioFilter == CustomIOFilter.NONE || this.ioFilter == CustomIOFilter.EXTRACT_ONLY) return 0;
        return super.insert(slot, what, amount, mode);
    }

    @Override
    public long extract(int slot, AEKey what, long amount, Actionable mode) {
        if (this.ioFilter == CustomIOFilter.NONE || this.ioFilter == CustomIOFilter.INSERT_ONLY) return 0;
        return super.extract(slot, what, amount, mode);
    }
}