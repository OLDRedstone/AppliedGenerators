package io.github.sapporo1101.appgen.util;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.helpers.externalstorage.GenericStackInv;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class CustomStackInv extends GenericStackInv {

    private final Map<Integer, CustomIOFilter> ioFilters;

    public CustomStackInv(Map<Integer, Set<AEKey>> allowedKeys, Map<Integer, CustomIOFilter> ioFilters, @Nullable Runnable listener, GenericStackInv.Mode mode, int size) {
        super(listener, mode, size);
        this.setFilter(
                (slot, key) -> {
                    final Set<AEKey> slotKeys = allowedKeys.get(slot);
                    if (slotKeys.isEmpty()) return true;
                    return allowedKeys.get(slot).contains(key);
                }
        );
        this.ioFilters = ioFilters;
    }

    @Override
    public long insert(int slot, AEKey what, long amount, Actionable mode) {
        final CustomIOFilter ioFilter = ioFilters.get(slot);
        if (ioFilter == CustomIOFilter.NONE || ioFilter == CustomIOFilter.EXTRACT_ONLY) return 0;
        return super.insert(slot, what, amount, mode);
    }

    @Override
    public long extract(int slot, AEKey what, long amount, Actionable mode) {
        final CustomIOFilter ioFilter = ioFilters.get(slot);
        if (ioFilter == CustomIOFilter.NONE || ioFilter == CustomIOFilter.INSERT_ONLY) return 0;
        return super.extract(slot, what, amount, mode);
    }
}