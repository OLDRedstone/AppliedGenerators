package io.github.sapporo1101.appgen.container;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.tileentities.TileFluxCell;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class ContainerFluxCell extends AEBaseMenu {

    public static final MenuType<ContainerFluxCell> TYPE = MenuTypeBuilder
            .create(ContainerFluxCell::new, TileFluxCell.class)
            .buildUnregistered(AppliedGenerators.id("ingredient_buffer"));

    public ContainerFluxCell(int id, Inventory playerInventory, TileFluxCell host) {
        super(TYPE, id, playerInventory, host);
        for (int index = 0; index < host.getGenericInv().size(); index++) {
            this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getGenericInv()), index), SlotSemantics.STORAGE);
        }
        this.createPlayerInventorySlots(playerInventory);
    }

}