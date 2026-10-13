package com.extrodius.sodamod.menu;

import com.extrodius.sodamod.ModBlocks;
import com.extrodius.sodamod.ModMenuTypes;
import com.extrodius.sodamod.blockentity.GasifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the Gasifier.
 *
 * Slot 0: wheat (burned as the CO2 source).
 * Slot 1: furnace fuel.
 * Slot 2: empty canister (glass bottle placeholder).
 * Slot 3: filled canister output (water bottle placeholder; output only).
 * Slots 4+: player inventory.
 *
 * The client constructor reads the BlockPos the server wrote into the open
 * packet and looks the block entity back up from the client level.
 */
public class GasifierMenu extends AbstractContainerMenu {
    public static final int WHEAT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int CANISTER_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int MACHINE_SLOT_COUNT = 4;

    private final Container blockInventory;
    private final ContainerData data;
    private final @Nullable GasifierBlockEntity blockEntity;

    /** Server-side constructor: the block entity is handed over directly. */
    public GasifierMenu(int containerId, Inventory playerInventory, GasifierBlockEntity blockEntity) {
        super(ModMenuTypes.GASIFIER.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockInventory = blockEntity;
        this.data = blockEntity;
        buildSlots(playerInventory);
        addDataSlots(this.data);
    }

    /** Client-side constructor: reads the BlockPos from the open packet. */
    public GasifierMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenuTypes.GASIFIER.get(), containerId);
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof GasifierBlockEntity gasifier) {
            this.blockEntity = gasifier;
            this.blockInventory = gasifier;
            this.data = gasifier;
        } else {
            // Fallback so the screen never NPEs if the BE is momentarily missing.
            this.blockEntity = null;
            this.blockInventory = new SimpleContainer(MACHINE_SLOT_COUNT);
            this.data = new SimpleContainerData(GasifierBlockEntity.DATA_COUNT);
        }
        buildSlots(playerInventory);
        addDataSlots(this.data);
    }

    private void buildSlots(Inventory playerInventory) {
        // Machine slots, positioned to match Zach's 176x166 artwork.
        // Slot(x, y) is the item's top-left; items render 16x16 at that point.
        this.addSlot(new WheatSlot(blockInventory, WHEAT_SLOT, 56, 17));
        this.addSlot(new FuelSlot(blockInventory, FUEL_SLOT, 56, 53));
        this.addSlot(new CanisterSlot(blockInventory, CANISTER_SLOT, 116, 53));
        // Output well art is 24x24; center the 16x16 item in it.
        this.addSlot(new OutputSlot(blockInventory, OUTPUT_SLOT, 116, 21));

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

    /** 0..1, how far the current fill operation has cooked (drives the arrow). */
    public float getCookProgress() {
        int progress = data.get(GasifierBlockEntity.DATA_COOK_TIME);
        int total = data.get(GasifierBlockEntity.DATA_COOK_TOTAL);
        return total != 0 && progress != 0 ? Mth.clamp((float) progress / total, 0.0F, 1.0F) : 0.0F;
    }

    /** 0..1, how much burn time the current fuel has left (drives the flame). */
    public float getBurnProgress() {
        int burnTime = data.get(GasifierBlockEntity.DATA_BURN_TIME);
        int burnDuration = data.get(GasifierBlockEntity.DATA_BURN_DURATION);
        if (burnDuration == 0) {
            burnDuration = GasifierBlockEntity.COOK_TIME_TOTAL;
        }
        return Mth.clamp((float) burnTime / burnDuration, 0.0F, 1.0F);
    }

    public boolean isBurning() {
        return data.get(GasifierBlockEntity.DATA_BURN_TIME) > 0;
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
            } else if (stack.is(Items.WHEAT)) {
                if (!this.moveItemStackTo(stack, WHEAT_SLOT, WHEAT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFuel(stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModBlocks.CO2_CANISTER_EMPTY.get())) {
                if (!this.moveItemStackTo(stack, CANISTER_SLOT, CANISTER_SLOT + 1, false)) {
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

    private static boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockInventory.stillValid(player);
    }

    // ------------------------------------------------------------------
    // Slot types
    // ------------------------------------------------------------------

    /** Wheat slot: the CO2 source the machine burns. */
    private static class WheatSlot extends Slot {
        WheatSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.WHEAT);
        }
    }

    /** Fuel slot: any vanilla furnace fuel. */
    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack);
        }
    }

    /** Empty-canister slot: only empty gas canisters. */
    private static class CanisterSlot extends Slot {
        CanisterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModBlocks.CO2_CANISTER_EMPTY.get());
        }
    }

    /** Output slot: nothing may be inserted, only taken. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
