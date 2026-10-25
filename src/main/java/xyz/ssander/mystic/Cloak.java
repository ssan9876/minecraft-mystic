package xyz.ssander.mystic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

/**
 * The Cloak of Levitation, worn in the chest slot. It lifts its wearer into the air (flight, toggled like creative
 * flight), and catches them when they fall: no fall damage, and long falls slow to a drift.
 */
public final class Cloak {
	/** Falling further than this without flying, the cloak slows the fall. */
	private static final double CATCH_DISTANCE = 4.0;
	/** Slow falling after the cloak comes off in mid-air, so survival players don't fall to their death. */
	private static final int SAFE_LANDING_TICKS = 100;

	private Cloak() {
	}

	public static boolean isWorn(Player player) {
		return player.getItemBySlot(EquipmentSlot.CHEST).is(Mystic.CLOAK_OF_LEVITATION);
	}

	/** Grants or removes the cloak's flight to match what's worn. Runs every server tick. */
	public static void tick(ServerPlayer player) {
		boolean worn = isWorn(player);
		updateFlight(player, worn);

		boolean levitating = worn && player.getAbilities().flying;
		if (levitating != player.getAttachedOrElse(MysticAttachments.LEVITATING, false)) {
			player.setAttached(MysticAttachments.LEVITATING, levitating ? true : null);
		}

		// Once a fall gets long, the cloak slows it to a drift until its wearer lands
		boolean falling = !player.onGround() && !player.getAbilities().flying && !player.isFallFlying() && player.getKnownMovement().y < -0.05;
		if (worn && falling && (player.fallDistance > CATCH_DISTANCE || player.hasEffect(MobEffects.SLOW_FALLING))) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false, false));
		}
	}

	private static void updateFlight(ServerPlayer player, boolean wanted) {
		Abilities abilities = player.getAbilities();
		boolean granted = player.getAttachedOrElse(MysticAttachments.CLOAK_FLIGHT_GRANTED, false);
		if (wanted) {
			// Also re-grants it after a game mode change (or another mod) takes it away
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				player.onUpdateAbilities();
			}
			if (!granted) {
				player.setAttached(MysticAttachments.CLOAK_FLIGHT_GRANTED, true);
			}
		} else if (granted) {
			player.removeAttached(MysticAttachments.CLOAK_FLIGHT_GRANTED);
			// Creative and spectator players fly anyway
			if (!player.isCreative() && !player.isSpectator()) {
				abilities.mayfly = false;
				abilities.flying = false;
				player.onUpdateAbilities();
				if (!player.onGround()) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SAFE_LANDING_TICKS, 0, false, false, true));
				}
			}
		}
	}
}
