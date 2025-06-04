package io.github.sapporo1101.appgen.common;

import io.github.sapporo1101.appgen.common.blockentities.FluxCellBlockEntity;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import io.github.sapporo1101.appgen.common.blocks.fluxcells.FluxCell;
import io.github.sapporo1101.appgen.common.blocks.singularitygenerators.SingularityGenerator;

public class AGSingletons {
    public static FluxCell FLUX_CELL;
    public static SingularityGenerator SINGULARITY_GENERATOR;

    public static void init(AGRegistryHandler regHandler) {
        FLUX_CELL = new FluxCell();
        SINGULARITY_GENERATOR = new SingularityGenerator();
        regHandler.block("flux_cell", FLUX_CELL, FluxCellBlockEntity.class, FluxCellBlockEntity::new);
        regHandler.block("singularity_generator", SINGULARITY_GENERATOR, SingularityGeneratorBlockEntity.class, SingularityGeneratorBlockEntity::new);
    }
}
