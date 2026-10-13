package com.extrodius.sodamod.blockentity;

import com.extrodius.sodamod.ModBlockEntities;
import com.extrodius.sodamod.ModBlocks;
import com.extrodius.sodamod.menu.SodaMixerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Prediction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Holds the Soda Mixer's inventory and the 10-click mix progress. The machine
 * is fully manual: every press of the mix button is one click of progress.
 *
 * Normal mode: 3 ingredient slots (dye + sugar + organic, any order),
 * 1 empty-can slot, 1 output slot -> flat soda.
 *
 * Graveyard mode (secret, toggled by triple-clicking "Mixer" in the UI):
 * 8 wells holding the 8 different carbonated sodas (any order), 1 empty-can
 * slot -> the Graveyard. Everything is consumed; nothing comes back.
 * Graveyard mode is deliberately NOT persisted: closing the UI (or unloading
 * the chunk) reverts to normal mode, keeping it a secret.
 *
 * Implements vanilla {@link Container} directly (NeoForge 26.x has no
 * ItemStackHandler anymore), so breaking the block drops its contents
 * automatically and hoppers/etc. can interact with it.
 */
public class SodaMixerBlockEntity extends BlockEntity implements Container, MenuProvider, ContainerData {
    public static final int INGREDIENT_SLOT_COUNT = 3;
    public static final int CAN_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    /** First of the 8 graveyard wells; wells occupy slots 5-12. */
    public static final int GRAVEYARD_WELL_START = 5;
    public static final int GRAVEYARD_WELL_COUNT = 8;
    public static final int GRAVEYARD_CAN_SLOT = 13;
    public static final int GRAVEYARD_OUTPUT_SLOT = 14;
    public static final int TOTAL_SLOTS = 15;
    public static final int MIXES_REQUIRED = 10;

    /** One mixer recipe: 3 ingredients in any order + soda can -> flat soda. */
    public record MixerRecipe(Set<Item> ingredients, Supplier<ItemStack> result) {}

