package com.extrodius.sodamod.menu;

import com.extrodius.sodamod.ModBlocks;
import com.extrodius.sodamod.ModMenuTypes;
import com.extrodius.sodamod.blockentity.SodaMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Prediction;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Container menu for the Soda Mixer, in both its moods.
 *
 * Normal mode (slots 0-4):
 *   Slots 0-2: ingredients (dye + sugar + organic, any order).
 *   Slot 3: empty soda can.
 *   Slot 4: finished flat soda (output only).
 *
 * Graveyard mode (slots 5-14), the secret triple-click easter egg:
 *   Slots 5-12: the 8 different carbonated sodas, any order.
 *   Slot 13: empty soda can.
 *   Slot 14: the finished Graveyard (output only).
 *
 * Only one set is ever active: inactive slots are hidden from rendering,
 * hovering, clicking, and shift-click (via mayPlace). The mode is synced
 * server->client through ContainerData and resets to normal when the UI
 * closes, keeping the secret a secret.
 */
public class SodaMixerMenu extends AbstractContainerMenu {
    public static final int INGREDIENT_SLOT_0 = 0;
    public static final int INGREDIENT_SLOT_1 = 1;
    public static final int INGREDIENT_SLOT_2 = 2;
    public static final int CAN_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int GRAVEYARD_WELL_0 = 5;
    public static final int GRAVEYARD_CAN_SLOT = 13;
    public static final int GRAVEYARD_OUTPUT_SLOT = 14;
    public static final int MACHINE_SLOT_COUNT = 15;
    public static final int MIXES_REQUIRED = SodaMixerBlockEntity.MIXES_REQUIRED;

    /** Menu button ids (see clickMenuButton). */
    private static final int BUTTON_MIX = 0;
    private static final int BUTTON_TOGGLE_MODE = 1;

    private final Container blockInventory;
    private final ContainerData data;
    private final @Nullable SodaMixerBlockEntity blockEntity;

