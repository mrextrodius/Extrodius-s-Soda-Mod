package com.extrodius.sodamod;

import com.extrodius.sodamod.effect.UndyingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom mob effect registration for Extrodius's Soda Mod.
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ExtrodiusSoda.MODID);

    /**
     The Undying effect, granted by the Graveyard. While active, lethal damage
     is cheated exactly once: instead of dying you return to full health, at
     the cost of every status effect you had.
     */
    public static final DeferredHolder<MobEffect, UndyingEffect> UNDYING =
            MOB_EFFECTS.register("undying", UndyingEffect::new);
}
