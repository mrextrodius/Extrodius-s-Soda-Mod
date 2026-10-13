package com.extrodius.sodamod.blockentity;

import com.extrodius.sodamod.ModBlockEntities;
import com.extrodius.sodamod.ModBlocks;
import com.extrodius.sodamod.menu.CarbonatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the Carbonator's inventory (1 canister slot, 1 soda slot).
 *
 * The transformation is instant and manual: pressing the valve wheel button
 * with a full gas canister and a flat Cocoa Cola turns them into an empty gas
 * canister and a carbonated Cocoa Cola in place.
 *
 * Both slots are limited to a single item so the 1:1 transformation stays clean.
 *
 * Implements vanilla {@link Container} directly (NeoForge 26.x has no
 * ItemStackHandler anymore), so breaking the block drops its contents
 * automatically and hoppers/etc. can interact with it.
 */
public class CarbonatorBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int CANISTER_SLOT = 0;
    public static final int SODA_SLOT = 1;
    public static final int TOTAL_SLOTS = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public CarbonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARBONATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Carbonation logic (server side; called from the menu's clickMenuButton)
    // ------------------------------------------------------------------

    /** The full gas canister consumed by carbonation. */
    public static ItemStack makeFilledCanister() {
        return new ItemStack(ModBlocks.CO2_CANISTER_FULL.get());
    }

    /** True for any flat soda the carbonator accepts. */
    public static boolean isFlatSoda(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModBlocks.COCOA_COLA_FLAT.get())
                        || stack.is(ModBlocks.DR_BLAZE_FLAT.get())
                        || stack.is(ModBlocks.PHANTA_SLIME_FLAT.get())
                        || stack.is(ModBlocks.MOUNTAIN_CREEPER_FLAT.get())
                        || stack.is(ModBlocks.PHANTA_ORANGE_FLAT.get())
                        || stack.is(ModBlocks.SPRUCE_BEER_FLAT.get())
                        || stack.is(ModBlocks.PHANTA_CREAM_FLAT.get())
                        || stack.is(ModBlocks.PHANTA_GRAPE_FLAT.get()));
    }

    /** The carbonated form of a flat soda. */
    public static ItemStack makeCarbonatedSoda(ItemStack flat) {
        if (flat.is(ModBlocks.COCOA_COLA_FLAT.get())) {
            return new ItemStack(ModBlocks.COCOA_COLA.get());
        }
        if (flat.is(ModBlocks.DR_BLAZE_FLAT.get())) {
            return new ItemStack(ModBlocks.DR_BLAZE.get());
        }
        if (flat.is(ModBlocks.PHANTA_SLIME_FLAT.get())) {
            return new ItemStack(ModBlocks.PHANTA_SLIME.get());
        }
        if (flat.is(ModBlocks.MOUNTAIN_CREEPER_FLAT.get())) {
            return new ItemStack(ModBlocks.MOUNTAIN_CREEPER.get());
        }
        if (flat.is(ModBlocks.PHANTA_ORANGE_FLAT.get())) {
            return new ItemStack(ModBlocks.PHANTA_ORANGE.get());
        }
        if (flat.is(ModBlocks.SPRUCE_BEER_FLAT.get())) {
            return new ItemStack(ModBlocks.SPRUCE_BEER.get());
        }
        if (flat.is(ModBlocks.PHANTA_CREAM_FLAT.get())) {
            return new ItemStack(ModBlocks.PHANTA_CREAM.get());
        }
        if (flat.is(ModBlocks.PHANTA_GRAPE_FLAT.get())) {
            return new ItemStack(ModBlocks.PHANTA_GRAPE.get());
        }
        return ItemStack.EMPTY;
    }

    public static boolean isFilledCanister(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModBlocks.CO2_CANISTER_FULL.get());
    }

    /** Full canister + flat soda, ready to carbonate. */
    public boolean hasValidRecipe() {
        return isFilledCanister(items.get(CANISTER_SLOT)) && isFlatSoda(items.get(SODA_SLOT));
    }

    public void onValveButtonPressed() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!hasValidRecipe()) {
            return;
        }
        items.set(CANISTER_SLOT, new ItemStack(ModBlocks.CO2_CANISTER_EMPTY.get()));
        items.set(SODA_SLOT, makeCarbonatedSoda(items.get(SODA_SLOT)));
        setChanged();
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
    // MenuProvider
    // ------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.extrodius_soda.carbonator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CarbonatorMenu(containerId, playerInventory, this);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
    }
}
