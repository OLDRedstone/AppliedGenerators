package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
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
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.common.blocks.FluxGeneratorBlock;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.pedroksl.advanced_ae.api.AAESettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public abstract class FluxGeneratorBlockEntity extends AENetworkedBlockEntity implements IGridTickable, IUpgradeableObject, IConfigurableObject {
    public static final @NotNull AEKey FE_KEY = FluxKey.of(EnergyType.FE);

    private final IUpgradeInventory upgrades;
    private final IConfigManager configManager;
    private final MachineSource source = new MachineSource(this);
    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    private double lastGeneratePerTick = 0;
    public boolean isOn = false;
    private YesNo lastRedstoneState = YesNo.UNDECIDED;
    public int pulse = 0;

    public FluxGeneratorBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState, FluxGeneratorBlock<?> block) {
        super(blockEntityType, pos, blockState);
        this.getMainNode().setIdlePowerUsage(0F).setFlags(GridFlags.REQUIRE_CHANNEL).addService(IGridTickable.class, this);
        this.upgrades = UpgradeInventories.forMachine(block, 4, this::onUpgradeChanged);
        this.configManager = IConfigManager.builder(this::onConfigChanged).registerSetting(AAESettings.ME_EXPORT, YesNo.YES).registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE).build();
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.complementOf(EnumSet.of(orientation.getSide(RelativeSide.TOP)));
    }

    abstract int getBaseGeneratePerTick();

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
        var sides = new ListTag();
        for (var side : this.outputSides) {
            sides.add(StringTag.valueOf(side.getName()));
        }
        data.put("output_side", sides);
    }

    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
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

    @Override
    public @Nullable InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }

        return super.getSubInventory(id);
    }

    private void onUpgradeChanged() {
        this.updateBlockEntity(shouldUpdateIsOn());
        this.saveChanges();
    }

    private void onConfigChanged() {
        this.pulse = 0;
        this.updateBlockEntity(shouldUpdateIsOn());
        this.saveChanges();
    }

    private void onRedstoneChanged(boolean redstoneState) {
        if (redstoneState && this.isPulseMode()) this.pulse++;
        this.updateBlockEntity(shouldUpdateIsOn());
        this.saveChanges();
    }

    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (ItemStack upgrade : this.upgrades) drops.add(upgrade);
    }

    public void clearContent() {
        super.clearContent();
        this.upgrades.clear();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    public TickingRequest getTickingRequest(IGridNode node) {
        this.updateBlockEntity(this.shouldUpdateIsOn());

        return new TickingRequest(TickRates.VibrationChamber, !this.isOn);
    }

    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.isPulseMode()) {
            if (this.pulse <= 0) return TickRateModulation.SLEEP;
            int ticks = Math.min(ticksSinceLastCall, this.pulse);
            this.pulse -= ticks;
            int newFE = ticks * this.getGeneratePerTick();
            final int sent = this.configManager.getSetting(AAESettings.ME_EXPORT) == YesNo.YES ? this.sendFEToNetwork(newFE) : this.sendFEToAdjacentBlock(newFE);
            this.lastGeneratePerTick = (double) sent / ticks;
            if (this.pulse <= 0) {
                this.updateBlockEntity(this.shouldUpdateIsOn());
                return TickRateModulation.SLEEP;
            } else {
                return sent > 0 ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
            }
        } else {
            int newFE = ticksSinceLastCall * this.getGeneratePerTick();
            final int sent = this.configManager.getSetting(AAESettings.ME_EXPORT) == YesNo.YES ? this.sendFEToNetwork(newFE) : this.sendFEToAdjacentBlock(newFE);
            this.lastGeneratePerTick = (double) sent / ticksSinceLastCall;
            return sent > 0 ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        }
    }

    public boolean shouldUpdateIsOn() {
        return !this.isOn && this.shouldEnabled() && !this.isPulseMode() || this.isOn && !this.shouldEnabled() && !this.isPulseMode() || !this.isOn && this.pulse > 0 && this.isPulseMode() || this.isOn && this.pulse <= 0 && this.isPulseMode();
    }

    public void updateBlockEntity(boolean condition) {
        if (!condition) return;
        this.markForUpdate();
        if (this.hasLevel()) {
            Platform.notifyBlocksOfNeighbors(this.level, this.worldPosition);
        }
        this.getMainNode().ifPresent((grid, node) -> {
            if (this.isOn) {
                grid.getTickManager().wakeDevice(node);
            } else {
                grid.getTickManager().sleepDevice(node);
                this.lastGeneratePerTick = 0;
            }
        });
    }

    public int getGeneratePerTick() {
        if (this.upgrades == null) {
            return this.getBaseGeneratePerTick();
        }
        double upgradeMultiplier = 1 + this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD) * 0.5;
        return (int) (this.getBaseGeneratePerTick() * upgradeMultiplier);
    }

    public int sendFEToNetwork(int amount) {
        if (this.getGridNode() == null) return 0;

        IGrid grid = this.getGridNode().getGrid();
        IStorageService storage = grid.getStorageService();

        return Math.toIntExact(storage.getInventory().insert(FE_KEY, amount, Actionable.MODULATE, this.source));
    }

    private int sendFEToAdjacentBlock(int amount) {
        if (this.level == null) return 0;

        int remaining = amount;
        for (Direction dir : this.outputSides) {
            if (remaining <= 0) break;
            BlockPos targetPos = this.getBlockPos().relative(dir);
            IEnergyStorage storage = this.level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, dir.getOpposite());
            if (storage != null && storage.canReceive()) {
                int canInsert = storage.receiveEnergy(remaining, true);
                if (canInsert <= 0) continue;
                int inserted = storage.receiveEnergy(remaining, false);
                remaining -= inserted;
            }
        }
        return amount - remaining;
    }

    public void updateRedstoneState() {
        if (level == null) return;

        final YesNo currentState = this.level.getBestNeighborSignal(this.worldPosition) != 0 ? YesNo.YES : YesNo.NO;
        if (this.lastRedstoneState != currentState) {
            this.lastRedstoneState = currentState;
            this.onRedstoneChanged(currentState == YesNo.YES);
        }
    }

    private boolean getRedstoneState() {
        if (this.lastRedstoneState == YesNo.UNDECIDED) this.updateRedstoneState();
        return this.lastRedstoneState == YesNo.YES;
    }

    public boolean shouldEnabled() {
        if (!upgrades.isInstalled(AEItems.REDSTONE_CARD)) return true;

        final RedstoneMode rs = this.configManager.getSetting(Settings.REDSTONE_CONTROLLED);
        if (rs == RedstoneMode.LOW_SIGNAL) return !this.getRedstoneState();
        if (rs == RedstoneMode.HIGH_SIGNAL) return this.getRedstoneState();
        return true;
    }

    public boolean isPulseMode() {
        return this.configManager.getSetting(Settings.REDSTONE_CONTROLLED) == RedstoneMode.SIGNAL_PULSE;
    }

    public double getLastGeneratePerTick() {
        return lastGeneratePerTick;
    }

    public Set<Direction> getOutputSides() {
        return this.outputSides;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    public static class FG1k extends FluxGeneratorBlockEntity {

        public FG1k(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG1k.class, FG1k::new, AGSingletons.FLUX_GENERATOR_1K), pos, blockState, AGSingletons.FLUX_GENERATOR_1K);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 50;
        }
    }

    public static class FG4k extends FluxGeneratorBlockEntity {

        public FG4k(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG4k.class, FG4k::new, AGSingletons.FLUX_GENERATOR_4K), pos, blockState, AGSingletons.FLUX_GENERATOR_4K);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 100;
        }
    }

    public static class FG16k extends FluxGeneratorBlockEntity {

        public FG16k(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG16k.class, FG16k::new, AGSingletons.FLUX_GENERATOR_16K), pos, blockState, AGSingletons.FLUX_GENERATOR_16K);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 200;
        }
    }

    public static class FG64k extends FluxGeneratorBlockEntity {

        public FG64k(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG64k.class, FG64k::new, AGSingletons.FLUX_GENERATOR_64K), pos, blockState, AGSingletons.FLUX_GENERATOR_64K);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 400;
        }
    }

    public static class FG256k extends FluxGeneratorBlockEntity {

        public FG256k(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG256k.class, FG256k::new, AGSingletons.FLUX_GENERATOR_256K), pos, blockState, AGSingletons.FLUX_GENERATOR_256K);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 800;
        }
    }

    public static class FG1m extends FluxGeneratorBlockEntity {

        public FG1m(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG1m.class, FG1m::new, AGSingletons.FLUX_GENERATOR_1M), pos, blockState, AGSingletons.FLUX_GENERATOR_1M);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 1600;
        }
    }

    public static class FG4m extends FluxGeneratorBlockEntity {

        public FG4m(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG4m.class, FG4m::new, AGSingletons.FLUX_GENERATOR_4M), pos, blockState, AGSingletons.FLUX_GENERATOR_4M);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 3200;
        }
    }

    public static class FG16m extends FluxGeneratorBlockEntity {

        public FG16m(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG16m.class, FG16m::new, AGSingletons.FLUX_GENERATOR_16M), pos, blockState, AGSingletons.FLUX_GENERATOR_16M);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 6400;
        }
    }

    public static class FG64m extends FluxGeneratorBlockEntity {

        public FG64m(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG64m.class, FG64m::new, AGSingletons.FLUX_GENERATOR_64M), pos, blockState, AGSingletons.FLUX_GENERATOR_64M);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 12800;
        }
    }

    public static class FG256m extends FluxGeneratorBlockEntity {

        public FG256m(BlockPos pos, BlockState blockState) {
            super(GlodUtil.getTileType(FG256m.class, FG256m::new, AGSingletons.FLUX_GENERATOR_256M), pos, blockState, AGSingletons.FLUX_GENERATOR_256M);
        }

        @Override
        int getBaseGeneratePerTick() {
            return 25600;
        }
    }
}