package com.extrodius.sodamod.effect;

import com.extrodius.sodamod.ModEffects;
import com.extrodius.sodamod.item.CaffeineCrashHandler;
import com.extrodius.sodamod.network.UndyingSavePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cheats death for players under the Undying effect. Mirrors a Totem of
 * Undying: damage that bypasses invulnerability (void, /kill) still kills.
 *
 * On a save the player returns to full health but loses every status effect,
 * the pending caffeine crash is called off, and the client plays the totem
 * animation with the Graveyard can instead of a totem.
 */
public class UndyingDeathHandler {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        // A real totem takes priority over the Undying effect. It clears all
        // effects when it saves you, so the pending crash is called off.
        if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            CaffeineCrashHandler.cancel(player);
            return;
        }
        if (!player.hasEffect(ModEffects.UNDYING)) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        CaffeineCrashHandler.cancel(player);
        PacketDistributor.sendToPlayer(player, new UndyingSavePayload());
    }
}
