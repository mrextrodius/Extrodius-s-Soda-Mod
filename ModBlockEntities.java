package com.extrodius.sodamod;

import com.extrodius.sodamod.blockentity.CarbonatorBlockEntity;
import com.extrodius.sodamod.blockentity.GasifierBlockEntity;
import com.extrodius.sodamod.blockentity.SodaMixerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block entity type registration for Extrodius's Soda Mod.
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ExtrodiusSoda.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SodaMixerBlockEntity>> SODA_MIXER =
            BLOCK_ENTITIES.register("soda_mixer",
                    () -> new BlockEntityType<>(SodaMixerBlockEntity::new, ModBlocks.SODA_MIXER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasifierBlockEntity>> GASIFIER =
            BLOCK_ENTITIES.register("gasifier",
                    () -> new BlockEntityType<>(GasifierBlockEntity::new, ModBlocks.GASIFIER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarbonatorBlockEntity>> CARBONATOR =
            BLOCK_ENTITIES.register("carbonator",
                    () -> new BlockEntityType<>(CarbonatorBlockEntity::new, ModBlocks.CARBONATOR.get()));
}
