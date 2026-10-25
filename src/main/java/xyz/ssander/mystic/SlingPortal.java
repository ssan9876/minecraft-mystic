package xyz.ssander.mystic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import xyz.ssander.mystic.MysticComponents.Destination;

/**
 * One end of a sling ring portal: a standing disc ringed with sparks. Portals come in linked pairs, one in front of
 * the sorcerer who opened it and one at the sling ring's destination. Anything that passes through one comes out of
 * the other, turned to match, still moving the same way. Pairs close together, after a while or when the sorcerer
 * uses the ring again, and aren't saved.
 * <p>
 * The entity's position is the bottom middle of the disc. It faces {@link #facing()}: walking that way through it
 * goes in at the front.
 */
public class SlingPortal extends Entity {
	/** The disc's radius when fully open, and the entity's size (enough room for the disc turned any way). */
	public static final float RADIUS = 1.3F;
	public static final float SIZE = 2.8F;
	/** How high the disc's middle is above the entity's position. */
	public static final float CENTER_HEIGHT = 1.4F;
	public static final DustParticleOptions SPARK = new DustParticleOptions(0xFF9A2A, 0.7F);
	/** Ticks to open up and to close, and how long a pair stays open. */
	private static final int OPEN_TICKS = 24;
	private static final int CLOSE_TICKS = 14;
	private static final int LIFETIME = 20 * 60;
	/** The new portal opens this far in front of the sorcerer. */
	private static final double OPEN_DISTANCE = 2.2;
	/** Ticks before an entity that came through can go through a portal again. */
	private static final int TRAVEL_COOLDOWN = 10;
	/** How often to renew the chunk tickets that keep both ends loaded (they last 300 ticks). */
	private static final int TICKET_TICKS = 100;

