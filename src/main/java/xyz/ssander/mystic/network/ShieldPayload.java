package xyz.ssander.mystic.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import xyz.ssander.mystic.Mystic;

/** Sent by the client when the shield key is pressed or let go. */
public record ShieldPayload(boolean shielding) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ShieldPayload> TYPE = new CustomPacketPayload.Type<>(Mystic.id("shield"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ShieldPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ShieldPayload::shielding,
			ShieldPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
