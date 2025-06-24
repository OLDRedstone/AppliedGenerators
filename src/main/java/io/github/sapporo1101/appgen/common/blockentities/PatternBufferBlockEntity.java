package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.AEKeySlotFilter;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatternBufferBlockEntity extends AEBaseBlockEntity implements InternalInventoryHost {

    private final GenericStackInv storageInv = new PatternBufferInv(this::onStorageChanged, 27);
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
        InternalInventoryHost.super.onChangeInventory(inv, slot);
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

    private long getMaxAmount(int slot, AEKey key) {
        System.out.println("PatternBuffer MaxAmount: " + key);
        if (slot < 0 || slot >= patternInputs.size() || !isValidKey(slot, key)) return 0;
        List<GenericStack> inputs = patternInputs.get(slot);
        long maxAmount = 0;
        for (GenericStack input : inputs) {
            if (key.matches(input)) {
                maxAmount = Math.max(maxAmount, input.amount());
            }
        }
        System.out.println("PatternBuffer MaxAmount: " + key + ": " + maxAmount);
        return maxAmount;
    }

    public GenericStackInv getStorageInv() {
        return storageInv;
    }

    public InternalInventory getPatternInv() {
        return patternInv;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        this.saveChanges();
    }

    private class PatternBufferInv extends GenericStackInv {
        public PatternBufferInv(Runnable onChange, int size) {
            super(onChange, size);
            this.setFilter(new PatternBufferFilter());
        }

        @Override
        public void setStack(int slot, @Nullable GenericStack stack) {
            System.out.println("PatternBuffer setStack: " + slot + ": " + stack);
            super.setStack(slot, stack);
        }

        @Override
        public long insert(int slot, AEKey what, long amount, Actionable mode) {
            System.out.println("PatternBuffer insert: " + what + ": " + amount);
            GenericStack stack = this.getStack(slot);
            long stackAmount = stack != null ? stack.amount() : 0;
            amount = Math.min(amount, PatternBufferBlockEntity.this.getMaxAmount(slot, what) - stackAmount);
            System.out.println("PatternBuffer insert: " + what + ": " + amount + " (max: " + PatternBufferBlockEntity.this.getMaxAmount(slot, what) + ")");
            return super.insert(slot, what, amount, mode);
        }
    }

    private class PatternBufferFilter implements AEKeySlotFilter {

        @Override
        public boolean isAllowed(int slot, AEKey what) {
            return isValidKey(slot, what);
        }
    }
}
