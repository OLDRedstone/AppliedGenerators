package io.github.sapporo1101.appgen.common.blocks;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.SmelterBlockEntity;
import io.github.sapporo1101.appgen.menu.SmelterMenu;
import net.minecraft.world.entity.player.Player;

public class SmelterBlock extends BlockBaseGui<SmelterBlockEntity> {

    public SmelterBlock() {
        super(metalProps());
    }

    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.full();
    }

    @Override
    public void openGui(SmelterBlockEntity tile, Player p) {
        MenuOpener.open(SmelterMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }
}
