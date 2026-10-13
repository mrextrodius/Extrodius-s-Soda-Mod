package com.extrodius.sodamod.client;

import com.extrodius.sodamod.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client side of the Undying death-cheat: exactly what the vanilla totem
 * animation does (particles, sound, item flying up at the camera), but the
 * item is the Graveyard can.
 */
@OnlyIn(Dist.CLIENT)
public class UndyingSaveClient {
    public static void play() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        mc.particleEngine.createTrackingEmitter(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, player.getSoundSource(), 1.0F, 1.0F, false);
        player.displayItemActivation(new ItemStack(ModBlocks.GRAVEYARD.get()));
    }
}
