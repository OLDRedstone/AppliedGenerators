package io.github.sapporo1101.appgen.common.blocks.cells;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blocks.BlockBaseGui;
import io.github.sapporo1101.appgen.common.tileentities.TileFluxCell;
import io.github.sapporo1101.appgen.container.ContainerFluxCell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class FluxCell extends BlockBaseGui<TileFluxCell> {
    public FluxCell() {
        super(glassProps().noOcclusion().isViewBlocking((a, b, c) -> false));
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter blockGetter, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter blockGetter, @NotNull BlockPos pos) {
        return 0.5f;
    }

    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter blockGetter, @NotNull BlockPos p4) {
        return true;
    }

    @Override
    public void openGui(TileFluxCell tile, Player p) {
        MenuOpener.open(ContainerFluxCell.TYPE, p, MenuLocators.forBlockEntity(tile));
    }
}
