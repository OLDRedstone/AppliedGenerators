package io.github.sapporo1101.appgen.menu;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import com.glodblock.github.extendedae.container.helper.DirectionSet;
import com.glodblock.github.glodium.network.packet.sync.ActionMap;
import com.glodblock.github.glodium.network.packet.sync.IActionHolder;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.blockentities.GenesisSynthesizerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GenesisSynthesizerMenu extends UpgradeableMenu<GenesisSynthesizerBlockEntity> implements IProgressProvider, IActionHolder {

    public static final SlotSemantic AG_TANK_INPUT = SlotSemantics.register("AG_TANK_INPUT", false);
    public static final SlotSemantic AG_TANK_OUTPUT = SlotSemantics.register("AG_TANK_OUTPUT", false);
    @GuiSync(3)
    public int processingTime = -1;

    @GuiSync(8)
    public YesNo autoExport = YesNo.NO;

    @GuiSync(9)
    public DirectionSet outputSides = new DirectionSet(new ArrayList<>());

    private final ActionMap actions = ActionMap.create();

    public static final MenuType<GenesisSynthesizerMenu> TYPE = MenuTypeBuilder
            .create(GenesisSynthesizerMenu::new, GenesisSynthesizerBlockEntity.class)
            .buildUnregistered(AppliedGenerators.id("genesis_synthesizer"));

    public GenesisSynthesizerMenu(int id, Inventory inventory, GenesisSynthesizerBlockEntity host) {
        super(TYPE, id, inventory, host);
        for (int index = 0; index < host.getInputInv().size(); index++) {
            this.addSlot(new AppEngSlot(host.getInputInv(), index), SlotSemantics.MACHINE_INPUT);
        }
        this.addSlot(new AppEngSlot(host.getSingularityInv(), 0), SlotSemantics.STORAGE);
        for (int index = 0; index < host.getOutputExposed().size(); index++) {
            this.addSlot(new AppEngSlot(host.getOutputExposed(), index), SlotSemantics.MACHINE_OUTPUT);
        }
        this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getTank()), 0), AG_TANK_INPUT);
        this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getTank()), 1), AG_TANK_OUTPUT);
        this.actions.put("set_side", o -> this.setOutputSide(o.get(0), o.get(1)));
    }

    private void setOutputSide(String name, boolean value) {
        var side = Direction.byName(name);
        if (value) {
            this.getHost().getOutputSides().add(side);
        } else {
            this.getHost().getOutputSides().remove(side);
        }
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.autoExport = cm.getSetting(Settings.AUTO_EXPORT);
        this.outputSides.clear();
        this.outputSides.addAll(this.getHost().getOutputSides());
    }

    @Override
    protected void standardDetectAndSendChanges() {
        if (isServerSide()) {
            this.processingTime = getHost().getProgress();
        }
        super.standardDetectAndSendChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.processingTime;
    }

    @Override
    public int getMaxProgress() {
        return GenesisSynthesizerBlockEntity.MAX_PROGRESS;
    }

    public YesNo getAutoExport() {
        return autoExport;
    }

    public List<Direction> getOutputSides() {
        return outputSides.sides();
    }

    @NotNull
    @Override
    public ActionMap getActionMap() {
        return this.actions;
    }
}
