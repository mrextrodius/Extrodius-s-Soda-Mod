package com.extrodius.sodamod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Undying: a beneficial effect that cheats death once. While active, lethal
 * damage (that doesn't bypass invulnerability, same as a Totem of Undying)
 * instead returns the player to full health and clears every status effect.
 */
public class UndyingEffect extends MobEffect {
    public UndyingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6A3FA0);
    }
}
