package xyz.ssander.mystic;

import java.util.UUID;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/** Per-player data. A missing attachment means "off". */
public final class MysticAttachments {
	/** Set while flight comes from the cloak, so it's only taken away from players we gave it to. */
	public static final AttachmentType<Boolean> CLOAK_FLIGHT_GRANTED = AttachmentRegistry.create(Mystic.id("cloak_flight_granted"),
			builder -> builder.persistent(Codec.BOOL));

	/** Set while the wearer is flying on the cloak. Synced to every client for the levitating pose. */
	public static final AttachmentType<Boolean> LEVITATING = AttachmentRegistry.create(Mystic.id("levitating"),
			builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/** Set while the player holds up a mandala shield. Synced to every client so everyone sees it. */
	public static final AttachmentType<Boolean> SHIELDING = AttachmentRegistry.create(Mystic.id("shielding"),
			builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/** The portal the player's sling ring has open, if any (server only, gone on relog). */
	public static final AttachmentType<UUID> OPEN_PORTAL = AttachmentRegistry.create(Mystic.id("open_portal"));

	private MysticAttachments() {
	}

	static void init() {
		// Loads the class so the attachment types register during mod init
	}
}
