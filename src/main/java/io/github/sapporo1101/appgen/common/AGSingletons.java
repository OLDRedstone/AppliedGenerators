package io.github.sapporo1101.appgen.common;

import io.github.sapporo1101.appgen.common.blocks.cells.FluxCell;
import io.github.sapporo1101.appgen.common.tileentities.TileFluxCell;

public class AGSingletons {
    public static FluxCell flux_cell;

    public static void init(AGRegistryHandler regHandler) {
        flux_cell = new FluxCell();
        regHandler.block("flux_cell", flux_cell, TileFluxCell.class, TileFluxCell::new);
    }
}
