package io.github.sapporo1101.appgen.container;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class ContainerSingularityGenerator extends AEBaseMenu {

    public static final MenuType<ContainerSingularityGenerator> TYPE = MenuTypeBuilder
            .create(ContainerSingularityGenerator::new, SingularityGeneratorBlockEntity.class)
            .buildUnregistered(AppliedGenerators.id("singularity_generator"));

    public ContainerSingularityGenerator(int id, Inventory playerInventory, SingularityGeneratorBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getGenericInv()), 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getGenericInv()), 1), SlotSemantics.MACHINE_OUTPUT);
        this.createPlayerInventorySlots(playerInventory);
    }
}
