package io.github.sapporo1101.appgen.common.blocks;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.sapporo1101.appgen.common.blockentities.PatternBufferBlockEntity;
import io.github.sapporo1101.appgen.menu.PatternBufferMenu;
import net.minecraft.world.entity.player.Player;

public class PatternBufferBlock extends BlockBaseGui<PatternBufferBlockEntity> {
    public PatternBufferBlock() {
        super(glassProps());
    }

    @Override
    public void openGui(PatternBufferBlockEntity tile, Player p) {
        MenuOpener.open(PatternBufferMenu.TYPE, p, MenuLocators.forBlockEntity(tile));
    }
}
