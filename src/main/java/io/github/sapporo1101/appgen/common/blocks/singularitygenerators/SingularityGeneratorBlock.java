package io.github.sapporo1101.appgen.common.blocks.singularitygenerators;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.core.AEConfig;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import io.github.sapporo1101.appgen.common.blocks.BlockBaseGui;
import io.github.sapporo1101.appgen.container.ContainerSingularityGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

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
        MenuOpener.open(ContainerSingularityGenerator.TYPE, p, MenuLocators.forBlockEntity(tile));
    }

    public static class SG1k extends SingularityGeneratorBlock<SingularityGeneratorBlockEntity.SG1k> {
    }
}
