package io.github.sapporo1101.appgen.common.blocks;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import io.github.sapporo1101.appgen.menu.SingularityGeneratorMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SingularityGeneratorBlock<T extends SingularityGeneratorBlockEntity> extends BlockBaseGui<T> {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SingularityGeneratorBlock() {
        super(metalProps().strength(4.2F));
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, SingularityGeneratorBlockEntity be) {
        be.isOn = be.getGeneratableFE() > 0 || be.canEatFuel();
        return currentState.setValue(ACTIVE, be.isOn);
    }

    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.full();
    }

    @Override
    public void openGui(SingularityGeneratorBlockEntity tile, Player p) {
        MenuOpener.open(SingularityGeneratorMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }

    public static class SG1k extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG1k> {
    }

    public static class SG4k extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG4k> {
    }

    public static class SG16k extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG16k> {
    }

    public static class SG64k extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG64k> {
    }

    public static class SG256k extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG256k> {
    }

    public static class SG1m extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG1m> {
    }

    public static class SG4m extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG4m> {
    }

    public static class SG16m extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG16m> {
    }

    public static class SG64m extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG64m> {
    }

    public static class SG256m extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG256m> {
    }
}
