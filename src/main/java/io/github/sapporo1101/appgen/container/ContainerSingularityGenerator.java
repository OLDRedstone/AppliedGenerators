package io.github.sapporo1101.appgen.container;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class ContainerSingularityGenerator extends UpgradeableMenu<SingularityGeneratorBlockEntity> implements IProgressProvider {

    @GuiSync(3)
    public int generatableFE = 0;

    public static final MenuType<ContainerSingularityGenerator> TYPE = MenuTypeBuilder
            .create(ContainerSingularityGenerator::new, SingularityGeneratorBlockEntity.class)
            .buildUnregistered(AppliedGenerators.id("singularity_generator"));

    public ContainerSingularityGenerator(int id, Inventory playerInventory, SingularityGeneratorBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.addSlot(new AppEngSlot(host.getInternalInventory(), 0), SlotSemantics.MACHINE_INPUT);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.generatableFE = this.getHost().getGeneratableFE();
        }
        super.broadcastChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.generatableFE;
    }

    @Override
    public int getMaxProgress() {
        return this.getHost().getFEPerSingularity();
    }
}
