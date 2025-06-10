package io.github.sapporo1101.appgen.common;

import io.github.sapporo1101.appgen.common.blockentities.FluxCellBlockEntity;
import io.github.sapporo1101.appgen.common.blockentities.FluxGeneratorBlockEntity;
import io.github.sapporo1101.appgen.common.blockentities.SingularityGeneratorBlockEntity;
import io.github.sapporo1101.appgen.common.blocks.FluxCellBlock;
import io.github.sapporo1101.appgen.common.blocks.FluxGeneratorBlock;
import io.github.sapporo1101.appgen.common.blocks.SingularityGeneratorBlock;

public class AGSingletons {
    public static FluxCellBlock FLUX_CELL;
    public static SingularityGeneratorBlock.SG1k SINGULARITY_GENERATOR_1K;
    public static SingularityGeneratorBlock.SG4k SINGULARITY_GENERATOR_4K;
    public static SingularityGeneratorBlock.SG16k SINGULARITY_GENERATOR_16K;
    public static SingularityGeneratorBlock.SG64k SINGULARITY_GENERATOR_64K;
    public static SingularityGeneratorBlock.SG256k SINGULARITY_GENERATOR_256K;
    public static SingularityGeneratorBlock.SG1m SINGULARITY_GENERATOR_1M;
    public static SingularityGeneratorBlock.SG4m SINGULARITY_GENERATOR_4M;
    public static SingularityGeneratorBlock.SG16m SINGULARITY_GENERATOR_16M;
    public static SingularityGeneratorBlock.SG64m SINGULARITY_GENERATOR_64M;
    public static SingularityGeneratorBlock.SG256m SINGULARITY_GENERATOR_256M;
    public static FluxGeneratorBlock.FG1k FLUX_GENERATOR_1K;
    public static FluxGeneratorBlock.FG4k FLUX_GENERATOR_4K;
    public static FluxGeneratorBlock.FG16k FLUX_GENERATOR_16K;
    public static FluxGeneratorBlock.FG64k FLUX_GENERATOR_64K;
    public static FluxGeneratorBlock.FG256k FLUX_GENERATOR_256K;
    public static FluxGeneratorBlock.FG1m FLUX_GENERATOR_1M;
    public static FluxGeneratorBlock.FG4m FLUX_GENERATOR_4M;
    public static FluxGeneratorBlock.FG16m FLUX_GENERATOR_16M;
    public static FluxGeneratorBlock.FG64m FLUX_GENERATOR_64M;
    public static FluxGeneratorBlock.FG256m FLUX_GENERATOR_256M;

