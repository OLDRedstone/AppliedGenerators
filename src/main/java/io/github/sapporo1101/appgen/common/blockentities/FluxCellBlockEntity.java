package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.stacks.AEKeyType;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.helpers.externalstorage.GenericStackInv;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.api.caps.IGenericInternalInvHost;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static io.github.sapporo1101.appgen.common.blocks.fluxcells.FluxCell.FE_STORAGE;
import static io.github.sapporo1101.appgen.common.blocks.fluxcells.FluxCell.MAX_FULLNESS;

public class FluxCellBlockEntity extends AEBaseBlockEntity implements IGenericInternalInvHost, BlockEntityTicker<FluxCellBlockEntity> {
    private final GenericStackInv feInv;

    public FluxCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(FluxCellBlockEntity.class, FluxCellBlockEntity::new, AGSingletons.FLUX_CELL), pos, blockState);
        this.feInv = new GenericStackInv(this::setChanged, 36);
        this.feInv.setCapacity(AEKeyType.items(), 0);
        this.feInv.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) this.feInv.setCapacity(ExternalTypes.GAS, 0);
        if (ExternalTypes.MANA != null) this.feInv.setCapacity(ExternalTypes.MANA, 0);
        if (ExternalTypes.FLUX != null) this.feInv.setCapacity(ExternalTypes.FLUX, 1073741824);
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
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.feInv.clear();
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.feInv.readFromChildTag(data, "buffer", registries);
    }

    @Override
    public GenericStackInv getGenericInv() {
        return this.feInv;
    }

    @Override
    public void tick(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull FluxCellBlockEntity be) {
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(FE_STORAGE, be.getFullness()));
    }
}