package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatternBufferBlockEntity extends AEBaseBlockEntity implements InternalInventoryHost {

    private final PatternBufferInv storageInv = new PatternBufferInv(this::onStorageChanged, 36);
    private final AppEngInternalInventory patternInv = new AppEngInternalInventory(this, 1);

    private ArrayList<List<GenericStack>> patternInputs = new ArrayList<>();

    public PatternBufferBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(PatternBufferBlockEntity.class, PatternBufferBlockEntity::new, AGSingletons.PATTERN_BUFFER), pos, blockState);
    }

    private void onStorageChanged() {
        setChanged();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        this.updatePattern();
        this.updateCapacity();
        InternalInventoryHost.super.onChangeInventory(inv, slot);
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.storageInv.size(); index++) {
            var stack = this.storageInv.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
        drops.add(patternInv.getStackInSlot(0));
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        for (int i = 0; i < this.storageInv.size(); i++) {
            GenericStackInv inv = this.storageInv.getInv(i);
            inv.writeToChildTag(data, "buffer_" + i, registries);
        }
        InternalInventory inv = this.patternInv;
        if (inv != InternalInventory.empty()) {
            final CompoundTag opt = new CompoundTag();
            ItemStack patternStack = inv.getStackInSlot(0);
            opt.put("item", patternStack.saveOptional(registries));
            data.put("inv", opt);
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        for (int i = 0; i < this.storageInv.size(); i++) {
            GenericStackInv inv = this.storageInv.getInv(i);
            inv.readFromChildTag(data, "buffer_" + i, registries);
        }
        InternalInventory inv = this.getPatternInv();
        if (inv != InternalInventory.empty()) {
            CompoundTag opt = data.getCompound("inv");
            CompoundTag item = opt.getCompound("item");
            inv.setItemDirect(0, ItemStack.parseOptional(registries, item));
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.storageInv.clear();
    }

    private void updatePattern() {
        System.out.println("PatternBuffer updatePattern called.");
        this.patternInputs = new ArrayList<>();
        ItemStack patternStack = patternInv.getStackInSlot(0);
        if (patternStack != null) {
            IPatternDetails patternDetails = PatternDetailsHelper.decodePattern(patternStack, this.level);
            System.out.println("PatternBuffer found pattern: " + patternDetails);
            if (patternDetails == null) return;
            for (IPatternDetails.IInput input : patternDetails.getInputs()) {
                System.out.println("PatternBuffer found input: " + input);
                List<GenericStack> inputStack = Arrays.stream(input.getPossibleInputs()).map(s -> new GenericStack(s.what(), s.amount() * input.getMultiplier())).toList();
                System.out.println("PatternBuffer found inputStack: " + inputStack + ", amount:" + inputStack.stream().mapToLong(GenericStack::amount));
                this.patternInputs.add(inputStack);
            }
        }
        System.out.println("PatternBuffer updated: " + patternInputs.size() + " inputs found.");
    }

    private boolean isValidKey(int slot, AEKey key) {
        if (slot < 0 || slot >= patternInputs.size()) return false;
        List<GenericStack> inputs = patternInputs.get(slot);
        for (GenericStack input : inputs) {
            if (key.matches(input)) {
                System.out.println("valid key found: " + key);
                return true;
            }
        }
        return false;
    }

    private void updateCapacity() {
        for (int i = 0; i < storageInv.size(); i++) {
            GenericStackInv inv = storageInv.getInv(i);
            inv.setCapacity(AEKeyType.items(), 0);
            inv.setCapacity(AEKeyType.fluids(), 0);
            inv.setCapacity(ExternalTypes.FLUX, 0);
            inv.setCapacity(ExternalTypes.GAS, 0);
            inv.setCapacity(ExternalTypes.MANA, 0);
            inv.setCapacity(ExternalTypes.SOURCE, 0);
            List<GenericStack> possibleInputs;
            try {
                possibleInputs = patternInputs.get(i);
            } catch (Exception e) {
                return;
            }
            for (GenericStack input : possibleInputs) {
                AEKeyType keyType = input.what().getType();
                this.storageInv.setCapacity(i, keyType, Math.max(inv.getCapacity(keyType), input.amount()));
            }
        }
    }

    public PatternBufferInv getStorageInv() {
        return storageInv;
    }

    public InternalInventory getPatternInv() {
        return patternInv;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        this.saveChanges();
    }

    @SuppressWarnings("UnstableApiUsage")
    public class PatternBufferInv implements GenericInternalInventory {
        final ArrayList<GenericStackInv> invs = new ArrayList<>();

        public PatternBufferInv(Runnable runnable, int size) {
            for (int i = 0; i < size; i++) invs.add(new PatternBufferSlotInv(runnable, i));
        }

        public GenericStackInv getInv(int slot) {
            return this.invs.get(slot);
        }

        @Override
        public int size() {
            return this.invs.size();
        }

        @Override
        public @Nullable GenericStack getStack(int slot) {
            return this.invs.get(slot) != null ? this.invs.get(slot).getStack(0) : null;
        }

        @Override
        public @Nullable AEKey getKey(int slot) {
            return this.invs.get(slot) != null ? this.invs.get(slot).getKey(0) : null;
        }

        @Override
        public long getAmount(int slot) {
            return this.invs.get(slot) != null ? this.invs.get(slot).getAmount(0) : 0;
        }

        @Override
        public long getMaxAmount(AEKey aeKey) {
            return this.invs.stream().mapToLong(inv -> inv.getMaxAmount(aeKey)).sum();
        }

        @Override
        public long getCapacity(AEKeyType aeKeyType) {
            if (aeKeyType == AEKeyType.items()) return 1024;
            if (aeKeyType == AEKeyType.fluids()) return 1024;
            if (aeKeyType == ExternalTypes.FLUX) return 1048576;
            if (aeKeyType == ExternalTypes.GAS) return 1024;
            if (aeKeyType == ExternalTypes.MANA) return 1000;
            if (aeKeyType == ExternalTypes.SOURCE) return 1000;
            return 0;
        }

        public void setCapacity(int slot, AEKeyType aeKeyType, long capacity) {
            if (slot < 0 || slot >= this.invs.size()) return;
            capacity = Math.min(capacity, this.getCapacity(aeKeyType));
            this.invs.get(slot).setCapacity(aeKeyType, capacity);
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
            System.out.println("PatternBuffer setStack called for slot: " + slot + ", stack: " + genericStack);
            if (slot < 0 || slot >= this.invs.size()) return;
            if (genericStack != null && !isAllowedIn(slot, genericStack.what())) return;
            System.out.println("PatternBuffer setStack setting stack: " + genericStack);
            this.invs.get(slot).setStack(0, genericStack);
        }

        @Override
        public boolean isSupportedType(AEKeyType aeKeyType) {
            return invs.stream().anyMatch(inv -> inv.isSupportedType(aeKeyType));
        }

        @Override
        public boolean isAllowedIn(int slot, AEKey aeKey) {
            System.out.println("PatternBuffer isAllowedIn called for slot: " + slot + ", key: " + aeKey + ", valid: " + isValidKey(slot, aeKey));
            return isValidKey(slot, aeKey);
        }

        @Override
        public long insert(int slot, AEKey aeKey, long l, Actionable actionable) {
            System.out.println("PatternBuffer insert called for slot: " + slot + ", key: " + aeKey + ", amount: " + l);
            if (!this.isAllowedIn(slot, aeKey)) return 0;
            System.out.println("PatternBuffer inserting into slot: " + slot);
            return this.invs.get(slot) != null ? this.invs.get(slot).insert(0, aeKey, l, actionable) : 0;
        }

        @Override
        public long extract(int slot, AEKey aeKey, long l, Actionable actionable) {
            return this.invs.get(slot) != null ? this.invs.get(slot).extract(0, aeKey, l, actionable) : 0;
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

    private class PatternBufferSlotInv extends GenericStackInv {
        private final int index;

        public PatternBufferSlotInv(Runnable runnable, int index) {
            super(runnable, 1);
            this.index = index;
        }

        @Override
        public boolean isAllowedIn(int slot, AEKey what) {
            System.out.println("PatternBufferSlotInv isAllowedIn called for slot: " + slot + ", key: " + what);
            return isValidKey(this.index, what);
        }
    }
}
