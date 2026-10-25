package xyz.ssander.mystic.client;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import xyz.ssander.mystic.Mystic;
import xyz.ssander.mystic.MysticAttachments;
import xyz.ssander.mystic.SlingPortal;

/** What each player is doing with the mystic arts, as the renderer and the effects need it. */
public final class MysticPoses {
	/** How fast the levitating pose eases in and out, per tick. */
	private static final float LEVITATE_EASE = 0.12F;

	private static final Map<Player, float[]> LEVITATION = new WeakHashMap<>();

	private MysticPoses() {
	}

	/** Eases each player toward or away from the levitating pose. Runs every client tick. */
	public static void tick(Minecraft minecraft) {
		if (minecraft.level == null) {
			LEVITATION.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		for (Player player : minecraft.level.players()) {
			float[] amount = LEVITATION.computeIfAbsent(player, p -> new float[2]);
			amount[0] = amount[1];
			boolean levitating = player.getAttachedOrElse(MysticAttachments.LEVITATING, false) && !player.onGround();
			amount[1] = Mth.clamp(amount[1] + (levitating ? LEVITATE_EASE : -LEVITATE_EASE), 0.0F, 1.0F);
		}
	}

	/** How far into the levitating pose the player is, 0 to 1. */
	public static float levitation(Player player, float partialTick) {
		float[] amount = LEVITATION.get(player);
		return amount == null ? 0.0F : Mth.lerp(partialTick, amount[0], amount[1]);
	}

	public static boolean isRewinding(Player player) {
		return player.isUsingItem() && player.getUseItem().is(Mystic.EYE_OF_AGAMOTTO);
	}

	/**
	 * How far the player is through tracing open a portal with their sling ring, 0 to 1, or -1 if they aren't. The
	 * portal's own opening shows it, since it opens in front of whoever cast it.
	 */
	public static float tracing(Player player, float partialTick) {
		for (SlingPortal portal : player.level().getEntitiesOfClass(SlingPortal.class, new AABB(player.blockPosition()).inflate(4.0))) {
			if (portal.owner() == player.getId() && portal.isOpening()) {
				return portal.openness(partialTick);
			}
		}
		return -1.0F;
	}
}
