package io.github.sapporo1101.appgen.container;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import com.glodblock.github.extendedae.container.helper.DirectionSet;
import com.glodblock.github.glodium.network.packet.sync.ActionMap;
import com.glodblock.github.glodium.network.packet.sync.IActionHolder;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.common.blockentities.FluxCellBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ContainerFluxCell extends AEBaseMenu implements IActionHolder {

    @GuiSync(9)
    public DirectionSet outputSides = new DirectionSet(new ArrayList<>());

    private final FluxCellBlockEntity host;
    private final ActionMap actions = ActionMap.create();

    public static final MenuType<ContainerFluxCell> TYPE = MenuTypeBuilder
            .create(ContainerFluxCell::new, FluxCellBlockEntity.class)
            .buildUnregistered(AppliedGenerators.id("flux_cell"));

    public ContainerFluxCell(int id, Inventory playerInventory, FluxCellBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        for (int index = 0; index < host.getGenericInv().size(); index++) {
            this.addSlot(new AppEngSlot(new ConfigMenuInventory(host.getGenericInv()), index), SlotSemantics.STORAGE);
        }
        this.createPlayerInventorySlots(playerInventory);
        this.actions.put("set_side", o -> this.setOutputSide(o.get(0), o.get(1)));
    }

    private void setOutputSide(String name, boolean value) {
        var side = Direction.byName(name);
        if (value) {
            this.host.getOutputSides().add(side);
        } else {
            this.host.getOutputSides().remove(side);
        }
    }

    @Override
    public void broadcastChanges() {
        this.outputSides.clear();
        this.outputSides.addAll(this.host.getOutputSides());
        super.broadcastChanges();
    }

    public List<Direction> getOutputSides() {
        return outputSides.sides();
    }

    public FluxCellBlockEntity getHost() {
        return host;
    }

    @Override
    public @NotNull ActionMap getActionMap() {
        return this.actions;
    }
}