package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.items.materials.MaterialItem;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemDefinitionFilter;
import appeng.util.inv.filter.IAEItemFilter;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class SingularityGeneratorBlockEntity extends AENetworkedInvBlockEntity implements IGridTickable, IUpgradeableObject, IConfigurableObject {
    public static final @NotNull AEKey FE_KEY = FluxKey.of(EnergyType.FE);
    public static final MaterialItem SINGULARITY = AEItems.SINGULARITY.asItem();

    private final AppEngInternalInventory inv = new AppEngInternalInventory(this, 1, 64, new AEItemDefinitionFilter(AEItems.SINGULARITY));
    private final InternalInventory invExt = new FilteredInternalInventory(this.inv, new SingularitySlotFilter());
    private final IUpgradeInventory upgrades;
    private final IConfigManager configManager;
    private final MachineSource source;
    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    private int generatableFE;
    public boolean isOn;

    public SingularityGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(SingularityGeneratorBlockEntity.class, SingularityGeneratorBlockEntity::new, AGSingletons.SINGULARITY_GENERATOR), pos, blockState);
        this.getMainNode().setIdlePowerUsage(0F).setFlags(GridFlags.REQUIRE_CHANNEL).addService(IGridTickable.class, this);
        this.upgrades = UpgradeInventories.forMachine(AGSingletons.SINGULARITY_GENERATOR, 5, this::upgradeSetChanged);
        this.configManager = IConfigManager.builder(this::onConfigChanged).registerSetting(Settings.AUTO_EXPORT, YesNo.NO).build();
        this.source = new MachineSource(this);

        this.generatableFE = 0;
    }

    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        var nbt = input.get(EAESingletons.EXTRA_SETTING);
        if (nbt != null) {
            this.outputSides.clear();
            for (var side : nbt.getList("output_side", CompoundTag.TAG_STRING)) {
                this.outputSides.add(Direction.byName(side.getAsString()));
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            var nbt = new CompoundTag();
            var sides = new ListTag();
            for (var side : this.outputSides) {
                sides.add(StringTag.valueOf(side.getName()));
            }
            nbt.put("output_side", sides);
            output.set(EAESingletons.EXTRA_SETTING, nbt);
        }
    }

    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        this.configManager.writeToNBT(data, registries);
        data.putDouble("generatableFE", this.getGeneratableFE());
        var sides = new ListTag();
        for (var side : this.outputSides) {
            sides.add(StringTag.valueOf(side.getName()));
        }
        data.put("output_side", sides);
    }

    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.setGeneratableFE(data.getInt("generatableFE"));
        this.configManager.readFromNBT(data, registries);
        this.outputSides.clear();
        if (data.contains("output_side")) {
            var list = data.getList("output_side", CompoundTag.TAG_STRING);
            for (var name : list) {
                this.outputSides.add(Direction.byName(name.getAsString()));
            }
        } else {
            this.outputSides.addAll(List.of(Direction.values()));
        }
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.getInternalInventory();
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }

        return super.getSubInventory(id);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(Direction facing) {
        return this.invExt;
    }

    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        updateBlockEntity(this.shouldUpdateIsOn());
        if (this.getGeneratableFE() <= 0 && this.canEatFuel()) {
            System.out.println("Singularity Generator state changed, start charging");
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
    }

    private void upgradeSetChanged() {
        this.saveChanges();
    }

    private void onConfigChanged() {
        this.getMainNode().ifPresent((grid, node) -> {
            if (this.getGeneratableFE() > 0 || this.canEatFuel()) {
                grid.getTickManager().wakeDevice(node);
            } else {
                grid.getTickManager().sleepDevice(node);
            }
        });
        this.saveChanges();
    }

    public boolean canEatFuel() {
        ItemStack stack = this.inv.getStackInSlot(0);
        if (!stack.isEmpty() && stack.is(SINGULARITY)) {
            return stack.getCount() > 0;
        }
        return false;
    }

    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.inv.size(); index++) {
            ItemStack stack = this.inv.getStackInSlot(index);
            if (!stack.isEmpty()) drops.add(stack);
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
            final boolean sent = this.configManager.getSetting(Settings.AUTO_EXPORT) == YesNo.YES ? this.sendFEToAdjacentBlock(newFE) : this.sendFEToNetwork(newFE);
            return sent ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        }
    }

    private void charge() {
        System.out.println("Singularity Generator charging: " + this.getGeneratableFE() + " FE remaining, " + this.isOn);
        ItemStack stack = this.inv.getStackInSlot(0);
        System.out.println("Singularity Generator fuel item: " + stack);
        if (!stack.isEmpty() && stack.is(SINGULARITY)) {
            System.out.println("Singularity Generator charging singularity fuel");
            if (stack.getCount() > 0) {
                this.setGeneratableFE(this.getGeneratableFE() + this.getFEPerSingularity());
                if (stack.getCount() <= 1) {
                    this.inv.setItemDirect(0, ItemStack.EMPTY);
                } else {
                    stack.shrink(1);
                    this.inv.setItemDirect(0, stack);
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

    private boolean sendFEToAdjacentBlock(int newFE) {
        if (this.level == null) return false;

        boolean sent = false;
        for (Direction dir : this.outputSides) {
            if (newFE <= 0) break;
            BlockPos targetPos = this.getBlockPos().relative(dir);
            IEnergyStorage storage = this.level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, dir.getOpposite());
            if (storage != null && storage.canReceive()) {
                System.out.println("Singularity Generator found energy storage at " + targetPos + " for dir " + dir);
                int canInsert = storage.receiveEnergy(newFE, true);
                if (canInsert <= 0) continue;
                int inserted = storage.receiveEnergy(newFE, false);
                if (inserted > 0) sent = true;
                this.setGeneratableFE(Math.max(0, this.getGeneratableFE() - inserted));
                newFE -= inserted;
            } else {
                System.out.println("Singularity Generator no energy storage found at " + targetPos + " for dir " + dir);
            }
        }
        return sent;
    }

    public Set<Direction> getOutputSides() {
        return this.outputSides;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    private static class SingularitySlotFilter implements IAEItemFilter {

        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return !inv.getStackInSlot(slot).is(SINGULARITY);
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.is(SINGULARITY);
        }
    }
}