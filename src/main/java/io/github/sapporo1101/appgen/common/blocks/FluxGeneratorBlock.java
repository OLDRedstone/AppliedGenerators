package io.github.sapporo1101.appgen.common.blocks;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.FluxGeneratorBlockEntity;
import io.github.sapporo1101.appgen.menu.FluxGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.ParametersAreNonnullByDefault;

public class FluxGeneratorBlock<T extends FluxGeneratorBlockEntity> extends BlockBaseGui<T> {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public FluxGeneratorBlock() {
        super(metalProps().strength(4.2F));
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, FluxGeneratorBlockEntity be) {
        be.isOn = be.shouldEnabled() && !be.isPulseMode() || be.pulse > 0 && be.isPulseMode();
        return currentState.setValue(ACTIVE, be.isOn);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        System.out.println("Neighbor changed for FluxGeneratorBlock at " + pos);
        final FluxGeneratorBlockEntity be = this.getBlockEntity(level, pos);
        if (be != null) be.updateRedstoneState();
    }

    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.full();
    }

    @Override
    public void openGui(FluxGeneratorBlockEntity tile, Player p) {
        MenuOpener.open(FluxGeneratorMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }

    public static class FG1k extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG1k> {
    }

    public static class FG4k extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG4k> {
    }

    public static class FG16k extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG16k> {
    }

    public static class FG64k extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG64k> {
    }

    public static class FG256k extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG256k> {
    }

    public static class FG1m extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG1m> {
    }

    public static class FG4m extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG4m> {
    }

    public static class FG16m extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG16m> {
    }

    public static class FG64m extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG64m> {
    }

    public static class FG256m extends FluxGeneratorBlock<FluxGeneratorBlockEntity.FG256m> {
    }
}
