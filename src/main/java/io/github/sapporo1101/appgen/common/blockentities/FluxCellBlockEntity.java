package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKeyType;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.SettingsFrom;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.github.sapporo1101.appgen.common.blocks.FluxCellBlock.FE_STORAGE;
import static io.github.sapporo1101.appgen.common.blocks.FluxCellBlock.MAX_FULLNESS;

public class FluxCellBlockEntity extends AEBaseBlockEntity implements BlockEntityTicker<FluxCellBlockEntity> {
    private final GenericStackInv feInv;
    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    public FluxCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(FluxCellBlockEntity.class, FluxCellBlockEntity::new, AGSingletons.FLUX_CELL), pos, blockState);
        this.feInv = new GenericStackInv(this::setChanged, 36);
        this.feInv.setCapacity(AEKeyType.items(), 0);
        this.feInv.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) this.feInv.setCapacity(ExternalTypes.GAS, 0);
        if (ExternalTypes.MANA != null) this.feInv.setCapacity(ExternalTypes.MANA, 0);
        if (ExternalTypes.FLUX != null) this.feInv.setCapacity(ExternalTypes.FLUX, 1048576);
        if (ExternalTypes.SOURCE != null) this.feInv.setCapacity(ExternalTypes.SOURCE, 0);
    }

    public int getFullness() {
        long fluxAmount = 0;
        for (int i = 0; i < feInv.size(); i++) {
            fluxAmount += feInv.getAmount(i);
        }
        long maxFlux = feInv.getCapacity(ExternalTypes.FLUX) * feInv.size();
        if (maxFlux <= 0) return 1;
        double fluxPercentage = (double) fluxAmount / maxFlux;
        final int fullness;
        if (fluxPercentage < 0.25) {
            fullness = 0;
        } else if (fluxPercentage < 0.5) {
            fullness = 1;
        } else if (fluxPercentage < 0.75) {
            fullness = 2;
        } else if (fluxPercentage < 0.99) {
            fullness = 3;
        } else {
            fullness = MAX_FULLNESS;
        }
        return fullness;
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

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.feInv.size(); index++) {
            var stack = this.feInv.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.feInv.writeToChildTag(data, "buffer", registries);
        var sides = new ListTag();
        for (var side : this.outputSides) {
            sides.add(StringTag.valueOf(side.getName()));
        }
        data.put("output_side", sides);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.feInv.readFromChildTag(data, "buffer", registries);
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
    public void clearContent() {
        super.clearContent();
        this.feInv.clear();
    }

    public Set<Direction> getOutputSides() {
        return this.outputSides;
    }

    public GenericStackInv getGenericInv() {
        return this.feInv;
    }

    @Override
    public void tick(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull FluxCellBlockEntity be) {
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(FE_STORAGE, be.getFullness()));
        if (!this.feInv.isEmpty() && !this.outputSides.isEmpty()) this.sendFEToAdjacentBlock();
    }

    private void sendFEToAdjacentBlock() {
        if (this.level == null) return;

        for (Direction dir : this.outputSides) {
            if (this.feInv.isEmpty()) break;
            BlockPos targetPos = this.getBlockPos().relative(dir);
            IEnergyStorage storage = this.level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, dir.getOpposite());
            if (storage != null && storage.canReceive()) {
                int canInsert = storage.receiveEnergy(Integer.MAX_VALUE, true);
                if (canInsert <= 0) continue;
                int extracted = Math.toIntExact(this.feInv.extract(FluxKey.of(EnergyType.FE), canInsert, Actionable.MODULATE, null));
                storage.receiveEnergy(extracted, false);
            }
        }
    }

    public IEnergyStorage getEnergyStorage(Direction dir) {
        return new FluxCellEnergyStorage(this.feInv, this.outputSides, dir);
    }

    static class FluxCellEnergyStorage implements IEnergyStorage {
        private final GenericStackInv inv;
        private final Set<Direction> outputSides;
        private final Direction dir;

        public FluxCellEnergyStorage(GenericStackInv inv, Set<Direction> outputSides, Direction dir) {
            this.inv = inv;
            this.outputSides = outputSides;
            this.dir = dir;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (simulate) {
                // Simulate the insertion of energy
                return Math.toIntExact(this.inv.insert(FluxKey.of(EnergyType.FE), toReceive, Actionable.SIMULATE, null));
            } else {
                // Actually insert energy
                return Math.toIntExact(this.inv.insert(FluxKey.of(EnergyType.FE), toReceive, Actionable.MODULATE, null));
            }
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (simulate) {
                // Simulate the extraction of energy
                return Math.toIntExact(this.inv.extract(FluxKey.of(EnergyType.FE), toExtract, Actionable.SIMULATE, null));
            } else {
                // Actually extract energy
                return Math.toIntExact(this.inv.extract(FluxKey.of(EnergyType.FE), toExtract, Actionable.MODULATE, null));
            }
        }

        @Override
        public int getEnergyStored() {
            int total = 0;
            for (int i = 0; i < this.inv.size(); i++) {
                total += (int) this.inv.getAmount(i);
            }
            return total;
        }

        @Override
        public int getMaxEnergyStored() {
            return Math.toIntExact(this.inv.getCapacity(ExternalTypes.FLUX) * this.inv.size());
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return !this.outputSides.contains(this.dir);
        }
    }
}