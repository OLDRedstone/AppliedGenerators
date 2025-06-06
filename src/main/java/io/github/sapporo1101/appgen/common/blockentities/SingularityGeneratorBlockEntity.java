package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.api.IGenericInternalInvHost;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.util.CustomIOFilter;
import io.github.sapporo1101.appgen.util.CustomStackInv;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SingularityGeneratorBlockEntity extends AENetworkedBlockEntity implements IGridTickable, IGenericInternalInvHost, IUpgradeableObject {
    public static final @NotNull AEKey FE_KEY = FluxKey.of(EnergyType.FE);
    public static final AEKey SINGULARITY_KEY = AEItemKey.of(AEItems.SINGULARITY);

    private final GenericStackInv inv;
    private final IUpgradeInventory upgrades;
    private int generatableFE;
    private final MachineSource source;
    public boolean isOn;

    public SingularityGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(SingularityGeneratorBlockEntity.class, SingularityGeneratorBlockEntity::new, AGSingletons.SINGULARITY_GENERATOR), pos, blockState);
        this.inv = new CustomStackInv(Set.of(SINGULARITY_KEY), CustomIOFilter.INSERT_ONLY, this::singularitySetChanged, GenericStackInv.Mode.STORAGE, 1);
        this.inv.setCapacity(AEKeyType.items(), 64);
        this.upgrades = UpgradeInventories.forMachine(AGSingletons.SINGULARITY_GENERATOR, 5, this::upgradeSetChanged);
        this.source = new MachineSource(this);
        this.generatableFE = 0;
        this.getMainNode().setIdlePowerUsage(0F).setFlags().addService(IGridTickable.class, this);
    }

    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.inv.writeToChildTag(data, "inv", registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        data.putDouble("generatableFE", this.getGeneratableFE());
    }

    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.inv.readFromChildTag(data, "inv", registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.setGeneratableFE(data.getInt("generatableFE"));
    }

    public void singularitySetChanged() {
        updateBlockEntity(this.shouldUpdateIsOn());
        if (this.getGeneratableFE() <= 0 && this.canEatFuel()) {
            System.out.println("Singularity Generator state changed, start charging");
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
        this.setChanged();
    }

    private void upgradeSetChanged() {
        this.saveChanges();
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
        for (ItemStack upgrade : this.upgrades) drops.add(upgrade);
    }

    public void clearContent() {
        super.clearContent();
        this.inv.clear();
        this.upgrades.clear();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
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
            int newFE = Math.min(ticksSinceLastCall * this.getGeneratePerTick(), this.getGeneratableFE());
            return this.sendFEToNetwork(newFE) ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        }
    }

    private void charge() {
        System.out.println("Singularity Generator charging: " + this.getGeneratableFE() + " FE remaining, " + this.isOn);
        GenericStack stack = this.inv.getStack(0);
        System.out.println("Singularity Generator fuel item: " + stack);
        if (stack != null && stack.what().equals(SINGULARITY_KEY)) {
            System.out.println("Singularity Generator charging singularity fuel");
            if (stack.amount() > 0) {
                this.setGeneratableFE(this.getGeneratableFE() + this.getFEPerSingularity());
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

    public int getGeneratePerTick() {
        int baseGeneratePerTick = 50;
        if (this.upgrades == null) {
            return baseGeneratePerTick;
        }
        double upgradeMultiplier = 1 + this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD) * 0.5;
        return (int) (baseGeneratePerTick * upgradeMultiplier);
    }

    public int getFEPerSingularity() {
        final int baseFEPerSingularity = 100000;
        if (this.upgrades == null) {
            return baseFEPerSingularity;
        }
        double upgradeMultiplier = 1 + this.upgrades.getInstalledUpgrades(AEItems.ENERGY_CARD) * 0.5;
        return (int) (baseFEPerSingularity * upgradeMultiplier);
    }

    public boolean sendFEToNetwork(int amount) {
        if (this.getGridNode() == null) {
            return false;
        } else {
            IGrid grid = this.getGridNode().getGrid();
            IStorageService storage = grid.getStorageService();

            long inserted = storage.getInventory().insert(FE_KEY, amount, Actionable.MODULATE, this.source);
            this.setGeneratableFE(Math.max(0, this.getGeneratableFE() - (int) inserted));

            return inserted > 0;
        }
    }
}