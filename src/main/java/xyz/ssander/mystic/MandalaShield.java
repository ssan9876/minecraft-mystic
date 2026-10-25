package xyz.ssander.mystic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A mandala shield, held up with the shield key by whoever wears the Cloak of Levitation. It stops anything that
 * comes at its caster from the front, throws back whoever struck it, and sends projectiles back where they came from.
 */
public final class MandalaShield {
	public static final DustParticleOptions SPARK = new DustParticleOptions(0xFFA030, 0.9F);
	/** How far in front of the eyes the shield stands, and how wide it reaches. */
	public static final double DISTANCE = 1.0;
	public static final double RADIUS = 0.95;
	/** Hits from within this angle of where the caster looks are stopped (the dot product of the two directions). */
	private static final double FRONT = 0.2;

	private MandalaShield() {
	}

	public static boolean isShielding(Player player) {
		return player.getAttachedOrElse(MysticAttachments.SHIELDING, false);
	}

	/** The caster pressed or let go of the shield key. */
	public static void setShielding(ServerPlayer player, boolean shielding) {
		if (shielding == isShielding(player)) {
			return;
		}
		ServerLevel level = player.level();
		if (!shielding) {
			player.removeAttached(MysticAttachments.SHIELDING);
			level.playSound(null, player.getX(), player.getY() + 1.4, player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.8F);
			return;
		}
		if (!Cloak.isWorn(player)) {
			player.sendOverlayMessage(Component.translatable("message.mystic.shield_no_cloak"));
			return;
		}
		player.setAttached(MysticAttachments.SHIELDING, true);
		Vec3 at = center(player);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.5F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
	}

	/** Where the shield's middle is, in front of the caster's eyes and a little below. */
	public static Vec3 center(Player player) {
		return player.getEyePosition().add(player.getViewVector(1.0F).scale(DISTANCE)).subtract(0.0, 0.2, 0.0);
	}

	/** Turns back projectiles flying into the shield. Runs every server tick. */
	static void tick(ServerPlayer player) {
		if (!isShielding(player)) {
			return;
		}
		if (!Cloak.isWorn(player) || player.isSpectator() || !player.isAlive()) {
			player.removeAttached(MysticAttachments.SHIELDING);
			return;
		}

		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 c = center(player);
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(RADIUS + 1.5))) {
			Vec3 motion = projectile.getDeltaMovement();
			if (projectile.getOwner() == player || motion.lengthSqr() < 0.01 || motion.dot(look) >= 0.0) {
				continue;
			}
			// Will it reach the shield's plane within the next tick or two, inside its rim?
			Vec3 rel = projectile.position().subtract(c);
			double ahead = rel.dot(look);
			if (ahead < -0.5 || ahead > motion.length() * 2.0 + 0.5) {
				continue;
			}
			if (rel.subtract(look.scale(ahead)).length() > RADIUS + 0.4) {
				continue;
			}
			Vec3 bounced = motion.subtract(look.scale(2.0 * motion.dot(look)));
			projectile.setDeltaMovement(bounced);
			projectile.setOwner(player);
			projectile.needsSync = true;
			spark(level, projectile.position(), 10);
			level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.2F, 0.8F);
		}

		if (player.tickCount % 12 == 0) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.6F);
		}
	}

	/**
	 * Whether the shield stops this damage: anything coming from in front, whose source has a position (a blow, a
	 * projectile, an explosion), unless it gets past shields.
	 */
	static boolean blocks(ServerPlayer player, DamageSource source) {
		if (!isShielding(player) || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
			return false;
		}
		Vec3 from = source.getSourcePosition();
		if (from == null) {
			return false;
		}
		Vec3 toSource = from.subtract(player.getEyePosition());
		Vec3 look = player.getViewVector(1.0F);
		if (toSource.lengthSqr() > 1.0E-4 && toSource.normalize().dot(look) < FRONT) {
			return false;
		}

		ServerLevel level = player.level();
		spark(level, center(player), 16);
		level.playSound(null, player.getX(), player.getY() + 1.4, player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.4F, 0.6F);
		Entity attacker = source.getDirectEntity();
		if (attacker instanceof LivingEntity living && !source.is(DamageTypeTags.IS_PROJECTILE)) {
			Vec3 away = living.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
			living.push((away.lengthSqr() < 1.0E-4 ? look.multiply(1.0, 0.0, 1.0) : away).normalize().scale(0.9).add(0.0, 0.3, 0.0));
			living.needsSync = true;
		}
		return true;
	}

	private static void spark(ServerLevel level, Vec3 at, int count) {
		level.sendParticles(SPARK, at.x, at.y, at.z, count, 0.3, 0.3, 0.3, 0.0);
	}
}
