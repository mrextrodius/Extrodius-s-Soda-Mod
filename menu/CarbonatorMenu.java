package com.extrodius.sodamod.menu;

import com.extrodius.sodamod.ModMenuTypes;
import com.extrodius.sodamod.blockentity.CarbonatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the Carbonator.
 *
 * Slot 0: filled CO2 canister (water bottle placeholder; becomes the empty
 * glass bottle when the valve is pressed).
 * Slot 1: flat soda (glass bottle placeholder; becomes the carbonated potion
 * of healing when the valve is pressed).
 * Slots 2+: player inventory.
 *
 * Both machine slots hold a single item so the 1:1 transformation stays clean.
 *
 * The client constructor reads the BlockPos the server wrote into the open
 * packet and looks the block entity back up from the client level.
 */
public class CarbonatorMenu extends AbstractContainerMenu {
    public static final int CANISTER_SLOT = 0;
    public static final int SODA_SLOT = 1;
    public static final int MACHINE_SLOT_COUNT = 2;

    private final Container blockInventory;
    private final @Nullable CarbonatorBlockEntity blockEntity;

    /** Server-side constructor: the block entity is handed over directly. */
    public CarbonatorMenu(int containerId, Inventory playerInventory, CarbonatorBlockEntity blockEntity) {
        super(ModMenuTypes.CARBONATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockInventory = blockEntity;
        buildSlots(playerInventory);
    }

    /** Client-side constructor: reads the BlockPos from the open packet. */
    public CarbonatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenuTypes.CARBONATOR.get(), containerId);
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof CarbonatorBlockEntity carbonator) {
            this.blockEntity = carbonator;
            this.blockInventory = carbonator;
        } else {
            // Fallback so the screen never NPEs if the BE is momentarily missing.
            this.blockEntity = null;
            this.blockInventory = new SimpleContainer(MACHINE_SLOT_COUNT);
        }
        buildSlots(playerInventory);
    }

    private void buildSlots(Inventory playerInventory) {
        // Machine slots, positioned to match Zach's 176x166 artwork.
        // Slot(x, y) is the item's top-left; items render 16x16 at that point.
        this.addSlot(new CanisterSlot(blockInventory, CANISTER_SLOT, 44, 34));
        // Soda well art is 24x24; center the 16x16 item in it.
        this.addSlot(new SodaSlot(blockInventory, SODA_SLOT, 124, 34));

        // Player inventory (standard 176x166 layout).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** Client-side recipe check for the valve button UX (the server re-validates). */
    public boolean hasValidRecipe() {
        return CarbonatorBlockEntity.isFilledCanister(this.getSlot(CANISTER_SLOT).getItem())
                && CarbonatorBlockEntity.isFlatSoda(this.getSlot(SODA_SLOT).getItem());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 0 && blockEntity != null && !player.level().isClientSide()) {
            blockEntity.onValveButtonPressed();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (slotIndex < MACHINE_SLOT_COUNT) {
                // Machine -> player inventory.
                if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (CarbonatorBlockEntity.isFilledCanister(stack)) {
                if (!this.moveItemStackTo(stack, CANISTER_SLOT, CANISTER_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (CarbonatorBlockEntity.isFlatSoda(stack)) {
                if (!this.moveItemStackTo(stack, SODA_SLOT, SODA_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < MACHINE_SLOT_COUNT + 27) {
                // Main inventory -> hotbar.
                if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT + 27, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, MACHINE_SLOT_COUNT + 27, false)) {
                // Hotbar -> main inventory.
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockInventory.stillValid(player);
    }

    // ------------------------------------------------------------------
    // Slot types
    // ------------------------------------------------------------------

    /** Filled-canister slot: full gas canister only, single item. */
    private static class CanisterSlot extends Slot {
        CanisterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return CarbonatorBlockEntity.isFilledCanister(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Soda slot: any flat soda, single item. */
    private static class SodaSlot extends Slot {
        SodaSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return CarbonatorBlockEntity.isFlatSoda(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