    public static void init(AGRegistryHandler regHandler) {
        FLUX_CELL = new FluxCellBlock();
        SINGULARITY_GENERATOR_1K = new SingularityGeneratorBlock.SG1k();
        SINGULARITY_GENERATOR_4K = new SingularityGeneratorBlock.SG4k();
        SINGULARITY_GENERATOR_16K = new SingularityGeneratorBlock.SG16k();
        SINGULARITY_GENERATOR_64K = new SingularityGeneratorBlock.SG64k();
        SINGULARITY_GENERATOR_256K = new SingularityGeneratorBlock.SG256k();
        SINGULARITY_GENERATOR_1M = new SingularityGeneratorBlock.SG1m();
        SINGULARITY_GENERATOR_4M = new SingularityGeneratorBlock.SG4m();
        SINGULARITY_GENERATOR_16M = new SingularityGeneratorBlock.SG16m();
        SINGULARITY_GENERATOR_64M = new SingularityGeneratorBlock.SG64m();
        SINGULARITY_GENERATOR_256M = new SingularityGeneratorBlock.SG256m();
        FLUX_GENERATOR_1K = new FluxGeneratorBlock.FG1k();
        FLUX_GENERATOR_4K = new FluxGeneratorBlock.FG4k();
        FLUX_GENERATOR_16K = new FluxGeneratorBlock.FG16k();
        FLUX_GENERATOR_64K = new FluxGeneratorBlock.FG64k();
        FLUX_GENERATOR_256K = new FluxGeneratorBlock.FG256k();
        FLUX_GENERATOR_1M = new FluxGeneratorBlock.FG1m();
        FLUX_GENERATOR_4M = new FluxGeneratorBlock.FG4m();
        FLUX_GENERATOR_16M = new FluxGeneratorBlock.FG16m();
        FLUX_GENERATOR_64M = new FluxGeneratorBlock.FG64m();
        FLUX_GENERATOR_256M = new FluxGeneratorBlock.FG256m();

        regHandler.block("flux_cell", FLUX_CELL, FluxCellBlockEntity.class, FluxCellBlockEntity::new);
        regHandler.block("singularity_generator_1k", SINGULARITY_GENERATOR_1K, SingularityGeneratorBlockEntity.SG1k.class, SingularityGeneratorBlockEntity.SG1k::new);
        regHandler.block("singularity_generator_4k", SINGULARITY_GENERATOR_4K, SingularityGeneratorBlockEntity.SG4k.class, SingularityGeneratorBlockEntity.SG4k::new);
        regHandler.block("singularity_generator_16k", SINGULARITY_GENERATOR_16K, SingularityGeneratorBlockEntity.SG16k.class, SingularityGeneratorBlockEntity.SG16k::new);
        regHandler.block("singularity_generator_64k", SINGULARITY_GENERATOR_64K, SingularityGeneratorBlockEntity.SG64k.class, SingularityGeneratorBlockEntity.SG64k::new);
        regHandler.block("singularity_generator_256k", SINGULARITY_GENERATOR_256K, SingularityGeneratorBlockEntity.SG256k.class, SingularityGeneratorBlockEntity.SG256k::new);
        regHandler.block("singularity_generator_1m", SINGULARITY_GENERATOR_1M, SingularityGeneratorBlockEntity.SG1m.class, SingularityGeneratorBlockEntity.SG1m::new);
        regHandler.block("singularity_generator_4m", SINGULARITY_GENERATOR_4M, SingularityGeneratorBlockEntity.SG4m.class, SingularityGeneratorBlockEntity.SG4m::new);
        regHandler.block("singularity_generator_16m", SINGULARITY_GENERATOR_16M, SingularityGeneratorBlockEntity.SG16m.class, SingularityGeneratorBlockEntity.SG16m::new);
        regHandler.block("singularity_generator_64m", SINGULARITY_GENERATOR_64M, SingularityGeneratorBlockEntity.SG64m.class, SingularityGeneratorBlockEntity.SG64m::new);
        regHandler.block("singularity_generator_256m", SINGULARITY_GENERATOR_256M, SingularityGeneratorBlockEntity.SG256m.class, SingularityGeneratorBlockEntity.SG256m::new);
        regHandler.block("flux_generator_1k", FLUX_GENERATOR_1K, FluxGeneratorBlockEntity.FG1k.class, FluxGeneratorBlockEntity.FG1k::new);
        regHandler.block("flux_generator_4k", FLUX_GENERATOR_4K, FluxGeneratorBlockEntity.FG4k.class, FluxGeneratorBlockEntity.FG4k::new);
        regHandler.block("flux_generator_16k", FLUX_GENERATOR_16K, FluxGeneratorBlockEntity.FG16k.class, FluxGeneratorBlockEntity.FG16k::new);
        regHandler.block("flux_generator_64k", FLUX_GENERATOR_64K, FluxGeneratorBlockEntity.FG64k.class, FluxGeneratorBlockEntity.FG64k::new);
        regHandler.block("flux_generator_256k", FLUX_GENERATOR_256K, FluxGeneratorBlockEntity.FG256k.class, FluxGeneratorBlockEntity.FG256k::new);
        regHandler.block("flux_generator_1m", FLUX_GENERATOR_1M, FluxGeneratorBlockEntity.FG1m.class, FluxGeneratorBlockEntity.FG1m::new);
        regHandler.block("flux_generator_4m", FLUX_GENERATOR_4M, FluxGeneratorBlockEntity.FG4m.class, FluxGeneratorBlockEntity.FG4m::new);
        regHandler.block("flux_generator_16m", FLUX_GENERATOR_16M, FluxGeneratorBlockEntity.FG16m.class, FluxGeneratorBlockEntity.FG16m::new);
        regHandler.block("flux_generator_64m", FLUX_GENERATOR_64M, FluxGeneratorBlockEntity.FG64m.class, FluxGeneratorBlockEntity.FG64m::new);
        regHandler.block("flux_generator_256m", FLUX_GENERATOR_256M, FluxGeneratorBlockEntity.FG256m.class, FluxGeneratorBlockEntity.FG256m::new);
    }
}
