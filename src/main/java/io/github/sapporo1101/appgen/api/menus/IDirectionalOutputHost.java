package io.github.sapporo1101.appgen.api.menus;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.parts.IPart;
import appeng.api.storage.ISubMenuHost;
import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public interface IDirectionalOutputHost extends ISubMenuHost {
    BlockOrientation getOrientation();

    BlockPos getBlockPos();

    Level getLevel();

    EnumSet<RelativeSide> getAllowedOutputs();

    void updateOutputSides(EnumSet<RelativeSide> var1);

    default ItemStack getAdjacentBlock(RelativeSide side) {
        Direction dir = this.getOrientation().getSide(side);
        BlockPos blockPos = this.getBlockPos().relative(dir);
        Level level = this.getLevel();
        if (level == null) {
            return null;
        } else {
            BlockState blockState = level.getBlockState(blockPos);
            ItemStack itemStack = blockState.getBlock().asItem().getDefaultInstance();
            if (blockState.hasBlockEntity()) {
                BlockEntity blockEntity = level.getBlockEntity(blockPos);
                if (blockEntity != null) {
                    if (blockEntity instanceof CableBusBlockEntity cable) {
                        IPart part = cable.getPart(dir.getOpposite());
                        if (part != null) {
                            itemStack = new ItemStack(part.getPartItem().asItem(), 1);
                        }
                    } else {
                        blockEntity.saveToItem(itemStack, level.registryAccess());
                    }
                }
            }

            return itemStack;
        }
    }
}
