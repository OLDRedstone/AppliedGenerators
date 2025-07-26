package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CreativeFluxCellBlockEntity extends FluxCellBaseBlockEntity {

    public CreativeFluxCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(CreativeFluxCellBlockEntity.class, CreativeFluxCellBlockEntity::new, AGSingletons.CREATIVE_FLUX_CELL), pos, blockState);
    }

    @Override
    protected GenericStackInv createInv() {
        return new CreativeFEInv(this::setChanged, 36, this.getFluxCapacity());
    }

    @Override
    protected long getFluxCapacity() {
        return Integer.MAX_VALUE;
    }

    private static class CreativeFEInv extends GenericStackInv {
        public CreativeFEInv(@Nullable Runnable listener, int size, long capacity) {
            super(listener, size);
            for (int i = 0; i < size; i++) {
                this.setStack(i, new GenericStack(FluxKey.of(EnergyType.FE), capacity));
            }
        }

        @Override
        public long extract(int slot, AEKey what, long amount, Actionable mode) {
            // set the mode to simulate to prevent spending energy
            return super.extract(slot, what, amount, Actionable.SIMULATE);
        }

        @Override
        public long insert(int slot, AEKey what, long amount, Actionable mode) {
            return 0;
        }
    }
}