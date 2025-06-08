package io.github.sapporo1101.appgen.common;

import io.github.sapporo1101.appgen.common.blockentities.FluxCellBlockEntity;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import io.github.sapporo1101.appgen.common.blocks.fluxcells.FluxCell;
import io.github.sapporo1101.appgen.common.blocks.singularitygenerators.SingularityGeneratorBlock;

public class AGSingletons {
    public static FluxCell FLUX_CELL;
    public static SingularityGeneratorBlock.SG1k SINGULARITY_GENERATOR_1K;

    public static void init(AGRegistryHandler regHandler) {
        FLUX_CELL = new FluxCell();
        SINGULARITY_GENERATOR_1K = new SingularityGeneratorBlock.SG1k();
        regHandler.block("flux_cell", FLUX_CELL, FluxCellBlockEntity.class, FluxCellBlockEntity::new);
        regHandler.block("singularity_generator_1k", SINGULARITY_GENERATOR_1K, SingularityGeneratorBlockEntity.SG1k.class, SingularityGeneratorBlockEntity.SG1k::new);
    }
}
