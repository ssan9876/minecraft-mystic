package xyz.ssander.mystic.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.ssander.mystic.MandalaShield;
import xyz.ssander.mystic.Mystic;
import xyz.ssander.mystic.client.MysticPoses;
import xyz.ssander.mystic.client.MysticRenderState;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	// Hides the wearer's own cape under the cloak (CloakLayer), and notes what spells the player is casting for their pose
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void mystic$applyMystic(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		MysticRenderState mystic = (MysticRenderState) state;
		if (!(entity instanceof Player player)) {
			mystic.mystic$set(0.0F, false, false, -1.0F);
			return;
		}
		if (state.chestEquipment.is(Mystic.CLOAK_OF_LEVITATION)) {
			state.showCape = false;
		}
		mystic.mystic$set(MysticPoses.levitation(player, partialTicks), MandalaShield.isShielding(player), MysticPoses.isRewinding(player),
				MysticPoses.tracing(player, partialTicks));
	}

	// Levitating, the cloak holds its wearer up with a slow rise and fall
	@Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"))
	private void mystic$levitateBob(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo ci) {
		float levitation = ((MysticRenderState) state).mystic$levitation();
		if (levitation > 0.0F) {
			poseStack.translate(0.0F, levitation * Mth.sin(state.ageInTicks * 0.07F) * 0.08F, 0.0F);
		}
	}
}
