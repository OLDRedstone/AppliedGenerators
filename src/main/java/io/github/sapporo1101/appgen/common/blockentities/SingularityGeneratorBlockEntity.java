package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.AEConfig;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.Platform;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.api.caps.IGenericInvHost;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;

public class SingularityGeneratorBlockEntity extends AENetworkedBlockEntity implements IGridTickable, IUpgradeableObject, IGenericInvHost {
    private final GenericStackInv inv;
    private final IUpgradeInventory upgrades;
    private double currentFuelTicksPerTick;
    private double remainingFuelTicks;
    private double fuelItemFuelTicks;
    private double minFuelTicksPerTick;
    private double maxFuelTicksPerTick;
    private double initialFuelTicksPerTick;
    public boolean isOn;
    private final double minEnergyRate;
    private final double baseMaxEnergyRate;
    private final double initialEnergyRate;

    public SingularityGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(SingularityGeneratorBlockEntity.class, SingularityGeneratorBlockEntity::new, AGSingletons.SINGULARITY_GENERATOR), pos, blockState);
        this.inv = new GenericStackInv(this::setChanged, 2);
        this.inv.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) this.inv.setCapacity(ExternalTypes.GAS, 0);
        if (ExternalTypes.MANA != null) this.inv.setCapacity(ExternalTypes.MANA, 0);
        if (ExternalTypes.FLUX != null) this.inv.setCapacity(ExternalTypes.FLUX, 1073741824);
        if (ExternalTypes.SOURCE != null) this.inv.setCapacity(ExternalTypes.SOURCE, 0);
        this.remainingFuelTicks = 0.0F;
        this.fuelItemFuelTicks = 0.0F;
        this.getMainNode().setIdlePowerUsage(0.0F).setFlags().addService(IGridTickable.class, this);
        this.upgrades = UpgradeInventories.forMachine(AEBlocks.VIBRATION_CHAMBER, 3, this::saveChanges);
        this.minEnergyRate = AEConfig.instance().getVibrationChamberMinEnergyPerGameTick();
        this.baseMaxEnergyRate = AEConfig.instance().getVibrationChamberMaxEnergyPerGameTick();
        this.initialEnergyRate = Mth.clamp(AEConfig.instance().getVibrationChamberBaseEnergyPerFuelTick(), this.minEnergyRate, this.baseMaxEnergyRate);
        this.minFuelTicksPerTick = this.minEnergyRate / this.getEnergyPerFuelTick();
        this.maxFuelTicksPerTick = this.baseMaxEnergyRate / this.getEnergyPerFuelTick();
        this.initialFuelTicksPerTick = this.initialEnergyRate / this.getEnergyPerFuelTick();
        this.currentFuelTicksPerTick = this.initialFuelTicksPerTick;
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
        this.isOn = this.getRemainingFuelTicks() > (double) 0.0F;
        data.writeBoolean(this.isOn);
    }

    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        this.inv.writeToChildTag(data, "inv", registries);
        data.putDouble("burnTime", this.getRemainingFuelTicks());
        data.putDouble("maxBurnTime", this.getFuelItemFuelTicks());
        int speed = (int) (this.currentFuelTicksPerTick * (double) 100.0F / this.maxFuelTicksPerTick);
        data.putInt("burnSpeed", speed);
    }

    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.inv.readFromChildTag(data, "inv", registries);
        this.setRemainingFuelTicks(data.getDouble("burnTime"));
        this.setFuelItemFuelTicks(data.getDouble("maxBurnTime"));
        this.setCurrentFuelTicksPerTick((double) data.getInt("burnSpeed") * this.maxFuelTicksPerTick / (double) 100.0F);
    }

    @Override
    public void setChanged() {
        if (this.getRemainingFuelTicks() <= (double) 0.0F && this.canEatFuel()) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
        super.setChanged();
    }

    private boolean canEatFuel() {
        GenericStack is = this.inv.getStack(0);
        if (is != null) {
            int newBurnTime = 100;
            return newBurnTime > 0 && is.amount() > 0;
        }

        return false;
    }

    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        for (ItemStack upgrade : this.upgrades) {
            drops.add(upgrade);
        }
        for (int index = 0; index < this.inv.size(); index++) {
            var stack = this.inv.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
    }

    public void clearContent() {
        super.clearContent();
        this.upgrades.clear();
        this.inv.clear();
    }

    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    public TickingRequest getTickingRequest(IGridNode node) {
        System.out.println("Vibration Chamber Ticking Request: " + this.getRemainingFuelTicks() + " fuel ticks remaining, " + this.isOn);
        if (this.getRemainingFuelTicks() <= (double) 0.0F) {
            this.eatFuel();
        }

        return new TickingRequest(TickRates.VibrationChamber, this.getRemainingFuelTicks() <= (double) 0.0F);
    }

    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        System.out.println("Vibration Chamber Ticking: " + this.getRemainingFuelTicks() + " fuel ticks remaining, " + this.isOn);
        this.minFuelTicksPerTick = this.minEnergyRate / this.getEnergyPerFuelTick();
        this.maxFuelTicksPerTick = this.getMaxFuelTicksPerTick();
        this.initialFuelTicksPerTick = this.initialEnergyRate / this.getEnergyPerFuelTick();
        if (this.getRemainingFuelTicks() <= (double) 0.0F) {
            this.eatFuel();
            if (this.getRemainingFuelTicks() > (double) 0.0F) {
                return TickRateModulation.URGENT;
            } else {
                this.setCurrentFuelTicksPerTick(this.initialFuelTicksPerTick);
                return TickRateModulation.SLEEP;
            }
        } else {
            double fuelTicksConsumed = (double) ticksSinceLastCall * this.currentFuelTicksPerTick;
            this.setRemainingFuelTicks(this.getRemainingFuelTicks() - fuelTicksConsumed);
            if (this.getRemainingFuelTicks() < (double) 0.0F) {
                fuelTicksConsumed += this.getRemainingFuelTicks();
                this.setRemainingFuelTicks(0.0F);
            }

            double speedScalingPerTick = (this.maxFuelTicksPerTick - this.minFuelTicksPerTick) / (double) 100.0F;
            double speedStep = (double) ticksSinceLastCall * speedScalingPerTick;
            IGrid grid = node.getGrid();
            IEnergyService energy = grid.getEnergyService();
            if (Math.abs(fuelTicksConsumed - (double) 0.0F) < 0.01) {
                if (energy.injectPower(1.0F, Actionable.SIMULATE) == (double) 0.0F) {
                    this.setCurrentFuelTicksPerTick(this.getCurrentFuelTicksPerTick() + speedStep);
                    return TickRateModulation.FASTER;
                } else {
                    return TickRateModulation.IDLE;
                }
            } else {
                double newPower = fuelTicksConsumed * this.getEnergyPerFuelTick();
                long amount = this.inv.getStack(1) != null ? Objects.requireNonNull(this.inv.getStack(1)).amount() : 0L;
                this.inv.setStack(1, new GenericStack(FluxKey.of(EnergyType.FE), (long) newPower + amount));
                double overFlow = energy.injectPower(newPower, Actionable.MODULATE);
                if (overFlow > (double) 0.0F) {
                    this.setCurrentFuelTicksPerTick(this.getCurrentFuelTicksPerTick() - speedStep);
                } else {
                    this.setCurrentFuelTicksPerTick(this.getCurrentFuelTicksPerTick() + speedStep);
                }

                return overFlow > (double) 0.0F ? TickRateModulation.SLOWER : TickRateModulation.FASTER;
            }
        }
    }

    private void eatFuel() {
        System.out.println("Vibration Chamber eating fuel: " + this.getRemainingFuelTicks() + " fuel ticks remaining, " + this.isOn);
        GenericStack stack = this.inv.getStack(0);
        System.out.println("Vibration Chamber fuel item: " + stack);
        if (stack != null && stack.what().equals(AEItemKey.of(AEItems.SINGULARITY))) {
            System.out.println("Vibration Chamber eating singularity fuel");
            int newBurnTime = 100;
            if (newBurnTime > 0 && stack.amount() > 0) {
                this.setRemainingFuelTicks(this.getRemainingFuelTicks() + (double) newBurnTime);
                this.setFuelItemFuelTicks(this.getRemainingFuelTicks());
                if (stack.amount() <= 1) {
                    this.inv.setStack(0, GenericStack.fromItemStack(ItemStack.EMPTY));
                } else {
                    GenericStack newStack = new GenericStack(stack.what(), stack.amount() - 1);
                    this.inv.setStack(0, newStack);
                }

                this.saveChanges();
            }
        }

        if (this.getRemainingFuelTicks() > (double) 0.0F) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        if (!this.isOn && this.getRemainingFuelTicks() > (double) 0.0F || this.isOn && this.getRemainingFuelTicks() <= (double) 0.0F) {
            this.isOn = this.getRemainingFuelTicks() > (double) 0.0F;
            this.markForUpdate();
            if (this.hasLevel()) {
                Platform.notifyBlocksOfNeighbors(this.level, this.worldPosition);
            }
        }

    }

    public double getCurrentFuelTicksPerTick() {
        return this.currentFuelTicksPerTick;
    }

    private void setCurrentFuelTicksPerTick(double currentFuelTicksPerTick) {
        this.currentFuelTicksPerTick = Mth.clamp(currentFuelTicksPerTick, this.minFuelTicksPerTick, this.maxFuelTicksPerTick);
    }

    public double getFuelItemFuelTicks() {
        return this.fuelItemFuelTicks;
    }

    private void setFuelItemFuelTicks(double fuelItemFuelTicks) {
        this.fuelItemFuelTicks = fuelItemFuelTicks;
    }

    public double getRemainingFuelTicks() {
        return this.remainingFuelTicks;
    }

    private void setRemainingFuelTicks(double remainingFuelTicks) {
        this.remainingFuelTicks = remainingFuelTicks;
    }

    public double getEnergyPerFuelTick() {
        return AEConfig.instance().getVibrationChamberBaseEnergyPerFuelTick() * (double) (1.0F + (float) this.upgrades.getInstalledUpgrades(AEItems.ENERGY_CARD) / 2.0F);
    }

    public double getMaxFuelTicksPerTick() {
        return this.getMaxEnergyRate() / this.getEnergyPerFuelTick();
    }

    public double getMaxEnergyRate() {
        return this.baseMaxEnergyRate + this.baseMaxEnergyRate * (double) this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD) / (double) 2.0F;
    }

    @Override
    public GenericStackInv getGenericInv() {
        return this.inv;
    }
}