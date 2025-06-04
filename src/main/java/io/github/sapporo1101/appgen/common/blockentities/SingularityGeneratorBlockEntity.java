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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SingularityGeneratorBlockEntity extends AENetworkedBlockEntity implements IGridTickable, IGenericInvHost {
    private final GenericStackInv inv;
    private double generatableEnergy;
    public boolean isOn;

    public SingularityGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(SingularityGeneratorBlockEntity.class, SingularityGeneratorBlockEntity::new, AGSingletons.SINGULARITY_GENERATOR), pos, blockState);
        this.inv = new CustomStackInv(
                Map.of(0, Set.of(this.getFuelKey()), 1, Set.of(this.getEnergyKey())),
                Map.of(0, CustomIOFilter.INSERT_ONLY, 1, CustomIOFilter.EXTRACT_ONLY),
                this::setChanged, GenericStackInv.Mode.STORAGE, 2
        );
        this.inv.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) this.inv.setCapacity(ExternalTypes.GAS, 0);
        if (ExternalTypes.MANA != null) this.inv.setCapacity(ExternalTypes.MANA, 0);
        if (ExternalTypes.FLUX != null) this.inv.setCapacity(ExternalTypes.FLUX, this.getEnergyCapacity());
        if (ExternalTypes.SOURCE != null) this.inv.setCapacity(ExternalTypes.SOURCE, 0);
        this.generatableEnergy = 0F;
        this.getMainNode().setIdlePowerUsage(0F).setFlags().addService(IGridTickable.class, this);
    }

    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean c = super.readFromStream(data);
        boolean wasOn = this.isOn;
        this.isOn = data.readBoolean();
        return wasOn != this.isOn || c;
    }

    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        this.isOn = this.getGeneratableEnergy() > 0;
        data.writeBoolean(this.isOn);
    }

    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.inv.writeToChildTag(data, "inv", registries);
        data.putDouble("burnTime", this.getGeneratableEnergy());
    }

    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.inv.readFromChildTag(data, "inv", registries);
        this.setGeneratableEnergy(data.getDouble("burnTime"));
    }

    @Override
    public void setChanged() {
        if (this.getGeneratableEnergy() <= 0 && this.canEatFuel()) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
        super.setChanged();
    }

    private boolean canEatFuel() {
        GenericStack is = this.inv.getStack(0);
        if (is != null && is.what() != null && is.what().equals(this.getFuelKey())) {
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
        System.out.println("Singularity Generator Ticking Request: " + this.getGeneratableEnergy() + " FE remaining, " + this.isOn);
        if (this.getGeneratableEnergy() <= 0) {
            this.charge();
        }

        return new TickingRequest(TickRates.VibrationChamber, this.getGeneratableEnergy() <= 0);
    }

    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        System.out.println("Singularity Generator Ticking: " + this.getGeneratableEnergy() + " FE remaining, " + this.isOn);
        if (this.getGeneratableEnergy() <= 0) {
            this.charge();
            if (this.getGeneratableEnergy() > 0) {
                return TickRateModulation.URGENT;
            } else {
                return TickRateModulation.SLEEP;
            }
        } else {
            long newFE = (int) Math.min(ticksSinceLastCall * this.getGeneratePerTick(), this.getGeneratableEnergy());
            long outputAmount = this.inv.getStack(1) != null ? Objects.requireNonNull(this.inv.getStack(1)).amount() : 0L;
            long fixedNewFE = newFE + outputAmount > this.inv.getCapacity(ExternalTypes.FLUX) ? this.inv.getCapacity(ExternalTypes.FLUX) - outputAmount : newFE;
            this.setGeneratableEnergy(this.getGeneratableEnergy() - fixedNewFE);
            this.inv.setStack(1, new GenericStack(this.getEnergyKey(), fixedNewFE + outputAmount));
            return fixedNewFE < newFE ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        }
    }

    private void charge() {
        System.out.println("Singularity Generator charging: " + this.getGeneratableEnergy() + " FE remaining, " + this.isOn);
        GenericStack stack = this.inv.getStack(0);
        System.out.println("Singularity Generator fuel item: " + stack);
        if (stack != null && stack.what().equals(this.getFuelKey())) {
            System.out.println("Singularity Generator charging singularity fuel");
            if (stack.amount() > 0) {
                this.setGeneratableEnergy(this.getGeneratableEnergy() + this.getEnergyPerSingularity());
                if (stack.amount() <= 1) {
                    this.inv.setStack(0, GenericStack.fromItemStack(ItemStack.EMPTY));
                } else {
                    GenericStack newStack = new GenericStack(stack.what(), stack.amount() - 1);
                    this.inv.setStack(0, newStack);
                }

                this.saveChanges();
            }
        }

        if (this.getGeneratableEnergy() > 0) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        if (!this.isOn && this.getGeneratableEnergy() > 0 || this.isOn && this.getGeneratableEnergy() <= 0) {
            this.isOn = this.getGeneratableEnergy() > 0;
            this.markForUpdate();
            if (this.hasLevel()) {
                Platform.notifyBlocksOfNeighbors(this.level, this.worldPosition);
            }
        }
    }

    public AEKey getEnergyKey() {
        return FluxKey.of(EnergyType.FE);
    }

    public AEKey getFuelKey() {
        return AEItemKey.of(AEItems.SINGULARITY);
    }

    public long getEnergyCapacity() {
        return 1048576; // FE
    }

    public int getGeneratePerTick() {
        return 50; // FE per tick
    }

    public int getEnergyPerSingularity() {
        return 10000; // FE per singularity
    }

    public double getGeneratableEnergy() {
        return this.generatableEnergy;
    }

    private void setGeneratableEnergy(double generatableEnergy) {
        this.generatableEnergy = generatableEnergy;
    }

    @Override
    public GenericStackInv getGenericInv() {
        return this.inv;
    }
}