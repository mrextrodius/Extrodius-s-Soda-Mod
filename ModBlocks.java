package com.extrodius.sodamod;

import com.extrodius.sodamod.block.CarbonatorBlock;
import com.extrodius.sodamod.block.GasifierBlock;
import com.extrodius.sodamod.block.SodaMixerBlock;
import com.extrodius.sodamod.item.SodaItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Block (and block item) registration for Extrodius's Soda Mod.
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ExtrodiusSoda.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ExtrodiusSoda.MODID);

    public static final DeferredBlock<SodaMixerBlock> SODA_MIXER = BLOCKS.registerBlock(
            "soda_mixer",
            SodaMixerBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> SODA_MIXER_ITEM =
            ITEMS.registerSimpleBlockItem("soda_mixer", SODA_MIXER);

    public static final DeferredBlock<GasifierBlock> GASIFIER = BLOCKS.registerBlock(
            "gasifier",
            GasifierBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(GasifierBlock.LIT) ? 13 : 0));

    public static final DeferredItem<BlockItem> GASIFIER_ITEM =
            ITEMS.registerSimpleBlockItem("gasifier", GASIFIER);

    public static final DeferredBlock<CarbonatorBlock> CARBONATOR = BLOCKS.registerBlock(
            "carbonator",
            CarbonatorBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> CARBONATOR_ITEM =
            ITEMS.registerSimpleBlockItem("carbonator", CARBONATOR);

    // Regular items (non-block).
    public static final DeferredItem<Item> SODA_CAN_EMPTY =
            ITEMS.registerSimpleItem("soda_can_empty");

    public static final DeferredItem<Item> COCOA_COLA_FLAT =
            ITEMS.registerItem("cocoa_cola_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Cocoa Cola: Haste I for 4:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> COCOA_COLA =
            ITEMS.registerItem("cocoa_cola",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.HASTE, 4 * 60 * 20, 0)));

    public static final DeferredItem<Item> DR_BLAZE_FLAT =
            ITEMS.registerItem("dr_blaze_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Dr. Blaze: Fire Resistance for 4:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> DR_BLAZE =
            ITEMS.registerItem("dr_blaze",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4 * 60 * 20, 0)));

    public static final DeferredItem<Item> PHANTA_SLIME_FLAT =
            ITEMS.registerItem("phanta_slime_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Phanta Slime: Jump Boost I for 4:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> PHANTA_SLIME =
            ITEMS.registerItem("phanta_slime",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.JUMP_BOOST, 4 * 60 * 20, 0)));

    public static final DeferredItem<Item> MOUNTAIN_CREEPER_FLAT =
            ITEMS.registerItem("mountain_creeper_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Mountain Creeper: Speed I for 4:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> MOUNTAIN_CREEPER =
            ITEMS.registerItem("mountain_creeper",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.SPEED, 4 * 60 * 20, 0)));

    public static final DeferredItem<Item> PHANTA_ORANGE_FLAT =
            ITEMS.registerItem("phanta_orange_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Phanta Orange: Absorption II for 2:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> PHANTA_ORANGE =
            ITEMS.registerItem("phanta_orange",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.ABSORPTION, 2 * 60 * 20, 1)));

    public static final DeferredItem<Item> SPRUCE_BEER_FLAT =
            ITEMS.registerItem("spruce_beer_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Spruce Beer: Resistance I for 4:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> SPRUCE_BEER =
            ITEMS.registerItem("spruce_beer",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.RESISTANCE, 4 * 60 * 20, 0)));

    public static final DeferredItem<Item> PHANTA_CREAM_FLAT =
            ITEMS.registerItem("phanta_cream_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Phanta Cream: Regeneration I for 1:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> PHANTA_CREAM =
            ITEMS.registerItem("phanta_cream",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.REGENERATION, 1 * 60 * 20, 0)));

    public static final DeferredItem<Item> PHANTA_GRAPE_FLAT =
            ITEMS.registerItem("phanta_grape_flat", props -> new Item(props.stacksTo(1)));

    /** Drinkable carbonated Phanta Grape: Night Vision for 4:00, then the caffeine crash. */
    public static final DeferredItem<SodaItem> PHANTA_GRAPE =
            ITEMS.registerItem("phanta_grape",
                    props -> new SodaItem(props, new MobEffectInstance(MobEffects.NIGHT_VISION, 4 * 60 * 20, 0)));

    /**
     * The Graveyard: all eight soda effects at once, then the big crash.
     * No flat version. Creative-only for now (the secret Mixer mode comes later).
     */
    public static final DeferredItem<SodaItem> GRAVEYARD =
            ITEMS.registerItem("graveyard",
                    props -> new SodaItem(props, List.of(
                            new MobEffectInstance(MobEffects.HASTE, 4 * 60 * 20, 0),
                            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4 * 60 * 20, 0),
                            new MobEffectInstance(MobEffects.JUMP_BOOST, 4 * 60 * 20, 0),
                            new MobEffectInstance(MobEffects.SPEED, 4 * 60 * 20, 0),
                            new MobEffectInstance(MobEffects.ABSORPTION, 2 * 60 * 20, 1),
                            new MobEffectInstance(MobEffects.RESISTANCE, 4 * 60 * 20, 0),
                            new MobEffectInstance(MobEffects.REGENERATION, 1 * 60 * 20, 0),
                            new MobEffectInstance(MobEffects.NIGHT_VISION, 4 * 60 * 20, 0),
                            new MobEffectInstance(ModEffects.UNDYING, 4 * 60 * 20, 0))));

    /** Empty gas canister: crafted, stacks to 64, filled in the gasifier. */
    public static final DeferredItem<Item> CO2_CANISTER_EMPTY =
            ITEMS.registerItem("co2_canister_empty", props -> new Item(props.stacksTo(64)));

    /** Full gas canister: unstackable, spent in the carbonator (empty returned). */
    public static final DeferredItem<Item> CO2_CANISTER_FULL =
            ITEMS.registerItem("co2_canister_full", props -> new Item(props.stacksTo(1)));
}
