package io.github.sapporo1101.appgen.common.blocks;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.FluxCellBlockEntity;
import io.github.sapporo1101.appgen.menu.FluxCellMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public class FluxCellBlock<U extends FluxCellBlockEntity> extends BlockBaseGui<U> {
    public static final int MAX_FULLNESS = 4;
    public static final IntegerProperty FE_STORAGE = IntegerProperty.create("fullness", 0, MAX_FULLNESS);

    public FluxCellBlock() {
        super(glassProps());
        this.registerDefaultState(this.defaultBlockState().setValue(FE_STORAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FE_STORAGE);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos1, state1, be) -> ((FluxCellBlockEntity) be).tick(level1, pos1, state1, (FluxCellBlockEntity) be);
    }

    @Override
    public void openGui(FluxCellBlockEntity tile, Player p) {
        MenuOpener.open(FluxCellMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }

    public static class Standard extends FluxCellBlock<FluxCellBlockEntity.Standard> {
    }

    public static class Dense extends FluxCellBlock<FluxCellBlockEntity.Dense> {
    }
}