	private static final EntityDataAccessor<Float> OPENNESS = SynchedEntityData.defineId(SlingPortal.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(SlingPortal.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SlingPortal.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> CLOSING = SynchedEntityData.defineId(SlingPortal.class, EntityDataSerializers.BOOLEAN);

	private @Nullable UUID linked;
	private @Nullable ResourceKey<Level> linkedLevel;
	/** Which side of the disc each nearby entity was on last tick, as a signed distance. */
	private Map<UUID, Double> sides = new HashMap<>();
	private float opennessO;

	public SlingPortal(EntityType<? extends SlingPortal> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/**
	 * Opens a pair of portals: one in front of the sorcerer and facing the way they look, and one standing on the
	 * ring's destination, facing the way they faced when attuning it.
	 */
	public static void open(ServerPlayer owner, ServerLevel destinationLevel, Destination destination) {
		ServerLevel level = owner.level();
		float yaw = Mth.wrapDegrees(owner.getYRot());
		Vec3 base = owner.position().add(normal(yaw).scale(OPEN_DISTANCE));
		SlingPortal here = create(level, base, yaw, owner.getId());

		Vec3 there = Vec3.atBottomCenterOf(destination.pos().pos());
		ChunkPos chunk = ChunkPos.containing(destination.pos().pos());
		destinationLevel.getChunkSource().addTicketWithRadius(TicketType.PORTAL, chunk, 2);
		destinationLevel.getChunk(chunk.x(), chunk.z());
		SlingPortal away = create(destinationLevel, there, destination.yaw(), -1);

		here.link(away);
		away.link(here);
		level.addFreshEntity(here);
		destinationLevel.addFreshEntity(away);
		owner.setAttached(MysticAttachments.OPEN_PORTAL, here.getUUID());

		level.playSound(null, base.x, base.y + CENTER_HEIGHT, base.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
		level.playSound(null, base.x, base.y + CENTER_HEIGHT, base.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F, 1.6F);
		destinationLevel.playSound(null, there.x, there.y + CENTER_HEIGHT, there.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
	}

	private static SlingPortal create(ServerLevel level, Vec3 base, float yaw, int owner) {
		SlingPortal portal = new SlingPortal(Mystic.SLING_PORTAL, level);
		portal.setPos(base);
		portal.entityData.set(FACING, yaw);
		portal.entityData.set(OWNER, owner);
		return portal;
	}

	private void link(SlingPortal other) {
		this.linked = other.getUUID();
		this.linkedLevel = other.level().dimension();
	}

	/** The portal the player's sling ring has open, if it's still there. */
	public static @Nullable SlingPortal openBy(ServerPlayer player) {
		UUID id = player.getAttached(MysticAttachments.OPEN_PORTAL);
		if (id == null) {
			return null;
		}
		if (player.level().getEntity(id) instanceof SlingPortal portal && !portal.isClosing()) {
			return portal;
		}
		player.removeAttached(MysticAttachments.OPEN_PORTAL);
		return null;
	}

	/** The way into the front of the disc, for a portal facing {@code yaw}. */
	public static Vec3 normal(float yaw) {
		float radians = yaw * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
	}

	public Vec3 center() {
		return this.position().add(0.0, CENTER_HEIGHT, 0.0);
	}

	public float facing() {
		return this.entityData.get(FACING);
	}

	/** The entity id of the sorcerer who opened this end, or -1 for the far end. */
	public int owner() {
		return this.entityData.get(OWNER);
	}

	/** How far open the portal is, 0 to 1. */
	public float openness(float partialTick) {
		return Mth.lerp(partialTick, this.opennessO, this.entityData.get(OPENNESS));
	}

	public boolean isOpening() {
		return this.entityData.get(OPENNESS) < 1.0F && !this.isClosing();
	}

	public boolean isClosing() {
		return this.entityData.get(CLOSING);
	}

	public void closePair() {
		this.close();
		SlingPortal other = this.linkedPortal();
		if (other != null) {
			other.close();
		}
	}

	private void close() {
		if (!this.isClosing() && this.level() instanceof ServerLevel level) {
			this.entityData.set(CLOSING, true);
			Vec3 c = this.center();
			level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.2F);
		}
	}

	private @Nullable SlingPortal linkedPortal() {
		if (this.linked == null || this.linkedLevel == null || !(this.level() instanceof ServerLevel level)) {
			return null;
		}
		ServerLevel other = level.getServer().getLevel(this.linkedLevel);
		return other != null && other.getEntity(this.linked) instanceof SlingPortal portal ? portal : null;
	}

	@Override
	public void tick() {
		super.tick();
		this.opennessO = this.entityData.get(OPENNESS);
		if (!(this.level() instanceof ServerLevel level)) {
			this.sparks();
			return;
		}

		float openness = this.entityData.get(OPENNESS);
		if (this.isClosing()) {
			openness -= 1.0F / CLOSE_TICKS;
			if (openness <= 0.0F) {
				this.discard();
				return;
			}
		} else {
			openness = Math.min(1.0F, openness + 1.0F / OPEN_TICKS);
		}
		this.entityData.set(OPENNESS, openness);

		SlingPortal other = this.linkedPortal();
		if (this.tickCount > LIFETIME || (this.tickCount > 5 && (other == null || other.isClosing()))) {
			this.closePair();
		}
		if (this.tickCount % TICKET_TICKS == 1) {
			level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(this.blockPosition()), 2);
		}
		if (this.tickCount % 18 == 0 && this.random.nextBoolean()) {
			Vec3 c = this.center();
			level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_AMBIENT, SoundSource.AMBIENT, 0.7F, 1.4F + this.random.nextFloat() * 0.4F);
		}

		if (other != null && !this.isClosing() && openness > 0.6F) {
			this.passThrough(level, other, RADIUS * openness);
		}
	}

	/** Sends everything that crossed the disc since last tick out of the other portal. */
	private void passThrough(ServerLevel level, SlingPortal other, float radius) {
		Vec3 c = this.center();
		Vec3 n = normal(this.facing());
		Map<UUID, Double> seen = new HashMap<>();
		for (Entity entity : level.getEntities(this, new AABB(c, c).inflate(RADIUS + 1.5),
				e -> !(e instanceof SlingPortal) && !e.isSpectator() && !e.isPassenger())) {
			Vec3 rel = entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0).subtract(c);
			double side = rel.dot(n);
			seen.put(entity.getUUID(), side);
			Double before = this.sides.get(entity.getUUID());
			if (before == null || entity.isOnPortalCooldown() || Math.signum(before) == Math.signum(side) || Math.abs(before - side) > 3.0) {
				continue;
			}
			if (rel.subtract(n.scale(side)).length() < radius) {
				this.send(entity, other, rel, side);
			}
		}
		this.sides = seen;
	}

	/**
	 * Moves an entity from this portal to the other: the same place relative to the disc, turned by the difference
	 * between the two portals' facings, nudged clear of the far disc so it doesn't cross straight back.
	 */
	private void send(Entity entity, SlingPortal other, Vec3 rel, double side) {
		float turn = other.facing() - this.facing();
		Vec3 out = rotate(rel, turn).add(normal(other.facing()).scale(Math.signum(side) * 0.3));
		Vec3 to = other.center().add(out).subtract(0.0, entity.getBbHeight() / 2.0, 0.0);
		Vec3 motion = rotate(entity instanceof ServerPlayer player ? player.getKnownMovement() : entity.getDeltaMovement(), turn);

		ServerLevel from = (ServerLevel) this.level();
		ServerLevel otherLevel = (ServerLevel) other.level();
		Vec3 at = entity.position();
		Entity arrived = entity.teleport(new TeleportTransition(otherLevel, to, motion, entity.getYRot() + turn, entity.getXRot(), TeleportTransition.DO_NOTHING));
		if (arrived == null) {
			return;
		}
		arrived.setPortalCooldown(TRAVEL_COOLDOWN);
		other.sides.remove(arrived.getUUID());
		from.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.5F, 1.8F);
		otherLevel.playSound(null, to.x, to.y, to.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.5F, 1.8F);
	}

	/** Turns a vector about the vertical by {@code degrees} of yaw: normal(y) turned by d is normal(y + d). */
	private static Vec3 rotate(Vec3 v, float degrees) {
		float radians = degrees * Mth.DEG_TO_RAD;
		float cos = Mth.cos(radians);
		float sin = Mth.sin(radians);
		return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
	}

	/** Sparks thrown off the spinning rim. Client side. */
	private void sparks() {
		float openness = this.openness(1.0F);
		if (openness <= 0.05F) {
			return;
		}
		RandomSource random = this.random;
		Vec3 c = this.center();
		Vec3 n = normal(this.facing());
		Vec3 right = new Vec3(-n.z, 0.0, n.x);
		Vec3 up = new Vec3(0.0, 1.0, 0.0);
		float radius = RADIUS * openness;
		int count = this.isClosing() ? 2 : 5;
		for (int i = 0; i < count; i++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			Vec3 out = right.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
			// The rim spins clockwise as seen from the front: the sparks fly off along it
			Vec3 along = right.scale(Mth.sin(angle)).add(up.scale(-Mth.cos(angle)));
			Vec3 at = c.add(out.scale(radius));
			Vec3 speed = along.scale(0.12 + random.nextDouble() * 0.12).add(out.scale(0.03));
			this.level().addParticle(random.nextInt(4) == 0 ? ParticleTypes.SMALL_FLAME : SPARK, at.x, at.y, at.z, speed.x, speed.y, speed.z);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OPENNESS, 0.0F);
		builder.define(FACING, 0.0F);
		builder.define(OWNER, -1);
		builder.define(CLOSING, false);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
