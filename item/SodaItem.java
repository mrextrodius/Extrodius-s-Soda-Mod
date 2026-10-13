package com.extrodius.sodamod.item;

import com.extrodius.sodamod.ModBlocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A drinkable carbonated soda. Drinking applies the soda's effect(s), converts the
 * can back into an empty soda can, and schedules the caffeine crash for when
 * the effect(s) wear off.
 *
 * No FOOD component on purpose: sodas are potion-adjacent, they don't refill
 * hunger.
 */
public class SodaItem extends Item {
    private final List<MobEffectInstance> sodaEffects;

    public SodaItem(Item.Properties properties, MobEffectInstance sodaEffect) {
        this(properties, List.of(sodaEffect));
    }

    public SodaItem(Item.Properties properties, List<MobEffectInstance> sodaEffects) {
        super(properties
                .stacksTo(1)
                .component(DataComponents.CONSUMABLE, buildConsumable(sodaEffects))
                .usingConvertsTo(ModBlocks.SODA_CAN_EMPTY.get()));
        this.sodaEffects = List.copyOf(sodaEffects);
    }

    private static Consumable buildConsumable(List<MobEffectInstance> effects) {
        Consumable.Builder builder = Consumable.builder()
                .consumeSeconds(1.6F)
                .animation(ItemUseAnimation.DRINK)
                .sound(SoundEvents.GENERIC_DRINK)
                .hasConsumeParticles(false);
        for (MobEffectInstance effect : effects) {
            builder.onConsume(new ApplyStatusEffectsConsumeEffect(effect, 1.0F));
        }
        return builder.build();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        if (user instanceof ServerPlayer player) {
            if (sodaEffects.size() == 1) {
                CaffeineCrashHandler.schedule(player, sodaEffects.get(0));
            } else {
                CaffeineCrashHandler.scheduleGraveyard(player, sodaEffects);
            }
        }
        return result;
    }
}
