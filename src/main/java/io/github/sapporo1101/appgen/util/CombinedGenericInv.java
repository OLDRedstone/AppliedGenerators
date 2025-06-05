package io.github.sapporo1101.appgen.util;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CombinedGenericInv implements GenericInternalInventory {
    final ArrayList<GenericStackInv> invs;

    public CombinedGenericInv(GenericStackInv... invs) {
        this.invs = new ArrayList<>(List.of(invs));
    }

    public GenericStackInv getInvFromSlot(int slot) {
        for (GenericStackInv inv : this.invs) {
            if (slot < inv.size()) return inv;
            slot -= inv.size();
        }
        return null;
    }

    public int getInvIndexFromSlot(int slot) {
        for (GenericStackInv inv : this.invs) {
            if (slot < inv.size()) return slot;
            slot -= inv.size();
        }
        return -1; // Invalid index
    }

    public GenericStackInv getInv(int index) {
        if (index < 0 || index >= this.invs.size()) return null;
        return this.invs.get(index);
    }

    @Override
    public int size() {
        return this.invs.stream().mapToInt(GenericStackInv::size).sum();
    }

    @Override
    public @Nullable GenericStack getStack(int slot) {
        return getInvFromSlot(slot) != null ? getInvFromSlot(slot).getStack(getInvIndexFromSlot(slot)) : null;
    }

    @Override
    public @Nullable AEKey getKey(int slot) {
        return getInvFromSlot(slot) != null ? getInvFromSlot(slot).getKey(getInvIndexFromSlot(slot)) : null;
    }

    @Override
    public long getAmount(int slot) {
        return getInvFromSlot(slot) != null ? getInvFromSlot(slot).getAmount(getInvIndexFromSlot(slot)) : 0;
    }

    @Override
    public long getMaxAmount(AEKey aeKey) {
        // this returns average max amount with empty slots considered
        int canInsertSlot = 0;
        for (int i = 0; i < this.size(); i++) {
            if (this.isAllowedIn(i, aeKey) || this.getKey(i) == null) canInsertSlot++;
        }
        System.out.println("getMaxAmount called with key: " + aeKey + ", maxAmount: " + invs.stream().mapToLong(inv -> inv.getMaxAmount(aeKey)).sum() + ", fixed: " + (invs.stream().mapToLong(inv -> inv.getMaxAmount(aeKey)).sum() / canInsertSlot));
        return invs.stream().mapToLong(inv -> inv.getMaxAmount(aeKey)).sum() / canInsertSlot;
    }

    @Override
    public long getCapacity(AEKeyType aeKeyType) {
        return invs.stream().mapToLong(inv -> inv.getCapacity(aeKeyType)).sum();
    }

    @Override
    public boolean canInsert() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public void setStack(int slot, @Nullable GenericStack genericStack) {
        this.getInvFromSlot(slot).setStack(getInvIndexFromSlot(slot), genericStack);
    }

    @Override
    public boolean isSupportedType(AEKeyType aeKeyType) {
        return invs.stream().anyMatch(inv -> inv.isSupportedType(aeKeyType));
    }

    @Override
    public boolean isAllowedIn(int slot, AEKey aeKey) {
        return this.getInvFromSlot(slot).isAllowedIn(getInvIndexFromSlot(slot), aeKey);
    }

    @Override
    public long insert(int slot, AEKey aeKey, long l, Actionable actionable) {
        return this.getInvFromSlot(slot) != null ? this.getInvFromSlot(slot).insert(getInvIndexFromSlot(slot), aeKey, l, actionable) : 0;
    }

    @Override
    public long extract(int slot, AEKey aeKey, long l, Actionable actionable) {
        return this.getInvFromSlot(slot) != null ? this.getInvFromSlot(slot).extract(getInvIndexFromSlot(slot), aeKey, l, actionable) : 0;
    }

    @Override
    public void beginBatch() {
        for (GenericStackInv inv : this.invs) inv.beginBatch();
    }

    @Override
    public void endBatch() {
        for (GenericStackInv inv : this.invs) inv.endBatch();
    }

    @Override
    public void endBatchSuppressed() {
        for (GenericStackInv inv : this.invs) inv.endBatchSuppressed();
    }

    @Override
    public void onChange() {
        for (GenericStackInv inv : this.invs) inv.onChange();
    }

    public void clear() {
        for (GenericStackInv inv : this.invs) inv.clear();
    }
}
