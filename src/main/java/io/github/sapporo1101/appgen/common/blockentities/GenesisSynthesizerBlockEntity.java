package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigManager;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemDefinitionFilter;
import appeng.util.inv.filter.AEItemFilters;
import appeng.util.inv.filter.IAEItemFilter;
import com.glodblock.github.extendedae.api.IRecipeMachine;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.glodium.recipe.RecipeSearchContext;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.recipe.GenesisSynthesizerRecipe;
import io.github.sapporo1101.appgen.xmod.ExternalTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class GenesisSynthesizerBlockEntity extends AENetworkedPoweredBlockEntity implements IUpgradeableObject, IConfigurableObject, IRecipeMachine<RecipeInput, GenesisSynthesizerRecipe> {

    public static final long POWER_MAXIMUM_AMOUNT = 1000000L;
    public static final int MAX_PROGRESS = 200;

    private final AppEngInternalInventory inputInv = new AppEngInternalInventory(this, 9, 64, new RestrictSingularityFilter());
    private final AppEngInternalInventory singularityInv = new AppEngInternalInventory(this, 1, 64, new AEItemDefinitionFilter(AEItems.SINGULARITY));
    private final AppEngInternalInventory outputInv = new AppEngInternalInventory(this, 1, 64);
    private final InternalInventory combinedInputInv = new CombinedInternalInventory(this.inputInv, this.singularityInv);
    private final InternalInventory inv = new CombinedInternalInventory(this.combinedInputInv, this.outputInv);
    private final FilteredInternalInventory combinedInputExposed = new FilteredInternalInventory(this.combinedInputInv, AEItemFilters.INSERT_ONLY);
    private final FilteredInternalInventory outputExposed = new FilteredInternalInventory(this.outputInv, AEItemFilters.EXTRACT_ONLY);
    private final InternalInventory invExposed = new CombinedInternalInventory(this.combinedInputExposed, this.outputExposed);
    private final CustomTankInv tankInv = new CustomTankInv(this::onChangeTank, GenericStackInv.Mode.STORAGE, 2);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(AGSingletons.GENESIS_SYNTHESIZER, 4, this::saveChanges);
    private final ConfigManager configManager = new ConfigManager(this::onConfigChanged);
    private boolean isWorking = false;
    private int progress = 0;


    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    public GenesisSynthesizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(GenesisSynthesizerBlockEntity.class, GenesisSynthesizerBlockEntity::new, AGSingletons.GENESIS_SYNTHESIZER), pos, blockState);
        this.setInternalMaxPower(POWER_MAXIMUM_AMOUNT);
        this.setPowerSides(getGridConnectableSides(getOrientation()));
        this.configManager.registerSetting(Settings.AUTO_EXPORT, YesNo.NO);
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.complementOf(EnumSet.of(orientation.getSide(RelativeSide.TOP)));
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        var changed = super.readFromStream(data);
        boolean newIsWorking = data.readBoolean();
        if (this.isWorking != newIsWorking) {
            this.isWorking = newIsWorking;
            changed = true;
        }
        int newProgress = data.readInt();
        if (this.progress != newProgress) {
            this.progress = newProgress;
            changed = true;
        }
//        var outputStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
//        if (!ItemStack.isSameItem(outputStack, this.renderOutput)) {
//            this.renderOutput = outputStack;
//            changed = true;
//        }
        return changed;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isWorking);
        data.writeInt(this.progress);
