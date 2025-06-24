package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatternBufferBlockEntity extends AEBaseBlockEntity implements InternalInventoryHost, IUpgradeableObject {

    private final PatternBufferInv storageInv = new PatternBufferInv(this::onStorageChanged, 36);
    private final AppEngInternalInventory patternInv = new AppEngInternalInventory(this, 1);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(AGSingletons.PATTERN_BUFFER, 5, this::onUpgradeChanged);

    private final PatternInput patternInput = new PatternInput();

    public PatternBufferBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(PatternBufferBlockEntity.class, PatternBufferBlockEntity::new, AGSingletons.PATTERN_BUFFER), pos, blockState);
    }

    private void onStorageChanged() {
        setChanged();
    }

    private void onUpgradeChanged() {
        saveChanges();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        System.out.println("PatternBuffer onChangeInventory called for slot: " + slot);
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
        for (var upgrade : upgrades) {
            drops.add(upgrade);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        for (int i = 0; i < this.storageInv.size(); i++) {
            GenericStackInv inv = this.storageInv.getInv(i);
            inv.writeToChildTag(data, "buffer_" + i, registries);
        }
        InternalInventory pattern = this.getPatternInv();
        if (pattern != InternalInventory.empty()) {
            final CompoundTag opt = new CompoundTag();
            ItemStack patternStack = pattern.getStackInSlot(0);
            opt.put("item", patternStack.saveOptional(registries));
            data.put("inv", opt);
        }
        this.upgrades.writeToNBT(data, "upgrades", registries);
        this.patternInput.writeToChildTag(data, "pattern", registries);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        for (int i = 0; i < this.storageInv.size(); i++) {
            GenericStackInv inv = this.storageInv.getInv(i);
            inv.readFromChildTag(data, "buffer_" + i, registries);
        }
        InternalInventory pattern = this.getPatternInv();
        if (pattern != InternalInventory.empty()) {
            CompoundTag opt = data.getCompound("inv");
            CompoundTag item = opt.getCompound("item");
            pattern.setItemDirect(0, ItemStack.parseOptional(registries, item));
        }
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.patternInput.readFromChildTag(data, "pattern", registries);
        this.updateCapacity(); // update capacity after loading
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.storageInv.clear();
        this.patternInv.clear();
        this.upgrades.clear();
        this.patternInput.getInputs().clear();
    }

    private void updatePattern() {
        System.out.println("PatternBuffer updatePattern called.");
        this.patternInput.setInputs(new ArrayList<>());
        ItemStack patternStack = patternInv.getStackInSlot(0);
        if (patternStack != null) {
            IPatternDetails patternDetails = PatternDetailsHelper.decodePattern(patternStack, this.level);
            System.out.println("PatternBuffer found pattern: " + patternDetails);
            if (patternDetails == null) return;
            for (IPatternDetails.IInput input : patternDetails.getInputs()) {
                System.out.println("PatternBuffer found input: " + input);
                List<GenericStack> inputStack = Arrays.stream(input.getPossibleInputs()).map(s -> new GenericStack(s.what(), s.amount() * input.getMultiplier())).toList();
                System.out.println("PatternBuffer found inputStack: " + inputStack + ", amount:" + inputStack.stream().mapToLong(GenericStack::amount));
                this.patternInput.getInputs().add(inputStack);
            }
        }
        System.out.println("PatternBuffer updated: " + this.patternInput.getInputs().size() + " inputs found.");
    }

    private boolean isValidKey(int slot, AEKey key) {
        if (slot < 0 || slot >= this.patternInput.getInputs().size()) return false;
        List<GenericStack> inputs = this.patternInput.getInputs().get(slot);
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
            System.out.println("updateCapacity called for slot " + i);
            GenericStackInv inv = storageInv.getInv(i);
            inv.setCapacity(AEKeyType.items(), 0);
            inv.setCapacity(AEKeyType.fluids(), 0);
            inv.setCapacity(ExternalTypes.FLUX, 0);
            inv.setCapacity(ExternalTypes.GAS, 0);
            inv.setCapacity(ExternalTypes.MANA, 0);
            inv.setCapacity(ExternalTypes.SOURCE, 0);
            List<GenericStack> possibleInputs;
            try {
                possibleInputs = this.patternInput.getInputs().get(i);
            } catch (Exception e) {
                return;
            }
            System.out.println("Possible inputs for slot " + i + ": " + possibleInputs);
            for (GenericStack input : possibleInputs) {
                AEKeyType keyType = input.what().getType();
                this.storageInv.setCapacity(i, keyType, Math.max(inv.getCapacity(keyType), input.amount()));
                System.out.println("Setting capacity for slot " + i + ", keyType: " + keyType + ", amount: " + Math.max(inv.getCapacity(keyType), input.amount()));
            }
        }
    }

    public PatternBufferInv getStorageInv() {
        return storageInv;
    }

    public InternalInventory getPatternInv() {
        return patternInv;
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
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
            long baseCapacity = 0;
            if (aeKeyType == AEKeyType.items()) baseCapacity = 1024;
            if (aeKeyType == AEKeyType.fluids()) baseCapacity = 1024000;
            if (aeKeyType == ExternalTypes.FLUX) baseCapacity = 1048576;
            if (aeKeyType == ExternalTypes.GAS) baseCapacity = 1024;
            if (aeKeyType == ExternalTypes.MANA) baseCapacity = 1000;
            if (aeKeyType == ExternalTypes.SOURCE) baseCapacity = 1000;
            int upgradeMultiplier = (int) Math.pow(2, PatternBufferBlockEntity.this.upgrades.getInstalledUpgrades(AEItems.CAPACITY_CARD));
            System.out.println("PatternBuffer baseCapacity: " + baseCapacity + ", upgradeMultiplier: " + upgradeMultiplier);
            return baseCapacity * upgradeMultiplier;
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

    private static class PatternInput {
        private List<List<GenericStack>> inputs = new ArrayList<>();

        public PatternInput() {
        }

        public void setInputs(List<List<GenericStack>> inputs) {
            this.inputs = inputs.subList(0, Math.min(inputs.size(), 36));
        }

        public List<List<GenericStack>> getInputs() {
            return inputs;
        }

        public void writeToChildTag(CompoundTag tag, String name, HolderLookup.Provider registries) {
            if (!this.inputs.isEmpty()) {
                for (int i = 0; i < this.inputs.size(); i++) {
                    tag.put(name + "_" + i, this.writeToTag(registries, i));
                }
            } else {
                for (int i = 0; i < 36; i++) {
                    tag.remove(name + "_" + i);
                }
            }
        }

        private ListTag writeToTag(HolderLookup.Provider registries, int index) {
            ListTag tag = new ListTag();

            for (var stack : this.inputs.get(index)) {
                tag.add(GenericStack.writeTag(registries, stack));
            }

            // Strip out trailing nulls
            for (int i = tag.size() - 1; i >= 0; i--) {
                if (tag.getCompound(i).isEmpty()) {
                    tag.remove(i);
                } else {
                    break;
                }
            }

            return tag;
        }

        public void readFromChildTag(CompoundTag tag, String name, HolderLookup.Provider registries) {
            this.inputs.clear();
            for (int i = 0; i < 36; i++) {
                if (!tag.contains(name + "_" + i, Tag.TAG_LIST)) break;
                readFromTag(tag.getList(name + "_" + i, Tag.TAG_COMPOUND), registries, i);
            }
        }

        private void readFromTag(ListTag tag, HolderLookup.Provider registries, int index) {
            this.inputs.add(new ArrayList<>());
            for (int i = 0; i < tag.size(); ++i) {
                var stack = GenericStack.readTag(registries, tag.getCompound(i));
                this.inputs.get(index).add(stack);
            }
        }
    }
}
