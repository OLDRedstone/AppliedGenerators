package io.github.sapporo1101.appgen.common.blocks;

import io.github.sapporo1101.appgen.common.blockentities.FluxCellBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FluxCellBlock<U extends FluxCellBlockEntity> extends FluxCellBaseBlock<U> {
    public static final int MAX_FULLNESS = 4;
    public static final IntegerProperty FE_STORAGE = IntegerProperty.create("fullness", 0, MAX_FULLNESS);

    public FluxCellBlock() {
        super();
        this.registerDefaultState(this.defaultBlockState().setValue(FE_STORAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FE_STORAGE);
    }

    public static class Standard extends FluxCellBlock<FluxCellBlockEntity.Standard> {
    }

    public static class Dense extends FluxCellBlock<FluxCellBlockEntity.Dense> {
    }
}
