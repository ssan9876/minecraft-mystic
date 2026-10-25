package xyz.ssander.mystic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Data stored on item stacks. */
public final class MysticComponents {
	/** Where a sling ring's portals lead: a spot in some dimension, and which way to face coming out. */
	public record Destination(GlobalPos pos, float yaw) {
		public static final Codec<Destination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(Destination::pos),
				Codec.FLOAT.fieldOf("yaw").forGetter(Destination::yaw)
		).apply(instance, Destination::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Destination> STREAM_CODEC = StreamCodec.composite(
				GlobalPos.STREAM_CODEC, Destination::pos,
				ByteBufCodecs.FLOAT, Destination::yaw,
				Destination::new);
	}

	public static final DataComponentType<Destination> DESTINATION = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Mystic.id("destination"),
			DataComponentType.<Destination>builder().persistent(Destination.CODEC).networkSynchronized(Destination.STREAM_CODEC).build());

	private MysticComponents() {
	}

	static void init() {
		// Loads the class so the component types register during mod init
	}
}
