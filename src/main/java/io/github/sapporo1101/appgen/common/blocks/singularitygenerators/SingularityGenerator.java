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

public class SingularityGenerator extends BlockBaseGui<SingularityGeneratorBlockEntity> {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SingularityGenerator() {
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

    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource r) {
        if (AEConfig.instance().isEnableEffects()) {
            SingularityGeneratorBlockEntity tc = this.getBlockEntity(level, pos);
            if (tc != null && tc.isOn) {
                double f1 = (float) pos.getX() + 0.5F;
                double f2 = (float) pos.getY() + 0.5F;
                double f3 = (float) pos.getZ() + 0.5F;
                Direction front = tc.getFront();
                Direction top = tc.getTop();
                int west_x = front.getStepY() * top.getStepZ() - front.getStepZ() * top.getStepY();
                int west_y = front.getStepZ() * top.getStepX() - front.getStepX() * top.getStepZ();
                int west_z = front.getStepX() * top.getStepY() - front.getStepY() * top.getStepX();
                f1 += (double) front.getStepX() * 0.6;
                f2 += (double) front.getStepY() * 0.6;
                f3 += (double) front.getStepZ() * 0.6;
                double ox = r.nextDouble();
                double oy = r.nextDouble() * (double) 0.2F;
                f1 += (double) top.getStepX() * (-0.3 + oy);
                f2 += (double) top.getStepY() * (-0.3 + oy);
                f3 += (double) top.getStepZ() * (-0.3 + oy);
                f1 += (double) west_x * (0.3 * ox - 0.15);
                f2 += (double) west_y * (0.3 * ox - 0.15);
                f3 += (double) west_z * (0.3 * ox - 0.15);
                level.addParticle(ParticleTypes.SMOKE, f1, f2, f3, 0.0F, 0.0F, 0.0F);
                level.addParticle(ParticleTypes.FLAME, f1, f2, f3, 0.0F, 0.0F, 0.0F);
            }

        }
    }
}
