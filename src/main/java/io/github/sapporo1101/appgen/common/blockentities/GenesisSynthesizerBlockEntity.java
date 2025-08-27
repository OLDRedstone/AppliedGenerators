package io.github.sapporo1101.appgen.common.blockentities;

import appeng.api.config.*;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.*;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigManager;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemFilters;
import appeng.util.inv.filter.IAEItemFilter;
import com.glodblock.github.glodium.util.GlodUtil;
import io.github.sapporo1101.appgen.common.AGSingletons;
import io.github.sapporo1101.appgen.common.blocks.GenesisSynthesizerBlock;
import io.github.sapporo1101.appgen.recipe.GenesisSynthesizerRecipe;
import io.github.sapporo1101.appgen.recipe.GenesisSynthesizerRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GenesisSynthesizerBlockEntity extends AENetworkedPoweredBlockEntity implements IGridTickable, IUpgradeableObject, IConfigurableObject {

    public static final long POWER_MAXIMUM_AMOUNT = 10_000_000;
    public static final int MAX_PROGRESS = 200;
    public static final int MAX_CRYSTAL_TANK = 1000;

    private final AppEngInternalInventory inputInv = new AppEngInternalInventory(this, 9, 64, new RestrictItemFilter(AGSingletons.EMBER_CRYSTAL_CHARGED));
    private final AppEngInternalInventory crystalInv = new AppEngInternalInventory(this, 1, 64, new AllowItemFilter(AGSingletons.EMBER_CRYSTAL_CHARGED));
    private final AppEngInternalInventory outputInv = new AppEngInternalInventory(this, 1, 64);
    private final InternalInventory combinedInputInv = new CombinedInternalInventory(this.inputInv, this.crystalInv);
    private final InternalInventory inv = new CombinedInternalInventory(this.combinedInputInv, this.outputInv);
    private final FilteredInternalInventory combinedInputExposed = new FilteredInternalInventory(this.combinedInputInv, AEItemFilters.INSERT_ONLY);
    private final FilteredInternalInventory outputExposed = new FilteredInternalInventory(this.outputInv, AEItemFilters.EXTRACT_ONLY);
    private final InternalInventory invExposed = new CombinedInternalInventory(this.combinedInputExposed, this.outputExposed);
    private final CustomTankInv tankInv = new CustomTankInv(this::onChangeTank, GenericStackInv.Mode.STORAGE, 2);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(AGSingletons.GENESIS_SYNTHESIZER, 4, this::saveChanges);
    private final ConfigManager configManager = new ConfigManager(this::onConfigChanged);
    private final GenericStackInv crystalTank = new GenericItemTank(null, AGSingletons.EMBER_CRYSTAL_CHARGED);
    private boolean isWorking = false;
    private boolean hasInventoryChanged = false;
    private GenesisSynthesizerRecipe cachedTask = null;
    private int progress = 0;
    private boolean showWarning = false;


    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    public GenesisSynthesizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(GenesisSynthesizerBlockEntity.class, GenesisSynthesizerBlockEntity::new, AGSingletons.GENESIS_SYNTHESIZER), pos, blockState);
        this.getMainNode().setIdlePowerUsage(0).addService(IGridTickable.class, this);
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
        return changed;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isWorking);
        data.writeInt(this.progress);
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        var nbt = input.get(AGSingletons.EXTRA_SETTING);
        if (nbt != null) {
            this.outputSides.clear();
            for (var side : nbt.getList("output_side", CompoundTag.TAG_STRING)) {
                this.outputSides.add(Direction.byName(side.getAsString()));
            }
        }
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
            output.set(AGSingletons.EXTRA_SETTING, nbt);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.tankInv.writeToChildTag(data, "tank", registries);
        this.crystalTank.writeToChildTag(data, "crystal_tank", registries);
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
        this.crystalTank.readFromChildTag(data, "crystal_tank", registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.configManager.readFromNBT(data, registries);
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
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int index = 0; index < this.tankInv.size(); index++) {
            var stack = this.tankInv.getStack(index);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
        var crystalStack = this.crystalTank.getStack(0);
        if (crystalStack != null) {
            crystalStack.what().addDrops(crystalStack.amount(), drops, level, pos);
        }
        for (var upgrade : upgrades) {
            drops.add(upgrade);
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
        this.hasInventoryChanged = true;
        this.chargeCrystalTank();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    private void onConfigChanged(IConfigManager manager, Setting<?> setting) {
        if (setting == Settings.AUTO_EXPORT && configManager.getSetting(Settings.AUTO_EXPORT) == YesNo.YES) {
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
        saveChanges();
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    public InternalInventory getInputInv() {
        return this.inputInv;
    }

    public InternalInventory getCrystalInv() {
        return this.crystalInv;
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

    public int getProgress() {
        return this.progress;
    }

    public void addProgress(int delta) {
        this.progress += delta;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isWorking() {
        return this.isWorking;
    }

    public void setWorking(boolean work) {
        if (work != this.isWorking) {
            this.updateBlockState(work);
            this.markForUpdate();
        }

        this.isWorking = work;
    }

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

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Inscriber, !this.hasAutoExportWork() && !this.hasCraftWork());
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.hasInventoryChanged) {
            if (this.level != null) {
                GenesisSynthesizerRecipe recipe = this.findRecipe(this.level);
                if (recipe == null) {
                    this.setProgress(0);
                    this.setWorking(false);
                    this.cachedTask = null;
                }
            }

            this.markForUpdate();
            this.hasInventoryChanged = false;
        }

        if (this.hasCraftWork() && this.getGridNode() != null && this.getGridNode().isOnline()) {
            this.setWorking(true);
            getMainNode().ifPresent(grid -> {
                IEnergyService eg = grid.getEnergyService();
                IEnergySource src = this;

                final int speedFactor =
                        switch (this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
                            case 1 -> 4; // 50 ticks
                            case 2 -> 8; // 25 ticks
                            case 3 -> 16; // 12 ticks
                            case 4 -> 32; // 6 ticks
                            default -> 2; // 100 ticks
                        };

                final int progressReq = MAX_PROGRESS - this.getProgress();
                final float powerRatio = progressReq < speedFactor ? (float) progressReq / speedFactor : 1;
                final int requiredTicks = Mth.ceil((float) MAX_PROGRESS / speedFactor);
                final int aeConsumption = Mth.floor(((float) Objects.requireNonNull(getTask()).getEnergy() / requiredTicks) * powerRatio);
                final double powerThreshold = aeConsumption - 0.01;

                double powerReq = this.extractAEPower(aeConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);

                if (powerReq <= powerThreshold) {
                    src = eg;
                    var oldPowerReq = powerReq;
                    powerReq = eg.extractAEPower(aeConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                    if (oldPowerReq > powerReq) {
                        src = this;
                        powerReq = oldPowerReq;
                    }
                }

                if (powerReq > powerThreshold) {
                    src.extractAEPower(aeConsumption, Actionable.MODULATE, PowerMultiplier.CONFIG);
                    this.addProgress(speedFactor);
                    this.showWarning = false;
                } else if (powerReq != 0) {
                    var progressRatio = src == this
                            ? powerReq / aeConsumption
                            : (powerReq - 10 * eg.getIdlePowerUsage()) / aeConsumption;
                    var factor = Mth.floor(progressRatio * speedFactor);

                    if (factor > 1) {
                        var extracted = src.extractAEPower(
                                (double) (aeConsumption * factor) / speedFactor,
                                Actionable.MODULATE,
                                PowerMultiplier.CONFIG);
                        var actualFactor = (int) Math.floor(extracted / aeConsumption * speedFactor);
                        this.addProgress(actualFactor);
                    }

                    this.showWarning = true;
                }
            });

            if (this.getProgress() >= MAX_PROGRESS) {
                this.setProgress(0);
                final GenesisSynthesizerRecipe out = this.getTask();
                if (out != null) {
                    final ItemStack output = out.getResultItem();
                    final FluidStack fluidOut = out.getResultFluid();

                    if ((out.isItemOutput()
                            && this.outputInv
                            .insertItem(0, output, false)
                            .isEmpty())
                            || (!out.isItemOutput()
                            && this.tankInv.add(1, AEFluidKey.of(fluidOut), fluidOut.getAmount())
                            >= fluidOut.getAmount() - 0.01)) {
                        this.setProgress(0);

                        GenericStack fluid = this.tankInv.getStack(0);
                        FluidStack fluidStack = null;
                        if (fluid != null) {
                            AEKey aeKey = fluid.what();
                            if (aeKey instanceof AEFluidKey key) {
                                fluidStack = key.toStack((int) fluid.amount());
                            }
                        }

                        consume:
                        for (var input : out.getValidInputs()) {
                            for (int x = 0; x < this.combinedInputInv.size(); x++) {
                                var stack = this.combinedInputInv.getStackInSlot(x);
                                if (input.checkType(stack)) {
                                    input.consume(stack);
                                    this.combinedInputInv.setItemDirect(x, stack);
                                }

                                if (input.isEmpty()) {
                                    continue consume;
                                }
                            }
                            GenericStack crystalStack = this.crystalTank.getStack(0);
                            ItemStack crystalItemStack = crystalStack != null ? ((AEItemKey) crystalStack.what()).toStack((int) crystalStack.amount()) : ItemStack.EMPTY;
                            if (input.checkType(crystalItemStack)) {
                                input.consume(crystalItemStack);
                                this.crystalTank.setStack(0, GenericStack.fromItemStack(crystalItemStack));
                            }

                            if (fluidStack != null && !input.isEmpty() && input.checkType(fluidStack)) {
                                input.consume(fluidStack);
                            }
                        }

                        if (fluidStack != null) {
                            if (fluidStack.isEmpty()) {
                                this.tankInv.setStack(0, null);
                            } else {
                                this.tankInv.setStack(
                                        0,
                                        new GenericStack(
                                                Objects.requireNonNull(AEFluidKey.of(fluidStack)),
                                                fluidStack.getAmount()));
                            }
                        }
                    }
                }
                this.saveChanges();
                this.cachedTask = null;
                this.setWorking(false);
            }
        } else {
            this.setWorking(false);
            this.showWarning = false;
        }

        if (this.pushOutResult()) {
            return TickRateModulation.URGENT;
        }

        return this.hasCraftWork()
                ? TickRateModulation.URGENT
                : this.hasAutoExportWork() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
    }

    private boolean hasAutoExportWork() {
        return (!this.outputInv.getStackInSlot(0).isEmpty()
                || this.tankInv.getStack(1) != null
                || this.tankInv.getAmount(1) > 0)
                && configManager.getSetting(Settings.AUTO_EXPORT) == YesNo.YES
                && !this.outputSides.isEmpty();
    }

    private boolean hasCraftWork() {
        var task = this.getTask();
        if (task != null) {
            // Only process if the result would fit.
            if (task.isItemOutput()) {
                return this.outputInv.insertItem(0, task.getResultItem(), true).isEmpty();
            } else {
                var fluid = task.getResultFluid();
                return this.tankInv.canAdd(1, AEFluidKey.of(fluid), fluid.getAmount());
            }
        }

        this.setProgress(0);
        return this.isWorking();
    }

    private boolean pushOutResult() {
        if (!this.hasAutoExportWork() || this.level == null) {
            return false;
        }

        for (Direction dir : outputSides) {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            IItemHandler itemStorage = this.level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.getOpposite());
            IFluidHandler fluidStorage = this.level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.getOpposite());

            boolean movedStacks = false;
            if (itemStorage != null) {
                if (this.outputInv.getStackInSlot(0) != null && !this.outputInv.getStackInSlot(0).isEmpty()) {
                    ItemStack remainingStack = this.outputInv.extractItem(0, 64, false);
                    for (int i = 0; i < itemStorage.getSlots(); i++) {
                        if (remainingStack.getCount() <= 0) break;
                        remainingStack = itemStorage.insertItem(i, remainingStack, false);
                    }
                    this.outputInv.insertItem(0, remainingStack, false);
                    movedStacks |= !remainingStack.isEmpty();
                }
            }

            if (fluidStorage != null) {
                var outFluid = this.tankInv.getStack(1);
                var fluidKey = outFluid != null ? outFluid.what() : null;
                if (outFluid != null && fluidKey != null) {
                    var extracted = this.tankInv.extract(1, fluidKey, outFluid.amount(), Actionable.MODULATE);
                    var inserted = fluidStorage.fill(((AEFluidKey) fluidKey).toStack((int) outFluid.amount()), IFluidHandler.FluidAction.EXECUTE);
                    this.tankInv.add(1, ((AEFluidKey) fluidKey), (int) (extracted - inserted));

                    if (this.tankInv.getAmount(1) == 0) {
                        this.tankInv.clear(1);
                    }

                    movedStacks |= inserted > 0;
                }
            }

            if (movedStacks) {
                return true;
            }

        }
        return false;
    }

    @Nullable
    public GenesisSynthesizerRecipe getTask() {
        if (this.cachedTask == null && level != null) {

            this.cachedTask = findRecipe(level);
        }
        return this.cachedTask;
    }

    private GenesisSynthesizerRecipe findRecipe(Level level) {
        List<ItemStack> inputs = new ArrayList<>();
        for (var x = 0; x < this.inputInv.size(); x++) {
            inputs.add(this.inputInv.getStackInSlot(x));
        }
        GenericStack crystalStack = this.crystalTank.getStack(0);
        ItemStack crystalItemStack = crystalStack != null ? ((AEItemKey) crystalStack.what()).toStack((int) crystalStack.amount()) : ItemStack.EMPTY;
        inputs.add(crystalItemStack);

        return GenesisSynthesizerRecipes.findRecipe(level, inputs, this.tankInv.getStack(0));
    }

    private void updateBlockState(boolean working) {
        if (this.level != null && !this.notLoaded() && !this.isRemoved()) {
            BlockState current = this.level.getBlockState(this.worldPosition);
            if (current.getBlock() instanceof GenesisSynthesizerBlock) {
                BlockState newState = current.setValue(GenesisSynthesizerBlock.WORKING, working);
                if (current != newState) {
                    this.level.setBlock(this.worldPosition, newState, 2);
                }
            }
        }
    }

    private void chargeCrystalTank() {
        if (!this.crystalInv.getStackInSlot(0).isEmpty()) {
            ItemStack crystalInvStack = this.crystalInv.getStackInSlot(0);
            if (crystalInvStack != null && crystalInvStack.is(AGSingletons.EMBER_CRYSTAL_CHARGED)) {
                if (this.getCrystalCount() < MAX_CRYSTAL_TANK) {
                    ItemStack extracted = this.crystalInv.extractItem(0, MAX_CRYSTAL_TANK - this.getCrystalCount(), false);
                    GenericStack crystalTankStack = this.crystalTank.getStack(0);
                    ItemStack crystalTankItemStack = crystalTankStack != null ? ((AEItemKey) crystalTankStack.what()).toStack((int) crystalTankStack.amount()) : ItemStack.EMPTY;
                    extracted.setCount(extracted.getCount() + crystalTankItemStack.getCount());
                    this.crystalTank.setStack(0, GenericStack.fromItemStack(extracted));
                }
            }
        }
    }

    public int getCrystalCount() {
        return Math.toIntExact(this.crystalTank.getAmount(0));
    }

    public boolean showWarning() {
        return this.showWarning;
    }

    private static class RestrictItemFilter implements IAEItemFilter {

        private final Item RESTRICTED_ITEM;

        public RestrictItemFilter(Item item) {
            this.RESTRICTED_ITEM = item;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.is(this.RESTRICTED_ITEM);
        }
    }

    private static class AllowItemFilter implements IAEItemFilter {

        private final Item ALLOWED_ITEM;

        public AllowItemFilter(Item item) {
            this.ALLOWED_ITEM = item;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.is(this.ALLOWED_ITEM);
        }
    }

    private static class GenericItemTank extends GenericStackInv {

        private final Item RESTRICTED_ITEM;

        public GenericItemTank(@Nullable Runnable listener, Item item) {
            super(Set.of(AEKeyType.items()), listener, Mode.STORAGE, 1);
            this.setCapacity(AEKeyType.items(), GenesisSynthesizerBlockEntity.MAX_CRYSTAL_TANK);
            this.RESTRICTED_ITEM = item;
        }

        @Override
        public long getMaxAmount(AEKey key) {
            return getCapacity(key.getType());
        }

        @Override
        public boolean isAllowedIn(int slot, AEKey what) {
            return what instanceof AEItemKey itemKey && itemKey.toStack().is(this.RESTRICTED_ITEM);
        }
    }

    private static class CustomTankInv extends GenericStackInv {
        public CustomTankInv(@Nullable Runnable listener, Mode mode, int size) {
            super(Set.of(AEKeyType.fluids()), listener, mode, size);
            this.setCapacity(AEKeyType.fluids(), 16000);
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

        public boolean canAdd(int slot, AEFluidKey key, int amount) {
            var stack = this.getStack(slot);
            if (stack == null) return true;
            if (!stack.what().equals(key)) return false;
            return stack.amount() + amount <= this.getMaxAmount(key);
        }

        public int add(int slot, AEFluidKey key, int amount) {
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
