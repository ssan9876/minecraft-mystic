package xyz.ssander.mystic.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.ssander.mystic.client.MysticRenderState;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	// Runs after vanilla has posed the limbs: levitating, then the arms of whatever spell is being cast
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void mystic$spellPoses(AvatarRenderState state, CallbackInfo ci) {
		MysticRenderState mystic = (MysticRenderState) state;
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		float t = state.ageInTicks;

		// Levitating: legs together and hanging, toes down, arms held a little out from the sides, drifting
		float levitation = mystic.mystic$levitation();
		if (levitation > 0.0F) {
			float sway = Mth.sin(t * 0.07F);
			mystic$blend(model.rightLeg, levitation, 0.18F + sway * 0.04F, 0.0F, 0.03F);
			mystic$blend(model.leftLeg, levitation, 0.08F - sway * 0.04F, 0.0F, -0.03F);
			mystic$blend(model.rightArm, levitation, 0.05F + sway * 0.03F, 0.0F, 0.32F);
			mystic$blend(model.leftArm, levitation, 0.05F - sway * 0.03F, 0.0F, -0.32F);
		}

		// Shield: both palms pushed out along the line of sight
		if (mystic.mystic$shielding()) {
			model.rightArm.xRot = model.head.xRot - Mth.HALF_PI;
			model.rightArm.yRot = model.head.yRot - 0.25F;
			model.rightArm.zRot = 0.0F;
			model.leftArm.xRot = model.head.xRot - Mth.HALF_PI;
			model.leftArm.yRot = model.head.yRot + 0.25F;
			model.leftArm.zRot = 0.0F;
			return;
		}

		// Turning back time: the eye arm held across the chest, the other hand before it, winding backwards
		if (mystic.mystic$rewinding()) {
			float wind = t * 0.5F;
			model.leftArm.xRot = -Mth.HALF_PI + 0.15F;
			model.leftArm.yRot = 0.55F;
			model.leftArm.zRot = 0.0F;
			model.rightArm.xRot = -Mth.HALF_PI - 0.05F + Mth.sin(wind) * 0.18F;
			model.rightArm.yRot = -0.25F - Mth.cos(wind) * 0.18F;
			model.rightArm.zRot = 0.0F;
			return;
		}

		// Tracing a portal: the ring hand sweeps out two quick circles before it, the other hand held out
		float tracing = mystic.mystic$tracing();
		if (tracing >= 0.0F) {
			float angle = tracing * Mth.TWO_PI * 2.0F;
			model.rightArm.xRot = -Mth.HALF_PI + Mth.sin(angle) * 0.55F;
			model.rightArm.yRot = Mth.cos(angle) * 0.55F;
			model.rightArm.zRot = 0.0F;
			model.leftArm.xRot = -Mth.HALF_PI * 0.6F;
			model.leftArm.yRot = 0.35F;
			model.leftArm.zRot = 0.0F;
		}
	}

	@Unique
	private static void mystic$blend(ModelPart part, float amount, float xRot, float yRot, float zRot) {
		part.xRot = Mth.lerp(amount, part.xRot, xRot);
		part.yRot = Mth.lerp(amount, part.yRot, yRot);
		part.zRot = Mth.lerp(amount, part.zRot, zRot);
	}
}
