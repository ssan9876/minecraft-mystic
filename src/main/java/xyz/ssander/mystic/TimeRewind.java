package xyz.ssander.mystic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Eye of Agamotto's power: turning back time around its holder. Every block change in every loaded dimension is
 * remembered for a minute (what was there before, with its contents). Rewinding walks back through them, newest
 * first and faster the longer it's held, putting each block within reach back as it was. Items that spilled out of
 * broken blocks in that time go back too, so nothing is doubled.
 */
public final class TimeRewind {
	/** How far back time can be turned, in ticks. */
	public static final int WINDOW = 20 * 60;
	/** How far from the holder blocks are put back. */
	public static final double RADIUS = 20.0;
	/** The most changes remembered per dimension; the oldest go first past this. */
	private static final int MAX_CHANGES = 250_000;
	/** Ticks of history undone each tick, rising to the maximum the longer the eye is held. */
	private static final int MAX_RATE = 12;
	private static final int RAMP_TICKS = 6;
	/** Changes to block updates while putting things back: tell clients, but don't drop, update shapes or trigger anything. */
	private static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
	public static final DustParticleOptions GLOW = new DustParticleOptions(0x4CFF7A, 1.0F);

	private static final class Change {
		final long time;
		final BlockPos pos;
		final BlockState old;
		final @Nullable CompoundTag blockEntity;
		boolean undone;

		Change(long time, BlockPos pos, BlockState old, @Nullable CompoundTag blockEntity) {
			this.time = time;
			this.pos = pos;
			this.old = old;
			this.blockEntity = blockEntity;
		}
	}

	/** An item that spilled out of a block: when, and from where (it may have been flung far since). */
	private record Drop(long time, Vec3 from) {
	}

	/** How far back a holder has turned time: the game time they've reached. */
	private record Rewind(long reached, int ticks) {
	}

	private static final Map<ServerLevel, Deque<Change>> HISTORY = new WeakHashMap<>();
	/** Items spilled from blocks. */
	private static final Map<ServerLevel, Map<Entity, Drop>> DROPS = new WeakHashMap<>();
	private static final Map<UUID, Rewind> REWINDING = new HashMap<>();
	/** Set while putting blocks back, so that isn't remembered as a change itself. */
	private static boolean restoring;

	private TimeRewind() {
	}

	public static boolean isRestoring() {
		return restoring;
	}

	/**
	 * Remembers a block that changed, and its block entity's data from just before (a container's contents spill out
	 * as it goes). Called for every block change on the server (see LevelMixin).
	 */
	public static void record(ServerLevel level, BlockPos pos, BlockState old, @Nullable CompoundTag blockEntity) {
		Deque<Change> history = HISTORY.computeIfAbsent(level, l -> new ArrayDeque<>());
		history.addLast(new Change(level.getGameTime(), pos.immutable(), old, blockEntity));
		if (history.size() > MAX_CHANGES) {
			history.removeFirst();
		}
	}

	/** Remembers an item that spilled out of a block (see the drop mixins). */
	public static void recordDrop(ServerLevel level, Entity item) {
		DROPS.computeIfAbsent(level, l -> new WeakHashMap<>()).put(item, new Drop(level.getGameTime(), item.position()));
	}

	/** Forgets what's older than the window. Runs every server tick. */
	static void prune(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			long oldest = level.getGameTime() - WINDOW;
			Deque<Change> history = HISTORY.get(level);
			if (history != null) {
				while (!history.isEmpty() && (history.peekFirst().time < oldest || history.peekFirst().undone)) {
					history.removeFirst();
				}
			}
			Map<Entity, Drop> drops = DROPS.get(level);
			if (drops != null) {
				drops.entrySet().removeIf(entry -> entry.getValue().time() < oldest || entry.getKey().isRemoved());
			}
		}
	}

	public static boolean isRewinding(ServerPlayer player) {
		return REWINDING.containsKey(player.getUUID());
	}

	/** Forgets a holder's rewind once they stop using the eye. Runs every server tick. */
	static void tickPlayer(ServerPlayer player) {
		if (isRewinding(player) && !(player.isUsingItem() && player.getUseItem().is(Mystic.EYE_OF_AGAMOTTO))) {
			stop(player);
		}
	}

	/** Turns time back a little further around the holder. Runs every tick the eye is held. */
	public static void rewind(ServerPlayer player, int heldTicks) {
		ServerLevel level = player.level();
		long now = level.getGameTime();
		Rewind rewind = REWINDING.get(player.getUUID());
		if (rewind == null) {
			rewind = new Rewind(now, 0);
			level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
		}

		int rate = Math.min(MAX_RATE, 1 + rewind.ticks() / RAMP_TICKS);
		long reached = Math.max(now - WINDOW, rewind.reached() - rate);
		REWINDING.put(player.getUUID(), new Rewind(reached, rewind.ticks() + 1));

		Vec3 center = player.position();
		double radiusSqr = RADIUS * RADIUS;
		int undone = 0;
		Deque<Change> history = HISTORY.get(level);
		if (history != null) {
			for (Iterator<Change> it = history.descendingIterator(); it.hasNext(); ) {
				Change change = it.next();
				if (change.time < reached) {
					break;
				}
				if (!change.undone && change.pos.distToCenterSqr(center) <= radiusSqr) {
					restore(level, change);
					change.undone = true;
					undone++;
				}
			}
		}

		Map<Entity, Drop> drops = DROPS.get(level);
		if (drops != null) {
			drops.entrySet().removeIf(entry -> {
				Entity item = entry.getKey();
				Drop drop = entry.getValue();
				if (drop.time() < reached || item.isRemoved() || item.level() != level || drop.from().distanceToSqr(center) > radiusSqr) {
					return false;
				}
				level.sendParticles(GLOW, item.getX(), item.getY() + 0.2, item.getZ(), 4, 0.1, 0.1, 0.1, 0.0);
				item.discard();
				return true;
			});
		}

		if (undone > 0 && rewind.ticks() % 3 == 0) {
			level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F,
					0.5F + 0.1F * rate);
		}
		if (rewind.ticks() % 10 == 0) {
			level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.5F + 0.08F * rate);
		}
	}

	/** How many seconds back the holder has turned time so far. */
	public static float secondsBack(ServerPlayer player) {
		Rewind rewind = REWINDING.get(player.getUUID());
		return rewind == null ? 0.0F : (player.level().getGameTime() - rewind.reached()) / 20.0F;
	}

	public static void stop(ServerPlayer player) {
		if (REWINDING.remove(player.getUUID()) != null) {
			player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
		}
	}

	private static void restore(ServerLevel level, Change change) {
		restoring = true;
		try {
			level.setBlock(change.pos, change.old, RESTORE_FLAGS);
			if (change.blockEntity != null && level.getBlockEntity(change.pos) instanceof BlockEntity blockEntity) {
				blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), change.blockEntity));
				blockEntity.setChanged();
				level.sendBlockUpdated(change.pos, change.old, change.old, Block.UPDATE_CLIENTS);
			}
		} finally {
			restoring = false;
		}
		Vec3 at = Vec3.atCenterOf(change.pos);
		level.sendParticles(GLOW, at.x, at.y, at.z, 2, 0.35, 0.35, 0.35, 0.0);
	}
}
