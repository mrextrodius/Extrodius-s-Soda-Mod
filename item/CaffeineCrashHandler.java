package com.extrodius.sodamod.item;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Applies the caffeine crash when a soda's effect wears off: a brief wave of
 * nausea plus a few seconds of slowness. Mild and charming by design.
 *
 * The Graveyard gets the big version: longer nausea and a much heavier
 * slowness, once the last of its eight effects wears off.
 *
 * Tracks each player's soda effect expiry on the server. The crash is skipped
 * if the player still has the effect from another source (potion, beacon),
 * so nobody gets punished for overlapping buffs.
 */
public class CaffeineCrashHandler {
    private record ScheduledCrash(Set<Holder<MobEffect>> effects, long expiryTick, boolean graveyard) {}

    private static final Map<UUID, ScheduledCrash> SCHEDULED = new HashMap<>();

    public static void schedule(ServerPlayer player, MobEffectInstance sodaEffect) {
        SCHEDULED.put(player.getUUID(), new ScheduledCrash(
                Set.of(sodaEffect.getEffect()),
                player.level().getGameTime() + sodaEffect.getDuration(),
                false));
    }

    /** Graveyard: one big crash when the longest of its effects wears off. */
    public static void scheduleGraveyard(ServerPlayer player, List<MobEffectInstance> sodaEffects) {
        Set<Holder<MobEffect>> effects = new HashSet<>();
        long now = player.level().getGameTime();
        long expiry = now;
        for (MobEffectInstance effect : sodaEffects) {
            effects.add(effect.getEffect());
            expiry = Math.max(expiry, now + effect.getDuration());
        }
        SCHEDULED.put(player.getUUID(), new ScheduledCrash(Set.copyOf(effects), expiry, true));
    }

    /** Calls off a pending crash, e.g. when the Undying effect cheats death. */
    public static void cancel(ServerPlayer player) {
        SCHEDULED.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SCHEDULED.isEmpty()) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, ScheduledCrash>> it = SCHEDULED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ScheduledCrash> entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                // Logged out or died: the crash dies with them. No haunting the next life.
                it.remove();
                continue;
            }
            boolean hasAny = entry.getValue().effects().stream().anyMatch(player::hasEffect);
            if (!hasAny && now < entry.getValue().expiryTick() - 100) {
                // Effects vanished well before expiry — cleared early by a totem
                // save (which wipes all effects without firing LivingDeathEvent).
                // No crash; the soda's already gone.
                it.remove();
                continue;
            }
            if (now < entry.getValue().expiryTick()) {
                continue;
            }
            it.remove();
            if (hasAny) {
                continue;
            }
            if (entry.getValue().graveyard()) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 3 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10 * 20, 2));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
            }
        }
    }
}
