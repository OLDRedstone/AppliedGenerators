package io.github.sapporo1101.appgen.api;

import com.mojang.serialization.Codec;
import io.github.sapporo1101.appgen.AppliedGenerators;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

public class AGComponents {

    @ApiStatus.Internal
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister
            .create(Registries.DATA_COMPONENT_TYPE, AppliedGenerators.MODID);

    @SuppressWarnings("SameParameterValue")
    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DR.register(name, () -> componentType);
        return componentType;
    }

    public static final DataComponentType<Double> STORED_ENERGY = register("stored_energy",
            builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
}
