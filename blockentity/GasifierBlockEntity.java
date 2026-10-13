package com.extrodius.sodamod.blockentity;

import com.extrodius.sodamod.ModBlockEntities;
import com.extrodius.sodamod.ModBlocks;
import com.extrodius.sodamod.block.GasifierBlock;
import com.extrodius.sodamod.menu.GasifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Holds the Gasifier's inventory and furnace-like burn/cook state.
 *
 * Slots: 0 = wheat, 1 = fuel, 2 = empty canister (glass bottle placeholder),
 * 3 = output (water bottle placeholder for the filled CO2 canister).
 *
 * One operation burns 1 wheat + 1 empty canister (plus furnace fuel) into
 * 1 filled canister, taking COOK_TIME_TOTAL ticks like a furnace smelt.
 *
 * Implements vanilla {@link Container} directly (NeoForge 26.x has no
 * ItemStackHandler anymore), so breaking the block drops its contents
 * automatically and hoppers/etc. can interact with it.
 */
public class GasifierBlockEntity extends BlockEntity implements Container, MenuProvider, ContainerData {
    public static final int WHEAT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int CANISTER_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int TOTAL_SLOTS = 4;
    public static final int COOK_TIME_TOTAL = 200; // furnace standard

    // ContainerData indices: 0 = burn time left, 1 = burn duration, 2 = cook progress, 3 = cook total.
    public static final int DATA_BURN_TIME = 0;
    public static final int DATA_BURN_DURATION = 1;
    public static final int DATA_COOK_TIME = 2;
    public static final int DATA_COOK_TOTAL = 3;
    public static final int DATA_COUNT = 4;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration;
    private int cookTime;

    public GasifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GASIFIER.get(), pos, state);
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // ------------------------------------------------------------------
    // Server tick (furnace-like)
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, GasifierBlockEntity be) {
        boolean wasBurning = be.isBurning();
        boolean changed = false;

        if (be.isBurning()) {
            be.burnTime--;
        }

        ItemStack fuelStack = be.items.get(FUEL_SLOT);
        boolean hasFuel = !fuelStack.isEmpty();
        boolean hasIngredients = !be.items.get(WHEAT_SLOT).isEmpty() && !be.items.get(CANISTER_SLOT).isEmpty();
        if (be.isBurning() || hasFuel && hasIngredients) {
            if (be.canSmelt()) {
                if (!be.isBurning()) {
                    be.burnTime = be.getBurnDuration((ServerLevel) level, fuelStack);
                    be.burnDuration = be.burnTime;
                    if (be.isBurning()) {
                        changed = true;
                        consumeFuel((ServerLevel) level, pos, be.items, fuelStack);
                    }
                }
                if (be.isBurning()) {
                    be.cookTime++;
                    if (be.cookTime >= COOK_TIME_TOTAL) {
                        be.cookTime = 0;
                        be.finishSmelt();
                        changed = true;
                    }
                } else {
                    be.cookTime = 0;
                }
            } else {
                be.cookTime = 0;
            }
        } else if (be.cookTime > 0) {
            be.cookTime = Math.max(be.cookTime - 2, 0);
        }

        if (wasBurning != be.isBurning()) {
            changed = true;
            level.setBlock(pos, state.setValue(GasifierBlock.LIT, be.isBurning()), 3);
        }
        if (changed) {
            be.setChanged();
        }
    }

    /** Wheat + empty canister present, and the output can accept the filled canister. */
    public boolean canSmelt() {
        ItemStack wheat = items.get(WHEAT_SLOT);
        if (wheat.isEmpty() || !wheat.is(Items.WHEAT)) {
            return false;
        }
        ItemStack canister = items.get(CANISTER_SLOT);
        if (canister.isEmpty() || !canister.is(ModBlocks.CO2_CANISTER_EMPTY.get())) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, makeResult()) && output.getCount() < output.getMaxStackSize();
    }

    private void finishSmelt() {
        items.get(WHEAT_SLOT).shrink(1);
        items.get(CANISTER_SLOT).shrink(1);
        ItemStack result = makeResult();
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result);
        } else {
            output.grow(1);
        }
    }

    /** The filled gas canister produced by the gasifier. */
    public static ItemStack makeResult() {
        return new ItemStack(ModBlocks.CO2_CANISTER_FULL.get());
    }

    /** Vanilla-style fuel consumption, keeping crafting remainders (e.g. lava bucket -> bucket). */
    private static void consumeFuel(ServerLevel level, BlockPos pos, NonNullList<ItemStack> items, ItemStack fuel) {
        ItemStack newFuel = fuel;
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (remainder != null) {
            if (fuel.isEmpty()) {
                newFuel = remainder.create();
            } else {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
            }
        }
        items.set(FUEL_SLOT, newFuel);
    }

    /** Burn duration from the item's COOKING_FUEL data component (vanilla fuel values). */
    private int getBurnDuration(ServerLevel level, ItemStack fuelStack) {
        CookingFuel fuel = fuelStack.get(DataComponents.COOKING_FUEL);
        if (fuel == null) {
            return 0;
        }
        return fuel.burnTime().get(buildLootContext(level), 0);
    }

    private LootContext buildLootContext(ServerLevel level) {
        return new LootContext.Builder(
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
                        .withParameter(LootContextParams.BLOCK_ENTITY, this)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()))
                        .withParameter(LootContextParams.CONTAINER, this)
                        .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
    }

    // ------------------------------------------------------------------
    // Container
    // ------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ------------------------------------------------------------------
    // ContainerData (syncs burn/cook state to the client menu)
    // ------------------------------------------------------------------

    @Override
    public int get(int index) {
        return switch (index) {
            case DATA_BURN_TIME -> burnTime;
            case DATA_BURN_DURATION -> burnDuration;
            case DATA_COOK_TIME -> cookTime;
            case DATA_COOK_TOTAL -> COOK_TIME_TOTAL;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case DATA_BURN_TIME -> burnTime = value;
            case DATA_BURN_DURATION -> burnDuration = value;
            case DATA_COOK_TIME -> cookTime = value;
            default -> {
            }
        }
    }

    @Override
    public int getCount() {
        return DATA_COUNT;
    }

    // ------------------------------------------------------------------
    // MenuProvider
    // ------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.extrodius_soda.gasifier");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasifierMenu(containerId, playerInventory, this);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnDuration", burnDuration);
        output.putInt("CookTime", cookTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        burnTime = input.getIntOr("BurnTime", 0);
        burnDuration = input.getIntOr("BurnDuration", 0);
        cookTime = input.getIntOr("CookTime", 0);
    }
}
