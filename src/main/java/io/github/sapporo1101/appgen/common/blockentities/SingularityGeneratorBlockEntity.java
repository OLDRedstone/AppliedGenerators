package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.Platform;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.api.caps.IGenericInvHost;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.util.CustomIOFilter;
import io.github.sapporo1101.appgen.util.CustomStackInv;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SingularityGeneratorBlockEntity extends AENetworkedBlockEntity implements IGridTickable, IGenericInvHost {
    public static final @NotNull AEKey FE_KEY = FluxKey.of(EnergyType.FE);
    public static final AEKey SINGULARITY_KEY = AEItemKey.of(AEItems.SINGULARITY);
    public static final int FE_CAPACITY = 1048576;
    public static final int GENERATE_PER_TICK = 5000;
    public static final int FE_PER_SINGULARITY = 100000;

    private final GenericStackInv inv;
    private int generatableFE;
    public boolean isOn;
    public boolean isFull;

    public SingularityGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(SingularityGeneratorBlockEntity.class, SingularityGeneratorBlockEntity::new, AGSingletons.SINGULARITY_GENERATOR), pos, blockState);
        this.inv = new CustomStackInv(
                Map.of(0, Set.of(SINGULARITY_KEY), 1, Set.of(FE_KEY)),
                Map.of(0, CustomIOFilter.INSERT_ONLY, 1, CustomIOFilter.EXTRACT_ONLY),
                this::setChanged, GenericStackInv.Mode.STORAGE, 2
        );
        this.inv.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) this.inv.setCapacity(ExternalTypes.GAS, 0);
        if (ExternalTypes.MANA != null) this.inv.setCapacity(ExternalTypes.MANA, 0);
        if (ExternalTypes.FLUX != null) this.inv.setCapacity(ExternalTypes.FLUX, FE_CAPACITY);
        if (ExternalTypes.SOURCE != null) this.inv.setCapacity(ExternalTypes.SOURCE, 0);
        this.generatableFE = 0;
        this.getMainNode().setIdlePowerUsage(0F).setFlags().addService(IGridTickable.class, this);
    }

    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.inv.writeToChildTag(data, "inv", registries);
        data.putDouble("generatableFE", this.getGeneratableFE());
    }

    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.inv.readFromChildTag(data, "inv", registries);
        this.setGeneratableFE(data.getInt("generatableFE"));
    }

    @Override
    public void setChanged() {
        this.updateBlockEntity(this.shouldUpdateIsOn() || this.shouldUpdateIsFull());
        if (this.getGeneratableFE() <= 0 && this.canEatFuel() || !this.isFull && this.getGeneratableFE() > 0) {
            System.out.println("Singularity Generator state changed, start charging");
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
        super.setChanged();
    }

    public boolean canEatFuel() {
        GenericStack is = this.inv.getStack(0);
        if (is != null && is.what() != null && is.what().equals(SINGULARITY_KEY)) {
            return is.amount() > 0;
        }
        return false;
    }

    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.inv.size(); index++) {
            var stack = this.inv.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
    }

    public void clearContent() {
        super.clearContent();
        this.inv.clear();
    }

    public TickingRequest getTickingRequest(IGridNode node) {
        System.out.println("Singularity Generator Ticking Request: " + this.getGeneratableFE() + " FE remaining, " + this.isOn);
        if (this.getGeneratableFE() <= 0) {
            this.charge();
        }

        return new TickingRequest(TickRates.VibrationChamber, this.getGeneratableFE() <= 0);
    }

    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        System.out.println("Singularity Generator Ticking: " + this.getGeneratableFE() + " FE remaining, " + this.isOn);
        if (this.getGeneratableFE() <= 0) {
            this.charge();
            if (this.getGeneratableFE() > 0) {
                return TickRateModulation.URGENT;
            } else {
                return TickRateModulation.SLEEP;
            }
        } else {
            int newFE = Math.min(ticksSinceLastCall * GENERATE_PER_TICK, this.getGeneratableFE());
            long outputAmount = this.inv.getStack(1) != null ? Objects.requireNonNull(this.inv.getStack(1)).amount() : 0L;
            int fixedNewFE = (int) (newFE + outputAmount > this.inv.getCapacity(ExternalTypes.FLUX) ? this.inv.getCapacity(ExternalTypes.FLUX) - outputAmount : newFE);
            if (fixedNewFE <= 0) {
                System.out.println("Singularity Generator is full, cannot generate more FE");
                this.updateBlockEntity(this.shouldUpdateIsFull());
                return TickRateModulation.SLEEP;
            }
            this.setGeneratableFE(this.getGeneratableFE() - fixedNewFE);
            this.inv.setStack(1, new GenericStack(FE_KEY, fixedNewFE + outputAmount));
            return TickRateModulation.SAME;
        }
    }

    private void charge() {
        System.out.println("Singularity Generator charging: " + this.getGeneratableFE() + " FE remaining, " + this.isOn);
        GenericStack stack = this.inv.getStack(0);
        System.out.println("Singularity Generator fuel item: " + stack);
        if (stack != null && stack.what().equals(SINGULARITY_KEY)) {
            System.out.println("Singularity Generator charging singularity fuel");
            if (stack.amount() > 0) {
                this.setGeneratableFE(this.getGeneratableFE() + FE_PER_SINGULARITY);
                if (stack.amount() <= 1) {
                    this.inv.setStack(0, GenericStack.fromItemStack(ItemStack.EMPTY));
                } else {
                    GenericStack newStack = new GenericStack(stack.what(), stack.amount() - 1);
                    this.inv.setStack(0, newStack);
                }

                this.saveChanges();
            }
        }

        if (this.getGeneratableFE() > 0) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        this.updateBlockEntity(this.shouldUpdateIsOn());
    }

    public boolean shouldUpdateIsOn() {
        return !this.isOn && (this.getGeneratableFE() > 0 || this.canEatFuel()) || this.isOn && this.getGeneratableFE() <= 0 && !this.canEatFuel();
    }

    public boolean shouldUpdateIsFull() {
        long feAmount = this.getGenericInv().getAmount(1);
        return !this.isFull && feAmount >= FE_CAPACITY || this.isFull && feAmount < FE_CAPACITY;
    }

    public void updateBlockEntity(boolean condition) {
        if (!condition) return;
        this.markForUpdate();
        if (this.hasLevel()) {
            Platform.notifyBlocksOfNeighbors(this.level, this.worldPosition);
        }
    }

    public int getGeneratableFE() {
        return this.generatableFE;
    }

    private void setGeneratableFE(int generatableFE) {
        this.generatableFE = generatableFE;
    }

    @Override
    public GenericStackInv getGenericInv() {
        return this.inv;
    }
}