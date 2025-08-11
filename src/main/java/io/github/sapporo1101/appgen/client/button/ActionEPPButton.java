package io.github.sapporo1101.appgen.client.button;

import appeng.client.gui.style.Blitter;
import net.minecraft.client.gui.components.Button;

public class ActionEPPButton extends EPPButton {
    private final Blitter icon;

    public ActionEPPButton(Button.OnPress onPress, Blitter icon) {
        super(onPress);
        this.icon = icon;
    }

    protected Blitter getBlitterIcon() {
        return this.icon;
    }
}
