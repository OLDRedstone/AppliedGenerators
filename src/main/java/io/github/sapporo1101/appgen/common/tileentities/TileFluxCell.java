package io.github.sapporo1101.appgen.common.tileentities;

import appeng.api.stacks.AEKeyType;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.helpers.externalstorage.GenericStackInv;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.api.caps.IGenericInvHost;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TileFluxCell extends AEBaseBlockEntity implements IGenericInvHost {

    private final GenericStackInv buffer;

    public TileFluxCell(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(TileFluxCell.class, TileFluxCell::new, AGSingletons.flux_cell), pos, blockState);
        this.buffer = new GenericStackInv(this::setChanged, 36);
        this.buffer.setCapacity(AEKeyType.items(), 0);
        this.buffer.setCapacity(AEKeyType.fluids(), 0);
        if (ExternalTypes.GAS != null) {
            this.buffer.setCapacity(ExternalTypes.GAS, 0);
        }
        if (ExternalTypes.MANA != null) {
            this.buffer.setCapacity(ExternalTypes.MANA, 0);
        }
        if (ExternalTypes.FLUX != null) {
            this.buffer.setCapacity(ExternalTypes.FLUX, 1073741824);
        }
        if (ExternalTypes.SOURCE != null) {
            this.buffer.setCapacity(ExternalTypes.SOURCE, 0);
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.buffer.size(); index++) {
            var stack = this.buffer.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.buffer.writeToChildTag(data, "buffer", registries);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.buffer.clear();
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.buffer.readFromChildTag(data, "buffer", registries);
    }

    @Override
    public GenericStackInv getGenericInv() {
        return this.buffer;
    }

}