    private static final List<MixerRecipe> RECIPES = List.of(
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.BLACK), Items.SUGAR, Items.COCOA_BEANS),
                    () -> new ItemStack(ModBlocks.COCOA_COLA_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.RED), Items.SUGAR, Items.SWEET_BERRIES),
                    () -> new ItemStack(ModBlocks.DR_BLAZE_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.LIME), Items.SUGAR, Items.MELON_SLICE),
                    () -> new ItemStack(ModBlocks.PHANTA_SLIME_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.YELLOW), Items.SUGAR, Items.GLOW_BERRIES),
                    () -> new ItemStack(ModBlocks.MOUNTAIN_CREEPER_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.ORANGE), Items.SUGAR, Items.PUMPKIN),
                    () -> new ItemStack(ModBlocks.PHANTA_ORANGE_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.BROWN), Items.SUGAR, Items.SPRUCE_SAPLING),
                    () -> new ItemStack(ModBlocks.SPRUCE_BEER_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.WHITE), Items.SUGAR, Items.MILK_BUCKET),
                    () -> new ItemStack(ModBlocks.PHANTA_CREAM_FLAT.get())),
            new MixerRecipe(
                    Set.of(Items.DYE.pick(DyeColor.PURPLE), Items.SUGAR, Items.LILAC),
                    () -> new ItemStack(ModBlocks.PHANTA_GRAPE_FLAT.get())));

    /** Every item valid in any recipe's ingredient slots. */
    private static final Set<Item> ALL_INGREDIENTS = RECIPES.stream()
            .flatMap(r -> r.ingredients().stream())
            .collect(Collectors.toUnmodifiableSet());

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int mixProgress = 0;
    /** Secret graveyard mode. Deliberately transient: never saved, never synced
     *  by the block entity itself (the open menu syncs it via ContainerData). */
    private boolean graveyardMode = false;

    public SodaMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SODA_MIXER.get(), pos, state);
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
        if (!isOutputSlot(slot) && !removed.isEmpty()) {
            resetProgress();
        }
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
        if (!isOutputSlot(slot)) {
            resetProgress();
        }
        setChanged();
    }

    private static boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT || slot == GRAVEYARD_OUTPUT_SLOT;
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
    // ContainerData (syncs mix progress and graveyard mode to the client menu)
    // ------------------------------------------------------------------

    @Override
    public int get(int index) {
        if (index == 0) {
            return mixProgress;
        }
        if (index == 1) {
            return graveyardMode ? 1 : 0;
        }
        return 0;
    }

    @Override
    public void set(int index, int value) {
        if (index == 0) {
            mixProgress = value;
        } else if (index == 1) {
            graveyardMode = value != 0;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    // ------------------------------------------------------------------
    // MenuProvider
    // ------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.extrodius_soda.soda_mixer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SodaMixerMenu(containerId, playerInventory, this);
    }

    // ------------------------------------------------------------------
    // Mixing logic (server side; called from the menu's clickMenuButton)
    // ------------------------------------------------------------------

    public int getMixProgress() {
        return mixProgress;
    }

    public boolean isGraveyardMode() {
        return graveyardMode;
    }

    public void setGraveyardMode(boolean graveyardMode) {
        if (this.graveyardMode != graveyardMode) {
            this.graveyardMode = graveyardMode;
            resetProgress();
            setChanged();
        }
    }

    /** The 8 carbonated sodas, the only valid graveyard well ingredients.
     *  Built lazily: DeferredItem.get() is only safe after registration. */
    private static Set<Item> graveyardSodas;

    private static Set<Item> getGraveyardSodas() {
        if (graveyardSodas == null) {
            graveyardSodas = Set.of(
                    ModBlocks.COCOA_COLA.get(),
                    ModBlocks.DR_BLAZE.get(),
                    ModBlocks.PHANTA_SLIME.get(),
                    ModBlocks.MOUNTAIN_CREEPER.get(),
                    ModBlocks.PHANTA_ORANGE.get(),
                    ModBlocks.SPRUCE_BEER.get(),
                    ModBlocks.PHANTA_CREAM.get(),
                    ModBlocks.PHANTA_GRAPE.get());
        }
        return graveyardSodas;
    }

    /** True if the stack is one of the 8 carbonated sodas (flat sodas excluded). */
    public static boolean isCarbonatedSoda(ItemStack stack) {
        return !stack.isEmpty() && getGraveyardSodas().contains(stack.getItem());
    }

    /**
     * Graveyard recipe: the 8 wells must hold the 8 different carbonated sodas
     * in any order, plus an empty soda can in the graveyard can slot. The
     * output slot must be empty (the Graveyard doesn't stack).
     */
    public boolean hasValidGraveyardRecipe() {
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < GRAVEYARD_WELL_COUNT; i++) {
            ItemStack stack = items.get(GRAVEYARD_WELL_START + i);
            if (!isCarbonatedSoda(stack) || !seen.add(stack.getItem())) {
                return false;
            }
        }
        if (seen.size() != GRAVEYARD_WELL_COUNT) {
            return false;
        }
        if (!items.get(GRAVEYARD_CAN_SLOT).is(ModBlocks.SODA_CAN_EMPTY.get())) {
            return false;
        }
        return items.get(GRAVEYARD_OUTPUT_SLOT).isEmpty();
    }

    private void onGraveyardMixButtonPressed() {
        if (!hasValidGraveyardRecipe()) {
            resetProgress();
            return;
        }
        mixProgress++;
        if (mixProgress >= MIXES_REQUIRED) {
            // The price of the ultimate soda: everything is consumed, nothing comes back.
            for (int i = 0; i < GRAVEYARD_WELL_COUNT; i++) {
                items.get(GRAVEYARD_WELL_START + i).shrink(1);
            }
            items.get(GRAVEYARD_CAN_SLOT).shrink(1);
            items.set(GRAVEYARD_OUTPUT_SLOT, new ItemStack(ModBlocks.GRAVEYARD.get()));
            mixProgress = 0;
        }
        setChanged();
    }

    private void resetProgress() {
        if (mixProgress != 0) {
            mixProgress = 0;
            setChanged();
        }
    }

    /**
     * The 3 ingredient slots must hold exactly one each of some recipe's
     * ingredients, in any order, plus an empty soda can in the can slot. The
     * output slot must be empty or already hold that recipe's flat soda with
     * room to grow.
     */
    public boolean hasValidRecipe() {
        Optional<MixerRecipe> recipe = findRecipe(this);
        if (recipe.isEmpty()) {
            return false;
        }
        if (!items.get(CAN_SLOT).is(ModBlocks.SODA_CAN_EMPTY.get())) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        ItemStack result = recipe.get().result().get();
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() < output.getMaxStackSize();
    }

    /** Order-independent match: the 3 ingredient slots as a set vs each recipe. */
    public static Optional<MixerRecipe> findRecipe(Container inv) {
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !seen.add(stack.getItem())) {
                return Optional.empty();
            }
        }
        for (MixerRecipe recipe : RECIPES) {
            if (recipe.ingredients().equals(seen)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /** True if the stack is a valid ingredient in any recipe. */
    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty() && ALL_INGREDIENTS.contains(stack.getItem());
    }

    public void onMixButtonPressed(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (graveyardMode) {
            onGraveyardMixButtonPressed();
            return;
        }
        if (!hasValidRecipe()) {
            resetProgress();
            return;
        }
        mixProgress++;
        if (mixProgress >= MIXES_REQUIRED) {
            // Identify the recipe BEFORE consuming the ingredients.
            ItemStack result = findRecipe(this).orElseThrow().result().get();
            boolean usedMilkBucket = false;
            for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
                if (items.get(i).is(Items.MILK_BUCKET)) {
                    usedMilkBucket = true;
                }
                items.get(i).shrink(1);
            }
            items.get(CAN_SLOT).shrink(1);
            if (usedMilkBucket) {
                // The milk's in the soda; the bucket comes back.
                ItemStack bucket = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(bucket)) {
                    player.drop(bucket, false, Prediction.SERVER_ONLY);
                }
            }
            ItemStack output = items.get(OUTPUT_SLOT);
            if (output.isEmpty()) {
                items.set(OUTPUT_SLOT, result);
            } else {
                output.grow(1);
            }
            mixProgress = 0;
        }
        setChanged();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("MixProgress", mixProgress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        mixProgress = input.getIntOr("MixProgress", 0);
    }
}
