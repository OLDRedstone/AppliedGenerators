package io.github.sapporo1101.appgen.common;

import appeng.api.AECapabilities;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.upgrades.Upgrades;
import appeng.block.AEBaseBlockItem;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.powersink.AEBasePoweredBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;
import com.glodblock.github.glodium.registry.RegistryHandler;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.api.IGenericInternalInvHost;
import io.github.sapporo1101.appgen.container.ContainerFluxCell;
import io.github.sapporo1101.appgen.container.ContainerSingularityGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.commons.lang3.tuple.Pair;

public class AGRegistryHandler extends RegistryHandler {

    public static final AGRegistryHandler INSTANCE = new AGRegistryHandler();

    @SuppressWarnings("UnstableApiUsage")
    public AGRegistryHandler() {
        super(AppliedGenerators.MODID);
        this.cap(AEBaseInvBlockEntity.class, Capabilities.ItemHandler.BLOCK, AEBaseInvBlockEntity::getExposedItemHandler);
        this.cap(AEBasePoweredBlockEntity.class, Capabilities.EnergyStorage.BLOCK, AEBasePoweredBlockEntity::getEnergyStorage);
        this.cap(IInWorldGridNodeHost.class, AECapabilities.IN_WORLD_GRID_NODE_HOST, (object, context) -> object);
        this.cap(IAEItemPowerStorage.class, Capabilities.EnergyStorage.ITEM, (object, context) -> new PoweredItemCapabilities(object, (IAEItemPowerStorage) object.getItem()));
        this.cap(ICraftingMachine.class, AECapabilities.CRAFTING_MACHINE, (object, context) -> object);
        this.cap(IGenericInternalInvHost.class, AECapabilities.GENERIC_INTERNAL_INV, IGenericInternalInvHost::getGenericInv);
    }

    public <T extends AEBaseBlockEntity> void block(String name, AEBaseEntityBlock<T> block, Class<T> clazz, BlockEntityType.BlockEntitySupplier<? extends T> supplier) {
        bindTileEntity(clazz, block, supplier);
        block(name, block, b -> new AEBaseBlockItem(b, new Item.Properties()));
        tile(name, block.getBlockEntityType());
    }

    @Override
    public void runRegister() {
        super.runRegister();
        this.onRegisterContainer();
    }

    @SubscribeEvent
    public void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        super.onRegisterCapabilities(event);
    }

    private void onRegisterContainer() {
        Registry.register(BuiltInRegistries.MENU, AppliedGenerators.id("flux_cell"), ContainerFluxCell.TYPE);
        Registry.register(BuiltInRegistries.MENU, AppliedGenerators.id("singularity_generator_1k"), ContainerSingularityGenerator.TYPE);
    }

    private <T extends AEBaseBlockEntity> void bindTileEntity(Class<T> clazz, AEBaseEntityBlock<T> block, BlockEntityType.BlockEntitySupplier<? extends T> supplier) {
        BlockEntityTicker<T> serverTicker = null;
        if (ServerTickingBlockEntity.class.isAssignableFrom(clazz)) {
            serverTicker = (level, pos, state, entity) -> ((ServerTickingBlockEntity) entity).serverTick();
        }
        BlockEntityTicker<T> clientTicker = null;
        if (ClientTickingBlockEntity.class.isAssignableFrom(clazz)) {
            clientTicker = (level, pos, state, entity) -> ((ClientTickingBlockEntity) entity).clientTick();
        }
        block.setBlockEntity(clazz, GlodUtil.getTileType(clazz, supplier, block), clientTicker, serverTicker);
    }

    public void registerTab(Registry<CreativeModeTab> registry) {
        var tab = CreativeModeTab.builder()
                .icon(() -> new ItemStack(AGSingletons.FLUX_CELL))
                .title(Component.translatable("itemGroup.appgen"))
                .displayItems((p, o) -> {
                    for (Pair<String, Item> entry : items) {
                        if (entry.getRight() instanceof AEBaseItem aeItem) {
                            aeItem.addToMainCreativeTab(p, o);
                        } else {
                            o.accept(entry.getRight());
                        }
                    }
                    for (Pair<String, Block> entry : blocks) {
                        o.accept(entry.getRight());
                    }
                })
                .build();
        Registry.register(registry, AppliedGenerators.id("tab_main"), tab);
    }

    public void onInit() {
        for (Pair<String, Block> entry : blocks) {
            Block block = entry.getRight();
            if (block instanceof AEBaseEntityBlock<?>) {
                AEBaseBlockEntity.registerBlockEntityItem(
                        ((AEBaseEntityBlock<?>) block).getBlockEntityType(),
                        block.asItem()
                );
            }
        }
        this.registerAEUpgrade();
    }

    public void registerAEUpgrade() {
        Upgrades.add(AEItems.SPEED_CARD, AGSingletons.SINGULARITY_GENERATOR_1K, 3);
        Upgrades.add(AEItems.ENERGY_CARD, AGSingletons.SINGULARITY_GENERATOR_1K, 3);
    }
}