//        this.renderOutput = this.ctx.currentRecipe == null ? ItemStack.EMPTY : this.ctx.currentRecipe.value().output;
//        ItemStack.OPTIONAL_STREAM_CODEC.encode(data, this.renderOutput);
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            var nbt = new CompoundTag();
            var sides = new ListTag();
            for (var side : this.outputSides) {
                sides.add(StringTag.valueOf(side.getName()));
            }
            nbt.put("output_side", sides);
            output.set(EAESingletons.EXTRA_SETTING, nbt);
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.tankInv.size(); index++) {
            var stack = this.tankInv.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
        for (var upgrade : upgrades) {
            drops.add(upgrade);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.tankInv.writeToChildTag(data, "tank", registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        this.configManager.writeToNBT(data, registries);
        var sides = new ListTag();
        for (var side : this.outputSides) {
            sides.add(StringTag.valueOf(side.getName()));
        }
        data.put("output_side", sides);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.tankInv.readFromChildTag(data, "tank", registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        this.configManager.writeToNBT(data, registries);
        this.outputSides.clear();
        if (data.contains("output_side")) {
            var list = data.getList("output_side", CompoundTag.TAG_STRING);
            for (var name : list) {
                this.outputSides.add(Direction.byName(name.getAsString()));
            }
        } else {
            this.outputSides.addAll(List.of(Direction.values()));
        }
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.tankInv.clear();
        this.upgrades.clear();
    }

    private void onChangeTank() {
        onChangeInventory();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        onChangeInventory();
    }

    private void onChangeInventory() {
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    private void onConfigChanged(IConfigManager manager, Setting<?> setting) {
        if (setting == Settings.AUTO_EXPORT) {
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        this.saveChanges();
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    public InternalInventory getInputInv() {
        return this.inputInv;
    }

    public InternalInventory getSingularityInv() {
        return this.singularityInv;
    }

    public InternalInventory getOutputExposed() {
        return this.outputExposed;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(Direction facing) {
        return this.invExposed;
    }

    public GenericStackInv getTank() {
        return this.tankInv;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    public Set<Direction> getOutputSides() {
        return this.outputSides;
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public void addProgress(int delta) {
        this.progress += delta;
    }

    @Override
    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public RecipeSearchContext<RecipeInput, GenesisSynthesizerRecipe> getContext() {
        return null;
    }

    public boolean isWorking() {
        return this.isWorking;
    }

    @Override
    public void setWorking(boolean work) {
        boolean oldVal = this.isWorking;
        this.isWorking = work;
        if (oldVal != work) {
            this.markForUpdate();
        }
    }

    @Override
    public InternalInventory getOutput() {
        return this.outputInv;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.getInternalInventory();
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }

        return super.getSubInventory(id);
    }

    private static class RestrictSingularityFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !AEItems.SINGULARITY.is(stack);
        }
    }

    private static class CustomTankInv extends GenericStackInv {
        public CustomTankInv(@Nullable Runnable listener, Mode mode, int size) {
            super(listener, mode, size);
            this.setCapacity(AEKeyType.items(), 0);
            this.setCapacity(AEKeyType.fluids(), 16000);
            if (ExternalTypes.GAS != null) this.setCapacity(ExternalTypes.GAS, 16000);
            if (ExternalTypes.MANA != null) this.setCapacity(ExternalTypes.MANA, 1000);
            if (ExternalTypes.SOURCE != null) this.setCapacity(ExternalTypes.SOURCE, 1000);
            if (ExternalTypes.FLUX != null) this.setCapacity(ExternalTypes.FLUX, 0);
        }

        @Override
        public boolean isAllowedIn(int slot, AEKey what) {
            if (slot == 1) return false;

            return super.isAllowedIn(slot, what);
        }

        @Override
        public long extract(int slot, AEKey what, long amount, Actionable mode) {
            if (slot == 0) return 0L;

            return super.extract(slot, what, amount, mode);
        }

        public boolean canAdd(int slot, AEKey key, int amount) {
            var stack = this.getStack(slot);
            if (stack == null) return true;
            if (!stack.what().equals(key)) return false;
            return stack.amount() + amount <= this.getMaxAmount(key);
        }

        public int add(int slot, AEKey key, int amount) {
            if (!canAdd(slot, key, amount)) return 0;

            var stack = this.getStack(slot);
            var newAmount = amount;
            if (stack != null) newAmount += (int) stack.amount();
            assert stack != null;
            this.setStack(slot, new GenericStack(key, newAmount));
            return amount;
        }

        public void clear(int slot) {
            boolean changed = this.stacks[slot] != null;
            this.setStack(slot, null);

            if (changed) {
                onChange();
            }
        }
    }
}
