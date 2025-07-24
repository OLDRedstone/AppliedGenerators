package io.github.sapporo1101.appgen.common.blocks;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.FluxCellBaseBlockEntity;
import io.github.sapporo1101.appgen.menu.FluxCellMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class FluxCellBaseBlock<U extends FluxCellBaseBlockEntity> extends BlockBaseGui<U> {

    public FluxCellBaseBlock() {
        super(glassProps());
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos1, state1, be) -> ((FluxCellBaseBlockEntity) be).tick(level1, pos1, state1, (FluxCellBaseBlockEntity) be);
    }

    @Override
    public void openGui(FluxCellBaseBlockEntity tile, Player p) {
        MenuOpener.open(FluxCellMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }
}
