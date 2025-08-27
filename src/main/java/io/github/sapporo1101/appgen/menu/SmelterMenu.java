package io.github.sapporo1101.appgen.menu;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.blockentities.SmelterBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class SmelterMenu extends UpgradeableMenu<SmelterBlockEntity> implements IProgressProvider {

    private final SmelterBlockEntity host;

    @GuiSync(3)
    public int maxProgress;

    @GuiSync(4)
    public int progress;

    public static final MenuType<SmelterMenu> TYPE = MenuTypeBuilder
            .create(SmelterMenu::new, SmelterBlockEntity.class)
            .buildUnregistered(AppliedGenerators.id("smelter"));

    public SmelterMenu(int id, Inventory playerInventory, SmelterBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        this.addSlot(new AppEngSlot(host.getInputInv(), 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngSlot(host.getOutputExposed(), 0), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.progress = this.host.getProgress();
            this.maxProgress = this.host.getMaxProgress();
        }
        super.broadcastChanges();
    }

    @Override
    public int getCurrentProgress() {
        return (int) Math.ceil((double) this.progress / this.maxProgress * this.getMaxProgress());
    }

    @Override
    public int getMaxProgress() {
        return 10;
    }
}