    /** Server-side constructor: the block entity is handed over directly. */
    public SodaMixerMenu(int containerId, Inventory playerInventory, SodaMixerBlockEntity blockEntity) {
        super(ModMenuTypes.SODA_MIXER.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockInventory = blockEntity;
        this.data = blockEntity;
        buildSlots(playerInventory);
        addDataSlots(this.data);
    }

    /** Client-side constructor: reads the BlockPos from the open packet. */
    public SodaMixerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenuTypes.SODA_MIXER.get(), containerId);
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof SodaMixerBlockEntity mixer) {
            this.blockEntity = mixer;
            this.blockInventory = mixer;
            this.data = mixer;
        } else {
            // Fallback so the screen never NPEs if the BE is momentarily missing.
            this.blockEntity = null;
            this.blockInventory = new SimpleContainer(MACHINE_SLOT_COUNT);
            this.data = new SimpleContainerData(2);
        }
        buildSlots(playerInventory);
        addDataSlots(this.data);
    }

    private void buildSlots(Inventory playerInventory) {
        // Normal-mode slots, positioned to match Zach's 176x166 artwork.
        // Slot(x, y) is the item's top-left; items render 16x16 at that point.
        this.addSlot(new ModeSlot(blockInventory, INGREDIENT_SLOT_0, 30, 21, false) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && SodaMixerBlockEntity.isIngredient(stack);
            }
        });
        this.addSlot(new ModeSlot(blockInventory, INGREDIENT_SLOT_1, 48, 21, false) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && SodaMixerBlockEntity.isIngredient(stack);
            }
        });
        this.addSlot(new ModeSlot(blockInventory, INGREDIENT_SLOT_2, 66, 21, false) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && SodaMixerBlockEntity.isIngredient(stack);
            }
        });
        this.addSlot(new ModeSlot(blockInventory, CAN_SLOT, 48, 48, false) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && stack.is(ModBlocks.SODA_CAN_EMPTY.get());
            }
        });
        // Output slot art is a larger 24x24 well; center the 16x16 item in it.
        this.addSlot(new ModeSlot(blockInventory, OUTPUT_SLOT, 124, 21, false) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Graveyard-mode slots, positioned to match Zach's Graveyard Mixer art.
        // (In-game verified: wells sit 1px lower than first measured.)
        int[] wellX = {12, 30, 48, 66};
        int[] wellY = {21, 39};
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = GRAVEYARD_WELL_0 + row * 4 + col;
                this.addSlot(new ModeSlot(blockInventory, slotIndex, wellX[col], wellY[row], true) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return super.mayPlace(stack) && SodaMixerBlockEntity.isCarbonatedSoda(stack);
                    }
                });
            }
        }
        this.addSlot(new ModeSlot(blockInventory, GRAVEYARD_CAN_SLOT, 90, 21, true) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && stack.is(ModBlocks.SODA_CAN_EMPTY.get());
            }
        });
        // Graveyard output well is larger; center the 16x16 item in it.
        this.addSlot(new ModeSlot(blockInventory, GRAVEYARD_OUTPUT_SLOT, 148, 21, true) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

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

    /** A machine slot that only exists in one of the two modes. */
    private abstract class ModeSlot extends Slot {
        private final boolean graveyardOnly;

        ModeSlot(Container container, int slot, int x, int y, boolean graveyardOnly) {
            super(container, slot, x, y);
            this.graveyardOnly = graveyardOnly;
        }

        @Override
        public boolean isActive() {
            return getGraveyardMode() == graveyardOnly;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Shift-click (moveItemStackTo) doesn't check isActive, so gate it here.
            return isActive();
        }
    }

    /** Mix progress (0-10), synced from the server via ContainerData. */
    public int getProgress() {
        return data.get(0);
    }

    /** Current mode, synced from the server via ContainerData. */
    public boolean getGraveyardMode() {
        return data.get(1) != 0;
    }

    /** Client-side recipe check for the mix button UX (the server re-validates). */
    public boolean hasValidRecipe() {
        if (getGraveyardMode()) {
            return blockEntity != null && blockEntity.hasValidGraveyardRecipe();
        }
        Optional<SodaMixerBlockEntity.MixerRecipe> recipe =
                SodaMixerBlockEntity.findRecipe(this.blockInventory);
        if (recipe.isEmpty()) {
            return false;
        }
        if (!this.getSlot(CAN_SLOT).getItem().is(ModBlocks.SODA_CAN_EMPTY.get())) {
            return false;
        }
        ItemStack output = this.getSlot(OUTPUT_SLOT).getItem();
        if (output.isEmpty()) {
            return true;
        }
        ItemStack result = recipe.get().result().get();
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() < output.getMaxStackSize();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide()) {
            return false;
        }
        if (buttonId == BUTTON_MIX && blockEntity != null) {
            blockEntity.onMixButtonPressed(player);
            return true;
        }
        if (buttonId == BUTTON_TOGGLE_MODE && blockEntity != null) {
            toggleGraveyardMode(player);
            return true;
        }
        return false;
    }

    /**
     * Flips the secret mode. Machine contents are rehoused first: ingredients
     * and any unclaimed output go back to the player (or drop if full), while
     * the empty can carries over to the other mode's can slot when possible.
     */
    private void toggleGraveyardMode(Player player) {
        boolean toGraveyard = !blockEntity.isGraveyardMode();
        if (toGraveyard) {
            returnToPlayer(player, INGREDIENT_SLOT_0);
            returnToPlayer(player, INGREDIENT_SLOT_1);
            returnToPlayer(player, INGREDIENT_SLOT_2);
            returnToPlayer(player, OUTPUT_SLOT);
            carryCanTo(player, CAN_SLOT, GRAVEYARD_CAN_SLOT);
        } else {
            for (int i = 0; i < 8; i++) {
                returnToPlayer(player, GRAVEYARD_WELL_0 + i);
            }
            returnToPlayer(player, GRAVEYARD_OUTPUT_SLOT);
            carryCanTo(player, GRAVEYARD_CAN_SLOT, CAN_SLOT);
        }
        blockEntity.setGraveyardMode(toGraveyard);
    }

    private void returnToPlayer(Player player, int slotIndex) {
        ItemStack stack = blockInventory.getItem(slotIndex);
        if (stack.isEmpty()) {
            return;
        }
        blockInventory.setItem(slotIndex, ItemStack.EMPTY);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    private void carryCanTo(Player player, int fromSlot, int toSlot) {
        ItemStack stack = blockInventory.getItem(fromSlot);
        if (stack.isEmpty()) {
            return;
        }
        blockInventory.setItem(fromSlot, ItemStack.EMPTY);
        if (blockInventory.getItem(toSlot).isEmpty()) {
            blockInventory.setItem(toSlot, stack);
        } else {
            // Shouldn't happen (modes are exclusive), but don't void the can.
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false, Prediction.SERVER_ONLY);
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (blockEntity != null && !player.level().isClientSide()) {
            if (blockEntity.isGraveyardMode()) {
                // The secret doesn't keep your sodas: everything in the
                // graveyard slots pops back into your inventory (or drops).
                for (int i = 0; i < 8; i++) {
                    returnToPlayer(player, GRAVEYARD_WELL_0 + i);
                }
                returnToPlayer(player, GRAVEYARD_CAN_SLOT);
                returnToPlayer(player, GRAVEYARD_OUTPUT_SLOT);
            }
            // The secret doesn't survive closing the UI.
            blockEntity.setGraveyardMode(false);
        }
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
            } else {
                // Player inventory -> machine (mayPlace gates on the active mode's slots).
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
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
}
