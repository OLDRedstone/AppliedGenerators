package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.core.settings.TickRates;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemFilters;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SmelterBlockEntity extends AENetworkedPoweredBlockEntity implements RecipeCraftingHolder, IGridTickable {

    private static final int POWER_MAXIMUM_AMOUNT = 10_000;
    private static final int AE_PER_OPERATION = 4000;

    private final RecipeType<SmeltingRecipe> recipeType = RecipeType.SMELTING;
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck = RecipeManager.createCheck(recipeType);

    private final InternalInventory inputInv = new AppEngInternalInventory(this, 1, 64);
    private final InternalInventory outputInv = new AppEngInternalInventory(this, 1, 64);
    private final InternalInventory inv = new CombinedInternalInventory(this.inputInv, this.outputInv);
    private final FilteredInternalInventory inputExposed = new FilteredInternalInventory(this.inputInv, AEItemFilters.INSERT_ONLY);
    private final FilteredInternalInventory outputExposed = new FilteredInternalInventory(this.outputInv, AEItemFilters.EXTRACT_ONLY);
    private final InternalInventory invExposed = new CombinedInternalInventory(this.inputExposed, this.outputExposed);

    private boolean hasWork = false;
    private int maxProgress = 0;
    private int progress = 0;
    public boolean showWarning = false;

    public SmelterBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(SmelterBlockEntity.class, SmelterBlockEntity::new, AGSingletons.SMELTER), pos, blockState);
        this.getMainNode().setIdlePowerUsage(0).addService(IGridTickable.class, this);
        this.setInternalMaxPower(POWER_MAXIMUM_AMOUNT);
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        data.putInt("max_smelting_progress", maxProgress);
        data.putInt("smelting_progress", progress);
        data.putBoolean("has_smelting_work", hasWork);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.maxProgress = data.getInt("max_smelting_progress");
        this.setProgress(data.getInt("smelting_progress"));
        this.hasWork = data.getBoolean("has_smelting_work");
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.add(this.inputInv.getStackInSlot(0));
        drops.add(this.outputInv.getStackInSlot(0));
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        super.onChangeInventory(inv, slot);
        if (this.level == null) return;
        ItemStack inputStack = this.inputInv.getStackInSlot(0);
        ItemStack outputStack = this.outputInv.getStackInSlot(0);
        if (canSmelt(this.level.registryAccess(), getRecipeHolder(this.level, inputStack, this), inputStack, outputStack, 64, this)) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Inscriber, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        int oldMaxProgress = this.maxProgress;
        int oldProgress = this.progress;
        boolean oldHasWork = this.hasWork;
        ItemStack inputStack = this.inputInv.getStackInSlot(0);
        ItemStack outputStack = this.outputInv.getStackInSlot(0);
        System.out.println("Smelter: tickingRequest with input " + inputStack + " and output " + outputStack);
        if (this.level == null) return TickRateModulation.SAME;
        if (!this.hasWork && inputStack.isEmpty()) return TickRateModulation.SLEEP;
        System.out.println("Smelter: has input or is working");
        RecipeHolder<?> recipeholder = getRecipeHolder(this.level, inputStack, this);

        if (!this.hasWork && canSmelt(this.level.registryAccess(), recipeholder, inputStack, outputStack, 64, this)) {
            System.out.println("Smelter: start working with " + inputStack + " -> " + recipeholder);
            this.hasWork = true;
            this.maxProgress = getMaxProgress(this.level, this);
        }

        if (this.hasWork && canSmelt(level.registryAccess(), recipeholder, inputStack, outputStack, 64, this)) {
            this.getMainNode().ifPresent(grid -> useEnergy(grid, this, ticksSinceLastCall));
            System.out.println("Smelter: working... " + this.progress + "/" + this.maxProgress);
            if (this.progress >= this.maxProgress) {
                System.out.println("Smelter: finish working with " + inputStack + " -> " + recipeholder);
                this.setProgress(0);
                if (smelt(level.registryAccess(), recipeholder, inputStack, outputStack, 64, this)) {
                    this.hasWork = false;
                }
            }
        } else {
            System.out.println("Smelter: stop working");
            this.setProgress(0);
            this.hasWork = false;
        }
        if (oldMaxProgress != this.maxProgress || oldProgress != this.progress || oldHasWork != this.hasWork) {
            this.saveChanges();
        }
        return canSmelt(level.registryAccess(), recipeholder, inputStack, outputStack, 64, this) ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    private static RecipeHolder<?> getRecipeHolder(Level level, ItemStack inputStack, SmelterBlockEntity smelter) {
        RecipeHolder<?> recipeholder;
        if (!inputStack.isEmpty()) {
            recipeholder = smelter.quickCheck.getRecipeFor(new SingleRecipeInput(inputStack), level).orElse(null);
        } else {
            recipeholder = null;
        }
        return recipeholder;
    }

    private static boolean canSmelt(RegistryAccess registryAccess, @javax.annotation.Nullable RecipeHolder<?> recipe, ItemStack inputStack, ItemStack outputStack, @SuppressWarnings("SameParameterValue") int maxStackSize, SmelterBlockEntity smelter) {
        if (!inputStack.isEmpty() && recipe != null) {
            ItemStack resultStack = ((AbstractCookingRecipe) recipe.value()).assemble(new SingleRecipeInput(smelter.getInputInv().getStackInSlot(0)), registryAccess);
            if (resultStack.isEmpty()) {
                return false;
            } else {
                if (outputStack.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(outputStack, resultStack)) {
                    return false;
                } else {
                    return outputStack.getCount() + resultStack.getCount() <= maxStackSize && outputStack.getCount() + resultStack.getCount() <= outputStack.getMaxStackSize() || outputStack.getCount() + resultStack.getCount() <= resultStack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean smelt(RegistryAccess registryAccess, @javax.annotation.Nullable RecipeHolder<?> recipe, ItemStack inputStack, ItemStack outputStack, @SuppressWarnings("SameParameterValue") int maxStackSize, SmelterBlockEntity smelter) {
        System.out.print("Smelter: smelt action for " + inputStack + " -> " + recipe + ": ");
        if (recipe != null && canSmelt(registryAccess, recipe, inputStack, outputStack, maxStackSize, smelter)) {
            ItemStack resultStack = ((AbstractCookingRecipe) recipe.value()).assemble(new SingleRecipeInput(inputStack), registryAccess);
            if (outputStack.isEmpty()) {
                smelter.outputInv.setItemDirect(0, resultStack.copy());
            } else if (ItemStack.isSameItemSameComponents(outputStack, resultStack)) {
                outputStack.grow(resultStack.getCount());
                smelter.outputInv.setItemDirect(0, outputStack);
            }
            inputStack.shrink(1);
            smelter.inputInv.setItemDirect(0, inputStack);
            System.out.println("true");
            return true;
        } else {
            System.out.println("false");
            return false;
        }
    }

    private static void useEnergy(IGrid grid, SmelterBlockEntity smelter, int ticks) {
        IEnergyService eg = grid.getEnergyService();
        IEnergySource src = smelter;

        final int speedFactor = 1; // 200 ticks
//        final int speedFactor =
//                switch (smelter.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
//                    case 1 -> 4; // 50 ticks
//                    case 2 -> 8; // 25 ticks
//                    case 3 -> 16; // 12 ticks
//                    case 4 -> 32; // 6 ticks
//                    default -> 2; // 100 ticks
//                };

        final int progressReq = smelter.maxProgress - smelter.getProgress();
        final float powerRatio = progressReq < speedFactor ? (float) progressReq / speedFactor : 1;
        final int requiredTicks = Mth.ceil((float) smelter.maxProgress / speedFactor);
        final int aeConsumption = Mth.floor(((float) AE_PER_OPERATION / requiredTicks) * powerRatio * ticks);
        final double powerThreshold = aeConsumption - 0.01;

        double powerReq = smelter.extractAEPower(aeConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);

        if (powerReq <= powerThreshold) {
            src = eg;
            var oldPowerReq = powerReq;
            powerReq = eg.extractAEPower(aeConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            if (oldPowerReq > powerReq) {
                src = smelter;
                powerReq = oldPowerReq;
            }
        }

        if (powerReq > powerThreshold) {
            src.extractAEPower(aeConsumption, Actionable.MODULATE, PowerMultiplier.CONFIG);
            smelter.addProgress(speedFactor);
            smelter.showWarning = false;
        } else if (powerReq != 0) {
            var progressRatio = src == smelter
                    ? powerReq / aeConsumption
                    : (powerReq - 10 * eg.getIdlePowerUsage()) / aeConsumption;
            var factor = Mth.floor(progressRatio * speedFactor);

            if (factor > 1) {
                var extracted = src.extractAEPower(
                        (double) (aeConsumption * factor) / speedFactor,
                        Actionable.MODULATE,
                        PowerMultiplier.CONFIG);
                var actualFactor = (int) Math.floor(extracted / aeConsumption * speedFactor);
                smelter.addProgress(actualFactor);
            }

            smelter.showWarning = true;
        }

    }

    private static int getMaxProgress(Level level, SmelterBlockEntity smelter) {
        SingleRecipeInput singlerecipeinput = new SingleRecipeInput(smelter.inputInv.getStackInSlot(0));
        return smelter.quickCheck.getRecipeFor(singlerecipeinput, level).map((recipeHolder) -> recipeHolder.value().getCookingTime()).orElse(200);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(Direction side) {
        return this.invExposed;
    }

    public InternalInventory getInputInv() {
        return this.inputInv;
    }

    public InternalInventory getOutputExposed() {
        return this.outputExposed;
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipeHolder) {
    }

    @Override
    public @Nullable RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getProgress() {
        return this.progress;
    }

    private void addProgress(int delta) {
        this.progress += delta;
    }

    private void setProgress(int progress) {
        this.progress = progress;
    }
}
