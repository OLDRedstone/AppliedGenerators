package io.github.sapporo1101.appgen.container;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class ContainerSingularityGenerator extends AEBaseMenu implements IProgressProvider {

    @GuiSync(3)
    public int generatableFE = 0;

    public SingularityGeneratorBlockEntity host;

    public static final MenuType<ContainerSingularityGenerator> TYPE = MenuTypeBuilder
            .create(ContainerSingularityGenerator::new, SingularityGeneratorBlockEntity.class)
            .buildUnregistered(AppliedGenerators.id("singularity_generator"));

    public ContainerSingularityGenerator(int id, Inventory playerInventory, SingularityGeneratorBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getGenericInv().getInv(0)), 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getGenericInv().getInv(1)), 0), SlotSemantics.MACHINE_OUTPUT);
        this.createPlayerInventorySlots(playerInventory);
        this.host = host;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.generatableFE = this.host.getGeneratableFE();
        }
        super.broadcastChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.generatableFE;
    }

    @Override
    public int getMaxProgress() {
        return SingularityGeneratorBlockEntity.FE_PER_SINGULARITY;
    }
}
