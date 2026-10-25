package xyz.ssander.mystic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import xyz.ssander.mystic.Mystic;

/** Draws the Cloak of Levitation on whoever wears it (the tunic under it is its armor layer). */
public class CloakLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final CloakModel model;

	public CloakLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, CloakModel model) {
		super(renderer);
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || !state.chestEquipment.is(Mystic.CLOAK_OF_LEVITATION)) {
			return;
		}
		submitNodeCollector.submitModel(this.model, state, poseStack, RenderTypes.entityCutout(CloakModel.TEXTURE), lightCoords,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
	}
}
