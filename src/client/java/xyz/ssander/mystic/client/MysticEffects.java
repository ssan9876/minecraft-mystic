package xyz.ssander.mystic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import xyz.ssander.mystic.MandalaShield;
import xyz.ssander.mystic.TimeRewind;

/**
 * Spells drawn in the world: mandala shields held up in front of their casters, and the Eye of Agamotto's green
 * sigils around the hands of whoever is turning back time, with a great clock face on the ground marking its reach.
 */
public final class MysticEffects {
	private static final int SHIELD = 0xFF8A1C;
	private static final int SHIELD_HOT = 0xFFD27A;
	private static final int TIME = 0x3CFF6A;
	private static final int TIME_PALE = 0xA0FFC0;

	private MysticEffects() {
	}

	/** Green motes drifting backwards around whoever turns back time. Runs every client tick. */
	public static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.isPaused()) {
			return;
		}
		for (Player player : level.players()) {
			if (!MysticPoses.isRewinding(player)) {
				continue;
			}
			RandomSource random = player.getRandom();
			for (int i = 0; i < 3; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double radius = 1.0 + random.nextDouble() * 3.0;
				level.addParticle(TimeRewind.GLOW, player.getX() + Math.cos(angle) * radius, player.getY() + random.nextDouble() * 2.5,
						player.getZ() + Math.sin(angle) * radius, 0.0, 0.0, 0.0);
			}
		}
	}

	/** Submits every shield and time sigil this frame. Runs as the level is drawn. */
	public static void render(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}

		CameraRenderState camera = context.levelState().cameraRenderState;
		float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float time = (level.getGameTime() % 24000L) + partialTick;
		PoseStack poseStack = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		for (Player player : level.players()) {
			if (player.isInvisible() && player != minecraft.player) {
				continue;
			}
			boolean ownFirstPerson = player == minecraft.getCameraEntity() && camera.isFirstPerson;
			if (MandalaShield.isShielding(player)) {
				shield(poseStack, collector, camera.pos, player, partialTick, time, ownFirstPerson);
			}
			if (MysticPoses.isRewinding(player)) {
				timeSigils(poseStack, collector, camera.pos, player, partialTick, time, ownFirstPerson);
			}
		}
	}

	/** The shield stands in front of the caster's eyes, square to where they look, its rings turning both ways. */
	private static void shield(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera, Player player, float partialTick, float time,
			boolean ownFirstPerson) {
		Vec3 look = player.getViewVector(partialTick);
		// In first person it stands further off and fainter, so the caster can still see past it
		Vec3 at = player.getEyePosition(partialTick).add(look.scale(ownFirstPerson ? 1.7 : MandalaShield.DISTANCE)).subtract(0.0, ownFirstPerson ? 0.3 : 0.2, 0.0);
		float pulse = (0.85F + 0.15F * Mth.sin(time * 0.5F + player.getId())) * (ownFirstPerson ? 0.6F : 1.0F);

		poseStack.pushPose();
		poseStack.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
		poseStack.rotateDegrees(Axis.YP, -player.getViewYRot(partialTick));
		poseStack.rotateDegrees(Axis.XP, player.getViewXRot(partialTick));
		float radius = (float) MandalaShield.RADIUS;

		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.ZP, time * 2.5F);
		Glyphs.submit(collector, poseStack, Glyphs.MANDALA, radius, SHIELD, pulse);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, 0.02F);
		poseStack.rotateDegrees(Axis.ZP, -time * 4.0F);
		Glyphs.submit(collector, poseStack, Glyphs.MANDALA, radius * 0.62F, SHIELD_HOT, 0.8F * pulse);
		poseStack.popPose();

		poseStack.popPose();
	}

	/**
	 * Two small rings of runes around the forearm and a clock face before the hand, turning backwards, and the great
	 * clock face on the ground under the holder, as wide as the rewind reaches.
	 */
	private static void timeSigils(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera, Player player, float partialTick, float time,
			boolean ownFirstPerson) {
		Vec3 hand;
		float yaw;
		float pitch;
		float size = 1.0F;
		float alpha = 1.0F;
		if (ownFirstPerson) {
			// Smaller, fainter and down by the hand, so the view stays clear
			Vec3 look = player.getViewVector(partialTick);
			Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
			right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
			Vec3 up = right.cross(look);
			hand = player.getEyePosition(partialTick).add(look.scale(1.0)).add(right.scale(0.42)).add(up.scale(-0.38));
			yaw = player.getViewYRot(partialTick);
			pitch = player.getViewXRot(partialTick);
			size = 0.55F;
			alpha = 0.75F;
		} else {
			yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
			pitch = 0.0F;
			float radians = yaw * Mth.DEG_TO_RAD;
			Vec3 forward = new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
			hand = player.getPosition(partialTick).add(0.0, 1.3, 0.0).add(forward.scale(0.75));
		}

		poseStack.pushPose();
		poseStack.translate(hand.x - camera.x, hand.y - camera.y, hand.z - camera.z);
		poseStack.rotateDegrees(Axis.YP, -yaw);
		poseStack.rotateDegrees(Axis.XP, pitch);

		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.ZP, time * 6.0F);
		Glyphs.submit(collector, poseStack, Glyphs.TIME, 0.55F * size, TIME, alpha);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, -0.25F * size);
		poseStack.rotateDegrees(Axis.ZP, -time * 9.0F);
		Glyphs.submit(collector, poseStack, Glyphs.TIME, 0.3F * size, TIME_PALE, 0.9F * alpha);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, -0.45F * size);
		poseStack.rotateDegrees(Axis.ZP, time * 11.0F);
		Glyphs.submit(collector, poseStack, Glyphs.TIME, 0.24F * size, TIME, 0.8F * alpha);
		poseStack.popPose();

		poseStack.popPose();

		// The reach, on the ground: a clock face turning backwards
		Vec3 feet = player.getPosition(partialTick);
		poseStack.pushPose();
		poseStack.translate(feet.x - camera.x, feet.y + 0.06 - camera.y, feet.z - camera.z);
		poseStack.rotateDegrees(Axis.XP, 90.0F);
		poseStack.rotateDegrees(Axis.ZP, -time * 1.5F);
		Glyphs.submit(collector, poseStack, Glyphs.TIME, (float) TimeRewind.RADIUS, TIME, 0.35F);
		poseStack.popPose();
	}
}
