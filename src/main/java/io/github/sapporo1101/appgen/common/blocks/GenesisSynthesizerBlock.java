package io.github.sapporo1101.appgen.common.blocks;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.GenesisSynthesizerBlockEntity;
import io.github.sapporo1101.appgen.menu.GenesisSynthesizerMenu;
import net.minecraft.world.entity.player.Player;

public class GenesisSynthesizerBlock extends BlockBaseGui<GenesisSynthesizerBlockEntity> {

    public GenesisSynthesizerBlock() {
        super(metalProps());
    }

    @Override
    public void openGui(GenesisSynthesizerBlockEntity tile, Player p) {
        MenuOpener.open(GenesisSynthesizerMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }
}
