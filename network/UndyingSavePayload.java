package com.extrodius.sodamod.network;

import com.extrodius.sodamod.ExtrodiusSoda;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells the client to play the death-cheat animation: the totem camera shot,
 * particles and sound, but with the Graveyard can instead of a totem.
 */
public record UndyingSavePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UndyingSavePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ExtrodiusSoda.MODID, "undying_save"));
    public static final StreamCodec<FriendlyByteBuf, UndyingSavePayload> STREAM_CODEC =
            StreamCodec.unit(new UndyingSavePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
